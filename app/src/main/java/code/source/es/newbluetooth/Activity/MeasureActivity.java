package code.source.es.newbluetooth.Activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Jama.Matrix;
import code.source.es.newbluetooth.R;
import code.source.es.newbluetooth.utils.FileCache;

/**
 * Created by fanwe on 2017/3/13.
 */

public class MeasureActivity extends AppCompatActivity{
    private static final int ENABLE_BLUETOOTH = 1;
    int RSSI_LIMIT = 15, BLE_CHOOSED_NUM = 5;
    Double FLOOR_HEIGHT = 3.3, DEVICE_HEIGHT = 0.75;;

    Button rssiButton;
    TextView rssiText, rssiText1, coordinateText;
    Map<String,ArrayList<String>> m1 = new HashMap<>();  //储存RSSI的map
    Map<String,Double> m2 = new HashMap<>();     //过滤后的RSSI的Map
    Map<String,Double[]> bleDevLoc = new HashMap<>(); //固定节点的位置Map
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        String dFinished = BluetoothAdapter.ACTION_DISCOVERY_FINISHED;

        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String remoteMAC;
            final Short rssi;
            if(remoteDevice != null){
                remoteMAC = remoteDevice.getAddress();
                if (bleDevLoc.containsKey(remoteMAC)) {
                    rssi = intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI);
                    if (!dFinished.equals(intent.getAction())) {
                        if (m1.containsKey(remoteMAC)) {
                            ArrayList<String> list1 = m1.get(remoteMAC);
                            list1.add(0, rssi.toString());
                            m1.put(remoteMAC, list1);
                        } else {
                            ArrayList<String> list = new ArrayList<>();
                            list.add(rssi.toString());   //如果这个MAC地址没有出现过，建立list存储历次rssi
                            m1.put(remoteMAC, list);
                        }
                        Log.d("MAC", remoteMAC);
                        m2.put(remoteMAC, NormalDistribution(m1.get(remoteMAC)));   //更新MAC地址对应信号强度的map
//                        if (m2.size() > 4) {
//                            ArrayList<Double> coordinate = LeastSquares(m2, bleDevLoc);
//                            Log.d("coordinate", coordinate.toString());
//                        }
                    }
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            FileCache cache = new FileCache();
                            cache.saveFile((m1.toString() + "\n"));
                        }
                    });
                    thread.start();
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
        rssiText1 = (TextView)findViewById(R.id.rssiText1);
        coordinateText = (TextView)findViewById(R.id.coordinateText);
        initBluetooth();   //查看蓝牙是否打开，没有打开的话提醒用户打开

        Double[] location21 = {3.9,9.0};
        Double[] location22 = {8.2,9.0};
        Double[] location23 = {0.2, 5.0};
        Double[] location24 = {4.2, 5.0};
        Double[] location25 = {8.2, 5.0};
        Double[] location26 = {12.2, 5.0};
        Double[] location27 = {0.2, 1.0};
        Double[] location28 = {4.2, 1.0};
        Double[] location29 = {8.2, 1.0};
        Double[] location30 = {12.2, 1.0};
        bleDevLoc.put("19:18:FC:01:F1:0E",location21);
        bleDevLoc.put("19:18:FC:01:F1:0F",location22);
        bleDevLoc.put("19:18:FC:01:F0:F8",location23);
        bleDevLoc.put("19:18:FC:01:F0:F9",location24);
        bleDevLoc.put("19:18:FC:01:F0:FA",location25);
        bleDevLoc.put("19:18:FC:01:F0:FB",location26);
        bleDevLoc.put("19:18:FC:01:F0:FC",location27);
        bleDevLoc.put("19:18:FC:01:F0:FD",location28);
        bleDevLoc.put("19:18:FC:01:F0:FE",location29);
        bleDevLoc.put("19:18:FC:01:F0:FF",location30);

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
    }

    //  对应MAC地址对应的过滤后RSSI的平均值，过滤使用高斯分布
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

        Double rssiValue, staDev, proLowLim , proHighLim ;  //rssiValue作为一个中间变量在多个计算过程中间使用
        Double avg = GetAvg(value);  //获取RSSI的平均值
        staDev = GetStaDev(value,avg);  //获取RSSI的方差
        proLowLim = 0.15 * staDev + avg;         //高概率区下界
        proHighLim = 3.09 * staDev + avg;        //高概率区上界
