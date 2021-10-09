
package com.xul.core.redis.serializer;



public class SerializationException extends RuntimeException {

    /**
     * Constructs a new <code>SerializationException</code> instance.
     *
     * @param msg   msg
     * @param cause 原因
     */
    public SerializationException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * Constructs a new <code>SerializationException</code> instance.
     *
     * @param msg msg
     */
    public SerializationException(String msg) {
        super(msg);
    }
}
