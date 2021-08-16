/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8193524
 * @summary Redefine a class' public static method that contains a lambda expression
 * @requires vm.jvmti
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules java.compiler
 *          java.instrument
 *          jdk.jartool/sun.tools.jar
 * @run main RedefineClassHelper
 * @run main/othervm -javaagent:redefineagent.jar -XX:+AllowRedefinitionToAddDeleteMethods -Xlog:redefine+class*=trace RedefineSubtractLambdaExpression
 */

interface MathOperation {
    public int operation(int a, int b);
}

class B {
    public static int operate(int a, int b, MathOperation mathOperation) {
        return mathOperation.operation(a, b);
    }
    static int test_math(String p) {
        System.out.println(p + " from class B's test_math method");
        MathOperation subtraction = (int a, int b) -> a - b;
        MathOperation addition = (int a, int b) -> a + b;
        return operate(10, 5, addition);
    }
}

public class RedefineSubtractLambdaExpression {

    public static String newB =
        "class B {" +
        "    public static int operate(int a, int b, MathOperation mathOperation) {" +
        "        return mathOperation.operation(a, b);" +
        "    }" +
        "    static int test_math(String p) {" +
        "        MathOperation subtraction = (int a, int b) -> a - b;" +
        "        return operate(10, 5, subtraction);" +
        "    }" +
        "}";

    public static void main(String[] args) throws Exception {
        int res = B.test_math("Hello");
        System.out.println("Result = " + res);
        if (res != 15) {
            throw new Error("test_math returned " + res + " expected " + 15);
        }
        RedefineClassHelper.redefineClass(B.class, newB);

        res = B.test_math("Hello");
        if (res != 5)
            throw new Error("test_math returned " + res + " expected " + 5);
    }
}