//        Log.d("value", value.toString());
//        Log.d("staDev", staDev.toString());

        for (int i = 0; i < value.size(); i++) {          //去掉value中的低概率RSSI
            rssiValue = Double.valueOf(value.get(i));
            if ((proHighLim < rssiValue) || (rssiValue < proLowLim)) {
                value.remove(i);                              //删除不在高概率区域内的数据
                i -= 1;
            }
        }
        if(value.size() != 0) {
            avg = GetAvg(value);               //重新获取RSSI的平均值
//            staDev = GetStaDev(value, avg);   //重新获取RSSI的标准差
//            Log.d("value", value.toString());
//            Log.d("staDev", staDev.toString());
        }
        return avg;
    }


    //用来给ArrayLIst产生均值的函数
    private Double GetAvg(ArrayList list){
        Double sum = 0.0, avg;
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
                new ArrayList<>(m2.entrySet());
        ArrayList<String> list = new ArrayList<>();
        int limit = BLE_CHOOSED_NUM < m2.size() ? BLE_CHOOSED_NUM:m2.size();
        for (int i = 0; i < infoIds.size(); i++) {     //排序前
            String id = infoIds.get(i).toString();
            System.out.println(id);
        }
        Collections.sort(infoIds, new Comparator<Map.Entry<String, Double>>() {        //排序
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return  o2.getValue().compareTo(o1.getValue());
//                return o1.getValue() > o2.getValue() ? -1:1;
            }
        });
        for (int i = 0; i < limit; i++) {        //排序完,取前limit个
            String id = infoIds.get(i).toString();
            list.add(id.split("=")[0]);   //string.split后变为字符串数组。
            System.out.println(id);
        }
        return list;     //排序好的MAC地址的列表
    }

    private ArrayList<Double> LeastSquares(Map<String, Double> m2, Map<String, Double[]>bleDevLoc){    //最小二乘法实现定位
        int k = 3;
        Double xCor = 0.0, yCor = 0.0, weightSum = 0.0;
        ArrayList<Double[]> loc = new ArrayList<>();
        ArrayList<Double> evenLoc = new ArrayList<>();
        ArrayList<String> listMac = Sort(m2, BLE_CHOOSED_NUM);  //给m2中的值排序，输出排名前BLE_CHOOSED_NUM的点
        for(int i = 0; i < BLE_CHOOSED_NUM-k+1; i++){
            loc.add(LeastSquaresCal(m2, bleDevLoc, listMac, i, k));   //得到每k个蓝牙节点计算出来的坐标和平均信号强度
            weightSum += loc.get(i)[2] + 100 ;
            Log.d("weightSum",weightSum.toString());
        }
        for(int i = 0; i < BLE_CHOOSED_NUM-k+1; i++){
            xCor += (loc.get(i)[0]) * (loc.get(i)[2] + 100) / weightSum;   //加权之后的x
            yCor += (loc.get(i)[1]) * (loc.get(i)[2] + 100) / weightSum;   //加权之后的y
        }
        evenLoc.add(xCor);
        evenLoc.add(yCor);
        return evenLoc;
    }

    //每三个点实现一次定位，返回计算出来的坐标值和平均信号强度（作为权值）
    private Double[] LeastSquaresCal(Map<String, Double> m2, Map<String,Double[]>bleDevLoc, ArrayList<String> listMac,int i, int k){
        double[][] bleDevLocArray = new double[k][6];   //储存节点位置信息的二维数组，6列：x，y，到本节点的RSSI,x方，y方，RSSI方
        double[][] Aarray = new double[k-1][2];
        double[][] barray = new double[k-1][2];
        ArrayList<Double> storeAndAvg = new ArrayList<>();     //开始储存RSSI，求出平均值后，储存x、y坐标、rssi平均值
        Double[] result = new Double[3];
        for(int j = 0 ; j < k ; j++){   //形成储存节点位置信息的二维数组
            bleDevLocArray[j][0] = Double.valueOf(bleDevLoc.get(listMac.get(j + i))[0].toString());  //x
            bleDevLocArray[j][1] = Double.valueOf(bleDevLoc.get(listMac.get(j + i))[1].toString());  //y
            bleDevLocArray[j][2] = rssiToDis(m2.get(listMac.get(j + i)));    //距离  m2<MAC地址，信号强度>
            bleDevLocArray[j][3] = Math.pow(bleDevLocArray[j][0],2);     //x平方
            bleDevLocArray[j][4] = Math.pow(bleDevLocArray[j][1],2);     //y平方
            bleDevLocArray[j][5] = Math.pow(bleDevLocArray[j][2],2);     //距离平方
            storeAndAvg.add(Double.valueOf(m2.get(listMac.get(j + i)).toString()));
        }
        for(int j = 0; j < k - 1; j++){
            Aarray[j][0] = 2 * (bleDevLocArray[j][0] - bleDevLocArray[k-1][0]);
            Aarray[j][1] = 2 * (bleDevLocArray[j][1] - bleDevLocArray[k-1][1]);
            barray[j][0] = bleDevLocArray[j][3] + bleDevLocArray[j][4] - bleDevLocArray[j][5] - bleDevLocArray[k-1][3] - bleDevLocArray[k-1][4] - bleDevLocArray[k-1][5];
            barray[j][1] = 0;
        }
        Matrix A = new Matrix(Aarray);
        Matrix b = new Matrix(barray);
//        Log.d("A", A.getColumnPackedCopy().toString());
        if(A.det() == 0){
            Log.d("Matrix","singular Matrix");
            result[0] = 0.0;
            result[1] = 0.0;
            result[2] = 0.0;
            return result;
        }else {
            Matrix A_inverse = A.inverse();
            Matrix resultMatrix1 = A.transpose().times(A);
            Matrix resultMatrix2 = resultMatrix1.inverse();
            Matrix resultMatrix3 = resultMatrix2.times(A_inverse);
            Matrix resultMatrix = (A.transpose().times(A)).inverse().times(A.transpose()).times(b);
            result[0] = resultMatrix.get(0, 0);   //x坐标
            result[1] = resultMatrix.get(1, 0);   //y坐标
            result[2] = GetAvg(storeAndAvg);  //计算RSSI的平均值，作为加权的依据。
            return result;
        }
    }

    private double rssiToDis(double rssi){     //rssi到距离转化的函数
        Double directDistance = Math.pow(10.0, (rssi +52.393) / (-19.2));
        directDistance = Math.sqrt(Math.pow(directDistance, 2) - Math.pow(FLOOR_HEIGHT - DEVICE_HEIGHT, 2));
        return directDistance;
    }

}
