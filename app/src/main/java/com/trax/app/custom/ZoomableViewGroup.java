package com.trax.app.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class ZoomableViewGroup extends ViewGroup {

    //==============================================================================
    // Variables
    //==============================================================================

    private Matrix matrix = new Matrix();
    private Matrix matrixInverse = new Matrix();
    private Matrix savedMatrix = new Matrix();
    
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    private PointF start = new PointF();
    private PointF mid = new PointF();
    private float oldDist = 1f;
    private float[] lastEvent = null;

    private boolean initZoomApplied = false;

    private float[] mDispatchTouchEventWorkingArray = new float[2];
    private float[] mOnTouchEventWorkingArray = new float[2];

    private ImageView imageView;
    private float container_width = 0.0f;
    private float container_height = 0.0f;
    private float[] f = new float[9];
    private Actions actionListener;

    //==============================================================================
    // Constructors
    //==============================================================================

    //--------------------------------------------------
    // Constructor for dynamic instantiation in code.
    //--------------------------------------------------
    public ZoomableViewGroup(Context context) {
        super(context);
        init(context);
    }

    //--------------------------------------------------
    // Constructor for XML layout inflation.
    //--------------------------------------------------
    public ZoomableViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    //--------------------------------------------------
    // Constructor for default styled layouts XML inflation.
    //--------------------------------------------------
    public ZoomableViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    //==============================================================================
    // Setter Methods
    //==============================================================================

    //--------------------------------------------------
    // Assigns target ImageView references.
    //--------------------------------------------------
    public void setImageView(ImageView imageView) {
        this.imageView = imageView;
    }

    //--------------------------------------------------
    // Registers zoom and drag event callbacks listener.
    //--------------------------------------------------
    public void setActionListener(Actions actionListener) {
        this.actionListener = actionListener;
    }

    //==============================================================================
    // View Measurement & Layout Overrides
    //==============================================================================

    //--------------------------------------------------
    // Layouts child views to their measured positions.
    //--------------------------------------------------
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = getChildCount();
        t = 0;
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                child.layout(l, t, l + child.getMeasuredWidth(), t + child.getMeasuredHeight());
            }
        }
    }

    //--------------------------------------------------
    // Measures child views dimension specifications.
    //--------------------------------------------------
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        float[] values = new float[9];
        matrix.getValues(values);
        container_width = values[Matrix.MSCALE_X] * widthSize;
        container_height = values[Matrix.MSCALE_Y] * heightSize;

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
            }
        }
    }

    //--------------------------------------------------
    // Scales the matrix parameters to fit the container bounds.
    //--------------------------------------------------
    private void zoomToFit(int c_w, int c_h, float container_width, float container_height) {
        float proportion_firstChild = (float) c_w / (float) c_h;
        float proportion_container = container_width / container_height;

        if (proportion_container < proportion_firstChild) {
            float initZoom = container_height / c_h;
            matrix.postScale(initZoom, initZoom);
            matrix.postTranslate(-1 * (c_w * initZoom - container_width) / 2, 0);
            matrix.invert(matrixInverse);
        } else {
            float initZoom = container_width / c_w;
            matrix.postScale(initZoom, initZoom);
            matrix.postTranslate(0, -1 * (c_h * initZoom - container_height) / 2);
            matrix.invert(matrixInverse);
        }
        initZoomApplied = true;
        invalidate();
    }

    //--------------------------------------------------
    // Renders child views with active scaling transformations.
    //--------------------------------------------------
    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save();
        canvas.setMatrix(matrix);
        super.dispatchDraw(canvas);
        canvas.restore();
    }

    //==============================================================================
    // Touch Event Handling
    //==============================================================================

    //--------------------------------------------------
    // Standardizes touch coordinates before routing touch dispatches.
    //--------------------------------------------------
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        mDispatchTouchEventWorkingArray[0] = ev.getX();
        mDispatchTouchEventWorkingArray[1] = ev.getY();
        mDispatchTouchEventWorkingArray = screenPointsToScaledPoints(mDispatchTouchEventWorkingArray);
        ev.setLocation(mDispatchTouchEventWorkingArray[0], mDispatchTouchEventWorkingArray[1]);
        return super.dispatchTouchEvent(ev);
    }

    //--------------------------------------------------
    // Maps scaled points to screen points.
    //--------------------------------------------------
    private float[] scaledPointsToScreenPoints(float[] a) {
        matrix.mapPoints(a);
        return a;
    }

    //--------------------------------------------------
    // Maps screen points to scaled points.
    //--------------------------------------------------
    private float[] screenPointsToScaledPoints(float[] a) {
        matrixInverse.mapPoints(a);
        return a;
    }

    //--------------------------------------------------
    // Processes gestures for dragging and pinch-zooming layouts.
    //--------------------------------------------------
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mOnTouchEventWorkingArray[0] = event.getX();
        mOnTouchEventWorkingArray[1] = event.getY();
        mOnTouchEventWorkingArray = scaledPointsToScreenPoints(mOnTouchEventWorkingArray);
        event.setLocation(mOnTouchEventWorkingArray[0], mOnTouchEventWorkingArray[1]);

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                mode = DRAG;
                lastEvent = null;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                }
                lastEvent = new float[4];
                lastEvent[0] = event.getX(0);
                lastEvent[1] = event.getX(1);
                lastEvent[2] = event.getY(0);
                lastEvent[3] = event.getY(1);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                lastEvent = null;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    savedMatrix.getValues(f);
                    float transX = f[Matrix.MTRANS_X];
                    float transY = f[Matrix.MTRANS_Y];
                    float dx = event.getX() - start.x;
                    float dy = event.getY() - start.y;
                    if (transX + dx < -(container_width / 2)) {
                        dx = -container_width / 2 - transX;
                    }
                    if (transY + dy < -(container_height / 2)) {
                        dy = -container_height / 2 - transY;
                    }
                    if (transX + dx > container_width / 2) {
                        dx = container_width / 2 - transX;
                    }
                    if (transY + dy > container_height / 2) {
                        dy = container_height / 2 - transY;
                    }
                    Log.d("Zoom", "onTouchEvent: Post X " + transX + " " + dx);
                    matrix.set(savedMatrix);
                    matrix.postTranslate(dx, dy);
                    matrix.invert(matrixInverse);
                    if (actionListener != null) {
                        actionListener.onDrag(matrix);
                    }
                } else if (mode == ZOOM) {
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        savedMatrix.getValues(f);
                        float scaleX = f[Matrix.MSCALE_X];
                        float scale = (newDist / oldDist);
                        if (scaleX * scale >= 1 && scaleX * scale <= 5) {
                            matrix.set(savedMatrix);
                            matrix.postScale(scale, scale, mid.x, mid.y);
                            matrix.invert(matrixInverse);
                        }
                    }
                }
                break;
        }

        invalidate();
        return true;
    }

    //==============================================================================
    // Utility / Calculation Functions
    //==============================================================================

    //--------------------------------------------------
    // Calculates spacing distances between two fingers.
    //--------------------------------------------------
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    //--------------------------------------------------
    // Calculates coordinates of center points between two fingers.
    //--------------------------------------------------
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    //--------------------------------------------------
    // Initializes the ViewGroup layouts parameters.
    //--------------------------------------------------
    private void init(Context context) {
    }

    //==============================================================================
    // Interfaces
    //==============================================================================

    public interface Actions {
        void onDrag(float dx, float dy);
        void onDrag(Matrix matrix);
    }
}
