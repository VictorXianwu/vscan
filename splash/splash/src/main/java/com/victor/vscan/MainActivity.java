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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import cn.hugo.android.scanner.CaptureActivity;

@SuppressLint("InlinedApi")
public class MainActivity extends Activity {
    String qrcode = ""; // 当前的二维码编号
    QrMessage qrMessage;
    URL url = null;
    private ListView lv;
    public ArrayList<String> Datalist = new ArrayList<String>();
    private ArrayList<String> Qrcodelist = new ArrayList<String>();
    ArrayAdapter<String> adapter;
    ProgressDialog pdialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);//防止屏幕旋转
        forceShowActionBarOverflowMenu();  //强制显示overflow条目
        ActionBar actionBar = getActionBar();
        Uri uri = getIntent().getData();
        if (uri != null) {
            this.qrcode = uri.getPath().substring(8);
            Log.e("MainActivity","qrcode"+uri.getPath().substring(8));
            new downloadAndStartQrcode(MainActivity.this).execute(this.qrcode);
        }else{
            Log.e("MainActivity", "no uri");
        }
        this.qrcode = ""; // 初始化二维码编号为空
        // 列表处理
        lv = (ListView) findViewById(R.id.lv);
        Qrcodelist = getListQrcode();
        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_activated_1, getListData());
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view,
                                    int i, long l) {
                new downloadAndStartQrcode(MainActivity.this).execute(Qrcodelist.get(i));
            }
        });
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView,
                                           View view, int i, long l) {
                deleteListItem(i);
                return false;
            }
        });
    }

    public void OnClick_Event(View view) {
        switch (view.getId()) {
            case R.id.bt1:
                Intent openCameraIntent = new Intent(MainActivity.this,
                        CaptureActivity.class);
                startActivityForResult(openCameraIntent, 1);// 打开zxing库的照相
                break;
            case R.id.button:
                Intent intent = new Intent(MainActivity.this, FreeActivity.class);
                startActivity(intent); //免费获取二维码
        }
    }

    @Override
    protected void onResume() {
        /*Uri uri = getIntent().getData();
        if (uri != null) {
            this.qrcode = uri.getQueryParameter("qrcode");
            new downloadAndStartQrcode(MainActivity.this).execute(this.qrcode);
        }else{
            Log.e("MainActivity", "no uri");
        }*/
        super.onResume();

        resetQrcodelist();
    }

    @Override
    protected void onDestroy() {
        // 处理弹出的pdialog
        if (pdialog != null) {
            pdialog.dismiss();
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) { // 扫描成功
            Bundle bundle = data.getExtras();
            String result = bundle.getString("result");
            if ((result != null)
                    && result.matches("http://vscan.sinaapp.com/qrcode/\\S+")) { // 处理正确的二维码
                this.qrcode = result.substring(32, result.length()); // 截取二维码编码
                new downloadAndStartQrcode(MainActivity.this).execute(this.qrcode);
            } else {
                Toast.makeText(getApplicationContext(), "二维码错误！",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "二维码扫描失败",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("NewApi")
    private class downloadAndStartQrcode extends
            AsyncTask<String, Void, String> {

        public downloadAndStartQrcode(Context context) {
            pdialog = new ProgressDialog(context, 0);
            pdialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pdialog.show();
        }

        @Override
        protected String doInBackground(String... strings) {
            String qrcode = strings[0];
            String status = "Nonecode";
            MainActivity.this.qrMessage = new QrMessage(qrcode);
            try {
                status = MainActivity.this.qrMessage.get();
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
                if (!MainActivity.this.qrMessage.getdb()){
                    MainActivity.this.qrMessage.putdb();
                }
                starthistoryActivity();
            } else if (s.equals("blank")) { // 新的二维码
                new AlertDialog.Builder(MainActivity.this)// 弹出确定的对话框
                        .setTitle("提醒")
                        .setMessage("这是一张新二维码，确定要添加信息吗？")
                        .setPositiveButton("确定",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            DialogInterface dialogInterface,
                                            int i) {
                                        startAddActivity();
                                    }
                                })
                        .setNegativeButton("取消",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            DialogInterface dialogInterface,
                                            int i) {
                                        Log.i("MainActivity",
                                                "取消新建二维码信息");
                                    }
                                }).show();
            } else if (s.equals("error")) { // 假的二维码
                Toast.makeText(getApplicationContext(), "二维码错误！",
                        Toast.LENGTH_SHORT).show();
            } else if (s.equals("Nonecode")) {
                Toast.makeText(getApplicationContext(), "下载错误，请稍后重试！",
                        Toast.LENGTH_SHORT).show();
            }

        }


        private void starthistoryActivity() {
            Intent intent = new Intent(MainActivity.this, historyActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable("qrMessage", (java.io.Serializable) MainActivity.this.qrMessage);
            intent.putExtras(bundle);
            startActivity(intent);
        }

        private void startAddActivity() {
            Intent intent = new Intent(MainActivity.this, AddActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable("qrMessage", (java.io.Serializable) MainActivity.this.qrMessage);
            intent.putExtras(bundle);
            startActivity(intent);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.getfreeqrcode) {
            Intent intent = new Intent(MainActivity.this, FreeActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    private void deleteListItem(final int t) {
        new AlertDialog.Builder(MainActivity.this)
                . // 弹出删除的对话框
                        setTitle("提醒")
                .setMessage("要删除这条信息吗？")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        new PublicData().deleteItemDb(Qrcodelist.get(t));
                        resetQrcodelist();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.i("MainActivity", "取消删除信息");
                    }
                }).show();
    }

    private ArrayList<String> getListData() { //获取每个二维码的标题。
        for (int i = 0; i < Qrcodelist.size(); i++) {
            String qrcode = Qrcodelist.get(i);
            QrMessage qrMessage = new QrMessage(qrcode);
            try {
                qrMessage.get();
                Datalist.add(qrMessage.getTitle());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return Datalist;
    }

    private ArrayList<String> getListQrcode() {  //本地全部的二维码列表
        ArrayList<String> codelist = new ArrayList<String>();
        final SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(
                getString(R.string.dbString), null);
        try {
            db.execSQL("CREATE TABLE if not exists qrcode ( Id INTEGER PRIMARY KEY AUTOINCREMENT, QrId TEXT NOT NULL, Data BLOB, CreateTime TEXT, MotifyTime TEXT);");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Cursor c = db.rawQuery("Select QrId from qrcode order by CreateTime DESC", null);
        while (c.moveToNext()) {
            String sQrId = c.getString(0);
            codelist.add(sQrId);
        }
        c.close();
        db.close();
        return codelist;
    }

    void resetQrcodelist() {// 列表adapter重置
        Qrcodelist = getListQrcode();
        Datalist.clear();
        getListData();
        adapter.notifyDataSetChanged();
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
}
