package com.example.joystickapp;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Button;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private float roll, pitch, yaw;
    private InetAddress serverAddr;
    private final int serverPort = 9876;
    private SeekBar slider;

    // Set to keep track of currently pressed buttons
    private final Set<String> pressedButtons = new HashSet<>();
    // Scheduler for sending continuous button presses
    private ScheduledExecutorService buttonSenderScheduler;

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
                float adjustedYaw = progress - 90;
                send("manual_yaw:" + adjustedYaw);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBar.setProgress(90);
                send("manual_yaw:0");
            }
        });

        findViewById(R.id.lr).setOnClickListener(v -> send("lr"));
        findViewById(R.id.kr).setOnClickListener(v -> send("kr"));

        Object[][] buttonMappings = {
                { R.id.gear, "gear" },
                { R.id.brakes, "brakes" },
                { R.id.spoiler, "spoiler" },
                { R.id.flapsUp, "flapsup" },
                { R.id.flapsDown, "flapsdown" },
                { R.id.btn0, "b0" },
                { R.id.btn1, "b1" },
                { R.id.btn2, "b2" },
                { R.id.btn3, "b3" },
                { R.id.btn4, "b4" },
                { R.id.btn5, "b5" },
                { R.id.btn6, "b6" },
                { R.id.btn7, "b7" },
                { R.id.btn8, "b8" },
                { R.id.btn9, "b9" },
                { R.id.btn10, "b11" },
                { R.id.btn11, "b10" },
                { R.id.btn12, "b12" },
                { R.id.btn13, "b13" },
                { R.id.btn14, "b14" },
                { R.id.btn15, "b15" },
                { R.id.btn16, "b16" },
                { R.id.btn17, "b17" }
        };

        for (Object[] mapping : buttonMappings) {
            int viewId = (int) mapping[0];
            String btnName = (String) mapping[1];

            View btn = findViewById(viewId);

            btn.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (!pressedButtons.contains(btnName)) {
                            pressedButtons.add(btnName);
                            send("btn:" + btnName + "_down"); // Send initial down
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (pressedButtons.contains(btnName)) {
                            pressedButtons.remove(btnName);
                            send("btn:" + btnName + "_up"); // Send up once
                        }
                        break;
                }
                return true;
            });
        }

        // Initialize and start the scheduler to send continuous button presses
        buttonSenderScheduler = Executors.newSingleThreadScheduledExecutor();
        buttonSenderScheduler.scheduleAtFixedRate(() -> {
            synchronized (pressedButtons) {
                for (String btnName : pressedButtons) {
                    send("btn:" + btnName + "_down"); // Re-send down for held buttons
                }
            }
        }, 0, 50, TimeUnit.MILLISECONDS); // Send every 50ms (adjust as needed)
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shut down the scheduler when the activity is destroyed
        if (buttonSenderScheduler != null && !buttonSenderScheduler.isShutdown()) {
            buttonSenderScheduler.shutdownNow();
        }
        sensorManager.unregisterListener(this);
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