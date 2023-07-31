package dcc_ex.ex_testcs.util;

public class Turnout {

    public static final String TYPE_SERVO = "SERVO";
    public static final String TYPE_DCC = "DCC";
    public static final String TYPE_VPIN = "VPIN";
    public static final String TYPE_LCN = "LCN";

    static final int TYPE_NO_SERVO = 0;
    static final int TYPE_NO_DCC = 1;
    static final int TYPE_NO_VPIN = 2;
    static final int TYPE_NO_LCN = 3;

    public static final String STATE_UNKNOWN = "X";
    public static final String STATE_CLOSED = "0";
    public static final String STATE_THROWN = "1";

    public static final String ACTION_THROW = "T";
    public static final String ACTION_CLOSE = "C";



    public int id;
    public String idStr;
    public String name;
    public String type;
    public int typeNo;
    public String dccAddress;
    public String dccSubAddress;
    public String vpin;
    public String active_angle;
    public String inactive_angle;
    public String profile;
    public String state;

    public Turnout(String myAutomationLine) {

        if ((myAutomationLine.length() > 14) && (myAutomationLine.substring(0, 14).equals("SERVO_TURNOUT("))) {
            //SERVO_TURNOUT( id, pin, active_angle, inactive_angle, profile [, “description”] )
            //               0   1    2             3               4           5
            String restOfLine = myAutomationLine.trim().substring(14, myAutomationLine.length() - 1);
            String[] values = restOfLine.split(",");
            this.idStr = values[0];
            this.id = Integer.parseInt(idStr);
            this.name = values[5].trim();
            this.name = this.name.substring(1, this.name.length() - 1);
            this.type = TYPE_SERVO;
            this.typeNo = TYPE_NO_SERVO;
            this.vpin = values[1];
            this.active_angle = values[2];
            this.inactive_angle = values[3];
            this.profile = values[4];
            this.state = STATE_UNKNOWN;

        } else if ((myAutomationLine.length() > 8) && (myAutomationLine.substring(0, 8).equals("TURNOUT("))) {
            //TURNOUT( id, addr, sub_addr [, “description”] )
            //         0   1     2            3
            String restOfLine = myAutomationLine.trim().substring(8, myAutomationLine.length() - 1);
            String[] values = restOfLine.split(",");
            this.idStr = values[0];
            this.id = Integer.parseInt(idStr);
            this.name = values[3].trim();
            this.name = this.name.substring(1, this.name.length() - 1);
            this.type = TYPE_DCC;
            this.typeNo = TYPE_NO_DCC;
            this.dccAddress = values[1];
            this.dccSubAddress = values[2];
            this.state = STATE_UNKNOWN;

        } else if ((myAutomationLine.length() > 9) && (myAutomationLine.substring(0, 9).equals("TURNOUTL("))) {
            //TURNOUTL( id, addr [, “description”] )
            //         0   1         2
            String restOfLine = myAutomationLine.trim().substring(9, myAutomationLine.length() - 1);
            String[] values = restOfLine.split(",");
            this.idStr = values[0];
            this.id = Integer.parseInt(idStr);
            this.name = values[2].trim();
            this.name = this.name.substring(1, this.name.length() - 1);
            this.type = TYPE_LCN;
            this.typeNo = TYPE_NO_LCN;
            this.dccAddress = values[1];
            this.state = STATE_UNKNOWN;

        } else if ((myAutomationLine.length() > 12) && (myAutomationLine.substring(0, 12).equals("PIN_TURNOUT("))) {
            //PIN_TURNOUT( id, pin [, “description”] )
            //             0   1         2
            String restOfLine = myAutomationLine.trim().substring(12, myAutomationLine.length() - 1);
            String[] values = restOfLine.split(",");
            this.idStr = values[0];
            this.id = Integer.parseInt(idStr);
            this.name = values[2].trim();
            this.name = this.name.substring(1, this.name.length() - 1);
            this.type = TYPE_VPIN;
            this.typeNo = TYPE_NO_VPIN;
            this.state = STATE_UNKNOWN;

        } else if ((myAutomationLine.length() > 16) && (myAutomationLine.substring(0, 16).equals("VIRTUAL_TURNOUT("))) {
            //VIRTUAL_TURNOUT( id [, “description”] )
            //                 0      1
            String restOfLine = myAutomationLine.trim().substring(16, myAutomationLine.length() - 1);
            String[] values = restOfLine.split(",");
            this.idStr = values[0];
            this.id = Integer.parseInt(idStr);
            this.name = values[2].trim();
            this.name = this.name.substring(1, this.name.length() - 1);
            this.type = TYPE_DCC;
            this.typeNo = TYPE_NO_DCC;
            this.state = STATE_UNKNOWN;

        }
    }

    public String getTurnoutEntry() {
        String rslt = "<jT " + this.idStr + " " + this.state + " \"" + this.name + "\">";
        return rslt;
    }

    public String getTurnoutState() {
        return this.state;
    }

    public String throwTurnout() {
        this.state = STATE_THROWN;
        String rslt = "<H " + this.idStr + " " + this.state +">";
        return rslt;
    }

    public String closeTurnout() {
        this.state = STATE_CLOSED;
        String rslt = "<H " + this.idStr + " " + this.state +">";
        return rslt;
    }
}
