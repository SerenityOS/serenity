/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5061485
 * @summary Test sematics of ParameterizedType.equals
 * @author Joseph D. Darcy
 */

import java.util.*;
import java.lang.reflect.*;

public class TestParameterizedType {
    public <T> T genericMethod0() {
        return null;
    }

    public <T> Set<T> genericMethod1() {
        return null;
    }

    public <T> Set<T> genericMethod2() {
        return null;
    }

    public <S> List<S> genericMethod3() {
        return null;
    }

    public <X, Y> Map<X, Y> genericMethod4() {
        return null;
    }

    public <T> T[] genericMethod5() {
        return null;
    }

    public <T> T[] genericMethod6() {
        return null;
    }

    public Set<? extends Cloneable> genericMethod7() {
        return null;
    }

    public Set<? super Number> genericMethod8() {
        return null;
    }

    public Set<?> genericMethod9() {
        return null;
    }


    static List<Type> createTypes() throws Exception {
        List<Type> typeList = new ArrayList<Type>(3);
        String[] methodNames = {"genericMethod0",
                                "genericMethod1",
                                "genericMethod2",
                                "genericMethod3",
                                "genericMethod4",
                                "genericMethod5",
                                "genericMethod6",
                                "genericMethod7",
                                "genericMethod8",
                                "genericMethod9",
        };

        for(String s : methodNames) {
            Type t = TestParameterizedType.class.getDeclaredMethod(s).getGenericReturnType();
            // if (! (t instanceof ParameterizedType))
            //  throw new RuntimeException("Unexpected kind of return type");
            typeList.add(t);
        }

        return typeList;
    }

    static boolean testReflexes(List<Type> typeList) {
        for(Type t : typeList) {
            if (! t.equals(t) ) {
                System.err.printf("Bad reflexes for%s %s%n", t, t.getClass());
                return true;
            }
        }
        return false;
    }

    public static void main(String[] argv) throws Exception {
        boolean failed = false;

        List<Type> take1 = createTypes();
        List<Type> take2 = createTypes();

        // Test reflexivity
        failed = failed | testReflexes(take1);
        failed = failed | testReflexes(take2);

        for(int i = 0; i < take1.size(); i++) {
            Type type1 = take1.get(i);

            for(int j = 0; j < take2.size(); j++) {
                Type type2 = take2.get(j);

                if (i == j) {
                    // corresponding types should be .equals
                    if (!type1.equals(type2) ) {
                        failed = true;
                        System.err.printf("Unexpected inequality: [%d, %d] %n\t%s%n\t%s%n",
                                          i, j, type1, type2);
                    }
                } else {
                    // non-corresponding types should *not* be .equals
                    if (type1.equals(type2) ) {
                        failed = true;
                        System.err.printf("Unexpected equality: [%d, %d] %n\t%s%n\t%s%n",
                                          i, j, type1, type2);
                    }
                }

            }
        }

        if (failed)
            throw new RuntimeException("Bad equality on ParameterizedTypes");
    }
}
