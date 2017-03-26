package code.source.es.newbluetooth.Activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import code.source.es.newbluetooth.Service.ScanService;

public class LineTestActivity extends Activity {
    static long timeout=1000*5;//5 seconds
    DisplayMetrics dm = new DisplayMetrics();
    float height, width;
    final int numberofPoint = 4;
    final int distanceOfABC = 4;//4m
    float[] distance = new float[numberofPoint];
    int[] RSSIArray = new int[numberofPoint];

    Map NodeTime = new HashMap();
    Map Node = new HashMap();
    View view;

    Messenger messenger;


    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            messenger = new Messenger(service);
            Message msg = new Message();
            msg.what = ScanService.START_BLUETOOTH;
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
            if (ScanService.GET_BLUETOOTH_RSSI.equals(intent.getAction())) {
                float[] d = LineTestActivity.this.distance;
                String name = intent.getExtras().getString("name");
                String MAC = intent.getExtras().getString("MAC");
                int RSSI = intent.getExtras().getInt("RSSI");
                double distance = intent.getExtras().getDouble("distance");
                for (int i = 0; i < d.length; i++) {//record rssi，rssiflag
                    String s = "BINGO" + (i + 1);
                    if (s.equals(name)) {
                        //RSSIArray[i] = RSSI;
                        //d[i] = (float) distance;
                        if (RSSI<0){
                            Node.put(i + 1, RSSI);
                            NodeTime.put(i+1,System.currentTimeMillis());//获取当前的毫秒数
                        }
                        break;
                    }
                }
                Set ss = new HashSet();//初始化将要被删除的超时节点集合
                Iterator iter = NodeTime.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry kv = (Map.Entry) iter.next();
                    int k=(int) kv.getKey();
                    long v = (long) kv.getValue();
                    if (System.currentTimeMillis()-v>timeout){//判断是否超时
                        ss.add(k);
                    }

                }
                //删除超时节点信息
                Iterator it = ss.iterator();
                while (it.hasNext()){
                    int k = (int)it.next();
                    NodeTime.remove(k);
                    Node.remove(k);
                }

                reView();//重新绘制界面
            }
        }
    };
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("BasicActivity", getClass().getSimpleName());
        //setContentView(R.layout.activity_line_test);
        initValues();
        initDate();
        view = new MyView(this);
        setContentView(view);

        IntentFilter filter = new IntentFilter(ScanService.GET_BLUETOOTH_RSSI);
        this.registerReceiver(mReceiver, filter);
        bindService(new Intent(this, ScanService.class), conn, BIND_AUTO_CREATE);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

    }

    private void initValues() {
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        height = dm.heightPixels;
        width = dm.widthPixels;
    }

    private void initDate() {

        for (int i = 0; i < distance.length; i++) {
            distance[i] = 100f;
        }
        distance[1] = 1.7f;
        distance[2] = 2.7f;
    }

    void reView() {

        Message msg = new Message();
        msg.what = ScanService.START_BLUETOOTH;
        try {
            messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        view.postInvalidate();
    }

    private float getHeightOfScreen(float[] f) { //返回 用户所在位置
        if (Node.isEmpty())
            return -1.0f;

        int maxK = -1, maxV = -200;
        Iterator iter = Node.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry kv = (Map.Entry) iter.next();
            int v = (int) kv.getValue();
            if (v >= maxV) {
                maxK = (int) kv.getKey();
                maxV = v;
            }
        }
        int db=5;
        int left=0;
        int right=0;
        if(Node.containsKey(maxK - 1))
            left=(int) Node.get(maxK-1);
        if(Node.containsKey(maxK+1))
            right=(int) Node.get(maxK+1);
        if (left!=0&&right!=0) {//左右邻节点都存在
            if(left>right){
                if(maxV-left>db)
                    return maxK-0.25f;
                return maxK-0.5f;
            }else{
                if(maxV-right>db)
                    return maxK+0.25f;
                return maxK+0.5f;
            }
        }
        if (left!=0){
            if(maxV-left>db)
                return maxK-0.25f;
            return maxK-0.5f;
        }
        if (right!=0){
            if(maxV-right>db)
                return maxK+0.25f;
            return maxK+0.5f;
        }
        return maxK;
    }


    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("LineTest Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());


    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    private class MyView extends View {
        public MyView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(Color.WHITE);
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(Color.BLUE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            paint.setTextSize(25);
            canvas.drawLine(width * 0.25f, 0, width * 0.25f, height, paint);
            canvas.drawLine(width * 0.65f, 0, width * 0.65f, height, paint);
            float avg = height / (numberofPoint + 1.0f);//修改过
            Iterator iter = Node.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry kv = (Map.Entry) iter.next();
                int k=(int) kv.getKey();
                int v = (int) kv.getValue();
                canvas.drawCircle(width * 0.45f, avg * (k), 4, paint);
                char s = (char) ('A' + k-1);
                canvas.drawText(s + " RSSI=" + v, width * 0.47f, avg * k, paint);
            }
            paint.setColor(Color.RED);
            float h=getHeightOfScreen(distance);
            canvas.drawCircle(width * 0.45f, h * avg, 4, paint);
            canvas.drawText("您的位置", width * 0.35f, h * avg, paint);

        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return true;
        }
    }


    class TestThread extends Thread {
        @Override
        public void run() {
            while (true) {
                distance[0] += 0.1f;
                view.postInvalidate();
                try {
                    this.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
