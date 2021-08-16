/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8033445
 * @summary regression tests for 8033445, verify default methods call from JNI
 * @run main/native DefaultMethods
 */

interface A {

    default int getOne() {
        return 1;
    }
}

interface B extends A {

}

interface C extends B {

    @Override
    default int getOne() {
        return 2;
    }
}

abstract class Abstract implements C {
}

class Impl extends Abstract {

    @Override
    public int getOne() {
        return 3;
    }
}

class Impl2 extends Impl {

    public static final int expectedValue = 4;

    @Override
    public int getOne() {
        return expectedValue;
    }
}

public class DefaultMethods {

    static {
        System.loadLibrary("DefaultMethods");
    }

    static native int callAndVerify(Impl impl, String className, int expectedResult, int implExpectedResult);

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Impl2 impl2 = new Impl2();
        if (args.length == 0) {
            callAndVerify(impl2, "A", 1, Impl2.expectedValue);
            callAndVerify(impl2, "B", 1, Impl2.expectedValue);
            callAndVerify(impl2, "C", 2, Impl2.expectedValue);
            callAndVerify(impl2, "Abstract", 2, Impl2.expectedValue);
            callAndVerify(impl2, "Impl", 3, Impl2.expectedValue);
            callAndVerify(impl2, "Impl2", 4, Impl2.expectedValue);
        } else {
            verifyAndRun(args, impl2, Impl2.expectedValue);
        }
    }

    //Method to verify input arguments and run a specific test with an expected result provided in the args array
    static void verifyAndRun(String[] args, Impl2 impl, int expectedValue) {
        if (args.length != 2) {
            throw new RuntimeException("invalid number of input arguments");
        }

        String className = args[0];
        int expectedResult = Integer.parseInt(args[1]);

        callAndVerify(impl, className, expectedResult, expectedValue);
    }
}
