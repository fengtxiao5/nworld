package com.demo.job.admin.core.thread;

import com.demo.job.admin.core.conf.XxlJobAdminConfig;
import com.demo.job.admin.core.model.XxlJobGroup;
import com.demo.job.admin.core.model.XxlJobRegistry;
import com.demo.job.core.biz.model.RegistryParam;
import com.demo.job.core.biz.model.ReturnT;
import com.demo.job.core.enums.RegistryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.*;

public class JobRegistryHelper {

    private static Logger logger = LoggerFactory.getLogger(JobRegistryHelper.class);

    private static JobRegistryHelper instance = new JobRegistryHelper();

    public static JobRegistryHelper getInstance() {
        return instance;
    }

    private ThreadPoolExecutor registryOrRemoveThreadPool = null;
    private Thread registryMonitorThread;
    private volatile boolean toStop = false;

    public void start() {
        registryOrRemoveThreadPool = new ThreadPoolExecutor(2, 10,
                30L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(2000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "xxl-job, admin JobRegistryMonitorHelper-registryOrRemoveThreadPool-" + r.hashCode());
                    }
                }, new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                r.run();
                logger.warn(">>>>>>>>>>> xxl-job, registry or remove too fast, match threadpool rejected handler(run now).");
            }
        });

        registryMonitorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!toStop) {
                    try {
                        //auto registry group
                        List<XxlJobGroup> groupList = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().findByAddressType(0);
                        if (groupList != null && !groupList.isEmpty()) {

                            //remove dead address(admin/executor)
                            List<Integer> ids = XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().findDead(RegistryConfig.DEAD_TIMEOUT, new Date());
                            if (ids != null && !ids.isEmpty()) {
                                XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().removeDead(ids);
                            }

                            //fresh online address
                            HashMap<String, List<String>> appAddressMap = new HashMap<String, List<String>>();
                            List<XxlJobRegistry> list = XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().findAll(RegistryConfig.DEAD_TIMEOUT, new Date());
                            if (list != null) {
                                for (XxlJobRegistry item : list) {
                                    if (RegistryConfig.RegistType.EXECUTOR.name().equals(item.getRegistryGroup())) {
                                        String appname = item.getRegistryKey();
                                        List<String> registryList = appAddressMap.get(appname);
                                        if (registryList == null) {
                                            registryList = new ArrayList<String>();
                                        }

                                        if (!registryList.contains(item.getRegistryValue())) {
                                            registryList.add(item.getRegistryValue());
                                        }
                                        appAddressMap.put(appname, registryList);
                                    }
                                }
                            }
                            for (XxlJobGroup group : groupList) {
                                List<String> registryList = appAddressMap.get(group.getAppname());
                                String addressListStr = null;
                                if (registryList != null && !registryList.isEmpty()) {
                                    Collections.sort(registryList);
                                    addressListStr = String.join(",", registryList);
                                }
                                group.setAddressList(addressListStr);
                                group.setUpdateTime(new Date());
                                XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().update(group);
                            }
                        }
                    } catch (Exception e) {
                        if(!toStop){
                            logger.error(">>>>>>>>>>> xxl-job, job registry monitor thread error:{}", e);
                        }
                    }

                    try {
                        TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                    } catch (InterruptedException e) {
                        if(!toStop) {
                            logger.error(">>>>>>>>>>> xxl-job, job registry monitor thread error:{}", e);
                        }
                        e.printStackTrace();
                    }
                }
            }
        });
        registryMonitorThread.setDaemon(true);
        registryMonitorThread.setName("xxl-job, admin JobRegistryMonitorHelper-registryMonitorThread");
        registryMonitorThread.start();
    }

    public void toStop() {
        toStop = true;
        registryOrRemoveThreadPool.shutdownNow();
        registryMonitorThread.interrupt();
        try {
            registryMonitorThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public ReturnT<String> registry(RegistryParam registryParam) {
        //valid
        if(!StringUtils.hasText(registryParam.getRegistryGroup()) || !StringUtils.hasText(registryParam.getRegistryKey()) || !StringUtils.hasText(registryParam.getRegistryValue())) {
            return new ReturnT<>(ReturnT.FAIL_CODE,"Illegal Argument.");
        }

        registryOrRemoveThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                int ret = XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().registryUpdate(registryParam.getRegistryGroup(),registryParam.getRegistryKey(),registryParam.getRegistryValue(),new Date());
                if(ret < 1) {
                    XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().registrySave(registryParam.getRegistryGroup(),registryParam.getRegistryKey(),registryParam.getRegistryValue(),new Date());
                    // fresh
                    freshGroupRegistryInfo(registryParam);
                }
            }
        });
        return ReturnT.SUCCESS;
    }

    public ReturnT<String> registryRemove(RegistryParam registryParam) {

        // valid
        if (!StringUtils.hasText(registryParam.getRegistryGroup())
                || !StringUtils.hasText(registryParam.getRegistryKey())
                || !StringUtils.hasText(registryParam.getRegistryValue())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "Illegal Argument.");
        }

        // async execute
        registryOrRemoveThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                int ret = XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().registryDelete(registryParam.getRegistryGroup(), registryParam.getRegistryKey(), registryParam.getRegistryValue());
                if (ret > 0) {
                    // fresh
                    freshGroupRegistryInfo(registryParam);
                }
            }
        });

        return ReturnT.SUCCESS;
    }

    private void freshGroupRegistryInfo(RegistryParam registryParam){
        // Under consideration, prevent affecting core tables
    }


}
