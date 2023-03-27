package com.demo.job.admin.dao;


import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

/**
 * job log
 * @author xuxueli 2019-11-22
 */
@Mapper
public interface XxlJobLogReportDao {

	public int save(XxlJobLogReport xxlJobLogReport);

	public int update(XxlJobLogReport xxlJobLogReport);

	public List<XxlJobLogReport> queryLogReport(@Param("triggerDayFrom") Date triggerDayFrom,
												@Param("triggerDayTo") Date triggerDayTo);

	public XxlJobLogReport queryLogReportTotal();

}
