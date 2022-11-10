package com.ymjlab.devkkultrip;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

public class BaseActivity extends AppCompatActivity {


	public String getString(JSONObject json, String key) {
		try {
			if (json == null || json.equals("null")) return "";
			String s = json.getString(key);
			if (s == null || s.equals("null")) return "";
			return json.getString(key);
		} catch (Exception e) {
			MyUtil.log(e.getLocalizedMessage() + "[" + key + "]");
			MyUtil.log(json.toString());
		}
		return "";
	}



	public void showFailMsg(String title, String ment, String msg) {
		View dialogView = LayoutInflater.from(this).inflate(R.layout.alert_warning, null);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(dialogView);
		final AlertDialog alertDialog = builder.create();
		alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

		setText(dialogView.findViewById(R.id.txtTitle), title);
		setText(dialogView.findViewById(R.id.ment), ment);
		setText(dialogView.findViewById(R.id.msg), msg);
		dialogView.findViewById(R.id.btnOK).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				alertDialog.dismiss();
			}
		});
		dialogView.findViewById(R.id.btnCancel).setVisibility(View.GONE);
		alertDialog.show();
	}

	public void setText(View v, String s) {
		((TextView)v).setText(s);
	}

	public int parseInt(String s) {
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			return 0;
		}
	}
}
