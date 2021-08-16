/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @summary unit tests for method handles which permute their arguments
 * @run testng test.java.lang.invoke.ThrowExceptionsTest
 */

package test.java.lang.invoke;

import org.testng.*;
import org.testng.annotations.*;

import java.util.*;
import java.lang.reflect.*;

import java.lang.invoke.*;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;

public class ThrowExceptionsTest {
    private static final Class<?> CLASS = ThrowExceptionsTest.class;
    private static final Lookup LOOKUP = lookup();

    public static void main(String argv[]) throws Throwable {
        new ThrowExceptionsTest().testAll((argv.length == 0 ? null : Arrays.asList(argv).toString()));
    }

    @Test
    public void testWMT() throws Throwable {
        // mostly call testWMTCallee, but sometimes call its void-returning variant
        MethodHandle mh = testWMTCallee();
        MethodHandle mh1 = mh.asType(mh.type().changeReturnType(void.class));
        assert(mh1 != mh);
        testWMT(mh, mh1, 1000);
    }

    @Test
    public void testBoundWMT() throws Throwable {
        // mostly call exactInvoker.bindTo(testWMTCallee), but sometimes call its void-returning variant
        MethodHandle callee = testWMTCallee();
        MethodHandle callee1 = callee.asType(callee.type().changeReturnType(void.class));
        MethodHandle invoker = exactInvoker(callee.type());
        MethodHandle mh  = invoker.bindTo(callee);
        MethodHandle mh1 = invoker.bindTo(callee1);
        testWMT(mh, mh1, 1000);
    }

    @Test
    public void testFoldWMT() throws Throwable {
        // mostly call exactInvoker.fold(constant(testWMTCallee)), but sometimes call its void-returning variant
        MethodHandle callee = testWMTCallee();
        MethodHandle callee1 = callee.asType(callee.type().changeReturnType(void.class));
        MethodHandle invoker = exactInvoker(callee.type());
        MethodHandle mh  = foldArguments(invoker, constant(MethodHandle.class, callee));
        MethodHandle mh1 = foldArguments(invoker, constant(MethodHandle.class, callee1));
        testWMT(mh, mh1, 1000);
    }

    @Test
    public void testFoldCCE() throws Throwable {
        MethodHandle callee = testWMTCallee();
        MethodHandle callee1 = callee.asType(callee.type().changeParameterType(1, Number.class)).asType(callee.type());
        MethodHandle invoker = exactInvoker(callee.type());
        MethodHandle mh  = foldArguments(invoker, constant(MethodHandle.class, callee));
        MethodHandle mh1 = foldArguments(invoker, constant(MethodHandle.class, callee1));
        testWMT(mh, mh1, 1000);
    }

    @Test
    public void testStackOverflow() throws Throwable {
        MethodHandle callee = testWMTCallee();
        MethodHandle callee1 = makeStackOverflow().asType(callee.type());
        MethodHandle invoker = exactInvoker(callee.type());
        MethodHandle mh  = foldArguments(invoker, constant(MethodHandle.class, callee));
        MethodHandle mh1 = foldArguments(invoker, constant(MethodHandle.class, callee1));
        for (int i = 0; i < REPEAT; i++) {
            try {
                testWMT(mh, mh1, 1000);
            } catch (StackOverflowError ex) {
                // OK, try again
            }
        }
    }

    private static MethodHandle makeStackOverflow() {
        MethodType cellType = methodType(void.class);
        MethodHandle[] cell = { null };  // recursion point
        MethodHandle getCell = insertArguments(arrayElementGetter(cell.getClass()), 0, cell, 0);
        MethodHandle invokeCell = foldArguments(exactInvoker(cellType), getCell);
        assert(invokeCell.type() == cellType);
        cell[0] = invokeCell;
        // make it conformable to any type:
        invokeCell = dropArguments(invokeCell, 0, Object[].class).asVarargsCollector(Object[].class);
        return invokeCell;
    }

    static int testCases;

    private void testAll(String match) throws Throwable {
        testCases = 0;
        Lookup lookup = lookup();
        for (Method m : CLASS.getDeclaredMethods()) {
            String name = m.getName();
            if (name.startsWith("test") &&
                (match == null || match.contains(name.substring("test".length()))) &&
                m.getParameterTypes().length == 0 &&
                Modifier.isPublic(m.getModifiers()) &&
                !Modifier.isStatic(m.getModifiers())) {
                System.out.println("["+name+"]");
                int tc = testCases;
                try {
                    m.invoke(this);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    System.out.println("*** "+ex);
                    ex.printStackTrace(System.out);
                }
                if (testCases == tc)  testCases++;
            }
        }
        if (testCases == 0)  throw new RuntimeException("no test cases found");
        System.out.println("ran a total of "+testCases+" test cases");
    }

    private static MethodHandle findStatic(String name) {
        return findMethod(name, true);
    }
    private static MethodHandle findVirtual(String name) {
        return findMethod(name, false);
    }
    private static MethodHandle findMethod(String name, boolean isStatic) {
        MethodHandle mh = null;
        for (Method m : CLASS.getDeclaredMethods()) {
            if (m.getName().equals(name) &&
                Modifier.isStatic(m.getModifiers()) == isStatic) {
                if (mh != null)
                    throw new RuntimeException("duplicate methods: "+name);
                try {
                    mh = LOOKUP.unreflect(m);
                } catch (ReflectiveOperationException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        if (mh == null)
            throw new RuntimeException("no method: "+name);
        return mh;
    }

    int testWMTCallee;
    private int testWMTCallee(String x) {
        return testWMTCallee++;
    }
    private static MethodHandle testWMTCallee() {
        MethodHandle callee = findVirtual("testWMTCallee");
        // FIXME: should not have to retype callee
        callee = callee.asType(callee.type().changeParameterType(0, Object.class));
        return callee;
    }

    private Exception testWMT(MethodHandle[] mhs, int reps) throws Throwable {
        testCases += 1;
        testWMTCallee = 0;
        int catches = 0;
        Exception savedEx = null;
        for (int i = 0; i < reps; i++) {
            MethodHandle mh = mhs[i % mhs.length];
            int n;
            try {
                // FIXME: should not have to retype this
                n = (int) mh.invokeExact((Object)this, "x");
                assertEquals(n, i - catches);
                // Using the exact type for this causes endless deopt due to
                // 'non_cached_result' in SystemDictionary::find_method_handle_invoke.
                // The problem is that the compiler thread needs to access a cached
                // invoke method, but invoke methods are not cached if one of the
                // component types is not on the BCP.
            } catch (Exception ex) {
                savedEx = ex;
                catches++;
            }
        }
        //VERBOSE: System.out.println("reps="+reps+" catches="+catches);
        return savedEx;
    }

    private static final int REPEAT = Integer.getInteger(CLASS.getSimpleName()+".REPEAT", 10);

    private Exception testWMT(MethodHandle mh, MethodHandle mh1, int reps) throws Throwable {
        //VERBOSE: System.out.println("mh="+mh+" mh1="+mh1);
        MethodHandle[] mhs = new MethodHandle[100];
        Arrays.fill(mhs, mh);
        int patch = mhs.length-1;
        Exception savedEx = null;
        for (int i = 0; i < REPEAT; i++) {
            mhs[patch] = mh;
            testWMT(mhs, 10000);
            mhs[patch] = mh1;
            savedEx = testWMT(mhs, reps);
        }
        return savedEx;
    }

    private static void assertEquals(Object x, Object y) {
        if (x == y || x != null && x.equals(y))  return;
        throw new RuntimeException(x+" != "+y);
    }
}
