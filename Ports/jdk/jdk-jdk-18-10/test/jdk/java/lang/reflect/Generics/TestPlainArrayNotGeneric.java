/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5041784
 * @summary Check that plain arrays like String[] are never represented as
 * GenericArrayType.
 * @author Eamonn McManus
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestPlainArrayNotGeneric {
    public String[] m1(List<String> p1) {return null;}
    public List<String> m2(String[] p1) {return null;}
    public void m3(List<String> p1, String[] p2) {}
    public void m4(List<String[]> p1) {}
    public TestPlainArrayNotGeneric(List<String[]> p1) {}
    public TestPlainArrayNotGeneric(List<String> p1, String[] p2) {}

    public <T extends List<String[]>> T m5(T p1) {return null;}
    public <T extends Object> T[] m6(T[] p1, List<T[]> p2) {return null;}

    public List<? extends Object[]> m6(List<? extends Object[]> p1) {return null;}
    public <T extends List<? extends Object[]>> T m7(T[] p1) {return null;}
    public List<? super Object[]> m8(List<? super Object[]> p1) {return null;}
    public <T extends List<? super Object[]>> T[] m9(T[] p1) {return null;}

    public static interface XMap extends Map<List<String[]>, String[]> {}
    public static interface YMap<K extends List<String[]>, V>
            extends Map<K[], V[]> {}


    private static String lastFailure;
    private static int failureCount;

    public static void main(String[] args) throws Exception {
        checkClass(TestPlainArrayNotGeneric.class);

        if (failureCount == 0)
            System.out.println("TEST PASSED");
        else
            throw new Exception("TEST FAILED: Last failure: " + lastFailure);
    }

    private static void checkClass(Class<?> c) throws Exception {
        Method[] methods = c.getMethods();
        for (Method m : methods) {
            check(m.getGenericReturnType(), "return type of method " + m);
            check(m.getGenericParameterTypes(), "parameter", "method " + m);
            check(m.getTypeParameters(), "type parameter", "method " + m);
        }

        Constructor[] constructors = c.getConstructors();
        for (Constructor constr : constructors) {
            check(constr.getGenericParameterTypes(), "parameter",
                    "constructor " + constr);
            check(constr.getTypeParameters(), "type parameter",
                    "constructor " + constr);
        }

        Class<?>[] inners = c.getDeclaredClasses();
        for (Class inner : inners)
            checkClass(inner);
    }

    private static void check(Type[] types, String elementKind, String what) {
        for (int i = 0; i < types.length; i++) {
            Type t = types[i];
            check(t, elementKind + " " + (i+1) + " of " + what);
        }
    }

    private static final Set<Type> checking = new HashSet<>();

    private static void check(Type t, String what) {
        if (t == null || !checking.add(t))
            return;
        // Avoid infinite recursion.  t can be null e.g. for superclass of Object.
        try {
            check2(t, what);
        } finally {
            checking.remove(t);
        }
    }

    private static void check2(Type t, String what) {
        if (t instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) t;
            check(pt.getActualTypeArguments(), "type argument", what);
        } else if (t instanceof TypeVariable) {
            TypeVariable<?> tv = (TypeVariable<?>) t;
            check(tv.getBounds(), "bound", what);
            GenericDeclaration gd = tv.getGenericDeclaration();
            if (gd instanceof Type)
                check((Type) gd, "declaration containing " + what);
        } else if (t instanceof WildcardType) {
            WildcardType wt = (WildcardType) t;
            check(wt.getLowerBounds(), "lower bound", "wildcard type in " + what);
            check(wt.getUpperBounds(), "upper bound", "wildcard type in " + what);
        } else if (t instanceof Class<?>) {
            Class<?> c = (Class<?>) t;
            check(c.getGenericInterfaces(), "superinterface", c.toString());
            check(c.getGenericSuperclass(), "superclass of " + c);
            check(c.getTypeParameters(), "type parameter", c.toString());
        } else if (t instanceof GenericArrayType) {
            GenericArrayType gat = (GenericArrayType) t;
            Type comp = gat.getGenericComponentType();
            if (comp instanceof Class) {
                fail("Type " + t + " uses GenericArrayType when plain " +
                        "array would do, in " + what);
            } else
                check(comp, "component type of " + what);
        } else {
            fail("TEST BUG: mutant Type " + t + " (a " + t.getClass().getName() + ")");
        }
    }

    private static void fail(String why) {
        System.out.println("FAIL: " + why);
        lastFailure = why;
        failureCount++;
    }
}
