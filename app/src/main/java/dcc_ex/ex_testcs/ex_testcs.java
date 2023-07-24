package dcc_ex.ex_testcs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

public class ex_testcs extends AppCompatActivity {
    Button button_sent;
    EditText smessage;
    TextView chat, display_status;
    String str, msgHistory, line = "";
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

    boolean connectionOpen = false;

    private int[] cvValues;
    private boolean[] tracksPower;
    private String[] tracksMode;
    private int [] tracksAddress;
    private int [] tracksCurrent;

    JmDNS jmdns;

    static String TRACK_POWER_BOTH = "";
    static String TRACK_POWER_MAIN = "MAIN";
    static String TRACK_POWER_PROG = "PROG";
    static String TRACK_POWER_JOIN = "JOIN";

    static String TRACK_MODE_MAIN = "MAIN";
    static String TRACK_MODE_PROG = "PROG";
    static String TRACK_MODE_DC = "DC";
    static String TRACK_MODE_DCX = "DCX";
    static String TRACK_MODE_OFF = "OFF";

    Queue<String> queue = new LinkedList<>();

    Menu tMenu;

    SharedPreferences prefs;
    String pref_dcc_ex_version = "";
    String pref_dcc_ex_board = "";
    String pref_dcc_ex_motor_shield = "";
    String pref_dcc_ex_current_max = "";

    String pref_loco_address = "";

    Random rand = new Random();

    int [] randomLocoAddress = {123, 234, 345, 456};
    int [] randomLocoSpeed = {0, 0, 0, 0};
    boolean [] randomLocoDir = {true, true, true, true};
    boolean [] randomLocoAction = {true, true, true, true};

    boolean exitConfirmed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        initValues();
        getSharedPreferences();

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

        Thread serverThread = new Thread(new ServerThread());
        serverThread.start();

        Thread outputQueueThread = new Thread(new OutputQueueThread());
        outputQueueThread.start();

