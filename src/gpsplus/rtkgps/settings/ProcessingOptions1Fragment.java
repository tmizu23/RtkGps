package gpsplus.rtkgps.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.R;
import gpsplus.rtkgps.settings.widget.EarthTideCorrectionPreference;
import gpsplus.rtkgps.settings.widget.EphemerisOptionPreference;
import gpsplus.rtkgps.settings.widget.IonosphereCorrectionPreference;
import gpsplus.rtkgps.settings.widget.MultiSelectListPreferenceWorkaround;
import gpsplus.rtkgps.settings.widget.PositioningModePreference;
import gpsplus.rtkgps.settings.widget.TroposphereCorrectionPreference;
import gpsplus.rtkgps.utils.PreciseEphemerisProvider;
import gpsplus.rtklib.ProcessingOptions;
import gpsplus.rtklib.constants.EarthTideCorrectionType;
import gpsplus.rtklib.constants.EphemerisOption;
import gpsplus.rtklib.constants.IonosphereOption;
import gpsplus.rtklib.constants.NavigationSystem;
import gpsplus.rtklib.constants.PositioningMode;
import gpsplus.rtklib.constants.TroposphereOption;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;


public class ProcessingOptions1Fragment extends PreferenceFragment {

    private static final boolean DBG = BuildConfig.DEBUG & true;
    static final String TAG = ProcessingOptions1Fragment.class.getSimpleName();

    public static final String SHARED_PREFS_NAME = "ProcessingOptions1";

    static final String KEY_POSITIONING_MODE = "positioning_mode";
    static final String KEY_NUMBER_OF_FREQUENCIES = "number_of_frequencies";
    static final String KEY_NAVIGATION_SYSTEM = "navigation_system";
    static final String KEY_ELEVATION_MASK = "elevation_mask";
    static final String KEY_SNR_MASK = "snr_mask";
    static final String KEY_REC_DYNAMICS = "rec_dynamics";
    static final String KEY_EARTH_TIDES_CORRECTION = "rec_earth_tides_correction";
    static final String KEY_IONOSPHERE_CORRECTION = "ionosphere_correction";
    static final String KEY_TROPOSPHERE_CORRECTION = "troposphere_correction";
    public static final String KEY_SAT_EPHEM_CLOCK = "satellite_ephemeris_clock";
    static final String KEY_SAT_ANTENNA_PCV = "sat_antenna_pcv";
    static final String KEY_RECEIVER_ANTENNA_PCV = "receiver_antenna_pcv";
    static final String KEY_PHASE_WINDUP_CORRECTION = "phase_windup_correction";
    static final String KEY_EXCLUDE_ECLIPSING = "exclude_eclipsing_sat_measurements";
    static final String KEY_RAIM_FDE = "raim_fde";
    public static final String KEY_PROCESSING_CYCLE = "processing_cycle";
    public static final String KEY_AMBIGUITY_RESOLUTION = "ambiguity_resolution";
    public static final String KEY_GLONASS_AMBIGUITY_RESOLUTION ="glonass_ambiguity_resolution";

    public static final String KEY_BDS_AMBIGUITY_RESOLUTION ="bds_ambiguity_resolution";

    public static final String KEY_AR_FILTER ="ar_filter";

    public static final String KEY_AR_MIN_FIX ="ar_min_fix";
    public static final String KEY_MIN_FIX_SATS ="min_fix_sats";
    public static final String KEY_MIN_HOLD_SATS ="min_hold_sats";
    public static final String KEY_MIN_DROP_SATS ="min_drop_sats";
    public static final String KEY_RCV_STDS ="rcv_stds";

    public static final String KEY_N_ITER ="n_iter";
    public static final String KEY_MAX_AVE_AMB ="max_ave_ep";

    public static final String KEY_VAR_HOLD_AMB ="var_hold_amb";
    public static final String KEY_GAIN_HOLD_AMB ="gain_hold_amb";

    public static final String KEY_INIT_RST ="init_rst";
    public static final String KEY_OUT_SINGLE ="out_single";

    public static final String KEY_AR_OUT_CNT ="ar_out_cnt";
    public static final String KEY_SLIP_THRES ="slip_thres";
    public static final String KEY_MAX_AGE ="max_age";
    public static final String KEY_REJ_IONNO ="rej_ionno";


