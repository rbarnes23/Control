package com.control.robot;

import java.io.UnsupportedEncodingException;
import java.util.Date;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;


public class mainActivity extends Activity implements SensorEventListener {
    private SensorManager sensorManager;
    public boolean isClientSet = false;
    int maxmillis = 2000;
    double lastmillis;
    //NotificationManager mNotificationManager;
    int CONTROL_ID = 1;
    public static String MQTTHOST = "tcp://mapmymotion.com";
    public static String USERNAME = "rbarnes";
    public static String PASSWORD = "sasha23";
    static String mTopic = "BARNES123";
    static MqttAndroidClient client;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        setClient();


    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float[] values = event.values;
            // Movement
            int x = (int) ((values[0] * 1));

            int y = (int) (values[1] * 1);
            int z = (int) (values[2] * 1);
            TextView XValue = (TextView) findViewById(R.id.x);
            XValue.setText("X: " + Integer.toString(x));

            TextView YValue = (TextView) findViewById(R.id.y);
            YValue.setText("Direction: " + Integer.toString(y));

            TextView ZValue = (TextView) findViewById(R.id.z);
            ZValue.setText("Speed: " + Integer.toString(z));

            // Toast.makeText(this, Float.toString(values[0]),
            // Toast.LENGTH_SHORT).show();
            long now = (new Date()).getTime();
            if (((now - lastmillis) > maxmillis) && client.isConnected()) {
                sendPayload("ACC|" + Integer.toString(x) + "|"
                        + Integer.toString(y) + "|" + Integer.toString(z)
                        + "\0");
                lastmillis = now;
            }

            float accelationSquareRoot = (x * x + y * y + z * z)
                    / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
            if (accelationSquareRoot >= 4) // twice the acceleration of earth
                Toast.makeText(this, "Device was shaken!", Toast.LENGTH_SHORT)
                        .show();
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void onDestroy() {
        try{
        client.disconnect();
        client.close();
        } catch ( MqttException e) {
            e.printStackTrace();
        }

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // register this class as a listener for the orientation and
        // accelerometer sensors

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        //reb setClient();
    }

    @Override
    protected void onPause() {
        // unregister listener
        sensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    protected void onStop() {
      //  mNotificationManager.cancel(CONTROL_ID);
        // Unregister the listener
       // sensorManager.unregisterListener(this);
        super.onStop();
    }

    public void setClient() {
        String clientId = MqttClient.generateClientId();
        client =
                new MqttAndroidClient(this.getApplicationContext(), MQTTHOST,
                        clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(USERNAME);
        options.setPassword(PASSWORD.toCharArray());
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);


        try {
            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_LONG).show();
                    sendPayload("Connected");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Toast.makeText(getApplicationContext(), "Not Connected", Toast.LENGTH_LONG).show();

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    private void sendPayload(String payload) {
        if (client.isConnected()) {
            String topic = mTopic;
            byte[] encodedPayload = new byte[0];
            try {
                encodedPayload = payload.getBytes("UTF-8");
                MqttMessage message = new MqttMessage(encodedPayload);
                client.publish(topic, message);
                //client.publish(topic, message.getBytes(), 0, false);
            } catch (UnsupportedEncodingException | MqttException e) {
                e.printStackTrace();
            }

            }
        else{
            setClient();
        }
    }
}


