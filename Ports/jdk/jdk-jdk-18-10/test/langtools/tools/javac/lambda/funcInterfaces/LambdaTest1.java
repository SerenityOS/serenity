/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 *   This test is for lambda expressions
 * @compile LambdaTest1.java
 * @run main LambdaTest1
 */

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public class LambdaTest1 {
    public static void main(String[] args) {

        LambdaTest1 test = new LambdaTest1();

        test.method2((int n) -> { });
        test.method2((int n) -> { });
        test.method2((int n) -> { return; }); // ";" is mandatory here
        test.method2((int n) -> { System.out.println(n); }); // ";" is optional here
        test.method2(n -> { System.out.println(n); }); //warning, explict type required for n?

        test.method3(()-> { System.out.println("implementing VoidVoid.vvMethod()"); });
        test.method3(() -> {});

        test.method4(()-> 42);
        test.method4(()-> { return 42; });//";" is mandatory here

        test.method5((int n)-> n+1);
        test.method5((int n) -> 42);
        test.method5((int n) -> { return 42; });
        test.method5(
            (int n) -> { //"{" optional here
                if(n > 0)
                    return n++;
                else
                    return n--;
            }
        );

        Runnable r = ()-> { System.out.println("Runnable.run() method implemented"); };
        r.run();
        ((Runnable)()-> { System.out.println("Runnable.run() method implemented"); }).run();
    }

    void method2(VoidInt a) {
        System.out.println("method2()");
        final int N = 1;
        int n = 2; //effectively final variable
        System.out.println("method2() \"this\":" + this);
        ((Runnable)
            ()->{
                System.out.println("inside lambda \"this\":" + this);
                System.out.println("inside lambda accessing final variable N:" + N);
                System.out.println("inside lambda accessing effectively final variable n:" + n);
            }
        ).run();
        //n++; //compile error if n is modified
        a.viMethod(2);
    }

    void method3(VoidVoid a) {
        System.out.println("method3()");
        a.vvMethod();
    }

    void method4(IntVoid a) {
        System.out.println("method4()");
        System.out.println(a.ivMethod());
    }

    void method5(IntInt a) {
        System.out.println("method5()");
        System.out.println(a.iiMethod(5));
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
