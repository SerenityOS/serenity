/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @key headful
  @bug       7125044
  @summary   Tests defaut focus traversal policy in AWT & Swing toplevel windows.
  @author    anton.tarasov@sun.com: area=awt.focus
  @run       main InitialFTP_AWT
  @run       main InitialFTP_Swing
*/

import java.awt.FocusTraversalPolicy;
import java.awt.Window;

public class InitialFTP {
    public static void test(Window win, Class<? extends FocusTraversalPolicy> expectedPolicy) {
        FocusTraversalPolicy ftp = win.getFocusTraversalPolicy();

        System.out.println("==============" + "\n" +
                           "Tested window:    " + win + "\n" +
                           "Expected policy:  " + expectedPolicy + "\n" +
                           "Effective policy: " + ftp.getClass());

        if (!expectedPolicy.equals(ftp.getClass())) {
            throw new RuntimeException("Test failed: wrong effective focus policy");
        }
    }
}
