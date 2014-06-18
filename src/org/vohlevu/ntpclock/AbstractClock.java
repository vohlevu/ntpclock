package org.vohlevu.ntpclock;

import android.content.Context;
import android.view.View;

public abstract class AbstractClock extends View {

	public AbstractClock(Context context) {
		super(context);
    }
	
	abstract protected void updateTime(long time);
}
