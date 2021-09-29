package com.xul.core.logger;

public class LoggerHelper {


    /**默认为true*/
    private static boolean debugEnabled = true;

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    public static void setDebugEnabled(boolean enableDebug) {
        debugEnabled = enableDebug;
    }

}
