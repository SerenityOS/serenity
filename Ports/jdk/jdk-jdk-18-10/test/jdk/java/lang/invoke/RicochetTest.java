/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary unit tests for recursive method handles
 * @run junit/othervm -DRicochetTest.MAX_ARITY=255 test.java.lang.invoke.RicochetTest
 */

package test.java.lang.invoke;

import java.lang.invoke.*;
import java.util.*;
import org.junit.*;
import static java.lang.invoke.MethodType.*;
import static java.lang.invoke.MethodHandles.*;
import static org.junit.Assert.*;


/**
 *
 * @author jrose
 */
public class RicochetTest {
    private static final Class<?> CLASS = RicochetTest.class;
    private static final int MAX_ARITY = Integer.getInteger(CLASS.getSimpleName()+".MAX_ARITY", 40);

    public static void main(String... av) throws Throwable {
        RicochetTest test = new RicochetTest();
        if (av.length > 0)  test.testOnly = Arrays.asList(av).toString();
        if (REPEAT == 1 || test.testOnly != null) {
            test.testAll();
            if (test.testOnlyTests == null)  throw new RuntimeException("no matching test: "+test.testOnly);
        } else if (REPEAT == 0) {
            org.junit.runner.JUnitCore.runClasses(RicochetTest.class);
        } else {
            verbose(1, "REPEAT="+REPEAT);
            for (int i = 0; i < REPEAT; i++) {
                test.testRepetition = (i+1);
                verbose(0, "[#"+test.testRepetition+"]");
                test.testAll();
            }
        }
    }
    int testRepetition;

    public void testAll() throws Throwable {
        testNull();
        testBoxInteger();
        testFilterReturnValue();
        testFilterObject();
        testBoxLong();
        testFilterInteger();
        testIntSpreads();
        testByteSpreads();
        testLongSpreads();
        testIntCollects();
        testReturns();
        testRecursion();
    }

    @Test
    public void testNull() throws Throwable {
        if (testRepetition > (1+REPEAT/100))  return;  // trivial test
        if (!startTest("testNull"))  return;
        assertEquals(opI(37), opI.invokeWithArguments(37));
        assertEqualFunction(opI, opI);
    }

    @Test
    public void testBoxInteger() throws Throwable {
        if (!startTest("testBoxInteger"))  return;
        assertEqualFunction(opI, opI.asType(opL_I.type()).asType(opI.type()));
    }

    @Test
    public void testFilterReturnValue() throws Throwable {
        if (!startTest("testFilterReturnValue"))  return;
        int[] ints = { 12, 23, 34, 45, 56, 67, 78, 89 };
        Object res = list8ints.invokeExact(ints[0], ints[1], ints[2], ints[3], ints[4], ints[5], ints[6], ints[7]);
        assertEquals(Arrays.toString(ints), res.toString());
        MethodHandle idreturn = filterReturnValue(list8ints, identity(Object.class));
        res = idreturn.invokeExact(ints[0], ints[1], ints[2], ints[3], ints[4], ints[5], ints[6], ints[7]);
        assertEquals(Arrays.toString(ints), res.toString());
        MethodHandle add0 = addL.bindTo(0);
        assertEqualFunction(filterReturnValue(opL2, add0), opL2);
    }

    @Test
    public void testFilterObject() throws Throwable {
        if (!startTest("testFilterObject"))  return;
        MethodHandle add0 = addL.bindTo(0);
        assertEqualFunction(sequence(opL2, add0), opL2);
        int bump13 = -13;  // value near 20 works as long as test values are near [-80..80]
        MethodHandle add13   = addL.bindTo(bump13);
        MethodHandle add13_0 = addL.bindTo(opI2(bump13, 0));
        MethodHandle add13_1 = addL.bindTo(opI2(0, bump13));
        assertEqualFunction(sequence(opL2, add13_0),
                            filterArguments(opL2, 0, add13));
        assertEqualFunction(sequence(opL2, add13_1),
                            filterArguments(opL2, 1, add13));
        System.out.println("[testFilterObject done]");
    }

