package com.jack.jmediacontroler;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.jack.jmediacontroler.utils.DensityUtil;
import com.jack.jmediacontroler.view.JMediaControl;

import java.io.IOException;

public class MainActivity extends Activity {
    private static final String                   TAG = "jackzhous -- MainActivity";
    private SurfaceView                           mSurfaceView;
    private MediaPlayer                           mPlayer;
    private JMediaControl                         jMediaControl;
    private MediaPlayer.OnPreparedListener        mPreParedListener;
    private JMediaControl.ControlOper             mPlayerControl;
    private FrameLayout                           mSurfaceContainer;
    private MediaPlayer.OnBufferingUpdateListener mBufferListener;
    private MediaPlayer.OnCompletionListener      mOnCompletionListener;
    private int                                   mBufferLength;
    private boolean                               IsComplete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initResource();
    }

    private void initResource(){
        mSurfaceContainer = (FrameLayout)findViewById(R.id.surfacecontainer);
        mSurfaceView = (SurfaceView)findViewById(R.id.surfaceview);
        SurfaceHolder mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(mCallback);
        mPlayer = new MediaPlayer();
        jMediaControl = new JMediaControl(this);

        registerListener();
        jMediaControl.setmPlayerCtr(mPlayerControl);
        jMediaControl.setAnchorView(mSurfaceContainer);
        jMediaControl.preLoadingAnimation();
                                                             //开始加载动画
        try {
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.setDataSource(this, Uri.parse("http://220.165.142.145/vhotwsh.video.qq.com/flv/71/210/d00228v6kza.p412.1.mp4?sdtfrom=v1000&type=mp4&vkey=72B219170307F693D81B60CF24F816F2B84A6438C681952AC3658CB045BBA09CEA2DEF492E1EF2E8E92326738726D11CBAFFF55E308977527E80705B1D949B861B5A4D2724E7E36CB6A39FCB44BF6344DC6D4CE6198747F20440712DD87CA927F71D6AE9B3781E2E4EB2DEC337CC87DF&level=0&platform=11&br=85&fmt=hd&sp=0&guid=FE16D50EED1A35AF9D11263EC8661E33&wshc_tag=2&wsiphost=ipdbm"));
            mPlayer.setOnPreparedListener(mPreParedListener);
            mPlayer.setOnBufferingUpdateListener(mBufferListener);
            mPlayer.setOnCompletionListener(mOnCompletionListener);
            mPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private SurfaceHolder.Callback mCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            mPlayer.setDisplay(surfaceHolder);

        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        }
    };

    private void registerListener(){
        mPreParedListener = new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                jMediaControl.stopLoadingAnimation();
                jMediaControl.bindingContrlView();
                mPlayer.start();
                mPlayer.pause();
            }
        };

        mBufferListener = new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
                mBufferLength = i;
            }
        };

        mOnCompletionListener = new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                IsComplete = true;
                jMediaControl.show();
            }
        };

        mPlayerControl = new JMediaControl.ControlOper() {
            @Override
            public void start() {
                mPlayer.start();
            }

            @Override
            public void pause() {
                mPlayer.pause();
            }

            @Override
            public int getDuration() {
                return mPlayer.getDuration();
            }

            @Override
            public int getCurPosition() {
                return mPlayer.getCurrentPosition();
            }

            @Override
            public void seekTo(int pos) {
                mPlayer.seekTo(pos);
            }

            @Override
            public boolean isPlaying() {
                return mPlayer.isPlaying();
            }

            @Override
            public int getBufPercent() {
                return mBufferLength;
            }

            @Override
            public boolean canPause() {
                return true;
            }

            @Override
            public boolean canSeekBackward() {
                return true;
            }

            @Override
            public boolean canSeekForward() {
                return true;
            }

            @Override
            public boolean isFullScreen() {
                return true;
            }

            @Override
            public void fullScreen() {
                if(getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }else{
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
            }

            @Override
            public boolean isComplete() {
                return IsComplete;
            }
        };


    }

    private void releaseMediaPlayer(){
        jMediaControl.removeHandlerCallback();
        if(mPlayer != null){
            mPlayer.stop();
            mPlayer.release();
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        jMediaControl.show();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPlayer.pause();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(mPlayer == null){
            return;
        }
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().invalidate();
            float height = DensityUtil.getWidthInPx(this);
            float width = DensityUtil.getHeightInPx(this);
            mSurfaceContainer.getLayoutParams().height = (int)width;
            mSurfaceContainer.getLayoutParams().width = (int)height;
        }else{
            /**
             * 退出全屏，清楚full_screen标识，和请求重新布局no_limits
             */
            final WindowManager.LayoutParams attrs = getWindow().getAttributes();
            attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().setAttributes(attrs);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            float width = DensityUtil.getWidthInPx(this);
            float height = DensityUtil.dip2px(this, 200f);
            mSurfaceContainer.getLayoutParams().height = (int)height;
            mSurfaceContainer.getLayoutParams().width = (int)width;
        }
    }
}
