/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5071110
 * @summary Test whether an null descriptor will cause an NullPointerException.
 * @author Shanliang JIANG
 *
 * @run clean EqualExceptionTest
 * @run build EqualExceptionTest
 * @run main EqualExceptionTest
 */


import java.io.IOException;
import javax.management.*;

public class EqualExceptionTest {
    public static void main(String[] args) throws Exception {
        System.out.println("<<< Test whether an null descriptor will cause an NullPointerException.");

        MBeanInfo mbi1 = new MBeanInfo("MyClass","",null,null,null,null);
        MBeanInfo mbi2 = new MBeanInfo("MyClass",null,null,null,null,null);
        System.out.println("<<< mbi1.equals(mbi2) = "+mbi1.equals(mbi2));

        System.out.println("<<< Test whether an null class name will cause an NullPointerException.");
        MBeanInfo mbi3 = new MBeanInfo("MyClass","",null,null,null,null);
        MBeanInfo mbi4 = new MBeanInfo(null,null,null,null,null,null);
        System.out.println("<<< mbi3.equals(mbi4) = "+mbi3.equals(mbi4));

    }
}
