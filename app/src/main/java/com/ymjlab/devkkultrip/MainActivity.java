package com.ymjlab.devkkultrip;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservice.ServerResponse;
import net.gotev.uploadservice.UploadInfo;
import net.gotev.uploadservice.UploadStatusDelegate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends BaseActivity {

	private String[] permissions = {Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}; //권한 설정 변수
	private List<String> permissionList = new ArrayList<>();

	private final static int REQUEST_TAKE_ALBUM = 9990;
	private final static int REQUEST_TAKE_CAMERA = 9991;
	private static final int MULTIPLE_PERMISSIONS = 101;

	private Uri saveUri;
	private JSONObject jsonOCR = new JSONObject();
	private JSONObject jsonOCR_2 = new JSONObject();


	ProgressDialog customProgressDialog;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		findViewById(R.id.camera).setEnabled(false);
		findViewById(R.id.camera).setAlpha(0.4f);
		findViewById(R.id.gallery).setEnabled(false);
		findViewById(R.id.gallery).setAlpha(0.4f);

		findViewById(R.id.camera).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
				File f = new File(getFilesDir(), timeStamp+".jpg");

				saveUri = FileProvider.getUriForFile(MainActivity.this, "com.ymjlab.devkkultrip", f);
				Intent i = new Intent();
				i.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
				i.putExtra(MediaStore.EXTRA_OUTPUT, saveUri);
				i.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				startActivityForResult(i, REQUEST_TAKE_CAMERA);
			}
		});

		findViewById(R.id.gallery).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_PICK);
				i.setType("image/*");
				startActivityForResult(i, REQUEST_TAKE_ALBUM);
			}
		});

		customProgressDialog = new ProgressDialog(this);
		customProgressDialog.setCancelable(false);

		customProgressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));


		findViewById(R.id.upload).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				customProgressDialog.show();
				API_sendImage(getApplicationContext().getCacheDir().getAbsolutePath()+"/"+saveUri.getLastPathSegment());
			}
		});

		findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				fadeShow(findViewById(R.id.step2), findViewById(R.id.step1));
			}
		});



		checkPermissions();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		MyUtil.log("onActivityResult " + requestCode + ":" + resultCode + ":" + data);

		if( resultCode == RESULT_OK ) {
			if (requestCode == REQUEST_TAKE_CAMERA) {
				startCrop(saveUri);
			} else if (requestCode == REQUEST_TAKE_ALBUM) {
				saveUri = data.getData();
				startCrop(saveUri);
			} else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
				CropImage.ActivityResult result = CropImage.getActivityResult(data);
				if (resultCode == RESULT_OK) {
					MyUtil.log(saveUri.getPath());
					deleteFile(saveUri);
					saveUri = result.getUri();
					MyUtil.log(saveUri.getPath());
					try {
						galleryAddPic();

						String path = getApplicationContext().getCacheDir().getAbsolutePath()+"/"+saveUri.getLastPathSegment();
						ImageUtils.getInstant().getCompressedBitmap(path);
						MyUtil.log(saveUri.toString() +":"+ path);
						((ImageView)findViewById(R.id.imageView)).setImageURI(saveUri);

					}catch(Exception e) {
						MyUtil.printStackTrace(e);
					}
				} else {
					Exception error = result.getError();
					showToast(error.toString());
				}
			}
		}
	}

	private void startCrop(Uri uri) {
		try {
			CropImage.ActivityBuilder ab =CropImage.activity(uri);
			ab.setAspectRatio(2, 3);
			ab.setGuidelines(CropImageView.Guidelines.ON)
				.setActivityTitle("사진편집")
				.setCropMenuCropButtonTitle("완료")
				.start(MainActivity.this);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void galleryAddPic() {
		String path = getApplicationContext().getFilesDir().getAbsolutePath()+"/"+saveUri.getLastPathSegment();
		Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		File f = new File(path);
		Uri contentUri = Uri.fromFile(f);
		mediaScanIntent.setData(contentUri);
		this.sendBroadcast(mediaScanIntent);
	}

	private void deleteFile(Uri saveUri) {
		String path = getApplicationContext().getFilesDir().getAbsolutePath()+"/"+saveUri.getLastPathSegment();
		File fdelete = new File(path);
		if (fdelete.exists()) {
			if (fdelete.delete()) {
				System.out.println("file Deleted :" + saveUri.getPath());
			} else {
				System.out.println("file not Deleted :" + saveUri.getPath());
			}
		} else
			MyUtil.log("not exists");
	}

	private void setEnableButton() {
		findViewById(R.id.camera).setEnabled(true);
		findViewById(R.id.camera).setAlpha(1);
		findViewById(R.id.gallery).setEnabled(true);
		findViewById(R.id.gallery).setAlpha(1);
	}

	public boolean checkPermissions() {
		int result;
		for (String pm : permissions) {
			result = ContextCompat.checkSelfPermission(this, pm);
			if (result != PackageManager.PERMISSION_GRANTED) { //사용자가 해당 권한을 가지고 있지 않을 경우 리스트에 해당 권한명 추가
				permissionList.add(pm);
				MyUtil.log("Add permission "+pm.toString());
			}
		}
		if (!permissionList.isEmpty()) { //권한이 추가되었으면 해당 리스트가 empty가 아니므로 request 즉 권한을 요청합니다.
			ActivityCompat.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]), MULTIPLE_PERMISSIONS);
			return false;
		}
		setEnableButton();
		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		MyUtil.log("onRequestPermissionsResult "+ requestCode);
		switch (requestCode) {
			case MULTIPLE_PERMISSIONS: {
				MyUtil.log("onRequestPermissionsResult "+ grantResults.length);
				if (grantResults.length > 0) {
					for (int i = 0; i < permissions.length; i++) {
						if (permissions[i].equals(this.permissions[0])) {
							MyUtil.log("equal 0 " + grantResults[i]);
							if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
								MyUtil.log("equal not 0");
								showNoPermissionToastAndFinish();
								return;
							}
						} else if (permissions[i].equals(this.permissions[1])) {
							MyUtil.log("equal 1");
							if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
								MyUtil.log("equal not 1");
								showNoPermissionToastAndFinish();
								return;
							}
						} else if (permissions[i].equals(this.permissions[2])) {
							MyUtil.log("equal 2");
							if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
								MyUtil.log("equal not 2");
								showNoPermissionToastAndFinish();
								return;
							}
						}
					}
				} else {
					showNoPermissionToastAndFinish();
					return;
				}

				setEnableButton();
				return;
			}
		}
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	private void showNoPermissionToastAndFinish() {
		customProgressDialog.dismiss();
		showToast("권한 요청에 동의 해주셔야 이용 가능합니다. 설정에서 권한 허용 하시기 바랍니다.");

		startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
			Uri.fromParts("package", getPackageName(), null)));
	}

	private Toast mToast;
	private void showToast(String msg) {
		if(mToast == null)
			mToast = Toast.makeText(getApplicationContext(), null, Toast.LENGTH_SHORT);
		mToast.setText(msg);
		mToast.show();
	}

	private void API_sendImage(String path) {
		try {
//			MyUtil.setSSL();
			MultipartUploadRequest request = new MultipartUploadRequest(this, "KKUL1", MyUtil.ADMIN_URL+"/api/ocrUpload");
			request.addFileToUpload(path, "file");
			request.setMaxRetries(2);
			request.setUtf8Charset();
			request.setDelegate(new UploadStatusDelegate() {
				@Override
				public void onProgress(UploadInfo uploadInfo) {
					MyUtil.log("onProgress " + uploadInfo.getProgressPercent());
				}
				@Override
				public void onError(UploadInfo uploadInfo, Exception exception) {
					MyUtil.log("onError " + exception);
					customProgressDialog.dismiss();
					showFailMsg("알림", "오류가 발생했습니다.", "서버와의 통신이 원활하지 않습니다.\n잠시 후 이용해 주세요");
				}
				@Override
				public void onCompleted(UploadInfo uploadInfo, ServerResponse serverResponse) {
					MyUtil.log("onCompleted " + serverResponse.getBodyAsString());
					try {
						JSONObject rlt = new JSONObject(serverResponse.getBodyAsString());
						if( rlt.getString("result").equals("Y")) {
							//"fileId":652,"filePath":"https://images.kkultrip.com/OCR/OCR_20220721_652.jpg"
							JSONObject ocrInfo = rlt.getJSONObject("ocrInfo");
							MyUtil.log(ocrInfo.toString());
							jsonOCR.put("fileId", ocrInfo.getString("fileId"));

//							API_requestOCR();
							API_requestOCR_VER_1();
							return;
						}
					}catch(Exception e) {
						MyUtil.printStackTrace(e);
					}
					customProgressDialog.dismiss();
					showFailMsg("오류", "데이터 전송에 실패했습니다.", "네트워크 상태를 확인 하시고 재시도 해주세요.");
				}
				@Override
				public void onCancelled(UploadInfo uploadInfo) {

				}
			});
			request.startUpload();
		}catch(Exception e) {
			e.printStackTrace();
			customProgressDialog.dismiss();
			showFailMsg("오류", "데이터 전송에 실패했습니다.", "네트워크 상태를 확인 하시고 재시도 해주세요.");
		} finally {
		}
	}

    /* NAVER OCR 이용 API */
	private void API_requestOCR_VER_1(){
		HashMap<String, String> map = new HashMap<>();
		map.put("fileId",getString(jsonOCR,"fileId"));
		JsoupTask task = new JsoupTask(getApplicationContext(), MyUtil.ADMIN_URL + "/api/getCombineOcrByFileId", Connection.Method.POST, new MyUtil.JsonListener(){
			@Override
			public void onResult(JSONObject json){
				try{
					if( json == null){
						customProgressDialog.dismiss();
						showToast("네트워크 환경을 확인해 주세요.");
					} else if (json.has("HTTP_CODE")){
						customProgressDialog.dismiss();
						showFailMsg("알림", "오류가 발생했습니다.","서버와의 통신이 원할하지 않습니다. \n 잠시후 이용해 주세요.");
					} else if (json.getString("result").equals("N")){
						customProgressDialog.dismiss();
						showFailMsg("알림", "오류가 발생했습니다.", "서버와의 통신이 원할하지 않습니다. \n 잠시후 이용해 주세요 ");
					} else if (json.getString("result").equals("Y")){

						try{
							naverOcrVer_1(json.getJSONObject("rciptOcrInfo"));
						} catch (Exception e){
							customProgressDialog.dismiss();
							MyUtil.printStackTrace(e);
							showFailMsg("새로운 OCR ", "영수증 인증 실패","코드 확인 바랍니다.");
						}
						try{
							naverRciptOcrVer_1(json.getJSONObject("ocrCombineInfo"));
						} catch (Exception e){
							customProgressDialog.dismiss();
							MyUtil.printStackTrace(e);
							showFailMsg("네이버 영수증 OCR ", "영수증 인증 실패","코드 확인 바랍니다.");
						}
					}
					return;
				} catch (Exception e){
					MyUtil.printStackTrace(e);
					customProgressDialog.dismiss();
					showFailMsg("새로운 OCR ", "영수증 인증 실패","코드 확인 바랍니다.");
				}
			}
		});
		task.addData(map);
		task.execute();
	}




	/*NAVER OCR 결과 ver_1*/
	private void naverOcrVer_1(JSONObject rciptOcrInfoObj) {
		MyUtil.log("고도화 OCR~~~~~~~~~~~");
		MyUtil.log(rciptOcrInfoObj.toString());

		boolean error = false;

		try {
			String store_nm = rciptOcrInfoObj.getString("ocrStoreNm");
			String ocr_pay_amt = rciptOcrInfoObj.getString("ocrPayAmt");
			String bizrno = rciptOcrInfoObj.getString("ocrBizrno");
			String ocrDatetime = rciptOcrInfoObj.getString("ocrPayDttm");
			String addr = rciptOcrInfoObj.getString("addr");
			String ocr_confm_num = rciptOcrInfoObj.getString("ocrConfmNum");

			jsonOCR.put("ocrStoreNm1", store_nm);
			jsonOCR.put("ocrBizrno1", bizrno);
			jsonOCR.put("ocrPayDttm1", ocrDatetime);
			jsonOCR.put("ocrPayAmt1", ocr_pay_amt);
			jsonOCR.put("ocrAddr1", addr);
			jsonOCR.put("ocrConfmNum1", ocr_confm_num);


			setText(findViewById(R.id.storeName3), store_nm);
			setText(findViewById(R.id.bizrno3), bizrno);
			setText(findViewById(R.id.confm_num3), ocr_confm_num);
			setText(findViewById(R.id.pay_date), ocrDatetime);
			setText(findViewById(R.id.hive_addr3), addr);
			setText(findViewById(R.id.amount3), ocr_pay_amt);



			MyUtil.log(jsonOCR.toString());

		} catch (Exception e){
			MyUtil.printStackTrace(e);
			error = true;
		} if( error ) {
			customProgressDialog.dismiss();
			showFailMsg("영수증 인증","영수증 인식에 실패하였습니다", "영수증 사진을 확인 바랍니다.22");
			return;
		}

		fadeShow(findViewById(R.id.step1), findViewById(R.id.step2));
		return;
	}
	private void naverRciptOcrVer_1(JSONObject obj){
		MyUtil.log("기존~~~~~~~~~~~");
		MyUtil.log(obj.toString());

		boolean error = false;
		try {
			String store_nm = obj.getString("ocrStoreNm");
			String ocr_pay_amt = obj.getString("ocrPayAmt");
			String bizrno = obj.getString("ocrBizrno");
			String ocrDatetime = obj.getString("ocrPayDttm");
			String addr = obj.getString("addr");
			String ocr_confm_num = obj.getString("ocrConfmNum");

			jsonOCR_2.put("naverOcrStoreNm1", store_nm);
			jsonOCR_2.put("naverOcrBizrno1", bizrno);
			jsonOCR_2.put("naverOcrPayDttm1", ocrDatetime);
			jsonOCR_2.put("naverOcrPayAmt1", ocr_pay_amt);
			jsonOCR_2.put("naverOcrAddr1", addr);
			jsonOCR_2.put("naverOcrConfmNum1", ocr_confm_num);


			setText(findViewById(R.id.ocr_store_name), store_nm);
			setText(findViewById(R.id.ocr_bizrno), bizrno);
			setText(findViewById(R.id.ocr_confm_num), ocr_confm_num);
			setText(findViewById(R.id.ocr_pay_date), ocrDatetime);
			setText(findViewById(R.id.ocr_hive_addr), addr);
			setText(findViewById(R.id.ocr_amount), ocr_pay_amt);


			TextView tvOcrStoreName = findViewById(R.id.ocr_store_name);
			TextView tvOcrBizrno = findViewById(R.id.ocr_bizrno);
			TextView tvOcrConfmNum = findViewById(R.id.ocr_confm_num);
			TextView tvOcrPayDate = findViewById(R.id.ocr_pay_date);
			TextView tvOcrHiveAddr = findViewById(R.id.ocr_hive_addr);
			TextView ocrAmount = findViewById(R.id.ocr_amount);


			if(!tvOcrStoreName.getText().toString().equals(jsonOCR.getString("ocrStoreNm1"))){
				tvOcrStoreName.setTextColor(Color.parseColor("#F51F01"));
			}
			if(!tvOcrBizrno.getText().toString().equals(jsonOCR.getString("ocrBizrno1"))){
				tvOcrStoreName.setTextColor(Color.parseColor("#F51F01"));
			}
			if(!tvOcrConfmNum.getText().toString().equals(jsonOCR.getString("ocrConfmNum1"))){
				tvOcrStoreName.setTextColor(Color.parseColor("#F51F01"));
			}
			if(!tvOcrPayDate.getText().toString().equals(jsonOCR.getString("ocrPayDttm1"))){
				tvOcrStoreName.setTextColor(Color.parseColor("#F51F01"));
			}
			if(!tvOcrHiveAddr.getText().toString().equals(jsonOCR.getString("ocrAddr1"))){
				tvOcrStoreName.setTextColor(Color.parseColor("#F51F01"));
			}
			if(!ocrAmount.getText().toString().equals(jsonOCR.getString("ocrPayAmt1"))){
				tvOcrStoreName.setTextColor(Color.parseColor("#F51F01"));
			}
			customProgressDialog.dismiss();
			MyUtil.log(jsonOCR_2.toString());

		} catch (Exception e){
			MyUtil.printStackTrace(e);
			error = true;
		} if( error ) {
			customProgressDialog.dismiss();
			showFailMsg("영수증 인증","영수증 인식에 실패하였습니다", "영수증 사진을 확인 바랍니다.22");
			return;
		}

		fadeShow(findViewById(R.id.step1), findViewById(R.id.step2));
		return;





	}




	/* NAVER 영수증 OCR 결과 */
	private void returnNaverOCR(JSONObject obj) {
		MyUtil.log("######################################이거 보자");

		MyUtil.log(obj.toString());

		String bizrno = "";
		boolean error = false;
		try {



			 JSONArray images = obj.getJSONArray("images");
			 JSONObject obj0 = images.getJSONObject(0);

			 if(obj0.getString("inferResult").equals("ERROR")){
				 showFailMsg("영수증 인증", "네이버 인증 실패", "InferResult is ERROR");
				 return;
			 }

			JSONObject result = obj.getJSONArray("images").getJSONObject(0).getJSONObject("receipt").getJSONObject("result");

			JSONObject storeInfo = result.getJSONObject("storeInfo");
			String store_nm = storeInfo.getJSONObject("name").getString("text");
			bizrno = storeInfo.getJSONObject("bizNum").getString("text");
			bizrno = bizrno.replaceAll("[^0-9]","");
			String addr = "";
			if( storeInfo.has("addresses"))
				addr = storeInfo.getJSONArray("addresses").getJSONObject(0).getString("text");
			String date = "";
			String time = "";
			String confirmNum = "";
			String cardInfo = "";
			String ocrDatetime = "";
			if( result.has("paymentInfo") ) {
				JSONObject paymentInfo = result.getJSONObject("paymentInfo");
				if (paymentInfo.has("date") ) {
					if( paymentInfo.getJSONObject("date").has("formatted")) {
						JSONObject dateFormat = paymentInfo.getJSONObject("date").getJSONObject("formatted");
						String year = getString(dateFormat, "year");
						if(year.length() != 2 && parseInt(year) >= 22  && parseInt(year) < 2000) {
							date = "20" +  getString(dateFormat, "year") +"-";
						} else
							date = getString(dateFormat, "year") +"-";
						date += getString(dateFormat, "month") +"-";
						date += getString(dateFormat, "day");
					} else {
						date = paymentInfo.getJSONObject("date").getString("text");
					}
					ocrDatetime = paymentInfo.getJSONObject("date").getString("text");
				}
				if (paymentInfo.has("time")) {
					if( paymentInfo.getJSONObject("time").has("formatted")) {
						JSONObject format = paymentInfo.getJSONObject("time").getJSONObject("formatted");
						time = format.getString("hour") + ":" + format.getString("minute") + ":" + format.getString("second");
					} else
						time = paymentInfo.getJSONObject("time").getString("text");
					ocrDatetime += " "+paymentInfo.getJSONObject("time").getString("text");
				}
				if( paymentInfo.has("cardInfo"))
					if( paymentInfo.getJSONObject("cardInfo").has("company") )
						cardInfo = paymentInfo.getJSONObject("cardInfo").getJSONObject("company").getString("text");

				if (paymentInfo.has("confirmNum")) {
					confirmNum = paymentInfo.getJSONObject("confirmNum").getString("text");
					confirmNum = confirmNum.replaceAll("[^0-9]","");
				}
			}

			String ocr_pay_amt = "0";
			if( result.has("totalPrice")) {
				JSONObject totalPrice = result.getJSONObject("totalPrice");
				if( totalPrice.has("price") && totalPrice.getJSONObject("price").has("formatted"))
					ocr_pay_amt = totalPrice.getJSONObject("price").getJSONObject("formatted").getString("value");
				else if( totalPrice.has("price") && totalPrice.getJSONObject("price").has("text") )
					ocr_pay_amt = totalPrice.getJSONObject("price").getString("text");
			}
			jsonOCR.put("ocrStoreNm", store_nm);
			jsonOCR.put("ocrBizrno", bizrno);
			jsonOCR.put("ocrPayDttm", ocrDatetime);
			jsonOCR.put("ocrPayDate", date);
			jsonOCR.put("ocrPayTime", time);
			jsonOCR.put("ocrConfmNum", confirmNum);
			jsonOCR.put("ocrPayAmt", ocr_pay_amt);
			jsonOCR.put("addr", addr);
			jsonOCR.put("cardNm", cardInfo);

			setText(findViewById(R.id.storeName3), store_nm);
			setText(findViewById(R.id.bizrno3), bizrno);
			setText(findViewById(R.id.confm_num3), confirmNum);
			setText(findViewById(R.id.pay_date), date);
//			setText(findViewById(R.id.pay_time), time);
			setText(findViewById(R.id.hive_addr3), addr);
			setText(findViewById(R.id.amount3), ocr_pay_amt);

			MyUtil.log(jsonOCR.toString());
		}catch(Exception e) {
			MyUtil.printStackTrace(e);
			error = true;
		}
		if( error ) {
			showFailMsg("영수증 인증","영수증 인식에 실패하였습니다", "깨끗한 배경에 영수증을 놓고 처음부터 끝까지 잘 나오도록\n촬영해주시길 바랍니다.");
			return;
		}
		fadeShow(findViewById(R.id.step1), findViewById(R.id.step2));

//		if( bizrno.length() == 10 ) {
//			API_checkAddress();
//		} else
//			checkError();



	}

	private void fadeShow(View gone, View visible) {
		visible.setVisibility(View.VISIBLE);
		visible.setAlpha(0f);
		visible.animate().alpha(1f).setDuration(600).setListener(null);
		gone.animate().alpha(0f).setDuration(600).setListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				gone.setVisibility(View.GONE);
			}
		});
	}

