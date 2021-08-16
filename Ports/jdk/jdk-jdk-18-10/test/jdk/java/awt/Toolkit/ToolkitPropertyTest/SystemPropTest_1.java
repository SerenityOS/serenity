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
  @summary verifies that system property sun.awt.enableExtraMouseButtons is true by default
  @author Andrei Dmitriev : area=awt.mouse
  @run main SystemPropTest_1
 */
//1) Verifies that System.getProperty("sun.awt.enableExtraMouseButtons") returns false initially.
//2) Verifies that Toolkit.areExtraMouseButtonsEnabled() returns true by default.
// This must initlizes the Toolkit class.
//3) Verifies that System.getProperty("sun.awt.enableExtraMouseButtons") returns true (default).
import java.awt.*;

public class SystemPropTest_1 {

    public static void main(String []s){
        boolean propValue = Boolean.parseBoolean(System.getProperty("sun.awt.enableExtraMouseButtons"));
        System.out.println("1. System.getProperty = " + propValue);
        if (propValue){
            throw new RuntimeException("TEST FAILED(1) : System property sun.awt.enableExtraMouseButtons = " + propValue);
        }
        if (!Toolkit.getDefaultToolkit().areExtraMouseButtonsEnabled()){
            throw new RuntimeException("TEST FAILED : Toolkit.areExtraMouseButtonsEnabled() returns false");
        }

        System.getProperties().list(System.out);
        System.out.println("XXXX. System.getProperty = " + System.getProperty("sun.awt.enableExtraMouseButtons"));

        propValue = Boolean.parseBoolean(System.getProperty("sun.awt.enableExtraMouseButtons"));
        System.out.println("2. System.getProperty = " + propValue);
        if (!propValue){
            throw new RuntimeException("TEST FAILED(2) : System property sun.awt.enableExtraMouseButtons = " + propValue);
        }
        System.out.println("Test passed.");
    }
}
