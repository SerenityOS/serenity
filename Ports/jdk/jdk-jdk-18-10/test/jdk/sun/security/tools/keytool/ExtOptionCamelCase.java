/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8231950 8257497
 * @summary keytool -ext camel-case shorthand not working
 * @modules java.base/sun.security.tools.keytool
 *          java.base/sun.security.tools.keytool:open
 *          java.base/sun.security.util
 *          java.base/sun.security.x509
 * @compile -XDignore.symbol.file ExtOptionCamelCase.java
 * @run main ExtOptionCamelCase
 */

import sun.security.tools.keytool.Main;
import sun.security.util.DerValue;
import sun.security.x509.BasicConstraintsExtension;
import sun.security.x509.CertificateExtensions;
import sun.security.x509.Extension;
import sun.security.x509.KeyIdentifier;
import sun.security.x509.KeyUsageExtension;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.List;

public class ExtOptionCamelCase {
    static Method createV3Extensions;
    static Constructor<Main> ctor;
    static PublicKey pk;
    static Method oneOf;

    public static void main(String[] args) throws Exception {

        prepare();

        // Unseen ext name
        testCreateFail("abc");

        // camelCase match, both cases work
        testCreate("bc", BasicConstraintsExtension.class);
        testCreate("BC", BasicConstraintsExtension.class);

        // Prefix match
        testCreate("BasicC", BasicConstraintsExtension.class);

        // Ambiguous, digitalSignature or dataEncipherment?
        testCreateFail("ku=d");

        // prefix match
        testCreate("ku:c=dig", KeyUsageExtension.class,
                x -> x.get(KeyUsageExtension.DIGITAL_SIGNATURE));

        // camelCase match
        testCreate("ku=kE", KeyUsageExtension.class,
                x -> x.get(KeyUsageExtension.KEY_ENCIPHERMENT));

        // camelCase match must be only 1st+CAPITALs
        testCreateFail("ku=KeUs");

        // camelCase match, must be only 1st + all CAPITALs
        testCreate("ku=kCS", KeyUsageExtension.class,
                x -> x.get(KeyUsageExtension.KEY_CERTSIGN));

        // ... not all CAPITALs
        testCreateFail("ku=kC");

        // ... has lowercase letters
        testCreateFail("ku=keCeSi");

        // Ambiguous, keyAgreement or keyCertSign
        testCreateFail("ku:c=ke");

        // camelCase natch
        testCreate("ku:c=dE", KeyUsageExtension.class,
                x -> x.get(KeyUsageExtension.DATA_ENCIPHERMENT));
        // prefix match
        testCreate("ku:c=de", KeyUsageExtension.class,
                x -> x.get(KeyUsageExtension.DECIPHER_ONLY));

        // camelCase match
        testCreate("ku:c=kA", KeyUsageExtension.class,
                x -> x.get(KeyUsageExtension.KEY_AGREEMENT));

        // camelCase match, fallback
        testCreate("ku:c=ka", KeyUsageExtension.class,
                x -> x.get(KeyUsageExtension.KEY_AGREEMENT));

        // Testing oneOf() directly
        testOneOf("a", -1, "b", "c"); // -1 means not found
        testOneOf("a", -2, "ab", "ac"); // -2 means ambiguous

        testOneOf("a", 0, "a", "ac"); //exact match
        testOneOf("a", 0, "a", "b");
        testOneOf("ac", 1, "a", "ac");

        testOneOf("a", 0, "abc", "bcd");
        testOneOf("ab", 0, "abc", "ABC");
        testOneOf("ab", 0, "abc", "aBC");
        testOneOf("ab", 0, "abc", "Abc");
        testOneOf("AB", 1, "abc", "ABC");
        testOneOf("aB", 0, "abcBcd", "abcDef");
        testOneOf("ab", -2, "abcBcd", "abcDef");
        testOneOf("aB", -2, "abcBcdEfg", "abcDef");

        testOneOf("ab", 0, "abcDef", "axyBuv");
        testOneOf("aB", 1, "abcDef", "axyBuv");
        testOneOf("a", -2, "abcDef", "axyBuv");

        testOneOf("aBC", -1, "a12BxyCuvDmn"); // 12 is not removed
        testOneOf("a12BCD", 0, "a12BxyCuvDmn");
        testOneOf("a12BC", -1, "a12BxyCuvDmn"); // must be full

        // Fallback
        testOneOf("bc", 0, "BasicConstraints");
        testOneOf("BC", 0, "BasicConstraints");
        testOneOf("BasicConstraints", 0, "BasicConstraints");
        testOneOf("basicconstraints", 0, "BasicConstraints");
        testOneOf("Basic", 0, "BasicConstraints");
        testOneOf("basic", 0, "BasicConstraints");

        testOneOf("BaCo", -1, "BasicConstraints");
    }

