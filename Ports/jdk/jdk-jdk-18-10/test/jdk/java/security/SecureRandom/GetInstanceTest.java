/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8141039
 * @library /test/lib
 * @summary SecureRandom supports multiple getInstance method including
 *          getInstanceStrong() method. This test verifies a set of possible
 *          cases for getInstance with different SecureRandom mechanism
 *          supported in Java.
 * @run main GetInstanceTest
 */
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.SecureRandomParameters;
import java.security.DrbgParameters;
import static java.security.DrbgParameters.Capability.*;
import java.security.Security;
import java.util.Arrays;
import jdk.test.lib.Asserts;

public class GetInstanceTest {

    private static final boolean PASS = true;
    private static final String INVALID_ALGO = "INVALID";
    private static final String SUN_PROVIDER = "SUN";
    private static final String INVALID_PROVIDER = "INVALID";
    private static final String STRONG_ALG_SEC_PROP
            = "securerandom.strongAlgorithms";
    private static final String DRBG_CONFIG = "securerandom.drbg.config";
    private static final String DRBG_CONFIG_VALUE
            = Security.getProperty(DRBG_CONFIG);

    public static void main(String[] args) throws Exception {

        boolean success = true;
        // Only accepted failure is NoSuchAlgorithmException.
        // For any other failure the test case will fail here.
        SecureRandom sr = matchExc(() -> SecureRandom.getInstanceStrong(),
                PASS, NoSuchAlgorithmException.class,
                "PASS - Undefined security Property "
                + "'securerandom.strongAlgorithms'");
        System.out.format("Current platform supports mechanism: '%s' through "
                + "provider: '%s' for the method getInstanceStrong().",
                sr.getAlgorithm(), sr.getProvider().getName());

        // DRBG name should appear with "securerandom.strongAlgorithms"
        // security property.
        String origDRBGConfig = Security.getProperty(STRONG_ALG_SEC_PROP);
        if (!origDRBGConfig.contains("DRBG")) {
            throw new RuntimeException("DRBG is not associated with default "
                    + "strong algorithm through security Property: "
                    + "'securerandom.strongAlgorithms'.");
        }
        try {
            Security.setProperty(STRONG_ALG_SEC_PROP, "DRBG:SUN");
            sr = matchExc(() -> SecureRandom.getInstanceStrong(),
                    PASS, NoSuchAlgorithmException.class,
                    "PASS - Undefined security Property "
                    + "'securerandom.strongAlgorithms'");
            checkAttributes(sr, "DRBG");
        } finally {
            Security.setProperty(STRONG_ALG_SEC_PROP, origDRBGConfig);
        }

        for (String mech : new String[]{
            "SHA1PRNG", "Hash_DRBG", "HMAC_DRBG", "CTR_DRBG", INVALID_ALGO,}) {
            System.out.printf("%nTest SecureRandom mechanism: '%s'", mech);
            try {
                if (isDRBG(mech)) {
                    Security.setProperty(DRBG_CONFIG, mech);
                }
                verifyInstance(mech);
            } catch (Exception e) {
                e.printStackTrace(System.out);
                success = false;
            } finally {
                Security.setProperty(DRBG_CONFIG, DRBG_CONFIG_VALUE);
            }
        }
        if (!success) {
            throw new RuntimeException("At least one test failed.");
        }
    }

