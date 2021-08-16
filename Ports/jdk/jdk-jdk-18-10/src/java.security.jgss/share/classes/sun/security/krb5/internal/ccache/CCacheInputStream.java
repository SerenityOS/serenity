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

package sun.security.krb5.internal.ccache;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import sun.security.krb5.*;
import sun.security.krb5.internal.*;
import sun.security.krb5.internal.util.KrbDataInputStream;
import sun.security.util.IOUtils;

/**
 * This class extends KrbDataInputStream. It is used for parsing FCC-format
 * data from file to memory.
 *
 * @author Yanni Zhang
 *
 */
public class CCacheInputStream extends KrbDataInputStream implements FileCCacheConstants {

    /*
     * FCC version 2 contains type information for principals.  FCC
     * version 1 does not.
     *
     * FCC version 3 contains keyblock encryption type information, and is
     * architecture independent.  Previous versions are not.
     *
     * The code will accept version 1, 2, and 3 ccaches, and depending
     * what KRB5_FCC_DEFAULT_FVNO is set to, it will create version 1, 2,
     * or 3 FCC caches.
     *
     * The default credentials cache should be type 3 for now (see
     * init_ctx.c).
     */
    /* V4 of the credentials cache format allows for header tags */

    private static boolean DEBUG = Krb5.DEBUG;

    public CCacheInputStream(InputStream is){
        super(is);
    }

    /* Read tag field introduced in KRB5_FCC_FVNO_4 */
    // this needs to be public for Kinit.
    public Tag readTag() throws IOException {
        char[] buf = new char[1024];
        int len;
        int tag = -1;
        int taglen;
        Integer time_offset = null;
        Integer usec_offset = null;

        len = read(2);
        if (len < 0) {
            throw new IOException("stop.");
        }
        if (len > buf.length) {
            throw new IOException("Invalid tag length.");
        }
        while (len > 0) {
            tag    = read(2);
            taglen = read(2);
            switch (tag) {
            case FCC_TAG_DELTATIME:
                time_offset = read(4);
                usec_offset = read(4);
                break;
            default:
            }
            len = len - (4 + taglen);
        }
        return new Tag(len, tag, time_offset, usec_offset);
    }
    /*
     * In file-based credential cache, the realm name is stored as part of
     * principal name at the first place.
     */
    // made public for KinitOptions to call directly
    public PrincipalName readPrincipal(int version) throws IOException, RealmException {
        int type, length, namelength, kret;
        String[] pname = null;
        String realm;
        /* Read principal type */
        if (version == KRB5_FCC_FVNO_1) {
            type = KRB5_NT_UNKNOWN;
        } else {
            type = read(4);
        }
        length = readLength4();
        List<String> result = new ArrayList<String>();
        /*
         * DCE includes the principal's realm in the count; the new format
         * does not.
         */
        if (version == KRB5_FCC_FVNO_1)
            length--;
        for (int i = 0; i <= length; i++) {
            namelength = readLength4();
            byte[] bytes = IOUtils.readExactlyNBytes(this, namelength);
            result.add(new String(bytes));
        }
        if (result.isEmpty()) {
            throw new IOException("No realm or principal");
        }
        if (isRealm(result.get(0))) {
            realm = result.remove(0);
            if (result.isEmpty()) {
                throw new IOException("No principal name components");
            }
            return new PrincipalName(
                    type,
                    result.toArray(new String[result.size()]),
                    new Realm(realm));
        }
        try {
            return new PrincipalName(
                    type,
                    result.toArray(new String[result.size()]),
                    Realm.getDefault());
        } catch (RealmException re) {
            return null;
        }
    }

    /*
     * In practice, a realm is named by uppercasing the DNS domain name. we currently
     * rely on this to determine if the string within the principal identifier is realm
     * name.
     *
     */
    boolean isRealm(String str) {
        try {
            Realm r = new Realm(str);
        }
        catch (Exception e) {
            return false;
        }
        StringTokenizer st = new StringTokenizer(str, ".");
        String s;
        while (st.hasMoreTokens()) {
            s = st.nextToken();
            for (int i = 0; i < s.length(); i++) {
                if (s.charAt(i) >= 141) {
                    return false;
                }
            }
        }
        return true;
    }

