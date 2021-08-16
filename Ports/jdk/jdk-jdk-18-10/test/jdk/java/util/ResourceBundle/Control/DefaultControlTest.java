/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5102289 6278334 8261179
 * @summary Test the default Control implementation. The expiration
 * functionality of newBundle, getTimeToLive, and needsReload is
 * tested by ExpirationTest.sh. The factory methods are tested
 * separately.
 * @build TestResourceRB
 * @build NonResourceBundle
 * @run main DefaultControlTest
 */

import java.util.*;
import static java.util.ResourceBundle.Control.*;

public class DefaultControlTest {
    // The ResourceBundle.Control instance
    static final ResourceBundle.Control CONTROL
        = ResourceBundle.Control.getControl(FORMAT_DEFAULT);

    static final ResourceBundle BUNDLE = new ResourceBundle() {
            public Enumeration<String> getKeys() { return null; }
            protected Object handleGetObject(String key) { return null; }
        };

    static final String CLAZZ = FORMAT_CLASS.get(0);

    static final String PROPERTIES = FORMAT_PROPERTIES.get(0);

    static final ClassLoader LOADER = DefaultControlTest.class.getClassLoader();

    // Full arguments for NPE testing
    static final Object[] FULLARGS = { "any",
                                       Locale.US,
                                       FORMAT_PROPERTIES.get(0),
                                       LOADER,
                                       BUNDLE };

    static int errors;

    public static void main(String[] args) {
        checkConstants();

        // Test getFormats(String)
        testGetFormats();

        // Test getCandidateLocales(String, Locale)
        testGetCandidateLocales();

        // Test getFallbackLocale(String, Locale)
        testGetFallbackLocale();

        // Test newBundle(String, Locale, String, ClassLoader, boolean)
        testNewBundle();

        // Test toBundleName(String, Locale)
        testToBundleName();

        // Test getTimeToLive(String, Locale)
        testGetTimeToLive();

        // Test needsReload(String, Locale, String, ClassLoader,
        //                  ResourceBundle, long)
        testNeedsReload();

        // Test toResourceName(String, String)
        testToResourceName();

        if (errors > 0) {
            throw new RuntimeException("FAILED: " + errors + " error(s)");
        }
    }

    private static void checkConstants() {
        // Check FORMAT_*
        if (!CONTROL.FORMAT_DEFAULT.equals(Arrays.asList("java.class",
                                                        "java.properties"))) {
            error("Wrong Control.FORMAT_DEFAULT");
        }
        checkImmutableList(CONTROL.FORMAT_DEFAULT);
        if (!CONTROL.FORMAT_CLASS.equals(Arrays.asList("java.class"))) {
            error("Wrong Control.FORMAT_CLASS");
        }
        checkImmutableList(CONTROL.FORMAT_CLASS);
        if (!CONTROL.FORMAT_PROPERTIES.equals(Arrays.asList("java.properties"))) {
            error("Wrong Control.FORMAT_PROPERTIES");
        }
        checkImmutableList(CONTROL.FORMAT_PROPERTIES);

        // Check TTL_*
        if (CONTROL.TTL_DONT_CACHE != -1) {
            error("Wrong Control.TTL_DONT_CACHE: %d%n", CONTROL.TTL_DONT_CACHE);
        }
        if (CONTROL.TTL_NO_EXPIRATION_CONTROL != -2) {
            error("Wrong Control.TTL_NO_EXPIRATION_CONTROL: %d%n",
                  CONTROL.TTL_NO_EXPIRATION_CONTROL);
        }
    }

    private static void checkImmutableList(List<String> list) {
        try {
            list.add("hello");
            error("%s is mutable%n", list);
        } catch (UnsupportedOperationException e) {
        }
    }

    private static void testGetFormats() {
        List<String> list = CONTROL.getFormats("foo");
        if (list != CONTROL.FORMAT_DEFAULT) {
            error("getFormats returned " + list);
        }
        try {
            list = CONTROL.getFormats(null);
            error("getFormats doesn't throw NPE.");
        } catch (NullPointerException e) {
        }
    }

