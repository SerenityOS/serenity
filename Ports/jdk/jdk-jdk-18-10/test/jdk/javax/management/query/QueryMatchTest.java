/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6266438
 * @summary Query.match code for character sequences like [a-z] is wrong.
 * @author Luis-Miguel Alventosa
 *
 * @run clean QueryMatchTest
 * @run build QueryMatchTest
 * @run main QueryMatchTest
 */

import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.Query;
import javax.management.QueryExp;

public class QueryMatchTest {

    public static interface SimpleMBean {
        public String getStringNumber();
    }

    public static class Simple implements SimpleMBean {
        public Simple(String number) {
            this.number = number;
        }
        public String getStringNumber() {
            return number;
        }
        private String number;
    }

    // Pattern = 2[7-9]
    private static String[][] data11 = {
        { "20", "KO" },
        { "21", "KO" },
        { "22", "KO" },
        { "23", "KO" },
        { "24", "KO" },
        { "25", "KO" },
        { "26", "KO" },
        { "27", "OK" },
        { "28", "OK" },
        { "29", "OK" },
        { "2-", "KO" },
    };

    // Pattern = 2[7-9]5
    private static String[][] data12 = {
        { "205", "KO" },
        { "215", "KO" },
        { "225", "KO" },
        { "235", "KO" },
        { "245", "KO" },
        { "255", "KO" },
        { "265", "KO" },
        { "275", "OK" },
        { "285", "OK" },
        { "295", "OK" },
        { "2-5", "KO" },
    };

    // Pattern = 2[-9]
    private static String[][] data13 = {
        { "20", "KO" },
        { "21", "KO" },
        { "22", "KO" },
        { "23", "KO" },
        { "24", "KO" },
        { "25", "KO" },
        { "26", "KO" },
        { "27", "KO" },
        { "28", "KO" },
        { "29", "OK" },
        { "2-", "OK" },
    };

    // Pattern = 2[-9]5
    private static String[][] data14 = {
        { "205", "KO" },
        { "215", "KO" },
        { "225", "KO" },
        { "235", "KO" },
        { "245", "KO" },
        { "255", "KO" },
        { "265", "KO" },
        { "275", "KO" },
        { "285", "KO" },
        { "295", "OK" },
        { "2-5", "OK" },
    };

    // Pattern = 2[9-]
    private static String[][] data15 = {
        { "20", "KO" },
        { "21", "KO" },
        { "22", "KO" },
        { "23", "KO" },
        { "24", "KO" },
        { "25", "KO" },
        { "26", "KO" },
        { "27", "KO" },
        { "28", "KO" },
        { "29", "OK" },
        { "2-", "OK" },
    };

    // Pattern = 2[9-]5
    private static String[][] data16 = {
        { "205", "KO" },
        { "215", "KO" },
        { "225", "KO" },
        { "235", "KO" },
        { "245", "KO" },
        { "255", "KO" },
        { "265", "KO" },
        { "275", "KO" },
        { "285", "KO" },
        { "295", "OK" },
        { "2-5", "OK" },
    };

    // Pattern = 2[-]
    private static String[][] data17 = {
        { "20", "KO" },
        { "21", "KO" },
        { "22", "KO" },
        { "23", "KO" },
        { "24", "KO" },
        { "25", "KO" },
        { "26", "KO" },
        { "27", "KO" },
        { "28", "KO" },
        { "29", "KO" },
        { "2-", "OK" },
    };

    // Pattern = 2[-]5
    private static String[][] data18 = {
        { "205", "KO" },
        { "215", "KO" },
        { "225", "KO" },
        { "235", "KO" },
        { "245", "KO" },
        { "255", "KO" },
        { "265", "KO" },
        { "275", "KO" },
        { "285", "KO" },
        { "295", "KO" },
        { "2-5", "OK" },
    };

    // Pattern = 2[1-36-8]
    private static String[][] data19 = {
        { "20", "KO" },
        { "21", "OK" },
        { "22", "OK" },
        { "23", "OK" },
        { "24", "KO" },
        { "25", "KO" },
        { "26", "OK" },
        { "27", "OK" },
        { "28", "OK" },
        { "29", "KO" },
        { "2-", "KO" },
    };

    // Pattern = 2[1-36-8]5
    private static String[][] data20 = {
        { "205", "KO" },
        { "215", "OK" },
        { "225", "OK" },
        { "235", "OK" },
        { "245", "KO" },
        { "255", "KO" },
        { "265", "OK" },
        { "275", "OK" },
        { "285", "OK" },
        { "295", "KO" },
        { "2-5", "KO" },
    };