    @Test
    public void testBoxLong() throws Throwable {
        if (!startTest("testBoxLong"))  return;
        assertEqualFunction(opJ, opJ.asType(opL_J.type()).asType(opJ.type()));
    }

    @Test
    public void testFilterInteger() throws Throwable {
        if (!startTest("testFilterInteger"))  return;
        assertEqualFunction(opI, sequence(convI_L, opL_I));
    }

    @Test
    public void testIntSpreads() throws Throwable {
        if (!startTest("testIntSpreads"))  return;
        MethodHandle id = identity(int[].class);
        final int MAX = MAX_ARITY-2;  // 253+1 would cause parameter overflow with 'this' added
        for (int nargs = 0; nargs <= MAX; nargs++) {
            if (nargs > 30 && nargs < MAX-20)  nargs += 10;
            int[] args = new int[nargs];
            for (int j = 0; j < args.length; j++)  args[j] = j + 11;
            //System.out.println("testIntSpreads "+Arrays.toString(args));
            int[] args1 = (int[]) id.invokeExact(args);
            assertArrayEquals(args, args1);
            MethodHandle coll = id.asCollector(int[].class, nargs);
            int[] args2 = args;
            switch (nargs) {
                case 0:  args2 = (int[]) coll.invokeExact(); break;
                case 1:  args2 = (int[]) coll.invokeExact(args[0]); break;
                case 2:  args2 = (int[]) coll.invokeExact(args[0], args[1]); break;
                case 3:  args2 = (int[]) coll.invokeExact(args[0], args[1], args[2]); break;
                case 4:  args2 = (int[]) coll.invokeExact(args[0], args[1], args[2], args[3]); break;
                case 5:  args2 = (int[]) coll.invokeExact(args[0], args[1], args[2], args[3], args[4]); break;
            }
            assertArrayEquals(args, args2);
            MethodHandle mh = coll.asSpreader(int[].class, nargs);
            int[] args3 = (int[]) mh.invokeExact(args);
            assertArrayEquals(args, args3);
        }
    }

    @Test
    public void testByteSpreads() throws Throwable {
        if (!startTest("testByteSpreads"))  return;
        MethodHandle id = identity(byte[].class);
        final int MAX = MAX_ARITY-2;  // 253+1 would cause parameter overflow with 'this' added
        for (int nargs = 0; nargs <= MAX; nargs++) {
            if (nargs > 30 && nargs < MAX-20)  nargs += 10;
            byte[] args = new byte[nargs];
            for (int j = 0; j < args.length; j++)  args[j] = (byte)(j + 11);
            //System.out.println("testByteSpreads "+Arrays.toString(args));
            byte[] args1 = (byte[]) id.invokeExact(args);
            assertArrayEquals(args, args1);
            MethodHandle coll = id.asCollector(byte[].class, nargs);
            byte[] args2 = args;
            switch (nargs) {
                case 0:  args2 = (byte[]) coll.invokeExact(); break;
                case 1:  args2 = (byte[]) coll.invokeExact(args[0]); break;
                case 2:  args2 = (byte[]) coll.invokeExact(args[0], args[1]); break;
                case 3:  args2 = (byte[]) coll.invokeExact(args[0], args[1], args[2]); break;
                case 4:  args2 = (byte[]) coll.invokeExact(args[0], args[1], args[2], args[3]); break;
                case 5:  args2 = (byte[]) coll.invokeExact(args[0], args[1], args[2], args[3], args[4]); break;
            }
            assertArrayEquals(args, args2);
            MethodHandle mh = coll.asSpreader(byte[].class, nargs);
            byte[] args3 = (byte[]) mh.invokeExact(args);
            assertArrayEquals(args, args3);
        }
    }

