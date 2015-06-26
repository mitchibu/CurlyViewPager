package jp.gr.java_conf.mitchibu.lib.curlyviewpager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Region;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class CurlyViewPager extends ViewPager {
	private static final int SHEER = 160;
	private static final int SHADOW = 10;
	private static final int SHADOW_DX = -5;
	private static final int SHADOW_DY = 5;

	private final Paint effectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Path differencePath = new Path();

	private Bitmap effectBitmap = null;
	private Canvas effectCanvas = null;
	private int offsetX = -1;
	private int offsetY;
	private float downX;
	private float downY;
	private float curX;
	private float curY;
	private boolean isSimple;

	public CurlyViewPager(Context context) {
		this(context, null);
	}

	public CurlyViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
		setSimpleCurl(false);
		effectPaint.setStyle(Paint.Style.FILL);
		effectPaint.setColor(Color.WHITE);
		effectPaint.setAlpha(SHEER);
		effectPaint.setShadowLayer(SHADOW, SHADOW_DX, SHADOW_DY, Color.argb(SHEER, 0, 0, 0));
	}

	public void setSimpleCurl(boolean isSimple) {
		this.isSimple = isSimple;
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		recycleEffectBitmap();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		recycleEffectBitmap();

		if(w == 0 || h == 0) return;

		effectBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		effectCanvas = new Canvas(effectBitmap);

		if(offsetX < 0) offsetX = getCurrentItem() * w;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		switch(ev.getActionMasked()) {
		case MotionEvent.ACTION_DOWN:
			downX = ev.getX();
			downY = ev.getY();
			break;
//		case MotionEvent.ACTION_MOVE:
//			curX = ev.getX();
//			curY = ev.getY();
//			offsetX = isToNext ? 0 : -effectBitmap.getWidth() * 2;
//			break;
		}
		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		switch(ev.getActionMasked()) {
//		case MotionEvent.ACTION_DOWN:
//			downX = ev.getX();
//			downY = ev.getY();
//			break;
		case MotionEvent.ACTION_MOVE:
			curX = ev.getX();
			curY = ev.getY();
			break;
		}
		return super.onTouchEvent(ev);
	}

	@Override
	protected void dispatchDraw(@NonNull Canvas canvas) {
		if(effectBitmap == null) return;
		effectCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
		super.dispatchDraw(canvas);
		canvas.save();
		canvas.translate(getScrollX(), offsetY);
		canvas.drawBitmap(effectBitmap, 0, 0, null);
		canvas.restore();
	}

	@Override
	protected boolean drawChild(@NonNull Canvas canvas, @NonNull View child, long drawingTime) {
		ViewGroup.LayoutParams lp = child.getLayoutParams();
		if(lp instanceof LayoutParams && ((LayoutParams)lp).isDecor) {
			return super.drawChild(canvas, child, drawingTime);
		}
		final int w = getWidth();
		final int x = getScrollX();
		final int a = (x + offsetX) % w;
		final int position = (child.getLeft() + offsetX) / w;
		final int cur = (x + offsetX) / w;

		if(a == 0) {
			if(cur == position) {
				canvas.save();
				canvas.translate(x, child.getTop());
				child.draw(canvas);
				canvas.restore();
			}
		} else {
			if(cur == position) {
				offsetY = child.getTop();
				if(isSimple) drawEffectBitmapSimple(child, a);
				else drawEffectBitmap(child, a);

//				child.bringToFront();
//
//				canvas.save();
//				canvas.translate(x, 0);
//				canvas.drawBitmap(getEffectBitmap(child, a), 0, 0, null);
//				canvas.restore();
			} else {
				if(cur + 1 == position) {
					canvas.save();
					canvas.translate(x, child.getTop());
					child.draw(canvas);
					canvas.restore();
				}
			}
		}
		return true;
	}

	private void recycleEffectBitmap() {
		if(effectBitmap == null) return;
		effectBitmap.recycle();
		effectBitmap = null;
		effectCanvas = null;
	}

	private void drawEffectBitmapSimple(@NonNull View v, int a) {
		final int w = effectBitmap.getWidth();
		final int h = effectBitmap.getHeight();

		effectCanvas.save();
		effectCanvas.clipRect(0, 0, w - a, h);
		v.draw(effectCanvas);
		effectCanvas.restore();

		effectCanvas.save();
		effectCanvas.clipRect(w - a, 0, w, h, Region.Op.DIFFERENCE);
		effectCanvas.translate(w - a - a, 0);
		effectCanvas.scale(-1, 1, w / 2, 0);
		v.draw(effectCanvas);
		effectCanvas.restore();

		effectCanvas.save();
		effectCanvas.drawRect(w - a - a, 0, w - a, h, effectPaint);
		effectCanvas.restore();
	}

	private void drawEffectBitmap(@NonNull View v, int a) {
		float rad = (float)Math.atan(Math.abs(downY - curY) / a);
		float deg = (float)Math.toDegrees(rad);
		final int w = effectBitmap.getWidth();
		final int h = effectBitmap.getHeight();

		int baseY;
		if(downX > curX) {
			if(downY > curY) deg = 360 - deg;
			baseY = downY < curY ? 0 : h;
		} else if(downX < curX) {
			if(downY < curY) deg = 360 - deg;
			baseY = downY < curY ? h : 0;
		} else {
			return;
		}
		if(downY == curY) {
			drawEffectBitmapSimple(v, a);
		} else {
			differencePath.reset();
			differencePath.moveTo(w, baseY);
			differencePath.lineTo(w - a, baseY);
			differencePath.lineTo(w, baseY + (float)(a / Math.tan(Math.toRadians(deg))));
			differencePath.close();

			effectCanvas.save();
			effectCanvas.clipPath(differencePath, Region.Op.DIFFERENCE);
			v.draw(effectCanvas);
			effectCanvas.scale(-1, 1, w - a, 0);
			effectCanvas.rotate(deg * 2, w - a, baseY);
			v.draw(effectCanvas);
			effectCanvas.drawPath(differencePath, effectPaint);
			effectCanvas.restore();
		}
	}
}