    public static final String KEY_MIN_FIX_RATIO ="min_fix_ratio";
    private static final String KEY_RESET_RECOMMENDED = "reset_recommended";
    public static final String KEY_MAX_POS_VAR ="max_pos_var";
    private static final String KEY_MIN_FIX_ELEVATION = "min_fix_elevation";
    private static final String KEY_MIN_HOLD_ELEVATION = "min_hold_elevation";

    private static final String KEY_MIN_FIX_LOCK = "min_fix_lock";

    private static final String KEY_RTKLIB_SETTINGS_VERSION = "rtklib_settings_version";
    /** settings written for RTKLIB demo5 (rtklibexplorer 2.5.x) */
    private static final int RTKLIB_SETTINGS_VERSION_DEMO5 = 1;

    // Settings
    private PositioningModePreference mPositioningModePref;
    private ListPreference mNumberOfFrequenciesPref;
    private MultiSelectListPreferenceWorkaround mNavigationSystem;
    private ListPreference mElevationMaskPref;
    private ListPreference mSnrMaskPref;
    private ListPreference mAmbiguityResolutionPref;
    private ListPreference mGlonassAmbiguityResolutionPref;

    private ListPreference mBDSAmbiguityResolutionPref;

    private ListPreference mArFilterPref;

    private EditTextPreference mArMinFixPref;
    private EditTextPreference mMinFixSatsPref;
    private EditTextPreference mMinHoldSatsPref;
    private EditTextPreference mMinDropSatsPref;
    private ListPreference mRcvStdsPref;

    private EditTextPreference mNIterPref;
    private EditTextPreference mMaxAveEpPref;

    private EditTextPreference mVarHoldAmbPref;
    private EditTextPreference mGainHoldAmbPref;

    private ListPreference mInitRstPref;
    private ListPreference mOutSinglePref;

    private EditTextPreference mArOutCntPref;
    private EditTextPreference mMaxAgePref;
    private EditTextPreference mSlipThreshPref;
    private EditTextPreference mRejIonnoPref;

    private EarthTideCorrectionPreference mEarthTidesCorrPref;
    private IonosphereCorrectionPreference mIonosphereCorrectionPref;
    private TroposphereCorrectionPreference mTroposphereCorrectionPref;
    private EphemerisOptionPreference mSatEphemClockPref;
    private EditTextPreference mMinRatioFixPref;
    private EditTextPreference mMaxPosVarPref;
    private EditTextPreference mMinElevationFixPref;
    private EditTextPreference mMinElevationHoldPref;
    private EditTextPreference mMinLockFixPref;

    private final PreferenceChangeListener mPreferenceChangeListener;

