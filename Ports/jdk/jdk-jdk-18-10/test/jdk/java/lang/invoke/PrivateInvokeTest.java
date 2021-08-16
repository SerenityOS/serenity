/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary white-box testing of method handle sub-primitives
 * @modules java.base/java.lang.invoke:open
 * @run junit test.java.lang.invoke.PrivateInvokeTest
 */

package test.java.lang.invoke;

import java.lang.invoke.*;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.*;
import static org.junit.Assert.*;

public class PrivateInvokeTest {
    // Utility functions
    private static final Lookup LOOKUP = lookup();
    private static final Class<?> THIS_CLASS = PrivateInvokeTest.class;
    private static final int
            REF_NONE                    = 0,  // null value
            REF_getField                = 1,
            REF_getStatic               = 2,
            REF_putField                = 3,
            REF_putStatic               = 4,
            REF_invokeVirtual           = 5,
            REF_invokeStatic            = 6,
            REF_invokeSpecial           = 7,
            REF_newInvokeSpecial        = 8,
            REF_invokeInterface         = 9,
            REF_LIMIT                  = 10,
            REF_MH_invokeBasic         = REF_NONE;;
    private static final String[] REF_KIND_NAMES = {
        "MH::invokeBasic",
        "REF_getField", "REF_getStatic", "REF_putField", "REF_putStatic",
        "REF_invokeVirtual", "REF_invokeStatic", "REF_invokeSpecial",
        "REF_newInvokeSpecial", "REF_invokeInterface"
    };
    private int verbose;
    //{ verbose = 99; }  // for debugging
    {
        String vstr = System.getProperty(THIS_CLASS.getSimpleName()+".verbose");
        if (vstr == null)
            vstr = System.getProperty(THIS_CLASS.getName()+".verbose");
        if (vstr == null)
            vstr = System.getProperty("test.verbose");
        if (vstr != null)  verbose = Integer.parseInt(vstr);
    }
    private static int referenceKind(Method m) {
        if (Modifier.isStatic(m.getModifiers()))
            return REF_invokeStatic;
        else if (m.getDeclaringClass().isInterface())
            return REF_invokeInterface;
        else if (Modifier.isFinal(m.getModifiers()) ||
            Modifier.isFinal(m.getDeclaringClass().getModifiers()))
            return REF_invokeSpecial;
        else
            return REF_invokeVirtual;
    }
    private static MethodType basicType(MethodType mtype) {
        MethodType btype = mtype.erase();
        if (btype.hasPrimitives()) {
            for (int i = -1; i < mtype.parameterCount(); i++) {
                Class<?> type = (i < 0 ? mtype.returnType() : mtype.parameterType(i));
                if (type == boolean.class ||
                    type == byte.class ||
                    type == char.class ||
                    type == short.class) {
                    type = int.class;
                    if (i < 0)
                        btype = btype.changeReturnType(type);
                    else
                        btype = btype.changeParameterType(i, type);
                }
            }
        }
        return btype;
    }
    private static Method getMethod(Class<?> defc, String name, Class<?>... ptypes) {
        try {
            return defc.getDeclaredMethod(name, ptypes);
        } catch (NoSuchMethodException ex) {
        }
        try {
            return defc.getMethod(name, ptypes);
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    private static MethodHandle unreflect(Method m) {
        try {
            MethodHandle mh = LOOKUP.unreflect(m);
            if (Modifier.isTransient(m.getModifiers()))
                mh = mh.asFixedArity();  // remove varargs wrapper
            return mh;
        } catch (IllegalAccessException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    private static final Lookup DIRECT_INVOKER_LOOKUP;
    private static final Class<?> MEMBER_NAME_CLASS;
    private static final MethodHandle MH_INTERNAL_MEMBER_NAME;
    private static final MethodHandle MH_DEBUG_STRING;
    static {
        try {
            // This is white box testing.  Use reflection to grab private implementation bits.
            String magicName = "IMPL_LOOKUP";
            Field magicLookup = MethodHandles.Lookup.class.getDeclaredField(magicName);
            // This unit test will fail if a security manager is installed.
            magicLookup.setAccessible(true);
            // Forbidden fruit...
            DIRECT_INVOKER_LOOKUP = (Lookup) magicLookup.get(null);
            MEMBER_NAME_CLASS = Class.forName("java.lang.invoke.MemberName", false, MethodHandle.class.getClassLoader());
            MH_INTERNAL_MEMBER_NAME = DIRECT_INVOKER_LOOKUP
                    .findVirtual(MethodHandle.class, "internalMemberName", methodType(MEMBER_NAME_CLASS))
                    .asType(methodType(Object.class, MethodHandle.class));
            MH_DEBUG_STRING = DIRECT_INVOKER_LOOKUP
                    .findVirtual(MethodHandle.class, "debugString", methodType(String.class));
        } catch (ReflectiveOperationException ex) {
            throw new Error(ex);
        }
    }
    private Object internalMemberName(MethodHandle mh) {
        try {
            return MH_INTERNAL_MEMBER_NAME.invokeExact(mh);
        } catch (Throwable ex) {
            throw new Error(ex);
        }
    }
    private String debugString(MethodHandle mh) {
        try {
            return (String) MH_DEBUG_STRING.invokeExact(mh);
        } catch (Throwable ex) {
            throw new Error(ex);
        }
    }
    private static MethodHandle directInvoker(int refKind, MethodType mtype) {
        return directInvoker(REF_KIND_NAMES[refKind], mtype);
    }
    private static MethodHandle directInvoker(String name, MethodType mtype) {
        boolean isStatic;
        mtype = mtype.erase();
        if (name.startsWith("MH::")) {
            isStatic = false;
            name = strip("MH::", name);
        } else if (name.startsWith("REF_")) {
            isStatic = true;
            name = strip("REF_", name);
            if (name.startsWith("invoke"))
                name = "linkTo"+strip("invoke", name);
            mtype = mtype.appendParameterTypes(MEMBER_NAME_CLASS);
        } else {
            throw new AssertionError("name="+name);
        }
        //System.out.println("directInvoker = "+name+mtype);
        try {
            if (isStatic)
                return DIRECT_INVOKER_LOOKUP
                        .findStatic(MethodHandle.class, name, mtype);
            else
                return DIRECT_INVOKER_LOOKUP
                        .findVirtual(MethodHandle.class, name, mtype);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    private Object invokeWithArguments(Method m, Object... args) {
        Object recv = null;
        if (!Modifier.isStatic(m.getModifiers())) {
            recv = args[0];
            args = pop(1, args);
        }
        try {
            return m.invoke(recv, args);
        } catch (IllegalAccessException|IllegalArgumentException|InvocationTargetException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    private Object invokeWithArguments(MethodHandle mh, Object... args) {
        try {
            return mh.invokeWithArguments(args);
        } catch (Throwable ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    private int counter;
    private Object makeArgument(Class<?> type) {
        final String cname = type.getSimpleName();
        final int n = ++counter;
        final int nn = (n << 10) + 13;
        if (type.isAssignableFrom(String.class)) {
            return "<"+cname+"#"+nn+">";
        }
        if (type == THIS_CLASS)  return this.withCounter(nn);
        if (type == Integer.class   || type == int.class)     return nn;
        if (type == Character.class || type == char.class)    return (char)(n % 100+' ');
        if (type == Byte.class      || type == byte.class)    return (byte)-(n % 100);
        if (type == Long.class      || type == long.class)    return (long)nn;
        throw new IllegalArgumentException("don't know how to make argument of type: "+type);
    }
    private Object[] makeArguments(Class<?>... ptypes) {
        Object[] args = new Object[ptypes.length];
        for (int i = 0; i < args.length; i++)
            args[i] = makeArgument(ptypes[i]);
        return args;
    }
    private Object[] makeArguments(MethodType mtype) {
        return makeArguments(mtype.parameterArray());
    }
    private Object[] pop(int n, Object[] args) {
        if (n >= 0)
            return Arrays.copyOfRange(args, n, args.length);
        else
            return Arrays.copyOfRange(args, 0, args.length+n);
    }
    private Object[] pushAtFront(Object arg1, Object[] args) {
        Object[] res = new Object[1+args.length];
        res[0] = arg1;
        System.arraycopy(args, 0, res, 1, args.length);
        return res;
    }
    private Object[] pushAtBack(Object[] args, Object argN) {
        Object[] res = new Object[1+args.length];
        System.arraycopy(args, 0, res, 0, args.length);
        res[args.length] = argN;
        return res;
    }
    private static String strip(String prefix, String s) {
        assert(s.startsWith(prefix));
        return s.substring(prefix.length());
    }

    private final int[] refKindTestCounts = new int[REF_KIND_NAMES.length];
    @After
    public void printCounts() {
        ArrayList<String> zeroes = new ArrayList<>();
        for (int i = 0; i < refKindTestCounts.length; i++) {
            final int count = refKindTestCounts[i];
            final String name = REF_KIND_NAMES[i];
            if (count == 0) {
                if (name != null)  zeroes.add(name);
                continue;
            }
            if (verbose >= 0)
                System.out.println("test count for "+name+" : "+count);
            else if (name != null)
                zeroes.add(name);
        }
        if (verbose >= 0)
            System.out.println("test counts zero for "+zeroes);
    }

    // Test subjects
    public static String makeString(Object x) { return "makeString("+x+")"; }
    public static String dupString(String x) { return "("+x+"+"+x+")"; }
    public static String intString(int x) { return "intString("+x+")"; }
    public static String byteString(byte x) { return "byteString("+x+")"; }
    public static String longString(String x, long y, String z) { return "longString("+x+y+z+")"; }

    public final String toString() {
        return "<"+getClass().getSimpleName()+"#"+counter+">";
    }
    public final String hello() { return "hello from "+this; }
    private PrivateInvokeTest withCounter(int counter) {
        PrivateInvokeTest res = new PrivateInvokeTest();
        res.counter = counter;
        return res;
    }

    public static void main(String... av) throws Throwable {
        new PrivateInvokeTest().run();
    }
    public void run() throws Throwable {
        testFirst();
        testInvokeDirect();
    }

    @Test
    public void testFirst() throws Throwable {
        if (true)  return;  // nothing here
        try {
            System.out.println("start of testFirst");
        } finally {
            System.out.println("end of testFirst");
        }
    }

    @Test
    public void testInvokeDirect() {
        testInvokeDirect(getMethod(THIS_CLASS, "hello"));
        testInvokeDirect(getMethod(Object.class, "toString"));
        testInvokeDirect(getMethod(Comparable.class, "compareTo", Object.class));
        testInvokeDirect(getMethod(THIS_CLASS, "makeString", Object.class));
        testInvokeDirect(getMethod(THIS_CLASS, "dupString", String.class));
        testInvokeDirect(getMethod(THIS_CLASS, "intString", int.class));
        testInvokeDirect(getMethod(THIS_CLASS, "byteString", byte.class));
        testInvokeDirect(getMethod(THIS_CLASS, "longString", String.class, long.class, String.class));
    }

    void testInvokeDirect(Method m) {
        final int refKind = referenceKind(m);
        testInvokeDirect(m, refKind);
        testInvokeDirect(m, REF_MH_invokeBasic);
    }
    void testInvokeDirect(Method m, int refKind) {
        if (verbose >= 1)
            System.out.println("testInvoke m="+m+" : "+REF_KIND_NAMES[refKind]);
        final MethodHandle mh = unreflect(m);
        Object[] args = makeArguments(mh.type());
        Object res1 = invokeWithArguments(m, args);
        // res1 comes from java.lang.reflect.Method::invoke
        if (verbose >= 1)
            System.out.println("m"+Arrays.asList(args)+" => "+res1);
        // res2 comes from java.lang.invoke.MethodHandle::invoke
        Object res2 = invokeWithArguments(mh, args);
        assertEquals(res1, res2);
        MethodType mtype = mh.type();
        testInvokeVia("DMH invoker", refKind, directInvoker(refKind, mtype), mh, res1, args);
        MethodType etype = mtype.erase();
        if (etype != mtype) {
            // Try a detuned invoker.
            testInvokeVia("erased DMH invoker", refKind, directInvoker(refKind, etype), mh, res1, args);
        }
        MethodType btype = basicType(mtype);
        if (btype != mtype && btype != etype) {
            // Try a detuned invoker.
            testInvokeVia("basic DMH invoker", refKind, directInvoker(refKind, btype), mh, res1, args);
        }
        if (false) {
            // this can crash the JVM
            testInvokeVia("generic DMH invoker", refKind, directInvoker(refKind, mtype.generic()), mh, res1, args);
        }
        refKindTestCounts[refKind] += 1;
    }

    void testInvokeVia(String kind, int refKind, MethodHandle invoker, MethodHandle mh, Object res1, Object... args) {
        Object[] args1;
        if (refKind == REF_MH_invokeBasic)
            args1 = pushAtFront(mh, args);
        else
            args1 = pushAtBack(args, internalMemberName(mh));
        if (verbose >= 2) {
            System.out.println(kind+" invoker="+invoker+" mh="+debugString(mh)+" args="+Arrays.asList(args1));
        }
        Object res3 = invokeWithArguments(invoker, args1);
        assertEquals(res1, res3);
    }
}
