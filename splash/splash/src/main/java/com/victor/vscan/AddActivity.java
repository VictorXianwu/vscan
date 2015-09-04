package com.victor.vscan;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.pocketdigi.utils.FLameUtils;

@SuppressLint({"NewApi", "SimpleDateFormat"})
public class AddActivity extends Activity implements View.OnClickListener {
    private Button addBT, audioButton;
    private EditText addET;
    QrMessage qrMessage;
    ProgressDialog pdialog;
    int status = 1;
    AudioRecord mRecorder;
    private short[] mBuffer;
    final String tempFile = Environment.getExternalStorageDirectory() + "/temp.raw";
    final String mp3File = Environment.getExternalStorageDirectory() + "/temp.mp3";
    MediaPlayer mediaPlayer;
    String audioString = "null";  //录制之前audio默认值为"null"，确保上传成功

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);//防止屏幕旋转
        Bundle bundle = getIntent().getExtras();
        this.qrMessage = (QrMessage) bundle.getSerializable("qrMessage");
        addBT = (Button) findViewById(R.id.addBT);
        addET = (EditText) findViewById(R.id.addET);
        audioButton = (Button) findViewById(R.id.audio);
        addBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new upQrcode(AddActivity.this).execute();
            }
        });
        initRecorder();
        audioButton.setOnClickListener(this);
        audioButton.setOnLongClickListener(new ButtonOnlongClick());
    }

    private class ButtonOnlongClick implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View view) {
            if (view.getId() == R.id.audio) {
                new AlertDialog.Builder(AddActivity.this)
                        . // 弹出删除的对话框
                                setTitle("提醒")
                        .setMessage("新的录音？")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                status = 1;
                                audioButton = (Button) findViewById(R.id.audio);
                                audioButton.setText("录音");
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        }).show();

            }
            return true;
        }
    }

    @Override
    protected void onDestroy() {
        if (pdialog != null) {
            pdialog.dismiss();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        mRecorder.release();
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.audio:
                audioButton = (Button) findViewById(R.id.audio);
                switch (status) {
                    case 1: //录音
                        initRecorder();
                        status = 2;
                        audioButton.setText("停止");
                        mRecorder.startRecording();
                        startBufferedWrite(new File(tempFile));
                        break;
                    case 2: //停止
                        status = 3;
                        audioButton.setText("播放");
                        mRecorder.stop();
                        File file = new File(tempFile);
                        if (!file.exists()) {
                            return;
                        }
                        new Thread() {
                            public void run() {
                                FLameUtils lameUtils = new FLameUtils(1, 16000, 96);
                                System.out.println(lameUtils.raw2mp3(tempFile, mp3File));
                                try {
                                    audioString = new endecrypt().encodeBase64File(mp3File);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                File file = new File(tempFile);
                                if (file.exists()) {
                                    file.delete();
                                }
                            }
                        }.start();
                        break;
                    case 3://播放
                        status = 4;
                        mediaPlayer = new MediaPlayer();
                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                status = 3;
                                mediaPlayer.stop();
                                mediaPlayer.release();
                                audioButton.setText("播放");
                            }
                        });
                        try {
                            mediaPlayer.setDataSource(mp3File);
                            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mediaPlayer.prepareAsync();
                            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mp) {
                                    // 装载完毕回调
                                    mp.start();
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        audioButton.setText("停止");
                        break;
                    case 4://停止
                        status = 3;
                        mediaPlayer.stop();
                        mediaPlayer.release();
                        audioButton.setText("播放");
                }
                break;

        }
    }

    @SuppressLint("NewApi")
    public class upQrcode extends AsyncTask<String, Void, String> {

        public upQrcode(Context context) {
            pdialog = new ProgressDialog(context, 0);
            pdialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pdialog.show();
        }

        @Override
        protected String doInBackground(String... strings) {
            addET = (EditText) findViewById(R.id.addET);
            AddActivity.this.qrMessage.content = addET.getText().toString();
            AddActivity.this.qrMessage.audioString = audioString;
            Boolean status = false;
            try {
                status = AddActivity.this.qrMessage.post();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return status.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            if (pdialog != null) {
                pdialog.dismiss();
            }
            if (s.equals(String.valueOf(false))) {
                    Toast.makeText(AddActivity.this, "信息上传失败", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(AddActivity.this, historyActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("qrMessage", (java.io.Serializable) AddActivity.this.qrMessage);
                    intent.putExtras(bundle);
                    startActivity(intent);
                    AddActivity.this.finish();
                }
        }

    }

    /**
     * 初始化Recorder
     */
    public void initRecorder() {
        int bufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        mBuffer = new short[bufferSize];
        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 16000, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);
    }

    /**
     * 写入到文件
     *
     * @param file
     */
    private void startBufferedWrite(final File file) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                DataOutputStream output = null;
                try {
                    output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
                    while (status == 2) {
                        int readSize = mRecorder.read(mBuffer, 0, mBuffer.length);
                        for (int i = 0; i < readSize; i++) {
                            output.writeShort(mBuffer[i]);
                        }
                    }
                } catch (IOException e) {
                    Log.e("AddActivity", e.toString());
                } finally {
                    if (output != null) {
                        try {
                            output.flush();
                        } catch (IOException e) {
                            Log.e("AddActivity", e.toString());
                        } finally {
                            try {
                                output.close();
                            } catch (IOException e) {
                                Log.e("AddActivity", e.toString());
                            }
                        }
                    }
                }
            }
        }).start();
    }


}
