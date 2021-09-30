package com.xul.core.function;


/**
 * 执行业务方法，没有返回值
 *
 * @author: xl
 * @date: 2021/6/29
 **/
@FunctionalInterface
public interface CacheFunctionWithParamReturn<T,R> {


    /**
     * 执行方法
     *
     * @param
     * @author: xl
     * @date: 2021/6/29
     **/
    T invokeMethod(R param);


}
