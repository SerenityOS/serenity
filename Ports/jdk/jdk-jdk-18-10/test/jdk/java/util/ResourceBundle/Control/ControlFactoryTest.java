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
 * @summary Test the ResourceBundle.Control factory methods.
 */

import java.util.*;
import static java.util.ResourceBundle.Control.*;

public class ControlFactoryTest {

    /**
     * An interface for calling ResourceBundle.Control.getControl or
     * ResourceBundle.Control.getNoFallbackControl.
     */
    private static interface Factory {
        public ResourceBundle.Control getControl(List<String> formats);
        public String name();
    }

    static int errors;

    public static void main(String[] args) {
        // Test getControl.
        testControlFactory(new Factory() {
                public ResourceBundle.Control getControl(List<String> formats) {
                    return ResourceBundle.Control.getControl(formats);
                }
                public String name() { return "getControl"; }
            }, Locale.getDefault());

        // Test getNoFallbackControl.
        testControlFactory(new Factory() {
                public ResourceBundle.Control getControl(List<String> formats) {
                    return ResourceBundle.Control.getNoFallbackControl(formats);
                }
                public String name() { return "getNoFallbackControl"; }
            }, null);

        if (errors > 0) {
            throw new RuntimeException("FAILED: " + errors + " error(s)");
        }
    }

    private static void testControlFactory(Factory factory, Locale loc) {
        testGetControl(factory, loc, FORMAT_DEFAULT, "java.class", "java.properties");
        testGetControl(factory, loc, FORMAT_CLASS, "java.class");
        testGetControl(factory, loc, FORMAT_PROPERTIES, "java.properties");

        // test IllegalArgumentException
        String[][] data = {
            { "java.class", "java.properties", "java.xml" },
            { "java.class", "java.props" },
            { "java.properties", "java.class" },
            { "java.foo", "java.properties" },
            { "java.foo" },
            { null },
        };
        for (String[] fmts : data) {
            try {
                List<String> fmt = Arrays.asList(fmts);
                ResourceBundle.Control control = factory.getControl(fmt);
                error("getControl: %s%n", fmt);
            } catch (IllegalArgumentException e) {
            }
        }

        // test NPE
        try {
            ResourceBundle.Control control = factory.getControl(null);
            error("%s: doesn't throw NPE.%n", factory.name());
        } catch (NullPointerException npe) {
        }
    }

    private static void testGetControl(Factory factory,
                                       Locale loc,
                                       final List<String> FORMATS,
                                       String... fmtStrings) {
        final ResourceBundle.Control CONTROL = factory.getControl(FORMATS);
        List<String> fmt = CONTROL.getFormats("any");
        if (fmt != FORMATS) {
            error("%s: returns %s, expected %s.%n",
                  factory.name(), fmt, FORMATS);
        }
        ResourceBundle.Control control = null;

        // Check if getControl always returns the expected singleton.
        for (int i = 0; i < 10; i++) {
            fmt = Arrays.asList(fmtStrings);
            control = factory.getControl(fmt);
            if (control != CONTROL) {
                error("%s: doesn't return the singleton: got %s, expected %s%n",
                      factory.name(), control, CONTROL);
            }
        }

        // Check if getFallbackLocale performs as expected.
        Locale defaultLocale = Locale.getDefault();
        Locale nonDefaultLocale = defaultLocale.equals(Locale.US) ? Locale.JAPAN : Locale.US;
        if (loc != null) {
            // Test ResourceBundle.Control.getControl()
            Locale l = CONTROL.getFallbackLocale("any", nonDefaultLocale);
            if (!defaultLocale.equals(l)) {
                error("%s: getFallbackLocale doesn't return default locale. got %s, expected %s%n",
                      factory.name(), toString(l), toString(defaultLocale));
            }
            l = CONTROL.getFallbackLocale("any", defaultLocale);
            if (l != null) {
                error("%s: getFallbackLocale doesn't return null. got %s%n",
                      factory.name(), toString(l));
            }
        } else {
            // Test ResourceBundle.Control.getNoFallbackControl()
            Locale l = CONTROL.getFallbackLocale("any", nonDefaultLocale);
            if (l != null) {
                error("%s: getFallbackLocale doesn't return null. got %s%n",
                      factory.name(), toString(l));
            }
            l = CONTROL.getFallbackLocale("any", defaultLocale);
            if (l != null) {
                error("%s: getFallbackLocale doesn't return null. got %s%n",
                      factory.name(), toString(l));
            }
        }
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
