/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8138667
 * @summary Verify that javac emits suitable accessors when a method reference mentions a protected method that would need an accessor
 * @run main ProtectedInaccessibleMethodRefTest
 */


import pack.SuperClass;

import java.util.concurrent.Callable;

public final class ProtectedInaccessibleMethodRefTest extends SuperClass {

    static String message = "NOT OK";

    public void doTest() throws Exception {
        new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                final Runnable r = ProtectedInaccessibleMethodRefTest.this::myDo;
                r.run();
                return null;
            }
        }.call();

        new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                final Runnable r = ProtectedInaccessibleMethodRefTest::myStaticDo;
                r.run();
                return null;
            }
        }.call();
    }

    public void message(String s) {
        message = s;
    }

    public static void main(String[] args) throws Exception {
        new ProtectedInaccessibleMethodRefTest().doTest();
        if (!message.equals("OK!"))
            throw new AssertionError("Unexpected output");
        if (!sMessage.equals("OK!"))
            throw new AssertionError("Unexpected output");
    }
}
