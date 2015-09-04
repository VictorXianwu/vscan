package com.victor.vscan;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import com.google.zxing.WriterException;
import com.umeng.socialize.controller.UMServiceFactory;
import com.umeng.socialize.controller.UMSocialService;
import com.umeng.socialize.media.QQShareContent;
import com.umeng.socialize.media.QZoneShareContent;
import com.umeng.socialize.media.UMImage;
import com.umeng.socialize.sso.QZoneSsoHandler;
import com.umeng.socialize.sso.SinaSsoHandler;
import com.umeng.socialize.sso.TencentWBSsoHandler;
import com.umeng.socialize.sso.UMQQSsoHandler;
import com.umeng.socialize.weixin.controller.UMWXHandler;
import com.umeng.socialize.weixin.media.WeiXinShareContent;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static android.text.TextUtils.isEmpty;

public class PublicData {

	public void deleteItemDb(String qrcode) {
		final SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(
				"/data/data/com.victor.vscan/qr.db", null);
		db.execSQL("delete from qrcode where QrId = '"+ qrcode +"'");
		db.close();
	}

    public void share(Activity activity, String qrcode){
        final String url = "http://vscan.sinaapp.com/qrcode/" + qrcode;
        final UMSocialService mController = UMServiceFactory.getUMSocialService("com.umeng.share");
        // 设置分享内容
        String mShareContent = "扫一扫,看看这家伙说了什么！ 【微扫】";
        mController.setShareContent(mShareContent);
        UMImage mUMImgBitmap = new UMImage(activity, getStringQrcode(url));
        mController.setShareImage(mUMImgBitmap);
        mController.setAppWebSite(url);

        // 添加新浪和qq空间的SSO授权支持
        mController.getConfig().setSsoHandler(new SinaSsoHandler());
        // 添加腾讯微博SSO支持
        mController.getConfig().setSsoHandler(new TencentWBSsoHandler());

        // wx967daebe835fbeac是你在微信开发平台注册应用的AppID, 这里需要替换成你注册的AppID
        String appID = "wx141b74cfdfb8418a";
        String appSecret = "97b90b8b490275a044ba4313f602e70a";
        // 添加微信平台
        UMWXHandler wxHandler = new UMWXHandler(activity, appID);
        wxHandler.addToSocialSDK();
        // 支持微信朋友圈
        UMWXHandler wxCircleHandler = new UMWXHandler(activity, appID);
        wxCircleHandler.setToCircle(true);
        wxCircleHandler.addToSocialSDK();

        // 设置微信好友分享内容
        WeiXinShareContent weixinContent = new WeiXinShareContent();
        // 设置分享文字
        weixinContent.setShareContent(mShareContent);
        // 设置title
        weixinContent.setTitle("微扫");
        // 设置分享内容跳转URL
        weixinContent.setTargetUrl(url);
        // 设置分享图片
        weixinContent.setShareImage(mUMImgBitmap);
        mController.setShareMedia(weixinContent);


        // 参数1为当前Activity，参数2为开发者在QQ互联申请的APP ID，参数3为开发者在QQ互联申请的APP kEY.
        QZoneSsoHandler qZoneSsoHandler = new QZoneSsoHandler(activity, "1104203942", "2Ui0Y3dBN27GMWvy");
        UMQQSsoHandler qqSsoHandler = new UMQQSsoHandler(activity, "1104203942", "2Ui0Y3dBN27GMWvy");
        qZoneSsoHandler.addToSocialSDK();
        qqSsoHandler.addToSocialSDK();

        //分享到QQ
        QQShareContent qqShareContent = new QQShareContent();
        qqShareContent.setShareContent(mShareContent);
        qqShareContent.setTitle("微扫");
        qqShareContent.setShareImage(mUMImgBitmap);
        qqShareContent.setTargetUrl(url);
        mController.setShareMedia(qqShareContent);

        //分享到QQ空间
        QZoneShareContent qzone = new QZoneShareContent();
        // 设置分享文字
        qzone.setShareContent(mShareContent);
        // 设置点击消息的跳转URL
        qzone.setTargetUrl(url);
        // 设置分享内容的标题
        qzone.setTitle("微扫");
        // 设置分享图片
        qzone.setShareImage(mUMImgBitmap);
        mController.setShareMedia(qzone);
        mController.openShare(activity, false);
    }

    public Bitmap getStringQrcode(String url){
        Bitmap bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888);
        try {
            bitmap = BitmapUtil.createQRCode(url, 500);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public String getjsondata(String content,String audioString){
        JSONObject json=new JSONObject();
        try {
            json.put("content", content); //内容用json格式保存data
            json.put("audio", audioString); //audio
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    public String[] getContent(String data){
        String content = "";
        String audioString = "";
        try { //解析内容Json
            JSONTokener jsonParser = new JSONTokener(data);
            JSONObject dc = (JSONObject) jsonParser.nextValue();
            content = dc.getString("content");
            audioString = dc.getString("audio");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String[] a = new String[2];
        a[0] = content;
        a[1] = audioString;
        return a;
    }
}


