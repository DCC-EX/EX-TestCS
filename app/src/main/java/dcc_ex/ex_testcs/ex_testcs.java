package dcc_ex.ex_testcs;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

public class ex_testcs extends AppCompatActivity {
    Button button_sent;
    EditText smessage;
    TextView chat, display_status;
    String str, msg, line = "";
    int serverport = 2560;
    ServerSocket serverSocket;
    Socket client;
    Handler handler = new Handler();
    WifiManager wmanager;
    public String client_address;
    public Inet4Address client_address_inet4;
    public String ip;
    WifiInfo wifiinfo;
    Integer intaddr;
    DataInputStream in;
    DataOutputStream os;

    private int [] cvValues;
    private boolean [] powerStates;
    private String [] tracks;

    JmDNS jmdns;

    static String TRACK_POWER_BOTH = "";
    static String TRACK_POWER_MAIN = "MAIN";
    static String TRACK_POWER_PROG = "PROG";
    static String TRACK_POWER_JOIN = "JOIN";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        initValues();

        wmanager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        try {
            WifiManager wifi = (WifiManager) ex_testcs.this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifiinfo = wifi.getConnectionInfo();
            intaddr = wifiinfo.getIpAddress();
            if (intaddr != 0) {
                byte[] byteaddr = new byte[]{(byte) (intaddr & 0xff), (byte) (intaddr >> 8 & 0xff), (byte) (intaddr >> 16 & 0xff),
                        (byte) (intaddr >> 24 & 0xff)};
                client_address_inet4 = (Inet4Address) Inet4Address.getByAddress(byteaddr);
                client_address = client_address_inet4.toString().substring(1);      //strip off leading /
            }

            ip = client_address_inet4.toString().replaceAll("/", "");
        } catch (Exception except) {
            Log.e("EX-TestCS", "onCreate - error getting IP addr: " + except.getMessage());
        }

        smessage = (EditText) findViewById(R.id.smessage);
        chat = (TextView) findViewById(R.id.chat);
        display_status = (TextView)
                findViewById(R.id.display_status);
        display_status.setText("Server hosted on " + ip + ":" + serverport);
        Thread serverThread = new Thread(new serverThread());
        serverThread.start();

        button_sent = (Button) findViewById(R.id.button_sent);
        button_sent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread sentThread = new Thread(new sentMessage());
                sentThread.start();
            }
        });

        Thread jmdnsThread = new Thread(new ServiceRegistration());
        jmdnsThread.start();
    }

    @Override
    public void onDestroy() {
        closeConnections();

        jmdns.unregisterAllServices();
        Log.d("EX-TestCS", "onDestroy: jmdns.unregisterAllServices()");

        super.onDestroy();
    }

    void closeConnections() {
        try {
            in.close();
            os.close();
        } catch (Exception e) {
            Log.e("EX-TestCS", "onDestroy: Exception:" + e.getMessage());
        }

        try {
            client.close();
        } catch (Exception e) {
            Log.e("EX-TestCS", "onDestroy: Exception:" + e.getMessage());
        }
    }

    class sentMessage implements Runnable {
        @Override
        public void run() {
            try {
//                Socket client = serverSocket.accept();
//                DataOutputStream os = new
//                        DataOutputStream(client.getOutputStream());
                str = smessage.getText().toString() + "\n";
                msg = msg + "\n Server : " + str;
                Log.d("EX-TestCS", "sentMessage: Sending message" + str);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        chat.setText(msg);
                    }
                });
                os.writeBytes(str);
