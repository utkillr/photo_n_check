package nosikov_k.photoncheck;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Spinner;


public class SetActivity extends ActionBarActivity {

    public static final String APP_PREFERENCES = "mySettings";
    private SharedPreferences mySettings;
    public int Flashlight = 0, GeoPosition = 0, Effect = 0;
    public final String FLASHLIGHT = "fl", GEOPOSITION = "gp", EFFECT = "eff";
    Spinner GeoPositionSpin, FlashlightSpin, EffectSpin;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set);
        GeoPositionSpin = (Spinner)findViewById(R.id.spinnerGeoPosition);
        FlashlightSpin = (Spinner)findViewById(R.id.spinnerFlashlight);
        EffectSpin = (Spinner)findViewById(R.id.spinnerEffect);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mySettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);

        if (mySettings.contains(FLASHLIGHT)) {
            Flashlight = mySettings.getInt(FLASHLIGHT, 0);
            FlashlightSpin.setSelection(Flashlight);
        }
        if (mySettings.contains(GEOPOSITION)) {
            GeoPosition = mySettings.getInt(GEOPOSITION, 0);
            GeoPositionSpin.setSelection(GeoPosition);
        }
        if (mySettings.contains(EFFECT)) {
            Effect = mySettings.getInt(EFFECT, 0);
            EffectSpin.setSelection(Effect);
        }
    }

    public void onSaveButtonClick(View view) {
        int GP = GeoPositionSpin.getSelectedItemPosition();
        int FL = FlashlightSpin.getSelectedItemPosition();
        int EFF = EffectSpin.getSelectedItemPosition();

        SharedPreferences.Editor editor = mySettings.edit();
        editor.putInt(FLASHLIGHT, FL);
        editor.putInt(GEOPOSITION, GP);
        editor.putInt(EFFECT, EFF);
        editor.apply();

        Intent setIntent = new Intent();
        setResult(RESULT_OK, setIntent);
        finish();
    }
}
