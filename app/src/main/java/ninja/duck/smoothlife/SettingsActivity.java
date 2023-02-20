package ninja.duck.smoothlife;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SLPreferences";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
             Log.d(TAG, "Creating preferences");
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

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