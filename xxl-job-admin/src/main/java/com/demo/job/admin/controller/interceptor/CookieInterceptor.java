package com.demo.job.admin.controller.interceptor;

import com.demo.job.admin.core.util.FtlUtil;
import com.demo.job.admin.core.util.I18nUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

@Component
public class CookieInterceptor implements AsyncHandlerInterceptor {
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        if(modelAndView != null && request.getCookies()!=null && request.getCookies().length>0){
            HashMap<String, Cookie> cookieMap = new HashMap<>();
            for (Cookie cookie : request.getCookies()) {
                cookieMap.put(cookie.getName(),cookie);
            }
            modelAndView.addObject("cookieMap",cookieMap);
        }

        if (modelAndView != null) {
            modelAndView.addObject("I18nUtil", FtlUtil.generateStaticModel(I18nUtil.class.getName()));
        }
    }

}
