/*
 *         Copyright (C) 2016-2017 宙斯
 *         All rights reserved
 *
 *        filename :Class4
 *        description :
 *
 *         created by jackzhous at  11/07/2016 12:12:12
 *         http://blog.csdn.net/jackzhouyu
 */

package com.jack.jmediacontroler.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.jack.jmediacontroler.R;

import java.lang.ref.WeakReference;
import java.util.Formatter;
import java.util.Locale;

/***********
 * author: jackzhous
 * file: JMediaControl.java
 * create date: 2016/11/25 17:00
 * desc:
 ************/
public class JMediaControl extends FrameLayout {

    private static final String     TAG = "JMediaControl";
    private static final int        FADE_OUT = 1;
    private static final int        SHOW_PROGRESS = 2;
    private static final int        DEFAULTTIMEOUT = 3000;

    private Handler                 mHandler = new MyHandler(this);
    private View                    mRootView;
    private Activity                mContext;
    private ControlOper             mPlayerCtr;
    private ViewGroup               mAnchorVGroup;
    private ImageButton             mBtnFullscreen;
    private ImageButton             mBtnPause;
    private ImageButton             mBtnPause1;
    private TextView                mCurTime;
    private TextView                mEndTime;
    private ProgressBar             mProgress;
    private boolean                 mIsShowing;
    private boolean                 mIsDragging;
    private boolean                 mFromXml;
    private boolean                 mUseFastForward;
    private boolean                 mIsPause;
    private StringBuilder           mStrBuilder;
    private Formatter               mFormatter;

    public JMediaControl(Activity context) {
        this(context, true);
    }

    public JMediaControl(Activity context, AttributeSet attrs) {
        super(context, attrs);

        mRootView = null;
        mContext = context;
        mUseFastForward = true;
        mFromXml = true;
    }

    public JMediaControl(Activity context, boolean useFastForward) {
        super(context);
        mContext = context;
        mUseFastForward = useFastForward;
    }

    public void removeHandlerCallback(){
        if(null != mHandler){
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
    }

    public void setmPlayerCtr(ControlOper mPlayerCtr) {
        this.mPlayerCtr = mPlayerCtr;
    }

    /**
     * 把视频控制栏粘合到视频布局上去
     * @param viewGroup
     */
    public void setAnchorView(ViewGroup viewGroup){
        mAnchorVGroup = viewGroup;
        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        removeAllViews();

        View view = createCtrlView();
        addView(view, frameParams);
    }

    /**
     * 创建媒体蓝控制信息
     * @return
     */
    private View createCtrlView(){
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mRootView = inflater.inflate(R.layout.controller, null);

        initCtrView(mRootView);

        return mRootView;
    }

    /**
     * 初始化控制栏
     * @param view
     */
    private void initCtrView(View view){
        mBtnFullscreen = (ImageButton) view.findViewById(R.id.fullscreen);
        if (mBtnFullscreen != null) {
            mBtnFullscreen.requestFocus();
            mBtnFullscreen.setOnClickListener(mFullscreenListener);
        }

        mProgress = (ProgressBar) view.findViewById(R.id.mediacontroller_progress);
        if (mProgress != null) {
            if (mProgress instanceof SeekBar) {
                SeekBar seeker = (SeekBar) mProgress;
                seeker.setOnSeekBarChangeListener(mSeekListener);
            }
            mProgress.setMax(1000);
        }

        mBtnPause = (ImageButton)view.findViewById(R.id.pause);
        mBtnPause1 = (ImageButton)view.findViewById(R.id.btnPause);
        if(mBtnPause != null && mBtnPause1 != null){
            mBtnPause.setOnClickListener(mPauseClickListener);
            mBtnPause1.setOnClickListener(mPauseClickListener);
        }



        mEndTime = (TextView) view.findViewById(R.id.time);
        mCurTime = (TextView) view.findViewById(R.id.time_current);
        mStrBuilder = new StringBuilder();
        mFormatter = new Formatter(mStrBuilder, Locale.getDefault());           //格式化 区域位置
    }

    //===============================控制栏各个按钮监听===============================================
    private OnClickListener mFullscreenListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            int screenOrientation = mContext.getRequestedOrientation();
            if(screenOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT){
                mContext.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }else{
                mContext.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }
    };