    private static void verifyInstance(String mech) throws Exception {

        String srAlgo = isDRBG(mech) ? "DRBG" : mech;

        // Test for getInstance(algorithm) method.
        // It should pass for all case other than invalid algorithm name.
        // If it fails then the expected exception type should be
        // NoSuchAlgorithmException. Any other Exception type occured will be
        // treated as failure.
        checkAttributes(
                matchExc(() -> SecureRandom.getInstance(srAlgo), !(nsa(mech)),
                        NoSuchAlgorithmException.class,
                        String.format("PASS - It is expected to fail for"
                                + " getInstance(algorithm) when algorithm: '%s'"
                                + " is null or invalid.", mech)), mech);
        // Test for getInstance(algorithm, provider) method.
        checkAttributes(
                matchExc(() -> SecureRandom.getInstance(srAlgo,
                                Security.getProvider(SUN_PROVIDER)),
                        !(nsa(mech)),
                        NoSuchAlgorithmException.class,
                        String.format("PASS - It is expected to fail for"
                                + " getInstance(algorithm, provider) when"
                                + " algorithm:'%s' is null or invalid.", mech)),
                mech);
        // Test for getInstance(algorithm, providerName) method.
        checkAttributes(
                matchExc(() -> SecureRandom.getInstance(srAlgo, SUN_PROVIDER),
                        !(nsa(mech)), NoSuchAlgorithmException.class,
                        String.format("PASS - It is expected to fail for "
                                + "getInstance(algorithm, providerName) when "
                                + "algorithm: '%s' is null or invalid.", mech)),
                mech);
        // Test for getInstance(algorithm, providerName) method.
        checkAttributes(
                matchExc(() -> SecureRandom.getInstance(
                                srAlgo, INVALID_PROVIDER),
                        !PASS, NoSuchProviderException.class,
                        String.format("PASS - It is expected to fail for "
                                + "getInstance(algorithm, providerName) when "
                                + "provider name: '%s' is invalid and "
                                + "algorithm: '%s'", INVALID_PROVIDER, mech)),
                mech);

        // Run the test for a set of SecureRandomParameters
        for (SecureRandomParameters param : Arrays.asList(null,
                DrbgParameters.instantiation(-1, NONE, null))) {

            System.out.printf("%nRunning DRBG param getInstance() methods "
                    + "for algorithm: %s and DRBG param type: %s", mech,
                    (param != null) ? param.getClass().getName() : param);

            // Following Test are applicable for new DRBG methods only.
            // Test for getInstance(algorithm, params) method.
            // Tests are expected to pass for DRBG type with valid parameter
            // If it fails the expected exception type is derived from
            // getExcType(mech, param) method. If exception type is not
            // expected then the test will be considered as failure.
            checkAttributes(
                    matchExc(() -> SecureRandom.getInstance(srAlgo, param),
                            (isDRBG(mech)) && (isValidDRBGParam(param)),
                            getExcType(mech, param),
                            String.format("PASS - It is expected to fail "
                                    + "for getInstance(algorithm, params) "
                                    + "for algorithm: %s and parameter: %s",
                                    mech, param)),
                    mech);
            // Test for getInstance(algorithm, params, provider) method.
            checkAttributes(
                    matchExc(() -> SecureRandom.getInstance(srAlgo, param,
                                    Security.getProvider(SUN_PROVIDER)),
                            (isDRBG(mech)) && (isValidDRBGParam(param)),
                            getExcType(mech, param),
                            String.format("PASS - It is expected to fail "
                                    + "for getInstance(algorithm, params, "
                                    + "provider) for algorithm: %s and "
                                    + "parameter: %s", mech, param)),
                    mech);
            // Test for getInstance(algorithm, params, providerName) method.
            checkAttributes(
                    matchExc(() -> SecureRandom.getInstance(srAlgo, param,
                                    SUN_PROVIDER),
                            (isDRBG(mech)) && (isValidDRBGParam(param)),
                            getExcType(mech, param),
                            String.format("PASS - It is expected to fail "
                                    + "for getInstance(algorithm, params, "
                                    + "providerName) for algorithm: %s and "
                                    + "parameter: %s", mech, param)), mech);
            // getInstance(algorithm, params, providerName) when
            // providerName is invalid
            checkAttributes(
                    matchExc(() -> SecureRandom.getInstance(srAlgo, param,
                                    INVALID_PROVIDER),
                            !PASS, ((param == null)
                                    ? IllegalArgumentException.class
                                    : NoSuchProviderException.class),
                            String.format("PASS - It is expected to fail "
                                    + "for getInstance(algorithm, params, "
                                    + "providerName) when param is null or"
                                    + " provider: %s is invalid for "
                                    + "algorithm: '%s'", INVALID_PROVIDER,
                                    mech)), mech);
            // getInstance(algorithm, params, provider) when provider=null
            checkAttributes(
                    matchExc(() -> SecureRandom.getInstance(srAlgo, param,
                                    (String) null),
                            !PASS, IllegalArgumentException.class,
                            String.format("PASS - It is expected to fail "
                                    + "for getInstance(algorithm, params, "
                                    + "providerName) when provider name "
                                    + "is null")), mech);
            // getInstance(algorithm, params, providerName) when
            // providerName is empty.
            checkAttributes(
                    matchExc(() -> SecureRandom.getInstance(
                                    srAlgo, param, ""),
                            !PASS, IllegalArgumentException.class,
                            String.format("PASS - It is expected to fail "
                                    + "for getInstance(algorithm, params, "
                                    + "providerName) when provider name "
                                    + "is empty")), mech);
        }
    }

    private static boolean isValidDRBGParam(SecureRandomParameters param) {
        return (param instanceof DrbgParameters.Instantiation);
    }

    /**
     * If the mechanism should occur NoSuchAlgorithmException.
     */
    private static boolean nsa(String mech) {
        return mech.equals(INVALID_ALGO);
    }

    /**
     * Verify if the mechanism is DRBG type.
     * @param mech Mechanism name
     * @return True if the mechanism name is DRBG type else False.
     */
    private static boolean isDRBG(String mech) {
        return mech.contains("_DRBG");
    }

    /**
     * Type of exception expected for a SecureRandom instance when exception
     * occurred while calling getInstance method with a fixed set of parameter.
     * @param mech Mechanism used to create a SecureRandom instance
     * @param param Parameter to getInstance() method
     * @return Exception type expected
     */
    private static Class getExcType(String mech, SecureRandomParameters param) {
        return ((isDRBG(mech) && !isValidDRBGParam(param)) || param == null)
                ? IllegalArgumentException.class
                : NoSuchAlgorithmException.class;
    }

    private interface RunnableCode {

        SecureRandom run() throws Exception;
    }

    /**
     * Execute a given code block and verify, if the exception type is expected.
     * @param r Code block to run
     * @param ex Expected exception type
     * @param shouldPass If the code execution expected to pass without failure
     * @param msg Message to log in case of expected failure
     */
    private static SecureRandom matchExc(RunnableCode r, boolean shouldPass,
            Class ex, String msg) {
        SecureRandom sr = null;
        try {
            sr = r.run();
            if (!shouldPass) {
                throw new RuntimeException("Excecution should fail here.");
            }
        } catch (Exception e) {
            System.out.printf("%nOccured exception: %s - Expected exception: %s"
                    + " : ", e.getClass(), ex.getCanonicalName());
            if (ex.isAssignableFrom(e.getClass())) {
                System.out.printf("%n%s : Expected Exception: %s : ",
                        e.getClass(), msg);
            } else if (shouldPass) {
                throw new RuntimeException(e);
            } else {
                System.out.printf("%nIgnore the following exception: %s%n",
                        e.getMessage());
            }
        }
        return sr;
    }

    /**
     * Check specific attributes of a SecureRandom instance.
     */
    private static void checkAttributes(SecureRandom sr, String mech) {
        if (sr == null) {
            return;
        }
        Asserts.assertEquals(sr.getAlgorithm(), (isDRBG(mech) ? "DRBG" : mech));
        Asserts.assertEquals(sr.getProvider().getName(), SUN_PROVIDER);
    }

}
