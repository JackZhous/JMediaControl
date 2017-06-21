package com.jackzhous.label.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;


import com.jackzhous.label.R;

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

    private static final String     TAG = "jackzhous -- JMediaControl";
    private static final int        FADE_OUT = 1;
    private static final int        SHOW_SEEKBAR = 2;
    private static final int        SHOW_PROGRESSBAR = 3;
    private static final int        DEFAULTTIMEOUT = 3000;
    private static final int        MAX_PROGRESS = 1000;

    private Handler                 mHandler = new MyHandler(this);
    private View                    mCtrlView;
    private View                    mProgressView;
    private View                    mLoadingView;
    private Activity                mContext;
    private ControlOper             mPlayerCtr;
    private ViewGroup               mAnchorVGroup;
    private ImageButton             mBtnFullscreen;
    private ImageButton             mBtnPause;
    private ImageButton             mBtnPause1;
    private TextView                mCurTime;
    private TextView                mEndTime;
    private ProgressBar             mSeekBar;                                                       //控制视图中的进度条
    private ProgressBar             mProgressBar;                                                   //默认显示IDE进度条
    private boolean                 mIsCtlShowing;
    private boolean                 mIsDefaultProgressShowing;
    private boolean                 mIsDragging;
    private boolean                 mFromXml;
    private boolean                 mUseFastForward;
    private boolean                 mIsPause = true;
    private StringBuilder           mStrBuilder;
    private Formatter               mFormatter;
    private AnimationDrawable       mLoadingAnimation;
    private boolean                 mIsLoadingComplelte;
    private LayoutInflater          mInflater;

    public JMediaControl(Activity context) {
        this(context, true);
    }

    public JMediaControl(Activity context, AttributeSet attrs) {
        super(context, attrs);

        mCtrlView = null;
        mContext = context;
        mUseFastForward = true;
        mFromXml = true;
        init();
    }

    public JMediaControl(Activity context, boolean useFastForward) {
        super(context);
        mContext = context;
        mUseFastForward = useFastForward;
        init();
    }

    private void init(){
        mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mStrBuilder = new StringBuilder();
        mFormatter = new Formatter(mStrBuilder, Locale.getDefault());           //格式化 区域位置
    }

    public void recycleSelf(){
        if(null != mHandler){
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        removeAllViews();
        if(mAnchorVGroup != null){
            mAnchorVGroup.removeView(this);
        }
        cancelViewListener();
        mAnchorVGroup = null;
        mContext = null;
        mPlayerCtr = null;
    }

    /**
     * 取消监听，因为同一个listener持有许多组件的引用（setclick），
     * 而且listener持有外部引用，导致不用的组件就无法释放
     */
    private void cancelViewListener(){
        if(mBtnPause1 != null && mBtnPause != null){
            mBtnPause1.setOnClickListener(null);
            mBtnPause.setOnClickListener(null);
        }

        if(mBtnFullscreen != null){
            mBtnFullscreen.setOnClickListener(null);
            mBtnFullscreen = null;
        }

        if(mSeekBar != null){
            SeekBar bar = (SeekBar) mSeekBar;
            bar.setOnSeekBarChangeListener(null);
            mSeekBar = null;
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
    }


    /**
     * 绑定控制视图到当前的自定义视图
     */
    private void bindingContrlView(){
        if(mAnchorVGroup == null){
            return;
        }
        mAnchorVGroup.removeView(this);
        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
        );
        removeAllViews();
        if(mCtrlView == null){
            createCtrlView();
        }
        addView(mCtrlView, frameParams);
    }


    /**
     * 绑定进度条到当前自定义视图
     */
    private void bindingProgressBar(){
        if(mAnchorVGroup == null){
            return;
        }
        mAnchorVGroup.removeView(this);
        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
        );
        removeAllViews();
        if(mProgressView == null){
            createProgessView();
        }
        addView(mProgressView, frameParams);
    }

    public void startLoadingAnimation(){
        if(mAnchorVGroup == null){
            return;
        }
        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        removeAllViews();
        if(mLoadingView == null){
            createLoadingAnimation();
        }
        addView(mLoadingView, frameParams);
        mAnchorVGroup.addView(this, frameParams);
        mLoadingAnimation.start();
    }


    public void stopLoadingAnimation(){
        if(mLoadingAnimation != null){
            mIsLoadingComplelte = true;
            mLoadingAnimation.stop();
            mLoadingAnimation = null;
            mLoadingView = null;
        }
    }

    private void createProgessView(){

        mProgressView = mInflater.inflate(R.layout.progress, null);

        mProgressBar = (ProgressBar)mProgressView.findViewById(R.id.progress);

        if(mProgressBar != null){
            mProgressBar.setMax(MAX_PROGRESS);
        }
    }

    /**
     * 创建启动画面视图view
     * @return
     */
    private void createLoadingAnimation(){

        mLoadingView  = mInflater.inflate(R.layout.loading, null);

        ImageView loadingView = (ImageView)mLoadingView.findViewById(R.id.loadingAnimation);

        mLoadingAnimation = (AnimationDrawable)loadingView.getDrawable();

    }

    /**
     * 创建媒体蓝控制信息view
     * @return
     */
    private View createCtrlView(){

        mCtrlView = mInflater.inflate(R.layout.controller, null);

        initCtrView(mCtrlView);

        return mCtrlView;
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

        mSeekBar = (ProgressBar) view.findViewById(R.id.mediacontroller_progress);
        if (mSeekBar != null) {
            if (mSeekBar instanceof SeekBar) {
                SeekBar seeker = (SeekBar) mSeekBar;
                seeker.setOnSeekBarChangeListener(mSeekListener);
            }
            mSeekBar.setMax(MAX_PROGRESS);
        }

        mBtnPause = (ImageButton)view.findViewById(R.id.pause);
        mBtnPause1 = (ImageButton)view.findViewById(R.id.btnPause);
        if(mBtnPause != null && mBtnPause1 != null){
            mBtnPause.setOnClickListener(mPauseClickListener);
            mBtnPause1.setOnClickListener(mPauseClickListener);
        }



        mEndTime = (TextView) view.findViewById(R.id.time);
        mCurTime = (TextView) view.findViewById(R.id.time_current);
    }

    //===============================控制栏各个按钮监听===============================================
    private OnClickListener mFullscreenListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if(mPlayerCtr != null){
                mPlayerCtr.fullScreen();
            }
        }
    };

    private OnClickListener mPauseClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            mIsPause = !mIsPause;
            doPauseOrResume();
            updateBtnPauseStatus();
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
            mHandler.removeMessages(SHOW_SEEKBAR);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mIsDragging = false;
            setProgress(mSeekBar);
            show(DEFAULTTIMEOUT);

            mHandler.sendEmptyMessage(SHOW_SEEKBAR);
        }
    };


    //===============================控制栏各个按钮监听===============================================
    /**
     * Show the controller on screen. It will go away automatically after
     * 3 seconds of inactivity.
     */
    public void show(){
        if(mIsLoadingComplelte){
            show(DEFAULTTIMEOUT);
        }
    }

    private void show(int timeout) {
        if (!mIsCtlShowing && mAnchorVGroup != null) {
            //先移除掉默认显示的进度条
            if(mIsDefaultProgressShowing){
                mAnchorVGroup.removeView(this);
                mIsDefaultProgressShowing = false;
            }
            addCtrViewToMediaView();
        }

        updateBtnPauseStatus();
        mHandler.sendEmptyMessage(SHOW_SEEKBAR);

        //暂停状态不掩藏进度条等
        if(mIsPause && !mPlayerCtr.isComplete()){
            return;
        }else if(mPlayerCtr.isComplete()){      //结束时，跳转到暂停状态
            mIsPause = true;
        }
        Message msg = mHandler.obtainMessage(FADE_OUT);
        if (timeout != 0) {
            mHandler.removeMessages(FADE_OUT);
            mHandler.sendMessageDelayed(msg, timeout);
        }
    }

    /**
     * 添加控制视图
     */
    private void addCtrViewToMediaView(){
        bindingContrlView();
        if (mCtrlView != null) {
            setProgress(mSeekBar);
            if (mBtnPause != null && mBtnPause1 != null) {
                mBtnPause.requestFocus();
                mBtnPause1.requestFocus();
            }
            disableUnsupportedButtons();

            FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.BOTTOM
            );

            mAnchorVGroup.addView(this, tlp);
            mIsCtlShowing = true;
        }
    }

    /**
     * 添加默认进度条显示
     */
    private void addDefaultProgessToMediaView(){
        bindingProgressBar();
        if (mAnchorVGroup != null && mProgressView != null) {

            setProgress(mProgressBar);

            FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.BOTTOM
            );

            mAnchorVGroup.addView(this, tlp);
            mIsDefaultProgressShowing = true;
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
    private void setProgress(ProgressBar progressBar){
        if(mPlayerCtr == null || mIsDragging){
            return;
        }
        int position = mPlayerCtr.getCurPosition();
        int duration = mPlayerCtr.getDuration();
        if(progressBar != null){
            if(duration > 0){
                long pos = 1000L * position / duration;
                progressBar.setProgress((int)pos);                //拖拽的显示到进度条上去
            }
            if(mIsDefaultProgressShowing){                          //默认显示下不在设计耳机
                progressBar.setSecondaryProgress(0);
                return;
            }
            int percent = mPlayerCtr.getBufPercent();
            progressBar.setSecondaryProgress(percent * 10);
        }

        //显示时间
        if(mEndTime != null){
            mEndTime.setText(stringForTime(duration));
        }
        if(mCurTime != null){
            mCurTime.setText(stringForTime(position));
            if(mPlayerCtr.isComplete()){
                mCurTime.setText(stringForTime(duration));
                progressBar.setProgress(MAX_PROGRESS);
            }
        }
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

    private void updateBtnPauseStatus(){
        if(mPlayerCtr == null){
            return;
        }
        if(mIsPause || mPlayerCtr.isComplete()){
            mHandler.removeMessages(FADE_OUT);
            mBtnPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_tv_play));
            mBtnPause1.setImageDrawable(getResources().getDrawable(R.drawable.ic_portrait_play));
        }else{
            mHandler.sendEmptyMessageDelayed(FADE_OUT, DEFAULTTIMEOUT);
            mBtnPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_tv_stop));
            mBtnPause1.setImageDrawable(getResources().getDrawable(R.drawable.ic_portrait_stop));
        }
        mHandler.sendEmptyMessage(SHOW_SEEKBAR);
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
            addDefaultProgessToMediaView();
            if(mHandler != null){
                mHandler.removeMessages(SHOW_SEEKBAR);
                mHandler.sendEmptyMessage(SHOW_PROGRESSBAR);
            }
        }catch (IllegalArgumentException ex){
            ex.printStackTrace();
        }
        mIsCtlShowing = false;
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

        if(mSeekBar != null){
            mSeekBar.setEnabled(enabled);
        }
        disableUnsupportedButtons();
        super.setEnabled(enabled);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        show();
        return true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        show();
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
        boolean isComplete();
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
            switch (msg.what){
                case SHOW_PROGRESSBAR:
                    control.setProgress(control.mProgressBar);
                    if((!control.mIsCtlShowing) && (!control.mPlayerCtr.isComplete())){
                        sendEmptyMessageDelayed(SHOW_PROGRESSBAR, 1000);
                    }
                    break;

                case FADE_OUT:
                    if(!control.mPlayerCtr.isComplete()){
                        control.hide();
                    }
                    break;

                //更新显示进度条
                case SHOW_SEEKBAR:
                    control.setProgress(control.mSeekBar);
                    if(!control.mIsDragging && control.mIsCtlShowing && !control.mPlayerCtr.isComplete()){
                        msg = obtainMessage(SHOW_SEEKBAR);
                        sendMessageDelayed(msg, 1500);
                    }
                    break;
            }
        }
    }
}