    @Test
    public void testLongSpreads() throws Throwable {
        if (!startTest("testLongSpreads"))  return;
        MethodHandle id = identity(long[].class);
        final int MAX = (MAX_ARITY - 2) / 2;  // 253/2+1 would cause parameter overflow with 'this' added
        for (int nargs = 0; nargs <= MAX; nargs++) {
            if (nargs > 30 && nargs < MAX-20)  nargs += 10;
            long[] args = new long[nargs];
            for (int j = 0; j < args.length; j++)  args[j] = (long)(j + 11);
            //System.out.println("testLongSpreads "+Arrays.toString(args));
            long[] args1 = (long[]) id.invokeExact(args);
            assertArrayEquals(args, args1);
            MethodHandle coll = id.asCollector(long[].class, nargs);
            long[] args2 = args;
            switch (nargs) {
                case 0:  args2 = (long[]) coll.invokeExact(); break;
                case 1:  args2 = (long[]) coll.invokeExact(args[0]); break;
                case 2:  args2 = (long[]) coll.invokeExact(args[0], args[1]); break;
                case 3:  args2 = (long[]) coll.invokeExact(args[0], args[1], args[2]); break;
                case 4:  args2 = (long[]) coll.invokeExact(args[0], args[1], args[2], args[3]); break;
                case 5:  args2 = (long[]) coll.invokeExact(args[0], args[1], args[2], args[3], args[4]); break;
            }
            assertArrayEquals(args, args2);
            MethodHandle mh = coll.asSpreader(long[].class, nargs);
            long[] args3 = (long[]) mh.invokeExact(args);
            assertArrayEquals(args, args3);
        }
    }

    @Test
    public void testIntCollects() throws Throwable {
        if (!startTest("testIntCollects"))  return;
        for (MethodHandle lister : INT_LISTERS) {
            int outputs = lister.type().parameterCount();
            for (int collects = 0; collects <= Math.min(outputs, INT_COLLECTORS.length-1); collects++) {
                int inputs = outputs - 1 + collects;
                if (inputs < 0)  continue;
                for (int pos = 0; pos + collects <= inputs; pos++) {
                    MethodHandle collector = INT_COLLECTORS[collects];
                    int[] args = new int[inputs];
                    int ap = 0, arg = 31;
                    for (int i = 0; i < pos; i++)
                        args[ap++] = arg++ + 0;
                    for (int i = 0; i < collects; i++)
                        args[ap++] = arg++ + 10;
                    while (ap < args.length)
                        args[ap++] = arg++ + 20;
                    // calculate piecemeal:
                    //System.out.println("testIntCollects "+Arrays.asList(lister, pos, collector)+" on "+Arrays.toString(args));
                    int[] collargs = Arrays.copyOfRange(args, pos, pos+collects);
                    int coll = (int) collector.asSpreader(int[].class, collargs.length).invokeExact(collargs);
                    int[] listargs = Arrays.copyOfRange(args, 0, outputs);
                    System.arraycopy(args, pos+collects, listargs, pos+1, outputs - (pos+1));
                    listargs[pos] = coll;
                    //System.out.println("  coll="+coll+" listargs="+Arrays.toString(listargs));
                    Object expect = lister.asSpreader(int[].class, listargs.length).invokeExact(listargs);
                    //System.out.println("  expect="+expect);

                    // now use the combined MH, and test the output:
                    MethodHandle mh = collectArguments(lister, pos, int[].class, INT_COLLECTORS[collects]);
                    if (mh == null)  continue;  // no infix collection, yet
                    assert(mh.type().parameterCount() == inputs);
                    Object observe = mh.asSpreader(int[].class, args.length).invokeExact(args);
                    assertEquals(expect, observe);
                }
            }
        }
    }

