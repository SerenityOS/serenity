/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.api.recording.options;

import jdk.jfr.Recording;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.CommonHelper;
import jdk.test.lib.jfr.VoidFunction;

/**
 * @test
 * @summary Test setName().
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.options.TestName
 */
public class TestName {

    public static void main(String[] args) throws Throwable {
        Recording r = new Recording();

        Asserts.assertNotNull(r.getName(), "Default name should not be null");
        System.out.println("name=" + r.getName());
        Asserts.assertEquals(r.getName(), Long.toString(r.getId()), "Default name != id");

        testNames(r);
        r.start();
        testNames(r);
        r.stop();
        testNames(r);
        r.close();
    }

    private static void testNames(Recording r) throws Throwable {
        System.out.println("Recording state=" + r.getState().name());

        // Set simple name
        String name = "myName";
        r.setName(name);
        System.out.println("name=" + r.getName());
        Asserts.assertEquals(name, r.getName(), "Wrong get/set name");

        // Set null. Should get Exception and old name should be kept.
        verifyNull(()->{r.setName(null);}, "No NullPointerException when setName(null)");
        Asserts.assertEquals(name, r.getName(), "Current name overwritten after null");

        // Set empty name. Should work.
        name = "";
        r.setName(name);
        System.out.println("name=" + r.getName());
        Asserts.assertEquals(name, r.getName(), "Empty name is expected to work");

        // Test a long name.
        StringBuilder sb = new StringBuilder(500);
        while (sb.length() < 400) {
            sb.append("LongName-");
        }
        name = sb.toString();
        System.out.println("Length of long name=" + name.length());
        r.setName(name);
        System.out.println("name=" + r.getName());
        Asserts.assertEquals(name, r.getName(), "Wrong get/set long name");
    }

    private static void verifyNull(VoidFunction f, String msg) throws Throwable {
        CommonHelper.verifyException(f, msg, NullPointerException.class);
    }

}
