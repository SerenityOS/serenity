/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.ssl;

import java.security.CryptoPrimitive;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * Enum for an SSL/TLS/DTLS protocol version.
 *
 * @author  Andreas Sterbenz
 * @since   1.4.1
 */
enum ProtocolVersion {
    TLS13           (0x0304,    "TLSv1.3",      false),
    TLS12           (0x0303,    "TLSv1.2",      false),
    TLS11           (0x0302,    "TLSv1.1",      false),
    TLS10           (0x0301,    "TLSv1",        false),
    SSL30           (0x0300,    "SSLv3",        false),
    SSL20Hello      (0x0002,    "SSLv2Hello",   false),

    DTLS12          (0xFEFD,    "DTLSv1.2",     true),
    DTLS10          (0xFEFF,    "DTLSv1.0",     true),

    // Dummy protocol version value for invalid SSLSession
    NONE            (-1,        "NONE",         false);


    final int id;
    final String name;
    final boolean isDTLS;
    final byte major;
    final byte minor;
    final boolean isAvailable;

    // The limit of maximum protocol version
    static final int LIMIT_MAX_VALUE = 0xFFFF;

    // The limit of minimum protocol version
    static final int LIMIT_MIN_VALUE = 0x0000;

    // (D)TLS ProtocolVersion array for TLS 1.0 and previous versions.
    static final ProtocolVersion[] PROTOCOLS_TO_10 = new ProtocolVersion[] {
            TLS10, SSL30
        };

    // (D)TLS ProtocolVersion array for TLS 1.1/DTLS 1.0 and previous versions.
    static final ProtocolVersion[] PROTOCOLS_TO_11 = new ProtocolVersion[] {
            TLS11, TLS10, SSL30, DTLS10
        };

    // (D)TLS ProtocolVersion array for (D)TLS 1.2 and previous versions.
    static final ProtocolVersion[] PROTOCOLS_TO_12 = new ProtocolVersion[] {
            TLS12, TLS11, TLS10, SSL30, DTLS12, DTLS10
    };

    // (D)TLS ProtocolVersion array for (D)TLS 1.3 and previous versions.
    static final ProtocolVersion[] PROTOCOLS_TO_13 = new ProtocolVersion[] {
            TLS13, TLS12, TLS11, TLS10, SSL30, DTLS12, DTLS10
        };

    // No protocol version specified.
    static final ProtocolVersion[] PROTOCOLS_OF_NONE = new ProtocolVersion[] {
            NONE
        };

    // (D)TLS ProtocolVersion array for SSL 3.0.
    static final ProtocolVersion[] PROTOCOLS_OF_30 = new ProtocolVersion[] {
            SSL30
        };

    // (D)TLS ProtocolVersion array for TLS 1.1/DTSL 1.0.
    static final ProtocolVersion[] PROTOCOLS_OF_11 = new ProtocolVersion[] {
            TLS11, DTLS10
        };

    // (D)TLS ProtocolVersion array for (D)TLS 1.2.
    static final ProtocolVersion[] PROTOCOLS_OF_12 = new ProtocolVersion[] {
            TLS12, DTLS12
        };

    // (D)TLS ProtocolVersion array for (D)TLS 1.3.
    static final ProtocolVersion[] PROTOCOLS_OF_13 = new ProtocolVersion[] {
            TLS13
        };

    // (D)TLS ProtocolVersion array for TSL 1.0/1.1 and DTLS 1.0.
    static final ProtocolVersion[] PROTOCOLS_10_11 = new ProtocolVersion[] {
            TLS11, TLS10, DTLS10
        };

    // (D)TLS ProtocolVersion array for TSL 1.1/1.2 and DTLS 1.0/1.2.
    static final ProtocolVersion[] PROTOCOLS_11_12 = new ProtocolVersion[] {
            TLS12, TLS11, DTLS12, DTLS10
        };

    // (D)TLS ProtocolVersion array for TSL 1.2/1.3 and DTLS 1.2/1.3.
    static final ProtocolVersion[] PROTOCOLS_12_13 = new ProtocolVersion[] {
            TLS13, TLS12, DTLS12
        };

