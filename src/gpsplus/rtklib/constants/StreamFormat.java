package gpsplus.rtklib.constants;

import gpsplus.rtkgps.R;

/**
 * Stream format STRFMT_XXX
 */
public enum StreamFormat implements IHasRtklibId {

    /** stream format: RTCM 2 */
    RTCM2(0, R.string.strfmt_rtcm2),
    /** stream format: RTCM 3 */
    RTCM3(1, R.string.strfmt_rtcm3),
    /** stream format: NovAtel OEMV/4 */
    OEM4(2, R.string.strfmt_oem4),
    /** stream format: u-blox LEA-*T */
    UBX(4, R.string.strfmt_ubx),
    /** stream format: Swift Navigation SBP */
    SBP(5, R.string.strfmt_sbp),
    /** stream format: Hemisphere */
    CRES(6, R.string.strfmt_cres),
    /** stream format: SkyTraq S1315F */
    STQ(7, R.string.strfmt_stq),
    /** stream format: JAVAD GRIL/GREIS */
    JAVAD(8, R.string.strfmt_javad),
    /** stream format: NVS NVC08C */
    NVS(9, R.string.strfmt_nvs),
    /** stream format: BINEX */
    BINEX(10, R.string.strfmt_binex),
    /** Trimble RT17 */
    RT17(11, R.string.strfmt_rt17),
    /** stream format Septentrio */
    SEPT(12, R.string.strfmt_sept),
    /** stream format: Unicore */
    UNICORE(14, R.string.strfmt_unicore),
    /** stream format: RINEX */
    RINEX(15, R.string.strfmt_rinex),
    /** stream format: SP3 */
    SP3(16, R.string.strfmt_sp3),
    /** stream format: RINEX CLK */
    RNXCLK(17, R.string.strfmt_rnxclk),
    /** stream format: SBAS messages */
    SBAS(18, R.string.strfmt_sbas),
    /** stream format: NMEA 0183 */
    NMEA(19, R.string.strfmt_nmea)

    ;

    private final int mRtklibId;
    private final int mNameResId;

    private StreamFormat(int rtklibId, int nameResId) {
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

    public static StreamFormat valueOf(int rtklibId) {
        for (StreamFormat v: values()) {
            if (v.mRtklibId == rtklibId) return v;
        }
        throw new IllegalArgumentException();
    }
}
