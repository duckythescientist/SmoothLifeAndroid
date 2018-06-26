package ninja.duck.smoothlife;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;



public class SLPreferencesActivity extends Activity {

    private static final String TAG = "SLPreferences";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
        Log.d(TAG, "Creating preferences");

    }

    public static class SettingsFragment extends PreferenceFragment {

//        SeekBarPreference seek;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
//
//            seek = (SeekBarPreference)findPreference("color_scaling");
//            seek.setSummary(seek_format(prefs.getInt("color_scaling", 50)));
//            seek.setTitle("Fuck");

            addResetListener();

            Preference about_button = findPreference("about_page");
            about_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getActivity(), AboutPage.class);
                    startActivity(intent);
                    return true;
                }
            });


        }


        // https://stackoverflow.com/a/24648780/2293508
        void addResetListener() {
            Preference reset_button = findPreference("reset_defaults");
            reset_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    resetSettings();
                    return true;
                };
            });
        }

        // https://stackoverflow.com/a/24648780/2293508
        void resetSettings() {
            Log.d(TAG, "Resetting preferences");
            PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .edit().clear().commit();
            PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, true);
            getPreferenceScreen().removeAll();
            addPreferencesFromResource(R.xml.preferences);
            Toast toast = Toast.makeText(getActivity(), "Reset all preferences", Toast.LENGTH_SHORT);
            toast.show();
            addResetListener();
        }


    }

}
