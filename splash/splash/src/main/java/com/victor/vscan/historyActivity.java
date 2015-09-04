package com.victor.vscan;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;

import com.umeng.socialize.controller.UMServiceFactory;
import com.umeng.socialize.controller.UMSocialService;
import com.umeng.socialize.sso.UMSsoHandler;


public class historyActivity extends Activity implements View.OnClickListener {
    private TextView historyData, historyDate;
    private Button audioPlay;
    ProgressDialog pdialog;
    final String TAG = "historyActivity ---->  ";
    QrMessage qrMessage;
    final String mp3File = Environment.getExternalStorageDirectory() + "/temp.mp3";

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);//防止屏幕旋转
        forceShowActionBarOverflowMenu();  //强制显示overflow条目
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        historyData = (TextView) findViewById(R.id.historyData);
        historyDate = (TextView) findViewById(R.id.historyDate);
        audioPlay = (Button) findViewById(R.id.audioPlay);

        Bundle bundle = getIntent().getExtras();
        this.qrMessage = (QrMessage) bundle.getSerializable("qrMessage");
        if (this.qrMessage.audioString.equals("null")) {
            audioPlay.setVisibility(View.GONE);
        } else {
            try {
                this.qrMessage.createAudioFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        historyData.setText(this.qrMessage.content);
        historyDate.setText(this.qrMessage.createTime);
        historyData.setMovementMethod(ScrollingMovementMethod.getInstance());
        audioPlay.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        // 处理弹出的pdialog
        if (pdialog != null) {
            pdialog.dismiss();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        this.qrMessage.dropAudioFile();
        super.onDestroy();
    }

    private class downloadQrcode extends
            AsyncTask<String, Void, String> {

        public downloadQrcode(Context context) {
            pdialog = new ProgressDialog(context, 0);
            pdialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pdialog.show();
        }

        @Override
        protected String doInBackground(String... strings) {
            String qrcode = strings[0];
            String status = "Nonecode";
            historyActivity.this.qrMessage = new QrMessage(qrcode);
            try {
                status = historyActivity.this.qrMessage.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return status;
        }

        @Override
        protected void onPostExecute(String s) {
            if (pdialog != null) { // 下载结束后时pdialog消失
                pdialog.dismiss();
            }
            super.onPostExecute(s);

            if (s.equals("used")) { // 已经使用过的二维码
                try {
                    historyActivity.this.qrMessage.get();
                    if (!historyActivity.this.qrMessage.getdb()) {
                        historyActivity.this.qrMessage.post();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else if (s.equals("blank")) { // 新的二维码
                Toast.makeText(getApplicationContext(), "空的二维码！",
                        Toast.LENGTH_SHORT).show();
            } else if (s.equals("error")) { // 假的二维码
                Toast.makeText(getApplicationContext(), "二维码错误！",
                        Toast.LENGTH_SHORT).show();
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.deleteItem) {
            new PublicData().deleteItemDb(this.qrMessage.qrcode);
            historyActivity.this.finish();
        } else if (item.getItemId() == R.id.share) {
            new PublicData().share(historyActivity.this, this.qrMessage.qrcode);
        } else if (item.getItemId() == android.R.id.home) {
            historyActivity.this.finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /**使用SSO授权必须添加如下代码 */
        final UMSocialService mController = UMServiceFactory.getUMSocialService("com.umeng.share");
        UMSsoHandler ssoHandler = mController.getConfig().getSsoHandler(requestCode);
        if (ssoHandler != null) {
            ssoHandler.authorizeCallBack(requestCode, resultCode, data);
        }
    }

    private void forceShowActionBarOverflowMenu() {  //强制显示overflow按钮
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    MediaPlayer mediaPlayer = null;
    private Boolean isplaying = false;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.audioPlay:
                audioPlay = (Button) findViewById(R.id.audioPlay);
                if (isplaying) {
                    audioPlay.setText("播放");
                    isplaying = false;
                    mediaPlayer.stop();
                    mediaPlayer.release();
                } else {
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            audioPlay.setText("播放");
                            isplaying = false;
                            mediaPlayer.stop();
                            mediaPlayer.release();
                        }
                    });
                    try {
                        FileInputStream fis = new FileInputStream(new File(mp3File));
                        mediaPlayer.setDataSource(fis.getFD());
                        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mediaPlayer.prepareAsync();
                        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                // 装载完毕回调
                                mp.start();
                            }
                        });
                        audioPlay.setText("停止");
                        isplaying = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }
}
