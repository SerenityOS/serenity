/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package org.openjdk.tests.separate;

import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.openjdk.tests.separate.SourceModel.Class;
import static org.openjdk.tests.separate.SourceModel.*;
import static org.testng.Assert.*;

public class TestHarness {

    /**
     * Creates a per-thread persistent compiler object to allow as much
     * sharing as possible, but still allows for parallel execution of tests.
     */
    protected ThreadLocal<Compiler> compilerLocal = new ThreadLocal<Compiler>(){
         protected synchronized Compiler initialValue() {
             return new Compiler();
         }
    };

    protected ThreadLocal<Boolean> verboseLocal = new ThreadLocal<Boolean>() {
         protected synchronized Boolean initialValue() {
             return Boolean.FALSE;
         }
    };

    protected boolean verbose;
    protected boolean canUseCompilerCache;
    public static final String stdMethodName = SourceModel.stdMethodName;

    private TestHarness() {
    }

    protected TestHarness(boolean verbose, boolean canUseCompilerCache) {
        this.verbose = verbose;
        this.canUseCompilerCache = canUseCompilerCache;
    }

    public void setTestVerbose() {
        verboseLocal.set(Boolean.TRUE);
    }

    @AfterMethod
    public void reset() {
        if (!this.verbose) {
            verboseLocal.set(Boolean.FALSE);
        }
    }

    public Compiler.Flags[] compilerFlags() {
        HashSet<Compiler.Flags> flags = new HashSet<>();
        if (verboseLocal.get() == Boolean.TRUE) {
            flags.add(Compiler.Flags.VERBOSE);
        }
        if (this.canUseCompilerCache) {
            flags.add(Compiler.Flags.USECACHE);
        }
        return flags.toArray(new Compiler.Flags[0]);
    }

