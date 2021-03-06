package com.glitchcam.vepromei.edit.animatesticker;

import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.meicam.sdk.NvsAssetPackageManager;
import com.meicam.sdk.NvsMultiThumbnailSequenceView;
import com.meicam.sdk.NvsStreamingContext;
import com.meicam.sdk.NvsTimeline;
import com.meicam.sdk.NvsTimelineAnimatedSticker;
import com.meicam.sdk.NvsVideoClip;
import com.meicam.sdk.NvsVideoTrack;
import com.glitchcam.vepromei.R;
import com.glitchcam.vepromei.base.BaseActivity;
import com.glitchcam.vepromei.download.AssetDownloadActivity;
import com.glitchcam.vepromei.edit.VideoFragment;
import com.glitchcam.vepromei.edit.timelineEditor.NvsTimelineEditor;
import com.glitchcam.vepromei.edit.timelineEditor.NvsTimelineTimeSpan;
import com.glitchcam.vepromei.edit.view.CustomTitleBar;
import com.glitchcam.vepromei.edit.view.CustomViewPager;
import com.glitchcam.vepromei.edit.watermark.SingleClickActivity;
import com.glitchcam.vepromei.utils.AppManager;
import com.glitchcam.vepromei.utils.Constants;
import com.glitchcam.vepromei.utils.Logger;
import com.glitchcam.vepromei.utils.ScreenUtils;
import com.glitchcam.vepromei.utils.TimeFormatUtil;
import com.glitchcam.vepromei.utils.TimelineUtil;
import com.glitchcam.vepromei.utils.ToastUtil;
import com.glitchcam.vepromei.utils.asset.NvAsset;
import com.glitchcam.vepromei.utils.asset.NvAssetManager;
import com.glitchcam.vepromei.utils.dataInfo.KeyFrameInfo;
import com.glitchcam.vepromei.utils.dataInfo.StickerInfo;
import com.glitchcam.vepromei.utils.dataInfo.TimelineData;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author ms
 */
public class AnimateStickerActivity extends BaseActivity {
    private static final String TAG = "AnimateStickerActivity";
    private static final int ANIMATESTICKERREQUESTLIST = 104;
    private static final int VIDEOPLAYTOEOF = 105;
    private CustomTitleBar mTitleBar;
    private RelativeLayout mBottomLayout;
    private RelativeLayout mZoomOutButton;
    private RelativeLayout mZoomInButton;
    private TextView mCurrentPlaytime;
    private ImageView mVideoPlay;
    private NvsTimelineEditor mTimelineEditor;
    private ImageView mAddAnimateStickerButton;
    private ImageView mStickerFinish;

    private RelativeLayout mAnimateStickerAssetLayout;
    private ImageView mMoreDownload;
    private TabLayout mAnimateStickerTypeTab;
    private CustomViewPager mViewPager;
    private ImageView mStickerAssetFinish;

    private NvsStreamingContext mStreamingContext;
    private NvsTimeline mTimeline;
    private VideoFragment mVideoFragment;
    private NvsMultiThumbnailSequenceView mMultiThumbnailSequenceView;
    private boolean mIsSeekTimeline = true;
    private AnimateStickerActivity.AnimateStickerHandler m_handler = new AnimateStickerActivity.AnimateStickerHandler(this);
    private List<AnimateStickerActivity.AnimateStickerTimeSpanInfo> mTimeSpanInfoList = new ArrayList<>();
    private NvsTimelineAnimatedSticker mCurSelectAnimatedSticker;
    ArrayList<StickerInfo> mStickerDataListClone;
    //
    private ArrayList<String> mStickerAssetTypeList;
    /*
     * ??????????????????
     * List of total stickers
     * */
    private ArrayList<NvAsset> mTotalStickerAssetList;
    /*
     * ?????????????????????
     * Custom sticker list
     * */
    private ArrayList<NvAssetManager.NvCustomStickerInfo> mCustomStickerAssetList;
    private ArrayList<AnimateStickerListFragment> mAssetFragmentsArray = new ArrayList<>();
    private AnimateStickerListFragment mStickerListFragment;
    private AnimateStickerListFragment mCustomStickerListFragment;
    private NvAssetManager mAssetManager;
    private int mAssetType = NvAsset.ASSET_ANIMATED_STICKER;
    private long mInPoint = 0;
    private long mStickerDuration = 0;
    /*
     * ????????????tab???
     * Record the current tab page
     * */
    private int mCurTabPage = 0;
    /*
     * ?????????????????????Tab
     * Record the last operation tab
     * */
    private int mPrevTabPage = 0;

    /*
     * ?????????????????????
     * Newly added sticker object
     * */
    private NvsTimelineAnimatedSticker mAddAnimateSticker = null;
    private int mCurSelectedPos = -1;
    private boolean isNewStickerUuidItemClick = false;
    private String mSelectUuid = "";
    private int mCurAnimateStickerZVal = 0;
    private StringBuilder mShowCurrentDuration = new StringBuilder();
    private ImageView mStickerKeyFrameButton;
    private ImageView mRemoveAllKeyFrameButton;
    private LinearLayout mFrameOperationWrapperLayout;
    private ImageView mKeyFrameFinishView;
    private TextView mBeforeKeyFrameView;
    private TextView mAddDeleteKeyFrameView;
    private TextView mNextKeyFrameView;
    private boolean mCurrentStatusIsKeyFrame = false;

    static class AnimateStickerHandler extends Handler {
        WeakReference<AnimateStickerActivity> mWeakReference;

        public AnimateStickerHandler(AnimateStickerActivity activity) {
            mWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final AnimateStickerActivity activity = mWeakReference.get();
            if (activity != null) {
                switch (msg.what) {
                    case VIDEOPLAYTOEOF:
                        activity.resetView();
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /*
     * ?????????timeSpan??????????????????????????????TimeSpan
     * Stickers and timeSpan class, store added stickers and TimeSpan
     * */
    private class AnimateStickerTimeSpanInfo {
        public NvsTimelineAnimatedSticker mAnimateSticker;
        public NvsTimelineTimeSpan mTimeSpan;

        public AnimateStickerTimeSpanInfo(NvsTimelineAnimatedSticker sticker, NvsTimelineTimeSpan timeSpan) {
            this.mAnimateSticker = sticker;
            this.mTimeSpan = timeSpan;
        }
    }

    @Override
    protected int initRootView() {
        mStreamingContext = NvsStreamingContext.getInstance();
        return R.layout.activity_animate_sticker;
    }

    @Override
    protected void initViews() {
        mTitleBar = (CustomTitleBar) findViewById(R.id.title_bar);
        mBottomLayout = (RelativeLayout) findViewById(R.id.bottom_layout);
        mZoomOutButton = (RelativeLayout) findViewById(R.id.zoomOut);
        mZoomInButton = (RelativeLayout) findViewById(R.id.zoomIn);
        mCurrentPlaytime = (TextView) findViewById(R.id.currentPlaytime);
        mVideoPlay = (ImageView) findViewById(R.id.videoPlay);
        mTimelineEditor = (NvsTimelineEditor) findViewById(R.id.timelineEditor);
        mAddAnimateStickerButton = (ImageView) findViewById(R.id.addAnimateStickerButton);
        mStickerFinish = (ImageView) findViewById(R.id.stickerFinish);
        mMultiThumbnailSequenceView = mTimelineEditor.getMultiThumbnailSequenceView();

        mAnimateStickerAssetLayout = (RelativeLayout) findViewById(R.id.animateStickerAsset_layout);
        mMoreDownload = (ImageView) findViewById(R.id.moreDownload);
        mAnimateStickerTypeTab = (TabLayout) findViewById(R.id.animateStickerTypeTab);
        mViewPager = (CustomViewPager) findViewById(R.id.viewPager);
        mViewPager.setPagingEnabled(false);
        mStickerAssetFinish = (ImageView) findViewById(R.id.stickerAssetFinish);
        // keyFrame ?????????
        mStickerKeyFrameButton = findViewById(R.id.animateStickerKeyFrameButton);
        mStickerKeyFrameButton.setOnClickListener(this);
        mRemoveAllKeyFrameButton = findViewById(R.id.removeAllKeyFrameButton);
        mRemoveAllKeyFrameButton.setOnClickListener(this);
        mFrameOperationWrapperLayout = findViewById(R.id.key_frame_operation_wrapper_layout);
        mKeyFrameFinishView = findViewById(R.id.keyFrameFinishView);
        mKeyFrameFinishView.setOnClickListener(this);
        mBeforeKeyFrameView = findViewById(R.id.before_key_frame_view);
        mBeforeKeyFrameView.setOnClickListener(this);
        mAddDeleteKeyFrameView = findViewById(R.id.add_delete_frame_view);
        mAddDeleteKeyFrameView.setTag(0);
        mAddDeleteKeyFrameView.setOnClickListener(this);
        mNextKeyFrameView = findViewById(R.id.next_key_frame_view);
        mNextKeyFrameView.setOnClickListener(this);
    }

    private void setAddDeleteViewStatus(boolean isAddStatus) {
        if (isAddStatus) {
            mAddDeleteKeyFrameView.setTag(0);
            mAddDeleteKeyFrameView.setText(R.string.key_frame_add_frame_text);
            mAddDeleteKeyFrameView.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.key_frame_add_frame_selector), null, null);
        } else {
            mAddDeleteKeyFrameView.setTag(1);
            mAddDeleteKeyFrameView.setText(R.string.key_frame_delete_frame_text);
            mAddDeleteKeyFrameView.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.key_frame_delete_frame_selector), null, null);
        }
    }


    @Override
    protected void initTitle() {
        mTitleBar.setTextCenter(R.string.animatedSticker);
        mTitleBar.setBackImageVisible(View.GONE);
    }

    private void changeAddAndFinishViewStatus(int visibility) {
        mAddAnimateStickerButton.setVisibility(visibility);
        mStickerFinish.setVisibility(visibility);
    }