    @Test
    public void testByteCollects() throws Throwable {
        if (!startTest("testByteCollects"))  return;
        for (MethodHandle lister : BYTE_LISTERS) {
            int outputs = lister.type().parameterCount();
            for (int collects = 0; collects <= Math.min(outputs, BYTE_COLLECTORS.length-1); collects++) {
                int inputs = outputs - 1 + collects;
                if (inputs < 0)  continue;
                for (int pos = 0; pos + collects <= inputs; pos++) {
                    MethodHandle collector = BYTE_COLLECTORS[collects];
                    byte[] args = new byte[inputs];
                    int ap = 0, arg = 31;
                    for (int i = 0; i < pos; i++)
                        args[ap++] = (byte)(arg++ + 0);
                    for (int i = 0; i < collects; i++)
                        args[ap++] = (byte)(arg++ + 10);
                    while (ap < args.length)
                        args[ap++] = (byte)(arg++ + 20);
                    // calculate piecemeal:
                    //System.out.println("testIntCollects "+Arrays.asList(lister, pos, collector)+" on "+Arrays.toString(args));
                    byte[] collargs = Arrays.copyOfRange(args, pos, pos+collects);
                    byte coll = (byte) collector.asSpreader(byte[].class, collargs.length).invokeExact(collargs);
                    byte[] listargs = Arrays.copyOfRange(args, 0, outputs);
                    System.arraycopy(args, pos+collects, listargs, pos+1, outputs - (pos+1));
                    listargs[pos] = coll;
                    //System.out.println("  coll="+coll+" listargs="+Arrays.toString(listargs));
                    Object expect = lister.asSpreader(byte[].class, listargs.length).invokeExact(listargs);
                    //System.out.println("  expect="+expect);

                    // now use the combined MH, and test the output:
                    MethodHandle mh = collectArguments(lister, pos, byte[].class, BYTE_COLLECTORS[collects]);
                    if (mh == null)  continue;  // no infix collection, yet
                    assert(mh.type().parameterCount() == inputs);
                    Object observe = mh.asSpreader(byte[].class, args.length).invokeExact(args);
                    assertEquals(expect, observe);
                }
            }
        }
    }

    private static MethodHandle collectArguments(MethodHandle lister, int pos, Class<?> array, MethodHandle collector) {
        int collects = collector.type().parameterCount();
        int outputs = lister.type().parameterCount();
        if (pos == outputs - 1)
            return MethodHandles.filterArguments(lister, pos,
                        collector.asSpreader(array, collects))
                            .asCollector(array, collects);
        //return MethodHandles.collectArguments(lister, pos, collector); //no such animal
        return null;
    }

    private static final Class<?>[] RETURN_TYPES = {
        Object.class, String.class, Integer.class,
        int.class, long.class,
        boolean.class, byte.class, char.class, short.class,
        float.class, double.class,
        void.class,
    };

