package com.ams.hook;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by RLZ
 * on 2021/12/3
 */
public class SPUtil {

    private static final String SHARED_NAME = "user_sp";

    private static final SharedPreferences sSp;

    static {
        sSp = App.mContext.getSharedPreferences(SHARED_NAME, Context.MODE_PRIVATE);
    }

    public static void save(String nickName) {
        sSp.edit().putString("account", nickName).apply();
    }


    public static String get() {
        return sSp.getString("account", "");
    }

    public static void clear() {
        sSp.edit().clear().apply();
    }
}
