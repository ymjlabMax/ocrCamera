package com.ymjlab.devkkultrip;

import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.util.Map;

public class JsoupTask extends AsyncTask<String,Void, JSONObject> {
	private MyUtil.JsonListener jsonListener;
	private Context c;
	private Connection con;

	public JsoupTask(Context c, String url, Connection.Method method, MyUtil.JsonListener listener) {
		this.c = c;
		this.jsonListener = listener;

		con = Jsoup.connect(url).timeout(MyUtil.DEF_TIMEOUT)
			.ignoreContentType(true)
			.header("Accept-Encoding", "gzip, deflate")
			.header("accept", "application/json")
			.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.139 Safari/537.36")
			.maxBodySize(0)
			.method(method);
		MyUtil.log("URL(" + method.toString() + ")" +   url);
	}

	public void addHeader(String key, String value) {
		con.header(key, value);
	}
	public void addData(Map<String, String> data) {
		con.data(data);
	}
	public void addData(String key, String value) {
		con.data(key, value);
	}
	@Override
	protected JSONObject doInBackground(String... params) {
		int resCode = 0;
		try {
//			MyUtil.setSSL();
			Connection.Response res = con.execute();
			resCode = res.statusCode();

			JSONObject header = new JSONObject(res.headers());
			JSONObject result = new JSONObject(res.body());
			MyUtil.log("response (" + res.statusCode() + ")");
			MyUtil.log("res"+ res.statusCode()+  "(" + result.toString() + ")");
			return result;
		}catch(Exception e ) {
			MyUtil.printStackTrace(e);
			try {
				JSONObject result = new JSONObject();
				result.put("HTTP_CODE", resCode + "");
				return result;
			}catch(Exception e1) {
				MyUtil.printStackTrace(e1);
			}
		}
		return null;
	}

	@Override
	protected void onPostExecute(JSONObject result){
		if(result==null) {
		try {
			if(jsonListener!=null)
				jsonListener.onResult(null);
			return;
		} catch (Exception e) {}
			if(jsonListener!=null)
				jsonListener.onResult(null);
			return;
		} else if(jsonListener!=null)
			jsonListener.onResult(result);
	}

}

