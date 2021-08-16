/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.pkcs11;

import java.math.BigInteger;
import java.security.*;

/**
 * Collection of static utility methods.
 *
 * @author  Andreas Sterbenz
 * @since   1.5
 */
public final class P11Util {

    private static Object LOCK = new Object();

    private static volatile Provider sun, sunRsaSign, sunJce;

    private P11Util() {
        // empty
    }

    static Provider getSunProvider() {
        Provider p = sun;
        if (p == null) {
            synchronized (LOCK) {
                p = getProvider
                    (sun, "SUN", "sun.security.provider.Sun");
                sun = p;
            }
        }
        return p;
    }

    static Provider getSunRsaSignProvider() {
        Provider p = sunRsaSign;
        if (p == null) {
            synchronized (LOCK) {
                p = getProvider
                    (sunRsaSign, "SunRsaSign", "sun.security.rsa.SunRsaSign");
                sunRsaSign = p;
            }
        }
        return p;
    }

    static Provider getSunJceProvider() {
        Provider p = sunJce;
        if (p == null) {
            synchronized (LOCK) {
                p = getProvider
                    (sunJce, "SunJCE", "com.sun.crypto.provider.SunJCE");
                sunJce = p;
            }
        }
        return p;
    }

    @SuppressWarnings("removal")
    private static Provider getProvider(Provider p, String providerName,
            String className) {
        if (p != null) {
            return p;
        }
        p = Security.getProvider(providerName);
        if (p == null) {
            try {
                final Class<?> c = Class.forName(className);
                p = AccessController.doPrivileged(
                    new PrivilegedAction<Provider>() {
                        public Provider run() {
                            try {
                                @SuppressWarnings("deprecation")
                                Object o = c.newInstance();
                                return (Provider) o;
                            } catch (Exception e) {
                                throw new ProviderException(
                                        "Could not find provider " +
                                                providerName, e);
                            }
                        }
                    }, null, new RuntimePermission(
                            "accessClassInPackage." + c.getPackageName()));
            } catch (ClassNotFoundException e) {
                // Unexpected, as className is not a user but a
                // P11Util-internal value.
                throw new ProviderException("Could not find provider " +
                        providerName, e);
            }
        }
        return p;
    }

    static byte[] convert(byte[] input, int offset, int len) {
        if ((offset == 0) && (len == input.length)) {
            return input;
        } else {
            byte[] t = new byte[len];
            System.arraycopy(input, offset, t, 0, len);
            return t;
        }
    }

    static byte[] subarray(byte[] b, int ofs, int len) {
        byte[] out = new byte[len];
        System.arraycopy(b, ofs, out, 0, len);
        return out;
    }

    static byte[] concat(byte[] b1, byte[] b2) {
        byte[] b = new byte[b1.length + b2.length];
        System.arraycopy(b1, 0, b, 0, b1.length);
        System.arraycopy(b2, 0, b, b1.length, b2.length);
        return b;
    }

    static long[] concat(long[] b1, long[] b2) {
        if (b1.length == 0) {
            return b2;
        }
        long[] b = new long[b1.length + b2.length];
        System.arraycopy(b1, 0, b, 0, b1.length);
        System.arraycopy(b2, 0, b, b1.length, b2.length);
        return b;
    }

    public static byte[] getMagnitude(BigInteger bi) {
        byte[] b = bi.toByteArray();
        if ((b.length > 1) && (b[0] == 0)) {
            int n = b.length - 1;
            byte[] newarray = new byte[n];
            System.arraycopy(b, 1, newarray, 0, n);
            b = newarray;
        }
        return b;
    }

    static byte[] sha1(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(data);
            return md.digest();
        } catch (GeneralSecurityException e) {
            throw new ProviderException(e);
        }
    }

    private static final char[] hexDigits = "0123456789abcdef".toCharArray();

    static String toString(byte[] b) {
        if (b == null) {
            return "(null)";
        }
        StringBuilder sb = new StringBuilder(b.length * 3);
        for (int i = 0; i < b.length; i++) {
            int k = b[i] & 0xff;
            if (i != 0) {
                sb.append(':');
            }
            sb.append(hexDigits[k >>> 4]);
            sb.append(hexDigits[k & 0xf]);
        }
        return sb.toString();
    }

}
