package com.victor.vscan;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.WriterException;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static android.text.TextUtils.isEmpty;


public class FreeActivity extends Activity {
    ImageView qrImage;
    Button btUse;
    String qrcode;
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_free);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);//防止屏幕旋转
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true); 

        new downFreeQrcode().execute();
        btUse = (Button) findViewById(R.id.btUse);
        btUse.setEnabled(false);
        btUse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QrMessage qrMessage = new QrMessage(FreeActivity.this.qrcode);
                Intent intent = new Intent(FreeActivity.this, AddActivity.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable("qrMessage", (java.io.Serializable) qrMessage);
                intent.putExtras(bundle);
                startActivity(intent);
                FreeActivity.this.finish();
            }
        });
    }

    public class downFreeQrcode extends AsyncTask<Void, Void, String>{
        String qrcode = "";
        JSONObject QrcodeJSON = null;
        @Override
        protected String doInBackground(Void... parangms) {
            String status = "";
            String freeqc = getFreeQcJson();
            if (freeqc != null) {
                try {
                    JSONTokener jsonParser = new JSONTokener (freeqc);
                    QrcodeJSON = (JSONObject) jsonParser.nextValue();
                    status = QrcodeJSON.getString("status");
                    if (status.equals("blank")) {
                        this.qrcode = QrcodeJSON.getString("code");
                        FreeActivity.this.qrcode = this.qrcode;
                    }
                    return status.toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }else{ return "error"; }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            qrImage = (ImageView) findViewById(R.id.qrImage);
            qrImage.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.sorry));
            if (s!=null){
                if(s.equals("used")){
                    Toast.makeText(FreeActivity.this, "您已经领取并使用过二维码了哦，半个小时能领取一张哦！", Toast.LENGTH_SHORT).show();
                }else if(s.equals("error")){
                    Toast.makeText(FreeActivity.this, "获取二维码失败", Toast.LENGTH_SHORT).show();
                }else if(s.equals("blank")){
                    String uri = "http://vscan.sinaapp.com/qrcode/" + this.qrcode;
                    Bitmap bitmap;
                    try {
                        bitmap = BitmapUtil.createQRCode(uri, 500);
                        if (bitmap != null) {
                            qrImage.setImageBitmap(bitmap);
                            btUse = (Button)findViewById(R.id.btUse);
                            btUse.setEnabled(true);
                        }
                    } catch (WriterException e) {
                        e.printStackTrace();
                        Toast.makeText(FreeActivity.this, "获取二维码失败", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_free, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if(id == android.R.id.home){
            FreeActivity.this.finish();
        }

        return super.onOptionsItemSelected(item);
    }


    public String getFreeQcJson(){
        String result = null;
        HttpPost httppost = new HttpPost("http://linxianwu.sinaapp.com/freeqc/");
        HttpClient httpClient = new DefaultHttpClient();
        List<NameValuePair> params1 = new ArrayList<NameValuePair>();
        params1.add(new BasicNameValuePair("Id", Base64.encodeToString(getDeviceId(FreeActivity.this).getBytes(),Base64.DEFAULT)));
        Log.e("FreeActivity","DI ："+ Base64.encodeToString(getDeviceId(FreeActivity.this).getBytes(),Base64.DEFAULT));
        try {
            httppost.setEntity(new UrlEncodedFormEntity(params1));
            HttpResponse httpResponse = httpClient.execute(httppost);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                result = EntityUtils.toString(httpResponse.getEntity());
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
    /**
     * deviceID的组成为：渠道标志+识别符来源标志+hash后的终端识别符
     *
     * 渠道标志为：
     * 1，andriod（a）
     *
     * 识别符来源标志：
     * 1， wifi mac地址（wifi）；
     * 2， IMEI（imei）；
     * 3， 序列号（sn）；
     * 4， id：随机码。若前面的都取不到时，则随机生成一个随机码，需要缓存。
     *
     * @param context
     * @return
     */
    public static String getDeviceId(Context context) {


        StringBuilder deviceId = new StringBuilder();
        // 渠道标志
        deviceId.append("a");

        try {

            //wifi mac地址
            WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wifi.getConnectionInfo();
            String wifiMac = info.getMacAddress();
            if(!isEmpty(wifiMac)){
                deviceId.append("wifi");
                deviceId.append(wifiMac);
                deviceId.replace(7, 8, "3");
                deviceId.replace(deviceId.length() - 3, deviceId.length() - 2, "3");
                deviceId.replace(deviceId.length() - 1, deviceId.length(), "0");
                return deviceId.toString();
            }

            //IMEI（imei）
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String imei = tm.getDeviceId();
            if(!isEmpty(imei)){
                deviceId.append("imei");
                deviceId.append(imei);
                deviceId.replace(7, 8, "3");
                deviceId.replace(deviceId.length() - 3, deviceId.length() - 2, "3");
                deviceId.replace(deviceId.length() - 1, deviceId.length(), "0");
                return deviceId.toString();
            }

            //序列号（sn）
            String sn = tm.getSimSerialNumber();
            if(!isEmpty(sn)){
                deviceId.append("sn");
                deviceId.append(sn);
                deviceId.replace(7, 8, "3");
                deviceId.replace(deviceId.length() - 3, deviceId.length() - 2, "3");
                deviceId.replace(deviceId.length() - 1, deviceId.length(), "0");
                return deviceId.toString();
            }

            //如果上面都没有， 则生成一个id：随机码
            String uuid = getUUID(context);
            if(!isEmpty(uuid)){
                deviceId.append("id");
                deviceId.append(uuid);
                deviceId.replace(7, 8, "3");
                deviceId.replace(deviceId.length() - 3, deviceId.length() - 2, "3");
                deviceId.replace(deviceId.length() - 1, deviceId.length(), "0");
                return deviceId.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            deviceId.append("id").append(getUUID(context));
            deviceId.replace(7, 8, "3");
            deviceId.replace(deviceId.length() - 3, deviceId.length() - 2, "3");
            deviceId.replace(deviceId.length() - 1, deviceId.length(), "0");
        }

        //防一般伪造

        return deviceId.toString();
    }

    /**
     * 得到全局唯一UUID
     */
    public static String getUUID(Context context){
        SharedPreferences mShare = context.getSharedPreferences("sysCacheMap",MODE_PRIVATE);
        String uuid = "";
        if(mShare != null){
            uuid = mShare.getString("uuid", "");
        }

        if(isEmpty(uuid)){
            uuid = UUID.randomUUID().toString();
            SharedPreferences.Editor editor = mShare.edit();
            editor.putString("sysCacheMap", uuid);
            editor.commit();
        }

        return uuid;
    }
}
