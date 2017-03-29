package code.source.es.newbluetooth.Activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import code.source.es.newbluetooth.R;
import code.source.es.newbluetooth.Service.ScanService;

public class TestRSSI extends AppCompatActivity {
    String name,MAC;
    List<Map<String,Object>> list=new ArrayList<>();
    SimpleAdapter simpleAdapter;
    Button save;
    EditText distance,or;
    TextView textViewNum;
    ListView listView;
    BroadcastReceiver broadcastReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(ScanService.GET_BLUETOOTH_RSSI)){
                if(intent.getExtras().getString("MAC").equals(MAC)) {
                    Map map=new HashMap();
                    Date date=new Date();
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                    // public final String format(Date date)
                    String s = sdf.format(date);
                    System.out.println(s);
                    map.put("time",s);
                    map.put("RSSI",String.valueOf(intent.getExtras().getInt("RSSI")));
                    list.add(map);
                    textViewNum.setText(String.valueOf(list.size()));
                    simpleAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_rssi);
        MAC=getIntent().getExtras().getString("intention");
        name=getIntent().getExtras().getString("name");
        initView();
        registerReceiver(broadcastReceiver,new IntentFilter(ScanService.GET_BLUETOOTH_RSSI));
    }
    private void initView(){
        ((TextView)findViewById(R.id.testName)).setText(name);
        ((TextView)findViewById(R.id.testMac)).setText(MAC);
        save=(Button)findViewById(R.id.ButtonSaveTest);
        distance=(EditText)findViewById(R.id.EditTestDistance);
        or=(EditText)findViewById(R.id.EditTestOr);
        listView=(ListView)findViewById(R.id.TestListView);
        textViewNum=(TextView)findViewById(R.id.testMNum);

        String []from=new String[]{"time","RSSI"};
        int []to=new int[]{R.id.TextTestTime,R.id.TextTestRSSI};
        simpleAdapter=new SimpleAdapter(this,list,R.layout.item_rssi_test,from,to);
        listView.setAdapter(simpleAdapter);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(distance.getText().toString().length()==0){
                    distance.setError("Not Null");
                    return;
                }
                if(or.getText().toString().length()==0){
                    or.setError("Not Null");
                    return;
                }
                new Thread(){
                    @Override
                    public void run() {
                        saveToFile();
                    }
                }.start();
                Toast.makeText(TestRSSI.this,"保存成功",Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void saveToFile(){
        final int REQUEST_EXTERNAL_STORAGE = 1;
         String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE };
        int permission = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
        File appDir = new File(Environment.getExternalStorageDirectory(), "ARSSI/TEST");
        if (!appDir.exists()) {
            appDir.mkdirs();
        }
        String fileName = name+"+"+distance.getText().toString()+"+"+or.getText()+"+"+(System.currentTimeMillis()%100)+ ".txt";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fileOutputStream=new FileOutputStream(file);
            BufferedWriter bufferedWriter=new BufferedWriter(new OutputStreamWriter(fileOutputStream));
            bufferedWriter.write("name: "+name+" MAC: "+MAC+"\n");
            for (Map<String,Object> map:list){
                String string=(String) map.get("time");
                string+="    "+(String)map.get("RSSI");
                string+="\n";
                bufferedWriter.write(string);
            }
            bufferedWriter.close();
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
