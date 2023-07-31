package dcc_ex.ex_testcs.util;

import dcc_ex.ex_testcs.ex_testcs;

public class Loco {

    public static final int DIRECTION_FORWARD = 1;
    public static final int DIRECTION_REVERSE = 0;

    static final int FUNCTION_TYPE_LATCHING = 0;
    static final int FUNCTION_TYPE_NONLATCHING = 1;


    public int id;
    public String idStr;
    public String name;
    public int speed;
    public int direction;
    public String [] functionLabels;
    public int [] functionTypes;
    public int [] functionStates;

    public ex_testcs mainapp;

    public Loco(ex_testcs myApp, int discoveredId) {
        mainapp = myApp;

        this.id = discoveredId;
        this.idStr = String.valueOf(discoveredId);
        this.name = "";
        functionLabels = new String[28];
        functionTypes = new int[28];
        functionStates = new int[28];
        for (int i = 0; i < 28; i++) {
            functionTypes[i] = FUNCTION_TYPE_LATCHING;
            functionLabels[i] = "";
            functionStates[i] = 0;
        }

        this.speed = 0;
        this.direction = DIRECTION_FORWARD;
    }

    public Loco(ex_testcs myApp, String myAutomationLine) {
        mainapp = myApp;

        if ( (myAutomationLine.length()>7) && (myAutomationLine.substring(0, 7).equals("ROSTER("))) {
            //ROSTER( loco, name, func_map )
            this.idStr = myAutomationLine.substring(7, myAutomationLine.indexOf(","));
            this.id = Integer.parseInt(this.idStr);
            String restOfLine = myAutomationLine.substring(myAutomationLine.indexOf(",") + 1);
            String[] a = restOfLine.split("\",\"");
            String[] aa = a[0].split("\"");
            String[] ab = a[1].split("\"");
            this.name = aa[1];
            String[] functions = ab[0].split("/");
            if ( ( ab[0].length()>0) && (functions.length > 0) ) {
                functionLabels = new String[functions.length];
                functionTypes = new int[functions.length];
                functionStates = new int[functions.length];
                for (int i = 0; i < functions.length; i++) {
                    functionTypes[i] = FUNCTION_TYPE_LATCHING;
                    if (functions[i].length() > 0) {
                        if (functions[i].charAt(0) != '*') {
                            functionLabels[i] = functions[i];
                        } else {
                            functionLabels[i] = functions[i].substring(1);
                            functionTypes[i] = FUNCTION_TYPE_NONLATCHING;
                        }
                    }
                    functionStates[i] = 0;
                }
            } else {
                functionLabels = null;
                functionTypes = null;
                functionStates = null;
            }
            this.speed = 0;
            this.direction = DIRECTION_FORWARD;
        }
    }

    String getFunctionString() {
        String rslt = "";
        if (this.functionLabels != null) {
            if (this.functionLabels.length>0) {
                for (int i = 0; i < this.functionLabels.length; i++) {
                    if (this.functionTypes[i] == FUNCTION_TYPE_NONLATCHING) {
                        rslt = rslt + "*";
                    }
                    rslt = rslt + this.functionLabels[i];
                    if (i < this.functionLabels[i].length() - 1) {
                        rslt = rslt + "/";
                    }
                }
            }
        }
        return rslt;
    }

    public String getLocoEntry() {
        String rslt = "<jR " + this.idStr + " \"" + this.name +"\" \"" + getFunctionString() + "\">";
        return rslt;
    }

    public String getSpeed() {
        String msg = "<l " + this.idStr + " 1 " + mainapp.getSpeedByteFromSpeed(this.speed, this.direction ) + " " + getFunctionMap() + ">";
        return msg;
    }

    public String setSpeedAndDirection(int newSpeed, int newDirection) {
        String rslt = "";
        if ( (this.speed != newSpeed)  || (this.direction != newDirection) ) {
            this.speed = newSpeed;
            this.direction = newDirection;
            rslt = "<l " + this.idStr + " 1 " + mainapp.getSpeedByteFromSpeed(this.speed, this.direction ) + " " + getFunctionMap() + ">";
        }
        return rslt;
    }

    public String setSpeed(int newSpeed) {
        String rslt = "";
        if (this.speed != newSpeed) {
            this.speed = newSpeed;
            rslt = "<l " + this.idStr + " 1 " + mainapp.getSpeedByteFromSpeed(this.speed, this.direction ) + " " + getFunctionMap() + ">";
        }
        return rslt;
    }

    public int getDirection() {
        return this.direction;
    }

    public String setDirection(int newDirection) {
        String rslt = "";
        if (this.direction != newDirection) {
            this.direction = newDirection;
        }
        return rslt;
    }

    public int getSpeedByte() {
        int rslt = mainapp.getSpeedByteFromSpeed(this.speed, this.direction);
        return rslt;
    }

    public int getFunctionMap() {
        int rslt = 0;
        if (this.functionLabels!=null) {
            if (this.functionLabels.length > 0) {
                for (int i = 0; i < this.functionLabels.length; i++) {
                    if (functionStates[i] == 1) {
                        rslt = mainapp.setBit(rslt, i+1, true);
                    }
                }
            }
        }
        return rslt;
    }

    public String setFunctionState(int functionNo, int functionState) {
        String rslt ="";
        if (this.functionLabels!=null) {
            if (this.functionLabels.length > 0) {
                if (functionNo < this.functionLabels.length) {
                    if (this.functionStates[functionNo] != functionState) {
                        this.functionStates[functionNo] = functionState;
                        rslt = getSpeed();
                    }
                }
            }
        }
        return rslt;
    }

}