    private static void testGetCandidateLocales() {
        Map<Locale, Locale[]> candidateData = new HashMap<Locale, Locale[]>();
        candidateData.put(new Locale("ja", "JP", "YOK"), new Locale[] {
                              new Locale("ja", "JP", "YOK"),
                              new Locale("ja", "JP"),
                              new Locale("ja"),
                              Locale.ROOT });
        candidateData.put(new Locale("ja", "JP"), new Locale[] {
                              new Locale("ja", "JP"),
                              new Locale("ja"),
                              Locale.ROOT });
        candidateData.put(new Locale("ja"), new Locale[] {
                              new Locale("ja"),
                              Locale.ROOT });

        candidateData.put(new Locale("ja", "", "YOK"), new Locale[] {
                              new Locale("ja", "", "YOK"),
                              new Locale("ja"),
                              Locale.ROOT });
        candidateData.put(new Locale("", "JP", "YOK"), new Locale[] {
                              new Locale("", "JP", "YOK"),
                              new Locale("", "JP"),
                              Locale.ROOT });
        candidateData.put(new Locale("", "", "YOK"), new Locale[] {
                              new Locale("", "", "YOK"),
                              Locale.ROOT });
        candidateData.put(new Locale("", "JP"), new Locale[] {
                              new Locale("", "JP"),
                              Locale.ROOT });
        candidateData.put(Locale.ROOT, new Locale[] {
                              Locale.ROOT });

        // Norwegian Bokmal
        candidateData.put(Locale.forLanguageTag("nb-NO-POSIX"), new Locale[] {
                Locale.forLanguageTag("nb-NO-POSIX"),
                Locale.forLanguageTag("no-NO-POSIX"),
                Locale.forLanguageTag("nb-NO"),
                Locale.forLanguageTag("no-NO"),
                Locale.forLanguageTag("nb"),
                Locale.forLanguageTag("no"),
                Locale.ROOT});
        candidateData.put(Locale.forLanguageTag("no-NO-POSIX"), new Locale[] {
                Locale.forLanguageTag("no-NO-POSIX"),
                Locale.forLanguageTag("nb-NO-POSIX"),
                Locale.forLanguageTag("no-NO"),
                Locale.forLanguageTag("nb-NO"),
                Locale.forLanguageTag("no"),
                Locale.forLanguageTag("nb"),
                Locale.ROOT});


        for (Locale locale : candidateData.keySet()) {
            List<Locale> candidates = CONTROL.getCandidateLocales("any", locale);
            List<Locale> expected = Arrays.asList(candidateData.get(locale));
            if (!candidates.equals(expected)) {
                error("Wrong candidates for %s: got %s, expected %s%n",
                      toString(locale), candidates, expected);
            }
        }
        final int NARGS = 2;
        for (int mask = 0; mask < (1 << NARGS)-1; mask++) {
            Object[] data = getNpeArgs(NARGS, mask);
            try {
                List<Locale> candidates = CONTROL.getCandidateLocales((String) data[0],
                                                                      (Locale) data[1]);
                error("getCandidateLocales(%s, %s) doesn't throw NPE.%n",
                      data[0], toString((Locale)data[1]));
            } catch (NullPointerException e) {
            }
        }
    }

    private static void testGetFallbackLocale() {
        Locale current = Locale.getDefault();
        Locale.setDefault(Locale.ITALY);
        try {
            Locale loc = CONTROL.getFallbackLocale("any", Locale.FRANCE);
            if (loc != Locale.ITALY) {
                error("getFallbackLocale: got %s, expected %s%n",
                      toString(loc), toString(Locale.ITALY));
            }
            loc = CONTROL.getFallbackLocale("any", Locale.ITALY);
            if (loc != null) {
                error("getFallbackLocale: got %s, expected null%n", toString(loc));
            }
        } finally {
            Locale.setDefault(current);
        }

        final int NARGS = 2;
        for (int mask = 0; mask < (1 << NARGS)-1; mask++) {
            Object[] data = getNpeArgs(NARGS, mask);
            try {
                Locale loc = CONTROL.getFallbackLocale((String) data[0], (Locale) data[1]);
                error("getFallbackLocale(%s, %s) doesn't throw NPE.%n", data[0], data[1]);
            } catch (NullPointerException e) {
            }
        }
    }