//	private void API_requestOCR() {
//		HashMap<String, String> map = new HashMap<>();
//		map.put("fileId", getString(jsonOCR, "fileId"));
//		JsoupTask task = new JsoupTask(getApplicationContext(), MyUtil.ADMIN_URL + "/api/ocrData", Connection.Method.POST,  new MyUtil.JsonListener() {
//			@Override
//			public void onResult(JSONObject json) {
//				try {
//					if( json == null ) {
//						showToast("네트워크 환경을 확인해 주세요 ");
//					} else if (json.has("HTTP_CODE")) {
//						showFailMsg("알림", "오류가 발생했습니다.", "서버와의 통신이 원활하지 않습니다.\n잠시 후 이용해 주세요");
//					} else  if( json.getString("result").equals("N")) {
//						showFailMsg("알림", "오류가 발생했습니다.", "서버와의 통신이 원활하지 않습니다.\n잠시 후 이용해 주세요");
//					} else  if( json.getString("result").equals("Y")) {
//						returnNaverOCR(new JSONObject(json.getString("ocrData")));
//
//						return;
//					}
//				}catch(Exception e) {
//					MyUtil.printStackTrace(e);
//					showFailMsg("영수증 인증","영수증 인식에 실패하였습니다", "깨끗한 배경에 영수증을 놓고 처음부터 끝까지 잘 나오도록\n촬영해주시길 바랍니다.");
//				}
//			}
//		});
//		task.addData(map);
//		task.execute();
//	}
}