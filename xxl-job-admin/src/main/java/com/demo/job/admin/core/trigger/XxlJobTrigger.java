package com.demo.job.admin.core.trigger;


import com.demo.job.admin.core.conf.XxlJobAdminConfig;
import com.demo.job.admin.core.model.XxlJobGroup;
import com.demo.job.admin.core.model.XxlJobInfo;
import com.demo.job.admin.core.route.ExecutorRouteStrategyEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class XxlJobTrigger {

    private static Logger logger = LoggerFactory.getLogger(XxlJobTrigger.class);

    public static void trigger(int jobId,
                               TriggerTypeEnum triggerType,
                               int failRetryCount,
                               String executorShardingParam,
                               String executorParam,
                               String addressList) {
        XxlJobInfo jobInfo = XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao().loadById(jobId);
        if (jobInfo==null) {
            logger.warn(">>>>>>>> trigger fail,jobId invalid. jobId = {}", jobId);
            return;
        }
        if (executorParam!=null){
            jobInfo.setExecutorParam(executorParam);
        }

        int finalFailRetryCount = failRetryCount > 0? failRetryCount:jobInfo.getExecutorFailRetryCount();
        XxlJobGroup group = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().load(jobInfo.getJobGroup());
        if(addressList!=null&&addressList.trim().length()>0){
            group.setAddressType(1);
            group.setAddressList(addressList.trim());
        }
        int[] shardingParam = null;
        if(executorShardingParam!=null){
            String[] shardingArr=executorShardingParam.split("/");
            if(shardingArr.length ==2&&isNumeric(shardingArr[0])&&isNumeric(shardingArr[1])){
                shardingParam = new int[2];
                shardingParam[0] = Integer.valueOf(shardingArr[0]);
                shardingParam[1] = Integer.valueOf(shardingArr[1]);
            }
        }
        if(ExecutorRouteStrategyEnum.SHARDING_BROADCAST==ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(),null)){
            for (int i = 0; i < group.getRegistryList().size(); i++) {

            }
        }
    }

    private static boolean isNumeric(String param) {
        try {
            int result = Integer.valueOf(param);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
