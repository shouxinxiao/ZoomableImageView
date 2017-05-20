package com.sean.zoomableimageview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Build;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;


public class ZoomableImageView extends View {

	// Statics
	static final float sPanRate = 7;
	static final float sScaleRate = 1.25F;
	static final int sPaintDelay = 250;
	static final int sAnimationDelay = 500;


	private Matrix mBaseMatrix = new Matrix();

	
	private Matrix mSuppMatrix = new Matrix();

	private Matrix mDisplayMatrix = new Matrix();

	private Matrix mMatrix = new Matrix();

	private Paint mPaint;

	private float[] mMatrixValues = new float[9];

	private Bitmap mBitmap;

	private int mThisWidth = -1, mThisHeight = -1;

	private float mMaxZoom;

	private Runnable mOnLayoutRunnable = null;

	private Runnable mRefresh = null;

	private Runnable mFling = null;

	private double mLastDraw = 0;

	private ScaleGestureDetector mScaleDetector;
	private GestureDetector mGestureDetector;
	
	private OnImageTouchedListener mImageTouchedListener;

	public ZoomableImageView(Context context) {
		super(context);
		init( context );
	}
	
	public void setOnImageTouchedListener( OnImageTouchedListener listener ){
		this.mImageTouchedListener = listener;
	}

