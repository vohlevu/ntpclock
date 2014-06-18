package org.vohlevu.ntpclock;

import java.util.Calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.widget.LinearLayout;

public class AnalogClock extends AbstractClock {

	private final float x = 180;
	private final float y = 200;
	private final int r = 120;
	private Calendar calendar;
	private LinearLayout container;
	private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

	public AnalogClock(Context context) {
		super(context);
	}

	public AnalogClock(Context context, LinearLayout container) {
		super(context);
		this.container = container;
		calendar = Calendar.getInstance();
		
		this.container.removeAllViews();
		this.container.addView(this);
	}

	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		// canvas.drawCircle(x, y, r, mPaint);
		float sec = (float) calendar.get(Calendar.SECOND);
		float min = (float) calendar.get(Calendar.MINUTE);
		float hour = (float) calendar.get(Calendar.HOUR) + min / 60.0f;
		mPaint.setColor(0xFFFF0000);
		canvas.drawLine(
				x,
				y,
				(float) (x + (r - 15)
						* Math.cos(Math
								.toRadians((hour / 12.0f * 360.0f) - 90f))),
				(float) (y + (r - 10)
						* Math.sin(Math
								.toRadians((hour / 12.0f * 360.0f) - 90f))),
				mPaint);
		canvas.save();
		mPaint.setColor(0xFF0000FF);
		canvas.drawLine(
				x,
				y,
				(float) (x + r
						* Math.cos(Math.toRadians((min / 60.0f * 360.0f) - 90f))),
				(float) (y + r
						* Math.sin(Math.toRadians((min / 60.0f * 360.0f) - 90f))),
				mPaint);
		canvas.save();
		mPaint.setColor(0xFFA2BC13);
		canvas.drawLine(
				x,
				y,
				(float) (x + (r + 10)
						* Math.cos(Math.toRadians((sec / 60.0f * 360.0f) - 90f))),
				(float) (y + (r + 15)
						* Math.sin(Math.toRadians((sec / 60.0f * 360.0f) - 90f))),
				mPaint);
	}

	@Override
	protected void updateTime(long time) {
		calendar.setTimeInMillis(time);
		invalidate();
	}
}