    public ProcessingOptions1Fragment() {
        mPreferenceChangeListener = new PreferenceChangeListener();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPreferenceManager().setSharedPreferencesName(SHARED_PREFS_NAME);
        addPreferencesFromResource(R.xml.processing_options1);
        findPreference(KEY_RESET_RECOMMENDED).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                confirmResetToRecommended();
                return true;
            }
        });

        initSettings1();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final boolean handled = super.onPreferenceTreeClick(preferenceScreen, preference);
        if (preference instanceof PreferenceScreen) {
            // A nested screen is shown in a dialog whose action bar "up" does nothing: close it
            final Dialog dialog = ((PreferenceScreen) preference).getDialog();
            final View home = dialog == null ? null : dialog.findViewById(android.R.id.home);
            if (home != null && home.getParent() != null && home.getParent().getParent() instanceof View) {
                ((View) home.getParent().getParent()).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
            }
        }
        return handled;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateEnable();
        reloadSummaries();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(mPreferenceChangeListener);
        super.onPause();
    }

    @SuppressLint("NewApi")
    private void initSettings1() {
        final Resources r = getResources();
        ProcessingOptions opts = readPrefs(getActivity());

        // Positioning mode
        mPositioningModePref = (PositioningModePreference)findPreference(KEY_POSITIONING_MODE);

        // Number of frequencies
        mNumberOfFrequenciesPref = (ListPreference)findPreference(KEY_NUMBER_OF_FREQUENCIES);

        // Navigation system
        mNavigationSystem = (MultiSelectListPreferenceWorkaround)findPreference(KEY_NAVIGATION_SYSTEM);
        mNavigationSystem.setEntries(NavigationSystem.getEntries(r));
        mNavigationSystem.setEntryValues(NavigationSystem.getEntryValues());
        final Set<String> navsysSet = new HashSet<String>(7);
        for (NavigationSystem ns: opts.getNavigationSystem()) {
            navsysSet.add(ns.name());
        }
        mNavigationSystem.setValues(navsysSet);

        // Elevation mask
        mElevationMaskPref = (ListPreference)findPreference(KEY_ELEVATION_MASK);

        // Earth tides correction
        mEarthTidesCorrPref = (EarthTideCorrectionPreference)findPreference(KEY_EARTH_TIDES_CORRECTION);

        // Ionosphere correction
        mIonosphereCorrectionPref = (IonosphereCorrectionPreference)findPreference(KEY_IONOSPHERE_CORRECTION);

        // Troposphere correction
        mTroposphereCorrectionPref = (TroposphereCorrectionPreference)findPreference(KEY_TROPOSPHERE_CORRECTION);

        // Satellite Ephemeris/Clock
        mSatEphemClockPref = (EphemerisOptionPreference)findPreference(KEY_SAT_EPHEM_CLOCK);

        mSnrMaskPref = (ListPreference)findPreference(KEY_SNR_MASK);

        mAmbiguityResolutionPref = (ListPreference)findPreference(KEY_AMBIGUITY_RESOLUTION);

        mGlonassAmbiguityResolutionPref = (ListPreference)findPreference(KEY_GLONASS_AMBIGUITY_RESOLUTION);


        mBDSAmbiguityResolutionPref = (ListPreference)findPreference(KEY_BDS_AMBIGUITY_RESOLUTION);

        mArFilterPref = (ListPreference)findPreference(KEY_AR_FILTER);

        mArMinFixPref = (EditTextPreference)findPreference(KEY_AR_MIN_FIX);
        mMinFixSatsPref = (EditTextPreference)findPreference(KEY_MIN_FIX_SATS);
        mMinHoldSatsPref = (EditTextPreference)findPreference(KEY_MIN_HOLD_SATS);
        mMinDropSatsPref = (EditTextPreference)findPreference(KEY_MIN_DROP_SATS);
        mRcvStdsPref = (ListPreference)findPreference(KEY_RCV_STDS);

        mNIterPref = (EditTextPreference)findPreference(KEY_N_ITER);
        mMaxAveEpPref = (EditTextPreference)findPreference(KEY_MAX_AVE_AMB);

        mVarHoldAmbPref = (EditTextPreference)findPreference(KEY_VAR_HOLD_AMB);
        mGainHoldAmbPref = (EditTextPreference)findPreference(KEY_GAIN_HOLD_AMB);

        mInitRstPref = (ListPreference)findPreference(KEY_INIT_RST);
        mOutSinglePref = (ListPreference)findPreference(KEY_OUT_SINGLE);

        mArOutCntPref = (EditTextPreference)findPreference(KEY_AR_OUT_CNT);
        mSlipThreshPref = (EditTextPreference)findPreference(KEY_SLIP_THRES);
        mMaxAgePref = (EditTextPreference)findPreference(KEY_MAX_AGE);
        mRejIonnoPref = (EditTextPreference)findPreference(KEY_REJ_IONNO);

        mMinRatioFixPref = (EditTextPreference)findPreference(KEY_MIN_FIX_RATIO);
        mMaxPosVarPref = (EditTextPreference)findPreference(KEY_MAX_POS_VAR);
        mMinLockFixPref = (EditTextPreference)findPreference(KEY_MIN_FIX_LOCK);

        mMinElevationFixPref = (EditTextPreference)findPreference(KEY_MIN_FIX_ELEVATION);
        mMinElevationHoldPref = (EditTextPreference)findPreference(KEY_MIN_HOLD_ELEVATION);

    }

    private void reloadSummaries() {
        final Resources r = getResources();
        CharSequence summary;

        summary = mPositioningModePref.getEntry();
        mPositioningModePref.setSummary(summary);

        summary = mNumberOfFrequenciesPref.getEntry();
        mNumberOfFrequenciesPref.setSummary(summary);

        summary = mAmbiguityResolutionPref.getEntry();
        mAmbiguityResolutionPref.setSummary(summary);

        summary = mGlonassAmbiguityResolutionPref.getEntry();
        mGlonassAmbiguityResolutionPref.setSummary(summary);


        summary = mBDSAmbiguityResolutionPref.getEntry();
        mBDSAmbiguityResolutionPref.setSummary(summary);

        summary = mArFilterPref.getEntry();
        mArFilterPref.setSummary(summary);

        summary = mArMinFixPref.getText();
        mArMinFixPref.setSummary(summary);

        summary = mMinFixSatsPref.getText();
        mMinFixSatsPref.setSummary(summary);

        summary = mMinHoldSatsPref.getText();
        mMinHoldSatsPref.setSummary(summary);

        summary = mMinDropSatsPref.getText();
        mMinDropSatsPref.setSummary(summary);

        summary = mRcvStdsPref.getEntry();
        mRcvStdsPref.setSummary(summary);


        summary = mNIterPref.getText();
        mNIterPref.setSummary(summary);

        summary = mMaxAveEpPref.getText();
        mMaxAveEpPref.setSummary(summary);

        summary = mVarHoldAmbPref.getText();
        mVarHoldAmbPref.setSummary(summary);

        summary = mGainHoldAmbPref.getText();
        mGainHoldAmbPref.setSummary(summary);

        summary = mInitRstPref.getEntry();
        mInitRstPref.setSummary(summary);
        summary = mOutSinglePref.getEntry();
        mOutSinglePref.setSummary(summary);

        summary = mArOutCntPref.getText();
        mArOutCntPref.setSummary(summary);
        summary = mMaxAgePref.getText();
        mMaxAgePref.setSummary(summary);
        summary = mSlipThreshPref.getText();
        mSlipThreshPref.setSummary(summary);
        summary = mRejIonnoPref.getText();
        mRejIonnoPref.setSummary(summary);


        summary = mMinElevationFixPref.getText();
        mMinElevationFixPref.setSummary(summary);

        summary = mMinElevationHoldPref.getText();
        mMinElevationHoldPref.setSummary(summary);

        summary = mMinLockFixPref.getText();
        mMinLockFixPref.setSummary(summary);

        summary = mMinRatioFixPref.getText();
        mMinRatioFixPref.setSummary(summary);

        summary = mMaxPosVarPref.getText();
        mMaxPosVarPref.setSummary(summary);

        final ArrayList<String> navsys = new ArrayList<String>(10);
        final EnumSet<NavigationSystem> navsys0 = EnumSet.noneOf(NavigationSystem.class);
        // sort
        for (String v: mNavigationSystem.getValues()) navsys0.add(NavigationSystem.valueOf(v));

        for (NavigationSystem ns: navsys0) navsys.add(r.getString(ns.getNameResId()));

        summary = TextUtils.join(", ",  navsys);
        mNavigationSystem.setSummary(summary);

        summary = mElevationMaskPref.getEntry();
        mElevationMaskPref.setSummary(summary);

        summary = mSnrMaskPref.getEntry();
        mSnrMaskPref.setSummary(summary);

        mIonosphereCorrectionPref.setSummary(mIonosphereCorrectionPref.getEntry());
        mTroposphereCorrectionPref.setSummary(mTroposphereCorrectionPref.getEntry());
        mSatEphemClockPref.setSummary(mSatEphemClockPref.getEntry());
        mEarthTidesCorrPref.setSummary(mEarthTidesCorrPref.getEntry());
    }

    private void updateEnable() {
        PositioningMode posMode;
        final boolean moveb, rel, ppp, rtk;

        posMode = PositioningMode.valueOf(mPositioningModePref.getValue());

        rel = posMode.isRelative();
        rtk = posMode.isRtk();
        ppp = posMode.isPpp();
        moveb = posMode.isMoveB();

        mNumberOfFrequenciesPref.setEnabled(rel);
        findPreference(KEY_REC_DYNAMICS).setEnabled(rel);
        mEarthTidesCorrPref.setEnabled(rel || ppp);

        findPreference(KEY_SAT_ANTENNA_PCV).setEnabled(ppp);
        findPreference(KEY_RECEIVER_ANTENNA_PCV).setEnabled(ppp);
        findPreference(KEY_PHASE_WINDUP_CORRECTION).setEnabled(ppp);
        findPreference(KEY_EXCLUDE_ECLIPSING).setEnabled(ppp);
        findPreference(KEY_AMBIGUITY_RESOLUTION).setEnabled(rtk || ppp);
        findPreference(KEY_GLONASS_AMBIGUITY_RESOLUTION).setEnabled(rtk || ppp);

        findPreference(KEY_BDS_AMBIGUITY_RESOLUTION).setEnabled(rtk || ppp);

        findPreference(KEY_AR_FILTER).setEnabled(rtk || ppp);

        findPreference(KEY_AR_MIN_FIX).setEnabled(rtk || ppp);
        findPreference(KEY_MIN_FIX_SATS).setEnabled(rtk || ppp);
        findPreference(KEY_MIN_HOLD_SATS).setEnabled(rtk || ppp);

        findPreference(KEY_MIN_DROP_SATS).setEnabled(rtk || ppp);

        findPreference(KEY_RCV_STDS).setEnabled(rtk || ppp);

        findPreference(KEY_N_ITER).setEnabled(rtk || ppp);
        findPreference(KEY_MAX_AVE_AMB).setEnabled(rtk || ppp);

        findPreference(KEY_VAR_HOLD_AMB).setEnabled(rtk || ppp);
        findPreference(KEY_GAIN_HOLD_AMB).setEnabled(rtk || ppp);

        findPreference(KEY_OUT_SINGLE).setEnabled(rtk || ppp);
        findPreference(KEY_INIT_RST).setEnabled(rtk || ppp);

        findPreference(KEY_MIN_FIX_ELEVATION).setEnabled(rtk || ppp);
        findPreference(KEY_MIN_HOLD_ELEVATION).setEnabled(rtk || ppp);
        findPreference(KEY_MIN_FIX_LOCK).setEnabled(rtk || ppp);
        findPreference(KEY_MIN_FIX_RATIO).setEnabled(rtk || ppp);
        findPreference(KEY_MAX_POS_VAR).setEnabled(rtk || ppp);


    }

    public static ProcessingOptions readPrefs(Context ctx) {
        final ProcessingOptions opts;
        final SharedPreferences prefs;
        String v;
        Set<String> vSet;

        opts = new ProcessingOptions();
        prefs = ctx.getSharedPreferences(SHARED_PREFS_NAME, Activity.MODE_PRIVATE);

        v = prefs.getString(KEY_POSITIONING_MODE, null);
        opts.setPositioningMode(PositioningMode.valueOf(v));

        v = prefs.getString(KEY_NUMBER_OF_FREQUENCIES, null);
        opts.setNumberOfFrequencies(Integer.valueOf(v));

        vSet = prefs.getStringSet(KEY_NAVIGATION_SYSTEM, null);
        Set<NavigationSystem> navsys = EnumSet.noneOf(NavigationSystem.class);
        for (String v0: vSet) navsys.add(NavigationSystem.valueOf(v0));
        opts.setNavigationSystem(navsys);

        v = prefs.getString(KEY_ELEVATION_MASK, null);
        opts.setElevationMask(Math.toRadians(Double.valueOf(v)));

        v = prefs.getString(KEY_SNR_MASK, null);
        opts.setSnrMask(Integer.valueOf(v));

        opts.setRecDynamics(prefs.getBoolean(KEY_REC_DYNAMICS, opts.getRecDynamics()));

        v = prefs.getString(KEY_EARTH_TIDES_CORRECTION, null);
        opts.setEarthTidesCorrection(EarthTideCorrectionType.valueOf(v));

        v = prefs.getString(KEY_IONOSPHERE_CORRECTION, null);
        opts.setIonosphereCorrection(IonosphereOption.valueOf(v));

        v = prefs.getString(KEY_TROPOSPHERE_CORRECTION, null);
        opts.setTroposphereCorrection(TroposphereOption.valueOf(v));

        v = prefs.getString(KEY_SAT_EPHEM_CLOCK, null);
        opts.setSatEphemerisOption(EphemerisOption.valueOf(v));

        opts.setSatAntennaPcvEnabled(prefs.getBoolean(KEY_SAT_ANTENNA_PCV, opts.isSatAntennaPcvEnabled()));
        opts.setReceiverAntennaPcvEnabled(prefs.getBoolean(KEY_RECEIVER_ANTENNA_PCV, opts.isReceiverAntennaPcvEnabled()));
        opts.setPhaseWindupCorrectionEnabled(prefs.getBoolean(KEY_PHASE_WINDUP_CORRECTION, opts.isPhaseWindupCorrectionEnabled()));
        opts.setExcludeEclipsingSatMeasurements(prefs.getBoolean(KEY_EXCLUDE_ECLIPSING, opts.isExcludeEclipsingSatMeasurements()));
        opts.setRaimFdeEnabled(prefs.getBoolean(KEY_RAIM_FDE, opts.isRaimFdeEnabled()));

        opts.setModeAR(Integer.valueOf(prefs.getString(KEY_AMBIGUITY_RESOLUTION, "0")));
        opts.setModeGAR(Integer.valueOf(prefs.getString(KEY_GLONASS_AMBIGUITY_RESOLUTION, "0")));

        opts.setModeBDSAR(Integer.valueOf(prefs.getString(KEY_BDS_AMBIGUITY_RESOLUTION, "0")));

        opts.setArFilter(Integer.valueOf(prefs.getString(KEY_AR_FILTER, "0")));

        opts.setMinFixCountToHoldAmbiguity(Integer.valueOf(prefs.getString(KEY_AR_MIN_FIX, "0")));
        opts.setMinFixToFixAmbiguity(Integer.valueOf(prefs.getString(KEY_MIN_FIX_SATS, "0")));
        opts.setMinHoldToFixAmbiguity(Integer.valueOf(prefs.getString(KEY_MIN_HOLD_SATS, "0")));
        opts.setMinDropToFixAmbiguity(Integer.valueOf(prefs.getString(KEY_MIN_DROP_SATS, "0")));
        opts.setRcvStds(Integer.valueOf(prefs.getString(KEY_RCV_STDS, "0")));

        opts.setNIter(Integer.valueOf(prefs.getString(KEY_N_ITER, "0")));
        opts.setMaxAveEp(Integer.valueOf(prefs.getString(KEY_MAX_AVE_AMB, "0")));

        opts.setVarHoldAmb(Double.parseDouble(prefs.getString(KEY_VAR_HOLD_AMB, "0")));
        opts.setGainHoldAmb(Double.parseDouble(prefs.getString(KEY_GAIN_HOLD_AMB, "0")));

        opts.setInitRst(Integer.valueOf(prefs.getString(KEY_INIT_RST, "0")));
        opts.setOutSingle(Integer.valueOf(prefs.getString(KEY_OUT_SINGLE, "0")));

        opts.setArOutCnt(Integer.valueOf(prefs.getString(KEY_AR_OUT_CNT, "0")));

        opts.setSlipThres(Double.parseDouble(prefs.getString(KEY_SLIP_THRES, "0")));
        opts.setMaxAge(Double.parseDouble(prefs.getString(KEY_MAX_AGE, "0")));
        opts.setRejIonno(Double.parseDouble(prefs.getString(KEY_REJ_IONNO, "0")));


        opts.setValidThresoldAR(Double.parseDouble(prefs.getString(KEY_MIN_FIX_RATIO, "3.0")));
        opts.setMaxPositionVariance(Double.parseDouble(prefs.getString(KEY_MAX_POS_VAR, "0.25")));

        opts.setMinElevationToFixAmbiguityRad(Double.parseDouble(prefs.getString(KEY_MIN_FIX_ELEVATION, "0"))*(Math.PI/180));
        opts.setMinElevationToHoldAmbiguityRad(Double.parseDouble(prefs.getString(KEY_MIN_HOLD_ELEVATION, "0"))*(Math.PI/180));
        opts.setMinLockToFixAmbiguity(Integer.parseInt(prefs.getString(KEY_MIN_FIX_LOCK, "0")));


        SharedPreferences roverPrefs = ctx.getSharedPreferences(InputRoverFragment.SHARED_PREFS_NAME, Activity.MODE_PRIVATE);
        opts.setAntTypeRover(roverPrefs.getString(InputRoverFragment.KEY_ANTENNA, ""));
        SharedPreferences basePrefs = ctx.getSharedPreferences(InputBaseFragment.SHARED_PREFS_NAME, Activity.MODE_PRIVATE);
        opts.setAntTypeBase(basePrefs.getString(InputRoverFragment.KEY_ANTENNA, ""));

        return opts;
    }

    private class PreferenceChangeListener implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            updateEnable();
            reloadSummaries();
        }
    }

    public static void setValue(Context ctx, ProcessingOptions opts) {
        SharedPreferences prefs;

        prefs = ctx.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);

        Set<String> navSystemSet = new HashSet<String>();
        for (NavigationSystem ns: opts.getNavigationSystem()) navSystemSet.add(ns.name());

        String elmask = String.valueOf(Math.round(Math.toDegrees(opts.getElevationMask())));

        prefs
        .edit()
        .putString(KEY_POSITIONING_MODE, opts.getPositioningMode().name())
        .putString(KEY_NUMBER_OF_FREQUENCIES, String.valueOf(opts.getNumberOfFrequencies()))
        .putStringSet(KEY_NAVIGATION_SYSTEM, navSystemSet)
        .putString(KEY_ELEVATION_MASK, elmask)
        .putString(KEY_SNR_MASK, String.valueOf(opts.getSnrMask()))
        .putBoolean(KEY_REC_DYNAMICS, opts.getRecDynamics())
        .putString(KEY_EARTH_TIDES_CORRECTION, opts.getEarthTidersCorrection().name())
        .putString(KEY_IONOSPHERE_CORRECTION, opts.getIonosphereCorrection().name())
        .putString(KEY_TROPOSPHERE_CORRECTION, opts.getTroposphereCorrection().name())
        .putString(KEY_SAT_EPHEM_CLOCK, opts.getSatEphemerisOption().name())
        .putBoolean(KEY_SAT_ANTENNA_PCV, opts.isSatAntennaPcvEnabled())
        .putBoolean(KEY_RECEIVER_ANTENNA_PCV, opts.isReceiverAntennaPcvEnabled())
        .putBoolean(KEY_PHASE_WINDUP_CORRECTION, opts.isPhaseWindupCorrectionEnabled())
        .putBoolean(KEY_EXCLUDE_ECLIPSING, opts.isExcludeEclipsingSatMeasurements())
        .putBoolean(KEY_RAIM_FDE, opts.isRaimFdeEnabled())
        .putString(KEY_AMBIGUITY_RESOLUTION, String.valueOf(opts.getModeAR()))
        .putString(KEY_GLONASS_AMBIGUITY_RESOLUTION, String.valueOf(opts.getModeGAR()))
        .putString(KEY_BDS_AMBIGUITY_RESOLUTION, String.valueOf(opts.getModeBDSAR()))
        .putString(KEY_AR_FILTER, String.valueOf(opts.getArFilter()))
        .putString(KEY_AR_MIN_FIX, String.valueOf(opts.getMinFixCountToHoldAmbiguity()))
        .putString(KEY_MIN_FIX_SATS, String.valueOf(opts.getMinFixToFixAmbiguity()))
        .putString(KEY_MIN_HOLD_SATS, String.valueOf(opts.getMinHoldToFixAmbiguity()))
        .putString(KEY_MIN_DROP_SATS, String.valueOf(opts.getMinDropToFixAmbiguity()))
        .putString(KEY_RCV_STDS, String.valueOf(opts.getRcvStds()))
        .putString(KEY_N_ITER, String.valueOf(opts.getNIter()))
        .putString(KEY_MAX_AVE_AMB, String.valueOf(opts.getMaxAveEp()))
        .putString(KEY_VAR_HOLD_AMB, String.valueOf(opts.getVarHoldAmb()))
        .putString(KEY_GAIN_HOLD_AMB, String.valueOf(opts.getGainHoldAmb()))

        .putString(KEY_INIT_RST, String.valueOf(opts.getInitRst()))
        .putString(KEY_OUT_SINGLE, String.valueOf(opts.getOutSingle()))
        .putString(KEY_AR_OUT_CNT, String.valueOf(opts.getArOutCnt()))
        .putString(KEY_SLIP_THRES, String.valueOf(opts.getSlipThres()))
        .putString(KEY_MAX_AGE, String.valueOf(opts.getMaxAge()))
        .putString(KEY_REJ_IONNO, String.valueOf(opts.getRejIonno()))
        .putString(KEY_MIN_FIX_LOCK, String.valueOf(opts.getMinLockToFixAmbiguity()))
        .putString(KEY_MIN_FIX_RATIO, String.valueOf(opts.getValidThresoldAR()))
        .putString(KEY_MAX_POS_VAR, String.valueOf(opts.getMaxPositionVariance()))
        // Read back in degrees
        .putString(KEY_MIN_FIX_ELEVATION, String.valueOf(Math.toDegrees(opts.getMinElevationToFixAmbiguityRad())))
        .putString(KEY_MIN_HOLD_ELEVATION, String.valueOf(Math.toDegrees(opts.getMinElevationToHoldAmbiguityRad())))
        .commit()
        ;
        if (DBG) {
            ProcessingOptions opts2 = readPrefs(ctx);
            if (!opts.equals(opts2)) {
                Log.e(TAG, "saved processing options differ from the original");
            }
        }
    }

    public static void setDefaultValues(Context ctx, boolean force) {
        SharedPreferences prefs;

        prefs = ctx.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);

        final boolean needUpdate = force || !prefs.contains(KEY_POSITIONING_MODE);

        if (needUpdate) {
            setValue(ctx, getRecommendedOptions());
        } else {
            migrateToDemo5(prefs);
        }
        prefs.edit().putInt(KEY_RTKLIB_SETTINGS_VERSION, RTKLIB_SETTINGS_VERSION_DEMO5).commit();
    }

    /**
     * Settings saved with RTKLIB 2.4.3 b33 never set the demo5 AR options (saved as 0),
     * and some values were removed or changed meaning in demo5: reset them to demo5 defaults
     */
    private static void migrateToDemo5(SharedPreferences prefs) {
        if (prefs.getInt(KEY_RTKLIB_SETTINGS_VERSION, 0) >= RTKLIB_SETTINGS_VERSION_DEMO5) return;

        final ProcessingOptions defaults = getRecommendedOptions();
        final SharedPreferences.Editor editor = prefs.edit();

        editor
        .putString(KEY_AR_FILTER, String.valueOf(defaults.getArFilter()))
        .putString(KEY_MIN_FIX_SATS, String.valueOf(defaults.getMinFixToFixAmbiguity()))
        .putString(KEY_MIN_HOLD_SATS, String.valueOf(defaults.getMinHoldToFixAmbiguity()))
        .putString(KEY_MIN_DROP_SATS, String.valueOf(defaults.getMinDropToFixAmbiguity()))
        .putString(KEY_RCV_STDS, String.valueOf(defaults.getRcvStds()))
        .putString(KEY_VAR_HOLD_AMB, String.valueOf(defaults.getVarHoldAmb()))
        .putString(KEY_GAIN_HOLD_AMB, String.valueOf(defaults.getGainHoldAmb()))
        // b33: innovation threshold for all measurements, demo5: phase only
        .putString(KEY_REJ_IONNO, String.valueOf(defaults.getRejIonno()))
        // b33: ratio threshold, demo5: max position variance for AR
        .putString(KEY_MAX_POS_VAR, String.valueOf(defaults.getMaxPositionVariance()));

        // Options removed from demo5 (QZSS LEX, ZTD correction, slant TEC)
        if (!isEnumName(IonosphereOption.class, prefs.getString(KEY_IONOSPHERE_CORRECTION, null))) {
            editor.putString(KEY_IONOSPHERE_CORRECTION, defaults.getIonosphereCorrection().name());
        }
        if (!isEnumName(TroposphereOption.class, prefs.getString(KEY_TROPOSPHERE_CORRECTION, null))) {
            editor.putString(KEY_TROPOSPHERE_CORRECTION, defaults.getTroposphereCorrection().name());
        }
        if (!isEnumName(EphemerisOption.class, prefs.getString(KEY_SAT_EPHEM_CLOCK, null))) {
            editor.putString(KEY_SAT_EPHEM_CLOCK, defaults.getSatEphemerisOption().name());
        }
        editor.commit();
    }

    static <T extends Enum<T>> boolean isEnumName(Class<T> clazz, String name) {
        if (name == null) return false;
        try {
            Enum.valueOf(clazz, name);
            return true;
        } catch (IllegalArgumentException iae) {
            return false;
        }
    }

    /**
     * RTKLIB defaults tuned for network RTK (VRS) with a dual frequency receiver
     */
    public static ProcessingOptions getRecommendedOptions() {
        ProcessingOptions opts = new ProcessingOptions();
        opts.setPositioningMode(PositioningMode.KINEMA);
        opts.setNumberOfFrequencies(2);
        opts.setNavigationSystem(EnumSet.of(NavigationSystem.GPS, NavigationSystem.GLO,
                NavigationSystem.GAL, NavigationSystem.QZS, NavigationSystem.CMP));
        opts.setElevationMask(Math.toRadians(15));
        opts.setIonosphereCorrection(IonosphereOption.BRDC);
        opts.setTroposphereCorrection(TroposphereOption.SAAS);
        // Continuous AR: a wrong fix is not held and propagated as with fix and hold
        opts.setModeAR(1);
        // GLONASS inter-channel biases differ between receiver brands
        opts.setModeGAR(0);
        opts.setModeBDSAR(1);
        opts.setValidThresoldAR(3.0);
        return opts;
    }

    private void confirmResetToRecommended() {
        new AlertDialog.Builder(getActivity())
            .setTitle(R.string.procopt_reset_recommended_title)
            .setMessage(R.string.procopt_reset_recommended_confirm)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    setValue(getActivity(), getRecommendedOptions());
                    // Preferences cache their values: rebuild the screen to show the new ones
                    getActivity().recreate();
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

}
