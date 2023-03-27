package com.demo.job.core.biz;

import com.demo.job.core.biz.model.ReturnT;

public interface AdminBiz {

    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList);
}
