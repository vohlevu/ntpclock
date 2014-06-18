package org.vohlevu.ntpclock;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.NtpV3Packet;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;

public class MainActivity extends Activity implements RadioGroup.OnCheckedChangeListener {

	private final String TAG = "MainActivity";
    private IntentFilter mNetworkStateChangedFilter;
    private BroadcastReceiver mNetworkStateIntentReceiver;
	private LinearLayout container;
	private TextView tvCountDownTimer;
	private final long startTime = 10 * 60 * 1000; // 10 minutes
	private final long interval = 1 * 1000;	// 1 sec
	private final String ntpServer = "0.ubuntu.pool.ntp.org";
	private SimpleDateFormat formatMinuteSecond;
	private ReSyncTime reSyncTime;
	private long currentTime;
	private Timer timer;
	private UpdateTime updateTimeTask;
	private AbstractClock mClock = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		currentTime = 0;
		formatMinuteSecond = new SimpleDateFormat("mm:ss");
		reSyncTime = new ReSyncTime(startTime, interval);
		
		mNetworkStateChangedFilter = new IntentFilter();
		mNetworkStateChangedFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

		mNetworkStateIntentReceiver = new BroadcastReceiver() {
		    @Override
		    public void onReceive(Context context, Intent intent) {
		    	boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
				if (noConnectivity) {
					makeToast(getString(R.string.no_connection));
					reSyncTime.cancel();
				} else {
					new GetNTPClockTask().execute(ntpServer);	//Sync time immediate when have connection
				}
		    }
		};

		tvCountDownTimer = (TextView) findViewById(R.id.count_down_timer);
		container = (LinearLayout) findViewById(R.id.clock);
		mClock = new DigitalClock(this, container);
				
		findViewById(R.id.btnSync).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new GetNTPClockTask().execute(ntpServer);
			}
		});
		((RadioGroup)findViewById(R.id.clockType)).setOnCheckedChangeListener(this);
		
		timer = new Timer();
		updateTimeTask = new UpdateTime();
		timer.schedule(updateTimeTask, 1000, interval);	// Delay 1 second to update time
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		switch (checkedId) {
		case R.id.radioDigital:
			mClock = new DigitalClock(this, container);
			break;
		case R.id.radioAnalog:
			mClock = new AnalogClock(this, container);
			break;
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public TimeStamp getNtpClock(String ntpServer) {
		TimeStamp destNtpTime = null;
		NTPUDPClient client = new NTPUDPClient();
		// We want to timeout if a response takes longer than 10 seconds
		client.setDefaultTimeout(10000);
		try {
			client.open();

			try {
				InetAddress hostAddr = InetAddress.getByName(ntpServer);
				Log.i(TAG,
						"> " + hostAddr.getHostName() + "/"
								+ hostAddr.getHostAddress());
				TimeInfo info = client.getTime(hostAddr);
				destNtpTime = processResponse(info);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}

		client.close();
		return destNtpTime;

	}

	/**
	 * Process <code>TimeInfo</code> object and print its details.
	 * 
	 * @param info
	 *            <code>TimeInfo</code> object.
	 */
	public TimeStamp processResponse(TimeInfo info) {
		NtpV3Packet message = info.getMessage();
		int stratum = message.getStratum();
		String refType;
		if (stratum <= 0) {
			refType = "(Unspecified or Unavailable)";
		} else if (stratum == 1) {
			refType = "(Primary Reference; e.g., GPS)"; // GPS, radio clock,
														// etc.
		} else {
			refType = "(Secondary Reference; e.g. via NTP or SNTP)";
		}
		// stratum should be 0..15...
		Log.i(TAG, " Stratum: " + stratum + " " + refType);
		long destTime = info.getReturnTime();
		// Destination time is time reply received by client (t4)
		TimeStamp destNtpTime = TimeStamp.getNtpTime(destTime);
		return destNtpTime;
	}

	class GetNTPClockTask extends AsyncTask<String, Void, TimeStamp> {

		protected TimeStamp doInBackground(String... urls) {
			// Execurte the network related option here
			return getNtpClock(urls[0]);
		}

		protected void onPostExecute(TimeStamp destNtpTime) {
			if (destNtpTime != null) {
				makeToast(getString(R.string.time_update_successful));
				Log.i(TAG, "onPostExecute >> Destination Timestamp:\t"
						+ destNtpTime + "  " + destNtpTime.toDateString());
				currentTime = destNtpTime.getTime();
				reSyncTime.cancel();
				reSyncTime.start();
			} else {
				makeToast(getString(R.string.time_update_fail));
			}
		}
	}

	public class UpdateTime extends TimerTask {
		@Override
		public void run() {
			//Log.i(TAG, "UpdateTime >> run : " + currentTime);
			if (currentTime != 0) {
				currentTime += 1000;	//increase 1 second
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (mClock != null)
							mClock.updateTime(currentTime);
					}
				});
			}
		}

	}
	
	public class ReSyncTime extends CountDownTimer {
		public ReSyncTime(long startTime, long interval) {
			super(startTime, interval);
		}

		@Override
		public void onFinish() {
			new GetNTPClockTask().execute(ntpServer);
		}

		@Override
		public void onTick(long millisUntilFinished) {
			String date = formatMinuteSecond.format(millisUntilFinished);
			tvCountDownTimer.setText(getString(R.string.count_down_text, date));
		}
	}

	public void makeToast(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();
		registerReceiver(mNetworkStateIntentReceiver,
				mNetworkStateChangedFilter);
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
		super.onPause();
		unregisterReceiver(mNetworkStateIntentReceiver);
	}
}
