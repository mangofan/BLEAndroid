package code.source.es.newbluetooth.Activity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.Timer;
import java.util.TimerTask;

import code.source.es.newbluetooth.R;

/**
 * Created by fanwe on 2016/12/27.
 */

public class AccActivity extends AppCompatActivity {

    private TextView accelerationTextView;
    private SensorManager sensorManager;

    Double Sx = 0.0, Sy = 0.0;
    Double V0x = 0.0, V0y = 0.0;
    Double ax = 0.0, ay = 0.0;
    Integer T0 = 200;
    double[] localAcc = new double[3];
    double[] localAccFilterd = new double[2];
    double deviation = 1.0;  //误差值，认为小于某个值时即可接受，大于时继续进行卡尔曼滤波
    double EstimateValue1=0.0, EstimateValue2=0.0;
    double EstimateCovariance1=0.1, EstimateCovariance2=0.1;
    double MeasureCovariance1=0.02, MeasureCovariance2=0.02;
    BigDecimal b = new BigDecimal(Double.toString(0.0));
    BigDecimal[] result = {b, b, b, b};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acc);
        accelerationTextView = (TextView) findViewById(R.id.text1);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);  //得到传感器管理器
        Sensor accelermeterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);//明确传感器
        Sensor rotvectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sensorManager.registerListener(listener, accelermeterSensor, SensorManager.SENSOR_DELAY_GAME); //将传感器注册到监听器中
        sensorManager.registerListener(listener, rotvectorSensor, SensorManager.SENSOR_DELAY_GAME);

        //设置每隔T0更新UI
        Timer updateTimer = new Timer("Update");
        updateTimer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                updateGUI();
            }
        },0,T0);
    }

    private SensorEventListener listener = new SensorEventListener() {
        float[] accValues = new float[3];
        float[] rotValues = new float[5];
        float[] R1 = new float[9];

        @Override
        public void onSensorChanged(SensorEvent event) {
            if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
                rotValues = event.values.clone();
            }else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                accValues = event.values.clone();
            }
            SensorManager.getRotationMatrixFromVector(R1, rotValues);
            localAcc = matrixLeftMult(R1, accValues);  //获得转换到当地地理坐标系的加速度矢量
            run1();
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
};

    //UI 更新方法
    private void updateGUI(){
        runOnUiThread(new Runnable() {
    @Override
        public void run() {
            String need = result[0].toString() + '\n' + result[1].toString() + '\n' + result[2].toString() + '\n' + result[3].toString();  //展示Sx，Sy，ax，ay
            accelerationTextView.setText(need);
            }
        });
    }

    public BigDecimal[] run1() {
        localAccFilterd = localAccFiltered(localAcc);
        double T1 = T0 / 1000.0;
        ax = localAccFilterd[0];
        ay = localAccFilterd[1];
        Sx += V0x * T1 + 0.5 * ax * T1 * T1;
        Sy += V0y * T1 + 0.5 * ay * T1 * T1;
        V0x = V0x + ax * T1;
        V0y = V0y + ay * T1;

        BigDecimal b1 = new BigDecimal(Double.toString(Sx));
        BigDecimal b2 = new BigDecimal(Double.toString(Sy));
        BigDecimal b3 = new BigDecimal(Double.toString(ax));
        BigDecimal b4 = new BigDecimal(Double.toString(ay));

        result[0] = b1.setScale(4, BigDecimal.ROUND_CEILING);
        result[1] = b2.setScale(4, BigDecimal.ROUND_CEILING);
        result[2] = b3.setScale(4, BigDecimal.ROUND_CEILING);
        result[3] = b4.setScale(4, BigDecimal.ROUND_CEILING);
        Log.d("ax", String.valueOf(ax));
        Log.d("ay", String.valueOf(ay));
        Log.d("Sx", String.valueOf(Sx));
        Log.d("Sy", String.valueOf(Sy));
        Log.d("V0x", String.valueOf(V0x));
        Log.d("V0y", String.valueOf(V0y));

        return result;
    }

    protected void onResume() {
        super.onResume();
        Sensor accelermeterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor rotvectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sensorManager.registerListener(listener, accelermeterSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(listener, rotvectorSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        sensorManager.unregisterListener(listener);
        super.onPause();
    }

    public double[] matrixLeftMult(float[] R1, float[] accValues){
        double[] Vec = new double[3];
        Vec[0] = R1[0] * accValues[0] + R1[1] * accValues[1] + R1[2] * accValues[2];
        Vec[1] = R1[3] * accValues[0] + R1[4] * accValues[1] + R1[5] * accValues[2];
        Vec[2] = R1[6] * accValues[0] + R1[7] * accValues[1] + R1[8] * accValues[2];
        return Vec;
    }

    // 进行卡尔曼滤波
    public double[] localAccFiltered(double[] localAcc){
        //计算卡尔曼增益
        double KalmanGain1 = EstimateCovariance1 / (EstimateCovariance1 + MeasureCovariance1);
        double KalmanGain2 = EstimateCovariance2 / (EstimateCovariance2 + MeasureCovariance2);
        //计算本次滤波估计值
        EstimateValue1 = EstimateValue1 + KalmanGain1 * (localAcc[0] - EstimateValue1);
        EstimateValue2 = EstimateValue2 + KalmanGain2 * (localAcc[1] - EstimateValue2);
        //更新估计协方差
        EstimateCovariance1 = (1-KalmanGain1) * EstimateCovariance1;
        EstimateCovariance2 = (1-KalmanGain2) * EstimateCovariance2;
        //更新测量方差
        MeasureCovariance1 = (1-KalmanGain1) * MeasureCovariance1;
        MeasureCovariance2 = (1-KalmanGain1) * MeasureCovariance2;
        //返回估计值
        double[] Return = new double[2];
        Return[0] = EstimateValue1;
        Return[1] = EstimateValue2;

//        Log.d("KalmanGain1", String.valueOf(KalmanGain1));
//        Log.d("KalmanGain2", String.valueOf(KalmanGain2));
//        Log.d("EstimateValue1", String.valueOf(EstimateValue1));
//        Log.d("EstimateValue2", String.valueOf(EstimateValue2));
//        Log.d("EstimateCovariance2", String.valueOf(EstimateCovariance2));
//        Log.d("MeasureCovariance2", String.valueOf(MeasureCovariance2));
        deviation = Math.abs((localAcc[0] - EstimateValue1) * (localAcc[1] - EstimateValue2));
        if (deviation < 0.00001)
        {
            EstimateCovariance1=0.1;
            EstimateCovariance2=0.1;
            MeasureCovariance1=0.02;
            MeasureCovariance2=0.02;
        }
        return Return;
    }


//    public static float[] Orientation(float[] R, float values[], float test[]) {
//
//        if (R.length == 9) {
//            values[0] = (float)Math.atan2(R[1], R[4]);
//            values[2] = (float)Math.atan2(-R[6], R[8]);
//            values[1] = (float)Math.atan2(-R[7], (R[4]/Math.cos(values[0])));
//
//        }
//        return values;
//    }
}

    //传感器的注册的注册可以写在onCreate之外
//    final SensorEventListener myAccelerometerListener = new SensorEventListener() {
//        public void onSensorChanged(SensorEvent sensorEvent) {
//            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
//                accelerometerValues = sensorEvent.values.clone();
//        }
//        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
//    };
//
//    final SensorEventListener myMagneticFieldListener = new SensorEventListener() {
//        public void onSensorChanged(SensorEvent sensorEvent) {
//            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
//                magneticFieldValues = sensorEvent.values.clone();
//        }
//        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
//    };

//    private void calculateOrientation(){
//        //获取三个角
////        Log.d("important",accelerometerValues.toString());
////        Log.d("important",magneticFieldValues.toString()); //两个都是空
//        SensorManager.getRotationMatrix(R1, null,
//                accelerometerValues,
//                magneticFieldValues);
//        SensorManager.getOrientation(R1, values);

//        values[0] = (float) Math.toDegrees(values[0]); // Azimuth
//        values[1] = (float) Math.toDegrees(values[1]); // Pitch
//        values[2] = (float) Math.toDegrees(values[2]); // Roll
//        String need = values[0] + "\n" + values[1] + "\n" + values[2] + "\n";
//        accelerationTextView.setText(need);

//    }


//            for(int i=0; i<3; i++){
//                if(Math.toDegrees(values[i]) < 0 ){
//                    values1[i] = 2 * Math.PI + values[i];
//                }
//                else{
//                    values1[i] = values[i];
//                }
//            }
//            toAverage0.add((float)Math.toDegrees(values[0]));// Azimuth
//            toAverage1.add((float)Math.toDegrees(values[1]));// Pitch
//            toAverage2.add((float)Math.toDegrees(values[2]));// Roll


//        need = values[0] + "\n" + values[1] + "\n" + values[2] + "\n";
//        need1 = test[0] + "\n" + test[1] + '\n' +test[2];
//        accelerationTextView.setText(need1);


//toAverage.add(toAverage0);
//        toAverage.add(toAverage1);
//        toAverage.add(toAverage2);
//for(int i = 0; i <=2; i++)
//        {
//        Float[] ary = (Float[])toAverage.get(i).toArray(new Float[toAverage.get(i).size()]);
//        float sum = 0;
//        for (int j = 0; j < ary.length; j++)
//        {
//        sum += ary[j];
//        }
//        values[i] = sum / ary.length;
//        toAverage.get(i).clear();
//        }


//    public float[] matrixTranspose(float[] R1){
//        float[] R2 = new float[9];
//        R2[0] = R1[0];
//        R2[1] = R1[3];
//        R2[2] = R1[6];
//        R2[3] = R1[1];
//        R2[4] = R1[4];
//        R2[5] = R1[7];
//        R2[6] = R1[2];
//        R2[7] = R1[5];
//        R2[8] = R1[8];
//        return R2;
//    }
