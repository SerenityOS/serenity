/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
//package nsk.jvmti.RedefineClasses.StressRedefine;
package nsk.jvmti.RedefineClasses;

import java.util.Random;


public class GenerateSourceHelper {

    static final String STATIC_METHOD_NAME = "staticMethod";
    static final String NONSTATIC_METHOD_NAME = "regularMethod";
    static final String CLASS_NAME = "MyClass";

    private static Random random;

    public static void setRandom(Random random) {
        GenerateSourceHelper.random = random;
    }

    /**
     * We can vary only constant pool and method bodies from call to call
     *
     */
    static CharSequence generateSource() {
        return "public class " + CLASS_NAME + " { " +
                        "public static String s1 = \"s1s" + random.nextInt() + "dfsdf\"; " +
                                        "public int i = 1345345345; \n" +
                                        "public static double dd = 1e-4; \n" +
                                        "public String s2 = \"s2" + random.nextInt() + "sdfsdf\"; \n" +
                                        "public static String static_s2 = \"s2" + random.nextInt() + "sdfsdf\"; \n" +
                                        "protected String sprotected1 = \"asdfsdf" + random.nextInt() + "sdf\"; \n" +
                                        "protected double d = -.12345; \n" +
                                        "public String methodJustPadding() {return s1 + s2 + d; } \n" +
                                        "public static String " + STATIC_METHOD_NAME + "(double d, int i, Object o) {\n" +
                                                        "String ret_0 = \"little computation \" + (4 * dd  + d + i + o.hashCode() + s1.length()); \n" +
                                                        "String ret_1 = \"in_static_method call_random=" + random.nextInt() + "\"; \n" +
                                                        "String ret =  s1 + static_s2 + d + i + o; \n" +
                                                        //"System.out.println(\"ret stat is : \" + ret);" +
                                                        "return ret; " +
                                        "} \n" +
                                        "public String methodInTheMiddle() {return s1 + s2; } \n" +
                                        "public String " + NONSTATIC_METHOD_NAME + "(double d, int i, Object o) {\n" +
                                                        "String ret_0 = \"little computation \" + (2 * dd + 5 * d  + i + o.hashCode() + s2.length()); \n" +
                                                        "String ret_1 = \"in_nonstatic_method call_random=" + random.nextInt() + "\"; " +
                                                        "String ret = ret_0 + ret_1 +  s1 + s2 + i + o; \n" +
                                                        //"System.out.println(\"ret nonstat is : \" + ret);" +
                                                        "return ret;" +
                                        "} \n" +
                                        "public String methodFinalInClass() {return s1 + s2 + i; } \n" +
                        "}";
    }


}
