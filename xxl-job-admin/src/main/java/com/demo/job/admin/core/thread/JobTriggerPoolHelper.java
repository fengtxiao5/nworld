package com.demo.job.admin.core.thread;

import com.demo.job.admin.core.conf.XxlJobAdminConfig;
import com.demo.job.admin.core.trigger.TriggerTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class JobTriggerPoolHelper {

    private static Logger logger = LoggerFactory.getLogger(JobTriggerPoolHelper.class);

    private ThreadPoolExecutor fastTriggerPool = null;
    private ThreadPoolExecutor slowTriggerPool = null;

    public void start() {
        fastTriggerPool = new ThreadPoolExecutor(10,
                XxlJobAdminConfig.getAdminConfig().getTriggerPoolFastMax(),
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(1000), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "xxl-job, admin JobTriggerPoolHelper-fastTriggerPool-" + r.hashCode());
            }
        });

        slowTriggerPool = new ThreadPoolExecutor(10, XxlJobAdminConfig.getAdminConfig().getTriggerPoolSlowMax(), 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(2000), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "xxl-job, admin JobTriggerPoolHelper-slowTriggerPool-" + r.hashCode());
            }
        });
    }

    public void stop() {
        fastTriggerPool.shutdown();
        slowTriggerPool.shutdown();
        logger.info(">>>>>>>>> xxl-job trigger thread pool shutdown success.");
    }

    private static JobTriggerPoolHelper helper = new JobTriggerPoolHelper();

    public static void toStart() {
        helper.start();
    }
    public static void toStop() {
        helper.stop();
    }
    public static void trigger(int jobId, TriggerTypeEnum triggerType,int failRetryCount,String executorShardingParam,String executorParam,String addressList) {
        helper.addTrigger(jobId,triggerType,failRetryCount,executorShardingParam,executorParam,addressList);
    }

    private volatile long minTime = System.currentTimeMillis() / 60000; // ms -> min //todo
    private volatile ConcurrentMap<Integer, AtomicInteger> jobTimeoutCountMap = new ConcurrentHashMap<>();

    private void addTrigger(final int jobId, // final -> can't change in method
                            final TriggerTypeEnum triggerType,
                            final int failRetryCount,
                            final String executorShardingParam,
                            final String executorParam,
                            final String addressList) {
        ThreadPoolExecutor triggerPool_ = fastTriggerPool;
        AtomicInteger jobTimeoutCount = jobTimeoutCountMap.get(jobId);
        if (jobTimeoutCount != null && jobTimeoutCount.get() > 10) {
            triggerPool_ = slowTriggerPool;
        }
        triggerPool_.execute(new Runnable() {
            @Override
            public void run() {
                long beginTime = System.currentTimeMillis();
                try {
                    XxlJobTrigger.trigger(jobId, triggerType, failRetryCount, executorShardingParam, executorParam, addressList);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                } finally {
                    //check Timeout-count-map
                    long minTim_now = System.currentTimeMillis() / 60000;
                    if (minTime != minTim_now) {
                        minTime = minTim_now;
                        jobTimeoutCountMap.clear();
                    }

                    long costTime = System.currentTimeMillis() - beginTime;
                    if (costTime > 500) {
                        AtomicInteger count = jobTimeoutCountMap.putIfAbsent(jobId, new AtomicInteger(1));
                        if (count != null) {
                            count.incrementAndGet();
                        }
                    }
                }
            }
        });
    }
}
