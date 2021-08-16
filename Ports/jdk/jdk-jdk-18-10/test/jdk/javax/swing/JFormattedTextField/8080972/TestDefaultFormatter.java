/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.text.ParseException;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultFormatter;
/*
 * @test
 * @bug 8080972
 * @run main/othervm -Djava.security.manager=allow  TestDefaultFormatter
 * @summary Audit Core Reflection in module java.desktop for places that will
 *          require changes to work with modules
 * @author Alexander Scherbatiy
 */

public class TestDefaultFormatter {

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(TestDefaultFormatter::testDefaultFormatter);
        System.setSecurityManager(new SecurityManager());
        SwingUtilities.invokeAndWait(TestDefaultFormatter::testDefaultFormatter);
    }
    private static void testDefaultFormatter() {
        testDefaultFormatter(new DefaultFormatter() {
        });
        testDefaultFormatter(new DefaultFormatter());
    }

    private static void testDefaultFormatter(DefaultFormatter formatter ) {
        try {
            System.out.println("formatter: " + formatter.getClass());
            formatter.setValueClass(UserValueClass.class);
            UserValueClass userValue = (UserValueClass) formatter.stringToValue("test");

            if (!userValue.str.equals("test")) {
                throw new RuntimeException("String value is wrong!");
            }
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }

    }

    public static class UserValueClass {

        String str;

        public UserValueClass(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return "UserValueClass: " + str;
        }
    }
}
