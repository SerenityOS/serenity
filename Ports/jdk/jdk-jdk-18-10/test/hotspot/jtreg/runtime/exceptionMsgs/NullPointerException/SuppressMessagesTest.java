/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, 2020 SAP SE. All rights reserved.
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

/**
 * @test
 * @summary Test that the default of flag ShowCodeDetailsInExceptionMessages is 'true',
 *          i.e., make sure the VM does print the message by default.
 * @bug 8218628
 * @library /test/lib
 * @compile -g SuppressMessagesTest.java
 * @run main/othervm SuppressMessagesTest printMessage
 */
/**
 * @test
 * @summary Test that the messages are suppressed if flag ShowCodeDetailsInExceptionMessages is 'false'.
 * @bug 8218628
 * @library /test/lib
 * @compile -g SuppressMessagesTest.java
 * @run main/othervm -XX:-ShowCodeDetailsInExceptionMessages SuppressMessagesTest noMessage
 */
/**
 * @test
 * @summary Test that the messages are printed if flag ShowCodeDetailsInExceptionMessages is 'true'.
 * @bug 8218628
 * @library /test/lib
 * @compile -g SuppressMessagesTest.java
 * @run main/othervm -XX:+ShowCodeDetailsInExceptionMessages SuppressMessagesTest printMessage
 */

import jdk.test.lib.Asserts;

class A {
    int aFld;
}

// Tests that the messages are suppressed by flag ShowCodeDetailsInExceptionMessages.
public class SuppressMessagesTest {

    public static void main(String[] args) throws Exception {
        A a = null;

        if (args.length != 1) {
            Asserts.fail("You must specify one arg for this test");
        }

        try {
            @SuppressWarnings("null")
            int val = a.aFld;
            System.out.println(val);
            Asserts.fail();
        } catch (NullPointerException e) {
            System.out.println("Stacktrace of the expected exception:");
            e.printStackTrace(System.out);
            if (args[0].equals("noMessage")) {
                Asserts.assertNull(e.getMessage());
            } else {
                Asserts.assertEquals(e.getMessage(), "Cannot read field \"aFld\" because \"a\" is null");
            }
        }
    }
}
