/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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


// Simple framework to run all the tests in sequence
// Because all the tests are marked @ignore as they require special hardware,
// we cannot use jtreg to do this.

import java.lang.reflect.Method;

public class TestAll {

    private static final Class[] CLASSES = {
        TestDefault.class,
        TestChannel.class,
        TestConnect.class,
        TestConnectAgain.class,
        TestControl.class,
        TestExclusive.class,
        TestMultiplePresent.class,
        TestPresent.class,
        TestTransmit.class,
        TestDirect.class,
    };

    public static void main(String[] args) throws Exception {
        Utils.setLibrary(args);
        for (Class clazz : CLASSES) {
            run(clazz, args);
        }
    }

    private static void run(Class clazz, Object args) throws Exception {
        System.out.println("===== Running test " + clazz.getName() + " =====");
        Method method = clazz.getMethod("main", String[].class);
        method.invoke(null, args);
        System.out.println("===== Passed  test " + clazz.getName() + " =====");
    }

}
