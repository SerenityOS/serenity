/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5102289
 * @summary Test if ResourceBundle.getBundle detects bad Control implementations.
 */

import java.io.*;
import java.util.*;
import static java.util.ResourceBundle.Control.*;

public class BadControlsTest {
    public static void main(String[] args) {
        ResourceBundle.Control control;

        control = new ResourceBundle.Control() {
                public List<String> getFormats(String name) {
                    return null;
                }
            };
        testControl(control, "getFormats returns null");

        control = new ResourceBundle.Control() {
                public List<String> getFormats(String name) {
                    return Arrays.asList("java.class", null, "java.properties");
                }
            };
        testControl(control, "getFormats returns a List containing null");

        control = new ResourceBundle.Control() {
                public List<Locale> getCandidateLocales(String name, Locale loc) {
                    return null;
                }
            };
        testControl(control, "getCandidateLocales returns null");

        control = new ResourceBundle.Control() {
                public List<Locale> getCandidateLocales(String name, Locale loc) {
                    return Arrays.asList(Locale.US, null, Locale.ENGLISH);
                }
            };
        testControl(control, "getCandidateLocales returns a List containing null");

        long[] badTtls = {
            TTL_NO_EXPIRATION_CONTROL - 1,
            -10000,
            Long.MIN_VALUE
        };
        for (final long ttl : badTtls) {
            control = new ResourceBundle.Control() {
                    public long getTimeToLive(String name, Locale loc) {
                        return ttl;
                    }
                };
            testControl(control, "getTimeToLive returns " + ttl);
        }

        control = new ResourceBundle.Control() {
                public String toBundleName(String name, Locale loc) {
                    return null;
                }
            };
        try {
            ResourceBundle rb = ResourceBundle.getBundle("StressOut", control);
            throw new RuntimeException("toBundleName returns null");
        } catch (MissingResourceException e) {
            if (!(e.getCause() instanceof NullPointerException)) {
                throw new RuntimeException("toBundleName returns null. The cause isn't NPE.");
            }
        }

        // null Control tests
        control = null;
        try {
            ResourceBundle rb = ResourceBundle.getBundle("StressOut", control);
            throw new RuntimeException("getBundle doesn't throw NPE with null Control");
        } catch (NullPointerException e) {
            // OK
        }
    }

    private static void testControl(ResourceBundle.Control control, String testTitle) {
        try {
            ResourceBundle rb = ResourceBundle.getBundle("StressOut", control);
            throw new RuntimeException(testTitle);
        } catch (IllegalArgumentException e) {
            System.out.println(testTitle + ": PASSED (" + e.getMessage() + ")");
        }
    }
}
