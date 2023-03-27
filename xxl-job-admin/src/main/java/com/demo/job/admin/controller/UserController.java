package com.demo.job.admin.controller;


import com.demo.job.admin.dao.XxlJobGroupDao;
import com.demo.job.admin.dao.XxlJobUserDao;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;

@Controller
@RequestMapping("/user")
public class UserController {

    @Resource
    private XxlJobUserDao xxlJobUserDao;

    @Resource
    private XxlJobGroupDao xxlJobGroupDao;


}
