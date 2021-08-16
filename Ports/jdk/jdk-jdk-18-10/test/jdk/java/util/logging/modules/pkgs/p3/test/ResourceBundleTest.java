/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package p3.test;

import java.util.logging.Logger;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import p2.test.ModuleLoggerAccess;

/*
 * Test Logger.getLogger + logger.getResourceBundle in unnamed/named module,
 * resources are in named and unnamed modules respectively.
 */
public class ResourceBundleTest {
    private static final String RESOURCE_KEY = "OkKey";
    private static final String RESOURCE_VALUE = "OK";

    private static final String HELP_MSG =
            "Test that a class in a %s module %s obtain a logger " +
            "which uses a resource bundle %s bundled in " +
            "%s module. ( The package in which the resource is " +
            "contained is not exported by the module )";
    private static final String NAMED_POSITIVE_CLASSBUNDLE_MSG =
            String.format(HELP_MSG, "named", "can", "class", "the same");
    private static final String UNNAMED_POSITIVE_CLASSBUNDLE_MSG =
            String.format(HELP_MSG, "unnamed", "can", "class", "the same");
    private static final String NAMED_POSITIVE_PROPERTYBUNDLE_MSG =
            String.format(HELP_MSG, "named", "can", "property", "the same");
    private static final String UNNAMED_POSITIVE_PROPERTYBUNDLE_MSG =
            String.format(HELP_MSG, "unnamed", "can", "property", "the same");
    private static final String NAMED_NEGATIVE_CLASSBUNDLE_MSG =
            String.format(HELP_MSG, "named", "cannot", "class", "another");
    private static final String UNNAMED_NEGATIVE_CLASSBUNDLE_MSG =
            String.format(HELP_MSG, "unnamed", "cannot", "class", "another");
    private static final String NAMED_NEGATIVE_PROPERTYBUNDLE_MSG =
            String.format(HELP_MSG, "named", "cannot", "property", "another");
    private static final String UNNAMED_NEGATIVE_PROPERTYBUNDLE_MSG =
            String.format(HELP_MSG, "unnamed", "cannot", "property", "another");

    public static void main(String[] args) {
        verifySetup();
        testLoggerRBs();
        failToLoadRBs();
    }

    static void verifySetup() {
        Module m = ResourceBundleTest.class.getModule();
        System.out.println("Module Name for ResourceBundleTest : " + m.getName());
        assertTrue(!m.isNamed());
        m = ModuleLoggerAccess.class.getModule();
        System.out.println("Module Name for ModuleLoggerAccess : " + m.getName());
        assertTrue(m.isNamed());
    }

    /*
     * Positive tests :
     *  Should be able to access class/property resource bundle in current module,
     *  no matter named or unnamed module.
     */
    static void testLoggerRBs() {
        testLoggerClassRBs();
        testLoggerPropertyRBs();
    }

    static void testLoggerClassRBs() {
        testLoggerResoureBundle(
                Logger.getLogger("mylogger.a", "p3.resource.ClassResource"),
                p3.resource.ClassResource.class,
                UNNAMED_POSITIVE_CLASSBUNDLE_MSG);
        testLoggerResoureBundle(
                ModuleLoggerAccess.getLogger("mylogger.b", "p2.resource.ClassResource"),
                ModuleLoggerAccess.getResourceClass(),
                NAMED_POSITIVE_CLASSBUNDLE_MSG);
    }

    static void testLoggerPropertyRBs() {
        testLoggerResoureBundle(
                Logger.getLogger("mylogger.c", "p3.resource.p"),
                PropertyResourceBundle.class,
                UNNAMED_POSITIVE_PROPERTYBUNDLE_MSG);
        testLoggerResoureBundle(
                ModuleLoggerAccess.getLogger("mylogger.d", "p2.resource.p"),
                PropertyResourceBundle.class,
                NAMED_POSITIVE_PROPERTYBUNDLE_MSG);
    }

    static void testLoggerResoureBundle(Logger logger, Class<?> rbType, String helpMsg) {
        System.out.println(helpMsg);
        ResourceBundle rb = logger.getResourceBundle();
        assertTrue(rbType.isInstance(rb));
        assertTrue(RESOURCE_VALUE.equals(rb.getString(RESOURCE_KEY)));
    }

    /*
     * Negative tests :
     *  MissingResourceException should be thrown when access class/property resource bundle
     *  from another module, no matter named or unnamed module.
     */
    static void failToLoadRBs() {
        failToLoadClassRBs();
        failToLoadPropertyRBs();
    }

    static void failToLoadClassRBs() {
        // in an unnamed module, try to create a logger with
        // class resource bundle in named module m1 or m2.
        failToLoadResourceBundle("mylogger.e", "p1.resource.ClassResource",
                false, UNNAMED_NEGATIVE_CLASSBUNDLE_MSG);
        failToLoadResourceBundle("mylogger.f", "p2.resource.ClassResource",
                false, UNNAMED_NEGATIVE_CLASSBUNDLE_MSG);
        // in named module m2, try to create a logger with
        // class resource bundle in another named module m1.
        failToLoadResourceBundle("mylogger.g", "p1.resource.ClassResource",
                true, NAMED_NEGATIVE_CLASSBUNDLE_MSG);
    }

    static void failToLoadPropertyRBs() {
        // in an unnamed module, try to create a logger with
        // property resource bundle in named module m1 or m2.
        failToLoadResourceBundle("mylogger.i", "p1.resource.p",
                false, UNNAMED_NEGATIVE_PROPERTYBUNDLE_MSG);
        failToLoadResourceBundle("mylogger.j", "p2.resource.p",
                false, UNNAMED_NEGATIVE_PROPERTYBUNDLE_MSG);
        // in named module m2, try to create a logger with
        // property resource bundle in another named module m1.
        failToLoadResourceBundle("mylogger.k", "p1.resource.p",
                true, NAMED_NEGATIVE_PROPERTYBUNDLE_MSG);
    }

    static void failToLoadResourceBundle(String loggerName, String rbName,
            boolean getLoggerInNamedModule, String helpMsg) {
        String msg = String.format(
                "Logger : %s. Expected exception is not thrown for ResourceBundle : %s.",
                loggerName, rbName);
        System.out.println(helpMsg);
        try {
            if(getLoggerInNamedModule) {
                ModuleLoggerAccess.getLogger(loggerName, rbName);
            } else {
                Logger.getLogger(loggerName, rbName);
            }
            throw new RuntimeException(msg);
        } catch (MissingResourceException expected) {
            System.out.println("Get expected exception : " + expected);
            return;
        }
    }

    public static void assertTrue(boolean b) {
        if (!b) {
            throw new RuntimeException("Expect true, get false!");
        }
    }
}
