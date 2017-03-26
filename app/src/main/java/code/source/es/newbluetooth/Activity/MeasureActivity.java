package code.source.es.newbluetooth.Activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.AttrRes;
import android.support.annotation.IntegerRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
//import Jama

import Jama.Matrix;
import code.source.es.newbluetooth.R;
import code.source.es.newbluetooth.Service.ScanService;

/**
 * Created by fanwe on 2017/3/13.
 */

public class MeasureActivity extends AppCompatActivity{
    private static final int ENABLE_BLUETOOTH = 1;
    int RSSI_LIMIT = 15, BLE_CHOOSED_NUM = 5;

    Button rssiButton;
    TextView rssiText, distanceText, coordinateText;
    Map<String,ArrayList> m1 = new HashMap<String, ArrayList>();  //储存RSSI的map
    Map<String,Double> m2 = new HashMap<String, Double>();     //过滤后的RSSI的Map
    Map<String,Double[]> bleDevLoc = new HashMap<>(); //固定节点的位置Map
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        String dFinished = BluetoothAdapter.ACTION_DISCOVERY_FINISHED;

        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String remoteMAC;
            final Short rssi;
            Double n = 1.92;
            ArrayList<String> listMac = new ArrayList<>();
            if(remoteDevice != null){
                remoteMAC = remoteDevice.getAddress();
                    rssi = intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI);
                    if (!dFinished.equals(intent.getAction())) {
                        if (m1.containsKey(remoteMAC)) {
                            ArrayList list1 = m1.get(remoteMAC);
                            list1.add(0, rssi.toString());
                            m1.put(remoteMAC, list1);
                        } else {
                            ArrayList<String> list = new ArrayList<String>();
                            list.add(rssi.toString());   //如果这个MAC地址没有出现过，建立list存储历次rssi
                            m1.put(remoteMAC, list);
                        }
                        Log.d("MAC", remoteMAC);
                        m2.put(remoteMAC, NormalDistribution(m1.get(remoteMAC)));   //更新MAC地址对应信号强度的map
                        listMac = Sort(m2, BLE_CHOOSED_NUM);  //给m2中的值排序，输出排名前k的点
                        ArrayList<String> coordinate = LeastSquares(m2, bleDevLoc,listMac);

                    }
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mea);
        rssiButton = (Button)findViewById(R.id.rssiButton);
        rssiText = (TextView)findViewById(R.id.rssiText);
        distanceText = (TextView)findViewById(R.id.distanceText);
        coordinateText = (TextView)findViewById(R.id.coordinateText);
        initBluetooth();   //查看蓝牙是否打开，没有打开的话提醒用户打开
        Double[] location = {2.5,12.1};
        bleDevLoc.put("19:18:FC:00:82:90",location);
        Double[] location1 = {6.2,11.1};
        bleDevLoc.put("19:18:FC:00:82:98",location1);
        Double[] location2 = {2.3,8.1};
        bleDevLoc.put("19:18:FC:00:82:8F",location2);
        Double[] location3 = {7.4, 5.1};
        bleDevLoc.put("19:18:FC:00:82:86",location3);
        startDiscovery(); //开启扫描模式
    }

    private void initBluetooth(){
        if(!bluetoothAdapter.isEnabled()){
            //蓝牙未打开，提醒用户打开
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, ENABLE_BLUETOOTH);
        }
    }
    private void startDiscovery(){
        registerReceiver(mReceiver,new IntentFilter((BluetoothDevice.ACTION_FOUND)));
        if(bluetoothAdapter.isEnabled() && !bluetoothAdapter.isDiscovering())
            bluetoothAdapter.startDiscovery();
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
//        Log.d("open discovery", "success");
    }

    //  更新对应MAC地址对应的过滤后RSSI值的平均值。
    private Double NormalDistribution(ArrayList m1){
        ArrayList<String> value = new ArrayList<>();
        if(m1.size() > RSSI_LIMIT){             //截取长度合适RSSI字符串,长于15时截取前15个
            for (int i = 0; i <RSSI_LIMIT ; i++){
                value.add(m1.get(i).toString());
            }
        }else {
            for (int i = 0; i < m1.size(); i++) { //截取长度合适RSSI字符串，短于15时全部复制
                value.add(m1.get(i).toString());
            }
        }

        Double rssiValue = 0.0, staDev = 0.0, proLowLim , proHighLim ;  //rssiValue作为一个中间变量在多个计算过程中间使用
        Double avg = GetAvg(value);  //获取RSSI的平均值
        staDev = GetStaDev(value,avg);  //获取RSSI的方差
        proLowLim = 0.15 * staDev + avg;         //高概率区下界
        proHighLim = 3.09 * staDev + avg;        //高概率区上界

        Log.d("value", value.toString());
        Log.d("staDev", staDev.toString());

        for (int i = 0; i < value.size(); i++) {          //去掉value中的低概率RSSI
            rssiValue = Double.valueOf(value.get(i));
            if ((proHighLim < rssiValue) || (rssiValue < proLowLim)) {
                value.remove(i);                              //删除不在高概率区域内的数据
                i -= 1;
            }
        }
        if(value.size() != 0) {
            avg = GetAvg(value);               //重新获取RSSI的平均值
            staDev = GetStaDev(value, avg);   //重新获取RSSI的标准差
            Log.d("value", value.toString());
            Log.d("staDev", staDev.toString());
        }
        return avg;
    }


    //用来给ArrayLIst产生均值的函数
    private Double GetAvg(ArrayList list){
        Double sum = 0.0, avg = 0.0;
        for(int i=0; i< list.size(); i++){
            sum += Double.valueOf(list.get(i).toString());
        }
        avg = sum / list.size();
        return avg;
    }

    //用来给ArrayList产生标准差的函数
    private Double GetStaDev(ArrayList list, Double avg){
        Double stadardDev = 0.0;
        if (list.size() >1) {
            for (int i = 0; i < list.size(); i++) {
                stadardDev += Math.pow((Double.valueOf(list.get(i).toString()) - avg), 2);
            }
            stadardDev = Math.sqrt(stadardDev / (list.size() - 1));
//            Log.d("staDev",stadardDev.toString());
        }
        return stadardDev;
    }

    //用来给map中的值排序
    public ArrayList<String> Sort(Map<String, Double> m2, int BLE_CHOOSED_NUM){
        List<Map.Entry<String, Double>> infoIds =
                new ArrayList<Map.Entry<String, Double>>(m2.entrySet());
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < infoIds.size(); i++) {     //排序前
            String id = infoIds.get(i).toString();
            System.out.println(id);
        }
        Collections.sort(infoIds, new Comparator<Map.Entry<String, Double>>() {        //排序
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
        for (int i = 0; i < BLE_CHOOSED_NUM; i++) {        //排序完,取前K个
            String id = infoIds.get(i).toString();
            list.add(id.split("\\=")[0]);   //string.split后变为字符串数组。
            System.out.println(id.split("\\=")[0]);
        }
        Log.d("stop","stop");
        return list;     //应该是MAC地址的列表
    }

    private ArrayList<Double> LeastSquares(Map<String, Double> m2, Map<String, ArrayList>bleDevLoc){    //最小二乘法实现定位
        int k = 3;
        Double xCor, yCor;
        ArrayList<Double[]> loc = new ArrayList<>();
        double[][] calEvenLoc = new double[BLE_CHOOSED_NUM-k+1][3];
        ArrayList<String> listMac = Sort(m2, BLE_CHOOSED_NUM);  //给m2中的值排序，输出排名前4的点
        for(int i = 0; i < BLE_CHOOSED_NUM-k+1; i++){
            loc.add(LeastSquaresCal(m2, bleDevLoc, listMac, i, k));
        }
        for(int i = 0; i < BLE_CHOOSED_NUM-k+1; i++){
            xCor = loc.get(i)[0] / Math.abs(loc.get(i)[2]);   //加权之后的x
            yCor = loc.get(i)[1] / Math.abs(loc.get(i)[2]);   //加权之后的y
        }
    }

    private Double[] LeastSquaresCal(Map<String, Double> m2, Map<String,ArrayList>bleDevLoc, ArrayList<String> listMac,int i, int k){
        double[][] bleDevLocArray = new double[k][6];   //储存节点位置信息的二维数组，6列：x，y，到本节点的RSSI,x方，y方，RSSI方
        double[][] Aarray = new double[k-1][2];
        double[][] barray = new double[k-1][2];
        double avg = 0.0;
        ArrayList<Double> storeAndAvg = new ArrayList<>();     //开始储存RSSI，求出平均值后，储存x、y坐标、rssi平均值
        Double[] result = new Double[3];
        for(int j = 0 ; j < k ; j++){   //形成储存节点位置信息的二维数组
            bleDevLocArray[j][0] = Double.valueOf(bleDevLoc.get(listMac.get(j + i)).get(1).toString());  //x
            bleDevLocArray[j][1] = Double.valueOf(bleDevLoc.get(listMac.get(j + i)).get(2).toString());  //y
            bleDevLocArray[j][2] = rssiToDis(m2.get(listMac.get(j)));    //距离
            bleDevLocArray[j][3] = Math.pow(bleDevLocArray[j][0],2);     //x平方
            bleDevLocArray[j][4] = Math.pow(bleDevLocArray[j][1],2);     //y平方
            bleDevLocArray[j][5] = Math.pow(bleDevLocArray[j][2],2);     //距离平方
            storeAndAvg.add(Double.valueOf(m2.get(listMac.get(j)).toString()));
        }
        for(int j = i; j < i + k; j++){
            Aarray[j][0] = 2 * (bleDevLocArray[j][0] - bleDevLocArray[k][0]);
            Aarray[j][1] = 2 * (bleDevLocArray[j][1] - bleDevLocArray[k][1]);
            barray[j][0] = bleDevLocArray[j][3] + bleDevLocArray[j][4] - bleDevLocArray[j][5] - bleDevLocArray[k-1][3] - bleDevLocArray[k-1][4] - bleDevLocArray[k-1][5];
            barray[j][1] = 0;
        }
        Matrix A = new Matrix(Aarray);
        Matrix b = new Matrix(barray);
        Matrix resultMatrix = (A.transpose().times(A)).inverse().times(A.transpose()).times(b);
        avg = GetAvg(storeAndAvg);     //计算RSSI的平均值，作为加权的依据。
        result[0] = resultMatrix.get(0,0);   //x坐标
        result[1] = resultMatrix.get(1,0);   //y坐标
        result[2] = avg;  //rssi平均值
        return result;
    }

    private double rssiToDis(double rssi){     //rssi到距离转化的函数
        return Math.pow(10.0, (rssi +52.393) / (-19.2));
    }

    /*
    private Double triangle(Map<String,Double> m2, ArrayList<String> listMac){
        Double n;
        Map<String,ArrayList> mac_Location = new HashMap<>();
        for (int i = 0; i < 3; i++){
            mac_Location.put(listMac.get(i),bleDevLoc.get(listMac.get(i)));
        }
        for
        return n;
    }
    */
}
