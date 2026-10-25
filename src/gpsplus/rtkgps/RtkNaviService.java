package gpsplus.rtkgps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

//import com.dropbox.sync.android.DbxAccountManager;
//import com.dropbox.sync.android.DbxException;
//import com.dropbox.sync.android.DbxException.Unauthorized;
//import com.dropbox.sync.android.DbxFile;
//import com.dropbox.sync.android.DbxFileSystem;
//import com.dropbox.sync.android.DbxPath;
//import com.dropbox.sync.android.DbxPath.InvalidPathException;


import gpsplus.rtkgps.settings.OutputGPXTraceFragment;
import gpsplus.rtkgps.settings.ProcessingOptions1Fragment;
import gpsplus.rtkgps.settings.SettingsHelper;
import gpsplus.rtkgps.settings.SolutionOutputSettingsFragment;
import gpsplus.rtkgps.settings.StreamBluetoothFragment;
import gpsplus.rtkgps.settings.StreamBluetoothFragment.Value;
import gpsplus.rtkgps.settings.StreamMobileMapperFragment;
import gpsplus.rtkgps.settings.StreamUsbFragment;
import gpsplus.rtkgps.utils.GpsTime;
import gpsplus.rtkgps.utils.PreciseEphemerisDownloader;
import gpsplus.rtkgps.utils.PreciseEphemerisProvider;
import gpsplus.rtkgps.utils.PointStore;
import gpsplus.rtklib.RtkCommon;
import gpsplus.rtklib.RtkCommon.Position3d;
import gpsplus.rtklib.RtkControlResult;
import gpsplus.rtklib.RtkServer;
import gpsplus.rtklib.RtkServerObservationStatus;
import gpsplus.rtklib.RtkServerSettings;
import gpsplus.rtklib.RtkServerSettings.TransportSettings;
import gpsplus.rtklib.RtkServerStreamStatus;
import gpsplus.rtklib.Solution;
import gpsplus.rtklib.constants.EphemerisOption;
import gpsplus.rtklib.constants.GeoidModel;
import gpsplus.rtklib.constants.SolutionStatus;
import gpsplus.rtklib.constants.StreamType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Locale;

public class RtkNaviService extends IntentService implements LocationListener {

    private String m_pointName = "POINT";

    private class dpFile {
        private String localFilenameWithPath;
        private String remoteFilename;

        public dpFile(String localFilenameWithPath, String remoteFilename) {
            super();
            this.localFilenameWithPath = localFilenameWithPath;
            this.remoteFilename = remoteFilename;
        }

        @SuppressWarnings("unused")
        public dpFile(String filename) {
            super();
            this.localFilenameWithPath = filename;
            File _file = new File(filename);
            this.remoteFilename = _file.getName();
        }

        public String getLocalFilenameWithPath() {
            return localFilenameWithPath;
        }

        @SuppressWarnings("unused")
        public void setLocalFilenameWithPath(String localFilenameWithPath) {
            this.localFilenameWithPath = localFilenameWithPath;
        }

        public String getRemoteFilename() {
            return remoteFilename;
        }

        @SuppressWarnings("unused")
        public void setRemoteFilename(String remoteFilename) {
            this.remoteFilename = remoteFilename;
        }

    }

    public RtkNaviService() {
        super(RtkNaviService.class.getSimpleName());
        // TODO Auto-generated constructor stub
    }

    @SuppressWarnings("unused")
    private static final boolean DBG = BuildConfig.DEBUG & true;
    static final String TAG = RtkNaviService.class.getSimpleName();

    public static final String ACTION_START = "gpsplus.rtkgps.RtkNaviService.START";
    public static final String ACTION_STOP = "gpsplus.rtkgps.RtkNaviService.STOP";
    public static final String ACTION_STORE_POINT = "gpsplus.rtkgps.RtkNaviService.STORE_POINT";
    public static final String EXTRA_SESSION_CODE = "gpsplus.rtkgps.RtkNaviService.SESSION_CODE";
    public static final String EXTRA_POINT_NAME = "gpsplus.rtkgps.RtkNaviService.POINT_NAME";
    private static final String RTK_GPS_MOCK_LOC_SERVICE = "RtkGps mock loc service";
    private static final String MM_MAP_HEADER = "COMPD_CS[\"WGS 84\",GEOGCS[\"\",DATUM[\"WGS 84\",SPHEROID[\"WGS 84\",6378137,298.257223563],TOWGS84[0,0,0,0,0,0,0]],PRIMEM[\"Greenwich\",0],UNIT[\"Degrees\",0.0174532925199433],AXIS[\"Long\",East],AXIS[\"Lat\",North]],VERT_CS[\"\",VERT_DATUM[\"Ellipsoid\",2002],UNIT[\"Meters\",1],AXIS[\"Height\",Up]]]\r\n";
    private static final String GPS_PROVIDER = LocationManager.GPS_PROVIDER;
    private boolean mHavePoint = false;
    private int NOTIFICATION = R.string.local_service_started;
    private static final String NOTIFICATION_CHANNEL_ID = "rtknavi_service";
    private RtkCommon rtkCommon;

