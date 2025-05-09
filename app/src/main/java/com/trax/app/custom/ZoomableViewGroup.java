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

    // these matrices will be used to move and zoom image
    private Matrix matrix = new Matrix();
    private Matrix matrixInverse = new Matrix();
    private Matrix savedMatrix = new Matrix();
    // we can be in one of these 3 states
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;
    // remember some things for zooming
    private PointF start = new PointF();
    private PointF mid = new PointF();
    private float oldDist = 1f;
    private float[] lastEvent = null;

    private boolean initZoomApplied=false;

    private float[] mDispatchTouchEventWorkingArray = new float[2];
    private float[] mOnTouchEventWorkingArray = new float[2];

    private ImageView imageView;

    public void setImageView(ImageView imageView) {
        this.imageView = imageView;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        mDispatchTouchEventWorkingArray[0] = ev.getX();
        mDispatchTouchEventWorkingArray[1] = ev.getY();
        mDispatchTouchEventWorkingArray = screenPointsToScaledPoints(mDispatchTouchEventWorkingArray);
        ev.setLocation(mDispatchTouchEventWorkingArray[0],
                mDispatchTouchEventWorkingArray[1]);
        return super.dispatchTouchEvent(ev);
    }

    private float[] scaledPointsToScreenPoints(float[] a) {
        matrix.mapPoints(a);
        return a;
    }

    private float[] screenPointsToScaledPoints(float[] a){
        matrixInverse.mapPoints(a);
        return a;
    }

    public ZoomableViewGroup(Context context) {
        super(context);
        init(context);
    }

    public ZoomableViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomableViewGroup(Context context, AttributeSet attrs,
                             int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * Determine the space between the first two fingers
     */
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)Math.sqrt(x * x + y * y);
    }

    /**
     * Calculate the mid point of the first two fingers
     */
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }


    private void init(Context context){

    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = getChildCount();
        //TODO : Do not know why i was recieveing it 147
        t = 0;
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                child.layout(l, t, l+child.getMeasuredWidth(), t + child.getMeasuredHeight());
            }
        }
    }

    float container_width = 0.0f;
    float container_height = 0.0f;
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        float[] values = new float[9];
        matrix.getValues(values);
        container_width = values[Matrix.MSCALE_X]*widthSize;
        container_height = values[Matrix.MSCALE_Y]*heightSize;

        //Log.d("zoomToFit", "m width: "+container_width+" m height: "+container_height);
        //Log.d("zoomToFit", "m x: "+pan_x+" m y: "+pan_y);

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec);

                if(i==0 && !initZoomApplied && child.getWidth()>0){
                    int c_w = child.getWidth();
                    int c_h = child.getHeight();

                    //zoomToFit(c_w, c_h, container_width, container_height);
                }
            }
        }

    }

    private void zoomToFit(int c_w, int c_h, float container_width, float container_height){
        float proportion_firstChild = (float)c_w/(float)c_h;
        float proportion_container = container_width/container_height;

        //Log.d("zoomToFit", "firstChildW: "+c_w+" firstChildH: "+c_h);
        //Log.d("zoomToFit", "proportion-container: "+proportion_container);
        //Log.d("zoomToFit", "proportion_firstChild: "+proportion_firstChild);

        if(proportion_container<proportion_firstChild){
            float initZoom = container_height/c_h;
            //Log.d("zoomToFit", "adjust height with initZoom: "+initZoom);
            matrix.postScale(initZoom, initZoom);
            matrix.postTranslate(-1*(c_w*initZoom-container_width)/2, 0);
            matrix.invert(matrixInverse);
        }else {
            float initZoom = container_width/c_w;
            //Log.d("zoomToFit", "adjust width with initZoom: "+initZoom);
            matrix.postScale(initZoom, initZoom);
            matrix.postTranslate(0, -1*(c_h*initZoom-container_height)/2);
            matrix.invert(matrixInverse);
        }
        initZoomApplied=true;
        invalidate();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save();
        canvas.setMatrix(matrix);
        super.dispatchDraw(canvas);
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // handle touch events here
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
                //d = rotation(event);
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
                    if(transX+dx < -(container_width/2)){
                        dx = -container_width/2-transX;
                    }
                    if(transY+dy < -(container_height/2)){
                        dy = -container_height/2-transY;
                    }
                    if(transX+dx > container_width/2){
                        dx = container_width/2-transX;
                    }
                    if(transY+dy > container_height/2){
                        dy = container_height/2-transY;
                    }
                    Log.d("Zoom", "onTouchEvent: Post X "+transX+" "+dx);
                    //Log.d("Zoom", "onTouchEvent: Post Y "+transY+" "+dy);
                    matrix.set(savedMatrix);
                    matrix.postTranslate(dx, dy);
                    matrix.invert(matrixInverse);
                    if(actionListener != null){
                        actionListener.onDrag(matrix);
                    }
                } else if (mode == ZOOM) {
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        savedMatrix.getValues(f);
                        float scaleX = f[Matrix.MSCALE_X];
                        float scaleY = f[Matrix.MSCALE_Y];

                        //Log.d("Zoom", "onTouchEvent: Matrix "+f[0]+" "+f[1]+" "+f[2]+" "+f[3]+" "+f[4]+" "+f[5]+" "+f[6]+" "+f[7]+" "+f[8]);
                        float scale = (newDist / oldDist);
                        //Log.d("Zoom", "onTouchEvent: Post "+scale);
                        //Log.d("Zoom", "onTouchEvent: Post "+scaleX+" "+scale);
                        if(scaleX*scale >= 1 && scaleX*scale <= 5){
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
    float[] f = new float[9];


    Actions actionListener;

    public void setActionListener(Actions actionListener){
        this.actionListener = actionListener;
    }

    public interface Actions{
        public void onDrag(float dx,float dy);
        public void onDrag(Matrix matrix);
    }

}
