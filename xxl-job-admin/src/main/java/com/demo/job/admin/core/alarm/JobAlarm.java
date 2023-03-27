package com.demo.job.admin.core.alarm;

import com.demo.job.admin.core.model.XxlJobInfo;
import com.demo.job.admin.core.model.XxlJobLog;

public interface JobAlarm {

    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog);
}