    // Expose some private methods
    static void prepare() throws Exception {
        createV3Extensions = Main.class.getDeclaredMethod(
                "createV3Extensions",
                CertificateExtensions.class,
                CertificateExtensions.class,
                List.class,
                PublicKey.class,
                KeyIdentifier.class);
        createV3Extensions.setAccessible(true);
        ctor = Main.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        pk = kpg.generateKeyPair().getPublic();

        oneOf = Main.class.getDeclaredMethod(
                "oneOf", String.class, String[].class);
        oneOf.setAccessible(true);
    }

    /**
     * Ensures the given type of extension is created with the option
     */
    static <T extends Extension> void testCreate(String option, Class<T> clazz)
            throws Exception {
        testCreate(option, clazz, null);
    }

    /**
     * Ensures an option is invalid and will be rejected
     */
    static <T extends Extension> void testCreateFail(String option)
            throws Exception {
        testCreate(option, null, null);
    }

    /**
     * Ensures the given type of extension is created and match the rule
     * with the option.
     *
     * @param option the -ext option provided to keytool
     * @param clazz the expected extension to create, null means none
     * @param rule a predicate to check if the extension created is acceptable
     * @param <T> the extected extension type
     * @throws Exception if test result is unexpected
     */
    static <T extends Extension> void testCreate(String option, Class<T> clazz,
            PredicateWithException<T> rule) throws Exception {
        try {
            CertificateExtensions exts = (CertificateExtensions)
                    createV3Extensions.invoke(ctor.newInstance(),
                            null, null, List.of(option), pk, null);

            // ATTENTION: the extensions created above might contain raw
            // extensions (not of a subtype) and we need to store and reload
            // it to resolve them to subtypes.
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            exts.encode(bout);
            exts = new CertificateExtensions(new DerValue(bout.toByteArray()).data);

            if (clazz == null) {
                throw new Exception("Should fail");
            } else {
                for (Extension e : exts.getAllExtensions()) {
                    if (e.getClass() == clazz) {
                        if (rule == null || rule.test((T) e)) {
                            return;
                        }
                    }
                }
                throw new Exception("Unexpected result: " + exts);
            }
        } catch (InvocationTargetException e) {
            if (clazz == null) {
                return;
            } else {
                throw e;
            }
        }
    }

    @FunctionalInterface
    interface PredicateWithException<T> {
        boolean test(T t) throws Exception;
    }

    /**
     * Ensures oneOf returns the expected result.
     *
     * @param s input
     * @param expected expected value, -2 if ambiguous, -1 if no match
     * @param items existing strings to match
     * @throws Exception if test result is unexpected
     */
    static void testOneOf(String s, int expected, String... items)
            throws Exception {
        try {
            int res = (int)oneOf.invoke(null, s, items);
            if (expected == -2) {
                throw new Exception("Should fail");
            } else {
                if (expected != res) {
                    throw new Exception(
                            "Expected " + expected + ", actually " + res);
                }
            }
        } catch (InvocationTargetException e) {
            if (expected == -2) {
                return;
            } else {
                throw e;
            }
        }
    }
}
