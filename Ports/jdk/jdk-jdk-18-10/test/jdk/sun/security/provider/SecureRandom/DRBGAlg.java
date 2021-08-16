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
import sun.security.provider.MoreDrbgParameters;

import java.security.DrbgParameters;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SecureRandomParameters;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.security.DrbgParameters.Capability.*;

/**
 * @test
 * @bug 8051408
 * @modules java.base/sun.security.provider
 * @summary make sure DRBG alg can be defined and instantiated freely
 */
public class DRBGAlg {

    public static void main(String[] args) throws Exception {

        check(null, "Hash_DRBG", "SHA-256", "reseed_only", ",128");
        check("", "Hash_DRBG", "SHA-256", "reseed_only", ",128");
        check("sha-256", "Hash_DRBG", "SHA-256", "reseed_only", ",128");
        check("SHA-3");
        check("hash_drbg", "Hash_DRBG", "SHA-256", "reseed_only", ",128");
        check("hmac_drbg", "HMAC_DRBG", "SHA-256", "reseed_only", ",128");
        check("ctr_drbg", "CTR_DRBG", "AES-", "reseed_only", ",128", "use_df");

        // trying all permutations
        checkPermutations(
                Collections.emptyList(),
                Arrays.asList("hash_drbg","sha-512","Pr_and_Reseed","192"),
                "Hash_DRBG", "SHA-512", "pr_and_reseed", ",192");

        check("Hash_DRBG,Hmac_DRBG");
        check("SHA-224,SHA-256");
        check("128,256");
        check("none,reseed_only");
        check("use_df,no_df");
        check("Hash_DRBG,,SHA-256");

        check(null, DrbgParameters.instantiation(112, PR_AND_RESEED, null),
                "Hash_DRBG", "SHA-256", "pr_and_reseed", ",112");
        check(null, DrbgParameters.instantiation(256, PR_AND_RESEED, null),
                "Hash_DRBG", "SHA-256", "pr_and_reseed", ",256");
        check(null, DrbgParameters.instantiation(384, PR_AND_RESEED, null));
        check("sha-224", DrbgParameters.instantiation(112, PR_AND_RESEED, null),
                "Hash_DRBG", "SHA-224", "pr_and_reseed", ",112");
        check("sha-224", DrbgParameters.instantiation(256, PR_AND_RESEED, null));
        check("hash_drbg,sha-512,Pr_and_Reseed,192",
                DrbgParameters.instantiation(112, NONE, null),
                "Hash_DRBG", "SHA-512", "reseed_only", ",112");
        check("hash_drbg,sha-512,Pr_and_Reseed,192",
                DrbgParameters.instantiation(-1, NONE, null),
                "Hash_DRBG", "SHA-512", "reseed_only", ",192");
        // getInstance params can be stronger than definition
        check("hash_drbg,sha-256,None,112",
                DrbgParameters.instantiation(192, PR_AND_RESEED, null),
                "Hash_DRBG", "SHA-256", "pr_and_reseed", ",192");

        check("hash_drbg,sha-224", new MoreDrbgParameters(
                    null, null, "sha-512", null, false,
                    DrbgParameters.instantiation(-1, NONE, null)),
                "Hash_DRBG", "SHA-512");
        check("hash_drbg,sha-224", new MoreDrbgParameters(
                    null, null, null, null, false,
                    DrbgParameters.instantiation(-1, NONE, null)),
                "Hash_DRBG", "SHA-224");
        check("hash_drbg", new MoreDrbgParameters(
                    null, "hmac_drbg", null, null, false,
                    DrbgParameters.instantiation(-1, NONE, null)),
                "HMAC_DRBG", "SHA-256");

        check("hash_drbg,sha-224", new MoreDrbgParameters(
                    null, null, "sha-3", null, false,
                    DrbgParameters.instantiation(-1, NONE, null)));
        check("hash_drbg,sha-224", new MoreDrbgParameters(
                    null, "Unknown_DRBG", null, null, false,
                    DrbgParameters.instantiation(-1, NONE, null)));
    }

    /**
     * Checks all permutatins of a config. This is a recursive method and
     * should be called with checkPermutations(empty,config,expected).
     *
     * @param current the current chosen aspects
     * @param remains the remaining
     * @param expected the expected effective config
     * @throws Exception when check fails
     */
    private static void checkPermutations(List<String> current,
            List<String> remains, String... expected) throws Exception {
        if (remains.isEmpty()) {
            check(current.stream().collect(Collectors.joining(",")), expected);
        } else {
            for (String r : remains) {
                List<String> newCurrent = new ArrayList<>(current);
                newCurrent.add(r);
                List<String> newRemains = new ArrayList<>(remains);
                newRemains.remove(r);
                checkPermutations(newCurrent, newRemains, expected);
            }
        }
    }

    /**
     * Checks DRBG definition for getInstance(alg, params).
     *
     * @param define DRBG
     * @param params getInstance request (null if none)
     * @param expected expected actual instantiate params, empty if should fail
     */
    static void check(String define, SecureRandomParameters params,
                      String... expected) throws Exception {
        System.out.println("Testing " + define + " with " + params + "...");
        String old = Security.getProperty("securerandom.drbg.config");
        if (define != null) {
            Security.setProperty("securerandom.drbg.config", define);
        }
        try {
            String result = params != null ?
                    SecureRandom.getInstance("DRBG", params).toString() :
                    SecureRandom.getInstance("DRBG").toString();
            System.out.println("Result " + result);
            if (expected.length == 0) {
                throw new Exception("should fail");
            }
            for (String s : expected) {
                if (!result.contains(s)) {
                    throw new Exception(result);
                }
            }
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Result NSAE");
            if (expected.length > 0) {
                throw e;
            }
        } finally {
            Security.setProperty("securerandom.drbg.config", old);
        }
    }

    /**
     * Checks DRBG definition for getInstance(alg).
     *
     * @param define DRBG
     * @param expected expected actual instantiate params, empty if should fail
     */
    static void check(String define, String... expected) throws Exception {
        check(define, null, expected);
    }
}
