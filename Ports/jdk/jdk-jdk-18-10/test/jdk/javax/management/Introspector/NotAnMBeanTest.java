/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4914805
 * @summary Ensure that the right exception is thrown for illegal MBeans
 * @author Eamonn McManus
 *
 * @run clean NotAnMBeanTest
 * @run build NotAnMBeanTest
 * @run main NotAnMBeanTest
 */

import javax.management.*;

public class NotAnMBeanTest {
    public static void main(String[] args) throws Exception {
        System.out.println("Try to create a java.lang.Integer as an MBean");
        MBeanServer mbs = MBeanServerFactory.createMBeanServer();
        try {
            mbs.createMBean("java.lang.Integer", null);
            System.out.println("TEST FAILS: createMBean of " +
                               "java.lang.Integer succeeded!");
            System.exit(1);
        } catch (NotCompliantMBeanException e) {
            System.out.println("Got expected exception: " + e);
            System.out.println("Test passed");
        }
    }
}
