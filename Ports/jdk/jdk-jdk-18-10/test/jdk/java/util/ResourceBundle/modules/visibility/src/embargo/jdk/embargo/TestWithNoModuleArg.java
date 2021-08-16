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

package jdk.embargo;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class TestWithNoModuleArg {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: java ... basename should-be-loaded-flag");
            System.out.println("  ex. java ... jdk.test.resources.classes.MyResources false");
            return;
        }

        String basename = args[0];
        boolean shouldBeLoaded = "true".equals(args[1]);

        int errors = 0;
        try {
            // Set the default Locale to Locale.ROOT to avoid any confusions related to fallback
            Locale.setDefault(Locale.ROOT);
            ResourceBundle rb = ResourceBundle.getBundle(basename);
            if (shouldBeLoaded) {
                System.out.println("Passed: got resource bundle:");
            } else {
                System.out.println("Failed: no MissingResourceException thrown");
                errors++;
            }
            System.out.println("            bundle = " + rb);
        } catch (MissingResourceException e) {
            if (!shouldBeLoaded) {
                System.out.println("Passed: got expected " + e);
            } else {
                System.out.println("Failed: got unexpected " + e);
                errors++;
            }
            System.out.println("            cause = " + e.getCause());
        } catch (Throwable t) {
            System.out.println("Failed: unexpected throwable: " + t);
            errors++;
        }

        if (errors > 0) {
            throw new RuntimeException(errors + " errors");
        }
    }
}
