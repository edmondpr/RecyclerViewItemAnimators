package it.gmariotti.recyclerview.itemanimator.demo;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import it.gmariotti.recyclerview.itemanimator.demo.models.UpdateListEvent;


public class ExpandableRecyclerView extends LinearLayout implements View.OnClickListener {

    private static final String TAG = ExpandableRecyclerView.class.getSimpleName();

    /* The default number of lines */
    private static final int MAX_COLLAPSED_LINES = 2;

    /* The default animation duration */
    private static final int DEFAULT_ANIM_DURATION = 200;

    /* The default alpha value when the animation starts */
    private static final float DEFAULT_ANIM_ALPHA_START = 0.7f;

    protected RecyclerView mRecyclerView;

    protected ImageButton mButton; // Button to expand/collapse

    private boolean mRelayout;

    private boolean mCollapsed = true; // Show short version as default.

    private boolean firstLoad = true;

    private int mMaxCollapsedLines;

    private Drawable mExpandDrawable;

    private Drawable mCollapseDrawable;

    private int mAnimationDuration;

    private float mAnimAlphaStart;

    private boolean mAnimating;

    private float recyclerViewHeight = 0;
    private float recyclerViewCollapsedHeight = 0;
    ArrayList<Float> recyclerViewItemHeights = new ArrayList<Float>();


    public ExpandableRecyclerView(Context context) {
        this(context, null);
    }

    public ExpandableRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public ExpandableRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    @Override
    public void setOrientation(int orientation){
        if(LinearLayout.HORIZONTAL == orientation){
            throw new IllegalArgumentException("ExpandableRecyclerView only supports Vertical Orientation.");
        }
        super.setOrientation(orientation);
    }

    @Override
    public void onClick(View view) {
        if (mButton.getVisibility() != View.VISIBLE) {
            return;
        }

        mCollapsed = !mCollapsed;
        mButton.setImageDrawable(mCollapsed ? mExpandDrawable : mCollapseDrawable);

        Animation animation;
        if (mCollapsed) {
            animation = new ExpandCollapseAnimation(Math.round(recyclerViewHeight), Math.round(recyclerViewCollapsedHeight));
        } else {
            animation = new ExpandCollapseAnimation(Math.round(recyclerViewCollapsedHeight), Math.round(recyclerViewHeight));
        }

        doAnimation(animation);
    }

