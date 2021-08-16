/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8044737
 * @summary Lambda: NPE while obtaining method reference through lambda expression
 * @compile MethodRefNewInnerInLambdaNPE2.java
 */

public class MethodRefNewInnerInLambdaNPE2 {

    interface Constructor {
        MyTest execute();
    }

    class MyTest {
        MyTest() { System.out.println("Constructor executed"); }
    }

    public Constructor getConstructor() {
        return getConstructor(() -> { return MyTest::new; });
    }

    public static void main(String argv[]) {
        MethodRefNewInnerInLambdaNPE2 t = new MethodRefNewInnerInLambdaNPE2();
        MyTest mytest = t.getConstructor().execute();
    }

    Constructor getConstructor(Wrapper arg) {
        return arg.unwrap();
    }

    interface Wrapper {
        Constructor unwrap();
    }
}