	public ZoomableImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init( context );
	}

	private void init( Context context) {
		mPaint = new Paint();
		mPaint.setDither(true);
		mPaint.setFilterBitmap(true);
		mPaint.setAntiAlias(true);

		mRefresh = new Runnable() {
			@Override
			public void run() {
				postInvalidate();
			}
		};

		mScaleDetector = new ScaleGestureDetector( context, new ScaleListener() );
		mGestureDetector = new GestureDetector(context, new MyGestureListener());
		
		if( Build.VERSION.SDK_INT >=  Build.VERSION_CODES.HONEYCOMB )
			setLayerType(View.LAYER_TYPE_HARDWARE, null);
	}

	public Bitmap getImageBitmap(){
		return mBitmap;
	}

	public void clear(){
		if(mBitmap!=null)
			mBitmap = null;
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		mThisWidth = right - left;
		mThisHeight = bottom - top;
		Runnable r = mOnLayoutRunnable;
		if (r != null) {
			mOnLayoutRunnable = null;
			r.run();
		}
		if (mBitmap != null) {
			setBaseMatrix(mBitmap, mBaseMatrix);
			setImageMatrix(getImageViewMatrix());
		}
	}

	static private void translatePoint(Matrix matrix, float [] xy) {
		matrix.mapPoints(xy);
	}

	public void setImageMatrix(Matrix m){
		if (m != null && m.isIdentity()) {
			m = null;
		}

		if (m == null && !this.mMatrix.isIdentity() || m != null && !this.mMatrix.equals(m)) {
			this.mMatrix.set(m);
			invalidate();
		}
	}

	public void setImageBitmap(final Bitmap bitmap) {
		final int viewWidth = getWidth();
		
		if( Build.VERSION.SDK_INT >=  Build.VERSION_CODES.HONEYCOMB && bitmap!=null && bitmap.getHeight()>1800 )
			setLayerType(View.LAYER_TYPE_SOFTWARE, null);

		if (viewWidth <= 0)  {
			mOnLayoutRunnable = new Runnable() {
				public void run() {
					setImageBitmap(bitmap);
				}
			};
			return;
		}

		if (bitmap != null) {
			setBaseMatrix(bitmap, mBaseMatrix);
			this.mBitmap = bitmap;
		} else {
			mBaseMatrix.reset();
			this.mBitmap = bitmap;
		}

		mSuppMatrix.reset();
		setImageMatrix(getImageViewMatrix());
		mMaxZoom = maxZoom();
		
		zoomTo(zoomDefault());
	}

	protected void center(boolean vertical, boolean horizontal, boolean animate) {
		if (mBitmap == null)
			return;

		Matrix m = getImageViewMatrix();

		float [] topLeft  = new float[] { 0, 0 };
		float [] botRight = new float[] { mBitmap.getWidth(), mBitmap.getHeight() };

		translatePoint(m, topLeft);
		translatePoint(m, botRight);

		float height = botRight[1] - topLeft[1];
		float width  = botRight[0] - topLeft[0];

		float deltaX = 0, deltaY = 0;

		if (vertical) {
			int viewHeight = getHeight();
			if (height < viewHeight) {
				deltaY = (viewHeight - height)/2 - topLeft[1];
			} else if (topLeft[1] > 0) {
				deltaY = -topLeft[1];
			} else if (botRight[1] < viewHeight) {
				deltaY = getHeight() - botRight[1];
			}
		}

		if (horizontal) {
			int viewWidth = getWidth();
			if (width < viewWidth) {
				deltaX = (viewWidth - width)/2 - topLeft[0];
			} else if (topLeft[0] > 0) {
				deltaX = -topLeft[0];
			} else if (botRight[0] < viewWidth) {
				deltaX = viewWidth - botRight[0];
			}
		}

		postTranslate(deltaX, deltaY);
		if (animate) {
			Animation a = new TranslateAnimation(-deltaX, 0, -deltaY, 0);
			a.setStartTime(SystemClock.elapsedRealtime());
			a.setDuration(250);
			setAnimation(a);
		}
		setImageMatrix(getImageViewMatrix());
	}

	protected float getValue(Matrix matrix, int whichValue) {
		matrix.getValues(mMatrixValues);
		return mMatrixValues[whichValue];
	}

	protected float getScale(Matrix matrix) {

		if(mBitmap!=null)
			return getValue(matrix, Matrix.MSCALE_X);
		else
			return 1f;
	}

	public float getScale() {
		return getScale(mSuppMatrix);
	}

	private void setBaseMatrix(Bitmap bitmap, Matrix matrix) {
		float viewWidth = getWidth();
		float viewHeight = getHeight();

		matrix.reset();
		float widthScale = Math.min(viewWidth / (float)bitmap.getWidth(), 1.0f);
		float heightScale = Math.min(viewHeight / (float)bitmap.getHeight(), 1.0f);
		float scale;
		if (widthScale > heightScale) {
			scale = heightScale;
		} else {
			scale = widthScale;
		}
		matrix.setScale(scale, scale);
		matrix.postTranslate(
				(viewWidth  - ((float)bitmap.getWidth()  * scale))/2F, 
				(viewHeight - ((float)bitmap.getHeight() * scale))/2F);
	}


	protected Matrix getImageViewMatrix() {
		mDisplayMatrix.set(mBaseMatrix);
		mDisplayMatrix.postConcat(mSuppMatrix);
		return mDisplayMatrix;
	}

	// 200%.
	protected float maxZoom() {
		if (mBitmap == null)
			return 1F;

		float fw = (float) mBitmap.getWidth()  / (float)mThisWidth;
		float fh = (float) mBitmap.getHeight() / (float)mThisHeight;
		float max = Math.max(fw, fh) * 16;
		return max;
	}
	
	public float zoomDefault() {
		if (mBitmap == null)
			return 1F;

		float fw = (float)mThisWidth/(float)mBitmap.getWidth();
		float fh = (float)mThisHeight/(float)mBitmap.getHeight();
		return Math.max(Math.min(fw, fh),1);
	}

	protected void zoomTo(float scale, float centerX, float centerY) {
		if (scale > mMaxZoom) {
			scale = mMaxZoom;
		}

		float oldScale = getScale();
		float deltaScale = scale / oldScale;

		mSuppMatrix.postScale(deltaScale, deltaScale, centerX, centerY);
		setImageMatrix(getImageViewMatrix());
		center(true, true, false);
	}

	protected void zoomTo(final float scale, final float centerX, final float centerY, final float durationMs) {
		final float incrementPerMs = (scale - getScale()) / durationMs;
		final float oldScale = getScale();
		final long startTime = System.currentTimeMillis();

		post(new Runnable() {
			public void run() {
				long now = System.currentTimeMillis();
				float currentMs = Math.min(durationMs, (float)(now - startTime));
				float target = oldScale + (incrementPerMs * currentMs);
				zoomTo(target, centerX, centerY);

				if (currentMs < durationMs) {
					post(this);
				}
			}
		});
	}

	public void zoomTo(float scale) {
		float width = getWidth();
		float height = getHeight();

		zoomTo(scale, width/2F, height/2F);
	}

	protected void zoomIn() {
		zoomIn(sScaleRate);
	}

	protected void zoomOut() {
		zoomOut(sScaleRate);
	}

	protected void zoomIn(float rate) {
		if (getScale() >= mMaxZoom) {
		}
		if (mBitmap == null) {
			return;
		}

		float width = getWidth();
		float height = getHeight();

		mSuppMatrix.postScale(rate, rate, width/2F, height/2F);
		setImageMatrix(getImageViewMatrix());

	}

	// Unchanged from ImageViewTouchBase
	protected void zoomOut(float rate) {
		if (mBitmap == null) {
			return;
		}

		float width = getWidth();
		float height = getHeight();

		Matrix tmp = new Matrix(mSuppMatrix);
		tmp.postScale(1F/sScaleRate, 1F/sScaleRate, width/2F, height/2F);
		if (getScale(tmp) < 1F) {
			mSuppMatrix.setScale(1F, 1F, width/2F, height/2F);
		} else {
			mSuppMatrix.postScale(1F/rate, 1F/rate, width/2F, height/2F);
		}
		setImageMatrix(getImageViewMatrix());
		center(true, true, false);

	}

	// Unchanged from ImageViewTouchBase
	protected void postTranslate(float dx, float dy) {
		mSuppMatrix.postTranslate(dx, dy);
	}

	// Fling a view by a distance over time
	protected void scrollBy( float distanceX, float distanceY, final float durationMs ){
		final float dx = distanceX;
		final float dy = distanceY;
		final long startTime = System.currentTimeMillis();

		mFling = new Runnable() {
			float old_x	= 0;
			float old_y	= 0;

			public void run()
			{
				long now = System.currentTimeMillis();
				float currentMs = Math.min( durationMs, now - startTime );
				float x = easeOut( currentMs, 0, dx, durationMs );
				float y = easeOut( currentMs, 0, dy, durationMs );
				postTranslate( ( x - old_x ), ( y - old_y ) );
				center(true, true, false);

				old_x = x;
				old_y = y;
				if ( currentMs < durationMs ) {
					post( this );
				}
			}
		};
		post( mFling );
	}

	// Gradually slows down a fling velocity
	private float easeOut( float time, float start, float end, float duration){
		return end * ( ( time = time / duration - 1 ) * time * time + 1 ) + start;
	}

	// Custom draw operation to draw the bitmap using mMatrix
	@Override
	protected void onDraw(Canvas canvas) {

		// Check if the bitmap was ever set
		if(mBitmap!=null && !mBitmap.isRecycled() ){

			// If the current version is above Gingerbread and the layer type is 
			// hardware accelerated, the paint is no longer needed
			if( Build.VERSION.SDK_INT >=  Build.VERSION_CODES.HONEYCOMB 
					&& getLayerType() == View.LAYER_TYPE_HARDWARE ){
				canvas.drawBitmap(mBitmap, mMatrix, null);
			}else{
				// Check if the time between draws has been met and draw the bitmap
				if( (System.currentTimeMillis()-mLastDraw) > sPaintDelay ){
					canvas.drawBitmap(mBitmap, mMatrix, mPaint);
					mLastDraw = System.currentTimeMillis();
				}

				// Otherwise draw the bitmap without the paint and resubmit a new request
				else{
					canvas.drawBitmap(mBitmap, mMatrix, null);
					removeCallbacks(mRefresh);
					postDelayed(mRefresh, sPaintDelay);
				}
			}
		}
	}

	// Adjusts the zoom of the view
	class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

		@Override
		public boolean onScale( ScaleGestureDetector detector )
		{	
			// Check if the detector is in progress in order to proceed
			if(detector!=null && detector.isInProgress() ){
				try{
					// Grab the scale
					float targetScale = getScale() * detector.getScaleFactor();
					// Correct for the min scale
					targetScale = Math.min( maxZoom(), Math.max( targetScale, 1.0f) );

					// Zoom and invalidate the view
					zoomTo( targetScale, detector.getFocusX(), detector.getFocusY() );
					invalidate();

					return true;
				}catch(IllegalArgumentException e){
					e.printStackTrace();
				}
			}
			return false;
		}
	}

	// Handles taps and scrolls of the view
	private class MyGestureListener extends
	GestureDetector.SimpleOnGestureListener {
		
		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			if(mImageTouchedListener!=null){
				mImageTouchedListener.onImageTouched();
				return false;
			}
			
			return super.onSingleTapConfirmed(e);
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

			// Skip if there are multiple points of contact
			if ( (e1!=null&&e1.getPointerCount() > 1) || (e2!=null&&e2.getPointerCount() > 1) || (mScaleDetector!=null && mScaleDetector.isInProgress()) ) 
				return false;

			// Scroll the bitmap
			if ( getScale() > zoomDefault() ) {
				removeCallbacks(mFling);
				postTranslate(-distanceX, -distanceY);
				center(true, true, false);
			}

			// Default case
			return true;
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			// If the zoom is over 1x, reset to 1x
			if ( getScale() > zoomDefault() ){
				zoomTo(zoomDefault());
			}
			// If the zoom is default, zoom into 2x
			else 
				zoomTo(zoomDefault()*3, e.getX(), e.getY(),200);

			// Always true as double tap was performed
			return true;
		}

		@Override
		public boolean onFling( MotionEvent e1, MotionEvent e2, float velocityX, float velocityY )
		{
			if ( (e1!=null&&e1.getPointerCount() > 1) || (e2!=null&&e2.getPointerCount() > 1) ) return false;
			if ( mScaleDetector.isInProgress() ) return false;

			try{
				float diffX = e2.getX() - e1.getX();
				float diffY = e2.getY() - e1.getY();

				if ( Math.abs( velocityX ) > 800 || Math.abs( velocityY ) > 800 ) {
					scrollBy( diffX / 2, diffY / 2, 300 );
					invalidate();
				}
			}catch(NullPointerException  e){

			}

			return super.onFling( e1, e2, velocityX, velocityY );
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		// If the bitmap was set, check the scale and gesture detectors
		if(mBitmap!=null){

			// Check the scale detector
			mScaleDetector.onTouchEvent( event );

			// Check the gesture detector
			if(!mScaleDetector.isInProgress())
				mGestureDetector.onTouchEvent( event );
		}

		// Default case
		return true;
	}
}