    // (D)TLS ProtocolVersion array for TSL 1.0/1.1/1.2 and DTLS 1.0/1.2.
    static final ProtocolVersion[] PROTOCOLS_10_12 = new ProtocolVersion[] {
            TLS12, TLS11, TLS10, DTLS12, DTLS10
        };

    // TLS ProtocolVersion array for TLS 1.2 and previous versions.
    static final ProtocolVersion[] PROTOCOLS_TO_TLS12 = new ProtocolVersion[] {
            TLS12, TLS11, TLS10, SSL30
    };

    // TLS ProtocolVersion array for TLS 1.1 and previous versions.
    static final ProtocolVersion[] PROTOCOLS_TO_TLS11 = new ProtocolVersion[] {
            TLS11, TLS10, SSL30
    };

    // TLS ProtocolVersion array for TLS 1.0 and previous versions.
    static final ProtocolVersion[] PROTOCOLS_TO_TLS10 = new ProtocolVersion[] {
            TLS10, SSL30
    };

    // Empty ProtocolVersion array
    static final ProtocolVersion[] PROTOCOLS_EMPTY = new ProtocolVersion[0];

    private ProtocolVersion(int id, String name, boolean isDTLS) {
        this.id = id;
        this.name = name;
        this.isDTLS = isDTLS;
        this.major = (byte)((id >>> 8) & 0xFF);
        this.minor = (byte)(id & 0xFF);

        this.isAvailable = SSLAlgorithmConstraints.DEFAULT_SSL_ONLY.permits(
                EnumSet.<CryptoPrimitive>of(CryptoPrimitive.KEY_AGREEMENT),
                name, null);
    }

    /**
     * Return a ProtocolVersion with the specified major and minor
     * version numbers.
     */
    static ProtocolVersion valueOf(byte major, byte minor) {
        for (ProtocolVersion pv : ProtocolVersion.values()) {
            if ((pv.major == major) && (pv.minor == minor)) {
                return pv;
            }
        }

        return null;
    }

    /**
     * Return a ProtocolVersion with the specified version number.
     */
    static ProtocolVersion valueOf(int id) {
        for (ProtocolVersion pv : ProtocolVersion.values()) {
            if (pv.id == id) {
                return pv;
            }
        }

        return null;
    }

    /**
     * Return name of a (D)TLS protocol specified by major and
     * minor version numbers.
     */
    static String nameOf(byte major, byte minor) {
        for (ProtocolVersion pv : ProtocolVersion.values()) {
            if ((pv.major == major) && (pv.minor == minor)) {
                return pv.name;
            }
        }

        return "(D)TLS-" + major + "." + minor;
    }

    /**
     * Return name of a (D)TLS protocol specified by a protocol number.
     */
    static String nameOf(int id) {
        return nameOf((byte)((id >>> 8) & 0xFF), (byte)(id & 0xFF));
    }

    /**
     * Return a ProtocolVersion for the given (D)TLS protocol name.
     */
    static ProtocolVersion nameOf(String name) {
        for (ProtocolVersion pv : ProtocolVersion.values()) {
            if (pv.name.equals(name)) {
                return pv;
            }
        }

        return null;
    }

    /**
     * Return true if the specific (D)TLS protocol is negotiable.
     *
     * Used to filter out SSLv2Hello and protocol numbers less than the
     * minimal supported protocol versions.
     */
    static boolean isNegotiable(
            byte major, byte minor, boolean isDTLS, boolean allowSSL20Hello) {
        int v = ((major & 0xFF) << 8) | (minor & 0xFF);
        if (isDTLS) {
            return v <= DTLS10.id;
        } else {
            if (v < SSL30.id) {
               if (!allowSSL20Hello || (v != SSL20Hello.id)) {
                   return false;
               }
            }
            return true;
        }
    }

    /**
     * Get names of a list of ProtocolVersion objects.
     */
    static String[] toStringArray(List<ProtocolVersion> protocolVersions) {
        if ((protocolVersions != null) && !protocolVersions.isEmpty()) {
            String[] protocolNames = new String[protocolVersions.size()];
            int i = 0;
            for (ProtocolVersion pv : protocolVersions) {
                protocolNames[i++] = pv.name;
            }

            return protocolNames;
        }

        return new String[0];
    }

