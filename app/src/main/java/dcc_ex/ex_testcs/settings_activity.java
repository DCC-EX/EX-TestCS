package dcc_ex.ex_testcs;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class settings_activity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        //If you want to insert data in your settings
//        <YourSettingsFragmentClass> settingsFragment = new <YourSettingsFragmentClass>();
//        settingsFragment. ...
//        getSupportFragmentManager().beginTransaction().replace(R.id.<YourFrameLayout>,settingsFragment).commit();
        //Else
        getSupportFragmentManager().beginTransaction().replace(R.id.fl_settings,new settings_fragment()).commit();
    }
}
