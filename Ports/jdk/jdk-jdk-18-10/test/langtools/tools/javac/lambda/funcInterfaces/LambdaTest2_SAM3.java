/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
 *   This test is for identifying SAM types #5 and instantiating non-SAM types #7 through inner class,
             see Helper.java for SAM types
 * @modules java.sql
 * @compile LambdaTest2_SAM3.java Helper.java
 * @run main LambdaTest2_SAM3
 */

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

public class LambdaTest2_SAM3 {
    private static List<String> strs = new ArrayList<String>();
    private static List<Integer> integers = new ArrayList<Integer>();

    public static void main(String[] args) {
        LambdaTest2_SAM3 test = new LambdaTest2_SAM3();

        //type #7, Not SAM-convertible, through inner class only:
        test.methodFooBar(new FooBar() {
                public int getAge(Number n) {
                    System.out.println("getAge(Number n) called");
                    return 100;
                }
                public int getAge(Integer i) {
                    System.out.println("getAge(Integer i) called");
                    return 200;
                }
            }
        );

        //type #7:
        test.methodDE(new DE(){
                public int getOldest(List<Integer > list) {
                    System.out.println("getOldest(List<Integer> list) called");
                    return 100;
                }
                public int getOldest(Collection<?> collection) {
                    System.out.println("getOldest(Collection<?> collection) called");
                    return 200;
                }
            }
        );

    }

    //type #7: Not SAM type
    void methodFooBar(FooBar fb) {
        System.out.println("methodFooBar(): interface FooBar object instantiated: " + fb);
        System.out.println("result=" + fb.getAge(new Byte("10")));
        System.out.println("result=" + fb.getAge(new Integer(10)));
    }

    //type #7: Not SAM type
    void methodDE (DE de) {
        System.out.println("methodDE(): interface DE object instantiated: " + de);
        System.out.println(de.getOldest(integers));
        System.out.println(de.getOldest(strs));
    }
}