    private static void testNewBundle() {
        int testNo = 0;
        ResourceBundle rb = null;
        try {
            testNo = 1;
            rb = CONTROL.newBundle("StressOut", Locale.JAPANESE,
                                   PROPERTIES, LOADER, false);
            String s = rb.getString("data");
            if (!s.equals("Japan")) {
                error("newBundle: #%d got %s, expected Japan%n", testNo, s);
            }

            testNo = 2;
            rb = CONTROL.newBundle("TestResourceRB", Locale.ROOT,
                                   CLAZZ, LOADER, false);
            s = rb.getString("type");
            if (!s.equals(CLAZZ)) {
                error("newBundle: #%d got %s, expected %s%n", testNo, s, CLAZZ);
            }
        } catch (Throwable e) {
            error("newBundle: #%d threw %s%n", testNo, e);
            e.printStackTrace();
        }

        // Test exceptions

        try {
            // MalformedDataRB contains an invalid Unicode notation which
            // causes to throw an IllegalArgumentException.
            rb = CONTROL.newBundle("MalformedDataRB", Locale.ENGLISH,
                                   PROPERTIES, LOADER, false);
            error("newBundle: doesn't throw IllegalArgumentException with malformed data.");
        } catch (IllegalArgumentException iae) {
        } catch (Exception e) {
            error("newBundle: threw %s%n", e);
        }

        try {
            rb = CONTROL.newBundle("StressOut", Locale.JAPANESE,
                                   "foo.bar", LOADER, false);
            error("newBundle: doesn't throw IllegalArgumentException with invalid format.");
        } catch (IllegalArgumentException iae) {
        } catch (Exception e) {
            error("newBundle: threw %s%n", e);
        }

        try {
            rb = CONTROL.newBundle("NonResourceBundle", Locale.ROOT,
                                   "java.class", LOADER, false);
            error("newBundle: doesn't throw ClassCastException with a non-ResourceBundle subclass.");
        } catch (ClassCastException cce) {
        } catch (Exception e) {
            error("newBundle: threw %s%n", e);
        }

        // NPE test
        final int NARGS = 4;
        for (int mask = 0; mask < (1 << NARGS)-1; mask++) {
            Object[] data = getNpeArgs(NARGS, mask);
            Locale loc = (Locale) data[1];
            try {
                rb = CONTROL.newBundle((String) data[0], loc,
                                       (String) data[2], (ClassLoader) data[3],
                                       false);
                error("newBundle(%s, %s, %s, %s, false) doesn't throw NPE.%n",
                      data[0], toString(loc), data[2], data[3]);
            } catch (NullPointerException npe) {
            } catch (Exception e) {
                error("newBundle(%s, %s, %s, %s, false) threw %s.%n",
                      data[0], toString(loc), data[2], data[3], e);
            }
        }
    }

    private static void testGetTimeToLive() {
        long ttl = CONTROL.getTimeToLive("any", Locale.US);
        if (ttl != CONTROL.TTL_NO_EXPIRATION_CONTROL) {
            error("getTimeToLive: got %d, expected %d%n", ttl,
                  CONTROL.TTL_NO_EXPIRATION_CONTROL);
        }
        final int NARGS = 2;
        for (int mask = 0; mask < (1 << NARGS)-1; mask++) {
            Object[] data = getNpeArgs(NARGS, mask);
            try {
                ttl = CONTROL.getTimeToLive((String) data[0], (Locale) data[1]);
                error("getTimeToLive(%s, %s) doesn't throw NPE.%n", data[0], data[1]);
            } catch (NullPointerException e) {
            }
        }
    }

    // The functionality of needsReload is tested by
    // ExpirationTest.sh. Only parameter checking is tested here.
    private static void testNeedsReload() {
        long loadTime = System.currentTimeMillis();

        // NPE test
        final int NARGS = 5;
        for (int mask = 0; mask < (1 << NARGS)-1; mask++) {
            Object[] data = getNpeArgs(NARGS, mask);
            Locale loc = (Locale) data[1];
            try {
                boolean b = CONTROL.needsReload((String) data[0], loc,
                                                (String) data[2], (ClassLoader) data[3],
                                                (ResourceBundle) data[4], loadTime);
                error("needsReload(%s, %s, %s, %s, %s, loadTime) doesn't throw NPE.%n",
                      data[0], toString(loc), data[2], data[3], data[4]);
            } catch (NullPointerException e) {
            }
        }
    }