    @Test
    public void testReturns() throws Throwable {
        if (!startTest("testReturns"))  return;
        // fault injection:
        int faultCount = 0;  // total of 1296 tests
        faultCount = Integer.getInteger("testReturns.faultCount", 0);
        for (Class<?> ret : RETURN_TYPES) {
            // make a complicated identity function and pass something through it
            System.out.println(ret.getSimpleName());
            Class<?> vret = (ret == void.class) ? Void.class : ret;
            MethodHandle id = // (vret)->ret
                identity(vret).asType(methodType(ret, vret));
            final int LENGTH = 4;
            int[] index = {0};
            Object vals = java.lang.reflect.Array.newInstance(vret, LENGTH);
            MethodHandle indexGetter =  //()->int
                insertArguments(arrayElementGetter(index.getClass()), 0, index, 0);
            MethodHandle valSelector =  // (int)->vret
                arrayElementGetter(vals.getClass()).bindTo(vals);
            MethodHandle valGetter =  // ()->vret
                foldArguments(valSelector, indexGetter);
            if (ret != void.class) {
                for (int i = 0; i < LENGTH; i++) {
                    Object val = (i + 50);
                    if (ret == boolean.class)  val = (i % 3 == 0);
                    if (ret == String.class)   val = "#"+i;
                    if (ret == char.class)     val = (char)('a'+i);
                    if (ret == byte.class)     val = (byte)~i;
                    if (ret == short.class)    val = (short)(1<<i);
                    java.lang.reflect.Array.set(vals, i, val);
                }
            }
            for (int i = 0; i < LENGTH; i++) {
                Object val = java.lang.reflect.Array.get(vals, i);
                System.out.println(i+" => "+val);
                index[0] = i;
                if (--faultCount == 0)  index[0] ^= 1;
                Object x = valGetter.invokeWithArguments();
                assertEquals(val, x);
                // make a return-filter call:  x = id(valGetter())
                if (--faultCount == 0)  index[0] ^= 1;
                x = filterReturnValue(valGetter, id).invokeWithArguments();
                assertEquals(val, x);
                // make a filter call:  x = id(*,valGetter(),*)
                for (int len = 1; len <= 4; len++) {
                    for (int pos = 0; pos < len; pos++) {
                        MethodHandle proj = id;  // lambda(..., vret x,...){x}
                        for (int j = 0; j < len; j++) {
                            if (j == pos)  continue;
                            proj = dropArguments(proj, j, Object.class);
                        }
                        assert(proj.type().parameterCount() == len);
                        // proj: (Object*, pos: vret, Object*)->ret
                        assertEquals(vret, proj.type().parameterType(pos));
                        MethodHandle vgFilter = dropArguments(valGetter, 0, Object.class);
                        if (--faultCount == 0)  index[0] ^= 1;
                        x = filterArguments(proj, pos, vgFilter).invokeWithArguments(new Object[len]);
                        assertEquals(val, x);
                    }
                }
                // make a fold call:
                for (int len = 0; len <= 4; len++) {
                    for (int fold = 0; fold <= len; fold++) {
                        MethodHandle proj = id;  // lambda(ret x, ...){x}
                        if (ret == void.class)  proj = constant(Object.class, null);
                        int arg0 = (ret == void.class ? 0 : 1);
                        for (int j = 0; j < len; j++) {
                            proj = dropArguments(proj, arg0, Object.class);
                        }
                        assert(proj.type().parameterCount() == arg0 + len);
                        // proj: (Object*, pos: vret, Object*)->ret
                        if (arg0 != 0)  assertEquals(vret, proj.type().parameterType(0));
                        MethodHandle vgFilter = valGetter.asType(methodType(ret));
                        for (int j = 0; j < fold; j++) {
                            vgFilter = dropArguments(vgFilter, j, Object.class);
                        }
                        x = foldArguments(proj, vgFilter).invokeWithArguments(new Object[len]);
                        if (--faultCount == 0)  index[0] ^= 1;
                        assertEquals(val, x);
                    }
                }
            }
        }
        //System.out.println("faultCount="+faultCount);
    }

