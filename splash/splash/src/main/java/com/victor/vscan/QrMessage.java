package com.victor.vscan;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Victor on 2015/3/17.
 */
public class QrMessage implements Serializable {
    public String qrcode = "";   //二维码uuid
    public String content = "";  //文本内容字符串
    public String audioString = "null";  //录音base64字符串
    public String createTime = "";   //创建时间

    private String data;       //data字符串

    public QrMessage(String qrcode) {
        this.qrcode = qrcode;
    }

    public String get() throws Exception {
        String status = "Nonecode";
        if (getdb()) {
            return "used" ;
        } else {
            HttpGet getMethod = new HttpGet("http://linxianwu.sinaapp.com/qrcode/" + this.qrcode);
            HttpResponse response = new DefaultHttpClient().execute(getMethod); // 发起GET请求,下载二维码包含信息
            String serverData = EntityUtils.toString(response.getEntity()); // 获取返回数据
            JSONTokener jsonTokener = new JSONTokener(serverData);
            JSONObject QrcodeJSON = (JSONObject) jsonTokener.nextValue(); //解析Http源代码
            if (response.getStatusLine().getStatusCode() == 200) {  //从服务器获取信息成功
                status = QrcodeJSON.getString("status");
                if (status.equals("used")) { // 已经使用过的二维码
                    this.data = endecrypt.desDecrypt(QrcodeJSON.getString("data"));
                    String temp[] = new PublicData().getContent(this.data);
                    this.content = temp[0];
                    this.audioString = temp[1];
                    this.createTime = QrcodeJSON.getString("date");
                }
            }
        }
        return status;//返回服务器返回的状态
    }

    public Boolean post() throws Exception { //提交到server并插入本地数据库
        Date curDate = new Date(System.currentTimeMillis());
        this.createTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(curDate);
        this.data = new PublicData().getjsondata(this.content, this.audioString);

        String result = null;
        String status = "";
        HttpPost httppost = new HttpPost("http://linxianwu.sinaapp.com/qrcode/" + this.qrcode);
        HttpClient httpClient = new DefaultHttpClient();
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("date", this.createTime));
        params.add(new BasicNameValuePair("data", endecrypt.desEncrypt(this.data)));
        params.add(new BasicNameValuePair("secureid", getSecureid()));
        httppost.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
        HttpResponse httpResponse = httpClient.execute(httppost);
        if (httpResponse.getStatusLine().getStatusCode() == 200) {
            result = EntityUtils.toString(httpResponse.getEntity());
        }
        if (result != null) {   //HTTP返回值200,post正常
            try {
                JSONObject QrcodeJSON = (JSONObject) new JSONTokener(result).nextValue();
                status = QrcodeJSON.getString("status"); //解析server返回的数据
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (!status.equals("true")) {   //server返回数据显示为失败，post失败
                return false;
            } else {      //server返回数据显示为成功，post成功
                this.putdb();
                return true;
            }
        } else {        //HTTP返回值不是200，post失败
            return false;
        }
    }

    public String getTitle(){
        String sContent = this.content;
        sContent = sContent.replace("\n"," ");
        if (sContent.length() > 13) {
            sContent = sContent.substring(0, 13);
        }
        return createTime.substring(5, 10) +"   "+sContent;
    }

    public Boolean createAudioFile() throws Exception {
        final String mp3File = Environment.getExternalStorageDirectory() + "/temp.mp3";
        if (!this.audioString.equals("null")){
            endecrypt.decoderBase64File(this.audioString, mp3File);
            return true;
        }else{
            return false;
        }
    }

    public void dropAudioFile() {
        final String mp3File = Environment.getExternalStorageDirectory() + "/temp.mp3";
        File file = new File(mp3File);
        if (file.exists()) {
            file.delete();
        }
    }

    public void putdb() {
        final SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase("/data/data/com.victor.vscan/qr.db", null);
        db.execSQL("insert into qrcode (QrId,Data,CreateTime)values(?,?,?);",
                new Object[]{this.qrcode, this.data, this.createTime});
        db.close();
    }

    public Boolean getdb() {
        final SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(
                "/data/data/com.victor.vscan/qr.db", null);
        Cursor cursor = db.rawQuery("select * from qrcode where QrId = '"+ this.qrcode +"'",null);
        if (cursor.getCount() != 0) {   //本地数据库能查找到二维码信息
            if (cursor.moveToFirst()){
                this.data = cursor.getString(2);   //data
                this.createTime = cursor.getString(3);  //createtime
                String temp[] = new PublicData().getContent(this.data);
                this.content = temp[0];
                this.audioString = temp[1];
            }
        } else {
           return false;
        }
        cursor.close();
        db.close();
        return true;
    }

    private String getSecureid() { //计算post的secureid
        SimpleDateFormat formatter = new SimpleDateFormat("MMddHHmm");
        Date curDate = new Date(System.currentTimeMillis());
        int time = Integer.valueOf(formatter.format(curDate));
        int seed = 20121533;
        String ostr = String.valueOf(time ^ seed);
        return endecrypt.getMd5(ostr + this.qrcode);
    }

}