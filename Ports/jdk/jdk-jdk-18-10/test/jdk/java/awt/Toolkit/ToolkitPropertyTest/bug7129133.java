/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7129133
 * @summary [macosx] Accelerators are displayed as Meta instead of the Command symbol
 * @author leonid.romanov@oracle.com
 * @library /test/lib
 * @build jdk.test.lib.Platform
 * @run main bug7129133
 */

import jdk.test.lib.Platform;

import java.awt.*;

public class bug7129133 {
    public static void main(String[] args) throws Exception {
        if (!Platform.isOSX()) {
            System.out.println("This test is for MacOS only. Automatically passed on other platforms.");
            return;
        }

        Toolkit.getDefaultToolkit();

        String cmdSymbol = "\u2318";
        String val = Toolkit.getProperty("AWT.meta", "Meta");

        if (!val.equals(cmdSymbol)) {
           throw new Exception("Wrong property value for AWT.meta. Expected: " + cmdSymbol + ", actual: " + val);
        }
    }
}
