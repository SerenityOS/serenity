/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6970930
 * @summary verify that compare() throws NPE instead of IAE when an argument is null.
 */
import java.text.*;

public class Bug6970930 {

    private static boolean err = false;

    public static void main(String[] args) {
        // Check if compare() throws NPE.
        test1(null, null);
        test1("\"foo\"", null);
        test1(null, "\"bar\"");

        if (err) {
            throw new RuntimeException("Failed.");
        } else {
            System.out.println("Passed.");
        }
    }

    private static void test1(String s1, String s2) {
        RuleBasedCollator col = null;

        try {
            col = new RuleBasedCollator("< a < b");
        }
        catch (ParseException e) {
            err = true;
            System.err.println(e);
        }

        try {
            col.compare("foo", "bar"); // This line is necessary to reproduce the bug.
            col.compare(s1, s2);

            err = true;
            System.err.println("No exception was thrown for compare(" +
                               s1 + ", " +  s2 + ").");
        }
        catch (NullPointerException e) {
            System.out.println("NPE was thrown as expected for compare(" +
                               s1 + ", " + s2 + ").");
        }
        catch (Exception e) {
            err = true;
            System.err.println("Unexpected exception was thrown for compare(" +
                               s1 + ", " + s2 + "): " + e);
        }
    }

}
