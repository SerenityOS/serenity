/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 *
 *  (C) Copyright IBM Corp. 1999 All Rights Reserved.
 *  Copyright 1997 The Open Group Research Institute.  All rights reserved.
 */

package sun.security.krb5.internal;

import sun.security.krb5.Asn1Exception;
import sun.security.krb5.Config;
import sun.security.krb5.KrbException;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

import java.io.IOException;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Implements the ASN.1 KerberosTime type. This is an immutable class.
 *
 * {@code KerberosTime ::= GeneralizedTime} -- with no fractional seconds
 *
 * The timestamps used in Kerberos are encoded as GeneralizedTimes. A
 * KerberosTime value shall not include any fractional portions of the
 * seconds.  As required by the DER, it further shall not include any
 * separators, and it shall specify the UTC time zone (Z).
 *
 * <p>
 * This definition reflects the Network Working Group RFC 4120
 * specification available at
 * <a href="http://www.ietf.org/rfc/rfc4120.txt">
 * http://www.ietf.org/rfc/rfc4120.txt</a>.
 *
 * The implementation also includes the microseconds info so that the
 * same class can be used as a precise timestamp in Authenticator etc.
 */

public class KerberosTime {

    private final long kerberosTime; // milliseconds since epoch, Date.getTime()
    private final int  microSeconds; // last 3 digits of the real microsecond

    // The time when this class is loaded. Used in setNow()
    private static long initMilli = System.currentTimeMillis();
    private static long initMicro = System.nanoTime() / 1000;

    private static boolean DEBUG = Krb5.DEBUG;

    // Do not make this public. It's a little confusing that micro
    // is only the last 3 digits of microsecond.
    private KerberosTime(long time, int micro) {
        kerberosTime = time;
        microSeconds = micro;
    }

    /**
     * Creates a KerberosTime object from milliseconds since epoch.
     */
    public KerberosTime(long time) {
        this(time, 0);
    }

    // Warning: called by NativeCreds.c and nativeccache.c
    public KerberosTime(String time) throws Asn1Exception {
        this(toKerberosTime(time), 0);
    }

    private static long toKerberosTime(String time) throws Asn1Exception {
        // ASN.1 GeneralizedTime format:

        // "19700101000000Z"
        //  |   | | | | | |
        //  0   4 6 8 | | |
        //           10 | |
        //             12 |
        //               14

        if (time.length() != 15)
            throw new Asn1Exception(Krb5.ASN1_BAD_TIMEFORMAT);
        if (time.charAt(14) != 'Z')
            throw new Asn1Exception(Krb5.ASN1_BAD_TIMEFORMAT);
        int year = Integer.parseInt(time.substring(0, 4));
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.clear(); // so that millisecond is zero
        calendar.set(year,
                     Integer.parseInt(time.substring(4, 6)) - 1,
                     Integer.parseInt(time.substring(6, 8)),
                     Integer.parseInt(time.substring(8, 10)),
                     Integer.parseInt(time.substring(10, 12)),
                     Integer.parseInt(time.substring(12, 14)));
        return calendar.getTimeInMillis();
    }

    /**
     * Creates a KerberosTime object from a Date object.
     */
    public KerberosTime(Date time) {
        this(time.getTime(), 0);
    }

    /**
     * Creates a KerberosTime object from an Instant object
     */
    public KerberosTime(Instant instant) {
        this(instant.getEpochSecond()*1000 + instant.getNano()/1000000L,
                instant.getNano()/1000%1000);
    }

    /**
     * Creates a KerberosTime object for now. It uses System.nanoTime()
     * to get a more precise time than "new Date()".
     */
    public static KerberosTime now() {
        long newMilli = System.currentTimeMillis();
        long newMicro = System.nanoTime() / 1000;
        long microElapsed = newMicro - initMicro;
        long calcMilli = initMilli + microElapsed/1000;
        if (calcMilli - newMilli > 100 || newMilli - calcMilli > 100) {
            if (DEBUG) {
                System.out.println("System time adjusted");
            }
            initMilli = newMilli;
            initMicro = newMicro;
            return new KerberosTime(newMilli, 0);
        } else {
            return new KerberosTime(calcMilli, (int)(microElapsed % 1000));
        }
    }

