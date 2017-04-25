package cn.easyar.engine;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.view.Surface;

import java.io.IOException;

class VideoPlayer implements OnErrorListener, OnCompletionListener, OnPreparedListener {
    int STATE_ERROR = -1;
    int STATE_PREPARED = 0;
    int STATE_COMPLETED = 1;
    private Context mContext;
    private MediaPlayer mPlayer;
    private int mState;
    private VideoPlayer.VideoTexture mVideoTexture;
    private int nativeId;
    float audioVolume = 1.0F;
    boolean ready = false;
    boolean readyForUpdate = false;
    boolean readyForPause = false;

    public VideoPlayer(Context context, int id) {
        this.nativeId = id;
        this.mContext = context.getApplicationContext();
        this.mPlayer = new MediaPlayer();
        this.mPlayer.setOnErrorListener(this);
        this.mPlayer.setOnCompletionListener(this);
        this.mPlayer.setOnPreparedListener(this);
        this.mVideoTexture = new VideoPlayer.VideoTexture();
    }

    private void setState(int state) {
        int oldState = this.mState;
        this.mState = state;
        EasyARNative.onVideoStateChanged(this.nativeId, oldState, this.mState);
    }

    public void prepare(String url, int texture, boolean isAsset) {
        this.ready = false;

        try {
            if(isAsset) {
                AssetFileDescriptor e = this.mContext.getAssets().openFd(url);
                this.mPlayer.setDataSource(e.getFileDescriptor(), e.getStartOffset(), e.getLength());
            } else {
                this.mPlayer.setDataSource(this.mContext, Uri.parse(url));
            }

            this.mVideoTexture.init(texture);
            this.mPlayer.prepareAsync();
        } catch (IOException var5) {
            this.setState(this.STATE_ERROR);
        }

    }

    public boolean start() {
        if(!this.ready) {
            return false;
        } else {
            this.mPlayer.start();
            this.readyForUpdate = true;
            this.readyForPause = true;
            return true;
        }
    }

    public void updateFrame() {
        if(this.readyForUpdate) {
            this.mVideoTexture.updateTargetTexture();
        }
    }

    public boolean pause() {
        if(this.ready && this.readyForPause) {
            this.mPlayer.pause();
            this.readyForUpdate = false;
            return true;
        } else {
            return false;
        }
    }

    public boolean stop() {
        if(this.ready && this.readyForPause) {
            this.mPlayer.pause();
            this.mPlayer.seekTo(0);
            this.readyForUpdate = false;
            return true;
        } else {
            return false;
        }
    }

    public int getDuration() {
        return !this.ready?0:this.mPlayer.getDuration();
    }

    public int getCurrentPosition() {
        return !this.ready?0:this.mPlayer.getCurrentPosition();
    }

    public boolean seekTo(int position) {
        if(!this.ready) {
            return false;
        } else {
            this.mPlayer.seekTo(position);
            return true;
        }
    }

    public int getVideoWidth() {
        return !this.ready?0:this.mPlayer.getVideoWidth();
    }

    public int getVideoHeight() {
        return !this.ready?0:this.mPlayer.getVideoHeight();
    }

    public float getVolume() {
        return this.audioVolume;
    }

    public boolean setVolume(float volume) {
        volume = Math.max(Math.min(volume, 1.0F), 0.0F);
        this.audioVolume = volume;
        this.mPlayer.setVolume(volume, volume);
        return true;
    }

    public void release() {
        if(this.mPlayer != null) {
            this.mPlayer.stop();
            this.mPlayer.release();
        }

        if(this.mVideoTexture != null) {
            this.mVideoTexture.release();
        }

    }

    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        this.setState(this.STATE_ERROR);
        return true;
    }

    public void onCompletion(MediaPlayer mediaPlayer) {
        this.mPlayer.seekTo(0);
        this.setState(this.STATE_COMPLETED);
    }

    public void onPrepared(MediaPlayer mediaPlayer) {
        this.mPlayer.setSurface(new Surface(this.mVideoTexture.getSurfaceTexture()));
        this.setVolume(this.audioVolume);
        this.ready = true;
        this.setState(this.STATE_PREPARED);
    }

    class VideoTexture implements OnFrameAvailableListener {
        private SurfaceTexture mSurfaceTexture;
        private Boolean mUpdateSurface = Boolean.valueOf(false);

        VideoTexture() {
        }

        public void init(int textureId) {
            int mediaTexture = EasyARNative.nativePlayerInitGL(VideoPlayer.this.nativeId, textureId);
            this.mSurfaceTexture = new SurfaceTexture(mediaTexture);
            this.mSurfaceTexture.setOnFrameAvailableListener(this);
        }

        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            Boolean var2 = this.mUpdateSurface;
            synchronized(this.mUpdateSurface) {
                this.mUpdateSurface = Boolean.valueOf(true);
            }
        }

        public SurfaceTexture getSurfaceTexture() {
            return this.mSurfaceTexture;
        }

        public void updateTargetTexture() {
            Boolean var1 = this.mUpdateSurface;
            synchronized(this.mUpdateSurface) {
                if(this.mUpdateSurface.booleanValue()) {
                    this.mSurfaceTexture.updateTexImage();
                    float[] mSTMatrix = new float[16];
                    this.mSurfaceTexture.getTransformMatrix(mSTMatrix);
                    EasyARNative.nativePlayerUpdate(VideoPlayer.this.nativeId, mSTMatrix);
                    this.mUpdateSurface = Boolean.valueOf(false);
                }

            }
        }

        public void release() {
            if(this.mSurfaceTexture != null) {
                this.mSurfaceTexture.release();
            }

        }
    }
}
