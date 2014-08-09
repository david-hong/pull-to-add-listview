package com.pulltoaddlistview.app;

import android.content.Context;
import android.util.AttributeSet;
import android.util.AttributeSet;
import android.view.*;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.*;
import android.view.animation.Animation.AnimationListener;
import android.widget.*;

/**
 * A generic, customizable Android ListView implementation that has 'Pull to Add' functionality
 * Built on the open source project 'Pull to Refresh' (https://github.com/erikwt/PullToRefresh-ListView)
 * <p/>
 * This ListView can be used in place of the normal Android android.widget.ListView class.
 * <p/>
 * The using class can call setAdding() to set the state explicitly to ADDING. This
 * is useful when you want to show the spinner and 'ADDING' text when the
 * refresh was not triggered by 'Pull to Refresh', for example on start.
 * <p/>
 * For more information, visit the project page:
 * https://github.com/erikwt/PullToRefresh-ListView
 *
 * @author David Hong (http://david-hong.com/)
 * @version 1.0.0
 */
public class PullToAddListView extends ListView{

    // Animation variables, change to meet animation needs
    private static final float PULL_RESISTANCE                 = 1.7f;
    private static final int   BOUNCE_ANIMATION_DURATION       = 700;
    private static final int   BOUNCE_ANIMATION_DELAY          = 100;
    private static final float BOUNCE_OVERSHOOT_TENSION        = 1.4f;

    public static enum State{
        PULL_TO_ADD,
        RELEASE_TO_ADD,
        ADDING
    }

    private static int measuredHeaderHeight;

    private boolean scrollbarEnabled;
    private boolean bounceBackHeader;
    private boolean lockScrollWhileAdding;

    private float                   previousY;
    private int                     headerPadding;
    private boolean                 hasResetHeader;
    private State                   state;
    private LinearLayout            headerContainer;
    private RelativeLayout          header;
    private TextView                text;
    private Button                  add;
    private OnItemClickListener     onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;

    private float mScrollStartY;
    private final int IDLE_DISTANCE = 5;

    public PullToAddListView(Context context){
        super(context);
        init();
    }

    public PullToAddListView(Context context, AttributeSet attrs){
        super(context, attrs);
        init();
    }

    public PullToAddListView(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
        init();
    }

    @Override
    public void setOnItemClickListener(OnItemClickListener onItemClickListener){
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener){
        this.onItemLongClickListener = onItemLongClickListener;
    }

    /**
     * @return If the list is in 'ADDING' state
     */
    public boolean isAdding(){
        return state == State.ADDING;
    }

    /**
     * Default is false. When lockScrollWhileAdding is set to true, the list
     * cannot scroll when in 'Adding' mode. It's 'locked' on Adding.
     *
     * @param lockScrollWhileAdding
     */
    public void setLockScrollWhileAdding(boolean lockScrollWhileAdding){
        this.lockScrollWhileAdding = lockScrollWhileAdding;
    }


    /**
     * Explicitly set the state to Adding.
     */
    public void setAdding(){
        state = State.ADDING;
        scrollTo(0, 0);
        setUiAdding();
        setHeaderPadding(0);
    }


    private void init(){
        setVerticalFadingEdgeEnabled(false);

        headerContainer = (LinearLayout) LayoutInflater.from(getContext()).inflate(R.layout.ptr_header, null);
        header = (RelativeLayout) headerContainer.findViewById(R.id.ptr_id_header);
        text = (EditText) header.findViewById(R.id.ptr_id_text);

        addHeaderView(headerContainer);
        setState(State.PULL_TO_ADD);
        scrollbarEnabled = isVerticalScrollBarEnabled();

        ViewTreeObserver vto = header.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new PTROnGlobalLayoutListener());

