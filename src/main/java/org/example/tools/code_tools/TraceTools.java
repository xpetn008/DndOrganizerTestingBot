package org.example.tools.code_tools;

public class TraceTools {
    public static boolean actualMessageIsMenu(){
        boolean actualMessageIsMenu = false;
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTraceElements) {
            String methodName = element.getMethodName();
            if (methodName.equals("masterMenu") || methodName.equals("playerMenu") || methodName.equals("mainMenu") || methodName.equals("showMenu")) {
                actualMessageIsMenu = true;
                break;
            }
        }
        return actualMessageIsMenu;
    }
    public static boolean traceContainsMethod (String methodName) {
        boolean traceContainsMethod = false;
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTraceElements) {
            if (methodName.equals(element.getMethodName())) {
                traceContainsMethod = true;
                break;
            }
        }
        return traceContainsMethod;
    }
}
