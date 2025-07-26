package com.example.joystickapp;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Button;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private float roll, pitch, yaw;
    private InetAddress serverAddr;
    private final int serverPort = 9876;
    private SeekBar slider;

    private void send(String msg) {
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket();
                byte[] buf = msg.getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddr, serverPort);
                socket.send(packet);
                socket.close();
            } catch (Exception ignored) {}
        }).start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            serverAddr = InetAddress.getByName("192.168.8.102");
        } catch (Exception e) { finish(); }

        setContentView(R.layout.activity_main);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);

        slider = findViewById(R.id.slider1);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                send("throttle:" + progress);
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        SeekBar slider2 = findViewById(R.id.slider2);
        slider2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float adjustedYaw = progress - 90; // Convert slider [0..180] to [-90..+90]
                send("manual_yaw:" + adjustedYaw);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBar.setProgress(90); // Reset to center on release
                send("manual_yaw:0");
            }
        });

        findViewById(R.id.gear).setOnClickListener(v -> send("btn:gear"));
        findViewById(R.id.brakes).setOnClickListener(v -> send("btn:brakes"));
        findViewById(R.id.spoiler).setOnClickListener(v -> send("btn:spoiler"));
        findViewById(R.id.flapsUp).setOnClickListener(v -> send("btn:flapsup"));
        findViewById(R.id.flapsDown).setOnClickListener(v -> send("btn:flapsdown"));

        findViewById(R.id.lr).setOnClickListener(v -> send("lr"));
        findViewById(R.id.kr).setOnClickListener(v -> send("kr"));

        findViewById(R.id.btn0).setOnClickListener(v -> send("btn:b0"));
        findViewById(R.id.btn1).setOnClickListener(v -> send("btn:b1"));
        findViewById(R.id.btn2).setOnClickListener(v -> send("btn:b2"));
        findViewById(R.id.btn3).setOnClickListener(v -> send("btn:b3"));
        findViewById(R.id.btn4).setOnClickListener(v -> send("btn:b4"));
        findViewById(R.id.btn5).setOnClickListener(v -> send("btn:b5"));
        findViewById(R.id.btn6).setOnClickListener(v -> send("btn:b6"));
        findViewById(R.id.btn7).setOnClickListener(v -> send("btn:b7"));
        findViewById(R.id.btn8).setOnClickListener(v -> send("btn:b8"));
        findViewById(R.id.btn9).setOnClickListener(v -> send("btn:b9"));
        findViewById(R.id.btn11).setOnClickListener(v -> send("btn:b10"));
        findViewById(R.id.btn10).setOnClickListener(v -> send("btn:b11"));
        findViewById(R.id.btn12).setOnClickListener(v -> send("btn:b12"));
        findViewById(R.id.btn13).setOnClickListener(v -> send("btn:b13"));
        findViewById(R.id.btn14).setOnClickListener(v -> send("btn:b14"));
        findViewById(R.id.btn15).setOnClickListener(v -> send("btn:b15"));
        findViewById(R.id.btn16).setOnClickListener(v -> send("btn:b16"));
        findViewById(R.id.btn17).setOnClickListener(v -> send("btn:b17"));

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] rot = new float[9];
        float[] orient = new float[3];
        SensorManager.getRotationMatrixFromVector(rot, event.values);
        SensorManager.getOrientation(rot, orient);
        roll = (float) Math.toDegrees(orient[2]);
        pitch = (float) Math.toDegrees(orient[1]);
        yaw = (float) Math.toDegrees(orient[0]);
        send(roll + "," + pitch + "," + yaw);
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}