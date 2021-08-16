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

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.Serializable;
import java.io.File;

/**
 * @test
 * @bug 8003280
 * @summary Add lambda tests
 *   Parameter types inferred from target type in generics without wildcard
 * @compile InferenceTest2.java
 * @run main InferenceTest2
 */

public class InferenceTest2 {

    private static void assertTrue(boolean cond) {
        if (!cond)
            throw new AssertionError();
    }

    public static void main(String[] args) {

        InferenceTest2 test = new InferenceTest2();

        //test SAM1<T>
        SAM1<String> sam1 = para -> { String result = "";
                                      for(String s : para)
                                          if(s.compareTo(result) > 0)
                                              result = s;
                                      return result; };
        List<String> list = Arrays.asList("a", "b", "c");
        assertTrue(sam1.m1(list).equals("c"));

        test.method1(para -> para.get(0));

        //test SAM2<T>
        SAM2<String> sam2 = para -> {para = para.substring(0);};
        SAM2<Double> sam2_2 = para -> {};
        SAM2<File> sam2_3 = para -> { if(para.isDirectory())
                                          System.out.println("directory");
                                    };

        //test SAM3<T>
        SAM3<String> sam3 = para -> para[0].substring(0, para[0].length()-1);
        assertTrue(sam3.m3("hello+").equals("hello"));

        SAM3<Integer> sam3_2 = para -> para[0] - para[1];
        assertTrue(sam3_2.m3(1, -1) == 2);

        SAM3<Double> sam3_3 = para -> para[0] + para[1] + para[2] + para[3];
        assertTrue(sam3_3.m3(1.0, 2.0, 3.0, 4.0) == 10.0);

        test.method3(para -> para[0] + 1);

        //test SAM6<T>
        SAM6<String> sam6 = (para1, para2) -> para1.concat(para2);
        assertTrue(sam6.m6("hello", "world").equals("helloworld"));

        test.method6((para1, para2) -> para1 >= para2? para1 : para2);
    }

    void method1(SAM1<Integer> sam1) {
        List<Integer> list = Arrays.asList(3,2,1);
        assertTrue(sam1.m1(list) == 3);
    }

    void method3(SAM3<Double> sam3) {
        assertTrue(sam3.m3(2.5) == 3.5);
    }

    void method6(SAM6<Long> sam6) {
        assertTrue(sam6.m6(5L, -5L) == 5);
    }

    interface SAM1<T> {
        T m1(List<T> x);
    }

    interface SAM2<T extends Serializable> {
        void m2(T x);
    }

    interface SAM3<T> {
        T m3(T... x);
    }

    interface SAM6<T> {
        T m6(T a, T b);
    }
}