    EncryptionKey readKey(int version) throws IOException {
        int keyType, keyLen;
        keyType = read(2);
        if (version == KRB5_FCC_FVNO_3)
            read(2); /* keytype recorded twice in fvno 3 */
        keyLen = readLength4();
        byte[] bytes = IOUtils.readExactlyNBytes(this, keyLen);
        return new EncryptionKey(bytes, keyType, version);
    }

    long[] readTimes() throws IOException {
        long[] times = new long[4];
        times[0] = (long)read(4) * 1000;
        times[1] = (long)read(4) * 1000;
        times[2] = (long)read(4) * 1000;
        times[3] = (long)read(4) * 1000;
        return times;
    }

    boolean readskey() throws IOException {
        if (read() == 0) {
            return false;
        }
        else return true;
    }

    HostAddress[] readAddr() throws IOException, KrbApErrException {
        int numAddrs, addrType, addrLength;
        numAddrs = readLength4();
        if (numAddrs > 0) {
            List<HostAddress> addrs = new ArrayList<>();
            for (int i = 0; i < numAddrs; i++) {
                addrType = read(2);
                addrLength = readLength4();
                if (!(addrLength == 4 || addrLength == 16)) {
                    if (DEBUG) {
                        System.out.println("Incorrect address format.");
                    }
                    return null;
                }
                byte[] result = new byte[addrLength];
                for (int j = 0; j < addrLength; j++)
                    result[j] = (byte)read(1);
                addrs.add(new HostAddress(addrType, result));
            }
            return addrs.toArray(new HostAddress[addrs.size()]);
        }
        return null;
    }

    AuthorizationDataEntry[] readAuth() throws IOException {
        int num, adtype, adlength;
        num = readLength4();
        if (num > 0) {
            List<AuthorizationDataEntry> auData = new ArrayList<>();
            byte[] data = null;
            for (int i = 0; i < num; i++) {
                adtype = read(2);
                adlength = readLength4();
                data = IOUtils.readExactlyNBytes(this, adlength);
                auData.add(new AuthorizationDataEntry(adtype, data));
            }
            return auData.toArray(new AuthorizationDataEntry[auData.size()]);
        }
        else return null;
    }

    byte[] readData() throws IOException {
        int length;
        length = readLength4();
        if (length == 0) {
            return null;
        } else {
            return IOUtils.readExactlyNBytes(this, length);
        }
    }

    boolean[] readFlags() throws IOException {
        boolean[] flags = new boolean[Krb5.TKT_OPTS_MAX+1];
        int ticketFlags;
        ticketFlags = read(4);
        if ((ticketFlags & 0x40000000) == TKT_FLG_FORWARDABLE)
        flags[1] = true;
        if ((ticketFlags & 0x20000000) == TKT_FLG_FORWARDED)
        flags[2] = true;
        if ((ticketFlags & 0x10000000) == TKT_FLG_PROXIABLE)
        flags[3] = true;
        if ((ticketFlags & 0x08000000) == TKT_FLG_PROXY)
        flags[4] = true;
        if ((ticketFlags & 0x04000000) == TKT_FLG_MAY_POSTDATE)
        flags[5] = true;
        if ((ticketFlags & 0x02000000) == TKT_FLG_POSTDATED)
        flags[6] = true;
        if ((ticketFlags & 0x01000000) == TKT_FLG_INVALID)
        flags[7] = true;
        if ((ticketFlags & 0x00800000) == TKT_FLG_RENEWABLE)
        flags[8] = true;
        if ((ticketFlags & 0x00400000) == TKT_FLG_INITIAL)
        flags[9] = true;
        if ((ticketFlags & 0x00200000) == TKT_FLG_PRE_AUTH)
        flags[10] = true;
        if ((ticketFlags & 0x00100000) == TKT_FLG_HW_AUTH)
        flags[11] = true;
        if (DEBUG) {
            String msg = ">>> CCacheInputStream: readFlags() ";
            if (flags[1] == true) {
                msg += " FORWARDABLE;";
            }
            if (flags[2] == true) {
                msg += " FORWARDED;";
            }
            if (flags[3] == true) {
                msg += " PROXIABLE;";
            }
            if (flags[4] == true) {
                msg += " PROXY;";
            }
            if (flags[5] == true) {
                msg += " MAY_POSTDATE;";
            }
            if (flags[6] == true) {
                msg += " POSTDATED;";
            }
            if (flags[7] == true) {
                msg += " INVALID;";
            }
            if (flags[8] == true) {
                msg += " RENEWABLE;";
            }

            if (flags[9] == true) {
                msg += " INITIAL;";
            }
            if (flags[10] == true) {
                msg += " PRE_AUTH;";
            }
            if (flags[11] == true) {
                msg += " HW_AUTH;";
            }
            System.out.println(msg);
        }
        return flags;
    }

