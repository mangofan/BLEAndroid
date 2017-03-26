package code.source.es.newbluetooth.Activity;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import code.source.es.newbluetooth.R;
import code.source.es.newbluetooth.Service.ScanService;

import static code.source.es.newbluetooth.R.id.cancel_action;
import static code.source.es.newbluetooth.R.id.sensorButton;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    Button scanButton,lineTestButton,sensorButton,measureButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("BasicActivity", getClass().getSimpleName());
        setContentView(R.layout.activity_main);
        initView();
        startService(new Intent(this,ScanService.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this,ScanService.class));
    }

    private void initView(){
        scanButton=(Button) findViewById(R.id.scanButton);
        scanButton.setOnClickListener(this);
        lineTestButton=(Button) findViewById(R.id.lineButton);
        lineTestButton.setOnClickListener(this);
        sensorButton = (Button) findViewById(R.id.sensorButton);
        sensorButton.setOnClickListener(this);
        measureButton = (Button) findViewById(R.id.measureButton);
        measureButton.setOnClickListener(this);

    }

    public void onClick(View v) {
        Intent intent=null;
        switch (v.getId()){
            case R.id.scanButton:
                intent=new Intent(this,ScanActivity.class);
                break;
            case R.id.lineButton:
                intent=new Intent(this,LineTestActivity.class);
                break;
            case R.id.sensorButton:
                intent = new Intent(MainActivity.this, SensorActivity.class);
                break;
            case R.id.measureButton:
                intent = new Intent(this,MeasureActivity.class);
                break;
        }
        if(intent!=null)
            startActivity(intent);
    }

}
