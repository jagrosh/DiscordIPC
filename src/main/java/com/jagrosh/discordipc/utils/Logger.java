package com.jagrosh.discordipc.utils;

public class Logger {
    public static void debug(Object s) {
        System.out.println("[Debug] " + s);
    }
    public static void info(Object s) {
        System.out.println("[Info] " + s);
    }
    public static void warn(Object s) {
        System.out.println("[Warn] " + s);
    }
    public static void error(Object s) {
        System.out.println("[Error] " + s);
    }
}
