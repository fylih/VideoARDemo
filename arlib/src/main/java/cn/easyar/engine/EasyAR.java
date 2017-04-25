package cn.easyar.engine;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.os.Handler;
import android.os.Build.VERSION;
import android.view.OrientationEventListener;
import android.view.WindowManager;


public class EasyAR {
    static boolean initialized = false;
    static boolean initializeOK = false;
    static WindowManager windowManager;
    static OrientationEventListener orientationEventListener;
    static BroadcastReceiver configChangeReceiver;
    static int rotation_pre = -1;
    static boolean orientationEventListener_alwaysrun = false;
    static int default_portrait_count = 0;
    static int default_landscape_count = 0;
    static int default_orientation = 0;
    static boolean handle_rotate_external = false;
    static DisplayListener displayListener;

    public EasyAR() {
    }

    static boolean detectDefaultOrientation(int rotation) {
        if(default_portrait_count + default_landscape_count >= 50) {
            return false;
        } else {
            Point point = new Point();
            windowManager.getDefaultDisplay().getSize(point);
            int width = point.x;
            int height = point.y;
            if((rotation != 0 && rotation != 2 || height <= width) && (rotation != 1 && rotation != 3 || width <= height)) {
                ++default_landscape_count;
            } else {
                ++default_portrait_count;
            }

            if(default_portrait_count > default_landscape_count && default_orientation != 1) {
                default_orientation = 1;
                detectRotation(rotation);
            } else if(default_portrait_count <= default_landscape_count && default_orientation != 2) {
                default_orientation = 2;
                detectRotation(rotation);
            }

            return true;
        }
    }

    static void detectRotation(int rotation) {
        if(!handle_rotate_external) {
            short orientation;
            if(default_orientation == 1) {
                switch(rotation) {
                    case 0:
                        orientation = 270;
                        break;
                    case 1:
                        orientation = 0;
                        break;
                    case 2:
                        orientation = 90;
                        break;
                    case 3:
                        orientation = 180;
                        break;
                    default:
                        orientation = 270;
                }
            } else {
                switch(rotation) {
                    case 0:
                        orientation = 0;
                        break;
                    case 1:
                        orientation = 270;
                        break;
                    case 2:
                        orientation = 180;
                        break;
                    case 3:
                        orientation = 90;
                        break;
                    default:
                        orientation = 0;
                }
            }

            EasyARNative.onRotation(orientation);
        }
    }

    public static void onResume() {
        EasyARNative.onResume();
        if(orientationEventListener != null) {
            orientationEventListener.enable();
        }

    }

    public static void onPause() {
        EasyARNative.onPause();
        if(orientationEventListener != null) {
            orientationEventListener.disable();
        }

    }

    public static void setInternalRotateHandling(boolean enable) {
        handle_rotate_external = !enable;
    }

    @TargetApi(17)
    public static boolean initialize(final Activity activity, String key) {
        if(initialized) {
            if(!initializeOK) {
                initializeOK = EasyARNative.nativeInit(activity.getApplicationContext(), key);
            }

            return initializeOK;
        } else {
            windowManager = activity.getWindowManager();
            EasyARNative.AppContext = activity.getApplicationContext();
            initializeOK = EasyARNative.nativeInit(activity.getApplicationContext(), key);
            int rotation = windowManager.getDefaultDisplay().getRotation();
            detectDefaultOrientation(rotation);
            detectRotation(rotation);
            if(VERSION.SDK_INT >= 17) {
                displayListener = new DisplayListener() {
                    public void onDisplayAdded(int displayId) {
                    }

                    public void onDisplayRemoved(int displayId) {
                    }

                    public void onDisplayChanged(int displayId) {
                        EasyAR.detectRotation(EasyAR.windowManager.getDefaultDisplay().getRotation());
                    }
                };
                DisplayManager intentFilter = (DisplayManager)activity.getSystemService("display");
                intentFilter.registerDisplayListener(displayListener, new Handler(activity.getMainLooper()));
            } else {
                configChangeReceiver = new BroadcastReceiver() {
                    public void onReceive(Context context, Intent intent) {
                        EasyAR.detectRotation(EasyAR.windowManager.getDefaultDisplay().getRotation());
                    }
                };
                orientationEventListener_alwaysrun = true;
                IntentFilter intentFilter1 = new IntentFilter();
                intentFilter1.addAction("android.intent.action.CONFIGURATION_CHANGED");
                activity.getApplicationContext().registerReceiver(configChangeReceiver, intentFilter1, "android.permission.CHANGE_CONFIGURATION", new Handler(activity.getMainLooper()));
            }

            orientationEventListener = new OrientationEventListener(activity) {
                public void onOrientationChanged(int orientation) {
                    if(orientation != -1) {
                        int rotation = EasyAR.windowManager.getDefaultDisplay().getRotation();
                        if(!EasyAR.detectDefaultOrientation(rotation) && !EasyAR.orientationEventListener_alwaysrun) {
                            this.disable();
                            EasyAR.orientationEventListener = null;
                        } else if(rotation != EasyAR.rotation_pre) {
                            EasyAR.rotation_pre = rotation;
                            EasyAR.detectRotation(rotation);
                        }
                    }
                }
            };
            if(orientationEventListener.canDetectOrientation()) {
                orientationEventListener.enable();
            }

            initialized = true;
            return initializeOK;
        }
    }
}
