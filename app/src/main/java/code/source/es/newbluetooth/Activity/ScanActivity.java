package code.source.es.newbluetooth.Activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import code.source.es.newbluetooth.R;
import code.source.es.newbluetooth.Service.ScanService;

public class ScanActivity extends AppCompatActivity {

    SimpleAdapter simpleAdapter;
    ListView listView;
    String Intention;
    Messenger messenger;

    HashMap<String,Integer> count=new HashMap<>();

    List<Map<String,Object>> list=new ArrayList<>();

    ServiceConnection conn=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
           messenger= new Messenger(service);
            Message msg=new Message();
            msg.what=ScanService.START_BLUETOOTH;
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(ScanService.GET_BLUETOOTH_RSSI.equals(intent.getAction())){
                String name=intent.getExtras().getString("name");
                String MAC=intent.getExtras().getString("MAC");
                int RSSI=intent.getExtras().getInt("RSSI");
                //double distance=intent.getExtras().getDouble("distance");
                //setItem(name,MAC,RSSI,distance);
                setItem(name,MAC,RSSI);
            }
        }
    };
    final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("BasicActivity", getClass().getSimpleName());
        setContentView(R.layout.activity_sacn);

        IntentFilter filter=new IntentFilter(ScanService.GET_BLUETOOTH_RSSI);
        this.registerReceiver(mReceiver,filter);
        Button button=(Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message msg=new Message();
                msg.what=ScanService.START_BLUETOOTH;
                try {
                    messenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                Toast.makeText(v.getContext(),"开始搜索",Toast.LENGTH_SHORT).show();
            }
        });
        ((Button)findViewById(R.id.button_stop)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message msg=new Message();
                msg.what=ScanService.STOP_BLUETOOTH;
                try {
                    messenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                Toast.makeText(v.getContext(),"停止搜索",Toast.LENGTH_SHORT).show();
            }
        });
        ((Button)findViewById(R.id.initRSSIButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //double A1= Double.valueOf(((EditText)findViewById(R.id.RSSI1m)).getText().toString());
                //double A2= Double.valueOf(((EditText)findViewById(R.id.RSSI2m)).getText().toString());
                Toast.makeText(ScanActivity.this,"初始化N",Toast.LENGTH_SHORT).show();
                Message msg=new Message();
                Bundle bundle=new Bundle();
                //bundle.putDouble("A1",A1);
                //bundle.putDouble("A2",A2);
                msg.what=ScanService.SET_A_N;
                msg.setData(bundle);
                try {
                    messenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        simpleAdapter=new SimpleAdapter(this,this.list,R.layout.list_view,
                new String[]{"name","MAC","RSSI"},
                new int[]{R.id.name,R.id.MAC,R.id.RSSI});
        listView=(ListView)findViewById(R.id.listView);
        listView.setAdapter(simpleAdapter);
        if(adapter.isEnabled()){
            //BluetoothAdapter.ACTION_REQUEST_ENABLE 为启动蓝牙的action
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(intent);
        }
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intention=((TextView)view.findViewById(R.id.MAC)).getText().toString();
                Message msg=new Message();
                Bundle bundle=new Bundle();
                bundle.putString("intention",Intention);
                bundle.putString("name",((TextView)view.findViewById(R.id.name)).getText().toString());
                msg.what=ScanService.SET_INTENTION;
                msg.setData(bundle);
                try {
                    messenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                Intent intent=new Intent(ScanActivity.this ,TestRSSI.class);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

        bindService(new Intent(this, ScanService.class),conn,BIND_AUTO_CREATE);
    }
    private void setItem(String name,String MAC,int RSSI){
        //将参数放入成员变量list中（原方法目的是要计算均值）
        HashMap<String,Object> temp;
        Iterator<Map<String,Object>> iterator=list.iterator();
        while (iterator.hasNext()){
            temp=(HashMap<String, Object>) iterator.next();
            if(temp.get("MAC").equals(MAC)) {//如果list中取得的临时变量temp的MAC，是当前MAC要求的MAC
                temp.put("name",name);
                temp.put("MAC",MAC);
                temp.put("RSSI",RSSI);
                list.add(temp);
                simpleAdapter.notifyDataSetChanged();//可以待一定时间后再通知
                return;
            }
        }

        //count.put(MAC,Integer.valueOf(1));
        //simpleAdapter.notifyDataSetChanged();
    }



    public void Onclick(){
       /* Set<BluetoothDevice> devices = adapter.getBondedDevices();
        if(devices.size()>0){
            for(Iterator iterator = devices.iterator(); iterator.hasNext();){
                BluetoothDevice device = (BluetoothDevice) iterator.next();
               // System.out.println("已配对的设备："+device.getAddress());

            }
        }*/
        //Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        //设置蓝牙可见性的时间，方法本身规定最多可见300秒
        //intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        //startActivity(intent);
        if(adapter.isDiscovering())
            adapter.cancelDiscovery();
        else
            adapter.startDiscovery();

    }
}
