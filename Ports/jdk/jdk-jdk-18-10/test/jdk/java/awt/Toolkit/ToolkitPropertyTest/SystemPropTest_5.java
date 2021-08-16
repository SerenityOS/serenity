/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @test %I% %E%
  @key headful
  @bug 6315717
  @summary verifies that system property sun.awt.enableExtraMouseButtons might be set to false by the System class API.
  @author Andrei Dmitriev : area=awt.mouse
  @run main SystemPropTest_5
 */
//1)
// - Use System.setProperty("sun.awt.enableExtraMouseButtons", "false")
// - Verifies that System.getProperty("sun.awt.enableExtraMouseButtons") returns false
// - Verifies that Toolkit.areExtraMouseButtonsEnabled() returns false.
//2)
// - Use System.setProperty("sun.awt.enableExtraMouseButtons", "true")
// - Verifies that System.getProperty("sun.awt.enableExtraMouseButtons") returns true
// - Verifies that Toolkit.areExtraMouseButtonsEnabled() returns false still.

import java.awt.*;

public class SystemPropTest_5 {
    public static void main(String []s){
        System.out.println("STAGE 1");
        System.setProperty("sun.awt.enableExtraMouseButtons", "false");
        boolean propValue = Boolean.parseBoolean(System.getProperty("sun.awt.enableExtraMouseButtons"));
        if (propValue){
            throw new RuntimeException("TEST FAILED(1) : System property sun.awt.enableExtraMouseButtons = " + propValue);
        }
        if (Toolkit.getDefaultToolkit().areExtraMouseButtonsEnabled()){
            throw new RuntimeException("TEST FAILED(1) : Toolkit.areExtraMouseButtonsEnabled() returns true");
        }

        System.out.println("STAGE 2");
        System.setProperty("sun.awt.enableExtraMouseButtons", "true");
        propValue = Boolean.parseBoolean(System.getProperty("sun.awt.enableExtraMouseButtons"));
        if (!propValue){
            throw new RuntimeException("TEST FAILED(2) : System property sun.awt.enableExtraMouseButtons = " + propValue);
        }
        if (Toolkit.getDefaultToolkit().areExtraMouseButtonsEnabled()){
            throw new RuntimeException("TEST FAILED(2) : Toolkit.areExtraMouseButtonsEnabled() returns true");
        }
        System.out.println("Test passed.");
    }
}
