/*
 * Copyright (C) 2013 SlimRoms Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.beanstalk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.internal.util.beanstalk.DeviceUtils;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.Utils;

import com.android.internal.widget.LockPatternUtils;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import java.io.File;
import java.io.FileOutputStream;

public class LockscreenStyle extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener {

    private static final String TAG = "LockscreenStyle";

    private static final String KEY_LOCKSCREEN_COLORIZE_ICON =
            "lockscreen_colorize_icon";
    private static final String KEY_LOCKSCREEN_LOCK_ICON =
            "lockscreen_lock_icon";
    private static final String KEY_LOCKSCREEN_FRAME_COLOR =
            "lockscreen_frame_color";
    private static final String KEY_LOCKSCREEN_LOCK_COLOR =
            "lockscreen_lock_color";
    private static final String KEY_LOCKSCREEN_DOTS_COLOR =
            "lockscreen_dots_color";
    private static final String KEY_LOCKSCREEN_TARGETS_COLOR =
            "lockscreen_targets_color";
    private static final String KEY_LOCKSCREEN_MISC_COLOR =
            "lockscreen_misc_color";


    private String mDefault;

    private CheckBoxPreference mColorizeCustom;

    private ColorPickerPreference mFrameColor;
    private ColorPickerPreference mLockColor;
    private ColorPickerPreference mDotsColor;
    private ColorPickerPreference mTargetsColor;
    private ColorPickerPreference mMiscColor;

    private ListPreference mLockIcon;

    private boolean mCheckPreferences;

    private File mLockImage;

    private static final int MENU_RESET = Menu.FIRST;

    private static final int DLG_RESET = 0;
    private static final int REQUEST_PICK_LOCK_ICON = 100;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createCustomView();
    }

    private PreferenceScreen createCustomView() {
        mCheckPreferences = false;
        PreferenceScreen prefSet = getPreferenceScreen();
        if (prefSet != null) {
            prefSet.removeAll();
        }

        addPreferencesFromResource(R.xml.lockscreen_style);
        prefSet = getPreferenceScreen();

        // Set to string so we don't have to create multiple objects of it
        mDefault = getResources().getString(R.string.default_string);

        mLockImage = new File(getActivity().getFilesDir() + "/lock_icon.tmp");

        mLockIcon = (ListPreference)
                findPreference(KEY_LOCKSCREEN_LOCK_ICON);
        mLockIcon.setOnPreferenceChangeListener(this);

        mColorizeCustom = (CheckBoxPreference)
                findPreference(KEY_LOCKSCREEN_COLORIZE_ICON);
        mColorizeCustom.setChecked(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.LOCKSCREEN_COLORIZE_LOCK, 0) == 1);
        mColorizeCustom.setOnPreferenceChangeListener(this);

        mFrameColor = (ColorPickerPreference)
                findPreference(KEY_LOCKSCREEN_FRAME_COLOR);
        mFrameColor.setOnPreferenceChangeListener(this);
        int frameColor = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_FRAME_COLOR, -2);
        setPreferenceSummary(mFrameColor,
                getResources().getString(
                R.string.lockscreen_frame_color_summary), frameColor);
        mFrameColor.setNewPreviewColor(frameColor);

        mLockColor = (ColorPickerPreference)
                findPreference(KEY_LOCKSCREEN_LOCK_COLOR);
        mLockColor.setOnPreferenceChangeListener(this);
        int lockColor = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_LOCK_COLOR, -2);
        setPreferenceSummary(mLockColor,
                getResources().getString(
                R.string.lockscreen_lock_color_summary), lockColor);
        mLockColor.setNewPreviewColor(lockColor);

        mDotsColor = (ColorPickerPreference)
                findPreference(KEY_LOCKSCREEN_DOTS_COLOR);
        mDotsColor.setOnPreferenceChangeListener(this);
        int dotsColor = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_DOTS_COLOR, -2);
        setPreferenceSummary(mDotsColor,
                getResources().getString(
                R.string.lockscreen_dots_color_summary), dotsColor);
        mDotsColor.setNewPreviewColor(dotsColor);

        mTargetsColor = (ColorPickerPreference)
                findPreference(KEY_LOCKSCREEN_TARGETS_COLOR);
        mTargetsColor.setOnPreferenceChangeListener(this);
        int targetColor = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_TARGETS_COLOR, -2);
        setPreferenceSummary(mTargetsColor,
                getResources().getString(
                R.string.lockscreen_targets_color_summary), targetColor);
        mTargetsColor.setNewPreviewColor(targetColor);

        mMiscColor = (ColorPickerPreference)
                findPreference(KEY_LOCKSCREEN_MISC_COLOR);
        mMiscColor.setOnPreferenceChangeListener(this);
        int miscColor = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_MISC_COLOR, -2);
        setPreferenceSummary(mMiscColor,
                getResources().getString(
                R.string.lockscreen_misc_color_summary), miscColor);
        mMiscColor.setNewPreviewColor(miscColor);

        // No lock-slider is available
        boolean dotsDisabled = new LockPatternUtils(getActivity()).isSecure()
            && Settings.Secure.getInt(getContentResolver(),
            Settings.Secure.LOCK_BEFORE_UNLOCK, 0) == 0
            && Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.LOCK_SHAKE_TEMP_SECURE, 0) == 0;
        boolean imageExists = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.LOCKSCREEN_LOCK_ICON) != null;
        mDotsColor.setEnabled(!dotsDisabled);
        mLockIcon.setEnabled(!dotsDisabled);
        mColorizeCustom.setEnabled(!dotsDisabled && imageExists);
        // Tablets don't have the extended-widget lock icon
        if (DeviceUtils.isTablet(getActivity())) {
            mLockColor.setEnabled(!dotsDisabled);
        }

        updateLockSummary();

        setHasOptionsMenu(true);
        mCheckPreferences = true;
        return prefSet;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_PICK_LOCK_ICON) {

                if (mLockImage.length() == 0 || !mLockImage.exists()) {
                    Toast.makeText(getActivity(),
                            getResources().getString(R.string.shortcut_image_not_valid),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                File image = new File(getActivity().getFilesDir() + File.separator
                        + "lock_icon" + System.currentTimeMillis() + ".png");
                String path = image.getAbsolutePath();
                mLockImage.renameTo(image);
                image.setReadable(true, false);

                deleteLockIcon();  // Delete current icon if it exists before saving new.
                Settings.Secure.putString(getContentResolver(),
                        Settings.Secure.LOCKSCREEN_LOCK_ICON, path);

                mColorizeCustom.setEnabled(path != null);
            }
        } else {
            if (mLockImage.exists()) {
                mLockImage.delete();
            }
        }
        updateLockSummary();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_backup) // use the backup icon
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                showDialogInner(DLG_RESET);
                return true;
             default:
                return super.onContextItemSelected(item);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mLockIcon) {
            int indexOf = mLockIcon.findIndexOfValue(newValue.toString());
            if (indexOf == 0) {
                requestLockImage();
            } else if (indexOf == 1) {
                deleteLockIcon();
                resizeBsLock();
                updateLockSummary();
            } else if (indexOf == 2) {
                deleteLockIcon();
                resizeBsLockone();
                updateLockSummary();
            } else if (indexOf == 3) {
                deleteLockIcon();
                resizeBsLocktwo();
                updateLockSummary();
            } else if (indexOf == 4) {
                deleteLockIcon();
                resizeBsLockthree();
                updateLockSummary();
            } else if (indexOf == 5) {
                deleteLockIcon();
                resizeBsLockfour();
                updateLockSummary();
            } else if (indexOf == 6) {
                deleteLockIcon();
                resizeBsLockfive();
                updateLockSummary();
            } else if (indexOf == 7) {
                deleteLockIcon();
                resizeBsLocksix();
                updateLockSummary();
            } else if (indexOf == 8) {
                deleteLockIcon();
                resizeBsLockseven();
                updateLockSummary();
            } else if (indexOf == 9) {
                deleteLockIcon();
                resizeBsLockeight();
                updateLockSummary();
            } else if (indexOf == 10) {
                deleteLockIcon();
                resizeBsLocknine();
                updateLockSummary();
            } else if (indexOf == 11) {
                deleteLockIcon();
                resizeBsLockten();
                updateLockSummary();
	    } else if (indexOf == 12) {
                deleteLockIcon();
                resizeBsLockeleven();
                updateLockSummary();
	    } else if (indexOf == 13) {
                deleteLockIcon();
                resizeBsLocktwelve();
                updateLockSummary();	
            } else if (indexOf == 14) {
                deleteLockIcon();
                updateLockSummary();
            }
            return true;
        } else if (preference == mColorizeCustom) {
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_COLORIZE_LOCK,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mFrameColor) {
            int val = Integer.valueOf(String.valueOf(newValue));
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_FRAME_COLOR, val);
            setPreferenceSummary(preference,
                    getResources().getString(R.string.lockscreen_frame_color_summary), val);
            return true;
        } else if (preference == mLockColor) {
            int val = Integer.valueOf(String.valueOf(newValue));
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_LOCK_COLOR, val);
            setPreferenceSummary(preference,
                    getResources().getString(R.string.lockscreen_lock_color_summary), val);
            return true;
        } else if (preference == mDotsColor) {
            int val = Integer.valueOf(String.valueOf(newValue));
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_DOTS_COLOR, val);
            setPreferenceSummary(preference,
                    getResources().getString(R.string.lockscreen_dots_color_summary), val);
            return true;
        } else if (preference == mTargetsColor) {
            int val = Integer.valueOf(String.valueOf(newValue));
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_TARGETS_COLOR, val);
            setPreferenceSummary(preference,
                    getResources().getString(R.string.lockscreen_targets_color_summary), val);
            return true;
        } else if (preference == mMiscColor) {
            int val = Integer.valueOf(String.valueOf(newValue));
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_MISC_COLOR, val);
            setPreferenceSummary(preference,
                    getResources().getString(R.string.lockscreen_misc_color_summary), val);
            return true;
        }
        return false;
    }

    private void setPreferenceSummary(
            Preference preference, String defaultSummary, int value) {
        if (value == -2) {
            preference.setSummary(defaultSummary + " (" + mDefault + ")");
        } else {
            String hexColor = String.format("#%08x", (0xffffffff & value));
            preference.setSummary(defaultSummary + " (" + hexColor + ")");
        }
    }

    private void updateLockSummary() {
        int resId;
        String value = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.LOCKSCREEN_LOCK_ICON);
        if (value == null) {
            resId = R.string.lockscreen_lock_icon_default;
            mLockIcon.setValueIndex(14);
        } else if (value.contains("bs_lock")) {
            resId = R.string.lockscreen_lock_icon_bs;
            mLockIcon.setValueIndex(1);
        } else if (value.contains("one")) {
            resId = R.string.lockscreen_lock_icon_bs_one;
            mLockIcon.setValueIndex(2);
        } else if (value.contains("two")) {
            resId = R.string.lockscreen_lock_icon_bs_two;
            mLockIcon.setValueIndex(3);
        } else if (value.contains("three")) {
            resId = R.string.lockscreen_lock_icon_bs_three;
            mLockIcon.setValueIndex(4);
        } else if (value.contains("four")) {
            resId = R.string.lockscreen_lock_icon_bs_four;
            mLockIcon.setValueIndex(5);
        } else if (value.contains("five")) {
            resId = R.string.lockscreen_lock_icon_bs_five;
            mLockIcon.setValueIndex(6);
        } else if (value.contains("six")) {
            resId = R.string.lockscreen_lock_icon_bs_six;
            mLockIcon.setValueIndex(7);
        } else if (value.contains("seven")) {
            resId = R.string.lockscreen_lock_icon_bs_seven;
            mLockIcon.setValueIndex(8);
        } else if (value.contains("eight")) {
            resId = R.string.lockscreen_lock_icon_bs_eight;
            mLockIcon.setValueIndex(9);
        } else if (value.contains("nine")) {
            resId = R.string.lockscreen_lock_icon_bs_nine;
            mLockIcon.setValueIndex(10);
        } else if (value.contains("ten")) {
            resId = R.string.lockscreen_lock_icon_bs_ten;
            mLockIcon.setValueIndex(11);
		} else if (value.contains("eleven")) {
            resId = R.string.lockscreen_lock_icon_bs_eleven;
            mLockIcon.setValueIndex(12);
		} else if (value.contains("twelve")) {
            resId = R.string.lockscreen_lock_icon_bs_twelve;
            mLockIcon.setValueIndex(13);
        } else {
            resId = R.string.lockscreen_lock_icon_custom;
            mLockIcon.setValueIndex(0);
        }
        mLockIcon.setSummary(getResources().getString(resId));
    }

    private void requestLockImage() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        int px = requestImageSize();

        intent.setType("image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", px);
        intent.putExtra("aspectY", px);
        intent.putExtra("outputX", px);
        intent.putExtra("outputY", px);
        intent.putExtra("scale", true);
        intent.putExtra("scaleUpIfNeeded", false);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());

        try {
            mLockImage.createNewFile();
            mLockImage.setWritable(true, false);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mLockImage));
            startActivityForResult(intent, REQUEST_PICK_LOCK_ICON);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void deleteLockIcon() {
        String path = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.LOCKSCREEN_LOCK_ICON);

        if (path != null) {
            File f = new File(path);
            if (f != null && f.exists()) {
                f.delete();
            }
        }

        Settings.Secure.putString(getContentResolver(),
                Settings.Secure.LOCKSCREEN_LOCK_ICON, null);

        mColorizeCustom.setEnabled(false);
        updateLockSummary();
    }
	
	private void resizeBsLocktwelve() {
        Bitmap bsLocktwelve = BitmapFactory.decodeResource(getResources(), R.drawable.bs_lock_twelve);
        if (bsLocktwelve != null) {
            String path = null;
            int px = requestImageSize();
            bsLocktwelve = Bitmap.createScaledBitmap(bsLocktwelve, px, px, true);
            try {
                mLockImage.createNewFile();
                mLockImage.setWritable(true, false);
                File image = new File(getActivity().getFilesDir() + File.separator
                            + "twelve" + System.currentTimeMillis() + ".png");
                path = image.getAbsolutePath();
                mLockImage.renameTo(image);
                FileOutputStream outPut = new FileOutputStream(image);
                bsLocktwelve.compress(Bitmap.CompressFormat.PNG, 100, outPut);
                image.setReadable(true, false);
                outPut.flush();
                outPut.close();
            } catch (Exception e) {
                // Uh-oh Nothing we can do here.
                Log.e(TAG, e.getMessage(), e);
                return;
            }

            deleteLockIcon();  // Delete current icon if it exists before saving new.
            Settings.Secure.putString(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_LOCK_ICON, path);
            mColorizeCustom.setEnabled(path != null);
            updateLockSummary();
        }
    }
	
	private void resizeBsLockeleven() {
        Bitmap bsLockeleven = BitmapFactory.decodeResource(getResources(), R.drawable.bs_lock_eleven);
        if (bsLockeleven != null) {
            String path = null;
            int px = requestImageSize();
            bsLockeleven = Bitmap.createScaledBitmap(bsLockeleven, px, px, true);
            try {
                mLockImage.createNewFile();
                mLockImage.setWritable(true, false);
                File image = new File(getActivity().getFilesDir() + File.separator
                            + "eleven" + System.currentTimeMillis() + ".png");
                path = image.getAbsolutePath();
                mLockImage.renameTo(image);
                FileOutputStream outPut = new FileOutputStream(image);
                bsLockeleven.compress(Bitmap.CompressFormat.PNG, 100, outPut);
                image.setReadable(true, false);
                outPut.flush();
                outPut.close();
            } catch (Exception e) {
                // Uh-oh Nothing we can do here.
                Log.e(TAG, e.getMessage(), e);
                return;
            }

            deleteLockIcon();  // Delete current icon if it exists before saving new.
            Settings.Secure.putString(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_LOCK_ICON, path);
            mColorizeCustom.setEnabled(path != null);
            updateLockSummary();
        }
    }
	
    private void resizeBsLockten() {
        Bitmap bsLockten = BitmapFactory.decodeResource(getResources(), R.drawable.bs_lock_ten);
        if (bsLockten != null) {
            String path = null;
            int px = requestImageSize();
            bsLockten = Bitmap.createScaledBitmap(bsLockten, px, px, true);
            try {
                mLockImage.createNewFile();
                mLockImage.setWritable(true, false);
                File image = new File(getActivity().getFilesDir() + File.separator
                            + "ten" + System.currentTimeMillis() + ".png");
                path = image.getAbsolutePath();
                mLockImage.renameTo(image);
                FileOutputStream outPut = new FileOutputStream(image);
                bsLockten.compress(Bitmap.CompressFormat.PNG, 100, outPut);
                image.setReadable(true, false);
                outPut.flush();
                outPut.close();
            } catch (Exception e) {
                // Uh-oh Nothing we can do here.
                Log.e(TAG, e.getMessage(), e);
                return;
            }

            deleteLockIcon();  // Delete current icon if it exists before saving new.
            Settings.Secure.putString(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_LOCK_ICON, path);
            mColorizeCustom.setEnabled(path != null);
            updateLockSummary();
        }
    }

    private void resizeBsLocknine() {
        Bitmap bsLocknine = BitmapFactory.decodeResource(getResources(), R.drawable.bs_lock_nine);
        if (bsLocknine != null) {
            String path = null;
            int px = requestImageSize();
            bsLocknine = Bitmap.createScaledBitmap(bsLocknine, px, px, true);
            try {
                mLockImage.createNewFile();
                mLockImage.setWritable(true, false);
                File image = new File(getActivity().getFilesDir() + File.separator
                            + "nine" + System.currentTimeMillis() + ".png");
                path = image.getAbsolutePath();
                mLockImage.renameTo(image);
                FileOutputStream outPut = new FileOutputStream(image);
                bsLocknine.compress(Bitmap.CompressFormat.PNG, 100, outPut);
                image.setReadable(true, false);
                outPut.flush();
                outPut.close();
            } catch (Exception e) {
                // Uh-oh Nothing we can do here.
                Log.e(TAG, e.getMessage(), e);
                return;
            }

            deleteLockIcon();  // Delete current icon if it exists before saving new.
            Settings.Secure.putString(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_LOCK_ICON, path);
            mColorizeCustom.setEnabled(path != null);
            updateLockSummary();
        }
    }

    private void resizeBsLockeight() {
        Bitmap bsLockeight = BitmapFactory.decodeResource(getResources(), R.drawable.bs_lock_eight);
        if (bsLockeight != null) {
            String path = null;
            int px = requestImageSize();
            bsLockeight = Bitmap.createScaledBitmap(bsLockeight, px, px, true);
            try {
                mLockImage.createNewFile();
                mLockImage.setWritable(true, false);
                File image = new File(getActivity().getFilesDir() + File.separator
                            + "eight" + System.currentTimeMillis() + ".png");
                path = image.getAbsolutePath();
                mLockImage.renameTo(image);
                FileOutputStream outPut = new FileOutputStream(image);
                bsLockeight.compress(Bitmap.CompressFormat.PNG, 100, outPut);
                image.setReadable(true, false);
                outPut.flush();
                outPut.close();
            } catch (Exception e) {
                // Uh-oh Nothing we can do here.
                Log.e(TAG, e.getMessage(), e);
                return;
            }

            deleteLockIcon();  // Delete current icon if it exists before saving new.
            Settings.Secure.putString(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_LOCK_ICON, path);
            mColorizeCustom.setEnabled(path != null);
            updateLockSummary();
        }
    }

    private void resizeBsLockseven() {
        Bitmap bsLockseven = BitmapFactory.decodeResource(getResources(), R.drawable.bs_lock_seven);
        if (bsLockseven != null) {
            String path = null;
            int px = requestImageSize();
            bsLockseven = Bitmap.createScaledBitmap(bsLockseven, px, px, true);
            try {
                mLockImage.createNewFile();
                mLockImage.setWritable(true, false);
                File image = new File(getActivity().getFilesDir() + File.separator
                            + "seven" + System.currentTimeMillis() + ".png");
                path = image.getAbsolutePath();
                mLockImage.renameTo(image);
                FileOutputStream outPut = new FileOutputStream(image);
                bsLockseven.compress(Bitmap.CompressFormat.PNG, 100, outPut);
                image.setReadable(true, false);
                outPut.flush();
                outPut.close();
            } catch (Exception e) {
                // Uh-oh Nothing we can do here.
                Log.e(TAG, e.getMessage(), e);
                return;
            }

            deleteLockIcon();  // Delete current icon if it exists before saving new.
            Settings.Secure.putString(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_LOCK_ICON, path);
            mColorizeCustom.setEnabled(path != null);
            updateLockSummary();
        }
    }

    private void resizeBsLocksix() {
        Bitmap bsLocksix = BitmapFactory.decodeResource(getResources(), R.drawable.bs_lock_six);
        if (bsLocksix != null) {
            String path = null;
            int px = requestImageSize();
            bsLocksix = Bitmap.createScaledBitmap(bsLocksix, px, px, true);
            try {
                mLockImage.createNewFile();
                mLockImage.setWritable(true, false);
                File image = new File(getActivity().getFilesDir() + File.separator
                            + "six" + System.currentTimeMillis() + ".png");
                path = image.getAbsolutePath();
                mLockImage.renameTo(image);
                FileOutputStream outPut = new FileOutputStream(image);
                bsLocksix.compress(Bitmap.CompressFormat.PNG, 100, outPut);
                image.setReadable(true, false);
                outPut.flush();
                outPut.close();
            } catch (Exception e) {
                // Uh-oh Nothing we can do here.
                Log.e(TAG, e.getMessage(), e);
                return;
            }

            deleteLockIcon();  // Delete current icon if it exists before saving new.
            Settings.Secure.putString(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_LOCK_ICON, path);
            mColorizeCustom.setEnabled(path != null);
            updateLockSummary();
        }
    }

    private void resizeBsLockfive() {
        Bitmap bsLockfive = BitmapFactory.decodeResource(getResources(), R.drawable.bs_lock_five);
        if (bsLockfive != null) {
            String path = null;
            int px = requestImageSize();
            bsLockfive = Bitmap.createScaledBitmap(bsLockfive, px, px, true);
            try {
                mLockImage.createNewFile();
                mLockImage.setWritable(true, false);
                File image = new File(getActivity().getFilesDir() + File.separator
                            + "five" + System.currentTimeMillis() + ".png");
                path = image.getAbsolutePath();
                mLockImage.renameTo(image);
                FileOutputStream outPut = new FileOutputStream(image);
                bsLockfive.compress(Bitmap.CompressFormat.PNG, 100, outPut);
                image.setReadable(true, false);
                outPut.flush();
                outPut.close();
            } catch (Exception e) {
                // Uh-oh Nothing we can do here.
                Log.e(TAG, e.getMessage(), e);
                return;
            }

            deleteLockIcon();  // Delete current icon if it exists before saving new.
            Settings.Secure.putString(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_LOCK_ICON, path);
            mColorizeCustom.setEnabled(path != null);
            updateLockSummary();
        }
    }

    private void resizeBsLockfour() {
        Bitmap bsLockfour = BitmapFactory.decodeResource(getResources(), R.drawable.bs_lock_four);
        if (bsLockfour != null) {
            String path = null;
            int px = requestImageSize();
            bsLockfour = Bitmap.createScaledBitmap(bsLockfour, px, px, true);
            try {
                mLockImage.createNewFile();
                mLockImage.setWritable(true, false);
                File image = new File(getActivity().getFilesDir() + File.separator
                            + "four" + System.currentTimeMillis() + ".png");
                path = image.getAbsolutePath();
                mLockImage.renameTo(image);
                FileOutputStream outPut = new FileOutputStream(image);
                bsLockfour.compress(Bitmap.CompressFormat.PNG, 100, outPut);
                image.setReadable(true, false);
                outPut.flush();
                outPut.close();
            } catch (Exception e) {
                // Uh-oh Nothing we can do here.
                Log.e(TAG, e.getMessage(), e);
                return;
            }

            deleteLockIcon();  // Delete current icon if it exists before saving new.
            Settings.Secure.putString(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_LOCK_ICON, path);
            mColorizeCustom.setEnabled(path != null);
            updateLockSummary();
        }
    }

    private void resizeBsLockthree() {
        Bitmap bsLockthree = BitmapFactory.decodeResource(getResources(), R.drawable.bs_lock_three);
        if (bsLockthree != null) {
            String path = null;
            int px = requestImageSize();
            bsLockthree = Bitmap.createScaledBitmap(bsLockthree, px, px, true);
            try {
                mLockImage.createNewFile();
                mLockImage.setWritable(true, false);
                File image = new File(getActivity().getFilesDir() + File.separator
                            + "three" + System.currentTimeMillis() + ".png");
                path = image.getAbsolutePath();
                mLockImage.renameTo(image);
                FileOutputStream outPut = new FileOutputStream(image);
                bsLockthree.compress(Bitmap.CompressFormat.PNG, 100, outPut);
                image.setReadable(true, false);
                outPut.flush();
                outPut.close();
            } catch (Exception e) {
                // Uh-oh Nothing we can do here.
                Log.e(TAG, e.getMessage(), e);
                return;
            }

            deleteLockIcon();  // Delete current icon if it exists before saving new.
            Settings.Secure.putString(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_LOCK_ICON, path);
            mColorizeCustom.setEnabled(path != null);
            updateLockSummary();
        }
    }

    private void resizeBsLocktwo() {
        Bitmap bsLocktwo = BitmapFactory.decodeResource(getResources(), R.drawable.bs_lock_two);
        if (bsLocktwo != null) {
            String path = null;
            int px = requestImageSize();
            bsLocktwo = Bitmap.createScaledBitmap(bsLocktwo, px, px, true);
            try {
                mLockImage.createNewFile();
                mLockImage.setWritable(true, false);
                File image = new File(getActivity().getFilesDir() + File.separator
                            + "two" + System.currentTimeMillis() + ".png");
                path = image.getAbsolutePath();
                mLockImage.renameTo(image);
                FileOutputStream outPut = new FileOutputStream(image);
                bsLocktwo.compress(Bitmap.CompressFormat.PNG, 100, outPut);
                image.setReadable(true, false);
                outPut.flush();
                outPut.close();
            } catch (Exception e) {
                // Uh-oh Nothing we can do here.
                Log.e(TAG, e.getMessage(), e);
                return;
            }

            deleteLockIcon();  // Delete current icon if it exists before saving new.
            Settings.Secure.putString(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_LOCK_ICON, path);
            mColorizeCustom.setEnabled(path != null);
            updateLockSummary();
        }
    }

    private void resizeBsLockone() {
        Bitmap bsLockone = BitmapFactory.decodeResource(getResources(), R.drawable.bs_lock_one);
        if (bsLockone != null) {
            String path = null;
            int px = requestImageSize();
            bsLockone = Bitmap.createScaledBitmap(bsLockone, px, px, true);
            try {
                mLockImage.createNewFile();
                mLockImage.setWritable(true, false);
                File image = new File(getActivity().getFilesDir() + File.separator
                            + "one" + System.currentTimeMillis() + ".png");
                path = image.getAbsolutePath();
                mLockImage.renameTo(image);
                FileOutputStream outPut = new FileOutputStream(image);
                bsLockone.compress(Bitmap.CompressFormat.PNG, 100, outPut);
                image.setReadable(true, false);
                outPut.flush();
                outPut.close();
            } catch (Exception e) {
                // Uh-oh Nothing we can do here.
                Log.e(TAG, e.getMessage(), e);
                return;
            }

            deleteLockIcon();  // Delete current icon if it exists before saving new.
            Settings.Secure.putString(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_LOCK_ICON, path);
            mColorizeCustom.setEnabled(path != null);
            updateLockSummary();
        }
    }

    private void resizeBsLock() {
        Bitmap bsLock = BitmapFactory.decodeResource(getResources(), R.drawable.bs_lock);
        if (bsLock != null) {
            String path = null;
            int px = requestImageSize();
            bsLock = Bitmap.createScaledBitmap(bsLock, px, px, true);
            try {
                mLockImage.createNewFile();
                mLockImage.setWritable(true, false);
                File image = new File(getActivity().getFilesDir() + File.separator
                            + "bs_lock" + System.currentTimeMillis() + ".png");
                path = image.getAbsolutePath();
                mLockImage.renameTo(image);
                FileOutputStream outPut = new FileOutputStream(image);
                bsLock.compress(Bitmap.CompressFormat.PNG, 100, outPut);
                image.setReadable(true, false);
                outPut.flush();
                outPut.close();
            } catch (Exception e) {
                // Uh-oh Nothing we can do here.
                Log.e(TAG, e.getMessage(), e);
                return;
            }

            deleteLockIcon();  // Delete current icon if it exists before saving new.
            Settings.Secure.putString(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_LOCK_ICON, path);
            mColorizeCustom.setEnabled(path != null);
            updateLockSummary();
        }
    }

    private int requestImageSize() {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 68, getResources().getDisplayMetrics());
    }

    private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        LockscreenStyle getOwner() {
            return (LockscreenStyle) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_RESET:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.reset)
                    .setMessage(R.string.lockscreen_style_reset_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.Secure.putInt(getActivity().getContentResolver(),
                                    Settings.Secure.LOCKSCREEN_FRAME_COLOR, -2);
                            Settings.Secure.putInt(getActivity().getContentResolver(),
                                    Settings.Secure.LOCKSCREEN_LOCK_COLOR, -2);
                            Settings.Secure.putInt(getActivity().getContentResolver(),
                                    Settings.Secure.LOCKSCREEN_DOTS_COLOR, -2);
                            Settings.Secure.putInt(getActivity().getContentResolver(),
                                    Settings.Secure.LOCKSCREEN_TARGETS_COLOR, -2);
                            Settings.Secure.putInt(getActivity().getContentResolver(),
                                    Settings.Secure.LOCKSCREEN_MISC_COLOR, -2);
                            getOwner().createCustomView();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {

        }
    }
}

