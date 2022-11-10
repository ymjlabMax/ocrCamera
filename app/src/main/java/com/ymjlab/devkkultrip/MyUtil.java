package com.ymjlab.devkkultrip;

import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class MyUtil {

	final static boolean isDebug = true;
	public static final int DEF_TIMEOUT = 1000 * 10;
//	static public String SERVER_URL = "http://34.64.219.121";
//	static public String SERVER = SERVER_URL + ":9901/";
	static public String ADMIN_URL= "http://34.64.219.121:8090";
//	static public String ADMIN_URL= "http://12.23.34.254";

	public static void log(String s) {
		if (!isDebug) return;
		if (s.length() > 4000) {
			int maxLogSize = 1000;
			for (int i = 0; i <= s.length() / maxLogSize; i++) {
				int start = i * maxLogSize;
				int end = (i + 1) * maxLogSize;
				end = end > s.length() ? s.length() : end;
				if (i == 0)
					Log.i("TEST", "[KKUL] " + s.substring(start, end));
				else
					Log.i("TEST", " " + s.substring(start, end));
			}
		} else
			Log.i("TEST", "[KKUL] " + s);
	}

	public static void printStackTrace(Exception e) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(out);
		e.printStackTrace(printStream);
		log(out.toString());
	}

	public interface JsonListener {
		void onResult(JSONObject json);
	}

	public interface BoolListener {
		void onResult(boolean result);
	}

}