//                os.flush();
//                os.close();
//                client.close();
                smessage.setText("");
            } catch (IOException e) {
                Log.e("EX-TestCS", "sentMessage: IOException:" + e.getMessage());
            } catch (Exception e) {
                Log.e("EX-TestCS", "sentMessage: Exception:" + e.getMessage());
            }
        }
    }


    public class serverThread implements Runnable {
        @Override
        public void run() {

            client = null;
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(2560));

                client = serverSocket.accept();
                // setup i/p streams
                in = new DataInputStream(client.getInputStream());
                os = new DataOutputStream(client.getOutputStream());

                Log.d("EX-TestCS", "serverThread: Socket Open");

            } catch (UnknownHostException e) {
                Log.e("EX-TestCS", "serverThread: UnknownHostException: " + e.getMessage());
            } catch (IOException e) {
                Log.e("EX-TestCS", "serverThread: IOException: " + e.getMessage());
            } catch (Exception e) {
                Log.e("EX-TestCS", "serverThread: Exception:" + e.getMessage());
            }

            while(true){
                try {
                    line = null;
                    while ((line = in.readLine()) != null) {
                        Log.d("EX-TestCS", "received: " + line);
                        msg = msg + "\n Client : " + line;
                        handler.post(new Runnable() {
                            String thisLine;
                            @Override
                            public void run() {
                                Log.d("EX-TestCS", "runnable(): processing: " + line);
                                chat.setText(msg);
                                if (thisLine != null) {
                                    if (thisLine.equals("<s>")) {
                                        smessage.setText("<iDCCEX v-4.2.54 / MEGA / STANDARD_MOTOR_SHIELD G-Devel-202305250828Z>");
                                        button_sent.callOnClick();
                                    } else if (thisLine.equals("<#>")) {
                                        smessage.setText("<#>");
                                        button_sent.callOnClick();
                                    } else if (thisLine.equals("<R>")) {
                                        smessage.setText("<r 1234>");
                                        button_sent.callOnClick();
                                    } else if ((thisLine.charAt(1) == 'R') && (line.length() > 3) ) {   //CV read request
                                        int cv = Integer.valueOf(thisLine.substring(3, thisLine.length()-1));
                                        smessage.setText("<r " + cv + " " + cvValues[cv] + ">");
                                        button_sent.callOnClick();
                                    } else if ((thisLine.charAt(1) == 'W') && (line.length() > 3) ) {   //CV read request
                                        String[] params = thisLine.substring(3, thisLine.length()-1).split(" ");
                                        int cv = Integer.valueOf(params[0]);
                                        int cvValue = Integer.valueOf(params[1]);
                                        cvValues[cv] = cvValue;
                                        smessage.setText("<r " + cv + " " + cvValues[cv] + ">");
                                        button_sent.callOnClick();
                                    } else if (thisLine.equals("<0>")) {
                                        setPower(TRACK_POWER_BOTH, false);
                                    } else if (thisLine.equals("<1>")) {
                                        setPower(TRACK_POWER_BOTH, true);
                                    } else if ( ((thisLine.charAt(1) == '0') || (thisLine.charAt(1) == '1'))
                                            && (line.length() > 3) ) {   //CV read request
                                        String[] params = thisLine.substring(1, thisLine.length()-1).split(" ");
                                        setPower(params[1], (params[0].equals("0") ? false : true) );
                                    } else {
                                        Log.d("EX-TestCS", "serverThread: Unknown command: " + line);
                                    }
                                }
                            }
                            public Runnable init(String line) {
                                this.thisLine=line;
                                return(this);
                            }
                        }.init(line));
                    }
                    Thread.sleep(100);

                } catch (UnknownHostException e) {
                    Log.e("EX-TestCS", "serverThread: UnknownHostException:" + e.getMessage());
                } catch (IOException e) {
                    Log.e("EX-TestCS", "serverThread: IOException: " + e.getMessage());
                } catch (InterruptedException e) {
                    Log.e("EX-TestCS", "serverThread: InterruptedException: " + e.getMessage());
                } catch (Exception e) {
                    Log.e("EX-TestCS", "serverThread: Exception: " + e.getMessage());
                }
            }
        }
    }

    public class ServiceRegistration implements Runnable {
        @Override
        public void run() {
//        public void main(String[] args) throws InterruptedException {

            try {
                // Create a JmDNS instance
                InetAddress ia = InetAddress.getByName(ip);
                jmdns = JmDNS.create(ia, ip);
                jmdns.unregisterAllServices();

                // Register a service
                ServiceInfo serviceInfo = ServiceInfo.create("_withrottle._tcp.local.", "EX-TestCS", 2560, "EX-TestCS");
                jmdns.registerService(serviceInfo);
                Log.d("EX-TestCS", "ServiceRegistration: jmdns.registerService()");

            } catch (IOException e) {
                Log.e("EX-TestCS", "ServiceRegistration: " + e.getMessage());
            }
        }
    }

    void initValues() {
        cvValues = new int[256];
        for (int i=0; i<256; i++) {
            cvValues[i] = 255;
        }

        powerStates = new boolean[8];
        tracks = new String[8];
        for (int i=0; i<8; i++) {
            powerStates[i] = false;
            tracks[i] = "OFF";
        }
        tracks[0] = "MAIN";
        tracks[1] = "PROG";
    }

    void setPower(String track, boolean powerOn) {
        if (track.equals("")) {  // all tracks
            for (int i=0; i<8; i++) {
                powerStates[i] = powerOn;
            }
            smessage.setText("<p" + (powerOn ? 1 : 0) + ">");
            button_sent.callOnClick();
        } else {
            for (int i=0; i<8; i++) {
                if (tracks[i].equals(track)) {
                    powerStates[i] = powerOn;
                    smessage.setText("<p" + (powerOn ? 1 : 0) + " " + track + ">");
                    button_sent.callOnClick();
                }
            }
        }
    }
}