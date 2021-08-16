/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/* @test
 * @bug 8051408 8157308 8130181
 * @modules java.base/sun.security.provider
 * @build java.base/sun.security.provider.S
 * @run main SpecTest
 * @summary check the AbstractDrbg API etc
 */

import java.security.*;
import sun.security.provider.S;
import static java.security.DrbgParameters.Capability.*;

/**
 * This test makes sure the AbstractDrbg API works as specified. It also
 * checks the SecureRandom API.
 *
 * The implementations must be patched into java.base/sun.security.provider
 * because AbstractDrbg is not a public interface.
 */
public class SpecTest {

    public static void main(String args[]) throws Exception {

        // getInstance from a provider.

        Provider p = new All("A", "0", "");
        byte[] bytes = new byte[100];

        // A non-DRBG
        iae(() -> SecureRandom.getInstance("S1", null, p));
        nsae(() -> SecureRandom.getInstance("S1",
                new SecureRandomParameters() {}, p));

        SecureRandom s1 = SecureRandom.getInstance("S1", p);
        if (s1.getParameters() != null) {
            throw new Exception();
        }

        iae(() -> s1.nextBytes(bytes, null));
        uoe(() -> s1.nextBytes(bytes, new SecureRandomParameters() {}));
        uoe(() -> s1.reseed());
        iae(() -> s1.reseed(null));
        uoe(() -> s1.reseed(new SecureRandomParameters() {}));

        // A weak DRBG
        iae(() -> SecureRandom.getInstance("S2", null, p));
        nsae(() -> SecureRandom.getInstance("S2",
                new SecureRandomParameters() {}, p));
        nsae(() -> SecureRandom.getInstance("S2",
                DrbgParameters.instantiation(256, NONE, null), p));
        nsae(() -> SecureRandom.getInstance("S2",
                DrbgParameters.instantiation(-1, PR_AND_RESEED, null), p));
        nsae(() -> SecureRandom.getInstance("S2",
                DrbgParameters.instantiation(-1, RESEED_ONLY, null), p));

        SecureRandom s2 = SecureRandom.getInstance("S2",
                DrbgParameters.instantiation(-1, NONE, null), p);
        equals(s2, "S2,SQUEEZE,128,none");
        equals(s2.getParameters(), "128,none,null");

        npe(() -> s2.nextBytes(null));
        iae(() -> s2.nextBytes(bytes, null));
        iae(() -> s2.nextBytes(bytes, new SecureRandomParameters() {}));
        uoe(() -> s2.reseed());
        iae(() -> s2.reseed(null));

        iae(() -> s2.nextBytes(bytes,
                DrbgParameters.nextBytes(-1, false, new byte[101])));
        s2.nextBytes(new byte[101],
                DrbgParameters.nextBytes(-1, false, new byte[100]));
        s2.nextBytes(bytes,
                DrbgParameters.nextBytes(-1, false, new byte[100]));

        // A strong DRBG
        iae(() -> SecureRandom.getInstance("S3", null, p));
        nsae(() -> SecureRandom.getInstance("S3",
                new SecureRandomParameters() {}, p));
        SecureRandom.getInstance("S3",
                DrbgParameters.instantiation(192, PR_AND_RESEED, null), p);

        SecureRandom s3 = SecureRandom.getInstance("S3", p);
        equals(s3, "S3,SQUEEZE,128,reseed_only");
        equals(s3.getParameters(), "128,reseed_only,null");

        iae(() -> s3.nextBytes(bytes,
                DrbgParameters.nextBytes(192, false, null)));
        iae(() -> s3.nextBytes(bytes,
                DrbgParameters.nextBytes(112, true, null)));
        iae(() -> s3.reseed(new SecureRandomParameters() {}));

        SecureRandom s32 = SecureRandom.getInstance(
                "S3", DrbgParameters.instantiation(192, PR_AND_RESEED, null), p);
        equals(s32, "S3,SQUEEZE,192,pr_and_reseed");
        equals(s32.getParameters(), "192,pr_and_reseed,null");

        s32.nextBytes(bytes, DrbgParameters.nextBytes(192, false, null));
        s32.nextBytes(bytes, DrbgParameters.nextBytes(112, true, null));
        s32.reseed();
        s32.reseed(DrbgParameters.reseed(true, new byte[100]));

        // getInstance from competitive providers.

        Provider l = new Legacy("L", "0", "");
        Provider w = new Weak("W", "0", "");
        Provider s = new Strong("S", "0", "");

        Security.addProvider(l);
        Security.addProvider(w);
        Security.addProvider(s);

        SecureRandom s4;

        try {
            s4 = SecureRandom.getInstance("S");
            if (s4.getProvider() != l) {
                throw new Exception();
            }

            nsae(() -> SecureRandom.getInstance(
                    "S", DrbgParameters.instantiation(256, NONE, null)));

            s4 = SecureRandom.getInstance(
                    "S", DrbgParameters.instantiation(192, NONE, null));
            if (s4.getProvider() != s) {
                throw new Exception();
            }

            s4 = SecureRandom.getInstance(
                    "S", DrbgParameters.instantiation(128, PR_AND_RESEED, null));
            if (s4.getProvider() != s) {
                throw new Exception();
            }

            s4 = SecureRandom.getInstance(
                    "S", DrbgParameters.instantiation(128, RESEED_ONLY, null));
            if (s4.getProvider() != s) {
                throw new Exception();
            }

            s4 = SecureRandom.getInstance(
                    "S", DrbgParameters.instantiation(128, NONE, null));
            if (s4.getProvider() != w) {
                throw new Exception();
            }
        } finally {
            Security.removeProvider("L");
            Security.removeProvider("W");
            Security.removeProvider("S");
        }
    }

    public static class All extends Provider {
        protected All(String name, String version, String info) {
            super(name, version, info);
            put("SecureRandom.S1", S.S1.class.getName());
            put("SecureRandom.S2", S.S2.class.getName());
            put("SecureRandom.S3", S.S3.class.getName());
        }
    }

    // Providing S with no params support
    public static class Legacy extends Provider {
        protected Legacy(String name, String version, String info) {
            super(name, version, info);
            put("SecureRandom.S", S.S1.class.getName());
        }
    }

    public static class Weak extends Provider {
        protected Weak(String name, String version, String info) {
            super(name, version, info);
            put("SecureRandom.S", S.S2.class.getName());
        }
    }

    public static class Strong extends Provider {
        protected Strong(String name, String version, String info) {
            super(name, version, info);
            put("SecureRandom.S", S.S3.class.getName());
        }
    }

    static void nsae(RunnableWithException r) throws Exception {
        checkException(r, NoSuchAlgorithmException.class);
    }

    static void iae(RunnableWithException r) throws Exception {
        checkException(r, IllegalArgumentException.class);
    }

    static void uoe(RunnableWithException r) throws Exception {
        checkException(r, UnsupportedOperationException.class);
    }

    static void npe(RunnableWithException r) throws Exception {
        checkException(r, NullPointerException.class);
    }

    interface RunnableWithException {
        void run() throws Exception;
    }

    static void checkException(RunnableWithException r, Class ex)
            throws Exception {
        try {
            r.run();
        } catch (Exception e) {
            if (ex.isAssignableFrom(e.getClass())) {
                return;
            }
            throw e;
        }
        throw new Exception("No exception thrown");
    }

    static void equals(Object o, String s) throws Exception {
        if (!o.toString().equals(s)) {
            throw new Exception(o.toString() + " is not " + s);
        }
    }
}
