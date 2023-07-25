package dcc_ex.ex_testcs;

import android.content.Context;
import android.os.Environment;
import android.text.Html;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;


public class ReadMyAutomationH {

    public ex_testcs mainapp;

    public void readRoster(ex_testcs myApp, final Context context) throws IOException {
        mainapp = myApp;

        mainapp.msgHistory = "<i>Processing ROSTER - Start</i><br/>" + mainapp.msgHistory;

        File sdcard_path = Environment.getExternalStorageDirectory();
        File downloads_dir = new File(sdcard_path, "Download");
        if (downloads_dir.isDirectory()) {
            Log.d("EX-TestCS", "readMyAutomationH: readRoster - found Download");

            File myAutomationHfile = new File(sdcard_path, "Download/myAutomation.h");
            if (myAutomationHfile.exists()) {
                BufferedReader list_reader = new BufferedReader(new FileReader(myAutomationHfile));
                while (list_reader.ready()) {
                    String line = list_reader.readLine();
                    if ( (line.length()>7) && (line.substring(0, 7).equals("ROSTER("))) {
                        String locoAddressString = line.substring(7, line.indexOf(","));
                        int locoAddress = Integer.parseInt(locoAddressString);
                        String restOfLine = line.substring(line.indexOf(",")+1);
                        String[] a = restOfLine.split("\",\"");
                        String[] aa = a[0].split("\"");
                        String[] ab = a[1].split("\"");
                        String name = aa[1];
                        String functions = ab[0];

                        mainapp.rosterHashMap.put(locoAddress,name + "«»" + functions);
                        mainapp.msgHistory = "<i>" + line + "</i><br/>" + mainapp.msgHistory;

                    }
                }
                list_reader.close();
            } else {
                mainapp.msgHistory = "<i>  *** /Download/myAutomation.h  NOT FOUND! ***</i><br/>" + mainapp.msgHistory;
            }
        }
        mainapp.msgHistory = "<i>Processing ROSTER - End</i><br/>" + mainapp.msgHistory;
    }


    public void readTurnouts(ex_testcs myApp, final Context context) throws IOException {
        mainapp = myApp;

        mainapp.msgHistory = "<i>Processing TURNOUTS/POINTS - Start</i><br/>" + mainapp.msgHistory;

        File sdcard_path = Environment.getExternalStorageDirectory();
        File downloads_dir = new File(sdcard_path, "Download");
        if (downloads_dir.isDirectory()) {
            Log.d("EX-TestCS", "readMyAutomationH: readRoster - found Download");

            File myAutomationHfile = new File(sdcard_path, "Download/myAutomation.h");
            if (myAutomationHfile.exists()) {
                BufferedReader list_reader = new BufferedReader(new FileReader(myAutomationHfile));
                while (list_reader.ready()) {
                    String line = list_reader.readLine();
                    if ( (line.length()>14) && (line.substring(0, 14).equals("SERVO_TURNOUT("))) {
                        String restOfLine = line.trim().substring(14, line.length()-1);
                        String[] values = restOfLine.split(",");
                        String servoIdStr = values[0];
                        int servoId = Integer.parseInt(servoIdStr);
                        String servoName = values[6].trim();
                        servoName = servoName.substring(1, servoName.length()-1);

                        mainapp.servoHashMap.put(servoId,servoName);
                        mainapp.msgHistory = "<i>" + line + "</i><br/>" + mainapp.msgHistory;

                    }
                }
                list_reader.close();
            } else {
                mainapp.msgHistory = "<i>  *** /Download/myAutomation.h  NOT FOUND! ***</i><br/>" + mainapp.msgHistory;
            }
        }
        mainapp.msgHistory = "<i>Processing TURNOUTS/POINTS - End</i><br/>" + mainapp.msgHistory;
    }
}