    private OnClickListener mPauseClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            mIsPause = !mIsPause;
            doPauseOrResume();
        }
    };

    private SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromuser) {
            if(mPlayerCtr == null){
                return;
            }
            if (!fromuser) {
                // We're not interested in programmatically generated changes to
                // the progress bar's position.
                return;
            }
            /**
             * 改变视频时间  并定位
             */
            long duration = mPlayerCtr.getDuration();
            long newPosition = (duration * progress) / 1000L;
            mPlayerCtr.seekTo((int) newPosition);
            if(mCurTime != null){
                mCurTime.setText(stringForTime((int) newPosition));
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            show(3600000);

            mIsDragging = true;
            mHandler.removeMessages(SHOW_PROGRESS);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mIsDragging = false;
            setProgress();
            //updatePausePlay();
            show(DEFAULTTIMEOUT);

            mHandler.sendEmptyMessage(SHOW_PROGRESS);
        }
    };


    //===============================控制栏各个按钮监听===============================================
    /**
     * Show the controller on screen. It will go away automatically after
     * 3 seconds of inactivity.
     */
    public void show(){
        show(DEFAULTTIMEOUT);
    }

    private void show(int timeout) {
        if (!mIsShowing && mAnchorVGroup != null) {
            setProgress();
            if (mBtnPause != null && mBtnPause1 != null) {
                mBtnPause.requestFocus();
                mBtnPause1.requestFocus();
            }
            disableUnsupportedButtons();

            FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
            );

            mAnchorVGroup.addView(this, tlp);
            mIsShowing = true;
        }
        //updatePausePlay();
        //updateFullScreen();

        mHandler.sendEmptyMessage(SHOW_PROGRESS);

        Message msg = mHandler.obtainMessage(FADE_OUT);
        if (timeout != 0) {
            mHandler.removeMessages(FADE_OUT);
            mHandler.sendMessageDelayed(msg, timeout);
        }
    }

    /**
     * Disable pause or seek buttons if the stream cannot be paused or seeked.
     * This requires the control interface to be a MediaPlayerControlExt
     */
    private void disableUnsupportedButtons(){
        if(mPlayerCtr == null){
            return;
        }
        try {
            if(mBtnPause != null && mBtnPause1 != null && !mPlayerCtr.canPause()){
                mBtnPause.setEnabled(false);
                mBtnPause1.setEnabled(false);
            }
        }catch (IncompatibleClassChangeError ex){
            ex.printStackTrace();
        }

    }

    @SuppressLint("NewApi")
    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(JMediaControl.class.getName());
    }

    @SuppressLint("NewApi")
    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(JMediaControl.class.getName());
    }

    /**
     * 设置seekBar进度条
     * @return
     */
    private int setProgress(){
        if(mPlayerCtr == null || mIsDragging){
            return 0;
        }
        int position = mPlayerCtr.getCurPosition();
        int duration = mPlayerCtr.getDuration();
        if(mProgress != null){
            if(duration > 0){
                long pos = 1000L * position / duration;
                mProgress.setProgress((int)pos);                //拖拽的显示到进度条上去
            }
            int percent = mPlayerCtr.getBufPercent();
            mProgress.setSecondaryProgress(percent * 10);
        }

        //显示时间
        if(mEndTime != null){
            mEndTime.setText(stringForTime(duration));
        }
        if(mCurTime != null){
            mCurTime.setText(stringForTime(position));
        }
        return position;
    }

    /**
     * 暂停或继续视频
     */
    private void doPauseOrResume(){
        if(mPlayerCtr == null){
            return;
        }
        if(mIsPause){
            mPlayerCtr.pause();
        }else{
            mPlayerCtr.start();
        }
    }

    /**
     * 时间毫秒数转换为能识别的时间字符串
     * @param timeMs
     * @return
     */
    private String stringForTime(int timeMs){
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        mStrBuilder.setLength(0);
        if(hours > 0){
            return  mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        }else{
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }


    public void hide(){
        if(mAnchorVGroup == null){
            return;
        }

        try{
            mAnchorVGroup.removeView(this);
            if(mHandler != null){
                mHandler.removeMessages(SHOW_PROGRESS);
            }
        }catch (IllegalArgumentException ex){
            ex.printStackTrace();
        }
        mIsShowing = false;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if(mBtnPause != null){
            mBtnPause.setEnabled(enabled);
        }

        if(mBtnPause1 != null){
            mBtnPause1.setEnabled(enabled);
        }

        if(mBtnFullscreen != null){
            mBtnFullscreen.setEnabled(enabled);
        }

        if(mProgress != null){
            mProgress.setEnabled(enabled);
        }
        disableUnsupportedButtons();
        super.setEnabled(enabled);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        show(DEFAULTTIMEOUT);
        return true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        show(DEFAULTTIMEOUT);
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(mPlayerCtr == null){
            return true;
        }
        int keyCode = event.getKeyCode();
        final boolean uniqueDown = event.getRepeatCount() == 0 && event.getAction() == KeyEvent.ACTION_DOWN;
        if(keyCode == KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_SPACE){
            if(uniqueDown){
                doPauseOrResume();
                show(DEFAULTTIMEOUT);
                if(mBtnPause != null && mBtnPause1 != null){
                    mBtnPause.requestFocus();
                    mBtnPause1.requestFocus();
                }
            }
            return true;
        }else if(keyCode == KeyEvent.KEYCODE_MEDIA_PLAY){
            if (uniqueDown && !mPlayerCtr.isPlaying()) {
                mPlayerCtr.start();
                //updatePausePlay();
                show(DEFAULTTIMEOUT);
            }
            return true;
        }else if(keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE){
            if (uniqueDown && mPlayerCtr.isPlaying()) {
                mPlayerCtr.pause();
                //updatePausePlay();
                show(DEFAULTTIMEOUT);
            }
            return true;
        }else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
            // don't show the controls for volume adjustment
            return super.dispatchKeyEvent(event);
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
            if (uniqueDown) {
                hide();
            }
            return true;
        }
        show(DEFAULTTIMEOUT);
        return super.dispatchKeyEvent(event);
    }

    public interface ControlOper {
        void    start();
        void    pause();
        int     getDuration();
        int     getCurPosition();
        void    seekTo(int pos);
        boolean isPlaying();
        int     getBufPercent();
        boolean canPause();
        boolean canSeekBackward();
        boolean canSeekForward();
        boolean isFullScreen();
        void    fullScreen();
    }


    private static class MyHandler extends Handler{

        private final WeakReference<JMediaControl> weakReference;
        public MyHandler(JMediaControl control){
            weakReference = new WeakReference<>(control);
        }

        @Override
        public void handleMessage(Message msg) {
            JMediaControl control = weakReference.get();
            if(control != null){
                myHandlerMsg(control, msg);
            }
        }


        private void myHandlerMsg(JMediaControl control, Message msg){
            int pos;
            switch (msg.what){
                case FADE_OUT:
                    control.hide();
                    break;

                case SHOW_PROGRESS:
                    if(control.mPlayerCtr.isPlaying()){
                        pos = control.setProgress();
                    }else{
                        return;
                    }

                    if(!control.mIsDragging && control.mIsShowing && control.mPlayerCtr.isPlaying()){
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
            }
        }
    }
}