    /**
     * Returns a string representation of KerberosTime object.
     * @return a string representation of this object.
     */
    public String toGeneralizedTimeString() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.clear();

        calendar.setTimeInMillis(kerberosTime);
        return String.format("%04d%02d%02d%02d%02d%02dZ",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND));
    }

    /**
     * Encodes this object to a byte array.
     * @return a byte array of encoded data.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     */
    public byte[] asn1Encode() throws Asn1Exception, IOException {
        DerOutputStream out = new DerOutputStream();
        out.putGeneralizedTime(this.toDate());
        return out.toByteArray();
    }

    public long getTime() {
        return kerberosTime;
    }

    public Date toDate() {
        return new Date(kerberosTime);
    }

    public int getMicroSeconds() {
        int temp_int = (int) ((kerberosTime % 1000L) * 1000L);
        return temp_int + microSeconds;
    }

    /**
     * Returns a new KerberosTime object with the original seconds
     * and the given microseconds.
     */
    public KerberosTime withMicroSeconds(int usec) {
        return new KerberosTime(
                kerberosTime - kerberosTime%1000L + usec/1000L,
                usec%1000);
    }

    private boolean inClockSkew(int clockSkew) {
        return java.lang.Math.abs(kerberosTime - System.currentTimeMillis())
                <= clockSkew * 1000L;
    }

    public boolean inClockSkew() {
        return inClockSkew(getDefaultSkew());
    }

    public boolean greaterThanWRTClockSkew(KerberosTime time, int clockSkew) {
        if ((kerberosTime - time.kerberosTime) > clockSkew * 1000L)
            return true;
        return false;
    }

    public boolean greaterThanWRTClockSkew(KerberosTime time) {
        return greaterThanWRTClockSkew(time, getDefaultSkew());
    }

    public boolean greaterThan(KerberosTime time) {
        return kerberosTime > time.kerberosTime ||
            kerberosTime == time.kerberosTime &&
                    microSeconds > time.microSeconds;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof KerberosTime)) {
            return false;
        }

        return kerberosTime == ((KerberosTime)obj).kerberosTime &&
                microSeconds == ((KerberosTime)obj).microSeconds;
    }

    public int hashCode() {
        int result = 37 * 17 + (int)(kerberosTime ^ (kerberosTime >>> 32));
        return result * 17 + microSeconds;
    }

    public boolean isZero() {
        return kerberosTime == 0 && microSeconds == 0;
    }

    public int getSeconds() {
        return (int) (kerberosTime / 1000L);
    }

    /**
     * Parse (unmarshal) a kerberostime from a DER input stream.  This form
     * parsing might be used when expanding a value which is part of
     * a constructed sequence and uses explicitly tagged type.
     *
     * @exception Asn1Exception on error.
     * @param data the Der input stream value, which contains
     *             one or more marshaled value.
     * @param explicitTag tag number.
     * @param optional indicates if this data field is optional
     * @return an instance of KerberosTime.
     *
     */
    public static KerberosTime parse(
            DerInputStream data, byte explicitTag, boolean optional)
            throws Asn1Exception, IOException {
        if ((optional) && (((byte)data.peekByte() & (byte)0x1F)!= explicitTag))
            return null;
        DerValue der = data.getDerValue();
        if (explicitTag != (der.getTag() & (byte)0x1F))  {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        else {
            DerValue subDer = der.getData().getDerValue();
            Date temp = subDer.getGeneralizedTime();
            return new KerberosTime(temp.getTime(), 0);
        }
    }

    public static int getDefaultSkew() {
        int tdiff = Krb5.DEFAULT_ALLOWABLE_CLOCKSKEW;
        try {
            if ((tdiff = Config.getInstance().getIntValue(
                    "libdefaults", "clockskew"))
                        == Integer.MIN_VALUE) {   //value is not defined
                tdiff = Krb5.DEFAULT_ALLOWABLE_CLOCKSKEW;
            }
        } catch (KrbException e) {
            if (DEBUG) {
                System.out.println("Exception in getting clockskew from " +
                                   "Configuration " +
                                   "using default value: " +
                                   e.getMessage());
            }
        }
        return tdiff;
    }

    public String toString() {
        return toGeneralizedTimeString();
    }
}
