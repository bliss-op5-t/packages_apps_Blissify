/*
 * Copyright (C) 2016-2019 crDroid Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blissroms.blissify.fragments.statusbar;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.Menu;
import android.widget.EditText;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.SettingsPreferenceFragment;

import com.android.settings.R;
import com.bliss.support.preferences.CustomSeekBarPreference;
import com.bliss.support.preferences.SystemSettingListPreference;
import com.bliss.support.colorpicker.ColorPickerPreference;

import java.util.Date;

import lineageos.preference.LineageSystemSettingListPreference;
import lineageos.providers.LineageSettings;

public class Clock extends SettingsPreferenceFragment
            implements Preference.OnPreferenceChangeListener  {

    private static final String TAG = "Clock";

    private static final String STATUS_BAR_AM_PM = "status_bar_am_pm";
    private static final String CLOCK_DATE_DISPLAY = "status_bar_clock_date_display";
    private static final String CLOCK_DATE_POSITION = "status_bar_clock_date_position";
    private static final String CLOCK_DATE_STYLE = "status_bar_clock_date_style";
    private static final String CLOCK_DATE_FORMAT = "status_bar_clock_date_format";
    private static final String CLOCK_DATE_AUTO_HIDE_HDUR = "status_bar_clock_auto_hide_hduration";
    private static final String CLOCK_DATE_AUTO_HIDE_SDUR = "status_bar_clock_auto_hide_sduration";
    private static final String STATUS_BAR_CLOCK_COLOR = "status_bar_clock_color";
    private static final String STATUS_BAR_CLOCK_SIZE  = "status_bar_clock_size";
    private static final String STATUS_BAR_CLOCK_FONT_STYLE  = "status_bar_clock_font_style";

    static final int DEFAULT_STATUS_CLOCK_COLOR = 0xffffffff;
    private static final int CLOCK_DATE_STYLE_LOWERCASE = 1;
    private static final int CLOCK_DATE_STYLE_UPPERCASE = 2;
    private static final int CUSTOM_CLOCK_DATE_FORMAT_INDEX = 18;

    private CustomSeekBarPreference mHideDuration;
    private CustomSeekBarPreference mShowDuration;
    private LineageSystemSettingListPreference mStatusBarAmPm;
    private SystemSettingListPreference mClockDateDisplay;
    private SystemSettingListPreference mClockDatePosition;
    private SystemSettingListPreference mClockDateStyle;
    private ListPreference mClockDateFormat;
    private ColorPickerPreference mClockColor;
    private CustomSeekBarPreference mClockSize;
    private ListPreference mClockFontStyle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.status_bar_clock);

        ContentResolver resolver = getActivity().getContentResolver();

        int intColor;
        String hexColor;

        mHideDuration = (CustomSeekBarPreference) findPreference(CLOCK_DATE_AUTO_HIDE_HDUR);
        int hideVal = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_CLOCK_AUTO_HIDE_HDURATION, 60, UserHandle.USER_CURRENT);
        mHideDuration.setValue(hideVal);
        mHideDuration.setOnPreferenceChangeListener(this);

        mShowDuration = (CustomSeekBarPreference) findPreference(CLOCK_DATE_AUTO_HIDE_SDUR);
        int showVal = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_CLOCK_AUTO_HIDE_SDURATION, 5, UserHandle.USER_CURRENT);
        mShowDuration.setValue(showVal);
        mShowDuration.setOnPreferenceChangeListener(this);

        mStatusBarAmPm =
                (LineageSystemSettingListPreference) findPreference(STATUS_BAR_AM_PM);

        if (DateFormat.is24HourFormat(getActivity())) {
            mStatusBarAmPm.setEnabled(false);
            mStatusBarAmPm.setSummary(R.string.status_bar_am_pm_info);
        }

        int dateDisplay = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_CLOCK_DATE_DISPLAY, 0, UserHandle.USER_CURRENT);

        mClockDateDisplay = (SystemSettingListPreference) findPreference(CLOCK_DATE_DISPLAY);
        mClockDateDisplay.setOnPreferenceChangeListener(this);

        mClockDatePosition = (SystemSettingListPreference) findPreference(CLOCK_DATE_POSITION);
        mClockDatePosition.setEnabled(dateDisplay > 0);
        mClockDatePosition.setOnPreferenceChangeListener(this);

        mClockDateStyle = (SystemSettingListPreference) findPreference(CLOCK_DATE_STYLE);
        mClockDateStyle.setEnabled(dateDisplay > 0);
        mClockDateStyle.setOnPreferenceChangeListener(this);

        mClockDateFormat = (ListPreference) findPreference(CLOCK_DATE_FORMAT);
        if (mClockDateFormat.getValue() == null) {
            mClockDateFormat.setValue("EEE");
        }
        parseClockDateFormats();
        mClockDateFormat.setEnabled(dateDisplay > 0);
        mClockDateFormat.setOnPreferenceChangeListener(this);

        mClockColor = (ColorPickerPreference) findPreference(STATUS_BAR_CLOCK_COLOR);
        mClockColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_CLOCK_COLOR, DEFAULT_STATUS_CLOCK_COLOR);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mClockColor.setSummary(hexColor);
        mClockColor.setNewPreviewColor(intColor);

        mClockSize = (CustomSeekBarPreference) findPreference(STATUS_BAR_CLOCK_SIZE);
        int clockSize = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_CLOCK_SIZE, 14);
        mClockSize.setValue(clockSize / 1);
        mClockSize.setOnPreferenceChangeListener(this);

        mClockFontStyle = (ListPreference) findPreference(STATUS_BAR_CLOCK_FONT_STYLE);
        int showClockFont = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_CLOCK_FONT_STYLE, 28);
        mClockFontStyle.setValue(String.valueOf(showClockFont));
        mClockFontStyle.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      AlertDialog dialog;
      ContentResolver resolver = getActivity().getContentResolver();
      if (preference == mHideDuration) {
            int value = (Integer) newValue;
            Settings.System.putIntForUser(resolver,
                    Settings.System.STATUS_BAR_CLOCK_AUTO_HIDE_HDURATION, value, UserHandle.USER_CURRENT);
            return true;
      } else if (preference == mShowDuration) {
            int value = (Integer) newValue;
            Settings.System.putIntForUser(resolver,
                    Settings.System.STATUS_BAR_CLOCK_AUTO_HIDE_SDURATION, value, UserHandle.USER_CURRENT);
            return true;
      } else if (preference == mClockDateDisplay) {
          int val = Integer.parseInt((String) newValue);
          if (val == 0) {
              mClockDatePosition.setEnabled(false);
              mClockDateStyle.setEnabled(false);
              mClockDateFormat.setEnabled(false);
          } else {
              mClockDatePosition.setEnabled(true);
              mClockDateStyle.setEnabled(true);
              mClockDateFormat.setEnabled(true);
          }
          return true;
      } else if (preference == mClockDatePosition) {
            parseClockDateFormats();
            return true;
      } else if (preference == mClockDateStyle) {
          parseClockDateFormats();
          return true;
      } else if (preference == mClockColor) {
          String hex = ColorPickerPreference.convertToARGB(
                  Integer.valueOf(String.valueOf(newValue)));
          preference.setSummary(hex);
          int intHex = ColorPickerPreference.convertToColorInt(hex);
          Settings.System.putInt(resolver,
                Settings.System.STATUS_BAR_CLOCK_COLOR, intHex);
          return true;
      }  else if (preference == mClockSize) {
          int width = ((Integer)newValue).intValue();
          Settings.System.putInt(resolver,
            Settings.System.STATUS_BAR_CLOCK_SIZE, width);
          return true;
      }  else if (preference == mClockFontStyle) {
          int showClockFont = Integer.valueOf((String) newValue);
          int index = mClockFontStyle.findIndexOfValue((String) newValue);
          Settings.System.putInt(resolver, Settings.System.
              STATUS_BAR_CLOCK_FONT_STYLE, showClockFont);
          mClockFontStyle.setSummary(mClockFontStyle.getEntries()[index]);
          return true;
      } else if (preference == mClockDateFormat) {
          int index = mClockDateFormat.findIndexOfValue((String) newValue);

          if (index == CUSTOM_CLOCK_DATE_FORMAT_INDEX) {
              AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
              alert.setTitle(R.string.status_bar_date_string_edittext_title);
              alert.setMessage(R.string.status_bar_date_string_edittext_summary);

              final EditText input = new EditText(getActivity());
              String oldText = Settings.System.getString(
                  resolver,
                  Settings.System.STATUS_BAR_CLOCK_DATE_FORMAT);
              if (oldText != null) {
                  input.setText(oldText);
              }
              alert.setView(input);

              alert.setPositiveButton(R.string.menu_save, new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialogInterface, int whichButton) {
                      String value = input.getText().toString();
                      if (value.equals("")) {
                          return;
                      }
                      Settings.System.putString(resolver,
                          Settings.System.STATUS_BAR_CLOCK_DATE_FORMAT, value);

                      return;
                  }
              });

              alert.setNegativeButton(R.string.menu_cancel,
                  new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialogInterface, int which) {
                      return;
                  }
              });
              dialog = alert.create();
              dialog.show();
          } else {
              if ((String) newValue != null) {
                  Settings.System.putString(resolver,
                      Settings.System.STATUS_BAR_CLOCK_DATE_FORMAT, (String) newValue);
              }
          }
          return true;
      }
      return false;
    }

    private void parseClockDateFormats() {
        String[] dateEntries = getResources().getStringArray(
                R.array.status_bar_date_format_entries_values);
        CharSequence parsedDateEntries[];
        parsedDateEntries = new String[dateEntries.length];
        Date now = new Date();

        int lastEntry = dateEntries.length - 1;
        int dateFormat = Settings.System.getIntForUser(getActivity()
                .getContentResolver(), Settings.System.STATUS_BAR_CLOCK_DATE_STYLE, 0, UserHandle.USER_CURRENT);
        for (int i = 0; i < dateEntries.length; i++) {
            if (i == lastEntry) {
                parsedDateEntries[i] = dateEntries[i];
            } else {
                String newDate;
                CharSequence dateString = DateFormat.format(dateEntries[i], now);
                if (dateFormat == CLOCK_DATE_STYLE_LOWERCASE) {
                    newDate = dateString.toString().toLowerCase();
                } else if (dateFormat == CLOCK_DATE_STYLE_UPPERCASE) {
                    newDate = dateString.toString().toUpperCase();
                } else {
                    newDate = dateString.toString();
                }

                parsedDateEntries[i] = newDate;
            }
        }
        mClockDateFormat.setEntries(parsedDateEntries);
    }

    public static void reset(Context mContext) {
        ContentResolver resolver = mContext.getContentResolver();

        Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_CLOCK_AUTO_HIDE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_CLOCK_AUTO_HIDE_HDURATION, 60, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_CLOCK_AUTO_HIDE_SDURATION, 5, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_CLOCK_DATE_DISPLAY, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_CLOCK_DATE_POSITION, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_CLOCK_DATE_STYLE, 0, UserHandle.USER_CURRENT);
        Settings.System.putString(resolver,
                Settings.System.STATUS_BAR_CLOCK_DATE_FORMAT, "");
        Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_CLOCK_SECONDS, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_CLOCK_COLOR, 0xffffffff, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_CLOCK_SIZE, 14, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_CLOCK_FONT_STYLE, 28, UserHandle.USER_CURRENT);

        LineageSettings.System.putIntForUser(resolver,
                LineageSettings.System.STATUS_BAR_AM_PM, 0, UserHandle.USER_CURRENT);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.BLISSIFY;
    }
}
