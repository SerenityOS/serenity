/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8216558
 * @summary unit tests for java.lang.invoke.MethodHandles
 * @library /test/lib /java/lang/invoke/common
 * @compile MethodHandlesTest.java MethodHandlesGeneralTest.java remote/RemoteExample.java
 * @run junit/othervm/timeout=2500 -XX:+IgnoreUnrecognizedVMOptions
 *                                 -XX:-VerifyDependencies
 *                                 -esa
 *                                 test.java.lang.invoke.MethodHandlesGeneralTest
 */

package test.java.lang.invoke;

import org.junit.*;
import test.java.lang.invoke.lib.CodeCacheOverflowProcessor;
import test.java.lang.invoke.remote.RemoteExample;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.invoke.MethodType.methodType;
import static org.junit.Assert.*;

public class MethodHandlesGeneralTest extends MethodHandlesTest {

    @Test
    public void testFirst() throws Throwable {
        verbosity += 9;
        try {
            // left blank for debugging
        } finally { printCounts(); verbosity -= 9; }
    }

    @Test
    public void testFindStatic() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testFindStatic0);
    }

    public void testFindStatic0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("findStatic");
        testFindStatic(PubExample.class, void.class, "s0");
        testFindStatic(Example.class, void.class, "s0");
        testFindStatic(Example.class, void.class, "pkg_s0");
        testFindStatic(Example.class, void.class, "pri_s0");
        testFindStatic(Example.class, void.class, "pro_s0");
        testFindStatic(PubExample.class, void.class, "Pub/pro_s0");

        testFindStatic(Example.class, Object.class, "s1", Object.class);
        testFindStatic(Example.class, Object.class, "s2", int.class);
        testFindStatic(Example.class, Object.class, "s3", long.class);
        testFindStatic(Example.class, Object.class, "s4", int.class, int.class);
        testFindStatic(Example.class, Object.class, "s5", long.class, int.class);
        testFindStatic(Example.class, Object.class, "s6", int.class, long.class);
        testFindStatic(Example.class, Object.class, "s7", float.class, double.class);

        testFindStatic(false, PRIVATE, Example.class, void.class, "bogus");
        testFindStatic(false, PRIVATE, Example.class, void.class, "<init>", int.class);
        testFindStatic(false, PRIVATE, Example.class, void.class, "<init>", Void.class);
        testFindStatic(false, PRIVATE, Example.class, void.class, "v0");
    }

    void testFindStatic(Class<?> defc, Class<?> ret, String name, Class<?>... params) throws Throwable {
        for (Object[] ac : accessCases(defc, name)) {
            testFindStatic((Boolean)ac[0], (Lookup)ac[1], defc, ret, name, params);
        }
    }

    void testFindStatic(Lookup lookup, Class<?> defc, Class<?> ret, String name, Class<?>... params) throws Throwable {
        testFindStatic(true, lookup, defc, ret, name, params);
    }

    void testFindStatic(boolean positive, Lookup lookup, Class<?> defc, Class<?> ret, String name, Class<?>... params) throws Throwable {
        countTest(positive);
        String methodName = name.substring(1 + name.indexOf('/'));  // foo/bar => foo
        MethodType type = MethodType.methodType(ret, params);
        MethodHandle target = null;
        Exception noAccess = null;
        try {
            if (verbosity >= 4)  System.out.println("lookup via "+lookup+" of "+defc+" "+name+type);
            target = maybeMoveIn(lookup, defc).findStatic(defc, methodName, type);
        } catch (ReflectiveOperationException ex) {
            noAccess = ex;
            assertExceptionClass(
                (name.contains("bogus") || INIT_REF_CAUSES_NSME && name.contains("<init>"))
                ?   NoSuchMethodException.class
                :   IllegalAccessException.class,
                noAccess);
            if (verbosity >= 5)  ex.printStackTrace(System.out);
        }
        if (verbosity >= 3)
            System.out.println("findStatic "+lookup+": "+defc.getName()+"."+name+"/"+type+" => "+target
                    +(noAccess == null ? "" : " !! "+noAccess));
        if (positive && noAccess != null)  throw noAccess;
        assertEquals(positive ? "positive test" : "negative test erroneously passed", positive, target != null);
        if (!positive)  return; // negative test failed as expected
        assertEquals(type, target.type());
        assertNameStringContains(target, methodName);
        Object[] args = randomArgs(params);
        printCalled(target, name, args);
        target.invokeWithArguments(args);
        assertCalled(name, args);
        if (verbosity >= 1)
            System.out.print(':');
    }

    @Test
    public void testFindVirtual() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testFindVirtual0);
    }

    public void testFindVirtual0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("findVirtual");
        testFindVirtual(Example.class, void.class, "v0");
        testFindVirtual(Example.class, void.class, "pkg_v0");
        testFindVirtual(Example.class, void.class, "pri_v0");
        testFindVirtual(Example.class, Object.class, "v1", Object.class);
        testFindVirtual(Example.class, Object.class, "v2", Object.class, Object.class);
        testFindVirtual(Example.class, Object.class, "v2", Object.class, int.class);
        testFindVirtual(Example.class, Object.class, "v2", int.class, Object.class);
        testFindVirtual(Example.class, Object.class, "v2", int.class, int.class);
        testFindVirtual(Example.class, void.class, "pro_v0");
        testFindVirtual(PubExample.class, void.class, "Pub/pro_v0");

        testFindVirtual(false, PRIVATE, Example.class, Example.class, void.class, "bogus");
        testFindVirtual(false, PRIVATE, Example.class, Example.class, void.class, "<init>", int.class);
        testFindVirtual(false, PRIVATE, Example.class, Example.class, void.class, "<init>", Void.class);
        testFindVirtual(false, PRIVATE, Example.class, Example.class, void.class, "s0");

        // test dispatch
        testFindVirtual(SubExample.class, SubExample.class, void.class, "Sub/v0");
        testFindVirtual(SubExample.class, Example.class, void.class, "Sub/v0");
        testFindVirtual(SubExample.class, IntExample.class, void.class, "Sub/v0");
        testFindVirtual(SubExample.class, SubExample.class, void.class, "Sub/pkg_v0");
        testFindVirtual(SubExample.class, Example.class, void.class, "Sub/pkg_v0");
        testFindVirtual(Example.class, IntExample.class, void.class, "v0");
        testFindVirtual(IntExample.Impl.class, IntExample.class, void.class, "Int/v0");
    }

    @Test
    public void testFindVirtualClone() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testFindVirtualClone0);
    }

    public void testFindVirtualClone0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        // test some ad hoc system methods
        testFindVirtual(false, PUBLIC, Object.class, Object.class, "clone");

        // ##### FIXME - disable tests for clone until we figure out how they should work with modules

        /*
        testFindVirtual(true, PUBLIC, Object[].class, Object.class, "clone");
        testFindVirtual(true, PUBLIC, int[].class, Object.class, "clone");
        for (Class<?> cls : new Class<?>[]{ boolean[].class, long[].class, float[].class, char[].class })
            testFindVirtual(true, PUBLIC, cls, Object.class, "clone");
         */
    }

    void testFindVirtual(Class<?> defc, Class<?> ret, String name, Class<?>... params) throws Throwable {
        Class<?> rcvc = defc;
        testFindVirtual(rcvc, defc, ret, name, params);
    }

    void testFindVirtual(Class<?> rcvc, Class<?> defc, Class<?> ret, String name, Class<?>... params) throws Throwable {
        for (Object[] ac : accessCases(defc, name)) {
            testFindVirtual((Boolean)ac[0], (Lookup)ac[1], rcvc, defc, ret, name, params);
        }
    }

    void testFindVirtual(Lookup lookup, Class<?> rcvc, Class<?> defc, Class<?> ret, String name, Class<?>... params) throws Throwable {
        testFindVirtual(true, lookup, rcvc, defc, ret, name, params);
    }

    void testFindVirtual(boolean positive, Lookup lookup, Class<?> defc, Class<?> ret, String name, Class<?>... params) throws Throwable {
        testFindVirtual(positive, lookup, defc, defc, ret, name, params);
    }

    void testFindVirtual(boolean positive, Lookup lookup, Class<?> rcvc, Class<?> defc, Class<?> ret, String name, Class<?>... params) throws Throwable {
        countTest(positive);
        String methodName = name.substring(1 + name.indexOf('/'));  // foo/bar => foo
        MethodType type = MethodType.methodType(ret, params);
        MethodHandle target = null;
        Exception noAccess = null;
        try {
            if (verbosity >= 4)  System.out.println("lookup via "+lookup+" of "+defc+" "+name+type);
            target = maybeMoveIn(lookup, defc).findVirtual(defc, methodName, type);
        } catch (ReflectiveOperationException ex) {
            noAccess = ex;
            assertExceptionClass(
                (name.contains("bogus") || INIT_REF_CAUSES_NSME && name.contains("<init>"))
                ? NoSuchMethodException.class
                : IllegalAccessException.class,
                noAccess);
            if (verbosity >= 5)  ex.printStackTrace(System.out);
        }
        if (verbosity >= 3)
            System.out.println("findVirtual "+lookup+": "+defc.getName()+"."+name+"/"+type+" => "+target
                    +(noAccess == null ? "" : " !! "+noAccess));
        if (positive && noAccess != null)  throw noAccess;
        assertEquals(positive ? "positive test" : "negative test erroneously passed", positive, target != null);
        if (!positive)  return; // negative test failed as expected
        Class<?> selfc = defc;
        // predict receiver type narrowing:
        if (lookup == SUBCLASS &&
                name.contains("pro_") &&
                selfc.isAssignableFrom(lookup.lookupClass())) {
            selfc = lookup.lookupClass();
            if (name.startsWith("Pub/"))  name = "Rem/"+name.substring(4);
        }
        Class<?>[] paramsWithSelf = cat(array(Class[].class, (Class)selfc), params);
        MethodType typeWithSelf = MethodType.methodType(ret, paramsWithSelf);
        assertEquals(typeWithSelf, target.type());
        assertNameStringContains(target, methodName);
        Object[] argsWithSelf = randomArgs(paramsWithSelf);
        if (selfc.isAssignableFrom(rcvc) && rcvc != selfc)  argsWithSelf[0] = randomArg(rcvc);
        printCalled(target, name, argsWithSelf);
        Object res = target.invokeWithArguments(argsWithSelf);
        if (Example.class.isAssignableFrom(defc) || IntExample.class.isAssignableFrom(defc)) {
            assertCalled(name, argsWithSelf);
        } else if (name.equals("clone")) {
            // Ad hoc method call outside Example.  For Object[].clone.
            printCalled(target, name, argsWithSelf);
            assertEquals(MethodType.methodType(Object.class, rcvc), target.type());
            Object orig = argsWithSelf[0];
            assertEquals(orig.getClass(), res.getClass());
            if (res instanceof Object[])
                assertArrayEquals((Object[])res, (Object[])argsWithSelf[0]);
            assert(Arrays.deepEquals(new Object[]{res}, new Object[]{argsWithSelf[0]}));
        } else {
            assert(false) : Arrays.asList(positive, lookup, rcvc, defc, ret, name, deepToString(params));
        }
        if (verbosity >= 1)
            System.out.print(':');
    }

    @Test
    public void testFindSpecial() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testFindSpecial0);
    }

    public void testFindSpecial0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("findSpecial");
        testFindSpecial(SubExample.class, Example.class, void.class, false, "v0");
        testFindSpecial(SubExample.class, Example.class, void.class, false, "pkg_v0");
        testFindSpecial(RemoteExample.class, PubExample.class, void.class, false, "Pub/pro_v0");
        testFindSpecial(Example.class, IntExample.class, void.class, true, "vd");
        // Do some negative testing:
        for (Lookup lookup : new Lookup[]{ PRIVATE, EXAMPLE, PACKAGE, PUBLIC }) {
            testFindSpecial(false, lookup, Object.class, Example.class, void.class, "v0");
            testFindSpecial(false, lookup, SubExample.class, Example.class, void.class, "bogus");
            testFindSpecial(false, lookup, SubExample.class, Example.class, void.class, "<init>", int.class);
            testFindSpecial(false, lookup, SubExample.class, Example.class, void.class, "<init>", Void.class);
            testFindSpecial(false, lookup, SubExample.class, Example.class, void.class, "s0");
            testFindSpecial(false, lookup, Example.class, IntExample.class, void.class, "v0");
        }
    }

    void testFindSpecial(Class<?> specialCaller,
                         Class<?> defc, Class<?> ret, boolean dflt, String name, Class<?>... params) throws Throwable {
        if (specialCaller == RemoteExample.class) {
            testFindSpecial(false, EXAMPLE, specialCaller, defc, ret, name, params);
            testFindSpecial(false, PRIVATE, specialCaller, defc, ret, name, params);
            testFindSpecial(false, PACKAGE, specialCaller, defc, ret, name, params);
            testFindSpecial(true, SUBCLASS, specialCaller, defc, ret, name, params);
            testFindSpecial(false, PUBLIC, specialCaller, defc, ret, name, params);
            return;
        }
        testFindSpecial(true, EXAMPLE, specialCaller, defc, ret, name, params);
        testFindSpecial(true, PRIVATE, specialCaller, defc, ret, name, params);
        testFindSpecial(false || dflt, PACKAGE,  specialCaller, defc, ret, name, params);
        testFindSpecial(false, SUBCLASS, specialCaller, defc, ret, name, params);
        testFindSpecial(false, PUBLIC, specialCaller, defc, ret, name, params);
    }

    void testFindSpecial(boolean positive, Lookup lookup, Class<?> specialCaller,
                         Class<?> defc, Class<?> ret, String name, Class<?>... params) throws Throwable {
        countTest(positive);
        String methodName = name.substring(1 + name.indexOf('/'));  // foo/bar => foo
        MethodType type = MethodType.methodType(ret, params);
        Lookup specialLookup = maybeMoveIn(lookup, specialCaller);
        boolean specialAccessOK = (specialLookup.lookupClass() == specialCaller &&
                                  (specialLookup.lookupModes() & Lookup.PRIVATE) != 0);
        MethodHandle target = null;
        Exception noAccess = null;
        try {
            if (verbosity >= 4)  System.out.println("lookup via "+lookup+" of "+defc+" "+name+type);
            if (verbosity >= 5)  System.out.println("  lookup => "+specialLookup);
            target = specialLookup.findSpecial(defc, methodName, type, specialCaller);
        } catch (ReflectiveOperationException ex) {
            noAccess = ex;
            assertExceptionClass(
                (!specialAccessOK)  // this check should happen first
                ? IllegalAccessException.class
                : (name.contains("bogus") || INIT_REF_CAUSES_NSME && name.contains("<init>"))
                ? NoSuchMethodException.class
                : IllegalAccessException.class,
                noAccess);
            if (verbosity >= 5)  ex.printStackTrace(System.out);
        }
        if (verbosity >= 3)
            System.out.println("findSpecial from "+specialCaller.getName()+" to "+defc.getName()+"."+name+"/"+type+" => "+target
                               +(target == null ? "" : target.type())
                               +(noAccess == null ? "" : " !! "+noAccess));
        if (positive && noAccess != null)  throw noAccess;
        assertEquals(positive ? "positive test" : "negative test erroneously passed", positive, target != null);
        if (!positive)  return; // negative test failed as expected
        assertEquals(specialCaller, target.type().parameterType(0));
        assertEquals(type, target.type().dropParameterTypes(0,1));
        Class<?>[] paramsWithSelf = cat(array(Class[].class, (Class)specialCaller), params);
        MethodType typeWithSelf = MethodType.methodType(ret, paramsWithSelf);
        assertNameStringContains(target, methodName);
        Object[] args = randomArgs(paramsWithSelf);
        printCalled(target, name, args);
        target.invokeWithArguments(args);
        assertCalled(name, args);
    }

    @Test
    public void testFindConstructor() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testFindConstructor0);
    }

    public void testFindConstructor0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("findConstructor");
        testFindConstructor(true, EXAMPLE, Example.class);
        testFindConstructor(true, EXAMPLE, Example.class, int.class);
        testFindConstructor(true, EXAMPLE, Example.class, int.class, int.class);
        testFindConstructor(true, EXAMPLE, Example.class, int.class, long.class);
        testFindConstructor(true, EXAMPLE, Example.class, int.class, float.class);
        testFindConstructor(true, EXAMPLE, Example.class, int.class, double.class);
        testFindConstructor(true, EXAMPLE, Example.class, String.class);
        testFindConstructor(true, EXAMPLE, Example.class, int.class, int.class, int.class);
        testFindConstructor(true, EXAMPLE, Example.class, int.class, int.class, int.class, int.class);
    }

    void testFindConstructor(boolean positive, Lookup lookup,
                             Class<?> defc, Class<?>... params) throws Throwable {
        countTest(positive);
        MethodType type = MethodType.methodType(void.class, params);
        MethodHandle target = null;
        Exception noAccess = null;
        try {
            if (verbosity >= 4)  System.out.println("lookup via "+lookup+" of "+defc+" <init>"+type);
            target = lookup.findConstructor(defc, type);
        } catch (ReflectiveOperationException ex) {
            noAccess = ex;
            assertTrue(noAccess.getClass().getName(), noAccess instanceof IllegalAccessException);
        }
        if (verbosity >= 3)
            System.out.println("findConstructor "+defc.getName()+".<init>/"+type+" => "+target
                               +(target == null ? "" : target.type())
                               +(noAccess == null ? "" : " !! "+noAccess));
        if (positive && noAccess != null)  throw noAccess;
        assertEquals(positive ? "positive test" : "negative test erroneously passed", positive, target != null);
        if (!positive)  return; // negative test failed as expected
        assertEquals(type.changeReturnType(defc), target.type());
        Object[] args = randomArgs(params);
        printCalled(target, defc.getSimpleName(), args);
        Object obj = target.invokeWithArguments(args);
        if (!(defc == Example.class && params.length < 2))
            assertCalled(defc.getSimpleName()+".<init>", args);
        assertTrue("instance of "+defc.getName(), defc.isInstance(obj));
    }

    @Test
    public void testBind() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testBind0);
    }

    public void testBind0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("bind");
        testBind(Example.class, void.class, "v0");
        testBind(Example.class, void.class, "pkg_v0");
        testBind(Example.class, void.class, "pri_v0");
        testBind(Example.class, Object.class, "v1", Object.class);
        testBind(Example.class, Object.class, "v2", Object.class, Object.class);
        testBind(Example.class, Object.class, "v2", Object.class, int.class);
        testBind(Example.class, Object.class, "v2", int.class, Object.class);
        testBind(Example.class, Object.class, "v2", int.class, int.class);
        testBind(false, PRIVATE, Example.class, void.class, "bogus");
        testBind(false, PRIVATE, Example.class, void.class, "<init>", int.class);
        testBind(false, PRIVATE, Example.class, void.class, "<init>", Void.class);
        testBind(SubExample.class, void.class, "Sub/v0");
        testBind(SubExample.class, void.class, "Sub/pkg_v0");
        testBind(IntExample.Impl.class, void.class, "Int/v0");
    }

    void testBind(Class<?> defc, Class<?> ret, String name, Class<?>... params) throws Throwable {
        for (Object[] ac : accessCases(defc, name)) {
            testBind((Boolean)ac[0], (Lookup)ac[1], defc, ret, name, params);
        }
    }

    void testBind(boolean positive, Lookup lookup, Class<?> defc, Class<?> ret, String name, Class<?>... params) throws Throwable {
        countTest(positive);
        String methodName = name.substring(1 + name.indexOf('/'));  // foo/bar => foo
        MethodType type = MethodType.methodType(ret, params);
        Object receiver = randomArg(defc);
        MethodHandle target = null;
        Exception noAccess = null;
        try {
            if (verbosity >= 4)  System.out.println("lookup via "+lookup+" of "+defc+" "+name+type);
            target = maybeMoveIn(lookup, defc).bind(receiver, methodName, type);
        } catch (ReflectiveOperationException ex) {
            noAccess = ex;
            assertExceptionClass(
                (name.contains("bogus") || INIT_REF_CAUSES_NSME && name.contains("<init>"))
                ? NoSuchMethodException.class
                : IllegalAccessException.class,
                noAccess);
            if (verbosity >= 5)  ex.printStackTrace(System.out);
        }
        if (verbosity >= 3)
            System.out.println("bind "+receiver+"."+name+"/"+type+" => "+target
                    +(noAccess == null ? "" : " !! "+noAccess));
        if (positive && noAccess != null)  throw noAccess;
        assertEquals(positive ? "positive test" : "negative test erroneously passed", positive, target != null);
        if (!positive)  return; // negative test failed as expected
        assertEquals(type, target.type());
        Object[] args = randomArgs(params);
        printCalled(target, name, args);
        target.invokeWithArguments(args);
        Object[] argsWithReceiver = cat(array(Object[].class, receiver), args);
        assertCalled(name, argsWithReceiver);
        if (verbosity >= 1)
            System.out.print(':');
    }

    @Test
    public void testUnreflect() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testUnreflect0);
    }

    public void testUnreflect0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("unreflect");
        testUnreflect(Example.class, true, void.class, "s0");
        testUnreflect(Example.class, true, void.class, "pro_s0");
        testUnreflect(Example.class, true, void.class, "pkg_s0");
        testUnreflect(Example.class, true, void.class, "pri_s0");

        testUnreflect(Example.class, true, Object.class, "s1", Object.class);
        testUnreflect(Example.class, true, Object.class, "s2", int.class);
        testUnreflect(Example.class, true, Object.class, "s3", long.class);
        testUnreflect(Example.class, true, Object.class, "s4", int.class, int.class);
        testUnreflect(Example.class, true, Object.class, "s5", long.class, int.class);
        testUnreflect(Example.class, true, Object.class, "s6", int.class, long.class);

        testUnreflect(Example.class, false, void.class, "v0");
        testUnreflect(Example.class, false, void.class, "pkg_v0");
        testUnreflect(Example.class, false, void.class, "pri_v0");
        testUnreflect(Example.class, false, Object.class, "v1", Object.class);
        testUnreflect(Example.class, false, Object.class, "v2", Object.class, Object.class);
        testUnreflect(Example.class, false, Object.class, "v2", Object.class, int.class);
        testUnreflect(Example.class, false, Object.class, "v2", int.class, Object.class);
        testUnreflect(Example.class, false, Object.class, "v2", int.class, int.class);

        // Test a public final member in another package:
        testUnreflect(RemoteExample.class, false, void.class, "Rem/fin_v0");
    }

    void testUnreflect(Class<?> defc, boolean isStatic, Class<?> ret, String name, Class<?>... params) throws Throwable {
        for (Object[] ac : accessCases(defc, name)) {
            testUnreflectMaybeSpecial(null, (Boolean)ac[0], (Lookup)ac[1], defc, (isStatic ? null : defc), ret, name, params);
        }
    }

    void testUnreflect(Class<?> defc, Class<?> rcvc, Class<?> ret, String name, Class<?>... params) throws Throwable {
        for (Object[] ac : accessCases(defc, name)) {
            testUnreflectMaybeSpecial(null, (Boolean)ac[0], (Lookup)ac[1], defc, rcvc, ret, name, params);
        }
    }

    void testUnreflectMaybeSpecial(Class<?> specialCaller,
                                   boolean positive, Lookup lookup,
                                   Class<?> defc, Class<?> rcvc, Class<?> ret, String name, Class<?>... params) throws Throwable {
        countTest(positive);
        String methodName = name.substring(1 + name.indexOf('/'));  // foo/bar => foo
        MethodType type = MethodType.methodType(ret, params);
        Lookup specialLookup = (specialCaller != null ? maybeMoveIn(lookup, specialCaller) : null);
        boolean specialAccessOK = (specialCaller != null &&
                                  specialLookup.lookupClass() == specialCaller &&
                                  (specialLookup.lookupModes() & Lookup.PRIVATE) != 0);
        Method rmethod = defc.getDeclaredMethod(methodName, params);
        MethodHandle target = null;
        Exception noAccess = null;
        boolean isStatic = (rcvc == null);
        boolean isSpecial = (specialCaller != null);
        try {
            if (verbosity >= 4)  System.out.println("lookup via "+lookup+" of "+defc+" "+name+type);
            if (isSpecial)
                target = specialLookup.unreflectSpecial(rmethod, specialCaller);
            else
                target = maybeMoveIn(lookup, defc).unreflect(rmethod);
        } catch (ReflectiveOperationException ex) {
            noAccess = ex;
            assertExceptionClass(
                IllegalAccessException.class,  // NSME is impossible, since it was already reflected
                noAccess);
            if (verbosity >= 5)  ex.printStackTrace(System.out);
        }
        if (verbosity >= 3)
            System.out.println("unreflect"+(isSpecial?"Special":"")+" "+defc.getName()+"."+name+"/"+type
                               +(!isSpecial ? "" : " specialCaller="+specialCaller)
                               +( isStatic  ? "" : " receiver="+rcvc)
                               +" => "+target
                               +(noAccess == null ? "" : " !! "+noAccess));
        if (positive && noAccess != null)  throw noAccess;
        assertEquals(positive ? "positive test" : "negative test erroneously passed", positive, target != null);
        if (!positive)  return; // negative test failed as expected
        assertEquals(isStatic, Modifier.isStatic(rmethod.getModifiers()));
        Class<?>[] paramsMaybeWithSelf = params;
        if (!isStatic) {
            paramsMaybeWithSelf = cat(array(Class[].class, (Class)rcvc), params);
        }
        MethodType typeMaybeWithSelf = MethodType.methodType(ret, paramsMaybeWithSelf);
        if (isStatic) {
            assertEquals(typeMaybeWithSelf, target.type());
        } else {
            if (isSpecial)
                assertEquals(specialCaller, target.type().parameterType(0));
            else
                assertEquals(defc, target.type().parameterType(0));
            assertEquals(typeMaybeWithSelf, target.type().changeParameterType(0, rcvc));
        }
        Object[] argsMaybeWithSelf = randomArgs(paramsMaybeWithSelf);
        printCalled(target, name, argsMaybeWithSelf);
        target.invokeWithArguments(argsMaybeWithSelf);
        assertCalled(name, argsMaybeWithSelf);
        if (verbosity >= 1)
            System.out.print(':');
    }

    void testUnreflectSpecial(Class<?> defc, Class<?> rcvc, Class<?> ret, String name, Class<?>... params) throws Throwable {
        for (Object[] ac : accessCases(defc, name, true)) {
            Class<?> specialCaller = rcvc;
            testUnreflectMaybeSpecial(specialCaller, (Boolean)ac[0], (Lookup)ac[1], defc, rcvc, ret, name, params);
        }
    }

    @Test
    public void testUnreflectSpecial() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testUnreflectSpecial0);
    }

    public void testUnreflectSpecial0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("unreflectSpecial");
        testUnreflectSpecial(Example.class, Example.class, void.class, "v0");
        testUnreflectSpecial(Example.class, SubExample.class, void.class, "v0");
        testUnreflectSpecial(Example.class, Example.class, void.class, "pkg_v0");
        testUnreflectSpecial(Example.class, SubExample.class, void.class, "pkg_v0");
        testUnreflectSpecial(Example.class, Example.class, Object.class, "v2", int.class, int.class);
        testUnreflectSpecial(Example.class, SubExample.class, Object.class, "v2", int.class, int.class);
        testUnreflectMaybeSpecial(Example.class, false, PRIVATE, Example.class, Example.class, void.class, "s0");
    }

    @Test
    public void testUnreflectGetter() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testUnreflectGetter0);
    }

    public void testUnreflectGetter0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("unreflectGetter");
        testGetter(TEST_UNREFLECT);
    }

    @Test
    public void testFindGetter() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testFindGetter0);
    }

    public void testFindGetter0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("findGetter");
        testGetter(TEST_FIND_FIELD);
        testGetter(TEST_FIND_FIELD | TEST_BOUND);
    }

    @Test
    public void testFindStaticGetter() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testFindStaticGetter0);
    }

    public void testFindStaticGetter0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("findStaticGetter");
        testGetter(TEST_FIND_STATIC);
    }

    public void testGetter(int testMode) throws Throwable {
        Lookup lookup = PRIVATE;  // FIXME: test more lookups than this one
        for (Object[] c : HasFields.testCasesFor(testMode)) {
            boolean positive = (c[1] != Error.class);
            testGetter(positive, lookup, c[0], c[1], testMode);
            if (positive)
                testGetter(positive, lookup, c[0], c[1], testMode | TEST_NPE);
        }
        testGetter(true, lookup,
                   new Object[]{ true,  System.class, "out", java.io.PrintStream.class },
                   System.out, testMode);
        for (int isStaticN = 0; isStaticN <= 1; isStaticN++) {
            testGetter(false, lookup,
                       new Object[]{ (isStaticN != 0), System.class, "bogus", char.class },
                       null, testMode);
        }
    }

    public void testGetter(boolean positive, MethodHandles.Lookup lookup,
                           Object fieldRef, Object value, int testMode) throws Throwable {
        testAccessor(positive, lookup, fieldRef, value, testMode);
    }

    public void testAccessor(boolean positive0, MethodHandles.Lookup lookup,
                             Object fieldRef, Object value, int testMode0) throws Throwable {
        if (verbosity >= 4)
            System.out.println("testAccessor"+Arrays.deepToString(new Object[]{positive0, lookup, fieldRef, value, testMode0}));
        boolean isGetter = ((testMode0 & TEST_SETTER) == 0);
        boolean doBound  = ((testMode0 & TEST_BOUND) != 0);
        boolean testNPE  = ((testMode0 & TEST_NPE) != 0);
        int testMode = testMode0 & ~(TEST_SETTER | TEST_BOUND | TEST_NPE);
        boolean positive = positive0 && !testNPE;
        boolean isStatic;
        Class<?> fclass;
        String   fname;
        Class<?> ftype;
        Field f = (fieldRef instanceof Field ? (Field)fieldRef : null);
        if (f != null) {
            isStatic = Modifier.isStatic(f.getModifiers());
            fclass   = f.getDeclaringClass();
            fname    = f.getName();
            ftype    = f.getType();
        } else {
            Object[] scnt = (Object[]) fieldRef;
            isStatic = (Boolean)  scnt[0];
            fclass   = (Class<?>) scnt[1];
            fname    = (String)   scnt[2];
            ftype    = (Class<?>) scnt[3];
            try {
                f = fclass.getDeclaredField(fname);
            } catch (ReflectiveOperationException ex) {
                f = null;
            }
        }
        if (!testModeMatches(testMode, isStatic))  return;
        if (f == null && testMode == TEST_UNREFLECT)  return;
        if (testNPE && isStatic)  return;
        countTest(positive);
        MethodType expType;
        if (isGetter)
            expType = MethodType.methodType(ftype, HasFields.class);
        else
            expType = MethodType.methodType(void.class, HasFields.class, ftype);
        if (isStatic)  expType = expType.dropParameterTypes(0, 1);
        Exception noAccess = null;
        MethodHandle mh;
        try {
            switch (testMode0 & ~(TEST_BOUND | TEST_NPE)) {
            case TEST_UNREFLECT:   mh = lookup.unreflectGetter(f);                      break;
            case TEST_FIND_FIELD:  mh = lookup.findGetter(fclass, fname, ftype);        break;
            case TEST_FIND_STATIC: mh = lookup.findStaticGetter(fclass, fname, ftype);  break;
            case TEST_SETTER|
                 TEST_UNREFLECT:   mh = lookup.unreflectSetter(f);                      break;
            case TEST_SETTER|
                 TEST_FIND_FIELD:  mh = lookup.findSetter(fclass, fname, ftype);        break;
            case TEST_SETTER|
                 TEST_FIND_STATIC: mh = lookup.findStaticSetter(fclass, fname, ftype);  break;
            default:
                throw new InternalError("testMode="+testMode);
            }
        } catch (ReflectiveOperationException ex) {
            mh = null;
            noAccess = ex;
            assertExceptionClass(
                (fname.contains("bogus"))
                ?   NoSuchFieldException.class
                :   IllegalAccessException.class,
                noAccess);
            if (verbosity >= 5)  ex.printStackTrace(System.out);
        }
        if (verbosity >= 3)
            System.out.format("%s%s %s.%s/%s => %s %s%n",
                              (testMode0 & TEST_UNREFLECT) != 0
                                  ? "unreflect"
                                  : "find" + ((testMode0 & TEST_FIND_STATIC) != 0 ? "Static" : ""),
                              (isGetter ? "Getter" : "Setter"),
                              fclass.getName(), fname, ftype, mh,
                              (noAccess == null ? "" : " !! "+noAccess));
        // negative test case and expected noAccess, then done.
        if (!positive && noAccess != null) return;
        // positive test case but found noAccess, then error
        if (positive && !testNPE && noAccess != null)  throw new RuntimeException(noAccess);
        assertEquals(positive0 ? "positive test" : "negative test erroneously passed", positive0, mh != null);
        if (!positive && !testNPE)  return; // negative access test failed as expected
        assertEquals((isStatic ? 0 : 1)+(isGetter ? 0 : 1), mh.type().parameterCount());
        assertSame(mh.type(), expType);
        //assertNameStringContains(mh, fname);  // This does not hold anymore with LFs
        HasFields fields = new HasFields();
        HasFields fieldsForMH = fields;
        if (testNPE)  fieldsForMH = null;  // perturb MH argument to elicit expected error
        if (doBound)
            mh = mh.bindTo(fieldsForMH);
        Object sawValue;
        Class<?> vtype = ftype;
        if (ftype != int.class)  vtype = Object.class;
        if (isGetter) {
            mh = mh.asType(mh.type().generic()
                           .changeReturnType(vtype));
        } else {
            int last = mh.type().parameterCount() - 1;
            mh = mh.asType(mh.type().generic()
                           .changeReturnType(void.class)
                           .changeParameterType(last, vtype));
        }
        if (f != null && f.getDeclaringClass() == HasFields.class) {
            assertEquals(f.get(fields), value);  // clean to start with
        }
        Throwable caughtEx = null;
        // non-final field and setAccessible(true) on instance field will have write access
        boolean writeAccess = !Modifier.isFinal(f.getModifiers()) ||
                              (!Modifier.isStatic(f.getModifiers()) && f.isAccessible());
        if (isGetter) {
            Object expValue = value;
            for (int i = 0; i <= 1; i++) {
                sawValue = null;  // make DA rules happy under try/catch
                try {
                    if (isStatic || doBound) {
                        if (ftype == int.class)
                            sawValue = (int) mh.invokeExact();  // do these exactly
                        else
                            sawValue = mh.invokeExact();
                    } else {
                        if (ftype == int.class)
                            sawValue = (int) mh.invokeExact((Object) fieldsForMH);
                        else
                            sawValue = mh.invokeExact((Object) fieldsForMH);
                    }
                } catch (RuntimeException ex) {
                    if (ex instanceof NullPointerException && testNPE) {
                        caughtEx = ex;
                        break;
                    }
                }
                assertEquals(sawValue, expValue);
                if (f != null && f.getDeclaringClass() == HasFields.class && writeAccess) {
                    Object random = randomArg(ftype);
                    f.set(fields, random);
                    expValue = random;
                } else {
                    break;
                }
            }
        } else {
            for (int i = 0; i <= 1; i++) {
                Object putValue = randomArg(ftype);
                try {
                    if (isStatic || doBound) {
                        if (ftype == int.class)
                            mh.invokeExact((int)putValue);  // do these exactly
                        else
                            mh.invokeExact(putValue);
                    } else {
                        if (ftype == int.class)
                            mh.invokeExact((Object) fieldsForMH, (int)putValue);
                        else
                            mh.invokeExact((Object) fieldsForMH, putValue);
                    }
                } catch (RuntimeException ex) {
                    if (ex instanceof NullPointerException && testNPE) {
                        caughtEx = ex;
                        break;
                    }
                }
                if (f != null && f.getDeclaringClass() == HasFields.class) {
                    assertEquals(f.get(fields), putValue);
                }
            }
        }
        if (f != null && f.getDeclaringClass() == HasFields.class && writeAccess) {
            f.set(fields, value);  // put it back if it has write access
        }
        if (testNPE) {
            if (caughtEx == null || !(caughtEx instanceof NullPointerException))
                throw new RuntimeException("failed to catch NPE exception"+(caughtEx == null ? " (caughtEx=null)" : ""), caughtEx);
            caughtEx = null;  // nullify expected exception
        }
        if (caughtEx != null) {
            throw new RuntimeException("unexpected exception", caughtEx);
        }
    }

    @Test
    public void testUnreflectSetter() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testUnreflectSetter0);
    }

    public void testUnreflectSetter0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("unreflectSetter");
        testSetter(TEST_UNREFLECT);
    }

    @Test
    public void testFindSetter() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testFindSetter0);
    }

    public void testFindSetter0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("findSetter");
        testSetter(TEST_FIND_FIELD);
        testSetter(TEST_FIND_FIELD | TEST_BOUND);
    }

    @Test
    public void testFindStaticSetter() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testFindStaticSetter0);
    }

    public void testFindStaticSetter0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("findStaticSetter");
        testSetter(TEST_FIND_STATIC);
    }

    public void testSetter(int testMode) throws Throwable {
        Lookup lookup = PRIVATE;  // FIXME: test more lookups than this one
        startTest("testSetter");
        for (Object[] c : HasFields.testCasesFor(testMode|TEST_SETTER)) {
            boolean positive = (c[1] != Error.class);
            testSetter(positive, lookup, c[0], c[1], testMode);
            if (positive)
                testSetter(positive, lookup, c[0], c[1], testMode | TEST_NPE);
        }
        for (int isStaticN = 0; isStaticN <= 1; isStaticN++) {
            testSetter(false, lookup,
                       new Object[]{ (isStaticN != 0), System.class, "bogus", char.class },
                       null, testMode);
        }
    }

    public void testSetter(boolean positive, MethodHandles.Lookup lookup,
                           Object fieldRef, Object value, int testMode) throws Throwable {
        testAccessor(positive, lookup, fieldRef, value, testMode | TEST_SETTER);
    }

    @Test
    public void testArrayElementGetter() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testArrayElementGetter0);
    }

    public void testArrayElementGetter0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("arrayElementGetter");
        testArrayElementGetterSetter(false);
    }

    @Test
    public void testArrayElementSetter() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testArrayElementSetter0);
    }

    public void testArrayElementSetter0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("arrayElementSetter");
        testArrayElementGetterSetter(true);
    }

    private static final int TEST_ARRAY_NONE = 0, TEST_ARRAY_NPE = 1, TEST_ARRAY_OOB = 2, TEST_ARRAY_ASE = 3;

    public void testArrayElementGetterSetter(boolean testSetter) throws Throwable {
        testArrayElementGetterSetter(testSetter, TEST_ARRAY_NONE);
    }

    @Test
    public void testArrayElementErrors() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testArrayElementErrors0);
    }

    public void testArrayElementErrors0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("arrayElementErrors");
        testArrayElementGetterSetter(false, TEST_ARRAY_NPE);
        testArrayElementGetterSetter(true, TEST_ARRAY_NPE);
        testArrayElementGetterSetter(false, TEST_ARRAY_OOB);
        testArrayElementGetterSetter(true, TEST_ARRAY_OOB);
        testArrayElementGetterSetter(new Object[10], true, TEST_ARRAY_ASE);
        testArrayElementGetterSetter(new Example[10], true, TEST_ARRAY_ASE);
        testArrayElementGetterSetter(new IntExample[10], true, TEST_ARRAY_ASE);
    }

    public void testArrayElementGetterSetter(boolean testSetter, int negTest) throws Throwable {
        testArrayElementGetterSetter(new String[10], testSetter, negTest);
        testArrayElementGetterSetter(new Iterable<?>[10], testSetter, negTest);
        testArrayElementGetterSetter(new Example[10], testSetter, negTest);
        testArrayElementGetterSetter(new IntExample[10], testSetter, negTest);
        testArrayElementGetterSetter(new Object[10], testSetter, negTest);
        testArrayElementGetterSetter(new boolean[10], testSetter, negTest);
        testArrayElementGetterSetter(new byte[10], testSetter, negTest);
        testArrayElementGetterSetter(new char[10], testSetter, negTest);
        testArrayElementGetterSetter(new short[10], testSetter, negTest);
        testArrayElementGetterSetter(new int[10], testSetter, negTest);
        testArrayElementGetterSetter(new float[10], testSetter, negTest);
        testArrayElementGetterSetter(new long[10], testSetter, negTest);
        testArrayElementGetterSetter(new double[10], testSetter, negTest);
    }

    public void testArrayElementGetterSetter(Object array, boolean testSetter, int negTest) throws Throwable {
        boolean positive = (negTest == TEST_ARRAY_NONE);
        int length = Array.getLength(array);
        Class<?> arrayType = array.getClass();
        Class<?> elemType = arrayType.getComponentType();
        Object arrayToMH = array;
        // this stanza allows negative tests to make argument perturbations:
        switch (negTest) {
        case TEST_ARRAY_NPE:
            arrayToMH = null;
            break;
        case TEST_ARRAY_OOB:
            assert(length > 0);
            arrayToMH = Array.newInstance(elemType, 0);
            break;
        case TEST_ARRAY_ASE:
            assert(testSetter && !elemType.isPrimitive());
            if (elemType == Object.class)
                arrayToMH = new StringBuffer[length];  // very random subclass of Object!
            else if (elemType == Example.class)
                arrayToMH = new SubExample[length];
            else if (elemType == IntExample.class)
                arrayToMH = new SubIntExample[length];
            else
                return;  // can't make an ArrayStoreException test
            assert(arrayType.isInstance(arrayToMH))
                : Arrays.asList(arrayType, arrayToMH.getClass(), testSetter, negTest);
            break;
        }
        countTest(positive);
        if (verbosity > 2)  System.out.println("array type = "+array.getClass().getComponentType().getName()+"["+length+"]"+(positive ? "" : " negative test #"+negTest+" using "+Arrays.deepToString(new Object[]{arrayToMH})));
        MethodType expType = !testSetter
                ? MethodType.methodType(elemType,   arrayType, int.class)
                : MethodType.methodType(void.class, arrayType, int.class, elemType);
        MethodHandle mh = !testSetter
                ? MethodHandles.arrayElementGetter(arrayType)
                : MethodHandles.arrayElementSetter(arrayType);
        assertSame(mh.type(), expType);
        if (elemType != int.class && elemType != boolean.class) {
            MethodType gtype = mh.type().generic().changeParameterType(1, int.class);
            if (testSetter)  gtype = gtype.changeReturnType(void.class);
            mh = mh.asType(gtype);
        }
        Object sawValue, expValue;
        List<Object> model = array2list(array);
        Throwable caughtEx = null;
        for (int i = 0; i < length; i++) {
            // update array element
            Object random = randomArg(elemType);
            model.set(i, random);
            if (testSetter) {
                try {
                    if (elemType == int.class)
                        mh.invokeExact((int[]) arrayToMH, i, (int)random);
                    else if (elemType == boolean.class)
                        mh.invokeExact((boolean[]) arrayToMH, i, (boolean)random);
                    else
                        mh.invokeExact(arrayToMH, i, random);
                } catch (RuntimeException ex) {
                    caughtEx = ex;
                    break;
                }
                assertEquals(model, array2list(array));
            } else {
                Array.set(array, i, random);
            }
            if (verbosity >= 5) {
                List<Object> array2list = array2list(array);
                System.out.println("a["+i+"]="+random+" => "+array2list);
                if (!array2list.equals(model))
                    System.out.println("***   != "+model);
            }
            // observe array element
            sawValue = Array.get(array, i);
            if (!testSetter) {
                expValue = sawValue;
                try {
                    if (elemType == int.class)
                        sawValue = (int) mh.invokeExact((int[]) arrayToMH, i);
                    else if (elemType == boolean.class)
                        sawValue = (boolean) mh.invokeExact((boolean[]) arrayToMH, i);
                    else
                        sawValue = mh.invokeExact(arrayToMH, i);
                } catch (RuntimeException ex) {
                    caughtEx = ex;
                    break;
                }
                assertEquals(sawValue, expValue);
                assertEquals(model, array2list(array));
            }
        }
        if (!positive) {
            if (caughtEx == null)
                throw new RuntimeException("failed to catch exception for negTest="+negTest);
            // test the kind of exception
            Class<?> reqType = null;
            switch (negTest) {
            case TEST_ARRAY_ASE:  reqType = ArrayStoreException.class; break;
            case TEST_ARRAY_OOB:  reqType = ArrayIndexOutOfBoundsException.class; break;
            case TEST_ARRAY_NPE:  reqType = NullPointerException.class; break;
            default:              assert(false);
            }
            if (reqType.isInstance(caughtEx)) {
                caughtEx = null;  // nullify expected exception
            }
        }
        if (caughtEx != null) {
            throw new RuntimeException("unexpected exception", caughtEx);
        }
    }

    List<Object> array2list(Object array) {
        int length = Array.getLength(array);
        ArrayList<Object> model = new ArrayList<>(length);
        for (int i = 0; i < length; i++)
            model.add(Array.get(array, i));
        return model;
    }

    @Test
    public void testConvertArguments() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testConvertArguments0);
    }

    public void testConvertArguments0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("convertArguments");
        testConvert(Callee.ofType(1), null, "id", int.class);
        testConvert(Callee.ofType(1), null, "id", String.class);
        testConvert(Callee.ofType(1), null, "id", Integer.class);
        testConvert(Callee.ofType(1), null, "id", short.class);
        testConvert(Callee.ofType(1), null, "id", char.class);
        testConvert(Callee.ofType(1), null, "id", byte.class);
    }

    void testConvert(MethodHandle id, Class<?> rtype, String name, Class<?>... params) throws Throwable {
        testConvert(true, id, rtype, name, params);
    }

    void testConvert(boolean positive,
                     MethodHandle id, Class<?> rtype, String name, Class<?>... params) throws Throwable {
        countTest(positive);
        MethodType idType = id.type();
        if (rtype == null)  rtype = idType.returnType();
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null)  params[i] = idType.parameterType(i);
        }
        // simulate the pairwise conversion
        MethodType newType = MethodType.methodType(rtype, params);
        Object[] args = randomArgs(newType.parameterArray());
        Object[] convArgs = args.clone();
        for (int i = 0; i < args.length; i++) {
            Class<?> src = newType.parameterType(i);
            Class<?> dst = idType.parameterType(i);
            if (src != dst)
                convArgs[i] = castToWrapper(convArgs[i], dst);
        }
        Object convResult = id.invokeWithArguments(convArgs);
        {
            Class<?> dst = newType.returnType();
            Class<?> src = idType.returnType();
            if (src != dst)
                convResult = castToWrapper(convResult, dst);
        }
        MethodHandle target = null;
        RuntimeException error = null;
        try {
            target = id.asType(newType);
        } catch (WrongMethodTypeException ex) {
            error = ex;
        }
        if (verbosity >= 3)
            System.out.println("convert "+id+ " to "+newType+" => "+target
                    +(error == null ? "" : " !! "+error));
        if (positive && error != null)  throw error;
        assertEquals(positive ? "positive test" : "negative test erroneously passed", positive, target != null);
        if (!positive)  return; // negative test failed as expected
        assertEquals(newType, target.type());
        printCalled(target, id.toString(), args);
        Object result = target.invokeWithArguments(args);
        assertCalled(name, convArgs);
        assertEquals(convResult, result);
        if (verbosity >= 1)
            System.out.print(':');
    }

    @Test
    public void testVarargsCollector() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testVarargsCollector0);
    }

    public void testVarargsCollector0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("varargsCollector");
        MethodHandle vac0 = PRIVATE.findStatic(MethodHandlesTest.class, "called",
                               MethodType.methodType(Object.class, String.class, Object[].class));
        vac0 = vac0.bindTo("vac");
        MethodHandle vac = vac0.asVarargsCollector(Object[].class);
        testConvert(true, vac.asType(MethodType.genericMethodType(0)), null, "vac");
        testConvert(true, vac.asType(MethodType.genericMethodType(0)), null, "vac");
        for (Class<?> at : new Class<?>[] { Object.class, String.class, Integer.class }) {
            testConvert(true, vac.asType(MethodType.genericMethodType(1)), null, "vac", at);
            testConvert(true, vac.asType(MethodType.genericMethodType(2)), null, "vac", at, at);
        }
    }

    @Test
    public void testFilterReturnValue() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testFilterReturnValue0);
    }

    public void testFilterReturnValue0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("filterReturnValue");
        Class<?> classOfVCList = varargsList(1).invokeWithArguments(0).getClass();
        assertTrue(List.class.isAssignableFrom(classOfVCList));
        for (int nargs = 0; nargs <= 3; nargs++) {
            for (Class<?> rtype : new Class<?>[] { Object.class,
                                                   List.class,
                                                   int.class,
                                                   byte.class,
                                                   long.class,
                                                   CharSequence.class,
                                                   String.class }) {
                testFilterReturnValue(nargs, rtype);
            }
        }
    }

    void testFilterReturnValue(int nargs, Class<?> rtype) throws Throwable {
        countTest();
        MethodHandle target = varargsList(nargs, rtype);
        MethodHandle filter;
        if (List.class.isAssignableFrom(rtype) || rtype.isAssignableFrom(List.class))
            filter = varargsList(1);  // add another layer of list-ness
        else
            filter = MethodHandles.identity(rtype);
        filter = filter.asType(MethodType.methodType(target.type().returnType(), rtype));
        Object[] argsToPass = randomArgs(nargs, Object.class);
        if (verbosity >= 3)
            System.out.println("filter "+target+" to "+rtype.getSimpleName()+" with "+filter);
        MethodHandle target2 = MethodHandles.filterReturnValue(target, filter);
        if (verbosity >= 4)
            System.out.println("filtered target: "+target2);
        // Simulate expected effect of filter on return value:
        Object unfiltered = target.invokeWithArguments(argsToPass);
        Object expected = filter.invokeWithArguments(unfiltered);
        if (verbosity >= 4)
            System.out.println("unfiltered: "+unfiltered+" : "+unfiltered.getClass().getSimpleName());
        if (verbosity >= 4)
            System.out.println("expected: "+expected+" : "+expected.getClass().getSimpleName());
        Object result = target2.invokeWithArguments(argsToPass);
        if (verbosity >= 3)
            System.out.println("result: "+result+" : "+result.getClass().getSimpleName());
        if (!expected.equals(result))
            System.out.println("*** fail at n/rt = "+nargs+"/"+rtype.getSimpleName()+": "+
                               Arrays.asList(argsToPass)+" => "+result+" != "+expected);
        assertEquals(expected, result);
    }

    @Test
    public void testFilterArguments() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testFilterArguments0);
    }

    public void testFilterArguments0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("filterArguments");
        for (int nargs = 1; nargs <= 6; nargs++) {
            for (int pos = 0; pos < nargs; pos++) {
                testFilterArguments(nargs, pos);
            }
        }
    }

    void testFilterArguments(int nargs, int pos) throws Throwable {
        countTest();
        MethodHandle target = varargsList(nargs);
        MethodHandle filter = varargsList(1);
        filter = filter.asType(filter.type().generic());
        Object[] argsToPass = randomArgs(nargs, Object.class);
        if (verbosity >= 3)
            System.out.println("filter "+target+" at "+pos+" with "+filter);
        MethodHandle target2 = MethodHandles.filterArguments(target, pos, filter);
        // Simulate expected effect of filter on arglist:
        Object[] filteredArgs = argsToPass.clone();
        filteredArgs[pos] = filter.invokeExact(filteredArgs[pos]);
        List<Object> expected = Arrays.asList(filteredArgs);
        Object result = target2.invokeWithArguments(argsToPass);
        if (verbosity >= 3)
            System.out.println("result: "+result);
        if (!expected.equals(result))
            System.out.println("*** fail at n/p = "+nargs+"/"+pos+": "+Arrays.asList(argsToPass)+" => "+result+" != "+expected);
        assertEquals(expected, result);
    }

    @Test
    public void testCollectArguments() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testCollectArguments0);
    }

    public void testCollectArguments0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("collectArguments");
        testFoldOrCollectArguments(true, false);
    }

    @Test
    public void testFoldArguments() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testFoldArguments0);
        CodeCacheOverflowProcessor.runMHTest(this::testFoldArguments1);
    }

    public void testFoldArguments0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("foldArguments");
        testFoldOrCollectArguments(false, false);
    }

    public void testFoldArguments1() throws Throwable {
        if (CAN_SKIP_WORKING) return;
        startTest("foldArguments/pos");
        testFoldOrCollectArguments(false, true);
    }

    void testFoldOrCollectArguments(boolean isCollect, boolean withFoldPos) throws Throwable {
        assert !(isCollect && withFoldPos); // exclude illegal argument combination
        for (Class<?> lastType : new Class<?>[]{ Object.class, String.class, int.class }) {
            for (Class<?> collectType : new Class<?>[]{ Object.class, String.class, int.class, void.class }) {
                int maxArity = 10;
                if (collectType != String.class)  maxArity = 5;
                if (lastType != Object.class)  maxArity = 4;
                for (int nargs = 0; nargs <= maxArity; nargs++) {
                    ArrayList<Class<?>> argTypes = new ArrayList<>(Collections.nCopies(nargs, Object.class));
                    int maxMix = 20;
                    if (collectType != Object.class)  maxMix = 0;
                    Map<Object,Integer> argTypesSeen = new HashMap<>();
                    for (int mix = 0; mix <= maxMix; mix++) {
                        if (!mixArgs(argTypes, mix, argTypesSeen))  continue;
                        for (int collect = 0; collect <= nargs; collect++) {
                            for (int pos = 0; pos <= nargs - collect; pos++) {
                                testFoldOrCollectArguments(argTypes, pos, collect, collectType, lastType, isCollect, withFoldPos);
                            }
                        }
                    }
                }
            }
        }
    }

    boolean mixArgs(List<Class<?>> argTypes, int mix, Map<Object,Integer> argTypesSeen) {
        assert(mix >= 0);
        if (mix == 0)  return true;  // no change
        if ((mix >>> argTypes.size()) != 0)  return false;
        for (int i = 0; i < argTypes.size(); i++) {
            if (i >= 31)  break;
            boolean bit = (mix & (1 << i)) != 0;
            if (bit) {
                Class<?> type = argTypes.get(i);
                if (type == Object.class)
                    type = String.class;
                else if (type == String.class)
                    type = int.class;
                else
                    type = Object.class;
                argTypes.set(i, type);
            }
        }
        Integer prev = argTypesSeen.put(new ArrayList<>(argTypes), mix);
        if (prev != null) {
            if (verbosity >= 4)  System.out.println("mix "+prev+" repeated "+mix+": "+argTypes);
            return false;
        }
        if (verbosity >= 3)  System.out.println("mix "+mix+" = "+argTypes);
        return true;
    }

    void testFoldOrCollectArguments(List<Class<?>> argTypes,  // argument types minus the inserted combineType
                                    int pos, int fold, // position and length of the folded arguments
                                    Class<?> combineType, // type returned from the combiner
                                    Class<?> lastType,  // type returned from the target
                                    boolean isCollect,
                                    boolean withFoldPos) throws Throwable {
        int nargs = argTypes.size();
        if (pos != 0 && !isCollect && !withFoldPos)  return;  // test MethodHandles.foldArguments(MH,MH) only for pos=0
        countTest();
        List<Class<?>> combineArgTypes = argTypes.subList(pos, pos + fold);
        List<Class<?>> targetArgTypes = new ArrayList<>(argTypes);
        if (isCollect)  // does target see arg[pos..pos+cc-1]?
            targetArgTypes.subList(pos, pos + fold).clear();
        if (combineType != void.class)
            targetArgTypes.add(pos, combineType);
        MethodHandle target = varargsList(targetArgTypes, lastType);
        MethodHandle combine = varargsList(combineArgTypes, combineType);
        List<Object> argsToPass = Arrays.asList(randomArgs(argTypes));
        if (verbosity >= 3)
            System.out.println((isCollect ? "collect" : "fold")+" "+target+" with "+combine);
        MethodHandle target2;
        if (isCollect)
            target2 = MethodHandles.collectArguments(target, pos, combine);
        else
            target2 = withFoldPos ? MethodHandles.foldArguments(target, pos, combine) : MethodHandles.foldArguments(target, combine);
        // Simulate expected effect of combiner on arglist:
        List<Object> expectedList = new ArrayList<>(argsToPass);
        List<Object> argsToFold = expectedList.subList(pos, pos + fold);
        if (verbosity >= 3)
            System.out.println((isCollect ? "collect" : "fold")+": "+argsToFold+" into "+target2);
        Object foldedArgs = combine.invokeWithArguments(argsToFold);
        if (isCollect)
            argsToFold.clear();
        if (combineType != void.class)
            argsToFold.add(0, foldedArgs);
        Object result = target2.invokeWithArguments(argsToPass);
        if (verbosity >= 3)
            System.out.println("result: "+result);
        Object expected = target.invokeWithArguments(expectedList);
        if (!expected.equals(result))
            System.out.println("*** fail at n/p/f = "+nargs+"/"+pos+"/"+fold+": "+argsToPass+" => "+result+" != "+expected);
        assertEquals(expected, result);
    }

    @Test
    public void testDropArguments() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testDropArguments0);
    }

    public void testDropArguments0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("dropArguments");
        for (int nargs = 0; nargs <= 4; nargs++) {
            for (int drop = 1; drop <= 4; drop++) {
                for (int pos = 0; pos <= nargs; pos++) {
                    testDropArguments(nargs, pos, drop);
                }
            }
        }
    }

    void testDropArguments(int nargs, int pos, int drop) throws Throwable {
        countTest();
        MethodHandle target = varargsArray(nargs);
        Object[] args = randomArgs(target.type().parameterArray());
        MethodHandle target2 = MethodHandles.dropArguments(target, pos,
                Collections.nCopies(drop, Object.class).toArray(new Class<?>[0]));
        List<Object> resList = Arrays.asList(args);
        List<Object> argsToDrop = new ArrayList<>(resList);
        for (int i = drop; i > 0; i--) {
            argsToDrop.add(pos, "blort#"+i);
        }
        Object res2 = target2.invokeWithArguments(argsToDrop);
        Object res2List = Arrays.asList((Object[])res2);
        //if (!resList.equals(res2List))
        //    System.out.println("*** fail at n/p/d = "+nargs+"/"+pos+"/"+drop+": "+argsToDrop+" => "+res2List);
        assertEquals(resList, res2List);
    }

    @Test
    public void testGuardWithTest() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testGuardWithTest0);
    }

    public void testGuardWithTest0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("guardWithTest");
        for (int nargs = 0; nargs <= 50; nargs++) {
            if (CAN_TEST_LIGHTLY && nargs > 7)  break;
            testGuardWithTest(nargs, Object.class);
            testGuardWithTest(nargs, String.class);
        }
    }

    void testGuardWithTest(int nargs, Class<?> argClass) throws Throwable {
        testGuardWithTest(nargs, 0, argClass);
        if (nargs <= 5 || nargs % 10 == 3) {
            for (int testDrops = 1; testDrops <= nargs; testDrops++)
                testGuardWithTest(nargs, testDrops, argClass);
        }
    }

    void testGuardWithTest(int nargs, int testDrops, Class<?> argClass) throws Throwable {
        countTest();
        int nargs1 = Math.min(3, nargs);
        MethodHandle test = PRIVATE.findVirtual(Object.class, "equals", MethodType.methodType(boolean.class, Object.class));
        MethodHandle target = PRIVATE.findStatic(MethodHandlesTest.class, "targetIfEquals", MethodType.genericMethodType(nargs1));
        MethodHandle fallback = PRIVATE.findStatic(MethodHandlesTest.class, "fallbackIfNotEquals", MethodType.genericMethodType(nargs1));
        while (test.type().parameterCount() > nargs)
            // 0: test = constant(MISSING_ARG.equals(MISSING_ARG))
            // 1: test = lambda (_) MISSING_ARG.equals(_)
            test = MethodHandles.insertArguments(test, 0, MISSING_ARG);
        if (argClass != Object.class) {
            test = changeArgTypes(test, argClass);
            target = changeArgTypes(target, argClass);
            fallback = changeArgTypes(fallback, argClass);
        }
        int testArgs = nargs - testDrops;
        assert(testArgs >= 0);
        test = addTrailingArgs(test, Math.min(testArgs, nargs), argClass);
        target = addTrailingArgs(target, nargs, argClass);
        fallback = addTrailingArgs(fallback, nargs, argClass);
        Object[][] argLists = {
            { },
            { "foo" }, { MethodHandlesTest.MISSING_ARG },
            { "foo", "foo" }, { "foo", "bar" },
            { "foo", "foo", "baz" }, { "foo", "bar", "baz" }
        };
        for (Object[] argList : argLists) {
            Object[] argList1 = argList;
            if (argList.length != nargs) {
                if (argList.length != nargs1)  continue;
                argList1 = Arrays.copyOf(argList, nargs);
                Arrays.fill(argList1, nargs1, nargs, MethodHandlesTest.MISSING_ARG_2);
            }
            MethodHandle test1 = test;
            if (test1.type().parameterCount() > testArgs) {
                int pc = test1.type().parameterCount();
                test1 = MethodHandles.insertArguments(test, testArgs, Arrays.copyOfRange(argList1, testArgs, pc));
            }
            MethodHandle mh = MethodHandles.guardWithTest(test1, target, fallback);
            assertEquals(target.type(), mh.type());
            boolean equals;
            switch (nargs) {
            case 0:   equals = true; break;
            case 1:   equals = MethodHandlesTest.MISSING_ARG.equals(argList[0]); break;
            default:  equals = argList[0].equals(argList[1]); break;
            }
            String willCall = (equals ? "targetIfEquals" : "fallbackIfNotEquals");
            if (verbosity >= 3)
                System.out.println(logEntry(willCall, argList));
            Object result = mh.invokeWithArguments(argList1);
            assertCalled(willCall, argList);
        }
    }

    @Test
    public void testGenericLoopCombinator() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testGenericLoopCombinator0);
    }

    public void testGenericLoopCombinator0() throws Throwable {
        if (CAN_SKIP_WORKING) return;
        startTest("loop");
        // Test as follows:
        // * Have an increasing number of loop-local state. Local state type diversity grows with the number.
        // * Initializers set the starting value of loop-local state from the corresponding loop argument.
        // * For each local state element, there is a predicate - for all state combinations, exercise all predicates.
        // * Steps modify each local state element in each iteration.
        // * Finalizers group all local state elements into a resulting array. Verify end values.
        // * Exercise both pre- and post-checked loops.
        // Local state types, start values, predicates, and steps:
        // * int a, 0, a < 7, a = a + 1
        // * double b, 7.0, b > 0.5, b = b / 2.0
        // * String c, "start", c.length <= 9, c = c + a
        final Class<?>[] argTypes = new Class<?>[] {int.class, double.class, String.class};
        final Object[][] args = new Object[][] {
            new Object[]{0              },
            new Object[]{0, 7.0         },
            new Object[]{0, 7.0, "start"}
        };
        // These are the expected final state tuples for argument type tuple / predicate combinations, for pre- and
        // post-checked loops:
        final Object[][] preCheckedResults = new Object[][] {
            new Object[]{7                           }, // (int) / int
            new Object[]{7, 0.0546875                }, // (int,double) / int
            new Object[]{5, 0.4375                   }, // (int,double) / double
            new Object[]{7, 0.0546875, "start1234567"}, // (int,double,String) / int
            new Object[]{5, 0.4375,    "start1234"   }, // (int,double,String) / double
            new Object[]{6, 0.109375,  "start12345"  }  // (int,double,String) / String
        };
        final Object[][] postCheckedResults = new Object[][] {
            new Object[]{7                         }, // (int) / int
            new Object[]{7, 0.109375               }, // (int,double) / int
            new Object[]{4, 0.4375                 }, // (int,double) / double
            new Object[]{7, 0.109375, "start123456"}, // (int,double,String) / int
            new Object[]{4, 0.4375,   "start123"   }, // (int,double,String) / double
            new Object[]{5, 0.21875,  "start12345" }  // (int,double,String) / String
        };
        final Lookup l = MethodHandles.lookup();
        final Class<?> MHT = MethodHandlesTest.class;
        final Class<?> B = boolean.class;
        final Class<?> I = int.class;
        final Class<?> D = double.class;
        final Class<?> S = String.class;
        final MethodHandle hip = l.findStatic(MHT, "loopIntPred", methodType(B, I));
        final MethodHandle hdp = l.findStatic(MHT, "loopDoublePred", methodType(B, I, D));
        final MethodHandle hsp = l.findStatic(MHT, "loopStringPred", methodType(B, I, D, S));
        final MethodHandle his = l.findStatic(MHT, "loopIntStep", methodType(I, I));
        final MethodHandle hds = l.findStatic(MHT, "loopDoubleStep", methodType(D, I, D));
        final MethodHandle hss = l.findStatic(MHT, "loopStringStep", methodType(S, I, D, S));
        final MethodHandle[] preds = new MethodHandle[] {hip, hdp, hsp};
        final MethodHandle[] steps = new MethodHandle[] {his, hds, hss};
        for (int nargs = 1, useResultsStart = 0; nargs <= argTypes.length; useResultsStart += nargs++) {
            Class<?>[] useArgTypes = Arrays.copyOf(argTypes, nargs, Class[].class);
            MethodHandle[] usePreds = Arrays.copyOf(preds, nargs, MethodHandle[].class);
            MethodHandle[] useSteps = Arrays.copyOf(steps, nargs, MethodHandle[].class);
            Object[] useArgs = args[nargs - 1];
            Object[][] usePreCheckedResults = new Object[nargs][];
            Object[][] usePostCheckedResults = new Object[nargs][];
            System.arraycopy(preCheckedResults, useResultsStart, usePreCheckedResults, 0, nargs);
            System.arraycopy(postCheckedResults, useResultsStart, usePostCheckedResults, 0, nargs);
            testGenericLoopCombinator(nargs, useArgTypes, usePreds, useSteps, useArgs, usePreCheckedResults,
                    usePostCheckedResults);
        }
    }

    void testGenericLoopCombinator(int nargs, Class<?>[] argTypes, MethodHandle[] preds, MethodHandle[] steps,
                                   Object[] args, Object[][] preCheckedResults, Object[][] postCheckedResults)
            throws Throwable {
        List<Class<?>> lArgTypes = Arrays.asList(argTypes);
        // Predicate and step handles are passed in as arguments, initializer and finalizer handles are constructed here
        // from the available information.
        MethodHandle[] inits = new MethodHandle[nargs];
        for (int i = 0; i < nargs; ++i) {
            MethodHandle h;
            // Initializers are meant to return whatever they are passed at a given argument position. This means that
            // additional arguments may have to be appended and prepended.
            h = MethodHandles.identity(argTypes[i]);
            if (i < nargs - 1) {
                h = MethodHandles.dropArguments(h, 1, lArgTypes.subList(i + 1, nargs));
            }
            if (i > 0) {
                h = MethodHandles.dropArguments(h, 0, lArgTypes.subList(0, i));
            }
            inits[i] = h;
        }
        // Finalizers are all meant to collect all of the loop-local state in a single array and return that. Local
        // state is passed before the loop args. Construct such a finalizer by first taking a varargsArray collector for
        // the number of local state arguments, and then appending the loop args as to-be-dropped arguments.
        MethodHandle[] finis = new MethodHandle[nargs];
        MethodHandle genericFini = MethodHandles.dropArguments(
                varargsArray(nargs).asType(methodType(Object[].class, lArgTypes)), nargs, lArgTypes);
        Arrays.fill(finis, genericFini);
        // The predicate and step handles' signatures need to be extended. They currently just accept local state args;
        // append possibly missing local state args and loop args using dropArguments.
        for (int i = 0; i < nargs; ++i) {
            List<Class<?>> additionalLocalStateArgTypes = lArgTypes.subList(i + 1, nargs);
            preds[i] = MethodHandles.dropArguments(
                    MethodHandles.dropArguments(preds[i], i + 1, additionalLocalStateArgTypes), nargs, lArgTypes);
            steps[i] = MethodHandles.dropArguments(
                    MethodHandles.dropArguments(steps[i], i + 1, additionalLocalStateArgTypes), nargs, lArgTypes);
        }
        // Iterate over all of the predicates, using only one of them at a time.
        for (int i = 0; i < nargs; ++i) {
            MethodHandle[] usePreds;
            if (nargs == 1) {
                usePreds = preds;
            } else {
                // Create an all-null preds array, and only use one predicate in this iteration. The null entries will
                // be substituted with true predicates by the loop combinator.
                usePreds = new MethodHandle[nargs];
                usePreds[i] = preds[i];
            }
            // Go for it.
            if (verbosity >= 3) {
                System.out.println("calling loop for argument types " + lArgTypes + " with predicate at index " + i);
                if (verbosity >= 5) {
                    System.out.println("predicates: " + Arrays.asList(usePreds));
                }
            }
            MethodHandle[] preInits = new MethodHandle[nargs + 1];
            MethodHandle[] prePreds = new MethodHandle[nargs + 1];
            MethodHandle[] preSteps = new MethodHandle[nargs + 1];
            MethodHandle[] preFinis = new MethodHandle[nargs + 1];
            System.arraycopy(inits, 0, preInits, 1, nargs);
            System.arraycopy(usePreds, 0, prePreds, 0, nargs); // preds are offset by 1 for pre-checked loops
            System.arraycopy(steps, 0, preSteps, 1, nargs);
            System.arraycopy(finis, 0, preFinis, 0, nargs); // finis are also offset by 1 for pre-checked loops
            // Convert to clause-major form.
            MethodHandle[][] preClauses = new MethodHandle[nargs + 1][4];
            MethodHandle[][] postClauses = new MethodHandle[nargs][4];
            toClauseMajor(preClauses, preInits, preSteps, prePreds, preFinis);
            toClauseMajor(postClauses, inits, steps, usePreds, finis);
            MethodHandle pre = MethodHandles.loop(preClauses);
            MethodHandle post = MethodHandles.loop(postClauses);
            if (verbosity >= 6) {
                System.out.println("pre-handle: " + pre);
            }
            Object[] preResults = (Object[]) pre.invokeWithArguments(args);
            if (verbosity >= 4) {
                System.out.println("pre-checked: expected " + Arrays.asList(preCheckedResults[i]) + ", actual " +
                        Arrays.asList(preResults));
            }
            if (verbosity >= 6) {
                System.out.println("post-handle: " + post);
            }
            Object[] postResults = (Object[]) post.invokeWithArguments(args);
            if (verbosity >= 4) {
                System.out.println("post-checked: expected " + Arrays.asList(postCheckedResults[i]) + ", actual " +
                        Arrays.asList(postResults));
            }
            assertArrayEquals(preCheckedResults[i], preResults);
            assertArrayEquals(postCheckedResults[i], postResults);
        }
    }

    static void toClauseMajor(MethodHandle[][] clauses, MethodHandle[] init, MethodHandle[] step, MethodHandle[] pred, MethodHandle[] fini) {
        for (int i = 0; i < clauses.length; ++i) {
            clauses[i][0] = init[i];
            clauses[i][1] = step[i];
            clauses[i][2] = pred[i];
            clauses[i][3] = fini[i];
        }
    }

    @Test
    public void testThrowException() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testThrowException0);
    }

    public void testThrowException0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("throwException");
        testThrowException(int.class, new ClassCastException("testing"));
        testThrowException(void.class, new java.io.IOException("testing"));
        testThrowException(String.class, new LinkageError("testing"));
    }

    void testThrowException(Class<?> returnType, Throwable thrown) throws Throwable {
        countTest();
        Class<? extends Throwable> exType = thrown.getClass();
        MethodHandle target = MethodHandles.throwException(returnType, exType);
        //System.out.println("throwing with "+target+" : "+thrown);
        MethodType expectedType = MethodType.methodType(returnType, exType);
        assertEquals(expectedType, target.type());
        target = target.asType(target.type().generic());
        Throwable caught = null;
        try {
            Object res = target.invokeExact((Object) thrown);
            fail("got "+res+" instead of throwing "+thrown);
        } catch (Throwable ex) {
            if (ex != thrown) {
                if (ex instanceof Error)  throw (Error)ex;
                if (ex instanceof RuntimeException)  throw (RuntimeException)ex;
            }
            caught = ex;
        }
        assertSame(thrown, caught);
    }

    @Test
    public void testTryFinally() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testTryFinally0);
    }

    public void testTryFinally0() throws Throwable {
        if (CAN_SKIP_WORKING) return;
        startTest("tryFinally");
        String inputMessage = "returned";
        String augmentedMessage = "augmented";
        String thrownMessage = "thrown";
        String rethrownMessage = "rethrown";
        // Test these cases:
        // * target returns, cleanup passes through
        // * target returns, cleanup augments
        // * target throws, cleanup augments and returns
        // * target throws, cleanup augments and rethrows
        MethodHandle target = MethodHandles.identity(String.class);
        MethodHandle targetThrow = MethodHandles.dropArguments(
                MethodHandles.throwException(String.class, Exception.class).bindTo(new Exception(thrownMessage)), 0, String.class);
        MethodHandle cleanupPassThrough = MethodHandles.dropArguments(MethodHandles.identity(String.class), 0,
                Throwable.class, String.class);
        MethodHandle cleanupAugment = MethodHandles.dropArguments(MethodHandles.constant(String.class, augmentedMessage),
                0, Throwable.class, String.class, String.class);
        MethodHandle cleanupCatch = MethodHandles.dropArguments(MethodHandles.constant(String.class, thrownMessage), 0,
                Throwable.class, String.class, String.class);
        MethodHandle cleanupThrow = MethodHandles.dropArguments(MethodHandles.throwException(String.class, Exception.class).
                bindTo(new Exception(rethrownMessage)), 0, Throwable.class, String.class, String.class);
        testTryFinally(target, cleanupPassThrough, inputMessage, inputMessage, false);
        testTryFinally(target, cleanupAugment, inputMessage, augmentedMessage, false);
        testTryFinally(targetThrow, cleanupCatch, inputMessage, thrownMessage, true);
        testTryFinally(targetThrow, cleanupThrow, inputMessage, rethrownMessage, true);
        // Test the same cases as above for void targets and cleanups.
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Class<?> C = this.getClass();
        MethodType targetType = methodType(void.class, String[].class);
        MethodType cleanupType = methodType(void.class, Throwable.class, String[].class);
        MethodHandle vtarget = lookup.findStatic(C, "vtarget", targetType);
        MethodHandle vtargetThrow = lookup.findStatic(C, "vtargetThrow", targetType);
        MethodHandle vcleanupPassThrough = lookup.findStatic(C, "vcleanupPassThrough", cleanupType);
        MethodHandle vcleanupAugment = lookup.findStatic(C, "vcleanupAugment", cleanupType);
        MethodHandle vcleanupCatch = lookup.findStatic(C, "vcleanupCatch", cleanupType);
        MethodHandle vcleanupThrow = lookup.findStatic(C, "vcleanupThrow", cleanupType);
        testTryFinally(vtarget, vcleanupPassThrough, inputMessage, inputMessage, false);
        testTryFinally(vtarget, vcleanupAugment, inputMessage, augmentedMessage, false);
        testTryFinally(vtargetThrow, vcleanupCatch, inputMessage, thrownMessage, true);
        testTryFinally(vtargetThrow, vcleanupThrow, inputMessage, rethrownMessage, true);
    }

    void testTryFinally(MethodHandle target, MethodHandle cleanup, String input, String msg, boolean mustCatch)
            throws Throwable {
        countTest();
        MethodHandle tf = MethodHandles.tryFinally(target, cleanup);
        String result = null;
        boolean isVoid = target.type().returnType() == void.class;
        String[] argArray = new String[]{input};
        try {
            if (isVoid) {
                tf.invoke(argArray);
            } else {
                result = (String) tf.invoke(input);
            }
        } catch (Throwable t) {
            assertTrue(mustCatch);
            assertEquals(msg, t.getMessage());
            return;
        }
        assertFalse(mustCatch);
        if (isVoid) {
            assertEquals(msg, argArray[0]);
        } else {
            assertEquals(msg, result);
        }
    }

    @Test
    public void testAsInterfaceInstance() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testAsInterfaceInstance0);
    }

    public void testAsInterfaceInstance0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("asInterfaceInstance");
        Lookup lookup = MethodHandles.lookup();
        // test typical case:  Runnable.run
        {
            countTest();
            if (verbosity >= 2)  System.out.println("Runnable");
            MethodType mt = MethodType.methodType(void.class);
            MethodHandle mh = lookup.findStatic(MethodHandlesGeneralTest.class, "runForRunnable", mt);
            Runnable proxy = MethodHandleProxies.asInterfaceInstance(Runnable.class, mh);
            proxy.run();
            assertCalled("runForRunnable");
        }
        // well known single-name overloaded interface:  Appendable.append
        {
            countTest();
            if (verbosity >= 2)  System.out.println("Appendable");
            ArrayList<List<?>> appendResults = new ArrayList<>();
            MethodHandle append = lookup.bind(appendResults, "add", MethodType.methodType(boolean.class, Object.class));
            append = append.asType(MethodType.methodType(void.class, List.class)); // specialize the type
            MethodHandle asList = lookup.findStatic(Arrays.class, "asList", MethodType.methodType(List.class, Object[].class));
            MethodHandle mh = MethodHandles.filterReturnValue(asList, append).asVarargsCollector(Object[].class);
            Appendable proxy = MethodHandleProxies.asInterfaceInstance(Appendable.class, mh);
            proxy.append("one");
            proxy.append("two", 3, 4);
            proxy.append('5');
            assertEquals(Arrays.asList(Arrays.asList("one"),
                                       Arrays.asList("two", 3, 4),
                                       Arrays.asList('5')),
                         appendResults);
            if (verbosity >= 3)  System.out.println("appendResults="+appendResults);
            appendResults.clear();
            Formatter formatter = new Formatter(proxy);
            String fmt = "foo str=%s char='%c' num=%d";
            Object[] fmtArgs = { "str!", 'C', 42 };
            String expect = String.format(fmt, fmtArgs);
            formatter.format(fmt, fmtArgs);
            String actual = "";
            if (verbosity >= 3)  System.out.println("appendResults="+appendResults);
            for (List<?> l : appendResults) {
                Object x = l.get(0);
                switch (l.size()) {
                case 1:  actual += x; continue;
                case 3:  actual += ((String)x).substring((int)(Object)l.get(1), (int)(Object)l.get(2)); continue;
                }
                actual += l;
            }
            if (verbosity >= 3)  System.out.println("expect="+expect);
            if (verbosity >= 3)  System.out.println("actual="+actual);
            assertEquals(expect, actual);
        }
        // test case of an single name which is overloaded:  Fooable.foo(...)
        {
            if (verbosity >= 2)  System.out.println("Fooable");
            MethodHandle mh = lookup.findStatic(MethodHandlesGeneralTest.class, "fooForFooable",
                                                MethodType.methodType(Object.class, String.class, Object[].class));
            Fooable proxy = MethodHandleProxies.asInterfaceInstance(Fooable.class, mh);
            for (Method m : Fooable.class.getDeclaredMethods()) {
                countTest();
                assertSame("foo", m.getName());
                if (verbosity > 3)
                    System.out.println("calling "+m);
                MethodHandle invoker = lookup.unreflect(m);
                MethodType mt = invoker.type();
                Class<?>[] types = mt.parameterArray();
                types[0] = int.class;  // placeholder
                Object[] args = randomArgs(types);
                args[0] = proxy;
                if (verbosity > 3)
                    System.out.println("calling "+m+" on "+Arrays.asList(args));
                Object result = invoker.invokeWithArguments(args);
                if (verbosity > 4)
                    System.out.println("result = "+result);
                String name = "fooForFooable/"+args[1];
                Object[] argTail = Arrays.copyOfRange(args, 2, args.length);
                assertCalled(name, argTail);
                assertEquals(result, logEntry(name, argTail));
            }
        }
        // test processing of thrown exceptions:
        for (Throwable ex : new Throwable[] { new NullPointerException("ok"),
                                              new InternalError("ok"),
                                              new Throwable("fail"),
                                              new Exception("fail"),
                                              new MyCheckedException()
                                            }) {
            MethodHandle mh = MethodHandles.throwException(void.class, Throwable.class);
            mh = MethodHandles.insertArguments(mh, 0, ex);
            WillThrow proxy = MethodHandleProxies.asInterfaceInstance(WillThrow.class, mh);
            try {
                countTest();
                proxy.willThrow();
                System.out.println("Failed to throw: "+ex);
                assertTrue(false);
            } catch (Throwable ex1) {
                if (verbosity > 3) {
                    System.out.println("throw "+ex);
                    System.out.println("catch "+(ex == ex1 ? "UNWRAPPED" : ex1));
                }
                if (ex instanceof RuntimeException ||
                    ex instanceof Error) {
                    assertSame("must pass unchecked exception out without wrapping", ex, ex1);
                } else if (ex instanceof MyCheckedException) {
                    assertSame("must pass declared exception out without wrapping", ex, ex1);
                } else {
                    assertNotSame("must pass undeclared checked exception with wrapping", ex, ex1);
                    if (!(ex1 instanceof UndeclaredThrowableException) || ex1.getCause() != ex) {
                        ex1.printStackTrace(System.out);
                    }
                    assertSame(ex, ex1.getCause());
                    UndeclaredThrowableException utex = (UndeclaredThrowableException) ex1;
                }
            }
        }
        // Test error checking on bad interfaces:
        for (Class<?> nonSMI : new Class<?>[] { Object.class,
                                             String.class,
                                             CharSequence.class,
                                             java.io.Serializable.class,
                                             PrivateRunnable.class,
                                             Example.class }) {
            if (verbosity > 2)  System.out.println(nonSMI.getName());
            try {
                countTest(false);
                MethodHandleProxies.asInterfaceInstance(nonSMI, varargsArray(0));
                assertTrue("Failed to throw on "+nonSMI.getName(), false);
            } catch (IllegalArgumentException ex) {
                if (verbosity > 2)  System.out.println(nonSMI.getSimpleName()+": "+ex);
                // Object: java.lang.IllegalArgumentException:
                //     not a public interface: java.lang.Object
                // String: java.lang.IllegalArgumentException:
                //     not a public interface: java.lang.String
                // CharSequence: java.lang.IllegalArgumentException:
                //     not a single-method interface: java.lang.CharSequence
                // Serializable: java.lang.IllegalArgumentException:
                //     not a single-method interface: java.io.Serializable
                // PrivateRunnable: java.lang.IllegalArgumentException:
                //     not a public interface: test.java.lang.invoke.MethodHandlesTest$PrivateRunnable
                // Example: java.lang.IllegalArgumentException:
                //     not a public interface: test.java.lang.invoke.MethodHandlesTest$Example
            }
        }
        // Test error checking on interfaces with the wrong method type:
        for (Class<?> intfc : new Class<?>[] { Runnable.class /*arity 0*/,
                                            Fooable.class /*arity 1 & 2*/ }) {
            int badArity = 1;  // known to be incompatible
            if (verbosity > 2)  System.out.println(intfc.getName());
            try {
                countTest(false);
                MethodHandleProxies.asInterfaceInstance(intfc, varargsArray(badArity));
                assertTrue("Failed to throw on "+intfc.getName(), false);
            } catch (WrongMethodTypeException ex) {
                if (verbosity > 2)  System.out.println(intfc.getSimpleName()+": "+ex);
                // Runnable: java.lang.invoke.WrongMethodTypeException:
                //     cannot convert MethodHandle(Object)Object[] to ()void
                // Fooable: java.lang.invoke.WrongMethodTypeException:
                //     cannot convert MethodHandle(Object)Object[] to (Object,String)Object
            }
        }
    }

    @Test
    public void testInterfaceCast() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testInterfaceCast0);
    }

    public void testInterfaceCast0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("interfaceCast");
        assert( (((Object)"foo") instanceof CharSequence));
        assert(!(((Object)"foo") instanceof Iterable));
        for (MethodHandle mh : new MethodHandle[]{
            MethodHandles.identity(String.class),
            MethodHandles.identity(CharSequence.class),
            MethodHandles.identity(Iterable.class)
        }) {
            if (verbosity > 0)  System.out.println("-- mh = "+mh);
            for (Class<?> ctype : new Class<?>[]{
                Object.class, String.class, CharSequence.class,
                Number.class, Iterable.class
            }) {
                if (verbosity > 0)  System.out.println("---- ctype = "+ctype.getName());
                //                           doret  docast
                testInterfaceCast(mh, ctype, false, false);
                testInterfaceCast(mh, ctype, true,  false);
                testInterfaceCast(mh, ctype, false, true);
                testInterfaceCast(mh, ctype, true,  true);
            }
        }
    }

    private static Class<?> i2o(Class<?> c) {
        return (c.isInterface() ? Object.class : c);
    }

    public void testInterfaceCast(MethodHandle mh, Class<?> ctype,
                                                   boolean doret, boolean docast) throws Throwable {
        MethodHandle mh0 = mh;
        if (verbosity > 1)
            System.out.println("mh="+mh+", ctype="+ctype.getName()+", doret="+doret+", docast="+docast);
        String normalRetVal = "normal return value";
        MethodType mt = mh.type();
        MethodType mt0 = mt;
        if (doret)  mt = mt.changeReturnType(ctype);
        else        mt = mt.changeParameterType(0, ctype);
        if (docast) mh = MethodHandles.explicitCastArguments(mh, mt);
        else        mh = mh.asType(mt);
        assertEquals(mt, mh.type());
        MethodType mt1 = mt;
        // this bit is needed to make the interface types disappear for invokeWithArguments:
        mh = MethodHandles.explicitCastArguments(mh, mt.generic());
        Class<?>[] step = {
            mt1.parameterType(0),  // param as passed to mh at first
            mt0.parameterType(0),  // param after incoming cast
            mt0.returnType(),      // return value before cast
            mt1.returnType(),      // return value after outgoing cast
        };
        // where might a checkCast occur?
        boolean[] checkCast = new boolean[step.length];
        // the string value must pass each step without causing an exception
        if (!docast) {
            if (!doret) {
                if (step[0] != step[1])
                    checkCast[1] = true;  // incoming value is cast
            } else {
                if (step[2] != step[3])
                    checkCast[3] = true;  // outgoing value is cast
            }
        }
        boolean expectFail = false;
        for (int i = 0; i < step.length; i++) {
            Class<?> c = step[i];
            if (!checkCast[i])  c = i2o(c);
            if (!c.isInstance(normalRetVal)) {
                if (verbosity > 3)
                    System.out.println("expect failure at step "+i+" in "+Arrays.toString(step)+Arrays.toString(checkCast));
                expectFail = true;
                break;
            }
        }
        countTest(!expectFail);
        if (verbosity > 2)
            System.out.println("expectFail="+expectFail+", mt="+mt);
        Object res;
        try {
            res = mh.invokeWithArguments(normalRetVal);
        } catch (Exception ex) {
            res = ex;
        }
        boolean sawFail = !(res instanceof String);
        if (sawFail != expectFail) {
            System.out.println("*** testInterfaceCast: mh0 = "+mh0);
            System.out.println("  retype using "+(docast ? "explicitCastArguments" : "asType")+" to "+mt+" => "+mh);
            System.out.println("  call returned "+res);
            System.out.println("  expected "+(expectFail ? "an exception" : normalRetVal));
        }
        if (!expectFail) {
            assertFalse(res.toString(), sawFail);
            assertEquals(normalRetVal, res);
        } else {
            assertTrue(res.toString(), sawFail);
        }
    }

    static Example userMethod(Object o, String s, int i) {
        called("userMethod", o, s, i);
        return null;
    }

    @Test
    public void testUserClassInSignature() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testUserClassInSignature0);
    }

    public void testUserClassInSignature0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("testUserClassInSignature");
        Lookup lookup = MethodHandles.lookup();
        String name; MethodType mt; MethodHandle mh;
        Object[] args;

        // Try a static method.
        name = "userMethod";
        mt = MethodType.methodType(Example.class, Object.class, String.class, int.class);
        mh = lookup.findStatic(lookup.lookupClass(), name, mt);
        assertEquals(mt, mh.type());
        assertEquals(Example.class, mh.type().returnType());
        args = randomArgs(mh.type().parameterArray());
        mh.invokeWithArguments(args);
        assertCalled(name, args);

        // Try a virtual method.
        name = "v2";
        mt = MethodType.methodType(Object.class, Object.class, int.class);
        mh = lookup.findVirtual(Example.class, name, mt);
        assertEquals(mt, mh.type().dropParameterTypes(0,1));
        assertTrue(mh.type().parameterList().contains(Example.class));
        args = randomArgs(mh.type().parameterArray());
        mh.invokeWithArguments(args);
        assertCalled(name, args);
    }

    static void runForRunnable() {
        called("runForRunnable");
    }

    public interface Fooable {
        // overloads:
        Object  foo(Object x, String y);
        List<?> foo(String x, int y);
        Object  foo(String x);
    }

    static Object fooForFooable(String x, Object... y) {
        return called("fooForFooable/"+x, y);
    }

    @SuppressWarnings("serial")  // not really a public API, just a test case
    public static class MyCheckedException extends Exception {
    }

    public interface WillThrow {
        void willThrow() throws MyCheckedException;
    }

    /*non-public*/ interface PrivateRunnable {
        public void run();
    }

    @Test
    public void testRunnableProxy() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testRunnableProxy0);
    }

    public void testRunnableProxy0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("testRunnableProxy");
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle run = lookup.findStatic(lookup.lookupClass(), "runForRunnable", MethodType.methodType(void.class));
        Runnable r = MethodHandleProxies.asInterfaceInstance(Runnable.class, run);
        testRunnableProxy(r);
        assertCalled("runForRunnable");
    }

    private static void testRunnableProxy(Runnable r) {
        //7058630: JSR 292 method handle proxy violates contract for Object methods
        r.run();
        Object o = r;
        r = null;
        boolean eq = (o == o);
        int     hc = System.identityHashCode(o);
        String  st = o.getClass().getName() + "@" + Integer.toHexString(hc);
        Object expect = Arrays.asList(st, eq, hc);
        if (verbosity >= 2)  System.out.println("expect st/eq/hc = "+expect);
        Object actual = Arrays.asList(o.toString(), o.equals(o), o.hashCode());
        if (verbosity >= 2)  System.out.println("actual st/eq/hc = "+actual);
        assertEquals(expect, actual);
    }
}
