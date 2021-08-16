/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8003280
 * @summary Add lambda tests
 *   Test lambda expressions for different method signatures (parameter and return type)
 * @compile LambdaTest2.java
 * @run main LambdaTest2
 */

public class LambdaTest2 {

    private static int count = 0;

    private static void assertTrue(boolean b) {
        if(!b)
            throw new AssertionError();
    }

    public static void main(String[] args) {
        LambdaTest2 test = new LambdaTest2();

        test.method2((int n) -> { ; });
        test.method2(n -> { }); // "int" is optional here
        test.method2((int n) -> { }); // ";" is optional here
        test.method2((int n) -> { return; }); // ";" is mandatory here
        test.method2((int n) -> { count += n; });
        assertTrue(count == 10);

        VoidInt vi = (int i) -> {
                            switch (i) {
                                case 0:
                                    System.out.println("normal");
                                    break;
                                default:
                                    System.out.println("invalid");
                            }
                       };

        test.method3(()-> { count++; });
        test.method3(() -> {});
        assertTrue(count == 11);

        VoidVoid vv = ()-> { while(true)
                            if(false)
                                break;
                            else
                                continue;
                       };

        IntVoid iv1 = () -> 42;
        IntVoid iv2 = () -> { return 43; };//";" is mandatory here
        assertTrue(iv1.ivMethod() == 42);
        assertTrue(iv2.ivMethod() == 43);

        IntInt ii1 = (int n) -> n+1;
        IntInt ii2 = n -> 42;
        IntInt ii3 = n -> { return 43; };
        IntInt ii4 =
            (int n) -> {
                if(n > 0)
                    return n+1;
                else
                    return n-1;
            };
        assertTrue(ii1.iiMethod(1) == 2);
        assertTrue(ii2.iiMethod(1) == 42);
        assertTrue(ii3.iiMethod(1) == 43);
        assertTrue(ii4.iiMethod(-1) == -2);
    }

    void method2(VoidInt a) {
        a.viMethod(10);
    }

    void method3(VoidVoid a) {
        a.vvMethod();
    }

    //SAM type interfaces
    interface VoidInt {
        void viMethod(int n);
    }

    interface VoidVoid {
        void vvMethod();
    }

    interface IntVoid {
        int ivMethod();
    }

    interface IntInt {
        int iiMethod(int n);
    }
}