        Thread randomEventsThread = new Thread(new RandomEventsThread());
        randomEventsThread.start();

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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.ex_testcs_menu, menu);
        tMenu = menu;

        return  super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        Intent in;
        switch (item.getItemId()) {
            case R.id.reopen_connections_menu_item:
                closeConnections();
                openConnections();
                return true;
            case R.id.preferences_menu_item:
                in = new Intent().setClass(this, settings_activity.class);
                mLauncher.launch(in);
                return true;
            case R.id.exit_menu_item:
                checkExit(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    ActivityResultLauncher<Intent> mLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                // Do your code from onActivityResult
                getSharedPreferences();
            }
        });

    //Handle pressing of the back button to end this activity
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_BACK) {
            checkExit(this);
            return (true);
        }
        return (super.onKeyDown(key, event));
    }

    // prompt for Exit
    public void checkExit(final Activity activity) {
        final AlertDialog.Builder b = new AlertDialog.Builder(activity);
        b.setIcon(android.R.drawable.ic_dialog_alert);
        b.setTitle(R.string.dialog_exit_title);
        b.setMessage(R.string.dialog_exit_text);
        b.setCancelable(true);
        b.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                exitConfirmed = true;
                finishAffinity();
            }
        });
        b.setNegativeButton(R.string.no, null);
        AlertDialog alert = b.create();
        alert.show();

        // find positiveButton and negativeButton
        Button positiveButton = alert.findViewById(android.R.id.button1);
        Button negativeButton = alert.findViewById(android.R.id.button2);
        // then get their parent ViewGroup
        ViewGroup buttonPanelContainer = (ViewGroup) positiveButton.getParent();
        int positiveButtonIndex = buttonPanelContainer.indexOfChild(positiveButton);
        int negativeButtonIndex = buttonPanelContainer.indexOfChild(negativeButton);
        if (positiveButtonIndex < negativeButtonIndex) {  // force 'No' 'Yes' order
            // prepare exchange their index in ViewGroup
            buttonPanelContainer.removeView(positiveButton);
            buttonPanelContainer.removeView(negativeButton);
            buttonPanelContainer.addView(negativeButton, positiveButtonIndex);
            buttonPanelContainer.addView(positiveButton, negativeButtonIndex);
        }
    }

    // ******************************************************************************************//
    // ******************************************************************************************//
    // ******************************************************************************************//


    void openConnections() {
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

            connectionOpen = true;

        } catch (UnknownHostException e) {
            Log.e("EX-TestCS", "serverThread: UnknownHostException: " + e.getMessage());
        } catch (IOException e) {
            Log.e("EX-TestCS", "serverThread: IOException: " + e.getMessage());
        } catch (Exception e) {
            Log.e("EX-TestCS", "serverThread: Exception:" + e.getMessage());
        }
    }

    void closeConnections() {
        try {
            in.close();
            os.close();
        } catch (Exception e) {
            Log.e("EX-TestCS", "closeConnections: I/O Exception:" + e.getMessage());
        }

        try {
            client.close();
        } catch (Exception e) {
            Log.e("EX-TestCS", "closeConnections: Client: Exception:" + e.getMessage());
        }

        try {
            serverSocket.close();
        } catch (Exception e) {
            Log.e("EX-TestCS", "closeConnections: Socket: Exception:" + e.getMessage());
        }
        connectionOpen = false;
    }

    class sentMessage implements Runnable {
        @Override
        public void run() {
            try {
                str = smessage.getText().toString() + "\n";
                msgHistory = msgHistory + "\n Server : " + str;
                Log.d("EX-TestCS", "sentMessage: Sending message" + str);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        chat.setText(msgHistory);
                    }
                });
                os.writeBytes(str);
                smessage.setText("");
            } catch (IOException e) {
                Log.e("EX-TestCS", "sentMessage: IOException:" + e.getMessage());
            } catch (Exception e) {
                Log.e("EX-TestCS", "sentMessage: Exception:" + e.getMessage());
            }
        }
    }

    // ******************************************************************************************//

    public class OutputQueueThread implements Runnable {
        public void run() {
            while (true) {
                if ( (connectionOpen) && (queue!=null) && (!queue.isEmpty()) && (os!=null) ) {
                    try {
                        str = queue.remove();
                        msgHistory = msgHistory + "\n Server : " + str;
                        Log.d("EX-TestCS", "OutputQueueThread: Sending message" + str);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                chat.setText(msgHistory);
                            }
                        });
                        str = str + "\n";
                        os.writeBytes(str);

                        Thread.sleep(100);

                    } catch (IOException e) {
                        Log.e("EX-TestCS", "OutputQueueThread: IOException:" + e.getMessage());
                    } catch (InterruptedException e) {
                        Log.e("EX-TestCS", "OutputQueueThread: InterruptedException: " + e.getMessage());
                    } catch (Exception e) {
                        Log.e("EX-TestCS", "OutputQueueThread: Exception:" + e.getMessage());
                    }
                }
            }
        }
    }

    public class ServerThread implements Runnable {
        @Override
        public void run() {

            while (true) {
                if (!connectionOpen) {
                    openConnections();
                } else {
                    try {
                        line = null;
                        while ((line = in.readLine()) != null) {
                            Log.d("EX-TestCS", "received: " + line);
                            msgHistory = msgHistory + "\n Client : " + line;
                            handler.post(new Runnable() {
                                String thisLine;

                                @Override
                                public void run() {
                                    Log.d("EX-TestCS", "runnable(): processing: " + line);
                                    chat.setText(msgHistory);
                                    if (thisLine != null) {
                                        processIncomingMessage(thisLine);
                                    }
                                }

                                public Runnable init(String line) {
                                    this.thisLine = line;
                                    return (this);
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

    // ******************************************************************************************//

    void initValues() {
        msgHistory = "";

        cvValues = new int[256];
        for (int i = 0; i < 256; i++) {
            cvValues[i] = 255;
        }

        tracksPower = new boolean[8];
        tracksMode = new String[8];
        tracksAddress = new int[8];
        tracksCurrent = new int[8];
        for (int i = 0; i < 8; i++) {
            tracksPower[i] = false;
            tracksMode[i] = TRACK_MODE_OFF;
            tracksAddress[i] = -1;
            tracksCurrent[i] = 0;
        }
        tracksMode[0] = TRACK_POWER_MAIN;
        tracksMode[1] = TRACK_POWER_PROG;

        for (int i=0; i<randomLocoAddress.length; i++) {
            randomLocoSpeed[i] = rand.nextInt(127);
            randomLocoDir[i] = ((rand.nextInt(1)==1) ? true : false);
        }
    }

    void getSharedPreferences() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        pref_dcc_ex_version= prefs.getString("pref_dcc_ex_version", getResources().getString(R.string.pref_dcc_ex_version_default));
        pref_dcc_ex_board = prefs.getString("pref_dcc_ex_board", getResources().getString(R.string.pref_dcc_ex_board_default));
        pref_dcc_ex_motor_shield = prefs.getString("pref_dcc_ex_motor_shield", getResources().getString(R.string.pref_dcc_ex_motor_shield_default));
        pref_dcc_ex_current_max = prefs.getString("pref_dcc_ex_current_max", getResources().getString(R.string.pref_dcc_ex_current_max_default));

        pref_loco_address = prefs.getString("pref_loco_address", getResources().getString(R.string.pref_loco_address_default));
    }

    // ******************************************************************************************//

    int getSpeedFromSpeedByte(int speedByte) {
        int dir = -1;
        int speed = speedByte;
        if (speed >= 128) {
            speed = speed - 128;
            dir = 1;
        }
        if (speed>1) {
            speed = speed - 1; // get round and idiotic design of the speed command
        } else {
            speed=0;
        }
        return speed * dir;
    }

    int getSpeedByteFromSpeed(int speed, int dir) {  // dir 0=reverse 1=forward
        int speedByte = 0;
        if (dir==0) { // reverse
            speedByte = speed;
        } else {
            speedByte = speed + 128 + 1;
        }
        return speedByte;
    }

    public class RandomEventsThread implements Runnable {
        String msg = "";
        @Override
        public void run() {
            while (true) {
                try {
                    if (connectionOpen) {
                        for (int i=0; i<randomLocoAddress.length; i++) {
                            randomLocoSpeed[i] = randomLocoSpeed[i] + ( (randomLocoAction[i]) ? 5 : -5);

                            if (randomLocoSpeed[i]>126) {
                                randomLocoSpeed[i]=126;
                                randomLocoAction[i] = !randomLocoAction[i];
                            }

                            if (randomLocoSpeed[i]<0) {
                                randomLocoSpeed[i]=1;
                                randomLocoDir[i] = !randomLocoDir[i];
                                randomLocoAction[i] = !randomLocoAction[i];
                            }
                            msg = "<l " + randomLocoAddress[i] + " 1 " + getSpeedByteFromSpeed(randomLocoSpeed[i], (randomLocoDir[i] ? 1 : 0) ) + " 0>";
                            queue.add(msg);
                        }

                    }
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Log.e("EX-TestCS", "RandomEvents: InterruptedException: " + e.getMessage());
                }
            }
        }
    }

    // ******************************************************************************************//

    char getTrackLetter(int trackNo) {
        return (char) ('A' + trackNo);
    }

    int getTrackNumber(char trackLetter) {
        return (int) (trackLetter - 'A');
    }


    void setPower(String track, boolean powerOn) {
        if (track.equals("")) {  // all tracks
            for (int i = 0; i < 8; i++) {
                tracksPower[i] = powerOn;
            }
            queue.add("<p" + (powerOn ? 1 : 0) + ">");
        } else {
            for (int i = 0; i < 8; i++) {
                if ((tracksMode[i].equals(track))
                        || (((tracksMode[i].equals("MAIN")) || ((tracksMode[i].equals("PROG")) && (track.equals("JOIN")))))) {
                    tracksPower[i] = powerOn;
                    queue.add("<p" + (powerOn ? 1 : 0) + " " + track + ">");
                }
            }
        }
    }

    void getTrackModes() {
        String msg;
        for (int i = 0; i < 8; i++) {
            if ((tracksMode[i].equals(TRACK_MODE_DC)) || tracksMode[i].equals(TRACK_MODE_DCX)) {
                msg = "<= " + getTrackLetter(i) + " " + tracksMode[i] + " " + tracksAddress[i] + ">";
            } else {
                msg = "<= " + getTrackLetter(i) + " " + tracksMode[i] + ">";
            }
            queue.add(msg);
        }
    }

    void setTrackMode(int track, String trackMode, int address) {
        tracksMode[track] = trackMode;
        if ( (trackMode.equals(TRACK_MODE_DC)) || trackMode.equals(TRACK_MODE_DCX) ) {
            tracksAddress[track] = address;
        } else {
            tracksAddress[track] = -1;
        }

        boolean foundProg = false;
        for (int i = 0; i < 8; i++) {
            if (tracksMode[i].equals(TRACK_MODE_PROG)) {
                if (!foundProg) {
                    foundProg = true;
                } else {  // can only have one
                    tracksMode[i] = TRACK_MODE_OFF;
                }
            }
        }

    }

    void getTracksCurrent() {
        String msg = "<jI ";
        for (int i = 0; i < 8; i++) {
            if (!tracksMode[i].equals(TRACK_MODE_OFF)) {
                msg = msg + rand.nextInt(Integer.valueOf(pref_dcc_ex_current_max));
            } else {
                msg = msg + "0";
            }
            if (i<7) msg=msg+" ";
        }
        msg =msg + ">";
        queue.add(msg);
    }

    void getTracksCurrentMax() {
        String msg = "<jG ";
        for (int i = 0; i < 8; i++) {
            msg =msg + pref_dcc_ex_current_max;
            if (i<7) msg=msg+" ";
        }
        msg =msg + ">";
        queue.add(msg);
    }

    void setLocoSpeed(String locoStr, String speedStr, String dirStr) {
        int loco = Integer.valueOf(locoStr);
        int speed = Integer.valueOf(speedStr);
        int dir = Integer.valueOf(dirStr);

        String msg = "<l " + loco + " 1 " + getSpeedByteFromSpeed(speed, dir ) + " 0>";
        queue.add(msg);
    }

    void processIncomingMessage(String thisLine) {
        if (thisLine.equals("<s>")) {
            queue.add("<iDCCEX v-" + pref_dcc_ex_version
                    + " / " + pref_dcc_ex_board
                    + " / " + pref_dcc_ex_motor_shield + ">");

        } else if (thisLine.equals("<#>")) { // heartbeat
            queue.add("<#>");

        } else if (thisLine.equals("<R>")) { // read loco address
            queue.add("<r " + pref_loco_address + ">");

        } else if ((thisLine.charAt(1) == 'R') && (line.length() > 3)) {   //CV read request
            int cv = Integer.valueOf(thisLine.substring(3, thisLine.length() - 1));
            queue.add("<r " + cv + " " + cvValues[cv] + ">");

        } else if ((thisLine.charAt(1) == 'W') && (line.length() > 3)) {   //CV read request
            String[] params = thisLine.substring(3, thisLine.length() - 1).split(" ");
            int cv = Integer.valueOf(params[0]);
            int cvValue = Integer.valueOf(params[1]);
            cvValues[cv] = cvValue;
            queue.add("<r " + cv + " " + cvValues[cv] + ">");

        } else if (thisLine.equals("<0>")) { // power
            setPower(TRACK_POWER_BOTH, false);
        } else if (thisLine.equals("<1>")) { // power
            setPower(TRACK_POWER_BOTH, true);

        } else if (((thisLine.charAt(1) == '0') || (thisLine.charAt(1) == '1')) // power
                && (line.length() > 3)) {
            String[] params = thisLine.substring(1, thisLine.length() - 1).split(" ");
            setPower(params[1], (params[0].equals("0") ? false : true));

        } else if (thisLine.equals("<=>")) {   // get track modes
            getTrackModes();

        } else if ( (thisLine.charAt(1)=='=') && (line.length() > 3)) {   // set track mode
            String[] params = thisLine.substring(3, thisLine.length() - 1).split(" ");
            int trackNo = params[0].charAt(0) - 'A';
            int trackAddress = -1;
            if (params.length>2) trackAddress = Integer.parseInt(params[2]);
            setTrackMode(trackNo, params[1], trackAddress);

        } else if (thisLine.equals("<U DISCONNECT>")) {   // get track modes
            closeConnections();
            openConnections();

        } else if ( (thisLine.equals("<JI>")) || (thisLine.equals("<J I>")) ) {   // get track currents
            getTracksCurrent();

        } else if ( (thisLine.equals("<JG>")) || (thisLine.equals("<J G>")) ) {   // get track currents max
            getTracksCurrentMax();

        } else if ( (thisLine.charAt(1)=='t') && (line.length() > 3)) {   // loco
            String[] params = thisLine.substring(3, thisLine.length() - 1).split(" ");
            if (params.length>1) { // set speed
                setLocoSpeed(params[0], params[1], params[1]);
            } else { //request update
            }

        } else {
            Log.d("EX-TestCS", "serverThread: Unknown command: " + line);
        }

    }
}