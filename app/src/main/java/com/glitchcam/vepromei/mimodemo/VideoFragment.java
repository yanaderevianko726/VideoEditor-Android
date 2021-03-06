package com.glitchcam.vepromei.mimodemo;

import android.content.Context;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.meicam.sdk.NvsLiveWindow;
import com.meicam.sdk.NvsStreamingContext;
import com.meicam.sdk.NvsTimeline;
import com.meicam.sdk.NvsTimelineAnimatedSticker;
import com.meicam.sdk.NvsTimelineCaption;
import com.meicam.sdk.NvsTimelineCompoundCaption;
import com.glitchcam.vepromei.R;
import com.glitchcam.vepromei.mimodemo.common.Constants;
import com.glitchcam.vepromei.mimodemo.common.dataInfo.TimelineData;
import com.glitchcam.vepromei.mimodemo.common.utils.ScreenUtils;
import com.glitchcam.vepromei.mimodemo.common.utils.TimeUtils;
import com.glitchcam.vepromei.mimodemo.common.view.DrawRect;
import com.glitchcam.vepromei.utils.TimelineUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class VideoFragment extends Fragment{
    private static final float DEFAULT_SCALE_VALUE = 1.0f;
    private static final long BASE_VALUE = 100000;
    private static final String PARAM_MAX_DURATION = "max_duration";
    private static final int MESSAGE_RESET_PLATBACK_STATE = 100;
    private  NvsLiveWindow mLiveWindow;
    private NvsTimeline mTimeline;
    private NvsStreamingContext mStreamingContext = NvsStreamingContext.getInstance();
    private RelativeLayout mPlayerLayout;
    private long mStartTime;
    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            // TODO Auto-generated method stub
            switch (msg.what) {
                case MESSAGE_RESET_PLATBACK_STATE:
                    playVideo(mStartTime,mStartTime + getDuration());
                    break;
            }
            return false;
        }
    });
    private long mMaxDuration;
    private View mPlayBar;
    private View mPlayButton;
    private TextView mTotalDurationView;
    private TextView mCurrentPlaytimeView;
    private SeekBar mSeekBar;
    private ImageView mPlayButtonImage;
    private OnPlayProgressChangeListener mOnPlayProgessChangeListener;
    private DrawRect mDrawRect;
    private NvsTimelineCaption mCurCaption;
    private int mEditMode = 0;
    //liveWindow ??????view????????????
    private List<PointF> pointFListLiveWindow;
    private NvsTimelineCompoundCaption mCurCompoundCaption;
    private NvsTimelineAnimatedSticker mCurAnimateSticker;
    private int mStickerMuteIndex;
    private OnThemeCaptionSeekListener mThemeCaptionSeekListener;
    private OnLiveWindowClickListener mLiveWindowClickListener;
    private VideoCaptionTextEditListener mCaptionTextEditListener;
    private OnCompoundCaptionListener mCompoundCaptionListener;
    private OnStickerMuteListener mStickerMuteListener;
    private AssetEditListener mAssetEditListener;
    private OnFragmentLoadFinisedListener mFragmentLoadFinisedListener;

    public void setOnPlayProgressChangeListener(OnPlayProgressChangeListener onPlayProgessChangeListener) {
        this.mOnPlayProgessChangeListener = onPlayProgessChangeListener;
    }

    public VideoFragment() {
    }

    public static VideoFragment newInstance(long maxDuration) {
        VideoFragment fragment = new VideoFragment();
        Bundle args = new Bundle();
        args.putLong(PARAM_MAX_DURATION, maxDuration);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (getArguments() != null) {
            mMaxDuration = getArguments().getLong(PARAM_MAX_DURATION);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.mimo_fragment_video, container, false);
        mLiveWindow = (NvsLiveWindow) rootView.findViewById(R.id.liveWindow);
        mLiveWindow.setFillMode(NvsLiveWindow.FILLMODE_PRESERVEASPECTFIT);
        mPlayerLayout = (RelativeLayout) rootView.findViewById(R.id.playerLayout);
        mDrawRect = (DrawRect) rootView.findViewById(R.id.draw_rect);
        mPlayBar = rootView.findViewById(R.id.playBarLayout);
        mPlayButton = rootView.findViewById(R.id.playLayout);
        mCurrentPlaytimeView = (TextView)rootView.findViewById(R.id.currentPlaytime);
        mTotalDurationView = (TextView)rootView.findViewById(R.id.totalDuration);
        mSeekBar = (SeekBar) rootView.findViewById(R.id.playSeekBar);
        mPlayButtonImage = (ImageView) rootView.findViewById(R.id.playImage);
        initListener();
        return rootView;
    }

    private void initListener() {
        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getCurrentEngineState() == NvsStreamingContext.STREAMING_ENGINE_STATE_PLAYBACK) {
                    stopEngine();
                } else {
                    if(mTimeline == null){
                        return;
                    }
                    long startTime = mStreamingContext.getTimelineCurrentPosition(mTimeline);
                    long alreadyPlayDuration = startTime - mStartTime;//???????????????????????????????????????
                    long endTime = startTime + getDuration() - (alreadyPlayDuration);
                    playVideo(startTime, endTime);
                    if (mOnPlayProgessChangeListener != null) {
                        mOnPlayProgessChangeListener.onPlayStateChanged(true);
                    }
                }
            }
        });
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private long currentTime = 0L;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentTime = getDuration() * progress / 100 + mStartTime; //????????????
                    seekTimeline(currentTime, 0);
                    updateCurPlayTime(currentTime);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                playVideo(currentTime, currentTime + getDuration());
            }
        });
    }

    private void updateCurPlayTime(long currentTime) {
        mCurrentPlaytimeView.setText(TimeUtils.formatTimeStrWithUs(currentTime - mStartTime));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initData();
        if (mFragmentLoadFinisedListener != null) {
            mFragmentLoadFinisedListener.onLoadFinished();
        }
    }

    public void initData() {
        setLiveWindowRatio();
        initPlayBar();
        initDrawRectListener();
    }

    public void setMaxDuration(long maxDuration) {
        this.mMaxDuration = maxDuration;
    }

    public void setEditMode(int mode) {
        mEditMode = mode;
    }

    public void setStartTime(long startTime) {
        this.mStartTime = startTime;
    }

    public long getDuration() {
        if (mMaxDuration > 0L) {
            return mMaxDuration;
        }
        if (mTimeline == null) {
            return 0L;
        }
        return mTimeline.getDuration();
    }

    private void initPlayBar() {
        mCurrentPlaytimeView.setText(TimeUtils.formatTimeStrWithUs(0));
        mTotalDurationView.setText(TimeUtils.formatTimeStrWithUs(getDuration()));
    }

    private void setLiveWindowRatio() {
        if (null == mTimeline) {
            return;
        }
        int makeRatio = TimelineData.instance().getMakeRatio();
        setLiveWindowRatio(makeRatio);
        connectTimelineWithLiveWindow();
    }

    private void connectTimelineWithLiveWindow() {
        if (mStreamingContext == null || mTimeline == null || mLiveWindow == null)
            return;
        mStreamingContext.setPlaybackCallback(new NvsStreamingContext.PlaybackCallback() {
            @Override
            public void onPlaybackPreloadingCompletion(NvsTimeline nvsTimeline) {

            }

            @Override
            public void onPlaybackStopped(NvsTimeline nvsTimeline) {
            }

            @Override
            public void onPlaybackEOF(NvsTimeline nvsTimeline) {
                mHandler.sendEmptyMessage(MESSAGE_RESET_PLATBACK_STATE);
            }
        });

        mStreamingContext.setPlaybackCallback2(new NvsStreamingContext.PlaybackCallback2() {
            @Override
            public void onPlaybackTimelinePosition(NvsTimeline nvsTimeline, long cur_position) {
                updatePlayProgress(cur_position);
                if (mOnPlayProgessChangeListener != null) {
                    mOnPlayProgessChangeListener.onPlayProgressChanged(cur_position);
                }
            }
        });
        mStreamingContext.setSeekingCallback(new NvsStreamingContext.SeekingCallback() {
            @Override
            public void onSeekingTimelinePosition(NvsTimeline nvsTimeline, long cur_position) {
                updatePlayProgress(cur_position);
                if (mOnPlayProgessChangeListener != null) {
                    mOnPlayProgessChangeListener.onPlayProgressChanged(cur_position);
                }
            }
        });
        mStreamingContext.setStreamingEngineCallback(new NvsStreamingContext.StreamingEngineCallback() {
            @Override
            public void onStreamingEngineStateChanged(int i) {
                if (i == NvsStreamingContext.STREAMING_ENGINE_STATE_PLAYBACK) {
                    mPlayButtonImage.setBackgroundResource(R.mipmap.icon_pause);
                } else {
                    mPlayButtonImage.setBackgroundResource(R.mipmap.icon_play);
                }
            }

            @Override
            public void onFirstVideoFramePresented(NvsTimeline nvsTimeline) {

            }
        });

        mStreamingContext.connectTimelineWithLiveWindow(mTimeline, mLiveWindow);
        //playVideo(mStartTime, mStartTime + getDuration());
    }

    public boolean isPlaying() {
        return getCurrentEngineState() == NvsStreamingContext.STREAMING_ENGINE_STATE_PLAYBACK;
    }

    private void updatePlayProgress(long curPosition) {
        float progress = ((float) (curPosition - mStartTime) / (float) getDuration());
        mSeekBar.setProgress((int) (progress * 100));
        updateCurPlayTime(curPosition);
    }

    private void setLiveWindowRatio(int ratio) {
        ViewGroup.LayoutParams layoutParams = mPlayerLayout.getLayoutParams();
        int titleHeight = ScreenUtils.dip2px(getActivity(),44);
        int palyBarHeight = ScreenUtils.dip2px(getActivity(),48); //palybar ?????????????????????
        int bottomHeight = ScreenUtils.dip2px(getActivity(),145);
        int statusHeight = ScreenUtils.getStatusBarHeight(getActivity());//???????????????
        int screenWidth = ScreenUtils.getScreenWidth(getActivity());//??????
        int screenHeight = ScreenUtils.getScreenHeight(getActivity());//??????
        int newHeight = screenHeight - titleHeight - bottomHeight - statusHeight;
        switch (ratio) {
            case Constants.AspectRatio.AspectRatio_16v9: // 16:9
                layoutParams.width = screenWidth;
                layoutParams.height = (int) (screenWidth * 9.0 / 16);
                break;
            case Constants.AspectRatio.AspectRatio_1v1: //1:1
                layoutParams.width = screenWidth;
                layoutParams.height = screenWidth;
                if (newHeight < screenWidth) {
                    layoutParams.width = newHeight;
                    layoutParams.height = newHeight;
                }
                break;
            case Constants.AspectRatio.AspectRatio_9v16: //9:16
                layoutParams.width = (int) (newHeight * 9.0 / 16);
                layoutParams.height = newHeight;
                break;
            case Constants.AspectRatio.AspectRatio_3v4: // 3:4
                layoutParams.width = (int) (newHeight * 3.0 / 4);
                layoutParams.height = newHeight;
                break;
            case Constants.AspectRatio.AspectRatio_4v3: //4:3
                layoutParams.width = screenWidth;
                layoutParams.height = (int) (screenWidth * 3.0 / 4);
                break;
            default: // 16:9
                layoutParams.width = screenWidth;
                layoutParams.height = (int) (screenWidth * 9.0 / 16);
                break;
        }
        mPlayerLayout.setLayoutParams(layoutParams);
        mLiveWindow.setFillMode(NvsLiveWindow.FILLMODE_PRESERVEASPECTFIT);

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void setTimeLine(NvsTimeline timeline) {
        mTimeline = timeline;
    }

    public void playVideoButtonCilck() {
        if(mTimeline == null){
            return;
        }
        long endTime = getDuration();
        playVideoButtonClick(0, endTime);
    }

    public void playVideoButtonClick(long inPoint, long outPoint) {
        playVideo(inPoint, outPoint);
    }
    public void playVideo(long startTime, long endTime) {
        // ????????????
        mStreamingContext.playbackTimeline(mTimeline, startTime, endTime, NvsStreamingContext.VIDEO_PREVIEW_SIZEMODE_LIVEWINDOW_SIZE, true, 0);
    }

    public void playVideoFromStartPosition() {
        // ????????????
        mStreamingContext.playbackTimeline(mTimeline, mStartTime, mStartTime + getDuration(), NvsStreamingContext.VIDEO_PREVIEW_SIZEMODE_LIVEWINDOW_SIZE, true, 0);
    }

    //??????
    public void seekTimeline(long timestamp, int seekShowMode) {
        mStreamingContext.seekTimeline(mTimeline, timestamp, NvsStreamingContext.VIDEO_PREVIEW_SIZEMODE_LIVEWINDOW_SIZE, seekShowMode);
    }

    // ????????????????????????
    public int getCurrentEngineState() {
        return mStreamingContext.getStreamingEngineState();
    }

    //????????????
    public void stopEngine() {
        if (mStreamingContext != null) {
            mStreamingContext.stop();//????????????
        }
    }

    //??????timeline
    public void removeTimeLine() {
        if(mStreamingContext != null && mTimeline != null){
            TimelineUtil.removeTimeline(mTimeline);
            mTimeline = null;
        }
    }

    private void initDrawRectListener() {
        mDrawRect.setOnTouchListener(new DrawRect.OnTouchListener() {
            @Override
            public void onDrag(PointF prePointF, PointF nowPointF) {

                /* ????????????
                 *
                 * SDK??????????????????????????????Canonical?????????????????????????????????????????????????????????
                 * ?????????Android View ?????????????????????????????????????????????????????????????????????SDK???
                 * ?????????mapViewToCanonical?????????View???????????????Canonical?????????????????????
                 * ???????????????Canonical???????????????View????????????????????????mapCanonicalToView
                 * ?????????????????????
                 * */
                PointF pre = mLiveWindow.mapViewToCanonical(prePointF);
                PointF p = mLiveWindow.mapViewToCanonical(nowPointF);
                PointF timeLinePointF = new PointF(p.x - pre.x, p.y - pre.y);
                if (mEditMode == Constants.EDIT_MODE_CAPTION) {
                    // ????????????
                    if (mCurCaption != null) {
                        //mTimeline.setupInputCacheForCaption(mCurCaption,mStreamingContext.getTimelineCurrentPosition(mTimeline));//?????????????????????????????????????????????
                        mCurCaption.translateCaption(timeLinePointF);
                        updateCaptionCoordinate(mCurCaption);
                        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), NvsStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
                    }
                } else if (mEditMode == Constants.EDIT_MODE_STICKER) { // ????????????
                    // ????????????
                    if (mCurAnimateSticker != null) {
                        mCurAnimateSticker.translateAnimatedSticker(timeLinePointF);
                        updateAnimateStickerCoordinate(mCurAnimateSticker);
                        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), NvsStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_ANIMATED_STICKER_POSTER);
                    }
                } else if (mEditMode == Constants.EDIT_MODE_WATERMARK) {
                    updateWaterMarkPositionOnDrag(timeLinePointF.x, timeLinePointF.y, mDrawRect.getDrawRect());
                }else if(mEditMode == Constants.EDIT_MODE_COMPOUND_CAPTION){
                    if(mCurCompoundCaption != null){
                        mCurCompoundCaption.translateCaption(timeLinePointF);
                        updateCompoundCaptionCoordinate(mCurCompoundCaption);
                        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), NvsStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
                    }
                }
                if (mAssetEditListener != null) {
                    mAssetEditListener.onAssetTranstion();
                }
            }

            @Override
            public void onScaleAndRotate(float scaleFactor, PointF anchor, float angle) {
                /* ????????????
                 *
                 * SDK??????????????????????????????Canonical?????????????????????????????????????????????????????????
                 * ?????????Android View ?????????????????????????????????????????????????????????????????????SDK???
                 * ?????????mapViewToCanonical?????????View???????????????Canonical?????????????????????
                 *???????????????Canonical???????????????View????????????????????????mapCanonicalToView
                 * ?????????????????????
                 * */
                PointF assetAnchor = mLiveWindow.mapViewToCanonical(anchor);
                if (mEditMode == Constants.EDIT_MODE_CAPTION) {
                    if (mCurCaption != null) {
                        //mTimeline.setupInputCacheForCaption(mCurCaption,mStreamingContext.getTimelineCurrentPosition(mTimeline));//?????????????????????????????????????????????
                        // ????????????
                        mCurCaption.scaleCaption(scaleFactor, assetAnchor);
                        // ????????????
                        mCurCaption.rotateCaption(angle);
                        updateCaptionCoordinate(mCurCaption);
                        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), NvsStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
                    }
                } else if (mEditMode == Constants.EDIT_MODE_STICKER) { // ????????????
                    // ????????????
                    if (mCurAnimateSticker != null) {
                        //????????????
                        mCurAnimateSticker.scaleAnimatedSticker(scaleFactor, assetAnchor);
                        //????????????
                        mCurAnimateSticker.rotateAnimatedSticker(angle);
                        updateAnimateStickerCoordinate(mCurAnimateSticker);
                        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), NvsStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_ANIMATED_STICKER_POSTER);
                    }
                } else if (mEditMode == Constants.EDIT_MODE_WATERMARK) {
                    //updateWaterMarkPositionOnScaleAndRotate(scaleFactor, anchor, angle, mDrawRect.getDrawRect());
                }else if(mEditMode == Constants.EDIT_MODE_COMPOUND_CAPTION){
                    if(mCurCompoundCaption != null){
                        mCurCompoundCaption.scaleCaption(scaleFactor, assetAnchor);
                        // ????????????
                        mCurCompoundCaption.rotateCaption(angle,assetAnchor);
                        float scaleX = mCurCompoundCaption.getScaleX();
                        float scaleY = mCurCompoundCaption.getScaleY();
                        if (scaleX <= DEFAULT_SCALE_VALUE && scaleY <= DEFAULT_SCALE_VALUE){
                            mCurCompoundCaption.setScaleX(DEFAULT_SCALE_VALUE);
                            mCurCompoundCaption.setScaleY(DEFAULT_SCALE_VALUE);
                        }
                        updateCompoundCaptionCoordinate(mCurCompoundCaption);
                        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), NvsStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
                    }
                }
                if (mAssetEditListener != null) {
                    mAssetEditListener.onAssetScale();
                }
            }

            @Override
            public void onDel() {
                if (mAssetEditListener != null) {
                    mAssetEditListener.onAssetDelete();
                }
            }

            @Override
            public void onTouchDown(PointF curPoint) {
                if (mAssetEditListener != null) {
                    mAssetEditListener.onAssetSelected(curPoint);
                }
            }

            @Override
            public void onAlignClick() {
                if (mEditMode == Constants.EDIT_MODE_CAPTION
                        && mCurCaption != null) {
                    switch (mCurCaption.getTextAlignment()) {
                        case NvsTimelineCaption.TEXT_ALIGNMENT_LEFT:
                            mCurCaption.setTextAlignment(NvsTimelineCaption.TEXT_ALIGNMENT_CENTER);  //????????????
                            setAlignIndex(1);
                            break;
                        case NvsTimelineCaption.TEXT_ALIGNMENT_CENTER:
                            mCurCaption.setTextAlignment(NvsTimelineCaption.TEXT_ALIGNMENT_RIGHT);  //????????????
                            setAlignIndex(2);
                            break;

                        case NvsTimelineCaption.TEXT_ALIGNMENT_RIGHT:
                            mCurCaption.setTextAlignment(NvsTimelineCaption.TEXT_ALIGNMENT_LEFT);  //?????????
                            setAlignIndex(0);
                            break;
                    }
                    seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), NvsStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_CAPTION_POSTER);
                    if (mAssetEditListener != null) {
                        mAssetEditListener.onAssetAlign(mCurCaption.getTextAlignment());
                    }
                }
            }

            @Override
            public void onHorizFlipClick() {
                if (mEditMode == Constants.EDIT_MODE_STICKER) {
                    if (mCurAnimateSticker == null)
                        return;
                    // ??????????????????
                    boolean isHorizFlip = !mCurAnimateSticker.getHorizontalFlip();
                    mCurAnimateSticker.setHorizontalFlip(isHorizFlip);
                    updateAnimateStickerCoordinate(mCurAnimateSticker);
                    seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline), NvsStreamingContext.STREAMING_ENGINE_SEEK_FLAG_SHOW_ANIMATED_STICKER_POSTER);
                    if (mAssetEditListener != null) {
                        mAssetEditListener.onAssetHorizFlip(isHorizFlip);
                    }
                }
            }

            @Override
            public void onBeyondDrawRectClick() {
                mPlayButton.callOnClick();
            }
        });

        mDrawRect.setDrawRectClickListener(new DrawRect.onDrawRectClickListener() {
            @Override
            public void onDrawRectClick(int captionIndex) {
                if(mEditMode == Constants.EDIT_MODE_CAPTION){
                    if (mCaptionTextEditListener != null){
                        mCaptionTextEditListener.onCaptionTextEdit();
                    }
                }else if(mEditMode == Constants.EDIT_MODE_COMPOUND_CAPTION){
                    if(mCompoundCaptionListener != null) {
                        mCompoundCaptionListener.onCaptionIndex(captionIndex);
                    }
                }
            }
        });

        mDrawRect.setStickerMuteListenser(new DrawRect.onStickerMuteListenser() {
            @Override
            public void onStickerMute() {
                if (mCurAnimateSticker == null)
                    return;
                mStickerMuteIndex = mStickerMuteIndex == 0 ? 1 : 0;
                float volumeGain = mStickerMuteIndex == 0 ? 1.0f : 0.0f;
                mCurAnimateSticker.setVolumeGain(volumeGain, volumeGain);
                setStickerMuteIndex(mStickerMuteIndex);
                if (mStickerMuteListener != null)
                    mStickerMuteListener.onStickerMute();
            }
        });
    }

    private void updateWaterMarkPositionOnDrag(float x, float y, List<PointF> list) {
        List<PointF> newList = new ArrayList<>();
        for (PointF pointF : list) {
            newList.add(new PointF(pointF.x + x, pointF.y - y));
        }
        if (checkInLiveWindow(newList)) {
            mDrawRect.setDrawRect(newList, Constants.EDIT_MODE_WATERMARK);
        }
    }

    // ?????????????????????????????????
    public void updateCaptionCoordinate(NvsTimelineCaption caption) {
        if (caption != null) {
            // ????????????????????????????????????????????????????????????
            List<PointF> list = caption.getBoundingRectangleVertices();
            if (list == null || list.size() < 4)
                return;

            List<PointF> newList = getAssetViewVerticesList(list);
            mDrawRect.setDrawRect(newList, Constants.EDIT_MODE_CAPTION);
        }
    }

    //??????API
    public void setCurAnimateSticker(NvsTimelineAnimatedSticker animateSticker) {
        mCurAnimateSticker = animateSticker;
    }

    public void setStickerMuteIndex(int index) {
        mStickerMuteIndex = index;
        mDrawRect.setStickerMuteIndex(index);
    }

    public NvsTimelineAnimatedSticker getCurAnimateSticker() {
        return mCurAnimateSticker;
    }

    // ?????????????????????????????????
    public void updateAnimateStickerCoordinate(NvsTimelineAnimatedSticker animateSticker) {
        if (animateSticker != null) {
            // ????????????????????????????????????????????????????????????
            List<PointF> list = animateSticker.getBoundingRectangleVertices();
            if (list == null || list.size() < 4)
                return;
            boolean isHorizonFlip = animateSticker.getHorizontalFlip();
            if (isHorizonFlip) {//?????????????????????????????????????????????????????????
                Collections.swap(list, 0, 3);
                Collections.swap(list, 1, 2);
            }
            List<PointF> newList = getAssetViewVerticesList(list);
            mDrawRect.setDrawRect(newList, Constants.EDIT_MODE_STICKER);
        }
    }

    //???????????????????????????
    public void changeStickerRectVisible() {
        if (mEditMode == Constants.EDIT_MODE_STICKER) {
            setDrawRectVisible(isSelectedAnimateSticker() ? View.VISIBLE : View.GONE);
        }
    }

    //???liveWindow?????????????????????
    public void selectAnimateStickerByHandClick(PointF curPoint) {
        if(mTimeline == null){
            return;
        }
        List<NvsTimelineAnimatedSticker> stickerList = mTimeline.getAnimatedStickersByTimelinePosition(mStreamingContext.getTimelineCurrentPosition(mTimeline));
        if (stickerList.size() <= 1)
            return;

        for (int j = 0; j < stickerList.size(); j++) {
            NvsTimelineAnimatedSticker sticker = stickerList.get(j);
            List<PointF> list = sticker.getBoundingRectangleVertices();
            List<PointF> newList = getAssetViewVerticesList(list);
            boolean isSelected = mDrawRect.clickPointIsInnerDrawRect(newList,(int) curPoint.x, (int) curPoint.y);
            if (isSelected) {
                mDrawRect.setDrawRect(newList, Constants.EDIT_MODE_STICKER);
                mCurAnimateSticker = sticker;
                break;
            }
        }
    }

    public void setMuteVisible(boolean hasAudio) {
        mDrawRect.setMuteVisible(hasAudio);
    }

    private boolean isSelectedAnimateSticker() {
        long curPosition = mStreamingContext.getTimelineCurrentPosition(mTimeline);
        if (mCurAnimateSticker != null
                && curPosition >= mCurAnimateSticker.getInPoint()
                && curPosition <= mCurAnimateSticker.getOutPoint()) {
            return true;
        }
        return false;
    }

    public void setAlignIndex(int index) {
        mDrawRect.setAlignIndex(index);
    }

    //??????API
    public void setCurCaption(NvsTimelineCaption caption) {
        mCurCaption = caption;
    }

    public NvsTimelineCaption getCurCaption() {
        return mCurCaption;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //????????????API
    public void setCurCompoundCaption(NvsTimelineCompoundCaption caption) {
        mCurCompoundCaption = caption;
    }

    public NvsTimelineCompoundCaption getCurrCompoundCaption() {
        return mCurCompoundCaption;
    }

    // ???????????????????????????????????????
    public void updateCompoundCaptionCoordinate(NvsTimelineCompoundCaption caption) {
        if (caption != null) {
            // ????????????????????????????????????????????????????????????
            List<PointF> list = caption.getCompoundBoundingVertices(NvsTimelineCompoundCaption.BOUNDING_TYPE_FRAME);
            if (list == null || list.size() < 4){
                return;
            }
            List<PointF> newList = getAssetViewVerticesList(list);
            List<List<PointF>> newSubCaptionList = new ArrayList<>();
            int subCaptionCount = caption.getCaptionCount();
            for (int index = 0;index < subCaptionCount;index++){
                List<PointF> subList = caption.getCaptionBoundingVertices(index,NvsTimelineCompoundCaption.BOUNDING_TYPE_TEXT);
                if (subList == null || subList.size() < 4){
                    continue;
                }
                List<PointF> newSubList = getAssetViewVerticesList(subList);
                newSubCaptionList.add(newSubList);
            }
            mDrawRect.setCompoundDrawRect(newList,newSubCaptionList, Constants.EDIT_MODE_COMPOUND_CAPTION);
        }
    }

    public void changeCompoundCaptionRectVisible() {
        if (mEditMode == Constants.EDIT_MODE_COMPOUND_CAPTION) {
            setDrawRectVisible(false);
        }
    }

    private boolean isSelectedCompoundCaption() {
        long curPosition = mStreamingContext.getTimelineCurrentPosition(mTimeline);
        if (mCurCompoundCaption != null
                && curPosition >= mCurCompoundCaption.getInPoint()
                && curPosition <= mCurCompoundCaption.getOutPoint()) {
            return true;
        }
        return false;
    }

    private boolean isSelectedCaption() {
        long curPosition = mStreamingContext.getTimelineCurrentPosition(mTimeline);
        if (mCurCaption != null
                && curPosition >= mCurCaption.getInPoint()
                && curPosition <= mCurCaption.getOutPoint()) {
            return true;
        }
        return false;
    }

    public boolean curPointIsInnerDrawRect(int xPos, int yPos) {
        return mDrawRect.curPointIsInnerDrawRect(xPos, yPos);
    }

    //???liveWindow?????????????????????
    public boolean selectCompoundCaptionByHandClick(PointF curPoint) {
        if(mTimeline == null){
            return false;
        }
        List<NvsTimelineCompoundCaption> captionList = mTimeline.getCompoundCaptionsByTimelinePosition(mStreamingContext.getTimelineCurrentPosition(mTimeline));
        if (captionList.size() < 1)
            return false;

        boolean result = false;
        for (int j = 0; j < captionList.size(); j++) {
            NvsTimelineCompoundCaption caption = captionList.get(j);
            List<PointF> list = caption.getCompoundBoundingVertices(NvsTimelineCompoundCaption.BOUNDING_TYPE_FRAME);
            List<PointF> newList = getAssetViewVerticesList(list);
            boolean isSelected = mDrawRect.clickPointIsInnerDrawRect(newList,(int) curPoint.x, (int) curPoint.y);
            if (isSelected) {
                mDrawRect.setDrawRectVisible(true);
                mDrawRect.setDrawRect(newList, Constants.EDIT_MODE_COMPOUND_CAPTION);
                mCurCompoundCaption = caption;
                result = true;
                break;
            } else { //??????????????????????????????
                result = false;
                mCurCompoundCaption = null;
            }
        }
        return result;
    }

    private List<PointF> getAssetViewVerticesList(List<PointF> verticesList){
        List<PointF> newList = new ArrayList<>();
        for (int i = 0; i < verticesList.size(); i++) {
            PointF pointF = mLiveWindow.mapCanonicalToView(verticesList.get(i));
            newList.add(pointF);
        }
        return newList;
    }

    /**
     * ??????????????????livewindow??????????????????
     */
    private void setPointFListLiveWindow(int w, int h) {
//        int x0 = Math.abs(w - h) / 2;
        int x0 = 0;
        int x1 = w;
        int y0 = 0;
        int y1 = h;
        pointFListLiveWindow = setFourPointToList(x0, x1, y0, y1);
    }

    public void setDrawRect(List<PointF> newList) {
        mDrawRect.setDrawRect(newList, Constants.EDIT_MODE_WATERMARK);
    }

    public List<PointF> getDrawRect() {
        return mDrawRect.getDrawRect();
    }

    /**
     * ????????????????????????list????????????????????????0123
     */
    private List<PointF> setFourPointToList(float x0, float x1, float y0, float y1) {
        List<PointF> newList = new ArrayList<>();
        newList.add(new PointF(x0, y0));
        newList.add(new PointF(x0, y1));
        newList.add(new PointF(x1, y1));
        newList.add(new PointF(x1, y0));
        return newList;
    }

    public void setDrawRectVisible(int visibility) {
        mDrawRect.setVisibility(visibility);
    }

    public void setDrawRectVisible(boolean show) {
        mDrawRect.bringToFront();
        mDrawRect.setDrawRectVisible(show);
    }

    private boolean checkInLiveWindow(List<PointF> newList) {
        if (pointFListLiveWindow != null) {
            float minX = pointFListLiveWindow.get(0).x;
            float maxX = pointFListLiveWindow.get(2).x;
            float minY = pointFListLiveWindow.get(0).y;
            float maxY = pointFListLiveWindow.get(2).y;
            for (PointF pointF : newList) {
                if (pointF.x < minX || pointF.x > maxX || pointF.y < minY || pointF.y > maxY) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isDrawRectVisible() {
        return mDrawRect.isVisible();
    }

    public NvsTimeline getTimeLine() {
        return mTimeline;
    }

    //?????????????????????????????????????????????????????????
    public interface AssetEditListener {
        void onAssetDelete();

        void onAssetSelected(PointF curPoint);

        void onAssetTranstion();

        void onAssetScale();

        void onAssetAlign(int alignVal);//????????????

        void onAssetHorizFlip(boolean isHorizFlip);//????????????
    }

    //????????????????????????
    public interface VideoCaptionTextEditListener {
        void onCaptionTextEdit();
    }
    //????????????????????????
    public interface OnCompoundCaptionListener {
        void onCaptionIndex(int captionIndex);
    }

    //LiveWindowd????????????
    public interface OnLiveWindowClickListener {
        void onLiveWindowClick();
    }

    //LiveWindowd????????????
    public interface OnStickerMuteListener {
        void onStickerMute();
    }

    public interface OnThemeCaptionSeekListener {
        void onThemeCaptionSeek(long stamp);
    }

    //Fragment??????????????????
    public interface OnFragmentLoadFinisedListener {
        void onLoadFinished();
    }

    public void setThemeCaptionSeekListener(OnThemeCaptionSeekListener themeCaptionSeekListener) {
        mThemeCaptionSeekListener = themeCaptionSeekListener;
    }

    public void setLiveWindowClickListener(OnLiveWindowClickListener liveWindowClickListener) {
        this.mLiveWindowClickListener = liveWindowClickListener;
    }

    public void setCaptionTextEditListener(VideoCaptionTextEditListener captionTextEditListener) {
        this.mCaptionTextEditListener = captionTextEditListener;
    }

    public void setStickerMuteListener(OnStickerMuteListener stickerMuteListener) {
        this.mStickerMuteListener = stickerMuteListener;
    }

    public void setCompoundCaptionListener(OnCompoundCaptionListener compoundCaptionListener) {
        this.mCompoundCaptionListener = compoundCaptionListener;
    }

    public void setAssetEditListener(AssetEditListener assetEditListener) {
        this.mAssetEditListener = assetEditListener;
    }

    public void setFragmentLoadFinisedListener(OnFragmentLoadFinisedListener fragmentLoadFinisedListener) {
        this.mFragmentLoadFinisedListener = fragmentLoadFinisedListener;
    }

    public interface OnPlayProgressChangeListener {
        void onPlayProgressChanged(long curTime);
        void onPlayStateChanged(boolean isPlaying);
    }
}
