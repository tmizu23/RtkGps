package gpsplus.rtklib.constants;

import gpsplus.rtkgps.R;


/** Stream type STR_XXX */
public enum StreamType implements IHasRtklibId {

    /** stream type: none */
    NONE(0, R.string.str_none),

    /** stream type: serial */
    SERIAL(1, R.string.str_serial),

    /** stream type: file */
    FILE(2, R.string.str_file),

    /** stream type: TCP server */
    TCPSVR(3, R.string.str_tcpsvr),

    /** stream type: TCP client */
    TCPCLI(4, R.string.str_tcpcli),

    /** stream type: NTRIP server */
    NTRIPSVR(5, R.string.str_ntripsvr),

    /** stream type: NTRIP client */
    NTRIPCLI(6, R.string.str_ntripcli),

    /** stream type: ftp */
    FTP(7, R.string.str_ftp),

    /** stream type: http */
    HTTP(8, R.string.str_http),

    /** stream type: UDP server */
    UDP(10, R.string.str_udp),

    UDPCLI(11, R.string.str_udpcli),

    /** stream type: unix server (jni/RTKLIB-unixsocket.patch) */
    UNIXSVR(13, R.string.str_unixsvr),

    // XXX
    BLUETOOTH(13, R.string.str_bluetooth),

    USB(13, R.string.str_usb),

    // XXX
    MOBILEMAPPER(13, R.string.str_mobilemapper),

    ;

    private final int mRtklibId;
    private final int mNameResId;

    private StreamType(int rtklibId, int nameResId) {
        mRtklibId = rtklibId;
        mNameResId = nameResId;
    }

    @Override
    public int getRtklibId() {
        return mRtklibId;
    }

    @Override
    public int getNameResId() {
        return mNameResId;
    }

    public static StreamType valueOf(int rtklibId) {
        for (StreamType v: values()) {
            if (v.mRtklibId == rtklibId) return v;
        }
        throw new IllegalArgumentException();
    }
}
