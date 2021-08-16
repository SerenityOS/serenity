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
 * @bug 6228749
 * @summary MBeanPermission(null,"") should throw IllegalArgumentException.
 * @author Luis-Miguel Alventosa
 *
 * @run clean MBeanPermissionTest
 * @run build MBeanPermissionTest
 * @run main MBeanPermissionTest
 */

import javax.management.MBeanPermission;

public class MBeanPermissionTest {
    public static void main(String[] args) {
        int error = 0;
        System.out.println(">>> MBeanPermissionTest");
        try {
            System.out.println("Create MBeanPermission(null,\"\")");
            MBeanPermission mbp = new MBeanPermission(null, "");
            System.out.println("Didn't get expected IllegalArgumentException");
            error++;
        } catch (IllegalArgumentException e) {
            System.out.println("Got expected exception = " + e);
        } catch (Exception e) {
            System.out.println("Got unexpected exception = " + e);
            error++;
        }
        try {
            System.out.println("Create MBeanPermission(\"\", null)");
            MBeanPermission mbp = new MBeanPermission("", null);
            System.out.println("Didn't get expected IllegalArgumentException");
            error++;
        } catch (IllegalArgumentException e) {
            System.out.println("Got expected exception = " + e);
        } catch (Exception e) {
            System.out.println("Got unexpected exception = " + e);
            error++;
        }
        if (error > 0) {
            final String msg = "Test FAILED! Got " + error + " error(s)";
            System.out.println(msg);
            throw new IllegalArgumentException(msg);
        } else {
            System.out.println("Test PASSED!");
        }
    }
}