    @Test
    public void testRecursion() throws Throwable {
        if (!startTest("testRecursion"))  return;
        final int LIMIT = 10;
        for (int i = 0; i < LIMIT; i++) {
            RFCB rfcb = new RFCB(i);
            Object x = "x", y = "y";
            Object result = rfcb.recursiveFunction(x, y);
            verbose(1, result);
        }
    }
    /** Recursive Function Control Block */
    private static class RFCB {
        java.util.Random random;
        final MethodHandle[] fns;
        int depth;
        @SuppressWarnings("LeakingThisInConstructor")
        RFCB(int seed) throws Throwable {
            this.random = new java.util.Random(seed);
            this.fns = new MethodHandle[Math.max(29, (1 << MAX_DEPTH-2)/3)];
            java.util.Arrays.fill(fns, lookup().bind(this, "recursiveFunction", genericMethodType(2)));
            for (int i = 5; i < fns.length; i++) {
                switch (i % 4) {
                case 0: fns[i] = filterArguments(fns[i - 5], 0, insertArguments(fns[i - 4], 1, ".")); break;
                case 1: fns[i] = filterArguments(fns[i - 5], 1, insertArguments(fns[i - 3], 1, ".")); break;
                case 2: fns[i] = filterReturnValue(fns[i - 5], insertArguments(fns[i - 2], 1, ".")); break;
                }
            }
        }
        Object recursiveFunction(Object x, Object y) throws Throwable {
            depth++;
            try {
                final int ACTION_COUNT = 11;
                switch (random.nextInt(ACTION_COUNT)) {
                case 1:
                    Throwable ex = new RuntimeException();
                    ex.fillInStackTrace();
                    if (VERBOSITY >= 2) ex.printStackTrace(System.out);
                    x = "ST; " + x;
                    break;
                case 2:
                    System.gc();
                    x = "GC; " + x;
                    break;
                }
                boolean isLeaf = (depth >= MAX_DEPTH);
                if (isLeaf) {
                    return Arrays.asList(x, y).toString();
                }
                return fns[random.nextInt(fns.length)].invokeExact(x, y);
            } finally {
                depth--;
            }
        }
    }

    private static MethodHandle sequence(MethodHandle mh1, MethodHandle... mhs) {
        MethodHandle res = mh1;
        for (MethodHandle mh2 : mhs)
            res = filterReturnValue(res, mh2);
        return res;
    }
    private static void assertEqualFunction(MethodHandle x, MethodHandle y) throws Throwable {
        assertEquals(x.type(), y.type()); //??
        MethodType t = x.type();
        if (t.parameterCount() == 0) {
            assertEqualFunctionAt(null, x, y);
            return;
        }
        Class<?> ptype = t.parameterType(0);
        if (ptype == long.class || ptype == Long.class) {
            for (long i = -10; i <= 10; i++) {
                assertEqualFunctionAt(i, x, y);
            }
        } else {
            for (int i = -10; i <= 10; i++) {
                assertEqualFunctionAt(i, x, y);
            }
        }
    }
    private static void assertEqualFunctionAt(Object v, MethodHandle x, MethodHandle y) throws Throwable {
        Object[] args = new Object[x.type().parameterCount()];
        Arrays.fill(args, v);
        Object xval = invokeWithCatch(x, args);
        Object yval = invokeWithCatch(y, args);
        String msg = "ok";
        if (!Objects.equals(xval, yval)) {
            msg = ("applying "+x+" & "+y+" to "+v);
        }
        assertEquals(msg, xval, yval);
    }
    private static Object invokeWithCatch(MethodHandle mh, Object... args) throws Throwable {
        try {
            return mh.invokeWithArguments(args);
        } catch (Throwable ex) {
            System.out.println("threw: "+mh+Arrays.asList(args));
            ex.printStackTrace(System.out);
            return ex;
        }
    }