    private void doAnimation(Animation animation) {
        // mark that the animation is in progress
        mAnimating = true;

        animation.setFillAfter(true);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                applyAlphaAnimation(mRecyclerView, mAnimAlphaStart);
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                // clear animation here to avoid repeated applyTransformation() calls
                clearAnimation();
                // clear the animation flag
                mAnimating = false;

            }
            @Override
            public void onAnimationRepeat(Animation animation) { }
        });

        clearAnimation();
        startAnimation(animation);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // while an animation is in progress, intercept all the touch events to children to
        // prevent extra clicks during the animation
        return mAnimating;
    }

    @Override
    protected void onFinishInflate() {
        findViews();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Measure
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void collapse() {
        // If no change, measure and return
        if (!mRelayout || getVisibility() == View.GONE) {
            return;
        }
        mRelayout = false;

        // Setup with optimistic case
        // i.e. Everything fits. No button needed
        mButton.setVisibility(View.GONE);

        // If the text fits in collapsed mode, we are done.
        if (mRecyclerView.getAdapter().getItemCount() <= mMaxCollapsedLines) {
            return;
        }

        // Doesn't fit in collapsed mode. Collapse recycler view as needed. Show
        // button.
        if (mCollapsed) {
            ViewGroup.LayoutParams params = mRecyclerView.getLayoutParams();
            params.height = Math.round(recyclerViewCollapsedHeight);
            mRecyclerView.setLayoutParams(params);
        }
        mButton.setVisibility(View.VISIBLE);
    }


    public void setText(@Nullable CharSequence text) {
        mRelayout = true;
        //mRecyclerView.setText(text);
        setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
    }

    private void init(AttributeSet attrs) {
        EventBus.getDefault().register(this);
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.ExpandableRecyclerView);
        mMaxCollapsedLines = typedArray.getInt(R.styleable.ExpandableRecyclerView_maxCollapsedLines, MAX_COLLAPSED_LINES);
        mAnimationDuration = typedArray.getInt(R.styleable.ExpandableRecyclerView_animDuration, DEFAULT_ANIM_DURATION);
        mAnimAlphaStart = typedArray.getFloat(R.styleable.ExpandableRecyclerView_animAlphaStart, DEFAULT_ANIM_ALPHA_START);
        mExpandDrawable = typedArray.getDrawable(R.styleable.ExpandableRecyclerView_expandDrawable);
        mCollapseDrawable = typedArray.getDrawable(R.styleable.ExpandableRecyclerView_collapseDrawable);

        if (mExpandDrawable == null) {
            mExpandDrawable = getDrawable(getContext(), R.drawable.ic_expand_more);
        }
        if (mCollapseDrawable == null) {
            mCollapseDrawable = getDrawable(getContext(), R.drawable.ic_expand_less);
        }

        typedArray.recycle();

        // enforces vertical orientation
        setOrientation(LinearLayout.VERTICAL);

        // default visibility is gone
        setVisibility(GONE);
    }

    private void findViews() {
        mRecyclerView = (RecyclerView) findViewById(R.id.expandableList);
        mRecyclerView.setOnClickListener(this);
        mButton = (ImageButton) findViewById(R.id.expand_collapse);
        mButton.setImageDrawable(mCollapsed ? mExpandDrawable : mCollapseDrawable);
        mButton.setOnClickListener(this);
    }

    private static boolean isPostHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    private static boolean isPostLolipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static void applyAlphaAnimation(View view, float alpha) {
        if (isPostHoneycomb()) {
            view.setAlpha(alpha);
        } else {
            AlphaAnimation alphaAnimation = new AlphaAnimation(alpha, alpha);
            // make it instant
            alphaAnimation.setDuration(0);
            alphaAnimation.setFillAfter(true);
            view.startAnimation(alphaAnimation);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static Drawable getDrawable(@NonNull Context context, @DrawableRes int resId) {
        Resources resources = context.getResources();
        if (isPostLolipop()) {
            return resources.getDrawable(resId, context.getTheme());
        } else {
            return resources.getDrawable(resId);
        }
    }


    class ExpandCollapseAnimation extends Animation {
        private final int mStartHeight;
        private final int mEndHeight;

        public ExpandCollapseAnimation(int startHeight, int endHeight) {
            mStartHeight = startHeight;
            mEndHeight = endHeight;
            setDuration(mAnimationDuration);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            final int newHeight = (int)((mEndHeight - mStartHeight) * interpolatedTime + mStartHeight);

            if (Float.compare(mAnimAlphaStart, 1.0f) != 0) {
                applyAlphaAnimation(mRecyclerView, mAnimAlphaStart + interpolatedTime * (1.0f - mAnimAlphaStart));
            }
            ViewGroup.LayoutParams params = mRecyclerView.getLayoutParams();
            params.height = newHeight;
            mRecyclerView.setLayoutParams(params);
        }

        @Override
        public void initialize( int width, int height, int parentWidth, int parentHeight ) {
            super.initialize(width, height, parentWidth, parentHeight);
        }

        @Override
        public boolean willChangeBounds( ) {
            return true;
        }
    }

    @Subscribe
    public void onUpdateListEvent(UpdateListEvent updateListEvent) {
        if (updateListEvent.itemHeight != null) {
            if (recyclerViewItemHeights.size() < mRecyclerView.getAdapter().getItemCount()) {
                if (!updateListEvent.isNew) {
                    recyclerViewItemHeights.add(updateListEvent.itemHeight);
                    updateRecyclerViewHeight();
                } else {
                    if (mCollapsed) {
                        mButton.performClick();
                    }
                    // If it's a new element, increase the height of recycler view by 50 so that
                    // it can accommodate the element
                    ViewGroup.LayoutParams params = mRecyclerView.getLayoutParams();
                    params.height = Math.round(recyclerViewHeight) + 50;
                    mRecyclerView.setLayoutParams(params);
                }
            }
        } else if (updateListEvent.position != null) {
            recyclerViewItemHeights.remove(updateListEvent.position.intValue());
            updateRecyclerViewHeight();
        }
        if (recyclerViewItemHeights.size() == mRecyclerView.getAdapter().getItemCount() &&
                firstLoad) {
            collapse();
            firstLoad = false;
        }
        // Hide chevron if less than max collapsed lines
        if (recyclerViewItemHeights.size() <= mMaxCollapsedLines) {
            mButton.setVisibility(View.GONE);
        } else {
            mButton.setVisibility(View.VISIBLE);
        }
    }

    private void updateRecyclerViewHeight() {
        float oldRecyclerViewCollapsedHeight = recyclerViewCollapsedHeight;
        recyclerViewCollapsedHeight = 0;
        int count = mMaxCollapsedLines;
        if (recyclerViewItemHeights.size() < mMaxCollapsedLines) {
            count = recyclerViewItemHeights.size();
        }
        for (int i = 0; i < count; i++) {
            recyclerViewCollapsedHeight += recyclerViewItemHeights.get(i);
        }
        float oldRecyclerViewHeight = recyclerViewHeight;
        recyclerViewHeight = 0;
        for (int i = 0; i < recyclerViewItemHeights.size(); i++) {
            recyclerViewHeight += recyclerViewItemHeights.get(i);
        }
        // Animate only if not on first load
        if (!firstLoad) {
            if (!mCollapsed) {
                Animation animation = new ExpandCollapseAnimation(Math.round(oldRecyclerViewHeight), Math.round(recyclerViewHeight));
                doAnimation(animation);
            } else {
                Animation animation = new ExpandCollapseAnimation(Math.round(oldRecyclerViewCollapsedHeight), Math.round(recyclerViewCollapsedHeight));
                doAnimation(animation);
            }
        }
    }

}