    @AfterMethod
    public void printError(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE) {
            String clsName = result.getTestClass().getName();
            clsName = clsName.substring(clsName.lastIndexOf(".") + 1);
            System.out.println("Test " + clsName + "." +
                               result.getName() + " FAILED");
        }
    }

    private static final ConcreteMethod stdCM = ConcreteMethod.std("-1");
    private static final AbstractMethod stdAM =
            new AbstractMethod("int", stdMethodName);

    /**
     * Returns a class which has a static method with the same name as
     * 'method', whose body creates an new instance of 'specimen' and invokes
     * 'method' upon it via an invokevirtual instruction with 'args' as
     * function call parameters.
     *
     * 'returns' is a dummy return value that need only match 'methods'
     * return type (it is only used in the dummy class when compiling IV).
     */
    private Class invokeVirtualHarness(
            Class specimen, ConcreteMethod method,
            String returns, String ... args) {
        Method cm = new ConcreteMethod(
            method.getReturnType(), method.getName(),
            "return " + returns + ";",  method.getElements());
        Class stub = new Class(specimen.getName(), cm);

        String params = toJoinedString(args, ", ");

        ConcreteMethod sm = new ConcreteMethod(
            method.getReturnType(), method.getName(),
            String.format("return (new %s()).%s(%s);",
                          specimen.getName(), method.getName(), params),
            new AccessFlag("public"), new AccessFlag("static"));

        Class iv = new Class("IV_" + specimen.getName(), sm);

        iv.addCompilationDependency(stub);
        iv.addCompilationDependency(cm);

        return iv;
    }

    /**
     * Returns a class which has a static method with the same name as
     * 'method', whose body creates an new instance of 'specimen', casts it
     * to 'iface' (including the type parameters)  and invokes
     * 'method' upon it via an invokeinterface instruction with 'args' as
     * function call parameters.
     */
    private Class invokeInterfaceHarness(Class specimen, Extends iface,
            AbstractMethod method, String ... args) {
        Interface istub = new Interface(
            iface.getType().getName(), iface.getType().getAccessFlags(),
            iface.getType().getParameters(),
            null, Arrays.asList((Method)method));
        Class cstub = new Class(specimen.getName());

        String params = toJoinedString(args, ", ");

        ConcreteMethod sm = new ConcreteMethod(
            "int", SourceModel.stdMethodName,
            String.format("return ((%s)(new %s())).%s(%s);", iface.toString(),
                specimen.getName(), method.getName(), params),
            new AccessFlag("public"), new AccessFlag("static"));
        sm.suppressWarnings();

        Class ii = new Class("II_" + specimen.getName() + "_" +
            iface.getType().getName(), sm);
        ii.addCompilationDependency(istub);
        ii.addCompilationDependency(cstub);
        ii.addCompilationDependency(method);
        return ii;
    }


    /**
     * Uses 'loader' to load class 'clzz', and calls the static method
     * 'method'.  If the return value does not equal 'value' (or if an
     * exception is thrown), then a test failure is indicated.
     *
     * If 'value' is null, then no equality check is performed -- the assertion
     * fails only if an exception is thrown.
     */
    protected void assertStaticCallEquals(
            ClassLoader loader, Class clzz, String method, Object value) {
        java.lang.Class<?> cls = null;
        try {
            cls = java.lang.Class.forName(clzz.getName(), true, loader);
        } catch (ClassNotFoundException e) {}
        assertNotNull(cls);

        java.lang.reflect.Method m = null;
        try {
            m = cls.getMethod(method);
        } catch (NoSuchMethodException e) {}
        assertNotNull(m);

        try {
            Object res = m.invoke(null);
            assertNotNull(res);
            if (value != null) {
                assertEquals(res, value);
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            fail("Unexpected exception thrown: " + e.getCause(), e.getCause());
        }
    }

    /**
     * Creates a class which calls target::method(args) via invokevirtual,
     * compiles and loads both the new class and 'target', and then invokes
     * the method.  If the returned value does not match 'value' then a
     * test failure is indicated.
     */
    public void assertInvokeVirtualEquals(
            Object value, Class target, ConcreteMethod method,
            String returns, String ... args) {

        Compiler compiler = compilerLocal.get();
        compiler.setFlags(compilerFlags());

        Class iv = invokeVirtualHarness(target, method, returns, args);
        ClassLoader loader = compiler.compile(iv, target);

        assertStaticCallEquals(loader, iv, method.getName(), value);
        compiler.cleanup();
    }

    /**
     * Convenience method for above, which assumes stdMethodName,
     * a return type of 'int', and no arguments.
     */
    public void assertInvokeVirtualEquals(int value, Class target) {
        assertInvokeVirtualEquals(value, target, stdCM, "-1");
    }

    /**
     * Creates a class which calls target::method(args) via invokeinterface
     * through 'iface', compiles and loads both it and 'target', and
     * then invokes the method.  If the returned value does not match
     * 'value' then a test failure is indicated.
     */
    public void assertInvokeInterfaceEquals(Object value, Class target,
            Extends iface, AbstractMethod method, String ... args) {

        Compiler compiler = compilerLocal.get();
        compiler.setFlags(compilerFlags());

        Class ii = invokeInterfaceHarness(target, iface, method, args);
        ClassLoader loader = compiler.compile(ii, target);

        assertStaticCallEquals(loader, ii, method.getName(), value);
        compiler.cleanup();
    }

    /**
     * Convenience method for above, which assumes stdMethodName,
     * a return type of 'int', and no arguments.
     */
    public void assertInvokeInterfaceEquals(
            int value, Class target, Interface iface) {

        Compiler compiler = compilerLocal.get();
        compiler.setFlags(compilerFlags());

        assertInvokeInterfaceEquals(value, target, new Extends(iface), stdAM);

        compiler.cleanup();
    }

    protected void assertInvokeInterfaceThrows(java.lang.Class<? extends Throwable> errorClass,
                                               Class target, Extends iface, AbstractMethod method,
                                               String... args) {
        try {
            assertInvokeInterfaceEquals(0, target, iface, method, args);
            fail("Expected exception: " + errorClass);
        }
        catch (AssertionError e) {
            Throwable cause = e.getCause();
            if (cause == null)
                throw e;
            else if ((errorClass.isAssignableFrom(cause.getClass()))) {
                // this is success
                return;
            }
            else
                throw e;
        }
    }

    /**
     * Creates a class which calls target::method(args) via invokevirtual,
     * compiles and loads both the new class and 'target', and then invokes
     * the method.  If an exception of type 'exceptionType' is not thrown,
     * then a test failure is indicated.
     */
    public void assertThrows(java.lang.Class<?> exceptionType, Class target,
            ConcreteMethod method, String returns, String ... args) {

        Compiler compiler = compilerLocal.get();
        compiler.setFlags(compilerFlags());

        Class iv = invokeVirtualHarness(target, method, returns, args);
        ClassLoader loader = compiler.compile(iv, target);

        java.lang.Class<?> cls = null;
        try {
            cls = java.lang.Class.forName(iv.getName(), true, loader);
        } catch (ClassNotFoundException e) {}
        assertNotNull(cls);

        java.lang.reflect.Method m = null;
        try {
            m = cls.getMethod(method.getName());
        } catch (NoSuchMethodException e) {}
        assertNotNull(m);

        try {
            m.invoke(null);
            fail("Exception should have been thrown");
        } catch (InvocationTargetException | IllegalAccessException e) {
            if (verboseLocal.get() == Boolean.TRUE) {
                System.out.println(e.getCause());
            }
            assertTrue(exceptionType.isAssignableFrom(e.getCause().getClass()));
        }
        compiler.cleanup();
    }

    /**
     * Convenience method for above, which assumes stdMethodName,
     * a return type of 'int', and no arguments.
     */
    public void assertThrows(java.lang.Class<?> exceptionType, Class target) {
        assertThrows(exceptionType, target, stdCM, "-1");
    }

    private static <T> String toJoinedString(T[] a, String... p) {
        return toJoinedString(Arrays.asList(a), p);
    }

    private static <T> String toJoinedString(List<T> list, String... p) {
        StringBuilder sb = new StringBuilder();
        String sep = "";
        String init = "";
        String end = "";
        String empty = null;
        switch (p.length) {
            case 4:
                empty = p[3];
            /*fall-through*/
            case 3:
                end = p[2];
            /*fall-through*/
            case 2:
                init = p[1];
            /*fall-through*/
            case 1:
                sep = p[0];
                break;
        }
        if (empty != null && list.isEmpty()) {
            return empty;
        } else {
            sb.append(init);
            for (T x : list) {
                if (sb.length() != init.length()) {
                    sb.append(sep);
                }
                sb.append(x.toString());
            }
            sb.append(end);
            return sb.toString();
        }
    }
}