    private static void testToBundleName() {
        final String name = "J2SE";
        Map<Locale, String> bundleNames = new HashMap<Locale, String>();
        bundleNames.put(new Locale("ja", "JP", "YOK"),
                        name + "_" + "ja" + "_" + "JP" + "_" + "YOK");
        bundleNames.put(new Locale("ja", "JP"),
                        name + "_" + "ja" + "_" + "JP");
        bundleNames.put(new Locale("ja"),
                        name + "_" + "ja");
        bundleNames.put(new Locale("ja", "", "YOK"),
                        name + "_" + "ja" + "_" + "" + "_" + "YOK");
        bundleNames.put(new Locale("", "JP", "YOK"),
                        name + "_" + "" + "_" + "JP" + "_" + "YOK");
        bundleNames.put(new Locale("", "", "YOK"),
                        name + "_" + "" + "_" + "" + "_" + "YOK");
        bundleNames.put(new Locale("", "JP"),
                        name + "_" + "" + "_" + "JP");
        bundleNames.put(Locale.ROOT,
                        name);

        for (Locale locale : bundleNames.keySet()) {
            String bn = CONTROL.toBundleName(name, locale);
            String expected = bundleNames.get(locale);
            if (!bn.equals(expected)) {
                error("toBundleName: got %s, expected %s%n", bn, expected);
            }
        }

        final int NARGS = 2;
        for (int mask = 0; mask < (1 << NARGS)-1; mask++) {
            Object[] data = getNpeArgs(NARGS, mask);
            try {
                String s = CONTROL.toBundleName((String) data[0], (Locale) data[1]);
                error("toBundleName(%s, %s) doesn't throw NPE.%n", data[0], data[1]);
            } catch (NullPointerException e) {
            }
        }
    }

    private static void testToResourceName() {
        String[][] names = {
            // bundleName,   suffix, expected result
            { "com.sun.J2SE", "xml", "com/sun/J2SE.xml" },
            { ".J2SE", "xml", "/J2SE.xml" },
            { "J2SE", "xml", "J2SE.xml" },
            { "com/sun/J2SE", "xml", "com/sun/J2SE.xml" },
            { "com.sun.J2SE", "", "com/sun/J2SE." },
            { ".", "", "/." },
            { "", "", "." },

            // data for NPE tests
            { null, "any", null },
            { "any", null, null },
            { null, null, null },
            };

        for (String[] data : names) {
            String result = null;
            boolean npeThrown = false;
            try {
                result = CONTROL.toResourceName(data[0], data[1]);
            } catch (NullPointerException npe) {
                npeThrown = true;
            }
            String expected = data[2];
            if (expected != null) {
                if (!result.equals(expected)) {
                    error("toResourceName: got %s, expected %s%n", result, data[2]);
                }
            } else {
                if (!npeThrown) {
                    error("toResourceName(%s, %s) doesn't throw NPE.%n", data[0], data[1]);
                }
            }
        }
    }

    /**
     * Produces permutations argument data that contains at least one
     * null.
     */
    private static Object[] getNpeArgs(int length, int mask) {
        Object[] data = new Object[length];
        for (int i = 0; i < length; i++) {
            if ((mask & (1 << i)) == 0) {
                data[i] = null;
            } else {
                data[i] = FULLARGS[i];
            }
        }
        return data;
    }

    private static String toString(Locale loc) {
        if (loc == null)
            return "null";
        return "\"" + loc.getLanguage() + "_" + loc.getCountry() + "_" + loc.getVariant() + "\"";
    }

    private static void error(String msg) {
        System.out.println(msg);
        errors++;
    }

    private static void error(String fmt, Object... args) {
        System.out.printf(fmt, args);
        errors++;
    }
}
