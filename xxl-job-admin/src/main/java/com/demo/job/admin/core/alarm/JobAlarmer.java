package com.demo.job.admin.core.alarm;

import com.demo.job.admin.core.model.XxlJobInfo;
import com.demo.job.admin.core.model.XxlJobLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JobAlarmer implements ApplicationContextAware, InitializingBean {
    private static Logger logger = LoggerFactory.getLogger(JobAlarmer.class);
    private ApplicationContext applicationContext;
    private List<JobAlarm> jobAlarmList;

    @Override
    public void afterPropertiesSet() throws Exception {
        jobAlarmList = (List<JobAlarm>) applicationContext.getBeansOfType(JobAlarm.class).values();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public boolean alarm(XxlJobInfo info, XxlJobLog xxlJobLog) {
        boolean result = false;
        if(jobAlarmList!= null && jobAlarmList.size()!=0) {
            result = true;
            for(JobAlarm jobAlarm : jobAlarmList) {
                boolean resultItem = true;
                resultItem = jobAlarm.doAlarm(info,xxlJobLog);
                if (!resultItem) {
                    result = false;
                }
            }
        }
        return result;
    }
}