    // Pattern = 2[!7-9]
    private static String[][] data21 = {
        { "20", "OK" },
        { "21", "OK" },
        { "22", "OK" },
        { "23", "OK" },
        { "24", "OK" },
        { "25", "OK" },
        { "26", "OK" },
        { "27", "KO" },
        { "28", "KO" },
        { "29", "KO" },
        { "2-", "OK" },
    };

    // Pattern = 2[!7-9]5
    private static String[][] data22 = {
        { "205", "OK" },
        { "215", "OK" },
        { "225", "OK" },
        { "235", "OK" },
        { "245", "OK" },
        { "255", "OK" },
        { "265", "OK" },
        { "275", "KO" },
        { "285", "KO" },
        { "295", "KO" },
        { "2-5", "OK" },
    };

    // Pattern = 2[!-9]
    private static String[][] data23 = {
        { "20", "OK" },
        { "21", "OK" },
        { "22", "OK" },
        { "23", "OK" },
        { "24", "OK" },
        { "25", "OK" },
        { "26", "OK" },
        { "27", "OK" },
        { "28", "OK" },
        { "29", "KO" },
        { "2-", "KO" },
    };

    // Pattern = 2[!-9]5
    private static String[][] data24 = {
        { "205", "OK" },
        { "215", "OK" },
        { "225", "OK" },
        { "235", "OK" },
        { "245", "OK" },
        { "255", "OK" },
        { "265", "OK" },
        { "275", "OK" },
        { "285", "OK" },
        { "295", "KO" },
        { "2-5", "KO" },
    };

    // Pattern = 2[!9-]
    private static String[][] data25 = {
        { "20", "OK" },
        { "21", "OK" },
        { "22", "OK" },
        { "23", "OK" },
        { "24", "OK" },
        { "25", "OK" },
        { "26", "OK" },
        { "27", "OK" },
        { "28", "OK" },
        { "29", "KO" },
        { "2-", "KO" },
    };

    // Pattern = 2[!9-]5
    private static String[][] data26 = {
        { "205", "OK" },
        { "215", "OK" },
        { "225", "OK" },
        { "235", "OK" },
        { "245", "OK" },
        { "255", "OK" },
        { "265", "OK" },
        { "275", "OK" },
        { "285", "OK" },
        { "295", "KO" },
        { "2-5", "KO" },
    };

    // Pattern = 2[!-]
    private static String[][] data27 = {
        { "20", "OK" },
        { "21", "OK" },
        { "22", "OK" },
        { "23", "OK" },
        { "24", "OK" },
        { "25", "OK" },
        { "26", "OK" },
        { "27", "OK" },
        { "28", "OK" },
        { "29", "OK" },
        { "2-", "KO" },
    };

    // Pattern = 2[!-]5
    private static String[][] data28 = {
        { "205", "OK" },
        { "215", "OK" },
        { "225", "OK" },
        { "235", "OK" },
        { "245", "OK" },
        { "255", "OK" },
        { "265", "OK" },
        { "275", "OK" },
        { "285", "OK" },
        { "295", "OK" },
        { "2-5", "KO" },
    };

    // Pattern = 2[!1-36-8]
    private static String[][] data29 = {
        { "20", "OK" },
        { "21", "KO" },
        { "22", "KO" },
        { "23", "KO" },
        { "24", "OK" },
        { "25", "OK" },
        { "26", "KO" },
        { "27", "KO" },
        { "28", "KO" },
        { "29", "OK" },
        { "2-", "OK" },
    };

    // Pattern = 2[!1-36-8]5
    private static String[][] data30 = {
        { "205", "OK" },
        { "215", "KO" },
        { "225", "KO" },
        { "235", "KO" },
        { "245", "OK" },
        { "255", "OK" },
        { "265", "KO" },
        { "275", "KO" },
        { "285", "KO" },
        { "295", "OK" },
        { "2-5", "OK" },
    };

    // Pattern = a*b?c[d-e]
    private static String[][] data31 = {
        { "a*b?cd", "OK" },
        { "a*b?ce", "OK" },
        { "a*b?cde", "KO" },
        { "[a]*b?[c]", "KO" },
        { "abc", "KO" },
        { "ab?c", "KO" },
        { "a*bc", "KO" },
        { "axxbxc", "KO" },
        { "axxbxcd", "OK" },
    };

