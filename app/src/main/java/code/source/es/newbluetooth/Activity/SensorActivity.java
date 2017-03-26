package code.source.es.newbluetooth.Activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import code.source.es.newbluetooth.R;

/**
 * Created by fanwe on 2016/12/27.
 */

public class SensorActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_sensor);
        Button button1 = (Button) findViewById(R.id.button_acc);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SensorActivity.this, AccActivity.class);
                startActivity(intent);
            }
        });
        Button button2 = (Button) findViewById(R.id.button_gyro);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SensorActivity.this, GyroAcctivity.class);
                startActivity(intent);
            }
        });


    }


}
