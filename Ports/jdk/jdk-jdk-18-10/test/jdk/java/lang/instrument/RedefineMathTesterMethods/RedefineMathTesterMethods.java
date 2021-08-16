/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8225325
 * @summary Add tests for redefining a class' private method during resolution of the bootstrap specifier
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules java.compiler
 *          java.instrument
 * @compile ../NamedBuffer.java
 * @compile redef/Xost.java
 * @run main RedefineClassHelper
 * @run main/othervm -javaagent:redefineagent.jar -Xlog:redefine+class*=trace RedefineMathTesterMethods
 */

interface MathOperation {
    public int operation(int a, int b);
}

class Host {
    static class B {
        public static int operate(int a, int b, MathOperation mathOperation) {
            return mathOperation.operation(a, b);
        }

        static int test_math() {
            MathOperation addition = (int a, int b) -> a + b;
            return operate(10, 5, addition);
        }
    }
}

public class RedefineMathTesterMethods {
    private static byte[] bytesForHostClass(char replace) throws Throwable {
        return NamedBuffer.bytesForHostClass(replace, "Host$B");
    }

    public static void main(String[] args) throws Throwable {
        int res = Host.B.test_math();
        System.out.println("Result = " + res);
        if (res != 15) {
            throw new Error("test_math returned " + res + " expected " + 15);
        }

        byte[] buf = bytesForHostClass('X');
        RedefineClassHelper.redefineClass(Host.B.class, buf);

        res = Host.B.test_math();
        if (res != 5)
            throw new Error("test_math returned " + res + " expected " + 5);
    }
}
