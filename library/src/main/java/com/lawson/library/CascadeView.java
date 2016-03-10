package com.lawson.library;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lawson on 16/2/14.
 */
public class CascadeView extends FrameLayout {

  private static final int DEFAULT_COUNT = 2;
  private static final int COVER_POSITION_IN_CACHE = 0;

  private float ratio = 0.4f;

  private final List<ViewHolder> cache = new ArrayList<>();
  private final ArrayList<ViewHolder> mAttachedScrap = new ArrayList<>();

  private int visibleViewCount = DEFAULT_COUNT;
  private int coverPositionInItems;
  private boolean mRunPredictiveAnimations = false;
  private boolean disallowIntercept = false;

  private int initChildLeft;
  private int initChildRight;
  private int initChildTop;
  private int initChildBottom;
  private float lastY;

  private CasAdapter adapter;
  private OnScrollListener scrollToLastListener;
  private Context context;

  public CascadeView(Context context) {
    super(context);
    init(context);
  }

  public CascadeView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public CascadeView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  private void init(Context c) {
    context = c;
    setOnClickListener(new OnClickListener() {
      @Override public void onClick(View v) {
        //
      }
    });
  }

  public void setAdapter(CasAdapter casAdapter) {
    if (adapter != null) {
      adapter.unregisterAdapterDataObserver(mObserver);
    }

    adapter = casAdapter;

    if (adapter == null) {
      throw new RuntimeException("adapter must not be null");
    } else {
      adapter.registerAdapterDataObserver(mObserver);
    }

    refresh();
  }

  private void refresh() {
    clear();

    coverPositionInItems = 0;

    if (adapter.getItemCount() > 0){
      for (int i = 0; i < visibleViewCount; i++) {
        ViewHolder vh = adapter.createView();
        add(vh, i);
      }
    }
  }

  public void setOnSwipeToLastListener(OnScrollListener listener) {
    scrollToLastListener = listener;
  }

  public void setDisallowIntercept(boolean disallow) {
    disallowIntercept = disallow;
  }

  private void add(ViewHolder holder, int position) {
    adapter.bindViewHolder(holder, position);
    cache.add(holder);
    addView(holder.itemView, position);
    cache.get(COVER_POSITION_IN_CACHE).itemView.bringToFront();
    remeasureChildren();
  }

  private void addToLast(ViewHolder holder) {
    coverPositionInItems++;
    adapter.bindViewHolder(holder, coverPositionInItems + 1);
    cache.add(holder);
    addView(holder.itemView, visibleViewCount - 1);
    cache.get(COVER_POSITION_IN_CACHE).itemView.bringToFront();
    remeasureChildren();
  }

  private void addToCover(ViewHolder holder) {
    coverPositionInItems--;
    adapter.bindViewHolder(holder, coverPositionInItems);
    cache.add(COVER_POSITION_IN_CACHE, holder);
    addView(holder.itemView);
    remeasureChildren();
    finishLastMoving(holder.itemView);
  }

  private void remove(ViewHolder holder, int holderPosition) {
    cache.remove(holderPosition);
    removeView(holder.itemView);
  }

  private void removeCover() {
    ViewHolder holder = getCoverViewHolder();
    remove(holder, COVER_POSITION_IN_CACHE);
    if (scrollToLast(coverPositionInItems, adapter.getItemCount())) {
      mAttachedScrap.add(holder);
      scrollToLastListener.onScrollToLast();
    } else {
      addToLast(holder);
    }
  }

  private void removeLast() {
    ViewHolder holder = getLastViewHolder();
    remove(holder, visibleViewCount - 1);
    if (scrollToCover()) {
      scrollToLastListener.onScrollToCover();
    } else {
      addToCover(holder);
    }
  }

  private boolean scrollToLast(int position, int size) {
    if (position >= size) {
      throw new IndexOutOfBoundsException("Invalid index " + position + ", size is " + size);
    }
    return coverPositionInItems == (size - 1) - 1;
  }

  private boolean scrollToCover() {
    return coverPositionInItems == 0;
  }

