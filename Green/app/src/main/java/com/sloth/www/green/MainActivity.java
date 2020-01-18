package com.sloth.www.green;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.sloth.www.green.pipe.GreenClientReceiver;

public class MainActivity extends AppCompatActivity implements GreenClientReceiver.Callback{

    private Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn = findViewById(R.id.btn);

        startService(new Intent(this, GreenService.class));

        registerReceiver();

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent it = new Intent(GreenConfig.ACTION_SERVER);
                sendBroadcast(it);
                Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
                Toast.makeText(MainActivity.this, "找到【猪包】，开启服务即可", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerReceiver() {
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(GreenConfig.ACTION_CLIENT);
        registerReceiver(receiver, iFilter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    private GreenClientReceiver receiver = new GreenClientReceiver(this);

    @Override
    public void state(boolean opening) {
        if(btn != null){
            btn.setText(opening ? "点击暂停" : "点击开启");
        }
    }
}