    /**
     * Reads the next cred or config entry in stream.
     * @return the next cred or config entry, null if data unparseable.
     *
     * When data is unparseable, this method makes sure the correct number of
     * bytes are consumed so it's safe to start reading the next element.
     */
    Object readCred(int version) throws IOException,RealmException, KrbApErrException, Asn1Exception {
        PrincipalName cpname = null;
        try {
            cpname = readPrincipal(version);
        } catch (Exception e) {
            // Do not return here. All data for this cred should be fully
            // consumed so that we can read the next one.
        }
        if (DEBUG) {
            System.out.println(">>>DEBUG <CCacheInputStream>  client principal is " + cpname);
        }
        PrincipalName spname = null;
        try {
            spname = readPrincipal(version);
        } catch (Exception e) {
            // same as above
        }
        if (DEBUG) {
            System.out.println(">>>DEBUG <CCacheInputStream> server principal is " + spname);
        }
        EncryptionKey key = readKey(version);
        if (DEBUG) {
            System.out.println(">>>DEBUG <CCacheInputStream> key type: " + key.getEType());
        }
        long[] times = readTimes();
        KerberosTime authtime = new KerberosTime(times[0]);
        KerberosTime starttime =
                (times[1]==0) ? null : new KerberosTime(times[1]);
        KerberosTime endtime = new KerberosTime(times[2]);
        KerberosTime renewTill =
                (times[3]==0) ? null : new KerberosTime(times[3]);

        if (DEBUG) {
            System.out.println(">>>DEBUG <CCacheInputStream> auth time: " + authtime.toDate().toString());
            System.out.println(">>>DEBUG <CCacheInputStream> start time: " +
                    ((starttime==null)?"null":starttime.toDate().toString()));
            System.out.println(">>>DEBUG <CCacheInputStream> end time: " + endtime.toDate().toString());
            System.out.println(">>>DEBUG <CCacheInputStream> renew_till time: " +
                    ((renewTill==null)?"null":renewTill.toDate().toString()));
        }
        boolean skey = readskey();
        boolean[] flags = readFlags();
        TicketFlags tFlags = new TicketFlags(flags);
        HostAddress[] addr = readAddr();
        HostAddresses addrs = null;
        if (addr != null) {
            addrs = new HostAddresses(addr);
        }
        AuthorizationDataEntry[] auDataEntry = readAuth();
        AuthorizationData auData = null;
        if (auDataEntry != null) {
            auData = new AuthorizationData(auDataEntry);
        }
        byte[] ticketData = readData();
        byte[] ticketData2 = readData();

        // Skip this cred if either cpname or spname isn't created.
        if (cpname == null || spname == null) {
            return null;
        }

        try {
            if (spname.getRealmString().equals("X-CACHECONF:")) {
                String[] nameParts = spname.getNameStrings();
                if (nameParts[0].equals("krb5_ccache_conf_data")) {
                    return new CredentialsCache.ConfigEntry(nameParts[1],
                            nameParts.length > 2 ? new PrincipalName(nameParts[2]) : null,
                            ticketData);
                }
            }
            return new Credentials(cpname, spname, key, authtime, starttime,
                    endtime, renewTill, skey, tFlags,
                    addrs, auData,
                    ticketData != null ? new Ticket(ticketData) : null,
                    ticketData2 != null ? new Ticket(ticketData2) : null);
        } catch (Exception e) {     // If any of new Ticket(*) fails.
            if (DEBUG) {
                e.printStackTrace(System.out);
            }
            return null;
        }
    }
}
