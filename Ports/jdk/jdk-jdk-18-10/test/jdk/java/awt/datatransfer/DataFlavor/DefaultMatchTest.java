/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @bug 4250750
  @summary tests that DataFlavor.match() does not throw NPE.
  @author prs@sparc.spb.su: area=
  @modules java.datatransfer
  @run main DefaultMatchTest
*/


import java.awt.datatransfer.DataFlavor;

public class DefaultMatchTest {

    static DataFlavor df1, df2, df3;

    public static void main(String[] args) throws Exception {
        boolean passed = true;
        try {
            df1 = new DataFlavor("application/postscript");
            df2 = new DataFlavor();
            df3 = new DataFlavor();
        } catch (ClassNotFoundException e1) {
            throw new RuntimeException("Could not create DataFlavors. This should never happen.");
        } catch (IllegalArgumentException e2) {
            passed = false;
        }
        try {
            boolean b;
            b = df1.match(df2);
            b = df2.match(df1);
            b = df2.match(df3);
        } catch (NullPointerException e) {
            throw new RuntimeException("The test FAILED: DataFlavor.match still throws NPE");
        }
        if (!passed) {
            throw new RuntimeException("Test FAILED");
        }
        System.out.println("Test PASSED");
    }
}

