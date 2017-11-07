package com.example.linda.mpower;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Set;
import java.util.UUID;

/**
 * Created by linda on 12/09/17.
 */

public class MainActivity extends AppCompatActivity {
    private long startTime = 0L;
    private Handler customHandler = new Handler();
    long timeInMilliseconds = 0L;
    long timeSwapBuff = 0L;
    long updatedTime = 0L;
    TextView timer;
    TextView Powerlimit;
    TextView sens;
    TextView totalEnergy;
    Button onButton;
    Button offButton;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;
    double dataDouble = 0;
    double totalPower;
    String stringEnergy;
    String usage;
    Button con;
    Double us;
    TextView state;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sens = (TextView) findViewById(R.id.sensor_values);
        state = (TextView) findViewById(R.id.status);
        timer = (TextView) findViewById(R.id.time);
        Powerlimit = (TextView) findViewById(R.id.limit);
        onButton = (Button) findViewById(R.id.open);
        con = (Button) findViewById(R.id.connect);
        offButton = (Button) findViewById(R.id.close);
        totalEnergy = (TextView) findViewById(R.id.energy);
        View d = findViewById(R.id.connect);
        d.setVisibility(View.GONE);
        findBT();
        try {
            openBT();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void connectBT (View view)
    {
        usage = null;
        findBT();
        try {
            openBT();
        } catch (IOException e) {
            e.printStackTrace();
        }
        View b = findViewById(R.id.open);
        b.setVisibility(View.VISIBLE);
        View c = findViewById(R.id.close);
        c.setVisibility(View.VISIBLE);
        View d = findViewById(R.id.connect);
        d.setVisibility(View.GONE);
        Powerlimit.setText("");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.limit:
                setLimit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setLimit() {
        final EditText setLimit = new EditText(this);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Usage Limit")
                .setMessage("What limit would you like to set?")
                .setView(setLimit)
                .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        usage = setLimit.getText().toString();
                        Toast.makeText(getApplicationContext(), "Usage set", Toast.LENGTH_SHORT).show();
                        Powerlimit.setText(usage);
                        try {
                            checkData();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
    }

    public void checkData() throws IOException {
        if(usage != null)
        {
        us = Double.parseDouble(usage);
            if (totalPower>us) {
                Toast.makeText(getApplicationContext(), "Limit Reached", Toast.LENGTH_SHORT).show();
                Vibrator vib = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                vib.vibrate(2000);
                addNotification();
                mmOutputStream.write("0".getBytes());
                timeSwapBuff += timeInMilliseconds;
                customHandler.removeCallbacks(updateTimerThread);
                closeBT();
            }
        }
    }
    public void addNotification()
    {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.notification)
                        .setContentTitle("Usage limit reached")
                        .setContentText("You  have reached limit set")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(Notification.DEFAULT_ALL);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        // Add as notification
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }

    private void findBT() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            Toast.makeText(this,"No bluetooth adapter available", Toast.LENGTH_SHORT);
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("HC-05"))
                {
                    mmDevice = device;
                    break;
                }
            }

            Toast.makeText(this,"Device Found", Toast.LENGTH_SHORT).show();
        }
    }
    private void openBT() throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

        Toast.makeText(this,"Bluetooth Opened", Toast.LENGTH_SHORT).show();

    }

    private void beginListenForData() {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {

                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            sens.setText(data + " Joules");
                                            try{
                                                dataDouble = Double.parseDouble(data);}
                                            catch (NumberFormatException ex)
                                            {
                                                dataDouble = 0.30;
                                                //Toast.makeText(getApplication(),"NumberFormatException" ,Toast.LENGTH_SHORT).show();
                                            }
                                            String stt = (String) state.getText();
                                            if(stt=="Light Bulb is on")
                                            addData();
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    private void addData() {
        DecimalFormat df = new DecimalFormat("#.00");
        totalPower += dataDouble;
        df.format(totalPower);
        stringEnergy = String.valueOf(totalPower);
        totalEnergy.setText(stringEnergy + " Watts");
        try {
            checkData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onBT(View view) throws IOException {

        mmOutputStream.write("1".getBytes());
        beginListenForData();
        startTime = SystemClock.uptimeMillis();
        customHandler.postDelayed(updateTimerThread, 0);
       state.setText("Light Bulb is on");
    }

    public void offBT(View view) throws IOException {
        mmOutputStream.write("0".getBytes());
        timeSwapBuff += timeInMilliseconds;
        customHandler.removeCallbacks(updateTimerThread);
        state.setText("Light Bulb is off");
    }

    private Runnable updateTimerThread = new Runnable() {

        public void run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;
            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            secs = secs % 60;
            int milliseconds = (int) (updatedTime % 1000);
            timer.setText("" + mins + ":"
                    + String.format("%02d", secs) + ":"
                    + String.format("%03d", milliseconds));
            customHandler.postDelayed(this, 0);
        }
    };
    void closeBT() throws IOException
    {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        Toast.makeText(this,"Bluetooth Closed" ,Toast.LENGTH_SHORT).show();
        state.setText("Light Bulb is off");

        sens.setText("");
        View b = findViewById(R.id.open);
        b.setVisibility(View.GONE);
        View c = findViewById(R.id.close);
        c.setVisibility(View.GONE);
        View d = findViewById(R.id.connect);
        d.setVisibility(View.VISIBLE);
    }
}



