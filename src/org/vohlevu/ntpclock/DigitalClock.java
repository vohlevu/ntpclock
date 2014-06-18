package org.vohlevu.ntpclock;

import java.text.SimpleDateFormat;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class DigitalClock extends AbstractClock {

	private TextView tvHourMinute;
	private TextView tvSecond;
	private LinearLayout container;
	private SimpleDateFormat formatHourMinute;
	private SimpleDateFormat formatSecond;
	
	public DigitalClock(Context context) {
		super(context);
	}
	
	public DigitalClock(Context context, LinearLayout container) {
		super(context);
		this.container = container;

		formatHourMinute = new SimpleDateFormat("HH:mm");
		formatSecond = new SimpleDateFormat("ss");
		
		Typeface tf = Typeface.createFromAsset(context.getAssets(),
				"fonts/digital-7.ttf");
		LayoutParams llp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		tvHourMinute = new TextView(context);
		tvHourMinute.setTypeface(tf);
		tvHourMinute.setLayoutParams(llp);
		tvHourMinute.setTextColor(context.getResources().getColor(R.color.text));
		tvHourMinute.setTextSize(50);
		
		tvSecond = new TextView(context);
		tvSecond.setTypeface(tf);
		llp.setMargins(10, 0, 0, 0); // llp.setMargins(left, top, right, bottom);
		tvSecond.setLayoutParams(llp);
		tvSecond.setTextColor(context.getResources().getColor(R.color.text));
		tvSecond.setTextSize(30);

		this.container.removeAllViews();
		this.container.addView(tvHourMinute);
		this.container.addView(tvSecond);
	}

	@Override
	protected void updateTime(long time) {
		final String hourMinute = formatHourMinute.format(time);
		final String second = formatSecond.format(time);
		tvHourMinute.setText(hourMinute);
		tvSecond.setText(second);
	}

}
