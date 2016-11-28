package com.jack.jmediacontroler;

import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.jack.jmediacontroler.view.JMediaControl;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {
    private AnimationDrawable               drawable;
    private ImageView                       mediaContr;
    private SurfaceView                     mSurfaceView;
    private MediaPlayer                     mPlayer;
    private JMediaControl                   jMediaControl;
    private MediaPlayer.OnPreparedListener  mPreParedListener;
    private JMediaControl.ControlOper       mPlayerControl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        initResource();

    }

    private void initResource(){
        ImageView preview = (ImageView)findViewById(R.id.predis);
        drawable = (AnimationDrawable)preview.getDrawable();
        mediaContr = (ImageView)findViewById(R.id.control);
        mediaContr.setVisibility(View.GONE);
        preview.setVisibility(View.GONE);
        mSurfaceView = (SurfaceView)findViewById(R.id.surfaceview);
        SurfaceHolder mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(mCallback);
        mPlayer = new MediaPlayer();
        jMediaControl = new JMediaControl(this);

        registerListener();
        try {
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.setDataSource(this, Uri.parse("http://202.98.156.18/vhotwsh.video.qq.com/flv/224/225/c0022un5hcw.mp4?vkey=248C33CF9D1E7DFB3E2B5747AA4F5474C57EBA4E72748976BC9C3FBC8B32390AF2F4BA4DC5D5DD87ADF0B30D476C66AE15BA9B1022B2A4BA6721224C56EC55BDF7F8572EBEA5413398704836DE2599DB2AEB8B4D1C8AE3B0&br=60&platform=2&fmt=auto&level=0&sdtfrom=v5010&guid=321243d1ef41a3868788e16df4913dca&wshc_tag=2&wsiphost=ipdbm"));
            mPlayer.setOnPreparedListener(mPreParedListener);
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
                jMediaControl.setmPlayerCtr(mPlayerControl);
                jMediaControl.setAnchorView((FrameLayout)findViewById(R.id.surfacecontainer));
                mPlayer.start();
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
                return 0;
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
}