    @Override
    protected void initData() {
        if (!initAssetData()) {
            return;
        }
        setPlaytimeText(0);
        initMultiSequence();
        /*
         * ??????????????????
         * Add all stickers
         * */
        addAllTimeSpan();
        initVideoFragment();

        /*
         * ?????????????????????
         * Initialize material list
         * */
        initAnimateStickerDataList();
        initCustomAssetsDataList();
        initTabLayout();
        /*
         * gif???caf???????????????????????????
         * gif to caf chart needs this sticker template
         * */
        gifToCafStickerTemplateinstall();
    }

    @Override
    protected void initListener() {
        mZoomOutButton.setOnClickListener(this);
        mZoomInButton.setOnClickListener(this);
        mVideoPlay.setOnClickListener(this);
        mAddAnimateStickerButton.setOnClickListener(this);
        mStickerFinish.setOnClickListener(this);
        mMoreDownload.setOnClickListener(this);
        mStickerAssetFinish.setOnClickListener(this);
        mTimelineEditor.setOnScrollListener(new NvsTimelineEditor.OnScrollChangeListener() {
            @Override
            public void onScrollX(long timeStamp) {
                // ???????????????????????????
                if ((mCurSelectAnimatedSticker != null) && mCurrentStatusIsKeyFrame) {
                    Map<Long, KeyFrameInfo> keyFrameInfoHashMap = getCurrentStickerInfo().getKeyFrameInfoHashMap();
                    updateAllKeyFrameViewStatusWhenScroll(timeStamp, keyFrameInfoHashMap);
                    // ??????????????????
                    mVideoFragment.setDrawRectVisible(View.GONE);
                }
                // ????????????????????????????????????[???:??????????????? ?????????????????????]???????????????????????????????????????
                if ((mCurSelectAnimatedSticker != null) && !mCurrentStatusIsKeyFrame) {
                    StickerInfo currentStickerInfo = getCurrentStickerInfo();
                    if (currentStickerInfo != null) {
                        if (mIsSeekTimeline) {
                            mVideoFragment.setDrawRectVisible(View.VISIBLE);
                        } else {
                            mVideoFragment.setDrawRectVisible(View.INVISIBLE);
                        }
                    }
                }
                // ???????????????????????????
                if (!mCurrentStatusIsKeyFrame) {
                    // ???????????? ???????????????????????????
                    updateAllStickerPos();
                }
                // ????????????????????????????????????
                if (!mIsSeekTimeline) {
                    return;
                }
                // seek ??????????????????
                if (mCurSelectAnimatedSticker != null) {
                    Map<Long, KeyFrameInfo> keyFrameInfoHashMap = getCurrentStickerInfo().getKeyFrameInfoHashMap();
                    if (timeStamp > mCurSelectAnimatedSticker.getOutPoint() || timeStamp < mCurSelectAnimatedSticker.getInPoint()) {
                        // ????????????????????????????????? ???????????????????????????
                        mVideoFragment.setDrawRectVisible(View.GONE);
                        mAddDeleteKeyFrameView.setEnabled(false);
                        mBeforeKeyFrameView.setEnabled(false);
                        mNextKeyFrameView.setEnabled(false);
                    } else {
                        // seek??????????????????????????????????????????????????????????????????????????????????????????????????????????????????.
                        boolean hasKeyFrame = false;
                        long currentKeyFrameStamp = -1;
                        Set<Map.Entry<Long, KeyFrameInfo>> entries = keyFrameInfoHashMap.entrySet();
                        for (Map.Entry<Long,KeyFrameInfo> entry : entries) {
                            if (timeStamp >= entry.getKey() - 100000 && timeStamp <= entry.getKey() + 100000) {
                                hasKeyFrame = true;
                                currentKeyFrameStamp = entry.getKey();
                                break;
                            }
                        }
                        if (hasKeyFrame) {
                            mCurSelectAnimatedSticker.setCurrentKeyFrameTime(currentKeyFrameStamp - mCurSelectAnimatedSticker.getInPoint());
                            mVideoFragment.setDrawRectVisible(View.VISIBLE);
                            updateStickerBoundingRect();
                        } else {
                            // ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                            mCurSelectAnimatedSticker.setCurrentKeyFrameTime(timeStamp - mCurSelectAnimatedSticker.getInPoint());
                            mCurSelectAnimatedSticker.setFloatValAtTime("Sticker TransX", mCurSelectAnimatedSticker.getTranslation().x, timeStamp - mCurSelectAnimatedSticker.getInPoint());
                            mCurSelectAnimatedSticker.setFloatValAtTime("Sticker TransY", mCurSelectAnimatedSticker.getTranslation().y, timeStamp - mCurSelectAnimatedSticker.getInPoint());
                            mCurSelectAnimatedSticker.setFloatValAtTime("Sticker Scale", mCurSelectAnimatedSticker.getScale(), timeStamp - mCurSelectAnimatedSticker.getInPoint());
                            mCurSelectAnimatedSticker.setFloatValAtTime("Sticker RotZ", mCurSelectAnimatedSticker.getRotationZ(), timeStamp - mCurSelectAnimatedSticker.getInPoint());
                            updateStickerBoundingRect();
                            boolean removeStickerTransXSuccess = mCurSelectAnimatedSticker.removeKeyframeAtTime("Sticker TransX", timeStamp - mCurSelectAnimatedSticker.getInPoint());
                            boolean removeStickerTransYSuccess = mCurSelectAnimatedSticker.removeKeyframeAtTime("Sticker TransY", timeStamp - mCurSelectAnimatedSticker.getInPoint());
                            boolean removeStickerScale = mCurSelectAnimatedSticker.removeKeyframeAtTime("Sticker Scale", timeStamp - mCurSelectAnimatedSticker.getInPoint());
                            boolean removeStickerRotZ = mCurSelectAnimatedSticker.removeKeyframeAtTime("Sticker RotZ", timeStamp - mCurSelectAnimatedSticker.getInPoint());
                            if (removeStickerTransXSuccess || removeStickerTransYSuccess || removeStickerScale || removeStickerRotZ) {
                                Log.d(TAG, "onScrollX  removeKeyframeAtTime success");
                            }
                        }
                        updateStickerBoundingRect();
                        mAddDeleteKeyFrameView.setEnabled(true);
                        mVideoFragment.setDrawRectVisible(View.VISIBLE);
                    }
                }
                // Seeking ??????
                if (mTimeline != null) {
                    seekTimeline((mAnimateStickerAssetLayout.getVisibility() == View.VISIBLE) ? mInPoint : timeStamp);
                    setPlaytimeText(timeStamp);
                    selectAnimateStickerAndTimeSpan();
                }
            }
        });
        mMultiThumbnailSequenceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mIsSeekTimeline = true;
                return false;
            }
        });

        mVideoFragment.setVideoFragmentCallBack(new VideoFragment.VideoFragmentListener() {
            @Override
            public void playBackEOF(NvsTimeline timeline) {
                m_handler.sendEmptyMessage(VIDEOPLAYTOEOF);
            }

            @Override
            public void playStopped(NvsTimeline timeline) {
                if (isNewStickerUuidItemClick) {
                    return;
                }
                updateStickerBoundingRect();
                selectAnimateStickerAndTimeSpan();
            }

            @Override
            public void playbackTimelinePosition(NvsTimeline timeline, long stamp) {
                mVideoFragment.setDrawRectVisible(View.GONE);
                if (mAnimateStickerAssetLayout.getVisibility() != View.VISIBLE) {
                    setPlaytimeText(stamp);
                    if (!mCurrentStatusIsKeyFrame) {
                        mTimelineEditor.unSelectAllTimeSpan();
                    }
                    multiThumbnailSequenceViewSmooth(stamp);
                }
            }

            @Override
            public void streamingEngineStateChanged(int state) {
                if (state == NvsStreamingContext.STREAMING_ENGINE_STATE_PLAYBACK) {
                    mVideoPlay.setBackgroundResource(R.mipmap.icon_edit_pause);
                    mIsSeekTimeline = false;
                    mAssetFragmentsArray.get(mCurTabPage).setIsStickerInPlay(true);
                } else {
                    mVideoPlay.setBackgroundResource(R.mipmap.icon_edit_play);
                    mIsSeekTimeline = true;
                    int tapCount = mAssetFragmentsArray.size();
                    for (int index = 0; index < tapCount; ++index) {
                        mAssetFragmentsArray.get(index).setIsStickerInPlay(false);
                    }
                    selectAnimateStickerAndTimeSpan();
                }
                if (mAnimateStickerAssetLayout.getVisibility() == View.VISIBLE) {
                    int tapCount = mAssetFragmentsArray.size();
                    for (int index = 0; index < tapCount; ++index) {
                        mAssetFragmentsArray.get(index).notifyDataSetChanged();
                    }
                }
            }
        });
        mVideoFragment.setBeforeAnimateStickerEditListener(new VideoFragment.IBeforeAnimateStickerEditListener() {
            @Override
            public boolean beforeTransitionCouldDo() {
                if (mCurSelectAnimatedSticker == null) {
                    return false;
                }
                if (!mCurrentStatusIsKeyFrame) {
                    boolean b = ifCouldEditAnimateSticker();
                    if (!b) {
                        return false;
                    } else {
                        return true;
                    }
                }
                return true;
            }

            @Override
            public boolean beforeScaleCouldDo() {
                if (mCurSelectAnimatedSticker == null) {
                    return false;
                }
                if (!mCurrentStatusIsKeyFrame) {
                    boolean b = ifCouldEditAnimateSticker();
                    if (!b) {
                        return false;
                    } else {
                        return true;
                    }
                }
                return true;
            }
        });
        mVideoFragment.setAssetEditListener(new VideoFragment.AssetEditListener() {
            @Override
            public void onAssetDelete() {
                if (mCurrentStatusIsKeyFrame) {
                    ToastUtil.showToastCenter(getApplicationContext(), getResources().getString(R.string.tips_when_delete_animate_sticker));
                    return;
                }
                int zVal = (int) mCurSelectAnimatedSticker.getZValue();
                int stickerIndex = getAnimateStickerIndex(zVal);
                if (stickerIndex >= 0) {
                    // ???????????????????????? ????????????????????????
                    mStickerDataListClone.remove(stickerIndex);
                }
                mAddAnimateSticker = null;
                deleteAnimateSticker();

                /*
                 * ????????????Tab????????????????????????
                 * Deselect all tab page stickers
                 * */
                mCurSelectedPos = -1;
                int tabCount = mAssetFragmentsArray.size();
                for (int index = 0; index < tabCount; ++index) {
                    mAssetFragmentsArray.get(index).setSelectedPos(mCurSelectedPos);
                    mAssetFragmentsArray.get(index).notifyDataSetChanged();
                }
                // ?????????????????????????????????????????? ???????????????????????????????????????
                mAddDeleteKeyFrameView.setEnabled(false);
                mBeforeKeyFrameView.setEnabled(false);
                mNextKeyFrameView.setEnabled(false);
            }

            @Override
            public void onAssetSelected(PointF curPoint) {
                if ((mAnimateStickerAssetLayout.getVisibility() == View.VISIBLE) || mCurrentStatusIsKeyFrame) {
                    return;
                }
                mVideoFragment.selectAnimateStickerByHandClick(curPoint);
                mCurSelectAnimatedSticker = mVideoFragment.getCurAnimateSticker();
                mVideoFragment.updateAnimateStickerCoordinate(mCurSelectAnimatedSticker);
                updateStickerMuteVisible();
                mVideoFragment.changeStickerRectVisible();
                selectTimeSpan();
            }

            // ????????????
            @Override
            public void onAssetTranstion() {
                if (mCurSelectAnimatedSticker == null) {
                    return;
                }
                if (mCurrentStatusIsKeyFrame) {
                    updateOrAddKeyFrameInfors();
                }
                int zVal = (int) mCurSelectAnimatedSticker.getZValue();
                int index = getAnimateStickerIndex(zVal);
                if (index >= 0) {
                    mStickerDataListClone.get(index).setTranslation(mCurSelectAnimatedSticker.getTranslation());
                }
            }

            // ????????????
            @Override
            public void onAssetScale() {
                if (mCurSelectAnimatedSticker == null) {
                    return;
                }
                if (mCurrentStatusIsKeyFrame) {
                    updateOrAddKeyFrameInfors();
                }
                int zVal = (int) mCurSelectAnimatedSticker.getZValue();
                int index = getAnimateStickerIndex(zVal);
                if (index >= 0) {
                    mStickerDataListClone.get(index).setTranslation(mCurSelectAnimatedSticker.getTranslation());
                    mStickerDataListClone.get(index).setScaleFactor(mCurSelectAnimatedSticker.getScale());
                    mStickerDataListClone.get(index).setRotation(mCurSelectAnimatedSticker.getRotationZ());
                }
            }

            @Override
            public void onAssetAlign(int alignVal) {

            }

            @Override
            public void onAssetHorizFlip(boolean isHorizFlip) {
                if (mCurSelectAnimatedSticker == null) {
                    return;
                }
                int zVal = (int) mCurSelectAnimatedSticker.getZValue();
                int index = getAnimateStickerIndex(zVal);
                if (index >= 0) {
                    mStickerDataListClone.get(index).setHorizFlip(mCurSelectAnimatedSticker.getHorizontalFlip());
                }
            }
        });
        mVideoFragment.setLiveWindowClickListener(new VideoFragment.OnLiveWindowClickListener() {
            @Override
            public void onLiveWindowClick() {
                isNewStickerUuidItemClick = false;
            }
        });

        mVideoFragment.setStickerMuteListener(new VideoFragment.OnStickerMuteListener() {
            @Override
            public void onStickerMute() {
                if (mCurSelectAnimatedSticker == null) {
                    return;
                }
                float volumeGain = mCurSelectAnimatedSticker.getVolumeGain().leftVolume;
                int zVal = (int) mCurSelectAnimatedSticker.getZValue();
                int index = getAnimateStickerIndex(zVal);
                if (index >= 0) {
                    mStickerDataListClone.get(index).setVolumeGain(volumeGain);
                }
            }
        });

        mAnimateStickerAssetLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
    }

    private void updateAllStickerPos() {
        long curPos = mStreamingContext.getTimelineCurrentPosition(mTimeline);
        List<NvsTimelineAnimatedSticker> animateStickerList = mTimeline.getAnimatedStickersByTimelinePosition(curPos);
        for (NvsTimelineAnimatedSticker nvsTimelineAnimatedSticker : animateStickerList) {
            int zVal = (int) nvsTimelineAnimatedSticker.getZValue();
            int index = getAnimateStickerIndex(zVal);
            if (index >= 0) {
                StickerInfo stickerInfo = mStickerDataListClone.get(index);
                Map<Long, KeyFrameInfo> keyFrameInfoHashMap = stickerInfo.getKeyFrameInfoHashMap();
                if (!keyFrameInfoHashMap.isEmpty()) {
                    // ??????????????????
                    nvsTimelineAnimatedSticker.setCurrentKeyFrameTime(curPos - nvsTimelineAnimatedSticker.getInPoint());
                    Set<Long> longs = keyFrameInfoHashMap.keySet();
                    if (longs.contains(curPos)) {
                        // ????????????
                    } else {
                        nvsTimelineAnimatedSticker.removeKeyframeAtTime("Sticker TransX", curPos - nvsTimelineAnimatedSticker.getInPoint());
                        nvsTimelineAnimatedSticker.removeKeyframeAtTime("Sticker TransY", curPos - nvsTimelineAnimatedSticker.getInPoint());
                        nvsTimelineAnimatedSticker.removeKeyframeAtTime("Sticker Scale", curPos - nvsTimelineAnimatedSticker.getInPoint());
                        nvsTimelineAnimatedSticker.removeKeyframeAtTime("Sticker RotZ", curPos - nvsTimelineAnimatedSticker.getInPoint());
                    }
                }
            }
        }
    }

    // ??????????????????????????????????????????????????????????????? ??????????????? ?????????????????????????????????????????????
    private void updateOrAddKeyFrameInfors() {
        StickerInfo currentStickerInfo = getCurrentStickerInfo();
        Map<Long, KeyFrameInfo> keyFrameInfoHashMap = currentStickerInfo.getKeyFrameInfoHashMap();
        Set<Map.Entry<Long, KeyFrameInfo>> entries = keyFrameInfoHashMap.entrySet();
        long currentTimelinePos = mStreamingContext.getTimelineCurrentPosition(mTimeline);
        if (keyFrameInfoHashMap.isEmpty()) {
            // ??????????????? ???????????????????????????
            mAddDeleteKeyFrameView.performClick();
            return;
        }
        boolean hasKeyFrame = false;
        long currentKeyFrameStamp = -1;
        for (Map.Entry<Long, KeyFrameInfo> entry : entries) {
            if (currentTimelinePos > (entry.getKey() - 100000) && currentTimelinePos < (entry.getKey() + 100000)) {
                hasKeyFrame = true;
                currentKeyFrameStamp = entry.getKey();
                break;
            }
        }
        if (hasKeyFrame) {
            // ??????????????????????????????????????????
            currentStickerInfo.putKeyFrameInfo(currentKeyFrameStamp, generateKeyFrameInfo(mCurSelectAnimatedSticker));
        } else {
            // ??????????????????????????? ???????????????????????????
            mAddDeleteKeyFrameView.performClick();
        }
    }

    private void updateAllKeyFrameViewStatusWhenScroll(long timeStamp, Map<Long,KeyFrameInfo> keyFrameInfoHashMap) {
        boolean isHasKeyFrame = !keyFrameInfoHashMap.isEmpty();
        if (isHasKeyFrame) {
            Set<Map.Entry<Long, KeyFrameInfo>> entries = keyFrameInfoHashMap.entrySet();
            Set<Long> keyFrameKeySet = keyFrameInfoHashMap.keySet();
            Object[] objects = keyFrameKeySet.toArray();
            // before
            long beforeKeyFrame = -1;
            for (Map.Entry<Long, KeyFrameInfo> entry : entries) {
                Long key = entry.getKey();
                if (key < timeStamp) {
                    // ???????????????????????? ??????????????????????????????
                    beforeKeyFrame = key;
                }
            }
            if (beforeKeyFrame == -1 || ((objects != null) && ((long) (objects[0]) == timeStamp))) {
                mBeforeKeyFrameView.setEnabled(false);
            } else {
                mBeforeKeyFrameView.setEnabled(true);
            }

            // next
            long nextKeyFrame = -1;
            for (Map.Entry<Long, KeyFrameInfo> entry : entries) {
                Long key = entry.getKey();
                if (key > timeStamp) {
                    // ???????????????????????? ??????????????????????????????
                    nextKeyFrame = key;
                    break;
                }
            }

            if (nextKeyFrame == -1 || ((objects != null) && ((long) (objects[objects.length - 1]) == timeStamp))) {
                mNextKeyFrameView.setEnabled(false);
            } else {
                mNextKeyFrameView.setEnabled(true);
            }

            // add delete
            boolean hasKeyFrame = false;
            for (Map.Entry<Long, KeyFrameInfo> entry : entries) {
                if (timeStamp >= entry.getKey() - 100000 && timeStamp <= entry.getKey() + 100000) {
                    hasKeyFrame = true;
                    break;
                }
            }
            setAddDeleteViewStatus(!hasKeyFrame);
            // keyFramePoint
            NvsTimelineTimeSpan currentTimeSpan = getCurrentTimeSpan();
            if (currentTimeSpan != null) {
                currentTimeSpan.setCurrentTimelinePosition(timeStamp);
            }
        } else {
            mBeforeKeyFrameView.setEnabled(false);
            mNextKeyFrameView.setEnabled(false);
            setAddDeleteViewStatus(true);
        }
    }

    private StickerInfo getCurrentStickerInfo() {
        if (mCurSelectAnimatedSticker == null) {
            return null;
        }
        int zVal = (int) mCurSelectAnimatedSticker.getZValue();
        int index = getAnimateStickerIndex(zVal);
        if (index >= 0) {
            return mStickerDataListClone.get(index);
        }
        return null;
    }

    private KeyFrameInfo generateKeyFrameInfo(NvsTimelineAnimatedSticker sticker) {
        return new KeyFrameInfo().setScaleX(sticker.getScale())
                .setScaleY(sticker.getScale())
                .setRotationZ(sticker.getRotationZ())
                .setTranslation(sticker.getTranslation());
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            // ?????????
            case R.id.next_key_frame_view:
                if (mCurSelectAnimatedSticker != null) {
                    mVideoFragment.stopEngine();
                    StickerInfo currentStickerInfo = getCurrentStickerInfo();
                    Map<Long, KeyFrameInfo> keyFrameInfoHashMap = currentStickerInfo.getKeyFrameInfoHashMap();
                    Set<Map.Entry<Long, KeyFrameInfo>> entries = keyFrameInfoHashMap.entrySet();
                    long currentTimelinePosition = mStreamingContext.getTimelineCurrentPosition(mTimeline);
                    long nextKeyFrame = -1;
                    for (Map.Entry<Long, KeyFrameInfo> entry : entries) {
                        Long key = entry.getKey();
                        if (key > currentTimelinePosition) {
                            nextKeyFrame = key;
                            break;
                        }
                    }
                    if (nextKeyFrame == -1) {
                        mNextKeyFrameView.setEnabled(false);
                    } else {
                        mNextKeyFrameView.setEnabled(true);
                        seekTimeline(nextKeyFrame);
                        seekMultiThumbnailSequenceView();
                    }
                }
                break;
            // ?????????
            case R.id.before_key_frame_view:
                if (mCurSelectAnimatedSticker != null) {
                    mVideoFragment.stopEngine();
                    StickerInfo currentStickerInfo = getCurrentStickerInfo();
                    Map<Long, KeyFrameInfo> keyFrameInfoHashMap = currentStickerInfo.getKeyFrameInfoHashMap();
                    Set<Map.Entry<Long, KeyFrameInfo>> entries = keyFrameInfoHashMap.entrySet();
                    long currentTimelinePosition = mStreamingContext.getTimelineCurrentPosition(mTimeline);
                    long nextKeyFrame = -1;
                    for (Map.Entry<Long, KeyFrameInfo> entry : entries) {
                        Long key = entry.getKey();
                        if (key < currentTimelinePosition) {
                            nextKeyFrame = key;
                        }
                    }
                    if (nextKeyFrame == -1) {
                        mBeforeKeyFrameView.setEnabled(false);
                    } else {
                        mBeforeKeyFrameView.setEnabled(true);
                        seekTimeline(nextKeyFrame);
                        seekMultiThumbnailSequenceView();
                    }
                }
                break;
            // ????????????????????????
            case R.id.removeAllKeyFrameButton:
                if (mCurSelectAnimatedSticker != null) {
                    mCurSelectAnimatedSticker.removeAllKeyframe("Sticker TransX");
                    mCurSelectAnimatedSticker.removeAllKeyframe("Sticker TransY");
                    mCurSelectAnimatedSticker.removeAllKeyframe("Sticker Scale");
                    mCurSelectAnimatedSticker.removeAllKeyframe("Sticker RotZ");
                    mRemoveAllKeyFrameButton.setVisibility(View.INVISIBLE);
                    StickerInfo currentStickerInfo = getCurrentStickerInfo();
                    if (currentStickerInfo != null) {
                        // ??????????????????????????????????????????
                        currentStickerInfo.getKeyFrameInfoHashMap().clear();
                    }
                    NvsTimelineTimeSpan currentTimeSpan = getCurrentTimeSpan();
                    if (currentTimeSpan != null) {
                        currentTimeSpan.setKeyFrameInfo(currentStickerInfo.getKeyFrameInfoHashMap());
                    }
                    updateStickerKeyFrameButtonBackground();
                }
                break;
            // ??????/???????????????
            case R.id.add_delete_frame_view:
                if ((int) (v.getTag()) == 0) {
                    //add
                    if (mCurSelectAnimatedSticker != null) {
                        mVideoFragment.stopEngine();
                        // ???????????????????????????????????????????????????????????????
                        mCurSelectAnimatedSticker.setCurrentKeyFrameTime(mStreamingContext.getTimelineCurrentPosition(mTimeline) - mCurSelectAnimatedSticker.getInPoint());
                        StickerInfo currentStickerInfo = getCurrentStickerInfo();
                        if (currentStickerInfo != null) {
                            // ??????????????????????????????????????????
                            currentStickerInfo.putKeyFrameInfo(mStreamingContext.getTimelineCurrentPosition(mTimeline), generateKeyFrameInfo(mCurSelectAnimatedSticker));
                        }
                        NvsTimelineTimeSpan currentTimeSpan = getCurrentTimeSpan();
                        if (currentTimeSpan != null) {
                            currentTimeSpan.setKeyFrameInfo(currentStickerInfo.getKeyFrameInfoHashMap());
                        }
                        updateStickerBoundingRect();
                    }
                    // ?????????????????????
                    mIsSeekTimeline = true;
                    seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline));
                    seekMultiThumbnailSequenceView();
                    setAddDeleteViewStatus(false);
                } else {
                    //delete
                    if (mCurSelectAnimatedSticker != null) {
                        mVideoFragment.stopEngine();
                        StickerInfo currentStickerInfo = getCurrentStickerInfo();
                        if (currentStickerInfo != null) {
                            Map<Long, KeyFrameInfo> keyFrameInfoHashMap = currentStickerInfo.getKeyFrameInfoHashMap();
                            long timelineCurrentPosition = mStreamingContext.getTimelineCurrentPosition(mTimeline);
                            Set<Long> longs = keyFrameInfoHashMap.keySet();
                            for (Long aLong : longs) {
                                if (timelineCurrentPosition >= aLong - 100000 && timelineCurrentPosition <= aLong + 100000) {
                                    keyFrameInfoHashMap.get(aLong);
                                    keyFrameInfoHashMap.remove(aLong);
                                    boolean sticker_transX = mCurSelectAnimatedSticker.removeKeyframeAtTime("Sticker TransX", aLong - mCurSelectAnimatedSticker.getInPoint());
                                    Log.d(TAG, sticker_transX ? "sticker_transX success" : "sticker_transX failed");
                                    boolean sticker_transY = mCurSelectAnimatedSticker.removeKeyframeAtTime("Sticker TransY", aLong - mCurSelectAnimatedSticker.getInPoint());
                                    Log.d(TAG, sticker_transY ? "sticker_transY success" : "sticker_transY failed");
                                    boolean sticker_scale = mCurSelectAnimatedSticker.removeKeyframeAtTime("Sticker Scale", aLong - mCurSelectAnimatedSticker.getInPoint());
                                    Log.d(TAG, sticker_scale ? "sticker_scale success" : "sticker_scale failed");
                                    boolean sticker_rotZ = mCurSelectAnimatedSticker.removeKeyframeAtTime("Sticker RotZ", aLong - mCurSelectAnimatedSticker.getInPoint());
                                    Log.d(TAG, sticker_rotZ ? "sticker_rotZ success" : "sticker_rotZ failed");
                                    break;
                                }
                            }
                            NvsTimelineTimeSpan currentTimeSpan = getCurrentTimeSpan();
                            if (currentTimeSpan != null) {
                                currentTimeSpan.setKeyFrameInfo(currentStickerInfo.getKeyFrameInfoHashMap());
                            }
                            seekTimeline(timelineCurrentPosition);
                            updateStickerBoundingRect();
                        }
                    }
                    setAddDeleteViewStatus(true);
                }

                break;
            // ?????????????????????
            case R.id.keyFrameFinishView:
                mStreamingContext.stop();
                changeAddAndFinishViewStatus(View.VISIBLE);
                mFrameOperationWrapperLayout.setVisibility(View.INVISIBLE);
                mCurrentStatusIsKeyFrame = false;
                NvsTimelineTimeSpan currentTimeSpanView = getCurrentTimeSpan();
                if (currentTimeSpanView != null) {
                    currentTimeSpanView.setKeyFrameInfo(null);
                }
                // ???????????????????????????????????????
                if (mCurSelectAnimatedSticker != null) {
                    if ((mCurSelectAnimatedSticker.getOutPoint() < mStreamingContext.getTimelineCurrentPosition(mTimeline))
                            || mCurSelectAnimatedSticker.getInPoint() > mStreamingContext.getTimelineCurrentPosition(mTimeline)) {
                        mStickerKeyFrameButton.setVisibility(View.INVISIBLE);
                        mRemoveAllKeyFrameButton.setVisibility(View.INVISIBLE);
                        mVideoFragment.setDrawRectVisible(View.GONE);
                        mTimelineEditor.unSelectAllTimeSpan();
                    } else {
                        mStickerKeyFrameButton.setVisibility(View.VISIBLE);
                        updateStickerKeyFrameButtonBackground();
                        ifShowRemoveAllKeyFrameView();
                        mVideoFragment.setDrawRectVisible(View.VISIBLE);
                    }
                } else {
                    mTimelineEditor.unSelectAllTimeSpan();
                    mVideoFragment.setDrawRectVisible(View.GONE);
                    mStickerKeyFrameButton.setVisibility(View.INVISIBLE);
                    mRemoveAllKeyFrameButton.setVisibility(View.INVISIBLE);
                }
                break;
            // ?????????????????????
            case R.id.animateStickerKeyFrameButton:
                mStreamingContext.stop();
                changeAddAndFinishViewStatus(View.INVISIBLE);
                mFrameOperationWrapperLayout.setVisibility(View.VISIBLE);
                mCurrentStatusIsKeyFrame = true;
                NvsTimelineTimeSpan currentTimeSpan = getCurrentTimeSpan();
                StickerInfo currentStickerInfo = getCurrentStickerInfo();
                if (currentStickerInfo != null) {
                    currentTimeSpan.setKeyFrameInfo(currentStickerInfo.getKeyFrameInfoHashMap());
                    mVideoFragment.setDrawRectVisible(View.VISIBLE);
                    mIsSeekTimeline = true;
                    // ????????????????????????1/4
                    seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline) + 10000);
                    multiThumbnailSequenceViewSmooth(mStreamingContext.getTimelineCurrentPosition(mTimeline) + 10000);
                }
                break;
            case R.id.zoomIn:
                mIsSeekTimeline = false;
                mTimelineEditor.ZoomInSequence();
                break;
            case R.id.zoomOut:
                mIsSeekTimeline = false;
                mTimelineEditor.ZoomOutSequence();
                break;
            case R.id.videoPlay:
                playVideo();
                break;
            case R.id.addAnimateStickerButton:
                mVideoFragment.stopEngine();
                mInPoint = mStreamingContext.getTimelineCurrentPosition(mTimeline);
                mStickerDuration = 4 * Constants.NS_TIME_BASE;
                long duration = mTimeline.getDuration();
                long outPoint = mInPoint + mStickerDuration;
                if (outPoint > duration) {
                    mStickerDuration = duration - mInPoint;
                    if (mStickerDuration <= Constants.NS_TIME_BASE) {
                        mStickerDuration = Constants.NS_TIME_BASE;
                        mInPoint = duration - mStickerDuration;
                        if (duration <= Constants.NS_TIME_BASE) {
                            mStickerDuration = duration;
                            mInPoint = 0;
                        }
                    }
                }
                if (mCurSelectAnimatedSticker != null) {
                    mCurAnimateStickerZVal = (int) mCurSelectAnimatedSticker.getZValue();
                }
                mAnimateStickerAssetLayout.setVisibility(View.VISIBLE);
                mVideoFragment.setDrawRectVisible(View.GONE);
                break;
            case R.id.stickerFinish:
                // ??????????????????
                TimelineData.instance().setStickerData(mStickerDataListClone);
                mVideoFragment.stopEngine();
                removeTimeline();
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                AppManager.getInstance().finishActivity();
                break;
            case R.id.moreDownload:
                mStreamingContext.stop();
                mMoreDownload.setClickable(false);
                Bundle bundle = new Bundle();
                bundle.putInt("titleResId", R.string.moreAnimatedSticker);
                bundle.putInt("assetType", NvAsset.ASSET_ANIMATED_STICKER);
                AppManager.getInstance().jumpActivityForResult(AppManager.getInstance().currentActivity(), AssetDownloadActivity.class, bundle, ANIMATESTICKERREQUESTLIST);
                break;
            case R.id.stickerAssetFinish:
                // ????????????????????????
                multiThumbnailSequenceViewSmooth(mInPoint);
                mAnimateStickerAssetLayout.setVisibility(View.GONE);
                seekTimeline(mInPoint);
                if (mAddAnimateSticker != null) {
                    selectAnimateStickerAndTimeSpan();
                } else {
                    selectAnimateStickerAndTimeSpanByZVal();
                }
                /*
                 * Add a sticker object and leave it blank, otherwise entering the sticker list again will cause deletion by mistake
                 *
                 * */
                mAddAnimateSticker = null;
                mCurAnimateStickerZVal = 0;
                isNewStickerUuidItemClick = false;
                mSelectUuid = "";
                /*
                 * ????????????Tab????????????????????????
                 * Cancel the status of the current Tab page sticker selection
                 * */
                mCurSelectedPos = -1;
                mAssetFragmentsArray.get(mCurTabPage).setSelectedPos(mCurSelectedPos);
                mAssetFragmentsArray.get(mCurTabPage).notifyDataSetChanged();
                mFrameOperationWrapperLayout.setVisibility(View.INVISIBLE);
                changeAddAndFinishViewStatus(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private NvsTimelineTimeSpan getCurrentTimeSpan() {
        for (int i = 0; i < mTimeSpanInfoList.size(); i++) {
            if (mTimeSpanInfoList.get(i).mAnimateSticker == mCurSelectAnimatedSticker) {
                return mTimeSpanInfoList.get(i).mTimeSpan;
            }
        }
        return null;
    }

    private void playVideo() {
        if (mVideoFragment.getCurrentEngineState() != NvsStreamingContext.STREAMING_ENGINE_STATE_PLAYBACK) {
            long startTime = mStreamingContext.getTimelineCurrentPosition(mTimeline);
            long endTime = mTimeline.getDuration();
            mVideoFragment.playVideo(startTime, endTime);
        } else {
            mVideoFragment.stopEngine();
        }
    }

    @Override
    public void onBackPressed() {
        mVideoFragment.stopEngine();
        removeTimeline();
        AppManager.getInstance().finishActivity();
        super.onBackPressed();
    }

    private void removeTimeline() {
        TimelineUtil.removeTimeline(mTimeline);
        mTimeline = null;
        m_handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        if (data == null) {
            return;
        }
        switch (requestCode) {
            case ANIMATESTICKERREQUESTLIST:
                initAnimateStickerDataList();
                mAssetFragmentsArray.get(0).setAssetInfolist(mTotalStickerAssetList);
                mCurSelectedPos = getSelectedPos();
                mAssetFragmentsArray.get(0).setSelectedPos(mCurSelectedPos);
                mAssetFragmentsArray.get(0).notifyDataSetChanged();
                updateStickerBoundingRect();
                break;
            default:
                break;
        }
    }

    private void multiThumbnailSequenceViewSmooth(long stamp) {
        if (mMultiThumbnailSequenceView != null) {
            int x = Math.round((stamp / (float) mTimeline.getDuration() * mTimelineEditor.getSequenceWidth()));
            mMultiThumbnailSequenceView.smoothScrollTo(x, 0);
        }
    }

    private void selectAnimateStickerAndTimeSpanByZVal() {
        selectAnimateStickerByZVal();
        updateStickerBoundingRect();
        if (mCurSelectAnimatedSticker != null) {
            selectTimeSpan();
            mStickerKeyFrameButton.setVisibility(View.VISIBLE);
            updateStickerKeyFrameButtonBackground();
        } else {
            mTimelineEditor.unSelectAllTimeSpan();
            mStickerKeyFrameButton.setVisibility(View.INVISIBLE);
        }
        ifShowRemoveAllKeyFrameView();
    }

    private void selectAnimateStickerByZVal() {
        long curPos = mStreamingContext.getTimelineCurrentPosition(mTimeline);
        List<NvsTimelineAnimatedSticker> animateStickerList = mTimeline.getAnimatedStickersByTimelinePosition(curPos);
        int stickerCount = animateStickerList.size();
        if (stickerCount > 0) {
            int index = -1;
            for (int i = 0; i < animateStickerList.size(); i++) {
                int tmpZVal = (int) animateStickerList.get(i).getZValue();
                if (tmpZVal == mCurAnimateStickerZVal) {
                    index = i;
                    break;
                }
            }
            mCurSelectAnimatedSticker = index >= 0 ? animateStickerList.get(index) : null;
        } else {
            mCurSelectAnimatedSticker = null;
        }
    }

    private void updateStickerBoundingRect() {
        mVideoFragment.setCurAnimateSticker(mCurSelectAnimatedSticker);
        mVideoFragment.updateAnimateStickerCoordinate(mCurSelectAnimatedSticker);
        updateStickerMuteVisible();
        if (mAddAnimateSticker == null && mAnimateStickerAssetLayout.getVisibility() == View.VISIBLE) {
            mVideoFragment.setDrawRectVisible(View.GONE);
        } else {
            mVideoFragment.changeStickerRectVisible();
        }
    }

    private int getSelectedPos() {
        int selectPos = -1;
        if (mSelectUuid.isEmpty()) {
            return selectPos;
        }
        for (int index = 0; index < mTotalStickerAssetList.size(); ++index) {
            if (mTotalStickerAssetList.get(index).uuid.equals(mSelectUuid)) {
                selectPos = index;
                break;
            }
        }
        return selectPos;
    }

    private int getCustomStickerSelectedPos() {
        int selectPos = -1;
        if (mSelectUuid.isEmpty()) {
            return selectPos;
        }
        for (int index = 0; index < mCustomStickerAssetList.size(); ++index) {
            if (mCustomStickerAssetList.get(index).uuid.equals(mSelectUuid)) {
                selectPos = index;
                break;
            }
        }
        return selectPos;
    }

    private boolean initAssetData() {
        mTimeline = TimelineUtil.createTimeline();
        if (mTimeline == null) {
            return false;
        }
        mStickerDataListClone = TimelineData.instance().cloneStickerData();
        mStickerAssetTypeList = new ArrayList<>();
        mTotalStickerAssetList = new ArrayList<>();
        mCustomStickerAssetList = new ArrayList<>();
        mAssetManager = NvAssetManager.sharedInstance();
        mAssetManager.searchLocalAssets(mAssetType);
        String bundlePath = "sticker";
        mAssetManager.searchReservedAssets(mAssetType, bundlePath);

        mAssetManager.searchLocalAssets(NvAsset.ASSET_CUSTOM_ANIMATED_STICKER);
        String bundlePath2 = "customsticker";
        mAssetManager.searchReservedAssets(NvAsset.ASSET_CUSTOM_ANIMATED_STICKER, bundlePath2);//??????????????????????????????
        mAssetManager.initCustomStickerInfoFromSharedPreferences();//????????????????????????
        return true;
    }

    private void initAnimateStickerDataList() {
        mTotalStickerAssetList.clear();
        ArrayList<NvAsset> userableAsset = getAssetsDataList();
        if (userableAsset != null && userableAsset.size() > 0) {
            for (NvAsset asset : userableAsset) {
                if (asset.isReserved()) {
                    String coverPath = "file:///android_asset/sticker/";
                    coverPath += asset.uuid;
                    coverPath += ".png";
                    /*
                     * ??????assets/sticker?????????????????????
                     * Load images in the assets / sticker folder
                     * */
                    asset.coverUrl = coverPath;
                }
            }
            mTotalStickerAssetList = userableAsset;
        }
    }

    /*
     * ??????????????????????????????????????????????????????assets????????????????????????
     * Get the material downloaded to the cache path of the mobile phone, including the material that comes with the assets path
     * */
    private ArrayList<NvAsset> getAssetsDataList() {
        return mAssetManager.getUsableAssets(mAssetType, NvAsset.AspectRatio_All, 0);
    }

    /*
     * ???????????????????????????
     * Get custom sticker list
     * */
    private void initCustomAssetsDataList() {
        mCustomStickerAssetList.clear();
        mCustomStickerAssetList = mAssetManager.getUsableCustomStickerAssets();
    }

    private void initTabLayout() {
        String[] tabList = getResources().getStringArray(R.array.animatedSticker_type);
        mStickerAssetTypeList.add(tabList[0]);
        mStickerAssetTypeList.add(tabList[1]);
        for (int index = 0; index < mStickerAssetTypeList.size(); index++) {
            mAnimateStickerTypeTab.addTab(mAnimateStickerTypeTab.newTab().setText(mStickerAssetTypeList.get(index)));
        }
        initAnimateStickerFragment();
        mViewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return mAssetFragmentsArray.get(position);
            }

            @Override
            public int getCount() {
                return mAssetFragmentsArray.size();
            }
        });

        /*
         * ??????tab?????????????????????
         * Add a tab switch to listen for events
         * */
        mAnimateStickerTypeTab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                /*
                 * ???????????????tab??????????????????????????????fragment
                 * Position of the currently selected tab, switch to the corresponding fragment
                 * */
                mCurTabPage = tab.getPosition();
                mViewPager.setCurrentItem(mCurTabPage);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    private void gifToCafStickerTemplateinstall() {
        String stickerTemplatePath = "assets:/E14FEE65-71A0-4717-9D66-3397B6C11223.5.animatedsticker";
        StringBuilder packageId = new StringBuilder();
        mStreamingContext.getAssetPackageManager().installAssetPackage(stickerTemplatePath, null, NvsAssetPackageManager.ASSET_PACKAGE_TYPE_ANIMATEDSTICKER, true, packageId);
    }

    private void initAnimateStickerFragment() {
        mStickerListFragment = new AnimateStickerListFragment();
        mStickerListFragment.setAnimateStickerClickerListener(new AnimateStickerListFragment.AnimateStickerClickerListener() {
            @Override
            public void onFragmentLoadFinish() {
                mStickerListFragment.setCustomStickerButtonVisible(View.GONE);
                mCustomStickerListFragment.setIsCutomStickerAsset(false);
                mStickerListFragment.setAssetInfolist(mTotalStickerAssetList);
            }

            @Override
            public void onItemClick(View view, int pos) {
                if (pos < 0 || pos >= mTotalStickerAssetList.size()) {
                    return;
                }
                applyAnimateSticker(pos);
            }

            @Override
            public void onAddCustomSticker() {

            }
        });

        mAssetFragmentsArray.add(mStickerListFragment);
        mCustomStickerListFragment = new AnimateStickerListFragment();
        mCustomStickerListFragment.setAnimateStickerClickerListener(new AnimateStickerListFragment.AnimateStickerClickerListener() {
            @Override
            public void onFragmentLoadFinish() {
                mCustomStickerListFragment.setCustomStickerButtonVisible(View.VISIBLE);
                mCustomStickerListFragment.setIsCutomStickerAsset(true);
                mCustomStickerListFragment.setCustomStickerAssetInfolist(mCustomStickerAssetList);
            }

            @Override
            public void onItemClick(View view, int pos) {
                if (pos < 0 || pos >= mCustomStickerAssetList.size()) {
                    return;
                }
                applyCustomAnimateSticker(pos);
            }

            @Override
            public void onAddCustomSticker() {
                Bundle bundle = new Bundle();
                bundle.putInt(Constants.SELECT_MEDIA_FROM, Constants.SELECT_IMAGE_FROM_CUSTOM_STICKER);
                AppManager.getInstance().jumpActivity(AppManager.getInstance().currentActivity(), SingleClickActivity.class, bundle);
            }
        });

        mAssetFragmentsArray.add(mCustomStickerListFragment);
    }

    private void applyAnimateSticker(int pos) {
        if (mAddAnimateSticker != null
                && mPrevTabPage == mCurTabPage
                && mCurSelectedPos == pos) {
            isNewStickerUuidItemClick = false;
            if (mVideoFragment.getCurrentEngineState() != NvsStreamingContext.STREAMING_ENGINE_STATE_PLAYBACK) {
                long endTime = mInPoint + mStickerDuration;
                mVideoFragment.playVideo(mInPoint, endTime);
                mVideoFragment.setDrawRectVisible(View.GONE);
            } else {
                mVideoFragment.stopEngine();
            }
            return;
        }

        removeAddAniamteSticker();

        float zStickerVal = getCurAnimateStickerZVal();
        /*
         * ????????????
         * add stickers
         * */
        mAddAnimateSticker = mTimeline.addAnimatedSticker(mInPoint, mStickerDuration,
                mTotalStickerAssetList.get(pos).uuid);
        if (mAddAnimateSticker == null) {
            return;
        }
        mAddAnimateSticker.setZValue(zStickerVal);

        /*
         * ???????????????????????????
         * Uncheck other page stickers
         * */
        mCurSelectedPos = pos;
        mSelectUuid = mTotalStickerAssetList.get(pos).uuid;
        addTimeSpanAndPlayVideo(false, "");
    }

    private void applyCustomAnimateSticker(final int pos) {
        if (mAddAnimateSticker != null
                && mPrevTabPage == mCurTabPage
                && mCurSelectedPos == pos) {
            isNewStickerUuidItemClick = false;
            if (mVideoFragment.getCurrentEngineState() != NvsStreamingContext.STREAMING_ENGINE_STATE_PLAYBACK) {
                long endTime = mInPoint + mStickerDuration;
                mVideoFragment.playVideo(mInPoint, endTime);
                mVideoFragment.setDrawRectVisible(View.GONE);
            } else {
                mVideoFragment.stopEngine();
            }
            return;
        }

        removeAddAniamteSticker();

        /*
         * ?????????????????????
         * Add custom stickers
         * */
        String imageSrcFilePath = mCustomStickerAssetList.get(pos).imagePath;
        int lastPointPos = imageSrcFilePath.lastIndexOf(".");
        String fileSuffixName = imageSrcFilePath.substring(lastPointPos).toLowerCase();
        if (".gif".equals(fileSuffixName)) {//gif
            String targetCafPath = mCustomStickerAssetList.get(pos).targetImagePath;
            File targetCafFile = new File(targetCafPath);
            if (targetCafFile.exists()) {
                /*
                 * ????????????caf??????????????????
                 * Detect the existence of the target caf file
                 * */
                addCustomAnimateSticker(pos, targetCafPath);
            }
        } else {//image
            addCustomAnimateSticker(pos, mCustomStickerAssetList.get(pos).imagePath);
        }
    }

    private void addCustomAnimateSticker(int pos, String imageFilePath) {
        float zStickerVal = getCurAnimateStickerZVal();
        mAddAnimateSticker = mTimeline.addCustomAnimatedSticker(mInPoint, mStickerDuration,
                mCustomStickerAssetList.get(pos).templateUuid, imageFilePath);
        if (mAddAnimateSticker == null) {
            return;
        }

        mAddAnimateSticker.setZValue(zStickerVal);
        mCurSelectedPos = pos;
        mSelectUuid = mCustomStickerAssetList.get(pos).uuid;
        addTimeSpanAndPlayVideo(true, imageFilePath);
    }

    private void removeAddAniamteSticker() {
        if (mAddAnimateSticker != null) {
            int zVal = (int) mAddAnimateSticker.getZValue();
            int index = getAnimateStickerIndex(zVal);
            if (index >= 0) {
                mStickerDataListClone.remove(index);
            }
            deleteCurStickerTimeSpan(mAddAnimateSticker);
            mTimeline.removeAnimatedSticker(mAddAnimateSticker);
            mAddAnimateSticker = null;
            mVideoFragment.setCurAnimateSticker(mAddAnimateSticker);
            mVideoFragment.changeStickerRectVisible();
        }
    }

    private StickerInfo saveStickerInfo() {
        StickerInfo stickerInfo = new StickerInfo();
        stickerInfo.setInPoint(mAddAnimateSticker.getInPoint());
        stickerInfo.setOutPoint(mAddAnimateSticker.getOutPoint());
        stickerInfo.setHorizFlip(mAddAnimateSticker.getHorizontalFlip());
        stickerInfo.setTranslation(mAddAnimateSticker.getTranslation());
        String packagedId = mAddAnimateSticker.getAnimatedStickerPackageId();
        stickerInfo.setId(packagedId);
        int zVal = (int) mAddAnimateSticker.getZValue();
        stickerInfo.setAnimateStickerZVal(zVal);
        return stickerInfo;
    }

    private void addTimeSpanAndPlayVideo(boolean isCustomSticker, String imageFilePath) {
        if (mAddAnimateSticker == null) {
            return;
        }
        if (mPrevTabPage != mCurTabPage) {
            mAssetFragmentsArray.get(mPrevTabPage).setSelectedPos(-1);
            mAssetFragmentsArray.get(mPrevTabPage).notifyDataSetChanged();
        }
        isNewStickerUuidItemClick = true;
        mPrevTabPage = mCurTabPage;
        long endTime = mInPoint + mStickerDuration;
        /*
         * ??????timeSpan
         * */
        NvsTimelineTimeSpan timeSpan = addTimeSpan(mInPoint, endTime);
        if (timeSpan != null) {
            AnimateStickerActivity.AnimateStickerTimeSpanInfo timeSpanInfo = new AnimateStickerActivity.AnimateStickerTimeSpanInfo(mAddAnimateSticker, timeSpan);
            mTimeSpanInfoList.add(timeSpanInfo);
        }

        /*
         * ????????????
         * save data
         * */
        StickerInfo stickerInfo = saveStickerInfo();
        stickerInfo.setCustomSticker(isCustomSticker);
        stickerInfo.setCustomImagePath(imageFilePath);
        mStickerDataListClone.add(stickerInfo);
        mVideoFragment.setDrawRectVisible(View.GONE);
        /*
         * ????????????
         * Play video
         * */
        mVideoFragment.playVideo(mInPoint, endTime);
    }

    private float getCurAnimateStickerZVal() {
        float zVal = 0.0f;
        NvsTimelineAnimatedSticker animatedSticker = mTimeline.getFirstAnimatedSticker();
        while (animatedSticker != null) {
            float tmpZVal = animatedSticker.getZValue();
            if (tmpZVal > zVal) {
                zVal = tmpZVal;
            }
            animatedSticker = mTimeline.getNextAnimatedSticker(animatedSticker);
        }
        zVal += 1.0;
        return zVal;
    }

    private void selectAnimateStickerAndTimeSpan() {
        if (mCurrentStatusIsKeyFrame) {
            return;
        }
        selectAnimateSticker();
        StickerInfo currentStickerInfo = getCurrentStickerInfo();
        if (currentStickerInfo != null) {
            updateStickerBoundingRect();
        } else {
            mVideoFragment.setDrawRectVisible(View.GONE);
        }
        if (mCurSelectAnimatedSticker != null) {
            selectTimeSpan();
            mStickerKeyFrameButton.setVisibility(View.VISIBLE);
            updateStickerKeyFrameButtonBackground();
        } else {
            mTimelineEditor.unSelectAllTimeSpan();
            mStickerKeyFrameButton.setVisibility(View.INVISIBLE);
        }
        ifShowRemoveAllKeyFrameView();
    }

    private void ifShowRemoveAllKeyFrameView() {
        StickerInfo currentStickerInfo = getCurrentStickerInfo();
        if (currentStickerInfo != null) {
            Map<Long, KeyFrameInfo> keyFrameInfoHashMap = currentStickerInfo.getKeyFrameInfoHashMap();
            if (!keyFrameInfoHashMap.isEmpty()) {
                mRemoveAllKeyFrameButton.setVisibility(View.VISIBLE);
            } else {
                mRemoveAllKeyFrameButton.setVisibility(View.INVISIBLE);
            }
        } else {
            mRemoveAllKeyFrameButton.setVisibility(View.INVISIBLE);
        }
    }

    private void updateStickerKeyFrameButtonBackground() {
        StickerInfo currentStickerInfo = getCurrentStickerInfo();
        boolean isAddStatus = true;
        if (currentStickerInfo != null) {
            isAddStatus = currentStickerInfo.getKeyFrameInfoHashMap().isEmpty();
        }
        if (isAddStatus) {
            // ????????????
            mStickerKeyFrameButton.setImageResource(R.mipmap.caption_key_frame_add_icon);
        } else {
            // ????????????
            mStickerKeyFrameButton.setImageResource(R.mipmap.caption_key_frame_edit_icon);
        }
    }

    private void deleteAnimateSticker() {
        deleteCurStickerTimeSpan(mCurSelectAnimatedSticker);
        mTimeline.removeAnimatedSticker(mCurSelectAnimatedSticker);
        mCurSelectAnimatedSticker = null;
        selectAnimateStickerAndTimeSpan();
        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline));
    }

    private void deleteCurStickerTimeSpan(NvsTimelineAnimatedSticker animateSticker) {
        for (int i = 0; i < mTimeSpanInfoList.size(); i++) {
            if (animateSticker != null
                    && mTimeSpanInfoList.get(i).mAnimateSticker == animateSticker) {
                mTimelineEditor.deleteSelectedTimeSpan(mTimeSpanInfoList.get(i).mTimeSpan);
                mTimeSpanInfoList.remove(i);
                break;
            }
        }
    }

    private NvsTimelineTimeSpan addTimeSpan(long inPoint, long outPoint) {
        /*
         * warning: ??????addTimeSpanExt??????????????????setTimeSpanType()
         * warning: setTimeSpanType () must be set before using addTimeSpanExt
         * */
        mTimelineEditor.setTimeSpanType("NvsTimelineTimeSpan");
        final NvsTimelineTimeSpan timelineTimeSpan = mTimelineEditor.addTimeSpan(inPoint, outPoint);
        if (timelineTimeSpan == null) {
            Log.e(TAG, "addTimeSpan: " + " ??????TimeSpan??????!");
            return null;
        }
        // Span????????????
        timelineTimeSpan.setOnChangeListener(new NvsTimelineTimeSpan.OnTrimInChangeListener() {
            @Override
            public void onTrimInChange(long timeStamp, boolean isDragEnd) {
                seekTimeline(timeStamp);
                StickerInfo currentStickerInfo = getCurrentStickerInfo();
                if (currentStickerInfo != null) {
                    boolean noInfo = currentStickerInfo.getKeyFrameInfoHashMap().isEmpty();
                    if (!noInfo) {
                        mVideoFragment.changeStickerRectVisible();
                    }
                }
                setPlaytimeText(timeStamp);
                NvsTimelineTimeSpan currentTimeSpan = getCurrentTimeSpan();
                if (currentStickerInfo != null && mCurrentStatusIsKeyFrame) {
                    currentTimeSpan.setKeyFrameInfo(currentStickerInfo.getKeyFrameInfoHashMap());
                }
                if (isDragEnd && mCurSelectAnimatedSticker != null) {
                    mCurSelectAnimatedSticker.changeInPoint(timeStamp);
                    if (mCurSelectAnimatedSticker == null) {
                        return;
                    }
                    int zVal = (int) mCurSelectAnimatedSticker.getZValue();
                    int index = getAnimateStickerIndex(zVal);
                    if (index >= 0) {
                        mStickerDataListClone.get(index).setInPoint(mCurSelectAnimatedSticker.getInPoint());
                    }
                    seekMultiThumbnailSequenceView();
                    // ??????????????? ???????????? 1.???????????????????????????????????????????????????  2.?????????????????????????????????
                    if (mCurSelectAnimatedSticker != null && currentStickerInfo != null) {
                        // 1.step one
                        Map<Long, KeyFrameInfo> keyFrameInfoHashMap = currentStickerInfo.getKeyFrameInfoHashMap();
                        Set<Map.Entry<Long, KeyFrameInfo>> entries = keyFrameInfoHashMap.entrySet();
                        long currentTimeLinePosition = mStreamingContext.getTimelineCurrentPosition(mTimeline);
                        Iterator<Map.Entry<Long, KeyFrameInfo>> iterator = entries.iterator();
                        while (iterator.hasNext()) {
                            Map.Entry<Long, KeyFrameInfo> next = iterator.next();
                            if (next.getKey() < currentTimeLinePosition) {
                                iterator.remove();
                            }
                        }
                        if (mCurrentStatusIsKeyFrame) {
                            currentTimeSpan.setKeyFrameInfo(currentStickerInfo.getKeyFrameInfoHashMap());
                        }
                        // 2.step two ?????????????????????????????????????????? ?????????????????????????????????????????????
                        boolean removeStickerTransXSuccess = mCurSelectAnimatedSticker.removeAllKeyframe("Sticker TransX");
                        boolean removeStickerTransYSuccess = mCurSelectAnimatedSticker.removeAllKeyframe("Sticker TransY");
                        boolean removeStickerScale = mCurSelectAnimatedSticker.removeAllKeyframe("Sticker Scale");
                        boolean removeStickerRotZ = mCurSelectAnimatedSticker.removeAllKeyframe("Sticker RotZ");
                        if (removeStickerTransXSuccess || removeStickerTransYSuccess || removeStickerScale || removeStickerRotZ) {
                            Log.d(TAG, "timelineTimeSpan.setOnChangeListener onChangeLeft  removeAllKeyframe success");
                        }
                        Map<Long, KeyFrameInfo> keyFrameInfoHashMapAfter = currentStickerInfo.getKeyFrameInfoHashMap();
                        Set<Map.Entry<Long, KeyFrameInfo>> entriesAfter = keyFrameInfoHashMapAfter.entrySet();
                        for (Map.Entry<Long, KeyFrameInfo> longStickerKeyFrameInfoEntry : entriesAfter) {
                            KeyFrameInfo stickerKeyFrameInfo = longStickerKeyFrameInfoEntry.getValue();
                            mCurSelectAnimatedSticker.setCurrentKeyFrameTime(longStickerKeyFrameInfoEntry.getKey() - mCurSelectAnimatedSticker.getInPoint());
                            mCurSelectAnimatedSticker.setFloatValAtTime("Sticker TransX", stickerKeyFrameInfo.getTranslation().x, longStickerKeyFrameInfoEntry.getKey() - mCurSelectAnimatedSticker.getInPoint());
                            mCurSelectAnimatedSticker.setFloatValAtTime("Sticker TransY", stickerKeyFrameInfo.getTranslation().y, longStickerKeyFrameInfoEntry.getKey() - mCurSelectAnimatedSticker.getInPoint());
                            mCurSelectAnimatedSticker.setFloatValAtTime("Sticker Scale", stickerKeyFrameInfo.getScaleX(), longStickerKeyFrameInfoEntry.getKey() - mCurSelectAnimatedSticker.getInPoint());
                            mCurSelectAnimatedSticker.setFloatValAtTime("Sticker RotZ", stickerKeyFrameInfo.getRotationZ(), longStickerKeyFrameInfoEntry.getKey() - mCurSelectAnimatedSticker.getInPoint());
                        }
                    }
                }
            }
        });
        // Span????????????
        timelineTimeSpan.setOnChangeListener(new NvsTimelineTimeSpan.OnTrimOutChangeListener() {
            @Override
            public void onTrimOutChange(long timeStamp, boolean isDragEnd) {
                /*
                 * outPoint???????????????seekTimeline?????????????????????????????????0.04????????????????????????40000??????
                 * outPoint is an open interval. In seekTimeline, you need to pan one frame, that is, 0.04 seconds, and convert it to microseconds, that is, 40,000 microseconds.
                 * */
                seekTimeline(timeStamp - 40000);
                setPlaytimeText(timeStamp);
                StickerInfo curStickerInfo = getCurrentStickerInfo();
                if (curStickerInfo != null) {
                    boolean noInfo = curStickerInfo.getKeyFrameInfoHashMap().isEmpty();
                    if (!noInfo) {
                        mVideoFragment.changeStickerRectVisible();
                    }
                }
                if (isDragEnd && mCurSelectAnimatedSticker != null) {
                    mCurSelectAnimatedSticker.changeOutPoint(timeStamp);
                    if (mCurSelectAnimatedSticker == null) {
                        return;
                    }
                    int zVal = (int) mCurSelectAnimatedSticker.getZValue();
                    int index = getAnimateStickerIndex(zVal);
                    if (index >= 0) {
                        mStickerDataListClone.get(index).setOutPoint(timeStamp);
                    }
                    seekMultiThumbnailSequenceView();
                    // ????????????????????? ?????????????????????????????????
                    StickerInfo currentStickerInfo = getCurrentStickerInfo();
                    if (currentStickerInfo != null) {
                        Map<Long, KeyFrameInfo> keyFrameInfoHashMap = currentStickerInfo.getKeyFrameInfoHashMap();
                        Set<Map.Entry<Long, KeyFrameInfo>> entries = keyFrameInfoHashMap.entrySet();
                        Iterator<Map.Entry<Long, KeyFrameInfo>> iterator = entries.iterator();
                        while (iterator.hasNext()) {
                            Map.Entry<Long, KeyFrameInfo> next = iterator.next();
                            // ??????????????????timeline????????????
                            if (next.getKey() > mStreamingContext.getTimelineCurrentPosition(mTimeline)) {
                                iterator.remove();
                            }
                        }
                        if (mCurrentStatusIsKeyFrame) {
                            NvsTimelineTimeSpan currentTimeSpan = getCurrentTimeSpan();
                            currentTimeSpan.setKeyFrameInfo(currentStickerInfo.getKeyFrameInfoHashMap());
                        }
                    }
                }
            }
        });

        return timelineTimeSpan;
    }

    private void seekMultiThumbnailSequenceView() {
        if (mMultiThumbnailSequenceView != null) {
            long curPos = mStreamingContext.getTimelineCurrentPosition(mTimeline);
            long duration = mTimeline.getDuration();
            mMultiThumbnailSequenceView.scrollTo(Math.round(((float) curPos) / (float) duration * mTimelineEditor.getSequenceWidth()), 0);
        }
    }

    private void seekTimeline(long timestamp) {
        mVideoFragment.seekTimeline(timestamp, 0);
    }

    /**
     * ???????????????????????? ????????????????????????
     */
    private void addAllTimeSpan() {
        NvsTimelineAnimatedSticker animatedSticker = mTimeline.getFirstAnimatedSticker();
        while (animatedSticker != null) {
            long inPoint = animatedSticker.getInPoint();
            long outPoint = animatedSticker.getOutPoint();
            NvsTimelineTimeSpan timeSpan = addTimeSpan(inPoint, outPoint);
            if (timeSpan != null) {
                AnimateStickerActivity.AnimateStickerTimeSpanInfo timeSpanInfo = new AnimateStickerActivity.AnimateStickerTimeSpanInfo(animatedSticker, timeSpan);
                mTimeSpanInfoList.add(timeSpanInfo);
            }
            animatedSticker = mTimeline.getNextAnimatedSticker(animatedSticker);
        }
    }

    private void selectAnimateSticker() {
        // ??????????????????????????????
        long curPos = mStreamingContext.getTimelineCurrentPosition(mTimeline);
        List<NvsTimelineAnimatedSticker> animateStickerList = mTimeline.getAnimatedStickersByTimelinePosition(curPos);
        Logger.e(TAG, "animateStickerListCount-->" + animateStickerList.size());
        int stickerCount = animateStickerList.size();
        if (stickerCount > 0) {
            float zVal = animateStickerList.get(0).getZValue();
            int index = 0;
            for (int i = 0; i < animateStickerList.size(); i++) {
                float tmpZVal = animateStickerList.get(i).getZValue();
                if (tmpZVal > zVal) {
                    zVal = tmpZVal;
                    index = i;
                }
            }
            mCurSelectAnimatedSticker = animateStickerList.get(index);
        } else {
            mCurSelectAnimatedSticker = null;
        }
    }

    private void selectTimeSpan() {
        for (int i = 0; i < mTimeSpanInfoList.size(); i++) {
            if (mTimeSpanInfoList.get(i).mAnimateSticker == mCurSelectAnimatedSticker) {
                mTimelineEditor.selectTimeSpan(mTimeSpanInfoList.get(i).mTimeSpan);
                break;
            }
        }
    }

    private boolean ifCouldEditAnimateSticker() {
        StickerInfo currentStickerInfo = getCurrentStickerInfo();
        if (currentStickerInfo != null) {
            Map<Long, KeyFrameInfo> keyFrameInfoHashMap = currentStickerInfo.getKeyFrameInfoHashMap();
            if (!keyFrameInfoHashMap.isEmpty()) {
                // give tips
                ToastUtil.showToastCenter(getApplicationContext(), getResources().getString(R.string.tips_when_move_animate_sticker));
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    private void initVideoFragment() {
        mVideoFragment = new VideoFragment();
        mVideoFragment.setFragmentLoadFinisedListener(new VideoFragment.OnFragmentLoadFinisedListener() {
            @Override
            public void onLoadFinished() {
                mVideoFragment.setCurAnimateSticker(mCurSelectAnimatedSticker);
                mStickerFinish.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        seekTimeline(mStreamingContext.getTimelineCurrentPosition(mTimeline));
                        selectAnimateStickerAndTimeSpan();
                    }
                }, 100);
            }
        });
        mVideoFragment.setTimeline(mTimeline);
        /*
         * ??????????????????
         * Set sticker mode
         * */
        mVideoFragment.setEditMode(Constants.EDIT_MODE_STICKER);
        Bundle bundle = new Bundle();
        bundle.putInt("titleHeight", mTitleBar.getLayoutParams().height);
        bundle.putInt("bottomHeight", mBottomLayout.getLayoutParams().height);
        bundle.putInt("ratio", TimelineData.instance().getMakeRatio());
        bundle.putBoolean("playBarVisible", false);
        mVideoFragment.setArguments(bundle);
        getFragmentManager().beginTransaction()
                .add(R.id.spaceLayout, mVideoFragment)
                .commit();
        getFragmentManager().beginTransaction().show(mVideoFragment);
    }

    private void updateStickerMuteVisible() {
        if (mCurSelectAnimatedSticker != null) {
            boolean hasAudio = mCurSelectAnimatedSticker.hasAudio();
            mVideoFragment.setMuteVisible(hasAudio);
            if (hasAudio) {
                float leftVolume = (int) mCurSelectAnimatedSticker.getVolumeGain().leftVolume;
                mVideoFragment.setStickerMuteIndex(leftVolume > 0 ? 0 : 1);
            }
        }
    }

    private void initMultiSequence() {
        NvsVideoTrack videoTrack = mTimeline.getVideoTrackByIndex(0);
        if (videoTrack == null) {
            return;
        }
        int clipCount = videoTrack.getClipCount();
        ArrayList<NvsMultiThumbnailSequenceView.ThumbnailSequenceDesc> sequenceDescsArray = new ArrayList<>();
        for (int index = 0; index < clipCount; ++index) {
            NvsVideoClip videoClip = videoTrack.getClipByIndex(index);
            if (videoClip == null) {
                continue;
            }

            NvsMultiThumbnailSequenceView.ThumbnailSequenceDesc sequenceDescs = new NvsMultiThumbnailSequenceView.ThumbnailSequenceDesc();
            sequenceDescs.mediaFilePath = videoClip.getFilePath();
            sequenceDescs.trimIn = videoClip.getTrimIn();
            sequenceDescs.trimOut = videoClip.getTrimOut();
            sequenceDescs.inPoint = videoClip.getInPoint();
            sequenceDescs.outPoint = videoClip.getOutPoint();
            sequenceDescs.stillImageHint = false;
            sequenceDescsArray.add(sequenceDescs);
        }
        long duration = mTimeline.getDuration();
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mVideoPlay.getLayoutParams();
        int playBtnTotalWidth = layoutParams.width + layoutParams.leftMargin + layoutParams.rightMargin;
        int halfScreenWidth = ScreenUtils.getScreenWidth(this) / 2;
        int sequenceLeftPadding = halfScreenWidth - playBtnTotalWidth;
        mTimelineEditor.setSequencLeftPadding(sequenceLeftPadding);
        mTimelineEditor.setSequencRightPadding(halfScreenWidth);
        mTimelineEditor.setTimeSpanLeftPadding(sequenceLeftPadding);
        mTimelineEditor.initTimelineEditor(sequenceDescsArray, duration);
    }

    private void setPlaytimeText(long playTime) {
        long totalDuaration = mTimeline.getDuration();
        String strTotalDuration = TimeFormatUtil.formatUsToString1(totalDuaration);
        String strCurrentDuration = TimeFormatUtil.formatUsToString1(playTime);
        mShowCurrentDuration.setLength(0);
        mShowCurrentDuration.append(strCurrentDuration);
        mShowCurrentDuration.append("/");
        mShowCurrentDuration.append(strTotalDuration);
        mCurrentPlaytime.setText(mShowCurrentDuration.toString());
    }

    private void resetView() {
        setPlaytimeText(0);
        mVideoPlay.setBackgroundResource(R.mipmap.icon_edit_play);
        mMultiThumbnailSequenceView.fullScroll(HorizontalScrollView.FOCUS_LEFT);
        seekTimeline(mAnimateStickerAssetLayout.getVisibility() == View.VISIBLE ? mInPoint : 0);
        selectAnimateStickerAndTimeSpan();
    }

    private int getAnimateStickerIndex(int curZValue) {
        int index = -1;
        int count = mStickerDataListClone.size();
        for (int i = 0; i < count; ++i) {
            int zVal = mStickerDataListClone.get(i).getAnimateStickerZVal();
            if (curZValue == zVal) {
                index = i;
                break;
            }
        }
        return index;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMoreDownload.setClickable(true);
        initCustomAssetsDataList();
        mAssetFragmentsArray.get(1).setCustomStickerAssetInfolist(mCustomStickerAssetList);
        mCurSelectedPos = getCustomStickerSelectedPos();
        mAssetFragmentsArray.get(1).setSelectedPos(mCurSelectedPos);
        mAssetFragmentsArray.get(1).notifyDataSetChanged();
        updateStickerBoundingRect();
    }
}
