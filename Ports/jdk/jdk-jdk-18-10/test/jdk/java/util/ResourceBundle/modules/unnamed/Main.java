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


import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Main {
    public static void main(String[] args) throws Exception {
        int errors = 0;
        for (String loctag : args) {
            Locale locale = Locale.forLanguageTag(loctag);
            if (locale.equals(Locale.ROOT)) {
                continue;
            }
            ResourceBundle rb = ResourceBundle.getBundle("jdk.test.resources.MyResources",
                                                         locale);
            String tag = locale.toLanguageTag(); // normalized
            String value = rb.getString("key");
            System.out.println(rb.getBaseBundleName() + ": locale = " + tag + ", value = " + value);
            if (!value.startsWith(tag + ':')) {
                System.out.println("ERROR: " + value + " expected: " + tag);
                errors++;
            }

            // inaccessible bundles
            try {
                ResourceBundle.getBundle("jdk.test.internal.resources.Foo", locale);
                System.out.println("ERROR: jdk.test.internal.resources.Foo should not be accessible");
                errors++;
            } catch (MissingResourceException e) {
                e.printStackTrace();

                Throwable cause = e.getCause();
                System.out.println("Expected: " +
                    (cause != null ? cause.getMessage() : e.getMessage()));
            }
        }
        if (errors > 0) {
            throw new RuntimeException(errors + " errors");
        }
    }
}
