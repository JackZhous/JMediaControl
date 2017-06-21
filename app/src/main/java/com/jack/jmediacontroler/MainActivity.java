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

import com.jackzhous.label.utils.DensityUtil;
import com.jackzhous.label.view.JMediaControl;

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
        jMediaControl.startLoadingAnimation();
                                                             //开始加载动画
        try {
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.setDataSource(this, Uri.parse("http://183.222.103.8/cache/117.157.18.19/sports.tc.qq.com/s0024dqu1ip.p412.1.mp4?sdtfrom=v1010&guid=6b2b00332a10d6b4f24d1af42348cf69&vkey=B302EA1FE12BAEDC50AA5C123D7ACBD2F2236A20E4163D1923CC7AFD8C597D6FA2D9B6D3E63779EBC1F3E0B4027BD6804594FB126249DE6E90A20FC4DD1F5EE7428974BEE40D029345229FA149C201FDE600A2F7A38277E41DEDF9B56A1E39ABBB434DD42C05207F7731A50BDE9DBEF0&ich_args2=314-21101702062387_fff60d428ec0d5d8e8f530cb4913e813_10004421_9c886d24d7c2f0d59432518939a83798_6bc7f327a4070a18170bf4d8ffb13cb8"));
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
                jMediaControl.show();
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
                IsComplete = false;
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
        jMediaControl.recycleSelf();
        if(mPlayer != null){
            mPlayer.stop();
            mPlayer.release();
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        //jMediaControl.show();
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
