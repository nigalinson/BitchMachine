package com.sloth.www.green.ui;

import android.os.Bundle;
import com.sloth.www.green.R;
import com.sloth.www.green.client.IGreenHelper;
import com.sloth.www.green.client.impl.GreenHelper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;

public class SettingsActivity extends AppCompatActivity {

    private static IGreenHelper mGreenHelper = new GreenHelper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGreenHelper.register(this);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
    }

    @Override
    protected void onDestroy() {
        mGreenHelper.unRegister(this);
        super.onDestroy();
    }

    public static class SettingsFragment
            extends PreferenceFragmentCompat
            implements Preference.OnPreferenceChangeListener,
            Preference.OnPreferenceClickListener  {

        private SwitchPreferenceCompat general;

        private SwitchPreferenceCompat resume;
        private SwitchPreferenceCompat alarm;
        private EditTextPreference attention;
        private EditTextPreference success;
        private EditTextPreference failed;
        private SeekBarPreference delay;
        private SwitchPreferenceCompat rollBack;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            initPreference();
        }

        private void initPreference() {
            general = findPreference("general");
            resume = findPreference("resume");
            alarm = findPreference("alarm");
            attention = findPreference("attention");
            success = findPreference("success");
            failed = findPreference("failed");
            delay = findPreference("delay");
            rollBack = findPreference("roll_back");

            if (general != null) {
                general.setOnPreferenceChangeListener(this);
                general.setOnPreferenceClickListener(this);
            }
            if (resume != null) {
                resume.setOnPreferenceChangeListener(this);
            }
            if (alarm != null) {
                alarm.setOnPreferenceChangeListener(this);
            }
            if (attention != null) {
                attention.setOnPreferenceChangeListener(this);
            }
            if (success != null) {
                success.setOnPreferenceChangeListener(this);
            }
            if (failed != null) {
                failed.setOnPreferenceChangeListener(this);
            }

            if (delay != null) {
                delay.setOnPreferenceChangeListener(this);
                delay.setUpdatesContinuously(true);
            }
            if (rollBack != null) {
                rollBack.setOnPreferenceChangeListener(this);
            }

        }

        @Override
        public void onResume() {
            super.onResume();
            if(getActivity() != null && general != null){
                boolean isServiceRunning = mGreenHelper.isServiceRunning(getActivity());
                general.setChecked(isServiceRunning);
                showDetail(isServiceRunning);

                boolean uiVoiceState = alarm.isChecked();
                showVoice(isServiceRunning&uiVoiceState);

                setDelayLabel(delay.getValue());

                initToService();
            }
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if ("general".equals(preference.getKey())) { //总开关
                handleGeneral((boolean) newValue);
            } else if ("resume".equals(preference.getKey())) {
                handleResume((boolean) newValue);
            } else if ("alarm".equals(preference.getKey())) {
                handleAlarm((boolean) newValue);
            } else if ("attention".equals(preference.getKey())) {
                handleMsg();
            } else if ("success".equals(preference.getKey())) {
                handleMsg();
            } else if ("failed".equals(preference.getKey())) {
                handleMsg();
            } else if ("delay".equals(preference.getKey())) {
                handleDelay((int)newValue);
            } else if ("roll_back".equals(preference.getKey())) {
                handleRollBack((boolean)newValue);
            } else { }
            return true;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            if ("general".equals(preference.getKey())) { //总开关
                mGreenHelper.openAndCloseService(getActivity());
            }
            return false;
        }

        private void handleGeneral(boolean value) {
            showDetail(value);
        }

        private void handleResume(boolean newValue) {
            if(newValue){
                mGreenHelper.resume(getActivity());
            }else{
                mGreenHelper.pause(getActivity());
            }
        }

        private void handleAlarm(boolean newValue) {
            showVoice(newValue);
            mGreenHelper.voiceAlarm(getActivity(), newValue);
        }

        private void handleMsg() {
            mGreenHelper.voiceAlarm(getActivity(), attention.getText(), success.getText(), failed.getText());
        }

        private void handleDelay(int newValue) {
            setDelayLabel(newValue);
            mGreenHelper.delay(getActivity(), newValue);
        }

        private void handleRollBack(boolean newValue) {
            mGreenHelper.autoRollBackToChatList(getActivity(), newValue);
        }

        private void showDetail(boolean value) {
            resume.setVisible(value);
            alarm.setVisible(value);
            delay.setVisible(value);
            rollBack.setVisible(value);
        }

        private void showVoice(boolean value) {
            attention.setVisible(value);
            success.setVisible(value);
            failed.setVisible(value);
        }

        private void setDelayLabel(int value) {
            delay.setSummary( value+ "毫秒");
        }

        private void initToService() {
            handleGeneral(general.isChecked());
            handleResume(resume.isChecked());
            handleAlarm(alarm.isChecked());
            handleMsg();
            handleDelay(delay.getValue());
            handleRollBack(rollBack.isChecked());
        }
    }
}