        super.setOnItemClickListener(new PTROnItemClickListener());
        super.setOnItemLongClickListener(new PTROnItemLongClickListener());

    }

    private void setHeaderPadding(int padding){
        headerPadding = padding;

        MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) header.getLayoutParams();
        mlp.setMargins(0, Math.round(padding), 0, 0);
        header.setLayoutParams(mlp);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if(lockScrollWhileAdding
                && (state == State.ADDING || getAnimation() != null && !getAnimation().hasEnded())){
            return true;
        }

        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                if(getFirstVisiblePosition() == 0){
                    previousY = event.getY();
                }
                else {
                    previousY = -1;
                }

                // Remember where have we started
                mScrollStartY = event.getY();

                break;

            case MotionEvent.ACTION_UP:
                if(previousY != -1 && (state == State.RELEASE_TO_ADD || getFirstVisiblePosition() == 0)){
                    switch(state){
                        case RELEASE_TO_ADD:
                            setState(State.ADDING);
                            bounceBackHeader();

                            break;

                        case PULL_TO_ADD:
                            resetHeader();
                            break;
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if(previousY != -1 && getFirstVisiblePosition() == 0 && Math.abs(mScrollStartY-event.getY()) > IDLE_DISTANCE){
                    float y = event.getY();
                    float diff = y - previousY;
                    if(diff > 0) diff /= PULL_RESISTANCE;
                    previousY = y;

                    int newHeaderPadding = Math.max(Math.round(headerPadding + diff), -header.getHeight());

                    if(newHeaderPadding != headerPadding && state != State.ADDING){
                        setHeaderPadding(newHeaderPadding);

                        if(state == State.PULL_TO_ADD && headerPadding > 0){
                            setState(State.RELEASE_TO_ADD);

                        }else if(state == State.RELEASE_TO_ADD && headerPadding < 0){
                            setState(State.PULL_TO_ADD);

                        }
                    }
                }

                break;
        }

        return super.onTouchEvent(event);
    }

    // Resets the header and animates the reset
    public void bounceBackHeader(){
        int yTranslate = state == State.ADDING ?
                header.getHeight() - headerContainer.getHeight() :
                -headerContainer.getHeight() - headerContainer.getTop() + getPaddingTop();;

        TranslateAnimation bounceAnimation = new TranslateAnimation(
                TranslateAnimation.ABSOLUTE, 0,
                TranslateAnimation.ABSOLUTE, 0,
                TranslateAnimation.ABSOLUTE, 0,
                TranslateAnimation.ABSOLUTE, yTranslate);

        bounceAnimation.setDuration(BOUNCE_ANIMATION_DURATION);
        bounceAnimation.setFillEnabled(true);
        bounceAnimation.setFillAfter(false);
        bounceAnimation.setFillBefore(true);
        bounceAnimation.setInterpolator(new OvershootInterpolator(BOUNCE_OVERSHOOT_TENSION));
        bounceAnimation.setAnimationListener(new HeaderAnimationListener(yTranslate));

        startAnimation(bounceAnimation);
    }

    public void resetHeader(){
        if(getFirstVisiblePosition() > 0){
            setHeaderPadding(-header.getHeight());
            setState(State.PULL_TO_ADD);
            return;
        }

        if(getAnimation() != null && !getAnimation().hasEnded()){
            bounceBackHeader = true;
        }else{
            bounceBackHeader();
        }
    }

    private void setUiAdding(){
        // Set UI for adding here, eg. dim the listview while asking the user for info of a new view
    }

    public void setState(State state){
        this.state = state;
        switch(state){
            case PULL_TO_ADD:
                break;

            case RELEASE_TO_ADD:
                break;

            case ADDING:
                setUiAdding();

                break;
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt){
        super.onScrollChanged(l, t, oldl, oldt);

        if(!hasResetHeader){
            if(measuredHeaderHeight > 0 && state != State.ADDING){
                setHeaderPadding(-measuredHeaderHeight);
            }

            hasResetHeader = true;
        }
    }

    private class HeaderAnimationListener implements AnimationListener{

        private int height, translation;
        private State stateAtAnimationStart;

        public HeaderAnimationListener(int translation){
            this.translation = translation;
        }

        @Override
        public void onAnimationStart(Animation animation){
            stateAtAnimationStart = state;

            android.view.ViewGroup.LayoutParams lp = getLayoutParams();
            height = lp.height;
            lp.height = getHeight() - translation;
            setLayoutParams(lp);

            if(scrollbarEnabled){
                setVerticalScrollBarEnabled(false);
            }
        }

        @Override
        public void onAnimationEnd(Animation animation){
            setHeaderPadding(stateAtAnimationStart == State.ADDING ? 0 : -measuredHeaderHeight - headerContainer.getTop());
            setSelection(0);

            android.view.ViewGroup.LayoutParams lp = getLayoutParams();
            lp.height = height;
            setLayoutParams(lp);

            if(scrollbarEnabled){
                setVerticalScrollBarEnabled(true);
            }

            if(bounceBackHeader){
                bounceBackHeader = false;

                postDelayed(new Runnable(){

                    @Override
                    public void run(){
                        resetHeader();
                    }
                }, BOUNCE_ANIMATION_DELAY);
            }else if(stateAtAnimationStart != State.ADDING){
                setState(State.PULL_TO_ADD);
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation){}
    }

    private class PTROnGlobalLayoutListener implements OnGlobalLayoutListener{

        @Override
        public void onGlobalLayout(){
            int initialHeaderHeight = header.getHeight();

            if(initialHeaderHeight > 0){
                measuredHeaderHeight = initialHeaderHeight;

                if(measuredHeaderHeight > 0 && state != State.ADDING){
                    setHeaderPadding(-measuredHeaderHeight);
                    requestLayout();
                }
            }

            getViewTreeObserver().removeGlobalOnLayoutListener(this);
        }
    }

    private class PTROnItemClickListener implements OnItemClickListener{
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id){
            hasResetHeader = false;

            if(onItemClickListener != null && state == State.PULL_TO_ADD){
                // Passing up onItemClick. Correct position with the number of header views
                onItemClickListener.onItemClick(adapterView, view, position - getHeaderViewsCount(), id);
            }
            resetHeader();
        }
    }

    private class PTROnItemLongClickListener implements OnItemLongClickListener{

        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id){
            hasResetHeader = false;

            if(onItemLongClickListener != null && state == State.PULL_TO_ADD){
                // Passing up onItemLongClick. Correct position with the number of header views
                return onItemLongClickListener.onItemLongClick(adapterView, view, position - getHeaderViewsCount(), id);
            }
            resetHeader();
            return false;
        }
    }
}
