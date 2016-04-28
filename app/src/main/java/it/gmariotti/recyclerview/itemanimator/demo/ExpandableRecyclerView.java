package it.gmariotti.recyclerview.itemanimator.demo;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import it.gmariotti.recyclerview.itemanimator.demo.adapter.SimpleAdapter;


public class ExpandableRecyclerView extends LinearLayout implements View.OnClickListener {

    private static final String TAG = ExpandableRecyclerView.class.getSimpleName();

    /* The default number of lines */
    private static final int MAX_COLLAPSED_LINES = 2;

    /* The default animation duration */
    private static final int DEFAULT_ANIM_DURATION = 200;

    /* The default alpha value when the animation starts */
    private static final float DEFAULT_ANIM_ALPHA_START = 0.7f;

    protected RecyclerView mTv;

    protected ImageButton mButton; // Button to expand/collapse

    private boolean mRelayout;

    private boolean mCollapsed = true; // Show short version as default.

    private int mCollapsedHeight;

    private int mListHeightWithMaxLines;

    private int mMaxCollapsedLines;

    private int mMarginBetweenTxtAndBottom;

    private Drawable mExpandDrawable;

    private Drawable mCollapseDrawable;

    private int mAnimationDuration;

    private float mAnimAlphaStart;

    private boolean mAnimating;

    /* Listener for callback */
    private OnExpandStateChangeListener mListener;

    /* For saving collapsed status when used in ListView */
    private SparseBooleanArray mCollapsedStatus;
    private int mPosition;

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

        if (mCollapsedStatus != null) {
            mCollapsedStatus.put(mPosition, mCollapsed);
        }

        // mark that the animation is in progress
        mAnimating = true;

        Animation animation;
        if (mCollapsed) {
            animation = new ExpandCollapseAnimation(mTv, 1000, 330);
        } else {
            animation = new ExpandCollapseAnimation(mTv, 330, 1000);
        }

        animation.setFillAfter(true);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                applyAlphaAnimation(mTv, mAnimAlphaStart);
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                // clear animation here to avoid repeated applyTransformation() calls
                clearAnimation();
                // clear the animation flag
                mAnimating = false;

                // notify the listener
                if (mListener != null) {
                    mListener.onExpandStateChanged(mTv, !mCollapsed);
                }
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
        // If no change, measure and return
        if (!mRelayout || getVisibility() == View.GONE) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        mRelayout = false;

        // Setup with optimistic case
        // i.e. Everything fits. No button needed
        mButton.setVisibility(View.GONE);
        //mTv.setMaxLines(Integer.MAX_VALUE);

        // Measure
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // If the text fits in collapsed mode, we are done.
        if (mTv.getAdapter().getItemCount() <= mMaxCollapsedLines) {
            return;
        }

        // Saves the text height w/ max lines
        mListHeightWithMaxLines = getRealRecyclerViewHeight(mTv);

        // Doesn't fit in collapsed mode. Collapse recycler view as needed. Show
        // button.
        if (mCollapsed) {
            ViewGroup.LayoutParams params = mTv.getLayoutParams();
            params.height = 330;
            mTv.setLayoutParams(params);
        }
        mButton.setVisibility(View.VISIBLE);

        // Re-measure with new setup
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (mCollapsed) {
            // Gets the margin between the RecyclerView's bottom and the ViewGroup's bottom
            mTv.post(new Runnable() {
                @Override
                public void run() {
                    mMarginBetweenTxtAndBottom = getHeight() - mTv.getHeight();
                }
            });
            // Saves the collapsed height of this ViewGroup
            mCollapsedHeight = getMeasuredHeight();
        }
    }

    public void setOnExpandStateChangeListener(@Nullable OnExpandStateChangeListener listener) {
        mListener = listener;
    }

    public void setText(@Nullable CharSequence text) {
        mRelayout = true;
        //mTv.setText(text);
        setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
    }

    public void setText(@Nullable CharSequence text, @NonNull SparseBooleanArray collapsedStatus, int position) {
        mCollapsedStatus = collapsedStatus;
        mPosition = position;
        boolean isCollapsed = collapsedStatus.get(position, true);
        clearAnimation();
        mCollapsed = isCollapsed;
        mButton.setImageDrawable(mCollapsed ? mExpandDrawable : mCollapseDrawable);
        setText(text);
        getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        requestLayout();
    }

    private void init(AttributeSet attrs) {
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
        mTv = (RecyclerView) findViewById(R.id.expandableList);
        mTv.setOnClickListener(this);
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

    private static int getRealRecyclerViewHeight(@NonNull RecyclerView recyclerView) {
        return recyclerView.getAdapter().getItemCount();
    }

    class ExpandCollapseAnimation extends Animation {
        private final View mTargetView;
        private final int mStartHeight;
        private final int mEndHeight;

        public ExpandCollapseAnimation(View view, int startHeight, int endHeight) {
            mTargetView = view;
            mStartHeight = startHeight;
            mEndHeight = endHeight;
            setDuration(mAnimationDuration);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            final int newHeight = (int)((mEndHeight - mStartHeight) * interpolatedTime + mStartHeight);

            if (Float.compare(mAnimAlphaStart, 1.0f) != 0) {
                applyAlphaAnimation(mTargetView, mAnimAlphaStart + interpolatedTime * (1.0f - mAnimAlphaStart));
            }
            ViewGroup.LayoutParams params = mTargetView.getLayoutParams();
            params.height = newHeight;
            mTargetView.setLayoutParams(params);
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

    public interface OnExpandStateChangeListener {
        /**
         * Called when the expand/collapse animation has been finished
         *
         * @param recyclerView - RecyclerView being expanded/collapsed
         * @param isExpanded - true if the RecyclerView has been expanded
         */
        void onExpandStateChanged(RecyclerView recyclerView, boolean isExpanded);
    }
}