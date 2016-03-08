package com.lawson.library;

import android.content.Context;
import android.support.annotation.DimenRes;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lawson on 16/2/14.
 */
public class CascadeView extends FrameLayout {

  private static final int DEFAULT_COUNT = 2;
  private static final int MIN_FLING_DISTANCE = 80;
  private static final int COVER_POSITION_IN_CACHE = 0;

  private int insets = 0;

  private final List<ViewHolder> cache = new ArrayList<>();
  private final ArrayList<ViewHolder> mAttachedScrap = new ArrayList<>();
  private final LinearOutSlowInInterpolator linearOutSlowInInterpolator =
      new LinearOutSlowInInterpolator();
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

    for (int i = 0; i < visibleViewCount; i++) {
      ViewHolder vh = adapter.createView();
      add(vh, i);
    }
  }

  public void setInsets(@DimenRes int id) {
    insets = context.getResources().getDimensionPixelSize(id);
  }

  public void setInsetsPx(int px) {
    insets = context.getResources().getDimensionPixelSize(px);
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
    return d < 0 && Math.abs(d) > MIN_FLING_DISTANCE;
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
    final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

    int width;
    int height;

    switch (widthMode) {
      case MeasureSpec.EXACTLY:
      case MeasureSpec.AT_MOST:
        width = widthSize;
        break;
      case MeasureSpec.UNSPECIFIED:
      default:
        width = ViewCompat.getMinimumWidth(this);
        break;
    }

    switch (heightMode) {
      case MeasureSpec.EXACTLY:
      case MeasureSpec.AT_MOST:
        height = heightSize;
        break;
      case MeasureSpec.UNSPECIFIED:
      default:
        height = ViewCompat.getMinimumHeight(this);
        break;
    }

    setMeasuredDimension(width, height);
    measureChildren(widthMeasureSpec - insets, heightMeasureSpec - insets);
  }

  @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    int count = getChildCount();
    for (int i = 0; i < count; i++) {
      View child = getChildAt(i);

      child.layout(insets, insets, child.getMeasuredWidth(), child.getMeasuredHeight());
      if (i == count - 1) {
        initChildLeft = insets;
        initChildRight = child.getMeasuredWidth();
        initChildTop = insets;
        initChildBottom = child.getMeasuredHeight();
      }
    }
  }

  private void remeasureChildren() {
    int count = getChildCount();
    if (count > 0) {
      View child = getChildAt(count - 1);
      initChildLeft = insets;
      initChildRight = child.getMeasuredWidth();
      initChildTop = insets;
      initChildBottom = child.getMeasuredHeight();
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
          ViewHolder holder = getCoverViewHolder();
          finishCoverMoving(holder.itemView);
        }
        break;
    }
    return true;
  }

  private void moveCover(int distance) {
    ViewHolder holder = getCoverViewHolder();
    holder.itemView.layout(initChildLeft, initChildTop + distance, initChildRight,
        initChildBottom + distance);
  }

  private void finishCoverMoving(View v) {
    finishMovingAnimation(v, 0, 0, v.getTop(), initChildBottom, false);
  }

  private void finishLastMoving(View v) {
    finishMovingAnimation(v, 0, 0, initChildBottom, initChildTop, true);
  }

  public void finishMovingAnimation(final View v, float fromXDelta, float toXDelta,
      float fromYDelta, float toYDelta, final boolean upOrDown) {
    TranslateAnimation animation =
        new TranslateAnimation(fromXDelta, toXDelta, fromYDelta, toYDelta);
    animation.setInterpolator(linearOutSlowInInterpolator);
    animation.setDuration(400);
    animation.setFillAfter(true);
    animation.setAnimationListener(new Animation.AnimationListener() {
      @Override public void onAnimationStart(Animation animation) {
        mRunPredictiveAnimations = true;
      }

      @Override public void onAnimationRepeat(Animation animation) {
      }

      @Override public void onAnimationEnd(Animation animation) {
        mRunPredictiveAnimations = false;
        v.clearAnimation();
        if (!upOrDown) { //down
          removeCover();
        }
      }
    });
    v.startAnimation(animation);
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

  private final CasAdapterDataObserver mObserver = new CasAdapterDataObserver() {
    @Override public void onChanged() {
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
