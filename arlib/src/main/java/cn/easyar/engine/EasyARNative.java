package cn.easyar.engine;
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//


import android.content.Context;

class EasyARNative {
    public static Context AppContext;

    EasyARNative() {
    }

    public static native void onResume();

    public static native void onPause();

    public static native void nativeCameraFrame(int var0, byte[] var1, int var2);

    public static native boolean nativeInit(Context var0, String var1);

    public static native void onRotation(int var0);

    public static native void onVideoStateChanged(int var0, int var1, int var2);

    public static native int nativePlayerInitGL(int var0, int var1);

    public static native void nativePlayerUpdate(int var0, float[] var1);

    static {
        System.loadLibrary("EasyAR");
    }
}