  private boolean scrollToTopEdge(float d) {
    return d < 0 && Math.abs(d) > getCoverViewHolder().itemView.getPaddingTop();
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
    final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

    measureChildren(widthMeasureSpec, heightMeasureSpec);

    int width = getPaddingLeft() + getPaddingRight() + getMeasuredWidth();
    int height = getPaddingTop() + getPaddingBottom() + getMeasuredHeight();

    switch (widthMode) {
      case MeasureSpec.EXACTLY:
      case MeasureSpec.AT_MOST:
        width = widthSize;
        break;
      case MeasureSpec.UNSPECIFIED:
      default:
        break;
    }

    switch (heightMode) {
      case MeasureSpec.EXACTLY:
      case MeasureSpec.AT_MOST:
        height = heightSize;
        break;
      case MeasureSpec.UNSPECIFIED:
      default:
        break;
    }

    setMeasuredDimension(width, height);
  }

  @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    int count = getChildCount();

    int l = getPaddingLeft();
    int t = getPaddingTop();

    for (int i = 0; i < count; i++) {
      View child = getChildAt(i);

      child.layout(l, t, child.getMeasuredWidth() + l, child.getMeasuredHeight() + t);
      if (i == count - 1) {
        initChildLeft = l;
        initChildRight = child.getMeasuredWidth() + l;
        initChildTop = t;
        initChildBottom = child.getMeasuredHeight() + t;
      }
    }
  }

  private void remeasureChildren() {
    int count = getChildCount();

    int l = getPaddingLeft();
    int t = getPaddingTop();

    if (count > 0) {
      View child = getChildAt(count - 1);
      initChildLeft = l;
      initChildRight = child.getMeasuredWidth() + l;
      initChildTop = t;
      initChildBottom = child.getMeasuredHeight() + t;
    }
  }

  @Override public boolean onInterceptTouchEvent(MotionEvent ev) {
    requestDisallowInterceptTouchEvent(disallowIntercept);
    return super.onInterceptTouchEvent(ev);
  }

  @Override public boolean onTouchEvent(MotionEvent event) {
    if (getChildCount() < visibleViewCount || mRunPredictiveAnimations) {
      return true;
    }

    int action = event.getAction();
    float y = event.getY();
    float delta = y - lastY;
    switch (action) {
      case MotionEvent.ACTION_DOWN:
        lastY = y;
        break;
      case MotionEvent.ACTION_MOVE:
        if (scrollToTopEdge(delta)) {
          return true;
        } else {
          moveCover((int) delta);
        }
        break;
      case MotionEvent.ACTION_CANCEL:
      case MotionEvent.ACTION_UP:
        if (scrollToTopEdge(delta)) {
          removeLast();
        } else {
          finishCoverMoving(getCoverViewHolder().itemView);
        }
        break;
    }
    return true;
  }

  private void moveCover(int distance) {
    ViewHolder holder = getCoverViewHolder();
    distance *= ratio;
    holder.itemView.layout(initChildLeft, initChildTop + distance, initChildRight,
        initChildBottom + distance);
  }

  private void finishCoverMoving(View v) {
    v.startAnimation(new MoveAnimation(v, 0, 0, v.getTop(), initChildBottom, false));
  }

  private void finishLastMoving(View v) {
    v.startAnimation(new MoveAnimation(v, 0, 0, initChildBottom, initChildTop, true));
  }

  private ViewHolder getViewHolderForPosition(int position) {
    if (cache == null || cache.isEmpty()) {
      throw new RuntimeException("cache size is empty");
    }
    if (position < 0 || position >= visibleViewCount) {
      throw new IndexOutOfBoundsException(
          "Invalid index " + position + ", size is " + visibleViewCount);
    }
    return cache.get(position);
  }

  private ViewHolder getCoverViewHolder() {
    return getViewHolderForPosition(COVER_POSITION_IN_CACHE);
  }

  private ViewHolder getLastViewHolder() {
    return getViewHolderForPosition(visibleViewCount - 1);
  }

  private void clear() {
    cache.clear();
    mAttachedScrap.clear();
    removeAllViews();
  }

  public CasAdapter getAdapter() {
    return adapter;
  }

  public abstract static class OnScrollListener {
    public void onScrollToLast() {
    }

    public void onScrollToCover() {

    }
  }

  public class MoveAnimation extends AnimationSet implements Animation.AnimationListener {
    private final View view;
    private final boolean up;
    private final TranslateAnimation translateAnimation;
    private final ScaleAnimation scaleAnimation;
    private final AlphaAnimation alphaAnimation;
    private final DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator();

    public MoveAnimation(final View v, float fromXDelta, float toXDelta, float fromYDelta,
        float toYDelta, final boolean upOrDown) {
      super(true);
      view = v;
      up = upOrDown;

      translateAnimation = new TranslateAnimation(fromXDelta, toXDelta, fromYDelta, toYDelta);
      translateAnimation.setInterpolator(decelerateInterpolator);
      translateAnimation.setAnimationListener(this);

      scaleAnimation =
          !up ? new ScaleAnimation(1.0f, 0.0f, 1.0f, 0.0f, Animation.RELATIVE_TO_SELF, 0.5f,
              Animation.RELATIVE_TO_SELF, 1.0f)
              : new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f,
                  Animation.RELATIVE_TO_SELF, 1.0f);
      alphaAnimation = !up ? new AlphaAnimation(1.0f, 0.0f) : new AlphaAnimation(0.0f, 1.0f);

      setDuration(500);
      setFillAfter(true);
      addAnimation(translateAnimation);
      addAnimation(scaleAnimation);
      addAnimation(alphaAnimation);
    }

    @Override public void onAnimationEnd(Animation animation) {
      mRunPredictiveAnimations = false;
      view.clearAnimation();
      if (!up) { //down
        removeCover();
      }
    }

    @Override public void onAnimationRepeat(Animation animation) {

    }

    @Override public void onAnimationStart(Animation animation) {
      mRunPredictiveAnimations = true;
    }
  }

  private final CasAdapterDataObserver mObserver = new CasAdapterDataObserver() {
    @Override public void onChanged() {
      refresh();
    }

    @Override public void onItemRangeChanged(int positionStart, int itemCount) {
    }

    @Override public void onItemRangeInserted(int positionStart, int itemCount) {
      if (mAttachedScrap.isEmpty()) {
        new RuntimeException("scrap heap must not be empty here");
      }
      addToLast(mAttachedScrap.get(0));
      mAttachedScrap.remove(0);
    }

    @Override public void onItemRangeRemoved(int positionStart, int itemCount) {
    }

    @Override public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
    }
  };

  public static abstract class CasAdapter<VH extends ViewHolder> {

    private final CasAdapterDataObservable mObservable = new CasAdapterDataObservable();

    public void registerAdapterDataObserver(CasAdapterDataObserver observer) {
      mObservable.registerObserver(observer);
    }

    public void unregisterAdapterDataObserver(CasAdapterDataObserver observer) {
      mObservable.unregisterObserver(observer);
    }

    public final void notifyDataSetChanged() {
      mObservable.notifyChanged();
    }

    public final void notifyItemRangeChanged(int positionStart, int itemCount) {
      mObservable.notifyItemRangeChanged(positionStart, itemCount);
    }

    public final void notifyItemRangeInserted(int positionStart, int itemCount) {
      mObservable.notifyItemRangeInserted(positionStart, itemCount);
    }

    public final void notifyItemMoved(int fromPosition, int toPosition) {
      mObservable.notifyItemMoved(fromPosition, toPosition);
    }

    public final void notifyItemRangeRemoved(int positionStart, int itemCount) {
      mObservable.notifyItemRangeRemoved(positionStart, itemCount);
    }

    public abstract VH onCreateView();

    public abstract void onBindViewHolder(VH holder, int position);

    public abstract int getItemCount();

    public final VH createView() {
      return onCreateView();
    }

    public final void bindViewHolder(VH holder, int position) {
      onBindViewHolder(holder, position);
    }
  }

  public abstract static class ViewHolder {
    public final View itemView;

    public ViewHolder(View v) {
      if (v == null) {
        throw new IllegalArgumentException("itemView must not be null");
      }
      itemView = v;
    }
  }
}
