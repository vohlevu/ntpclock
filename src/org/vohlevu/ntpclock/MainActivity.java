package org.vohlevu.ntpclock;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.graphics.Typeface;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.NtpUtils;
import org.apache.commons.net.ntp.NtpV3Packet;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;

public class MainActivity extends Activity {

	private final String TAG = "MainActivity";
	private static final NumberFormat numberFormat = new java.text.DecimalFormat(
			"0.00");
	private TextView tv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Typeface tf = Typeface.createFromAsset(getAssets(),
				"fonts/digital-7.ttf");
		tv = (TextView) findViewById(R.id.digitalclock);
		tv.setTypeface(tf);

		String ntpServer = "0.ubuntu.pool.ntp.org";
		new GetNTPClockTask().execute(ntpServer);
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
		int version = message.getVersion();
		int li = message.getLeapIndicator();
		Log.i(TAG, " leap=" + li + ", version=" + version + ", precision="
				+ message.getPrecision());

		Log.i(TAG, " mode: " + message.getModeName() + " (" + message.getMode()
				+ ")");
		int poll = message.getPoll();
		// poll value typically btwn MINPOLL (4) and MAXPOLL (14)
		Log.i(TAG, " poll: " + (poll <= 0 ? 1 : (int) Math.pow(2, poll))
				+ " seconds" + " (2 ** " + poll + ")");
		double disp = message.getRootDispersionInMillisDouble();
		Log.i(TAG,
				" rootdelay="
						+ numberFormat.format(message
								.getRootDelayInMillisDouble())
						+ ", rootdispersion(ms): " + numberFormat.format(disp));

		int refId = message.getReferenceId();
		String refAddr = NtpUtils.getHostAddress(refId);
		String refName = null;
		if (refId != 0) {
			if (refAddr.equals("127.127.1.0")) {
				refName = "LOCAL"; // This is the ref address for the Local
									// Clock
			} else if (stratum >= 2) {
				// If reference id has 127.127 prefix then it uses its own
				// reference clock
				// defined in the form 127.127.clock-type.unit-num (e.g.
				// 127.127.8.0 mode 5
				// for GENERIC DCF77 AM; see refclock.htm from the NTP software
				// distribution.
				if (!refAddr.startsWith("127.127")) {
					try {
						InetAddress addr = InetAddress.getByName(refAddr);
						String name = addr.getHostName();
						if (name != null && !name.equals(refAddr)) {
							refName = name;
						}
					} catch (UnknownHostException e) {
						// some stratum-2 servers sync to ref clock device but
						// fudge stratum level higher... (e.g. 2)
						// ref not valid host maybe it's a reference clock name?
						// otherwise just show the ref IP address.
						refName = NtpUtils.getReferenceClock(message);
					}
				}
			} else if (version >= 3 && (stratum == 0 || stratum == 1)) {
				refName = NtpUtils.getReferenceClock(message);
				// refname usually have at least 3 characters (e.g. GPS, WWV,
				// LCL, etc.)
			}
			// otherwise give up on naming the beast...
		}
		if (refName != null && refName.length() > 1) {
			refAddr += " (" + refName + ")";
		}
		Log.i(TAG, " Reference Identifier:\t" + refAddr);

		TimeStamp refNtpTime = message.getReferenceTimeStamp();
		Log.i(TAG,
				" Reference Timestamp:\t" + refNtpTime + "  "
						+ refNtpTime.toDateString());

		// Originate Time is time request sent by client (t1)
		TimeStamp origNtpTime = message.getOriginateTimeStamp();
		Log.i(TAG,
				" Originate Timestamp:\t" + origNtpTime + "  "
						+ origNtpTime.toDateString());

		long destTime = info.getReturnTime();
		// Receive Time is time request received by server (t2)
		TimeStamp rcvNtpTime = message.getReceiveTimeStamp();
		Log.i(TAG,
				" Receive Timestamp:\t" + rcvNtpTime + "  "
						+ rcvNtpTime.toDateString());

		// Transmit time is time reply sent by server (t3)
		TimeStamp xmitNtpTime = message.getTransmitTimeStamp();
		Log.i(TAG,
				" Transmit Timestamp:\t" + xmitNtpTime + "  "
						+ xmitNtpTime.toDateString());

		// Destination time is time reply received by client (t4)
		TimeStamp destNtpTime = TimeStamp.getNtpTime(destTime);
		Log.i(TAG, " Destination Timestamp:\t" + destNtpTime + "  "
				+ destNtpTime.toDateString());

		info.computeDetails(); // compute offset/delay if not already done
		Long offsetValue = info.getOffset();
		Long delayValue = info.getDelay();
		String delay = (delayValue == null) ? "N/A" : delayValue.toString();
		String offset = (offsetValue == null) ? "N/A" : offsetValue.toString();

		Log.i(TAG, " Roundtrip delay(ms)=" + delay + ", clock offset(ms)="
				+ offset); // offset in ms
		return destNtpTime;
	}

	class GetNTPClockTask extends AsyncTask<String, Void, TimeStamp> {

		protected TimeStamp doInBackground(String... urls) {
			// Execurte the network related option here
			return getNtpClock(urls[0]);
		}

		protected void onPostExecute(TimeStamp destNtpTime) {
			// TODO: do something with the feed
			Log.i(TAG, "onPostExecute >> Destination Timestamp:\t"
					+ destNtpTime + "  " + destNtpTime.toDateString());
			String date = new SimpleDateFormat("HH:mm").format(destNtpTime.getTime());
			tv.setText(date);
		}
	}

}
