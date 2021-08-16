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
  @summary verifies that system property sun.awt.enableExtraMouseButtons might be set to false by the command line
  @author Andrei Dmitriev : area=awt.mouse
  @run main/othervm -Dsun.awt.enableExtraMouseButtons=false SystemPropTest_3
 */
//1) Verifies that System.getProperty("sun.awt.enableExtraMouseButtons") returns false if set through the command line.
//2) Verifies that Toolkit.areExtraMouseButtonsEnabled() returns false if the proprty is set through the command line.
import java.awt.*;

public class SystemPropTest_3 {

    public static void main(String []s){
        boolean propValue = Boolean.parseBoolean(System.getProperty("sun.awt.enableExtraMouseButtons"));
        System.out.println("Test System.getProperty = " + System.getProperty("sun.awt.enableExtraMouseButtons"));
        System.out.println("System.getProperty = " + propValue);
        if (propValue){
            throw new RuntimeException("TEST FAILED : System property sun.awt.enableExtraMouseButtons = " + propValue);
        }
        if (Toolkit.getDefaultToolkit().areExtraMouseButtonsEnabled()){
            throw new RuntimeException("TEST FAILED : Toolkit.areExtraMouseButtonsEnabled() returns true");
        }
        System.out.println("Test passed.");
    }
}