    private static final Lookup LOOKUP = lookup();
    private static MethodHandle findStatic(String name,
                                           Class<?> rtype,
                                           Class<?>... ptypes) {
        try {
            return LOOKUP.findStatic(LOOKUP.lookupClass(), name, methodType(rtype, ptypes));
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
    private static MethodHandle findStatic(String name,
                                           Class<?> rtype,
                                           List<?> ptypes) {
        return findStatic(name, rtype, ptypes.toArray(new Class<?>[ptypes.size()]));
    }
    static int getProperty(String name, int dflt) {
        String qual = LOOKUP.lookupClass().getName();
        String prop = System.getProperty(qual+"."+name);
        if (prop == null)  prop = System.getProperty(name);
        if (prop == null)  return dflt;
        return Integer.parseInt(prop);
    }

    private static int opI(int... xs) {
        stress();
        int base = 100;
        int z = 0;
        for (int x : xs) {
            z = (z * base) + (x % base);
        }
        verbose("opI", xs.length, xs, z);
        return z;
    }
    private static int opI2(int x, int y) { return opI(x, y); }  // x*100 + y%100
    private static int opI3(int x, int y, int z) { return opI(x, y, z); }
    private static int opI4(int w, int x, int y, int z) { return opI(w, x, y, z); }
    private static int opI(int x) { return opI2(x, 37); }
    private static Object opI_L(int x) { return (Object) opI(x); }
    private static long opJ3(long x, long y, long z) { return (long) opI3((int)x, (int)y, (int)z); }
    private static long opJ2(long x, long y) { return (long) opI2((int)x, (int)y); }
    private static long opJ(long x) { return (long) opI((int)x); }
    private static Object opL2(Object x, Object y) { return (Object) opI2((int)x, (int)y); }
    private static Object opL(Object x) { return (Object) opI((int)x); }
    private static int opL2_I(Object x, Object y) { return opI2((int)x, (int)y); }
    private static int opL_I(Object x) { return opI((int)x); }
    private static long opL_J(Object x) { return (long) opI((int)x); }
    private static final MethodHandle opI, opI2, opI3, opI4, opI_L, opJ, opJ2, opJ3, opL2, opL, opL2_I, opL_I, opL_J;
    static {
        opI4 = findStatic("opI4", int.class, int.class, int.class, int.class, int.class);
        opI3 = findStatic("opI3", int.class, int.class, int.class, int.class);
        opI2 = findStatic("opI2", int.class, int.class, int.class);
        opI = findStatic("opI", int.class, int.class);
        opI_L = findStatic("opI_L", Object.class, int.class);
        opJ = findStatic("opJ", long.class, long.class);
        opJ2 = findStatic("opJ2", long.class, long.class, long.class);
        opJ3 = findStatic("opJ3", long.class, long.class, long.class, long.class);
        opL2 = findStatic("opL2", Object.class, Object.class, Object.class);
        opL = findStatic("opL", Object.class, Object.class);
        opL2_I = findStatic("opL2_I", int.class, Object.class, Object.class);
        opL_I = findStatic("opL_I", int.class, Object.class);
        opL_J = findStatic("opL_J", long.class, Object.class);
    }
    private static final MethodHandle[] INT_COLLECTORS = {
        constant(int.class, 42), opI, opI2, opI3, opI4
    };
    private static final MethodHandle[] BYTE_COLLECTORS = {
        constant(byte.class, (byte)42), i2b(opI), i2b(opI2), i2b(opI3), i2b(opI4)
    };
    private static final MethodHandle[] LONG_COLLECTORS = {
        constant(long.class, 42), opJ, opJ2, opJ3
    };

    private static int addI(int x, int y) { stress(); return x+y; }
    private static Object addL(Object x, Object y) { return addI((int)x, (int)y); }
    private static final MethodHandle addI, addL;
    static {
        addI = findStatic("addI", int.class, int.class, int.class);
        addL = findStatic("addL", Object.class, Object.class, Object.class);
    }

    private static Object list8ints(int a, int b, int c, int d, int e, int f, int g, int h) {
        return Arrays.asList(a, b, c, d, e, f, g, h);
    }
    private static Object list8longs(long a, long b, long c, long d, long e, long f, long g, long h) {
        return Arrays.asList(a, b, c, d, e, f, g, h);
    }
    private static final MethodHandle list8ints = findStatic("list8ints", Object.class,
                                                             Collections.nCopies(8, int.class));
    private static final MethodHandle list8longs = findStatic("list8longs", Object.class,
                                                              Collections.nCopies(8, long.class));
    private static final MethodHandle[] INT_LISTERS, LONG_LISTERS, BYTE_LISTERS;
    static {
        int listerCount = list8ints.type().parameterCount() + 1;
        INT_LISTERS  = new MethodHandle[listerCount];
        LONG_LISTERS = new MethodHandle[listerCount];
        BYTE_LISTERS = new MethodHandle[listerCount];
        MethodHandle lister = list8ints;
        MethodHandle llister = list8longs;
        for (int i = listerCount - 1; ; i--) {
            INT_LISTERS[i] = lister;
            LONG_LISTERS[i] = llister;
            BYTE_LISTERS[i] = i2b(lister);
            if (i == 0)  break;
            lister  = insertArguments(lister,  i-1, 0);
            llister = insertArguments(llister, i-1, 0L);
        }
    }
    private static MethodHandle i2b(MethodHandle mh) {
        return MethodHandles.explicitCastArguments(mh, subst(mh.type(), int.class, byte.class));
    }
    private static MethodType subst(MethodType mt, Class<?> from, Class<?> to) {
        for (int i = 0; i < mt.parameterCount(); i++) {
            if (mt.parameterType(i) == from)
                mt = mt.changeParameterType(i, to);
        }
        if (mt.returnType() == from)
            mt = mt.changeReturnType(to);
        return mt;
    }


    private static Object  convI_L(int     x) { stress(); return (Object)  x; }
    private static int     convL_I(Object  x) { stress(); return (int)     x; }
    private static Object  convJ_L(long    x) { stress(); return (Object)  x; }
    private static long    convL_J(Object  x) { stress(); return (long)    x; }
    private static int     convJ_I(long    x) { stress(); return (int)     x; }
    private static long    convI_J(int     x) { stress(); return (long)    x; }
    private static final MethodHandle convI_L, convL_I, convJ_L, convL_J, convJ_I, convI_J;
    static {
        convI_L = findStatic("convI_L", Object.class, int.class);
        convL_I = findStatic("convL_I", int.class, Object.class);
        convJ_L = findStatic("convJ_L", Object.class, long.class);
        convL_J = findStatic("convL_J", long.class, Object.class);
        convJ_I = findStatic("convJ_I", int.class, long.class);
        convI_J = findStatic("convI_J", long.class, int.class);
    }

    // stress modes:
    private static final int MAX_DEPTH = getProperty("MAX_DEPTH", 5);
    private static final int REPEAT = getProperty("REPEAT", 0);
    private static final int STRESS = getProperty("STRESS", 0);
    private static /*v*/ int STRESS_COUNT;
    private static final Object[] SINK = new Object[4];
    private static void stress() {
        if (STRESS <= 0) return;
        int count = STRESS + (STRESS_COUNT++ & 0x1);  // non-constant value
        for (int i = 0; i < count; i++) {
            SINK[i % SINK.length] = new Object[STRESS + i % (SINK.length + 1)];
        }
    }

    // verbosity:
    private static final int VERBOSITY = getProperty("VERBOSITY", 0) + (REPEAT == 0 ? 0 : -1);
    private static void verbose(Object a, Object b, Object c, Object d) {
        if (VERBOSITY <= 0)  return;
        verbose(1, a, b, c, d);
    }
    private static void verbose(Object a, Object b, Object c) {
        if (VERBOSITY <= 0)  return;
        verbose(1, a, b, c);
    }
    private static void verbose(int level, Object a, Object... bcd) {
        if (level > VERBOSITY)  return;
        String m = a.toString();
        if (bcd != null && bcd.length > 0) {
            List<Object> l = new ArrayList<>(bcd.length);
            for (Object x : bcd) {
                if (x instanceof Object[])  x = Arrays.asList((Object[])x);
                if (x instanceof int[])     x = Arrays.toString((int[])x);
                if (x instanceof long[])    x = Arrays.toString((long[])x);
                l.add(x);
            }
            m = m+Arrays.asList(bcd);
        }
        System.out.println(m);
    }
    String testOnly;
    String testOnlyTests;
    private boolean startTest(String name) {
        if (testOnly != null && !testOnly.contains(name))
            return false;
        verbose(0, "["+name+"]");
        testOnlyTests = (testOnlyTests == null) ? name : testOnlyTests+" "+name;
        return true;
    }

}