    // Binder given to clients
    private final IBinder mBinder = new RtkNaviServiceBinder();

    private static final RtkServer mRtkServer = new RtkServer();

    public static boolean mbStarted = false;
    private PowerManager.WakeLock mCpuLock;

    private BluetoothToRtklib mBtRover, mBtBase;
    private UsbToRtklib mUsbReceiver;
    private MobileMapperToRtklib mMobileMapperToRtklib;
    private boolean mBoolIsRunning = false;
    private boolean mBoolLocationServiceIsConnected = false;
    private boolean mBoolMockLocationsPref = false;
    private Location mLocationPrec = null;
    private long mLStartingTime = 0;
    private boolean mBoolGenerateGPXTrace = false;
    private GPXTrace mGpxTrace = null;
    private long mLProcessingCycle = 5;
    private String mSessionCode;

    private static final int POINT_AVERAGING_SECONDS = 30;
    private static final int POINT_MIN_FIX_EPOCHS = 10;
    private static final long POINT_SAMPLING_MS = 200;
    private static final double EARTH_RADIUS = 6378137.0;
    private final Handler mPointHandler = new Handler(Looper.getMainLooper());
    private PointAverager mPointAverager = null;
    @Override
    public void onCreate() {
        super.onCreate();

        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mCpuLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.v(TAG, "RtkNaviService restarted");
            processStart();
        } else {
            final String action = intent.getAction();

            if (ACTION_START.equals(action)) {
                if (intent.hasExtra(EXTRA_SESSION_CODE)) {
                    mSessionCode = intent.getStringExtra(EXTRA_SESSION_CODE);
                }
                else {
                    mSessionCode = String.valueOf(System.currentTimeMillis());
                }
                processStart();
            } else if (ACTION_STOP.equals(action)) {
                cancelPointAveraging();
                processStop();
                if (mHavePoint) {
                    createMapFile();
                }
            } else if (ACTION_STORE_POINT.equals(action))
            {
                if (intent.hasExtra(EXTRA_POINT_NAME))
                {
                    m_pointName = intent.getStringExtra(EXTRA_POINT_NAME);
                }
                storePoint(m_pointName);
            }
            else Log.e(TAG, "onStartCommand(): unknown action " + action);
        }
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return mBinder;
    }


    public final RtkServerStreamStatus getStreamStatus(
            RtkServerStreamStatus status) {
        return mRtkServer.getStreamStatus(status);
    }

    public final RtkServerObservationStatus getRoverObservationStatus(
            RtkServerObservationStatus status) {
        if (MainActivity.getDemoModeLocation().isInDemoMode() && mbStarted) {
            return MainActivity.getDemoModeLocation().getObservationStatus(status);
        } else {
            return mRtkServer.getRoverObservationStatus(status);
        }
    }

    public final RtkServerObservationStatus getBaseObservationStatus(
            RtkServerObservationStatus status) {
        return mRtkServer.getBaseObservationStatus(status);
    }

    public RtkControlResult getRtkStatus(RtkControlResult dst) {
        return mRtkServer.getRtkStatus(dst);
    }

    public static void loadSP3(String file) {
        if (mRtkServer != null)
            mRtkServer.readSP3(file);
    }

    public static void loadSatAnt(String file) {
        if (mRtkServer != null)
            mRtkServer.readSatAnt(file);
    }

    public boolean isServiceStarted() {
        return mRtkServer.getStatus() != RtkServerStreamStatus.STATE_CLOSE;
    }

    public int getServerStatus() {
        return mRtkServer.getStatus();
    }

    public Solution[] readSolutionBuffer() {
        return mRtkServer.readSolutionBuffer();
    }

    private void createMapFile() {
        try {
            String sessionDirectory = MainActivity.getAndCheckSessionDirectory(mSessionCode);
            File mapFile = new File(sessionDirectory + File.separator + mSessionCode + ".map");
            if (!mapFile.exists()) {
                mapFile.createNewFile();
            }
            FileWriter mapFileWriter = new FileWriter(mapFile, true);
            BufferedWriter out = new BufferedWriter(mapFileWriter);
            out.write(MM_MAP_HEADER + "\r\n");
            out.write(mSessionCode+".shp\r\n");
            out.close();
            mapFileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void storePoint(String pointName) {
        if (MainActivity.getDemoModeLocation().isInDemoMode()) {
            DemoModeLocation demoModeLocation = MainActivity.getDemoModeLocation();
            Position3d roverPos = demoModeLocation.getPosition();
            addPointToCRW(pointName, roverPos.getLat(), roverPos.getLon(), roverPos.getHeight(),
                    demoModeLocation.getNAccuracy(), demoModeLocation.getEAccuracy(),
                    demoModeLocation.getVAccuracy(), demoModeLocation.getNbSat(), 0);
            return;
        }
        if (mPointAverager != null) {
            Toast.makeText(this, R.string.point_averaging_busy, Toast.LENGTH_SHORT).show();
            return;
        }
        Log.i(TAG, "Averaging point " + pointName);
        mPointAverager = new PointAverager(pointName);
        Toast.makeText(this, getString(R.string.point_averaging_started, pointName,
                POINT_AVERAGING_SECONDS), Toast.LENGTH_LONG).show();
        mPointHandler.post(mPointAverager);
    }

    private void cancelPointAveraging() {
        if (mPointAverager != null) {
            mPointHandler.removeCallbacks(mPointAverager);
            mPointAverager = null;
        }
    }

    /**
     * Averages the FIX solutions over POINT_AVERAGING_SECONDS so a ground control
     * point is not taken from a single epoch
     */
    private class PointAverager implements Runnable {
        private final String mName;
        private final long mEndTime;
        private final RtkControlResult mResult = new RtkControlResult();
        private long mLastSolutionTime = -1;
        private int mNbEpochs = 0, mNbFix = 0, mNbSat = 0;
        private double mLat0, mLon0, mHeight0;
        // Sums of offsets from the first FIX epoch, in metres (north, east, up)
        private double mSumN, mSumE, mSumU, mSumN2, mSumE2, mSumU2;

        PointAverager(String name) {
            mName = name;
            mEndTime = SystemClock.elapsedRealtime() + POINT_AVERAGING_SECONDS * 1000L;
        }

        @Override
        public void run() {
            Solution solution = getRtkStatus(mResult).getSolution();
            long solutionTime = solution.getTime().getGpsTimeMillis();
            if (solutionTime != mLastSolutionTime
                    && RtkCommon.norm(solution.getPosition().getValues()) > 0.0) {
                mLastSolutionTime = solutionTime;
                mNbEpochs++;
                if (solution.getSolutionStatus() == SolutionStatus.FIX) {
                    addFix(RtkCommon.ecef2pos(solution.getPosition()), solution.getNs());
                }
            }
            if (SystemClock.elapsedRealtime() < mEndTime) {
                mPointHandler.postDelayed(this, POINT_SAMPLING_MS);
            } else {
                mPointAverager = null;
                finish();
            }
        }

        private void addFix(Position3d pos, int nbSat) {
            if (mNbFix == 0) {
                mLat0 = pos.getLat();
                mLon0 = pos.getLon();
                mHeight0 = pos.getHeight();
            }
            double n = (pos.getLat() - mLat0) * EARTH_RADIUS;
            double e = (pos.getLon() - mLon0) * EARTH_RADIUS * Math.cos(mLat0);
            double u = pos.getHeight() - mHeight0;
            mSumN += n; mSumE += e; mSumU += u;
            mSumN2 += n * n; mSumE2 += e * e; mSumU2 += u * u;
            mNbFix++;
            mNbSat = nbSat;
        }

        private double stdDev(double sum, double sum2) {
            double mean = sum / mNbFix;
            return Math.sqrt(Math.max(0.0, sum2 / mNbFix - mean * mean));
        }

        private void finish() {
            Log.i(TAG, "Point " + mName + ": " + mNbFix + " FIX of " + mNbEpochs + " epochs");
            if (mNbFix < POINT_MIN_FIX_EPOCHS) {
                Toast.makeText(RtkNaviService.this, getString(R.string.point_averaging_no_fix,
                        mName, mNbFix, mNbEpochs), Toast.LENGTH_LONG).show();
                return;
            }
            double lat = mLat0 + mSumN / mNbFix / EARTH_RADIUS;
            double lon = mLon0 + mSumE / mNbFix / (EARTH_RADIUS * Math.cos(mLat0));
            double height = mHeight0 + mSumU / mNbFix;
            double sdN = stdDev(mSumN, mSumN2);
            double sdE = stdDev(mSumE, mSumE2);
            double sdU = stdDev(mSumU, mSumU2);
            addPointToCRW(mName, lat, lon, height, sdN, sdE, sdU, mNbSat, mNbFix);
            Toast.makeText(RtkNaviService.this, getString(R.string.point_averaging_stored,
                    mName, mNbFix, mNbEpochs, Math.max(sdN, sdE) * 100, sdU * 100),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void addPointToCRW(String pointName, double lat, double lon, double height,
                               double sdN, double sdE, double sdU, int nbSat, int nbFix) {
        double dLat = Math.toDegrees(lat);
        double dLon = Math.toDegrees(lon);

        GpsTime gpsTime = new GpsTime();
        gpsTime.setTime(System.currentTimeMillis());

        String currentLine = String.format(Locale.ROOT,"%s,%s,%.6f,%.6f,%.3f,%d,%.3f,%.3f,%.1f\n", gpsTime.getStringGpsWeek(), gpsTime.getStringGpsTOW(), dLon, dLat, height, 10, 0D, 0D, 0D);
        String szSessionDirectory = MainActivity.getAndCheckSessionDirectory(mSessionCode);
        try {
            File crwFile = new File(szSessionDirectory +File.separator+ mSessionCode+".crw");
            if (!crwFile.exists()) {
                crwFile.createNewFile();
            }
            FileWriter crwFileWriter = new FileWriter(crwFile, true);
            BufferedWriter out = new BufferedWriter(crwFileWriter);
            out.write(currentLine);
            out.close();
            crwFileWriter.close();
            mHavePoint = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        // The .crw keeps 6 decimals for MobileMapper; the point store keeps full precision
        PointStore.get().add(new PointStore.Point(pointName, dLat, dLon, height, sdN, sdE, sdU,
                nbFix, nbSat, gpsTime.getGpsWeek(), (long)gpsTime.getSecondsOfWeek()));
    }

    /**
     * Class used for the client Binder. Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class RtkNaviServiceBinder extends Binder {
        public RtkNaviService getService() {
            // Return this instance of UpdateDbService so clients can call
            // public methods
            return RtkNaviService.this;
        }
    }

    public void processStart() {
        final RtkServerSettings settings;

        mbStarted = true;
        try {
            MainActivity.getDemoModeLocation().reset();
            if (MainActivity.getDemoModeLocation().isInDemoMode()) {
                MainActivity.getDemoModeLocation().startDemoMode();
            }
        } catch (NullPointerException e) {
            // TODO Auto-generated catch block
        }
        if (isServiceStarted()) return;

        settings = SettingsHelper.loadSettings(this);

        mRtkServer.setServerSettings(settings);

        mLStartingTime = System.currentTimeMillis();
        if (!mRtkServer.start()) {
            Log.e(TAG, "rtkSrvStart() error");
            return;
        }

        SharedPreferences processPrefs = this.getBaseContext().getSharedPreferences(ProcessingOptions1Fragment.SHARED_PREFS_NAME, 0);
        String ephemVa = processPrefs.getString(ProcessingOptions1Fragment.KEY_SAT_EPHEM_CLOCK,"");
        EphemerisOption ephemerisOption = EphemerisOption.valueOf(ephemVa);
        PreciseEphemerisProvider provider = ephemerisOption.getProvider();
        if (provider != null) {
            if (PreciseEphemerisDownloader.isCurrentOrbitsPresent(provider)) {
                loadSP3(PreciseEphemerisDownloader.getCurrentOrbitFile(provider).getAbsolutePath());
            }
        }

        startBluetoothPipes();
        startUsb();
        startMobileMapper();

        if (!mCpuLock.isHeld()) mCpuLock.acquire();

        Notification notification = createForegroundNotification();
        startForeground(NOTIFICATION, notification);

        SharedPreferences prefs = this.getBaseContext().getSharedPreferences(SolutionOutputSettingsFragment.SHARED_PREFS_NAME, 0);
        mBoolMockLocationsPref = prefs.getBoolean(SolutionOutputSettingsFragment.KEY_OUTPUT_MOCK_LOCATION, false);
        prefs = this.getBaseContext().getSharedPreferences(OutputGPXTraceFragment.SHARED_PREFS_NAME, 0);
        mBoolGenerateGPXTrace = prefs.getBoolean(OutputGPXTraceFragment.KEY_ENABLE, false);
        prefs = this.getBaseContext().getSharedPreferences(ProcessingOptions1Fragment.SHARED_PREFS_NAME, 0);
        mLProcessingCycle = Long.valueOf(prefs.getString(ProcessingOptions1Fragment.KEY_PROCESSING_CYCLE, "5"));
        if (mBoolMockLocationsPref) {
            // Since Android 6 ALLOW_MOCK_LOCATION is always "0"; the app must instead be selected
            // as the mock location app in developer options, otherwise addTestProvider() throws SecurityException
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M && "0".equals(Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ALLOW_MOCK_LOCATION))) {
                Log.e(RTK_GPS_MOCK_LOC_SERVICE, "Mock Location is not allowed");
                mBoolMockLocationsPref = false;
            } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // A service cannot request permissions; MainActivity asks for them on startup
                Log.e(RTK_GPS_MOCK_LOC_SERVICE, "Mock Location needs ACCESS_FINE_LOCATION permission");
                Toast.makeText(this, R.string.permission_to_use_gps, Toast.LENGTH_LONG).show();
                mBoolMockLocationsPref = false;
            } else {
                // Connect to Location Services
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                try {
                    try {
                        locationManager.addTestProvider(GPS_PROVIDER, false, false,
                                false, false, true, false, true, 0, Criteria.ACCURACY_FINE);
                    } catch (IllegalArgumentException e) {
                        Log.e(RTK_GPS_MOCK_LOC_SERVICE, "Mock Location gps provider already exist");
                    }
                    locationManager.setTestProviderEnabled(GPS_PROVIDER, true);
                    locationManager.requestLocationUpdates(GPS_PROVIDER, 0, 0, this);
                    Log.i(RTK_GPS_MOCK_LOC_SERVICE,"Mock Location service was started");
                } catch (SecurityException e) {
                    Log.e(RTK_GPS_MOCK_LOC_SERVICE, "Mock Location is not allowed: select this app as mock location app in developer options", e);
                    Toast.makeText(this, "Mock Location is not allowed: select RtkGps+ as mock location app in developer options", Toast.LENGTH_LONG).show();
                    mBoolMockLocationsPref = false;
                }
            }
        }
        GeoidModel model = GeoidModel.valueOf( prefs.getString(SolutionOutputSettingsFragment.KEY_GEOID_MODEL,GeoidModel.EMBEDDED.name()) );
        rtkCommon = new RtkCommon(model);
        //load satellite antennas
        loadSatAnt(MainActivity.getApplicationDirectory()+File.separator+"files"+File.separator+"data"+File.separator+"igs05.atx");
        mBoolIsRunning = true;
    }


    private void processStop() {
        mBoolIsRunning = false;
        mbStarted = false;
        try {
            if (MainActivity.getDemoModeLocation().isInDemoMode())
            {
                MainActivity.getDemoModeLocation().stopDemoMode();
            }
            if (mBoolMockLocationsPref)
            {
                LocationManager lm = (LocationManager) getSystemService(
                        Context.LOCATION_SERVICE);
                      lm.removeTestProvider(GPS_PROVIDER);
            }
        } catch (Exception e) {
            //nothing to do it may appear normally on brutal ending because MainActivity was already shutdown
        }
        stop();
        finalizeGpxTrace();
//        syncDropbox();
        stopSelf();
    }

    private void finalizeGpxTrace() {
        if (mBoolGenerateGPXTrace && (mGpxTrace != null))
        {
            SharedPreferences prefs= this.getBaseContext().getSharedPreferences(OutputGPXTraceFragment.SHARED_PREFS_NAME, 0);
            String szFilename = prefs.getString(OutputGPXTraceFragment.KEY_FILENAME, "gpxtrace.gpx");
            if (szFilename.length()>0)
            {
                String szPath = MainActivity.getFileStorageDirectory() + File.separator + szFilename;
                mGpxTrace.writeFile(szPath);
            }
        }

    }

    private String getZipFilename(String filename)
    {
        return filename+".zip";
    }
/*
    private String insertDateTimeInDropboxFilename(String file)
    {
        File _file = new File(file);
        if (!file.contains("%Y") && !file.contains("%m")
                && !file.contains("%d")
                && !file.contains("%h")
                && !file.contains("%M")
                && !file.contains("%S"))
        {
            SimpleDateFormat sdtFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            sdtFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date dt = new Date(mLStartingTime);
            String szDate = sdtFormat.format(dt);
            int dotposition= _file.getName().lastIndexOf(".");
            String remoteFileName = _file.getName().substring(0, dotposition)+"-"+szDate+"."+_file.getName().substring(dotposition + 1, _file.getName().length());
            return remoteFileName;
        }else{
            return _file.getName();
        }
    }

    private void addToDropboxIfNeeded(SharedPreferences prefs, ArrayList<dpFile> alDropboxed, String file)
    {
        if(prefs.getBoolean(StreamFileClientFragment.KEY_SYNCDROPBOX, false)
                && prefs.getBoolean(StreamFileClientFragment.KEY_ENABLE, false))
            {
                if (prefs.getBoolean(StreamFileClientFragment.KEY_ZIPBEFORESYNCING, false))
                {
                    String[] _files = {file};
                    String zipFileName = getZipFilename(file);
                    ZipHelper.zip(_files, zipFileName);
                    String fileName = insertDateTimeInDropboxFilename(zipFileName);
                    alDropboxed.add(new dpFile(zipFileName,fileName));
                }else{
                    alDropboxed.add(new dpFile(file,insertDateTimeInDropboxFilename(file)));
                }
            }
    }
    private void syncDropbox()
    {
        DbxAccountManager mDbxAcctMgr;
        mDbxAcctMgr = DbxAccountManager.getInstance(getApplicationContext(), MainActivity.APP_KEY, MainActivity.APP_SECRET);
        if (mDbxAcctMgr.hasLinkedAccount())
        {
            ArrayList<dpFile> alDropboxed = new ArrayList<dpFile>();
            //Log rover
            if ( mRtkServer.getServerSettings().getLogRover().getType() == StreamType.FILE)
                {
                SharedPreferences prefs= this.getBaseContext().getSharedPreferences(LogRoverFragment.SHARED_PREFS_NAME, 0);
                addToDropboxIfNeeded(prefs, alDropboxed, mRtkServer.getServerSettings().getLogRover().getPath());
                }
            //Log base
            if ( mRtkServer.getServerSettings().getLogBase().getType() == StreamType.FILE)
            {
                SharedPreferences prefs= this.getBaseContext().getSharedPreferences(LogBaseFragment.SHARED_PREFS_NAME, 0);
                addToDropboxIfNeeded(prefs, alDropboxed, mRtkServer.getServerSettings().getLogBase().getPath());
            }
            //Solution 1
            if ( mRtkServer.getServerSettings().getOutputSolution1().getType() == StreamType.FILE)
            {
                SharedPreferences prefs= this.getBaseContext().getSharedPreferences(OutputSolution1Fragment.SHARED_PREFS_NAME, 0);
                addToDropboxIfNeeded(prefs, alDropboxed, mRtkServer.getServerSettings().getOutputSolution1().getPath());
            }
            //Solution 2
            if ( mRtkServer.getServerSettings().getOutputSolution2().getType() == StreamType.FILE)
            {
                SharedPreferences prefs= this.getBaseContext().getSharedPreferences(OutputSolution2Fragment.SHARED_PREFS_NAME, 0);
                addToDropboxIfNeeded(prefs, alDropboxed, mRtkServer.getServerSettings().getOutputSolution2().getPath());
            }
            //GPX Trace (not zippable)
            SharedPreferences prefs= this.getBaseContext().getSharedPreferences(OutputGPXTraceFragment.SHARED_PREFS_NAME, 0);
            if((prefs.getBoolean(OutputGPXTraceFragment.KEY_SYNCDROPBOX, false))
                    && (prefs.getBoolean(OutputGPXTraceFragment.KEY_ENABLE, false))
                    && (mGpxTrace != null))
                {
                    String szFilename = prefs.getString(OutputGPXTraceFragment.KEY_FILENAME, "");
                    if (szFilename.length()>0)
                    {
                        String szPath = MainActivity.getFileStorageDirectory() + File.separator + szFilename;
                        alDropboxed.add(new dpFile(szPath,insertDateTimeInDropboxFilename(szFilename)));
                    }

                }

             for(int i=0;i<alDropboxed.size();i++)
             {
                 String szCurrentPath = alDropboxed.get(i).getLocalFilenameWithPath();

                            if (!szCurrentPath.contains("%Y") && !szCurrentPath.contains("%m")
                                    && !szCurrentPath.contains("%d")
                                    && !szCurrentPath.contains("%h")
                                    && !szCurrentPath.contains("%M")
                                    && !szCurrentPath.contains("%S"))
                            {
                                DbxFileSystem dbxFs;
                                try {
                                    dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
                                    File f = new File(szCurrentPath);
                                    String remoteFileName = alDropboxed.get(i).getRemoteFilename();
                                    DbxFile remoteFile = dbxFs.create(new DbxPath(remoteFileName));
                                    remoteFile.writeFromExistingFile(f, false);
                                    remoteFile.close();
                                } catch (Unauthorized e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                } catch (InvalidPathException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                } catch (DbxException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }

                                Log.i(TAG, "Sync to dropbox: "+szCurrentPath);
                            }
             }
        }
    }
*/
    private void stop() {
        stopForeground(true);
        if (mCpuLock.isHeld()) mCpuLock.release();

        if (isServiceStarted()) {
            mRtkServer.stop();

            stopBluetoothPipes();
            stopUsb();
            stopMobileMapper();
            // Tell the user we stopped.
            Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT)
            .show();
        }
    }

    @Override
    public void onDestroy() {
        stop();
    }

    private Notification createForegroundNotification() {
        CharSequence text = getText(R.string.local_service_started);

        // Since Android 8 a notification without a valid channel makes startForeground() fail
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    getText(R.string.local_service_label), NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }

        // The PendingIntent to launch our activity if the user selects this
        // notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        builder.setContentTitle(getText(R.string.local_service_label));
        builder.setContentText(text);
        builder.setContentIntent(contentIntent);
        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setOngoing(true);
        builder.setNumber(100);
        builder.setAutoCancel(false);

        return builder.build();
    }

    private class BluetoothCallbacks implements BluetoothToRtklib.Callbacks {

        private int mStreamId;
        private final Handler mHandler;

        public BluetoothCallbacks(int streamId) {
            mStreamId = streamId;
            mHandler = new Handler();
        }

        @Override
        public void onConnected() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(RtkNaviService.this, R.string.bluetooth_connected,
                            Toast.LENGTH_SHORT).show();
                }
            });

            new Thread() {
                @Override
                public void run() {
                    mRtkServer.sendStartupCommands(mStreamId);
                }
            }.start();
        }

        @Override
        public void onStopped() {
        }

        @Override
        public void onConnectionLost() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(RtkNaviService.this, R.string.bluetooth_connection_lost,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

    }

    private class UsbCallbacks implements UsbToRtklib.Callbacks {

        private int mStreamId;
        private final Handler mHandler;

        public UsbCallbacks(int streamId) {
            mStreamId = streamId;
            mHandler = new Handler();
        }

        @Override
        public void onConnected() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(RtkNaviService.this, R.string.usb_connected,
                            Toast.LENGTH_SHORT).show();
                }
            });

            new Thread() {
                @Override
                public void run() {
                    mRtkServer.sendStartupCommands(mStreamId);
                }
            }.run();

        }

        @Override
        public void onStopped() {
        }

        @Override
        public void onConnectionLost() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(RtkNaviService.this, R.string.usb_connection_lost,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

    }


    private void startBluetoothPipes() {
        final TransportSettings roverSettngs, baseSettings;

        RtkServerSettings settings = mRtkServer.getServerSettings();

        roverSettngs = settings.getInputRover().getTransportSettings();

        if (roverSettngs.getType() == StreamType.BLUETOOTH) {
            StreamBluetoothFragment.Value btSettings = (Value)roverSettngs;
            mBtRover = new BluetoothToRtklib(btSettings.getAddress().toUpperCase(), btSettings.getPath());
            mBtRover.setCallbacks(new BluetoothCallbacks(RtkServer.RECEIVER_ROVER));
            mBtRover.start();
        }else {
            mBtRover = null;
        }

        baseSettings = settings.getInputBase().getTransportSettings();
        if (baseSettings.getType() == StreamType.BLUETOOTH) {
            StreamBluetoothFragment.Value btSettings = (Value)baseSettings;
            mBtBase = new BluetoothToRtklib(btSettings.getAddress(), btSettings.getPath());
            mBtBase.setCallbacks(new BluetoothCallbacks(RtkServer.RECEIVER_BASE));
            mBtBase.start();
        }else {
            mBtBase = null;
        }
    }

    private void stopBluetoothPipes() {
        if (mBtRover != null) mBtRover.stop();
        if (mBtBase != null) mBtBase.stop();
        mBtRover = null;
        mBtBase = null;
    }

    private void startMobileMapper()
    {
        RtkServerSettings settings = mRtkServer.getServerSettings();
        final TransportSettings roverSettings;
        roverSettings = settings.getInputRover().getTransportSettings();
        if (roverSettings.getType() == StreamType.MOBILEMAPPER) {
            StreamMobileMapperFragment.Value mobileMapperSettings = (gpsplus.rtkgps.settings.StreamMobileMapperFragment.Value)roverSettings;
            mMobileMapperToRtklib = new MobileMapperToRtklib(this, mobileMapperSettings, mSessionCode);
            mMobileMapperToRtklib.start();
        }
    }

    private void stopMobileMapper()
    {
        if (mMobileMapperToRtklib != null) {
            mMobileMapperToRtklib.stop();
            mMobileMapperToRtklib = null;
        }
    }

    private void startUsb() {
        RtkServerSettings settings = mRtkServer.getServerSettings();

        {
            final TransportSettings roverSettings;
            roverSettings = settings.getInputRover().getTransportSettings();
            if (roverSettings.getType() == StreamType.USB) {
                StreamUsbFragment.Value usbSettings = (gpsplus.rtkgps.settings.StreamUsbFragment.Value)roverSettings;
                mUsbReceiver = new UsbToRtklib(this, usbSettings.getPath());
                mUsbReceiver.setSerialLineConfiguration(usbSettings.getSerialLineConfiguration());
                mUsbReceiver.setCallbacks(new UsbCallbacks(RtkServer.RECEIVER_ROVER));
                mUsbReceiver.start();
                return;
            }
        }

        {
            final TransportSettings baseSettings;
            baseSettings = settings.getInputBase().getTransportSettings();
            if (baseSettings.getType() == StreamType.USB) {
                StreamUsbFragment.Value usbSettings = (gpsplus.rtkgps.settings.StreamUsbFragment.Value)baseSettings;
                mUsbReceiver = new UsbToRtklib(this, usbSettings.getPath());
                mUsbReceiver.setSerialLineConfiguration(usbSettings.getSerialLineConfiguration());
                mUsbReceiver.setCallbacks(new UsbCallbacks(RtkServer.RECEIVER_BASE));
                mUsbReceiver.start();
                return;
            }
        }
    }

    private void stopUsb() {
        if (mUsbReceiver != null) {
            mUsbReceiver.stop();
            mUsbReceiver = null;
        }
    }

    /* (non-Javadoc)
     * @see android.app.IntentService#onHandleIntent(android.content.Intent)
     */
    @Override
    protected void onHandleIntent(Intent arg0) {

        while (mBoolIsRunning){


            {
                try {
                    if (mBoolMockLocationsPref || mBoolGenerateGPXTrace)
                    {
                        RtkControlResult result = getRtkStatus(null);
                        Solution solution = result.getSolution();
                        Position3d positionECEF = solution.getPosition();

                        if (RtkCommon.norm(positionECEF.getValues()) > 0.0)
                        {
                            Position3d positionLatLon = RtkCommon.ecef2pos(positionECEF);

                            if (mBoolMockLocationsPref)
                            {
                                Location currentLocation = createLocation(Math.toDegrees(positionLatLon.getLat()), Math.toDegrees(positionLatLon.getLon()),positionLatLon.getHeight(), 1f);
                                if (mBoolLocationServiceIsConnected || true) // TO be corrected for Google maps API
                                    {
                                     // provide the new location
                                     //   Log.i(RTK_GPS_MOCK_LOCATION_SERVICE,"Mock location is "+currentLocation.getLatitude()+" "+currentLocation.getLongitude());
                                        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                                        try {
                                            locationManager.setTestProviderLocation(GPS_PROVIDER, currentLocation);
                                        }
                                        catch (IllegalArgumentException e)
                                        {
                                            Log.i(RTK_GPS_MOCK_LOC_SERVICE,"Your device does not support MOCK_LOCATION with provider:" +GPS_PROVIDER);
                                        }
                                    }
                            }
                            if (mBoolGenerateGPXTrace)
                            {
                                if (mGpxTrace  == null)
                                {
                                    mGpxTrace = new GPXTrace();
                                }
                               mGpxTrace.addPoint(Math.toDegrees(positionLatLon.getLat()),
                                                   Math.toDegrees(positionLatLon.getLon()),
                                                   positionLatLon.getHeight(),
                                                   rtkCommon.getAltitudeCorrection(positionLatLon.getLat(), positionLatLon.getLon()),
                                                   solution.getTime());
                            }
                        }
                    }
                    Thread.sleep(mLProcessingCycle*1000);

                } catch (InterruptedException e) {
                    mBoolIsRunning = false;
                    Log.i(RTK_GPS_MOCK_LOC_SERVICE,"Mock location service was interrupted");
                }
            }
          }
    }

    @SuppressLint("NewApi")
    public Location createLocation(double lat, double lng, double alt, float accuracy) {
        // Create a new Location
        Location newLocation = new Location(GPS_PROVIDER);
        newLocation.setLatitude(lat);
        newLocation.setLongitude(lng);
        newLocation.setAccuracy(accuracy);
        newLocation.setAltitude(alt);
        newLocation.setTime(System.currentTimeMillis());


         try{

            Method m = newLocation.getClass().getMethod("setElapsedRealtimeNanos", long.class);
            if (m != null){
                newLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtime());
            }
        }
        catch(Exception e)
        {
            //nothing to do unsupported before API17
        }
        newLocation.setSpeed(0f);

        if (mLocationPrec == null){
            newLocation.setBearing(0f);
            newLocation.setSpeed(0f);
            mLocationPrec = newLocation;
        }else
        {
            float fBearing = mLocationPrec.bearingTo(newLocation)+180;
            if (fBearing>360)
            {
                fBearing -= 360;
            }
            float fSpeed = (mLocationPrec.distanceTo(newLocation))/((newLocation.getTime()-mLocationPrec.getTime())/1000);
            newLocation.setBearing(fBearing);
            newLocation.setSpeed(fSpeed);
            mLocationPrec = newLocation;
        }

        return newLocation;
    }
    @Override
    public void onLocationChanged(Location arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderDisabled(String arg0) {
        mBoolLocationServiceIsConnected = false;
        Log.i(RTK_GPS_MOCK_LOC_SERVICE,"Mock location provide "+GPS_PROVIDER+" was disabled");

    }

    @Override
    public void onProviderEnabled(String arg0) {
        mBoolLocationServiceIsConnected = true;
        Log.i(RTK_GPS_MOCK_LOC_SERVICE,"Mock location provide "+GPS_PROVIDER+" is enabled");

    }

    @Override
    public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
        // TODO Auto-generated method stub

    }


}
