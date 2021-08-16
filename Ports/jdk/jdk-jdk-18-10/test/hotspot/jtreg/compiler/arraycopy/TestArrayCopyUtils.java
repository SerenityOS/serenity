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

package compiler.arraycopy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

abstract class TestArrayCopyUtils {
    public enum ArraySrc {
        SMALL,
        LARGE,
        ZERO
    }

    public enum ArrayDst {
        NONE,
        NEW,
        SRC
    }

    static class A {
    }

    static class B extends A {
    }

    static final A[] small_a_src = new A[5];
    static final A[] large_a_src = new A[10];
    static final A[] zero_a_src = new A[0];
    static final int[] small_int_src = new int[5];
    static final int[] large_int_src = new int[10];
    static final int[] zero_int_src = new int[0];
    static final Object[] small_object_src = new Object[5];
    static Object src;

    @Retention(RetentionPolicy.RUNTIME)
    @interface Args {
        ArraySrc src();
        ArrayDst dst() default ArrayDst.NONE;
        int[] extra_args() default {};
    }

    final HashMap<String,Method> tests = new HashMap<>();
    {
        for (Method m : this.getClass().getDeclaredMethods()) {
            if (m.getName().matches("m[0-9]+(_check)?")) {
                assert(Modifier.isStatic(m.getModifiers())) : m;
                tests.put(m.getName(), m);
            }
        }
    }

    boolean success = true;

    void doTest(String name) throws Exception {
        Method m = tests.get(name);
        Method m_check = tests.get(name + "_check");
        Class[] paramTypes = m.getParameterTypes();
        Object[] params = new Object[paramTypes.length];
        Class retType = m.getReturnType();
        boolean isIntArray = (retType.isPrimitive() && !retType.equals(Void.TYPE)) ||
            (retType.equals(Void.TYPE) && paramTypes[0].getComponentType().isPrimitive()) ||
            (retType.isArray() && retType.getComponentType().isPrimitive());

        Args args = m.getAnnotation(Args.class);

        Object src = null;
        switch(args.src()) {
        case SMALL: {
            if (isIntArray) {
                src = small_int_src;
            } else {
                src = small_a_src;
            }
            break;
        }
        case LARGE: {
            if (isIntArray) {
                src = large_int_src;
            } else {
                src = large_a_src;
            }
            break;
        }
        case ZERO: {
            if (isIntArray) {
                src = zero_int_src;
            } else {
                src = zero_a_src;
            }
            break;
        }
        }

        for (int i = 0; i < 20000; i++) {
            boolean failure = false;

            int p = 0;

            if (params.length > 0) {
                if (isIntArray) {
                    params[0] = ((int[])src).clone();
                } else {
                    params[0] = ((A[])src).clone();
                }
                p++;
            }

            if (params.length > 1) {
                switch(args.dst()) {
                case NEW: {
                    if (isIntArray) {
                        params[1] = new int[((int[])params[0]).length];
                    } else {
                        params[1] = new A[((A[])params[0]).length];
                    }
                    p++;
                    break;
                }
                case SRC: {
                    params[1] = params[0];
                    p++;
                    break;
                }
                case NONE: break;
                }
            }

            for (int j = 0; j < args.extra_args().length; j++) {
                params[p+j] = args.extra_args()[j];
            }

            Object res = m.invoke(null, params);

            if (retType.isPrimitive() && !retType.equals(Void.TYPE)) {
                int s = (int)res;
                int sum = 0;
                int[] int_res = (int[])src;
                for (int j = 0; j < int_res.length; j++) {
                    sum += int_res[j];
                }
                failure = (s != sum);
                if (failure) {
                    System.out.println("Test " + name + " failed: result = " + s + " != " + sum);
                }
            } else {
                Object dest = null;
                if (!retType.equals(Void.TYPE)) {
                    dest = res;
                } else {
                    dest = params[1];
                }

                if (m_check != null) {
                    failure = (boolean)m_check.invoke(null,  new Object[] { src, dest });
                } else {
                    if (isIntArray) {
                        int[] int_res = (int[])src;
                        int[] int_dest = (int[])dest;
                        for (int j = 0; j < int_res.length; j++) {
                            if (int_res[j] != int_dest[j]) {
                                System.out.println("Test " + name + " failed for " + j + " src[" + j +"]=" + int_res[j] + ", dest[" + j + "]=" + int_dest[j]);
                                failure = true;
                            }
                        }
                    } else {
                        Object[] object_res = (Object[])src;
                        Object[] object_dest = (Object[])dest;
                        for (int j = 0; j < object_res.length; j++) {
                            if (object_res[j] != object_dest[j]) {
                                System.out.println("Test " + name + " failed for " + j + " src[" + j +"]=" + object_res[j] + ", dest[" + j + "]=" + object_dest[j]);
                                failure = true;
                            }
                        }
                    }
                }
            }

            if (failure) {
                success = false;
                break;
            }
        }
    }

    TestArrayCopyUtils() {
        for (int i = 0; i < small_a_src.length; i++) {
            small_a_src[i] = new A();
        }

        for (int i = 0; i < small_int_src.length; i++) {
            small_int_src[i] = i;
        }

        for (int i = 0; i < large_int_src.length; i++) {
            large_int_src[i] = i;
        }

        for (int i = 0; i < 5; i++) {
            small_object_src[i] = new Object();
        }
    }
}
