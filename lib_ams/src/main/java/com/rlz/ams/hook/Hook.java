package com.rlz.ams.hook;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;


public class Hook {


    private static Class<?> sLoginActivityClazz;

    private static final List<String> sRequireLoginNames = new ArrayList<>();

    public static final String TAG = "Hook_Login";

    public static final String UTILS_PATH = "com.hook.ams.apt.LoginUtils";

    public static final String HOOK_AMS_EXTRA_NAME = "targetActivity";

    /**
     * 不可再Application的onCreate()中直接调用
     */
    @SuppressLint("all")
    public static void hookAms(Context context) {
        try {
            Field singletonField;
            Class<?> iActivityManagerClass;

            // 1，获取Instrumentation中调用startActivity(,intent,)方法的对象
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                // 10.0以上是ActivityTaskManager中的IActivityTaskManagerSingleton
                Class<?> activityTaskManagerClass = Class.forName("android.app.ActivityTaskManager");
                singletonField = activityTaskManagerClass.getDeclaredField("IActivityTaskManagerSingleton");
                iActivityManagerClass = Class.forName("android.app.IActivityTaskManager");
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                // 8.0,9.0在ActivityManager类中IActivityManagerSingleton
                Class<?> activityManagerClass = ActivityManager.class;
                singletonField = activityManagerClass.getDeclaredField("IActivityManagerSingleton");
                iActivityManagerClass = Class.forName("android.app.IActivityManager");
            } else {

                // 8.0以下在ActivityManagerNative类中 gDefault
                Class<?> activityManagerNative = Class.forName("android.app.ActivityManagerNative");
                singletonField = activityManagerNative.getDeclaredField("gDefault");
                iActivityManagerClass = Class.forName("android.app.IActivityManager");
            }

            Log.d(TAG,"hookAms");
            singletonField.setAccessible(true);
            Object singleton = singletonField.get(null);

            // 2，获取Singleton中的mInstance，也就是要代理的对象
            Class<?> singletonClass = Class.forName("android.util.Singleton");
            Field mInstanceField = singletonClass.getDeclaredField("mInstance");
            mInstanceField.setAccessible(true);


            /* Object mInstance = mInstanceField.get(singleton); */
            Method getMethod = singletonClass.getDeclaredMethod("get");
            Object mInstance = getMethod.invoke(singleton);
            if (mInstance == null) {
                return;
            }


            // 3，对IActivityManager进行动态代理
            Object proxyInstance = Proxy.newProxyInstance(context.getClassLoader(), new Class[]{iActivityManagerClass},
                    (proxy, method, args) -> {
                        if (method.getName().equals("startActivity")) {
                            if (!isLogin()) {
                                int pos = 0;
                                for (int i = 0; i < args.length; i++) {
                                    if (args[i] instanceof Intent) {
                                        pos = i;
                                        break;
                                    }
                                }

                                assert args[pos] instanceof Intent;

                                Intent originIntent = (Intent) args[pos];
                                if (originIntent.getComponent() != null) {
                                    String activityName = originIntent.getComponent().getClassName();

                                    if (isRequireLogin(activityName)) {
                                        if (getLoginActivity() != null) {
                                            Intent intent = new Intent(context, getLoginActivity());
                                            intent.putExtra(HOOK_AMS_EXTRA_NAME, originIntent);
                                            args[pos] = intent;
                                        }
                                    }
                                }
                            }
                        }
                        return method.invoke(mInstance, args);
                    });


            // 4，把代理赋值给IActivityManager类型的mInstance对象
            mInstanceField.set(singleton, proxyInstance);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 该activity是否需要登录
     */
    @SuppressLint("all")
    private static boolean isRequireLogin(String activityName) {
        if (sRequireLoginNames.size() == 0) {
            // 反射调用apt生成的方法
            try {
                Class<?> NeedLoginClazz = Class.forName(UTILS_PATH);
                Method m = NeedLoginClazz.getDeclaredMethod("getRequireLoginList");
                m.setAccessible(true);
                sRequireLoginNames.addAll((List<String>) m.invoke(null));
                Log.d(TAG, "size" + sRequireLoginNames.size());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sRequireLoginNames.contains(activityName);
    }

    /**
     * 获取登录activity
     */
    private static Class<?> getLoginActivity() {
        if (sLoginActivityClazz == null) {
            try {
                Class<?> NeedLoginClazz = Class.forName(UTILS_PATH);
                Method m = NeedLoginClazz.getDeclaredMethod("getLoginActivity");
                m.setAccessible(true);
                String loginActivity = (String) m.invoke(null);
                if (loginActivity != null)
                    sLoginActivityClazz = Class.forName(loginActivity);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sLoginActivityClazz;
    }

    /**
     * 是否已经登录
     */
    @SuppressLint("all")
    private static boolean isLogin() {
        try {
            Class<?> NeedLoginClazz = Class.forName(UTILS_PATH);
            Method m = NeedLoginClazz.getDeclaredMethod("getJudgeLoginMethod");
            m.setAccessible(true);
            String methodPath = (String) m.invoke(null);
            if (methodPath != null) {
                String[] split = methodPath.split("#");
                if (split.length == 2) {
                    String methodPkg = split[0];
                    String methodName = split[1];
                    Class<?> methodInClazz = Class.forName(methodPkg);
                    Method methodNameMethod = methodInClazz.getDeclaredMethod(methodName);
                    methodNameMethod.setAccessible(true);
                    Object invoke = methodNameMethod.invoke(null);
                    if (invoke != null) {
                        return (boolean) invoke;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
