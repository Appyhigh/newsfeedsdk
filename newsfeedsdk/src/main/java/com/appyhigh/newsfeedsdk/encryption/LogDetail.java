package com.appyhigh.newsfeedsdk.encryption;


import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.appyhigh.newsfeedsdk.BuildConfig;


public class LogDetail {

    private static boolean debugger = false;

    public static void LogD(String T, String S) {
        if (debugger) {
            if (S != null) {
                Log.d(T, S);
            }
        }
    }
    public static void LogDE(String T, String S) {
        if (debugger) {
            if (S != null) {
                Log.e(T, S);
            }
        }
    }

    public static void ToastD(Context T, String S) {
        if (debugger) {
            Toast.makeText(T, S, Toast.LENGTH_SHORT).show();
        }
    }

    // Release mode code
    public static void LogR(String T, String S) {
        if (S != null) {
            Log.d(T, S);
        }
    }

    public static void ToastR(Context T, String S) {
        Toast.makeText(T, S, Toast.LENGTH_SHORT).show();
    }

    public static void println(int ss) {
        println(String.valueOf(ss));
    }

    public static void println(String ss) {
        System.out.println(ss);

        /*if (debugger) {
            System.out.println(ss);
        }*/
    }
}
