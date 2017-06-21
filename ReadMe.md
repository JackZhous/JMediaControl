[![](https://www.jitpack.io/v/JackZhous/JMediaControl.svg)](https://www.jitpack.io/#JackZhous/JMediaControl)

# 一款自定义的MediaControl组件，替换Android系统自带的控制组件，控制视频播放、快进、暂停等功能
## 效果图
![show](demo.gif)

## 使用方法

### 导入：
#### step 1:
在项目根目录下的build.gradle添加
```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

#### step 2:
添加项目依赖
```
	dependencies {
	        compile 'com.github.JackZhous:JMediaControl:v1.1'
	}

```

### 代码调用：

1. 实现自定义组件必须的接口ControlOper,接口内部逻辑自己实现，该接口功能主要用于完成播放、快进和全屏等功能
```java
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
```
2. 初始化自定义组件
```java
jMediaControl = new JMediaControl(this);
jMediaControl.setmPlayerCtr(mPlayerControl);		//设置接口
jMediaControl.setAnchorView(mSurfaceContainer);		//设置组件的依附视图
jMediaControl.startLoadingAnimation();				//视频播放之前的动画效果，防止黑屏造成不友好
```
3. 开启你自己的MediaPlayer逻辑，如设置数据源、缓冲监听、预备播放监听和生命周期等，后完成播放即可
4. 在事件onTouchEvent里面调用
5. 最后在销毁的时候，调用自定义组件的销毁方法，防止内存溢出
```java
public void recycleSelf();
```

## 实现原理
根据功能需求决定实现的方法和原理，该组件主要是将自定义组件依附在播放的页面上，并且执行自定义组件上面的监听动作，当非全屏时，点击播放部分页面会展示自定义组件，非播放页面点击不展示;当控制界面展示时，3秒无操作自动掩藏，并且掩藏后自动展示默认的进度条（视频最下方的小横条），下次再次点击视频时，又展示控制栏
所以主要功能需求有三：
	1. 依附组件
	2. 执行播放、暂停和全屏等监听
	3. 两种进度条的显示和掩藏逻辑
	
### 依附组件
	1. 将依附的根布局viewgroup传入自定义组件中，通过setAnchorView
	2. 依附过程，主要是加载xml，移除我们自定义viewgroup的allviews子视图，将加载的视图xml添加addview自定义组件viewgroup中
	3. 最后，再将自定义组件addview到根布局视图中去
	__注意，最好每次添加视图前，都要先移除当前视图的内容__
	下面展示添加控制视图代码：
```java
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
```

### 执行播放、暂停和全屏等监听
	1. 该部分功能主要是通过回调接口ControlOper完成，比如点击暂停时就去掉用该接口的pause方法,这部分逻辑有许多需要用户自己完成
	这部分代码比较简单，稍稍看下代码就明白了：
```java
		mBtnPause = (ImageButton)view.findViewById(R.id.pause);
        mBtnPause1 = (ImageButton)view.findViewById(R.id.btnPause);
        if(mBtnPause != null && mBtnPause1 != null){
            mBtnPause.setOnClickListener(mPauseClickListener);
            mBtnPause1.setOnClickListener(mPauseClickListener);
        }
		
		 private OnClickListener mPauseClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            mIsPause = !mIsPause;
            doPauseOrResume();
            updateBtnPauseStatus();
        }
    };
```

### 进度条逻辑
	严格来说，此自定义组件一共加载了3中xml视图进入内存了：
	（1）播放前的动画，由于是一次性的动作，后续不在处理可以忽略；
	（2）控制操作界面的进度条，你可以点击暂停播放拉取等操作的进度条，这个视图3秒显示，超出时间后消失掩藏
	（3）无任何操作时，自动显示在视频最下方方便用户查看进度，这个界面不能操作，只能查看，并且它不能和条件2同时存在
	此处，只讲解2和3；
	a. 当视频MediaPlayer预备播放完成后，可以播放时，先让视频暂停并显示我们的操作界面，_暂停状态下，不消失操作界面_，这里调用show方法：
```java
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
```
这里先判断是否已经添加视图，添加了就不用再添加了；后面发送SHOW_SEEKBAR的handler消息，用于更新seekbar的进度条，并且会发送一个延时的messgae，用于控制界面的消失逻辑处理；中间的mISPause判断逻辑用处是：暂停状态一直显示界面，不消失，还有当视频结束时，也自动让界面显示不消失
	b. 在看看handler接收消息逻辑
```java
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
```
消息分为三种，显示控制界面消息、移除控制界面消息和显示默认的进度条消息；默认进度条显示的时机在哪里？
起初，我的想法是将显示控制界面和显示默认进度条设置两种状态量，根据状态量然后在show方法里面去调用各种进度条显示移除等，后来思绪很久，发现状态太多很难进行判断控制，逻辑过于复杂；
后面干脆就跟着逻辑走就行了，不需要状态判断显示，一个原则：__只要控制状态视图消失移除的时候就是我要添加默认显示视图的时候；只要点击事件传递到我自定义组件的时候，就是我要添加控制视图的时候__，所以就不需要状态来控制显示，逻辑控制即可，关键逻辑调用代码：
__添加默认进度视图时机__：
```java
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
```

__添加控制界面视图时机:__
```java
@Override
public boolean onTouchEvent(MotionEvent event) {
	show();
	return true;
}
```
这里，关于事件如何传递到我们的视图中，就需要在添加视图时，将我们的视频布满在MediaPlayer的整个播放视图里面才行，就是布局要完全覆盖MediaPlayer的视频视图，这样才能保证点击事件才能传递下来；还有一点，这两种视图在添加时，要注意移除上一个视图；
最后，其他的seekbar进度逻辑、更新缓存状态等逻辑就相对简单了，可以自己看下我的源代码，其实还可以继续添加手势监听，调节亮度音量等，由于没有时间，就暂时没完成；这部分在onTouchEvent里面去完成就行了；
