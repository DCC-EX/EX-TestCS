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

    private ok, int cv_values[256];

    JmDNS jmdns;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        wmanager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
//        String ip =
//                Formatter.formatIpAddress(wmanager.getConnectionInfo().getIpAddress());
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
                                        String cv = thisLine.substring(3, thisLine.length()-1);
                                        smessage.setText("<r "+cv+" 255>");
                                        button_sent.callOnClick();
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

//                // Wait a bit
//                Thread.sleep(120000);
//
//                // Unregister all services
//                jmdns.unregisterAllServices();
//                Log.d("EX-TestCS", "ServiceRegistration: jmdns.unregisterAllServices()");

            } catch (IOException e) {
                Log.e("EX-TestCS", "ServiceRegistration: " + e.getMessage());
//            } catch (InterruptedException e) {
//                Log.e("EX-TestCS", "ServiceRegistration: " + e.getMessage());
            }
        }
    }
}