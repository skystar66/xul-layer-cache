
package com.xul.core.supports;


import java.io.Serializable;

/**
 * 空值标识
 *
 * @author: xl
 * @date: 2021/9/28
 **/
public final class NullValue implements Serializable {

    public static final Object INSTANCE = new NullValue();

    private static final long serialVersionUID = 1L;

}