    /**
     * Get names of a list of protocol version identifiers.
     */
    static String[] toStringArray(int[] protocolVersions) {
        if ((protocolVersions != null) && protocolVersions.length != 0) {
            String[] protocolNames = new String[protocolVersions.length];
            int i = 0;
            for (int pv : protocolVersions) {
                protocolNames[i++] = ProtocolVersion.nameOf(pv);
            }

            return protocolNames;
        }

        return new String[0];
    }

    /**
     * Get a list of ProtocolVersion objects of an array protocol
     * version names.
     */
    static List<ProtocolVersion> namesOf(String[] protocolNames) {
        if (protocolNames == null || protocolNames.length == 0) {
            return Collections.<ProtocolVersion>emptyList();
        }

        List<ProtocolVersion> pvs = new ArrayList<>(protocolNames.length);
        for (String pn : protocolNames) {
            ProtocolVersion pv = ProtocolVersion.nameOf(pn);
            if (pv == null) {
                throw new IllegalArgumentException(
                        "Unsupported protocol" + pn);
            }

            pvs.add(pv);
        }

        return Collections.unmodifiableList(pvs);
    }

    /**
     * Return true if the specific protocol version name is
     * of (D)TLS 1.2 or newer version.
     */
    static boolean useTLS12PlusSpec(String name) {
        ProtocolVersion pv = ProtocolVersion.nameOf(name);
        if (pv != null && pv != NONE) {
            return pv.isDTLS ? (pv.id <= DTLS12.id) : (pv.id >= TLS12.id);
        }

        return false;
    }

    /**
     * Compares this object with the specified ProtocolVersion.
     *
     * @see java.lang.Comparable
     */
    int compare(ProtocolVersion that) {
        if (this == that) {
            return 0;
        }

        if (this == ProtocolVersion.NONE) {
            return -1;
        } else if (that == ProtocolVersion.NONE) {
            return 1;
        }

        if (isDTLS) {
            return that.id - this.id;
        } else {
            return this.id - that.id;
        }
    }

    /**
     * Return true if this ProtocolVersion object is of (D)TLS 1.3 or
     * newer version.
     */
    boolean useTLS13PlusSpec() {
        return isDTLS ? (this.id < DTLS12.id) : (this.id >= TLS13.id);
    }

    /**
     * Return true if this ProtocolVersion object is of (D)TLS 1.2 or
     * newer version.
     */
    boolean useTLS12PlusSpec() {
        return isDTLS ? (this.id <= DTLS12.id) : (this.id >= TLS12.id);
    }

    /**
     * Return true if this ProtocolVersion object is of
     * TLS 1.1/DTLS 1.0 or newer version.
     */
    boolean useTLS11PlusSpec() {
        return isDTLS || (this.id >= TLS11.id);
    }

    /**
     * Return true if this ProtocolVersion object is of TLS 1.0 or
     * newer version.
     */
    boolean useTLS10PlusSpec() {
        return isDTLS || (this.id >= TLS10.id);
    }

    /**
     * Return true if this ProtocolVersion object is of TLS 1.0 or
     * newer version.
     */
    static boolean useTLS10PlusSpec(int id, boolean isDTLS) {
        return isDTLS || (id >= TLS10.id);
    }

    /**
     * Return true if this ProtocolVersion object is of (D)TLS 1.3 or
     * newer version.
     */
    static boolean useTLS13PlusSpec(int id, boolean isDTLS) {
        return isDTLS ? (id < DTLS12.id) : (id >= TLS13.id);
    }

    /**
     * Select the lower of that suggested protocol version and
     * the highest of the listed protocol versions.
     *
     * @param listedVersions    the listed protocol version
     * @param suggestedVersion  the suggested protocol version
     */
    static ProtocolVersion selectedFrom(
            List<ProtocolVersion> listedVersions, int suggestedVersion) {
        ProtocolVersion selectedVersion = ProtocolVersion.NONE;
        for (ProtocolVersion pv : listedVersions) {
            if (pv.id == suggestedVersion) {
                return pv;
            } else if (pv.isDTLS) {
                if (pv.id > suggestedVersion && pv.id < selectedVersion.id) {
                    selectedVersion = pv;
                }
            } else {
                if (pv.id < suggestedVersion && pv.id > selectedVersion.id) {
                    selectedVersion = pv;
                }
            }
        }

        return selectedVersion;
    }
}