    // Pattern = a\*b\?c\[d-e]
    private static String[][] data32 = {
        { "a*b?cd", "KO" },
        { "a*b?ce", "KO" },
        { "a*b?cde", "KO" },
        { "[a]*b?[c]", "KO" },
        { "abc", "KO" },
        { "ab?c", "KO" },
        { "a*bc", "KO" },
        { "axxbxc", "KO" },
        { "axxbxcd", "KO" },
        { "a*b?c[d]", "KO" },
        { "a*b?c[e]", "KO" },
        { "a*b?c[d-e]", "OK" },
    };

    // Pattern = a\*b\?c\[de]
    private static String[][] data33 = {
        { "a*b?cd", "KO" },
        { "a*b?ce", "KO" },
        { "a*b?cde", "KO" },
        { "[a]*b?[c]", "KO" },
        { "abc", "KO" },
        { "ab?c", "KO" },
        { "a*bc", "KO" },
        { "axxbxc", "KO" },
        { "axxbxcd", "KO" },
        { "a*b?c[d]", "KO" },
        { "a*b?c[e]", "KO" },
        { "a*b?c[d-e]", "KO" },
        { "a*b?c[de]", "OK" },
    };

    // Pattern = abc[de]f
    private static String[][] data34 = {
        { "abcdf", "OK" },
        { "abcef", "OK" },
        { "abcdef", "KO" },
        { "abcedf", "KO" },
        { "abcd", "KO" },
        { "abce", "KO" },
        { "abcf", "KO" },
    };

    // Pattern = abc[d]e
    private static String[][] data35 = {
        { "abcde", "OK" },
        { "abcd", "KO" },
        { "abcdf", "KO" },
        { "abcdef", "KO" },
    };

    // Pattern = a[b]
    private static String[][] data36 = {
        { "a", "KO" },
        { "ab", "OK" },
        { "a[b]", "KO" },
    };

    // Pattern = a\b
    private static String[][] data37 = {
        { "a", "KO" },
        { "ab", "KO" },
        { "a\\b", "OK" },
    };

    private static Object[][] tests = {
        { "2[7-9]", data11 },
        { "2[7-9]5", data12 },
        { "2[-9]", data13 },
        { "2[-9]5", data14 },
        { "2[9-]", data15 },
        { "2[9-]5", data16 },
        { "2[-]", data17 },
        { "2[-]5", data18 },
        { "2[1-36-8]", data19 },
        { "2[1-36-8]5", data20 },
        { "2[!7-9]", data21 },
        { "2[!7-9]5", data22 },
        { "2[!-9]", data23 },
        { "2[!-9]5", data24 },
        { "2[!9-]", data25 },
        { "2[!9-]5", data26 },
        { "2[!-]", data27 },
        { "2[!-]5", data28 },
        { "2[!1-36-8]", data29 },
        { "2[!1-36-8]5", data30 },
        { "a*b?c[d-e]", data31 },
        { "a\\*b\\?c\\[d-e]", data32 },
        { "a\\*b\\?c\\[de]", data33 },
        { "abc[de]f", data34 },
        { "abc[d]e", data35 },
        { "a[b]", data36 },
        { "a\\\\b", data37 },
    };

    private static int query(MBeanServer mbs,
                             String pattern,
                             String[][] data) throws Exception {

        int error = 0;

        System.out.println("\nAttribute Value Pattern = " + pattern + "\n");
        for (int i = 0; i < data.length; i++) {
            ObjectName on = new ObjectName("domain:type=Simple,pattern=" +
                                           ObjectName.quote(pattern) +
                                           ",name=" + i);
            Simple s = new Simple(data[i][0]);
            mbs.registerMBean(s, on);
            QueryExp q =
                Query.match(Query.attr("StringNumber"), Query.value(pattern));
            q.setMBeanServer(mbs);
            boolean r = q.apply(on);
            System.out.print("Attribute Value = " +
                mbs.getAttribute(on, "StringNumber"));
            if (r && "OK".equals(data[i][1])) {
                System.out.println(" OK");
            } else if (!r && "KO".equals(data[i][1])) {
                System.out.println(" KO");
            } else {
                System.out.println(" Error");
                error++;
            }
        }

        return error;
    }

    public static void main(String[] args) throws Exception {

        int error = 0;

        System.out.println("\n--- Test javax.management.Query.match ---");

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        for (int i = 0; i < tests.length; i++) {
            error += query(mbs, (String) tests[i][0], (String[][]) tests[i][1]);
        }

        if (error > 0) {
            System.out.println("\nTest failed! " + error + " errors.\n");
            throw new IllegalArgumentException("Test failed");
        } else {
            System.out.println("\nTest passed!\n");
        }
    }
}
