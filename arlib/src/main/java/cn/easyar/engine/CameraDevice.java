package cn.easyar.engine;


import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

class CameraDevice implements PreviewCallback {
    private int mCameraId;
    private int mCameraDeviceId = -1;
    private Camera mCamera;
    private SurfaceTexture mSurfaceTexture = new SurfaceTexture(0);
    private List<Size> mSupportedPreviewSizes;
    List<Integer> mSupportedPreviewFrameRates;
    private int mCameraRotation;
    private PreviewCallback mPreviewCallback;
    private int userMode = -1;
    private int previousMode = -1;
    private boolean mStarted = false;
    private int mCachedWidth = 1280;
    private int mCachedHeight = 720;
    private int mCachedFPS = 30;
    private int setWhenStopFocus = -1;
    private boolean setWhenStopFlash = false;
    AutoFocusCallback autoFacusCallback = new AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            if(CameraDevice.this.userMode >= 0) {
                CameraDevice.this.setFocusMode(CameraDevice.this.userMode);
            }

            CameraDevice.this.userMode = -1;
        }
    };

    CameraDevice() {
    }

    public void setPreviewCallback(PreviewCallback cb) {
        this.mPreviewCallback = cb;
    }

    public boolean open(int id) {
        boolean opened = false;

        try {
            this.stopPreviewAndRelease();
            if(id == 0) {
                this.mCameraId = 0;
                this.mCamera = Camera.open(0);
                opened = true;
            } else {
                CameraInfo e;
                int cameraCount;
                int camIdx;
                if(id == 1) {
                    e = new CameraInfo();
                    cameraCount = Camera.getNumberOfCameras();

                    for(camIdx = 0; camIdx < cameraCount; ++camIdx) {
                        Camera.getCameraInfo(camIdx, e);
                        if(e.facing == 0) {
                            this.mCameraId = camIdx;
                            this.mCamera = Camera.open(camIdx);
                            opened = true;
                        }
                    }
                } else if(id == 2) {
                    e = new CameraInfo();
                    cameraCount = Camera.getNumberOfCameras();

                    for(camIdx = 0; camIdx < cameraCount; ++camIdx) {
                        Camera.getCameraInfo(camIdx, e);
                        if(e.facing == 1) {
                            this.mCameraId = camIdx;
                            this.mCamera = Camera.open(camIdx);
                            opened = true;
                        }
                    }
                }
            }

            this.getPreviewSizes();
            this.getFrameRates();
            this.setPreviewCallback(this);
            this.setSize(this.mCachedWidth, this.mCachedHeight);
            this.setFrameRate((float)this.mCachedFPS);
        } catch (RuntimeException var6) {
            var6.printStackTrace();
        }

        if(opened) {
            this.mCameraDeviceId = id;
        }

        return opened;
    }

    public boolean ready() {
        return this.mCamera != null;
    }

    private void getPreviewSizes() {
        if(this.ready()) {
            Parameters params = this.mCamera.getParameters();
            this.mSupportedPreviewSizes = params.getSupportedPreviewSizes();
        }
    }

    private void getFrameRates() {
        if(this.ready()) {
            Parameters params = this.mCamera.getParameters();
            this.mSupportedPreviewFrameRates = params.getSupportedPreviewFrameRates();
        }
    }

    private Size getOptimalPreviewSize(int width, int height) {
        long area = (long)(width * height);
        Size res = (Size)this.mSupportedPreviewSizes.get(0);
        long minAreaDiff = 9223372036854775807L;
        Iterator var8 = this.mSupportedPreviewSizes.iterator();

        while(var8.hasNext()) {
            Size size = (Size)var8.next();
            long areaDiff = Math.abs((long)(size.width * size.height) - area);
            if(areaDiff < minAreaDiff) {
                minAreaDiff = areaDiff;
                res = size;
            }
        }

        return res;
    }

    private int getOptimalFrameRate(int fps) {
        int res = ((Integer)this.mSupportedPreviewFrameRates.get(0)).intValue();
        int minDiff = 2147483647;
        Iterator var4 = this.mSupportedPreviewFrameRates.iterator();

        while(var4.hasNext()) {
            int f = ((Integer)var4.next()).intValue();
            int diff = Math.abs(fps - f);
            if(diff < minDiff) {
                minDiff = diff;
                res = f;
            }
        }

        return res;
    }

    public boolean start() {
        if(!this.ready()) {
            return false;
        } else {
            try {
                this.mCamera.setPreviewTexture(this.mSurfaceTexture);
                if(this.mPreviewCallback != null) {
                    this.mCamera.setPreviewCallback(this.mPreviewCallback);
                }

                this.mCamera.startPreview();
            } catch (IOException var2) {
                var2.printStackTrace();
            }

            this.setCameraRotation();
            if(this.previousMode >= 0) {
                this.setFocusMode(this.previousMode);
            }

            if(this.setWhenStopFocus != -1) {
                this.setFocusMode(this.setWhenStopFocus);
                this.setWhenStopFocus = -1;
            }

            if(this.setWhenStopFlash) {
                this.setFlashTorchMode(this.setWhenStopFlash);
                this.setWhenStopFlash = false;
            }

            this.mStarted = true;
            return true;
        }
    }

    public boolean stop() {
        this.stopPreviewAndRelease();
        return true;
    }

    private void stopPreviewAndRelease() {
        if(this.ready()) {
            this.mStarted = false;
            this.mCamera.setPreviewCallback((PreviewCallback)null);
            this.mCamera.stopPreview();
            this.mCamera.release();
            this.mCamera = null;
        }
    }

    private void setCameraRotation() {
        if(this.ready()) {
            Parameters params = this.mCamera.getParameters();
            params.setRotation(this.mCameraRotation);
            this.mCamera.setParameters(params);
        }
    }

    public CameraInfo getCameraInfo() {
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(this.mCameraId, info);
        return info;
    }

    public int getCameraRotation() {
        return this.mCameraRotation;
    }

    public void onPreviewFrame(byte[] data, Camera camera) {
        EasyARNative.nativeCameraFrame(this.mCameraDeviceId, data, data.length);
    }

    public int getPixelFormat() {
        if(!this.ready()) {
            return 0;
        } else {
            Parameters params = this.mCamera.getParameters();
            int format = params.getPreviewFormat();
            switch(format) {
                case 17:
                    return 2;
                case 842094169:
                    return 3;
                default:
                    return 0;
            }
        }
    }

    public float getFrameRate() {
        if(!this.ready()) {
            return 0.0F;
        } else {
            Parameters params = this.mCamera.getParameters();
            return (float)params.getPreviewFrameRate();
        }
    }

    public int getNumSupportedFrameRate() {
        return !this.ready()?0:this.mSupportedPreviewFrameRates.size();
    }

    public float getSupportedFrameRate(int idx) {
        return this.ready() && idx < this.mSupportedPreviewFrameRates.size()?(float)((Integer)this.mSupportedPreviewFrameRates.get(idx)).intValue():0.0F;
    }

    public boolean setFrameRate(float fps) {
        if(!this.ready()) {
            return false;
        } else {
            this.mCachedFPS = this.getOptimalFrameRate((int)fps);
            Parameters params = this.mCamera.getParameters();
            params.setPreviewFrameRate(this.mCachedFPS);
            this.mCamera.setParameters(params);
            return true;
        }
    }

    public Size getSize() {
        if(!this.ready()) {
            return null;
        } else {
            Parameters params = this.mCamera.getParameters();
            return params.getPreviewSize();
        }
    }

    public int getNumSupportedSize() {
        return !this.ready()?0:this.mSupportedPreviewSizes.size();
    }

    public Size getSupportedSize(int idx) {
        return this.ready() && idx < this.mSupportedPreviewSizes.size()?(Size)this.mSupportedPreviewSizes.get(idx):null;
    }

    public boolean setSize(int width, int height) {
        if(!this.ready()) {
            return false;
        } else {
            Size size = this.getOptimalPreviewSize(width, height);
            this.mCachedWidth = size.width;
            this.mCachedHeight = size.height;
            if(this.mStarted) {
                this.mCamera.stopPreview();
            }

            Parameters params = this.mCamera.getParameters();
            params.setPreviewSize(this.mCachedWidth, this.mCachedHeight);
            this.mCamera.setParameters(params);
            if(this.mStarted) {
                this.mCamera.startPreview();
            }

            return true;
        }
    }

    public boolean setFlashTorchMode(boolean on) {
        if(!this.ready()) {
            this.setWhenStopFlash = on;
            return false;
        } else {
            Parameters params = this.mCamera.getParameters();
            if(on) {
                params.setFlashMode("torch");
            } else {
                params.setFlashMode("off");
            }

            this.mCamera.setParameters(params);
            return true;
        }
    }

    private boolean checkFocusModeSupport(int focusMode) {
        Parameters params = this.mCamera.getParameters();
        List modes = params.getSupportedFocusModes();
        switch(focusMode) {
            case 0:
                if(modes.contains("auto")) {
                    return true;
                }

                return false;
            case 1:
                if(modes.contains("auto")) {
                    return true;
                }

                return false;
            case 2:
                if(modes.contains("continuous-picture")) {
                    return true;
                } else {
                    if(modes.contains("continuous-video")) {
                        return true;
                    }

                    return false;
                }
            case 3:
                if(modes.contains("infinity")) {
                    return true;
                }

                return false;
            case 4:
                if(modes.contains("macro")) {
                    return true;
                }

                return false;
            default:
                return false;
        }
    }

    public boolean setFocusMode(int focusMode) {
        if(!this.ready()) {
            this.setWhenStopFocus = focusMode;
            return false;
        } else if(this.userMode != -1) {
            if(!this.checkFocusModeSupport(focusMode)) {
                return false;
            } else {
                this.userMode = focusMode;
                return true;
            }
        } else {
            Parameters params = this.mCamera.getParameters();
            List modes = params.getSupportedFocusModes();
            switch(focusMode) {
                case 0:
                    if(!modes.contains("auto")) {
                        return false;
                    }

                    params.setFocusMode("auto");
                    break;
                case 1:
                    if(!modes.contains("auto")) {
                        return false;
                    }

                    params.setFocusMode("auto");
                    this.mCamera.autoFocus(this.autoFacusCallback);
                    if(this.previousMode >= 0) {
                        this.userMode = this.previousMode;
                    } else {
                        this.userMode = -2;
                    }
                    break;
                case 2:
                    if(modes.contains("continuous-picture")) {
                        params.setFocusMode("continuous-picture");
                    } else {
                        if(!modes.contains("continuous-video")) {
                            return false;
                        }

                        params.setFocusMode("continuous-video");
                    }
                    break;
                case 3:
                    if(!modes.contains("infinity")) {
                        return false;
                    }

                    params.setFocusMode("infinity");
                    break;
                case 4:
                    if(!modes.contains("macro")) {
                        return false;
                    }

                    params.setFocusMode("macro");
                    break;
                default:
                    return false;
            }

            if(focusMode != 1) {
                this.previousMode = focusMode;
            }

            this.mCamera.setParameters(params);
            return true;
        }
    }

    public class FocusMode {
        public static final int AutoTrigged = -2;
        public static final int None = -1;
        public static final int Normal = 0;
        public static final int Triggerauto = 1;
        public static final int Continousauto = 2;
        public static final int Infinity = 3;
        public static final int Macro = 4;

        public FocusMode() {
        }
    }

    public class Device {
        public static final int Default = 0;
        public static final int Back = 1;
        public static final int Front = 2;

        public Device() {
        }
    }
}
