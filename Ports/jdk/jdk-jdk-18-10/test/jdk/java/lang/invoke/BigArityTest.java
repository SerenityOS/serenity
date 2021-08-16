/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @summary High arity invocations
 * @compile BigArityTest.java
 * @run junit/othervm/timeout=2500 -XX:+IgnoreUnrecognizedVMOptions -XX:-VerifyDependencies -esa -DBigArityTest.ITERATION_COUNT=1 test.java.lang.invoke.BigArityTest
 */

package test.java.lang.invoke;

import org.junit.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class BigArityTest {

    static MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    static final int MAX_JVM_ARITY = 255;
    static final int ITERATION_COUNT = getProperty("ITERATION_COUNT", 40000);
    static final int MIN_ARITY = getProperty("MIN_ARITY", 250);
    static final int SLOW_ARITY = getProperty("SLOW_ARITY", MAX_JVM_ARITY-3);
    static final int MAX_ARITY = getProperty("MAX_ARITY", MAX_JVM_ARITY-1);  // always -1 for the MH reciever itself
    private static int getProperty(String name, int dflt) {
        return Integer.parseInt(getProperty(name, ""+dflt));
    }
    private static String getProperty(String name, String dflt) {
        String x = System.getProperty(BigArityTest.class.getSimpleName() + "." + name);
        if (x == null)  x = System.getProperty(BigArityTest.class.getName() + "." + name);
        return x == null ? dflt : x;
    }

    static final MethodType MT_A = MethodType.methodType(Object.class, Object.class, Object[].class, Object.class);

    static Object hashArguments(Object... args) {
        return Objects.hash(args);
    }
    static Object hashArguments(int... args) {
        Object[] wrappedArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            wrappedArgs[i] = args[i];
        }
        return hashArguments(wrappedArgs);
    }
    static Object hashArguments(long... args) {
        Object[] wrappedArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            wrappedArgs[i] = (int) args[i];
        }
        return hashArguments(wrappedArgs);
    }

    static Object hashArguments1(Object o, Object... args) {
        Object[] arr = new Object[args.length + 1];
        arr[0] = 0;
        System.arraycopy(args, 0, arr, 1, args.length);
        return Objects.hash(arr);
    }
    static Object hashArguments1(int i0, int... args) {
        Object[] wrappedArgs = new Object[args.length + 1];
        wrappedArgs[0] = i0;
        for (int i = 0; i < args.length; i++) {
            wrappedArgs[i + 1] = args[i];
        }
        return hashArguments(wrappedArgs);
    }
    static Object hashArguments1(long l0, long... args) {
        Object[] wrappedArgs = new Object[args.length + 1];
        wrappedArgs[0] = l0;
        for (int i = 0; i < args.length; i++) {
            wrappedArgs[i + 1] = (int) args[i];
        }
        return hashArguments(wrappedArgs);
    }

    static final MethodHandle MH_hashArguments_VA;
    static final MethodHandle MH_hashArguments_IVA;
    static final MethodHandle MH_hashArguments_JVA;
    static final MethodHandle MH_hashArguments1_VA;
    static final MethodHandle MH_hashArguments1_IVA;
    static final MethodHandle MH_hashArguments1_JVA;
    static {
        try {
            MH_hashArguments_VA =
                MethodHandles.lookup().unreflect(
                    BigArityTest.class.getDeclaredMethod("hashArguments", Object[].class));
            MH_hashArguments_IVA =
                MethodHandles.lookup().unreflect(
                    BigArityTest.class.getDeclaredMethod("hashArguments", int[].class));
            MH_hashArguments_JVA =
                MethodHandles.lookup().unreflect(
                    BigArityTest.class.getDeclaredMethod("hashArguments", long[].class));
            MH_hashArguments1_VA =
                MethodHandles.lookup().unreflect(
                    BigArityTest.class.getDeclaredMethod("hashArguments1", Object.class, Object[].class));
            MH_hashArguments1_IVA =
                MethodHandles.lookup().unreflect(
                    BigArityTest.class.getDeclaredMethod("hashArguments1", int.class, int[].class));
            MH_hashArguments1_JVA =
                MethodHandles.lookup().unreflect(
                    BigArityTest.class.getDeclaredMethod("hashArguments1", long.class, long[].class));
        } catch (ReflectiveOperationException ex) {
            throw new Error(ex);
        }
    }
    static MethodHandle MH_hashArguments(int arity) {
        MethodType mt = MethodType.genericMethodType(arity);
        return MH_hashArguments_VA.asType(mt);
    }
    static MethodHandle MH_hashArguments(Class<? extends Object[]> arrayClass, int arity) {
        if (arrayClass == Object[].class)
            return MH_hashArguments(arity);
        ArrayList<Class<?>> ptypes = new ArrayList<>(Collections.<Class<?>>nCopies(arity, arrayClass.getComponentType()));
        MethodType mt = MethodType.methodType(Object.class, ptypes);
        return MH_hashArguments_VA.asType(mt);
    }

    static Object[] testArgs(int arity) {
        Object args[] = new Object[arity];
        for (int i = 0; i < arity; i++)
            args[i] = i * (i + 1) / 2;
        return args;
    }

    @Test
    public void asCollectorIAE01() throws ReflectiveOperationException {
        final int [] INVALID_ARRAY_LENGTHS = {
            Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -2, -1, 255, 256, Integer.MAX_VALUE - 1, Integer.MAX_VALUE
        };
        MethodHandle target = MethodHandles.publicLookup().findStatic(Arrays.class,
                "deepToString", MethodType.methodType(String.class, Object[].class));
        int minbig = Integer.MAX_VALUE;
        for (int invalidLength : INVALID_ARRAY_LENGTHS) {
            if (minbig > invalidLength && invalidLength > 100)  minbig = invalidLength;
            try {
                target.asCollector(Object[].class, invalidLength);
                assert(false) : invalidLength;
            } catch (IllegalArgumentException ex) {
                System.out.println("OK: "+ex);
            }
        }
        // Sizes not in the above array are good:
        target.asCollector(Object[].class, minbig - 1);
        for (int i = 2; i <= 10; i++)
            target.asCollector(Object[].class, minbig - i);
    }

    static void asciae02target(Object[] a, Object b) {
        // naught
    }

    @Test
    public void asCollectorIAE02() throws ReflectiveOperationException {
        final int[] INVALID_ARRAY_LENGTHS = {
            Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -2, -1, 254, 255, Integer.MAX_VALUE - 1, Integer.MAX_VALUE
        };
        MethodHandle target = MethodHandles.lookup().findStatic(BigArityTest.class, "asciae02target",
                MethodType.methodType(void.class, Object[].class, Object.class));
        int minbig = Integer.MAX_VALUE;
        for (int invalidLength : INVALID_ARRAY_LENGTHS) {
            if (minbig > invalidLength && invalidLength > 100) minbig = invalidLength;
            try {
                target.asCollector(0, Object[].class, invalidLength);
                assert(false) : invalidLength;
            } catch (IllegalArgumentException ex) {
                System.out.println("OK: "+ex);
            }
        }
        // Sizes not in the above array are good:
        for (int i = 1; i <= 10; ++i) {
            target.asCollector(0, Object[].class, minbig - i);
        }
    }

    @Test
    public void invoker02() {
        for (int i = 0; i < 255; i++) {
            MethodType mt = MethodType.genericMethodType(i);
            MethodType expMT = mt.insertParameterTypes(0, MethodHandle.class);
            if (i < 254) {
                assertEquals(expMT, MethodHandles.invoker(mt).type());
            } else {
                try {
                    MethodHandles.invoker(mt);
                    assert(false) : i;
                } catch (IllegalArgumentException ex) {
                    System.out.println("OK: "+ex);
                }
            }
        }
    }

    @Test
    public void exactInvoker02() {
        for (int i = 0; i < 255; i++) {
            MethodType mt = MethodType.genericMethodType(i);
            MethodType expMT = mt.insertParameterTypes(0, MethodHandle.class);
            if (i < 254) {
                assertEquals(expMT, MethodHandles.exactInvoker(mt).type());
            } else {
                try {
                    MethodHandles.exactInvoker(mt);
                    assert(false) : i;
                } catch (IllegalArgumentException ex) {
                    System.out.println("OK: "+ex);
                }
            }
        }
    }

    @Test
    public void testBoundaryValues() throws Throwable {
        for (int badArity : new int[]{ -1, MAX_JVM_ARITY+1, MAX_JVM_ARITY }) {
            try {
                MethodHandle badmh = MH_hashArguments(badArity);
                throw new AssertionError("should not be able to build a 255-arity MH: "+badmh);
            } catch (IllegalArgumentException | WrongMethodTypeException ex) {
                System.out.println("OK: "+ex);
            }
        }
        final int MAX_MH_ARITY      = MAX_JVM_ARITY - 1;  // mh.invoke(arg*[N])
        final int MAX_INVOKER_ARITY = MAX_MH_ARITY - 1;   // inv.invoke(mh, arg*[N])
        for (int arity : new int[]{ 0, 1, MAX_MH_ARITY-2, MAX_MH_ARITY-1, MAX_MH_ARITY }) {
            MethodHandle mh = MH_hashArguments(arity);
            if (arity < MAX_INVOKER_ARITY) {
                MethodHandle ximh = MethodHandles.exactInvoker(mh.type());
                MethodHandle gimh = MethodHandles.invoker(mh.type());
                MethodHandle simh = MethodHandles.spreadInvoker(mh.type(), 0);
                if (arity != 0) {
                    simh = MethodHandles.spreadInvoker(mh.type(), 1);
                } else {
                    try {
                        simh = MethodHandles.spreadInvoker(mh.type(), 1);
                        assert(false) : arity;
                    } catch (IllegalArgumentException ex) {
                        System.out.println("OK: "+ex);
                    }
                }
                if (arity != 0) {
                    simh = MethodHandles.spreadInvoker(mh.type(), arity-1);
                } else {
                    try {
                        simh = MethodHandles.spreadInvoker(mh.type(), arity-1);
                        assert(false) : arity;
                    } catch (IllegalArgumentException ex) {
                        System.out.println("OK: "+ex);
                    }
                }
                simh = MethodHandles.spreadInvoker(mh.type(), arity);
            }
        }
    }

    // Make sure the basic argument spreading and varargs mechanisms are working.
    // Exercise arity 3 thoroughly.
    @Test
    public void testSpreads() throws Throwable {
        System.out.println("testing asSpreader on arity=3");
        Object[] args = testArgs(3);
        int r0 = Objects.hash(args);
        MethodHandle mh = MH_hashArguments(3);
        Object r;
        r = mh.invokeExact(args[0], args[1], args[2]);
        assertEquals(r0, r);
        r = mh.invoke(args[0], args[1], args[2]);
        assertEquals(r0, r);
        r = mh.invoke((Comparable) args[0], (Integer) args[1], (Number) args[2]);
        assertEquals(r0, r);
        r = mh.invokeWithArguments(args);
        assertEquals(r0, r);
        for (Class<?> cls0 : new Class<?>[] {
            Object[].class, Number[].class, Integer[].class, Comparable[].class
        }) {
            @SuppressWarnings("unchecked")
            Class<? extends Object[]> cls = (Class<? extends Object[]>) cls0;
            //Class<? extends Object[]> cls = Object[].class.asSubclass(cls0);
            int nargs = args.length, skip;
            Object hr;
            MethodHandle smh = mh.asSpreader(cls, nargs - (skip = 0));
            MethodHandle hsmh = mh.asSpreader(0, cls, nargs - skip);
            Object[] tail = Arrays.copyOfRange(args, skip, nargs, cls);
            Object[] head = Arrays.copyOfRange(args, 0, nargs - skip, cls);
            if (cls == Object[].class) {
                r = smh.invokeExact(tail);
                hr = hsmh.invokeExact(head);
            } else if (cls == Integer[].class) {
                r = smh.invokeExact((Integer[]) tail); //warning OK, see 8019340
                hr = hsmh.invokeExact((Integer[]) head);
            } else {
                r = smh.invoke(tail);
                hr = hsmh.invoke(head);
            }
            assertEquals(r0, r);
            assertEquals(r0, hr);
            smh = mh.asSpreader(cls, nargs - (skip = 1));
            hsmh = mh.asSpreader(0, cls, nargs - skip);
            tail = Arrays.copyOfRange(args, skip, nargs, cls);
            head = Arrays.copyOfRange(args, 0, nargs - skip, cls);
            if (cls == Object[].class) {
                r = smh.invokeExact(args[0], tail);
                hr = hsmh.invokeExact(head, args[2]);
            } else if (cls == Integer[].class) {
                r = smh.invokeExact(args[0], (Integer[]) tail);
                hr = hsmh.invokeExact((Integer[]) head, args[2]);
            } else {
                r = smh.invoke(args[0], tail);
                hr = hsmh.invoke(head, args[2]);
            }
            assertEquals(r0, r);
            assertEquals(r0, hr);
            smh = mh.asSpreader(cls, nargs - (skip = 2));
            hsmh = mh.asSpreader(0, cls, nargs - skip);
            tail = Arrays.copyOfRange(args, skip, nargs, cls);
            head = Arrays.copyOfRange(args, 0, nargs - skip, cls);
            if (cls == Object[].class) {
                r = smh.invokeExact(args[0], args[1], tail);
                hr = hsmh.invokeExact(head, args[1], args[2]);
            } else if (cls == Integer[].class) {
                r = smh.invokeExact(args[0], args[1], (Integer[]) tail);
                hr = hsmh.invokeExact((Integer[]) head, args[1], args[2]);
            } else {
                r = smh.invoke(args[0], args[1], tail);
                hr = hsmh.invoke(head, args[1], args[2]);
            }
            assertEquals(r0, r);
            assertEquals(r0, hr);
            smh = mh.asSpreader(cls, nargs - (skip = 3));
            hsmh = mh.asSpreader(0, cls, nargs - skip);
            tail = Arrays.copyOfRange(args, skip, nargs, cls);
            head = Arrays.copyOfRange(args, 0, nargs - skip, cls);
            if (cls == Object[].class) {
                r = smh.invokeExact(args[0], args[1], args[2], tail);
                hr = hsmh.invokeExact(head, args[0], args[1], args[2]);
            } else if (cls == Integer[].class) {
                r = smh.invokeExact(args[0], args[1], args[2], (Integer[]) tail);
                hr = hsmh.invokeExact((Integer[]) head, args[0], args[1], args[2]);
            } else {
                r = smh.invoke(args[0], args[1], args[2], tail);
                hr = hsmh.invoke(head, args[0], args[1], args[2]);
            }
            assertEquals(r0, r);
            assertEquals(r0, hr);
            // Try null array in addition to zero-length array:
            tail = null;
            head = null;
            if (cls == Object[].class) {
                r = smh.invokeExact(args[0], args[1], args[2], tail);
                hr = hsmh.invokeExact(head, args[0], args[1], args[2]);
            } else if (cls == Integer[].class) {
                r = smh.invokeExact(args[0], args[1], args[2], (Integer[]) tail);
                hr = hsmh.invokeExact((Integer[]) head, args[0], args[1], args[2]);
            } else {
                r = smh.invoke(args[0], args[1], args[2], tail);
                hr = hsmh.invoke(head, args[0], args[1], args[2]);
            }
            assertEquals(r0, r);
            assertEquals(r0, hr);
        }
    }

    @Test
    public void testInvokeWithArguments() throws Throwable {
        System.out.println("testing invokeWithArguments on all arities");
        for (int arity = 0; arity < MAX_ARITY; arity++) {
            Object[] args = testArgs(arity);
            Object r0 = Objects.hash(args);
            Object r = MH_hashArguments(arity).invokeWithArguments(args);
            assertEquals("arity="+arity, r0, r);
        }
        // The next one is the most likely to fail:
        int arity = MAX_ARITY;
        Object[] args = testArgs(arity);
        Object r0 = Objects.hash(args);
        Object r = MH_hashArguments(arity).invokeWithArguments(args);
        assertEquals("arity=MAX_ARITY", r0, r);
    }

    @Test
    public void testInvokeWithArgumentsJumbo() throws Throwable {
        System.out.println("testing invokeWithArguments on jumbo arities");
        ArrayList<Integer> arities = new ArrayList<>();
        for (int arity = 125; arity < 1000; arity += (arity < MAX_ARITY+10 ? 1 : arity/7)) {
            arities.add(arity);
            Object[] args = testArgs(arity);
            Object r0 = Objects.hash(args);

            assertEquals("jumbo arity="+arity, r0, MH_hashArguments_VA.invokeWithArguments(args));
            assertEquals("jumbo arity="+arity, r0, MH_hashArguments1_VA.invokeWithArguments(args));

            // use primitive typed argument lists also:
            assertEquals("jumbo int arity="+arity, r0, MH_hashArguments_IVA.invokeWithArguments(args));
            assertEquals("jumbo int arity="+arity, r0, MH_hashArguments1_IVA.invokeWithArguments(args));

            assertEquals("jumbo long arity="+arity, r0, MH_hashArguments_JVA.invokeWithArguments(args));
            assertEquals("jumbo long arity="+arity, r0, MH_hashArguments1_JVA.invokeWithArguments(args));
        }
        System.out.println("  jumbo arities = "+arities);
    }

    static Object[] cat(Object a, Object[] b) {
        int alen = 1, blen = b.length;
        Object[] c = new Object[alen + blen];
        c[0] = a;
        System.arraycopy(b, 0, c, alen, blen);
        return c;
    }

    @Test
    public void testArities() throws Throwable {
        System.out.println("testing spreaders and collectors on high arities...");
        int iterations = ITERATION_COUNT;
        testArities(Object[].class, MIN_ARITY-10, MIN_ARITY-1, iterations / 1000);
        testArities(Object[].class, MIN_ARITY, SLOW_ARITY-1, iterations);
        testArities(Object[].class, SLOW_ARITY, MAX_ARITY, iterations / 1000);
    }

    @Test
    public void testAritiesOnTypedArrays() throws Throwable {
        for (Class<?> cls0 : new Class<?>[] {
            Number[].class, Integer[].class, Comparable[].class
        }) {
            @SuppressWarnings("unchecked")
            Class<? extends Object[]> cls = (Class<? extends Object[]>) cls0;
            System.out.println("array class: "+cls.getSimpleName());
            int iterations = ITERATION_COUNT / 1000;
            try {
                testArities(cls, MIN_ARITY, SLOW_ARITY - 1, iterations);
                testArities(cls, SLOW_ARITY, MAX_ARITY, iterations / 100);
            } catch (Throwable t) {
                t.printStackTrace();
                throw t;
            }
        }
    }

    private void testArities(Class<? extends Object[]> cls,
                             int minArity,
                             int maxArity,
                             int iterations) throws Throwable {
        boolean verbose = (cls == Object[].class);
        for (int arity = minArity; arity <= maxArity; arity++) {
            if (verbose)  System.out.println("arity="+arity);
            MethodHandle mh = MH_hashArguments(cls, arity);
            MethodHandle mh_VA = mh.asSpreader(cls, arity);
            MethodHandle mh_VA_h = mh.asSpreader(0, cls, arity-1);
            assert(mh_VA.type().parameterType(0) == cls);
            assert(mh_VA_h.type().parameterType(0) == cls);
            testArities(cls, arity, iterations, verbose, mh, mh_VA, mh_VA_h);
            // mh_CA will collect arguments of a particular type and pass them to mh_VA
            MethodHandle mh_CA = mh_VA.asCollector(cls, arity);
            MethodHandle mh_VA2 = mh_CA.asSpreader(cls, arity);
            MethodHandle mh_VA2_h = mh_CA.asSpreader(0, cls, arity-1);
            assert(mh_CA.type().equals(mh.type()));
            assert(mh_VA2.type().equals(mh_VA.type()));
            if (cls != Object[].class) {
                try {
                    mh_VA2.invokeWithArguments(new Object[arity]);
                    throw new AssertionError("should not reach");
                } catch (ClassCastException | WrongMethodTypeException ex) {
                }
            }
            int iterations_VA = iterations / 100;
            testArities(cls, arity, iterations_VA, false, mh_CA, mh_VA2, mh_VA2_h);
        }
    }

   /**
     * Tests calls to {@link BigArityTest#hashArguments hashArguments} as related to a single given arity N.
     * Applies the given {@code mh} to a set of N integer arguments, checking the answer.
     * Also applies the varargs variation {@code mh_VA} to an array of type C[] (given by {@code cls}).
     * Test steps:
     * <ul>
     * <li>mh_VA.invokeExact(new C[]{ arg, ... })</li>
     * <li>mh.invokeWithArguments((Object[]) new C[]{ arg, ... })</li>
     * <li>exactInvoker(mh.type()).invokeWithArguments(new Object[]{ mh, arg, ... })</li>
     * <li>invoker(mh.type()).invokeWithArguments(new Object[]{ mh, arg, ... })</li>
     * </ul>
     * @param cls     array type for varargs call (one of Object[], Number[], Integer[], Comparable[])
     * @param arity   N, the number of arguments to {@code mh} and length of its varargs array, in [0..255]
     * @param iterations  number of times to repeat each test step (at least 4)
     * @param verbose are we printing extra output?
     * @param mh      a fixed-arity version of {@code hashArguments}
     * @param mh_VA   a variable-arity version of {@code hashArguments}, accepting the given array type {@code cls}
     * @param mh_VA_h a version of {@code hashArguments} that has a leading {@code cls} array and one final {@code cls}
    *                 argument
     */
    private void testArities(Class<? extends Object[]> cls,
                             int arity,
                             int iterations,
                             boolean verbose,
                             MethodHandle mh,
                             MethodHandle mh_VA,
                             MethodHandle mh_VA_h
                             ) throws Throwable {
        if (iterations < 4)  iterations = 4;
        final int MAX_MH_ARITY      = MAX_JVM_ARITY - 1;  // mh.invoke(arg*[N])
        final int MAX_INVOKER_ARITY = MAX_MH_ARITY - 1;   // inv.invoke(mh, arg*[N])
        Object[] args = testArgs(arity);
        if (cls != Object[].class)
            args = Arrays.copyOf(args, arity, cls);
        Object r0 = Objects.hash(args);
        Object r;
        Object hr;
        MethodHandle ximh = null;
        MethodHandle gimh = null;
        if (arity <= MAX_INVOKER_ARITY) {
            ximh = MethodHandles.exactInvoker(mh.type());
            gimh = MethodHandles.invoker(mh.type());
        } else {
            try {
                ximh = MethodHandles.exactInvoker(mh.type());
                throw new AssertionError("should fail to create ximh of arity "+arity);
            } catch (IllegalArgumentException ex) {
                if (verbose)
                    System.out.println("OK: xmih["+arity+"] => "+ex);
            }
            try {
                gimh = MethodHandles.invoker(mh.type());
                throw new AssertionError("should fail to create gimh of arity "+arity);
            } catch (IllegalArgumentException ex) {
                if (verbose)
                    System.out.println("OK: gmih["+arity+"] => "+ex);
            }
        }
        Object[] mh_args = cat(mh, args);
        assert(arity <= MAX_MH_ARITY);
        for (int i = 0; i < iterations; ++i) {
            if (cls == Object[].class) {
                r = mh_VA.invokeExact(args);
                hr = mh_VA_h.invokeExact(Arrays.copyOfRange(args, 0, arity - 1), args[arity - 1]);
            } else if (cls == Integer[].class) {
                r = mh_VA.invokeExact((Integer[]) args); //warning OK, see 8019340
                hr = mh_VA_h.invokeExact((Integer[]) Arrays.copyOfRange(args, 0, arity - 1), (Integer) args[arity - 1]);
            } else {
                r = mh_VA.invoke(args);
                hr = mh_VA_h.invoke(Arrays.copyOfRange(args, 0, arity - 1), args[arity - 1]);
            }
            assertEquals(r0, r);
            assertEquals(r0, hr);
            r = mh.invokeWithArguments(args);
            assertEquals(r0, r);
            if (ximh != null) {
                r = ximh.invokeWithArguments(mh_args);
                assertEquals(r0, r);
            }
            if (gimh != null) {
                r = gimh.invokeWithArguments(mh_args);
                assertEquals(r0, r);
            }
        }
    }

    static Object hashArguments_252(
    // <editor-fold defaultstate="collapsed" desc="Object x00, Object x01, Object x02, Object x03, Object x04, ...">
    Object x00, Object x01, Object x02, Object x03, Object x04, Object x05, Object x06, Object x07,
    Object x08, Object x09, Object x0A, Object x0B, Object x0C, Object x0D, Object x0E, Object x0F,
    Object x10, Object x11, Object x12, Object x13, Object x14, Object x15, Object x16, Object x17,
    Object x18, Object x19, Object x1A, Object x1B, Object x1C, Object x1D, Object x1E, Object x1F,
    Object x20, Object x21, Object x22, Object x23, Object x24, Object x25, Object x26, Object x27,
    Object x28, Object x29, Object x2A, Object x2B, Object x2C, Object x2D, Object x2E, Object x2F,
    Object x30, Object x31, Object x32, Object x33, Object x34, Object x35, Object x36, Object x37,
    Object x38, Object x39, Object x3A, Object x3B, Object x3C, Object x3D, Object x3E, Object x3F,
    Object x40, Object x41, Object x42, Object x43, Object x44, Object x45, Object x46, Object x47,
    Object x48, Object x49, Object x4A, Object x4B, Object x4C, Object x4D, Object x4E, Object x4F,
    Object x50, Object x51, Object x52, Object x53, Object x54, Object x55, Object x56, Object x57,
    Object x58, Object x59, Object x5A, Object x5B, Object x5C, Object x5D, Object x5E, Object x5F,
    Object x60, Object x61, Object x62, Object x63, Object x64, Object x65, Object x66, Object x67,
    Object x68, Object x69, Object x6A, Object x6B, Object x6C, Object x6D, Object x6E, Object x6F,
    Object x70, Object x71, Object x72, Object x73, Object x74, Object x75, Object x76, Object x77,
    Object x78, Object x79, Object x7A, Object x7B, Object x7C, Object x7D, Object x7E, Object x7F,
    Object x80, Object x81, Object x82, Object x83, Object x84, Object x85, Object x86, Object x87,
    Object x88, Object x89, Object x8A, Object x8B, Object x8C, Object x8D, Object x8E, Object x8F,
    Object x90, Object x91, Object x92, Object x93, Object x94, Object x95, Object x96, Object x97,
    Object x98, Object x99, Object x9A, Object x9B, Object x9C, Object x9D, Object x9E, Object x9F,
    Object xA0, Object xA1, Object xA2, Object xA3, Object xA4, Object xA5, Object xA6, Object xA7,
    Object xA8, Object xA9, Object xAA, Object xAB, Object xAC, Object xAD, Object xAE, Object xAF,
    Object xB0, Object xB1, Object xB2, Object xB3, Object xB4, Object xB5, Object xB6, Object xB7,
    Object xB8, Object xB9, Object xBA, Object xBB, Object xBC, Object xBD, Object xBE, Object xBF,
    Object xC0, Object xC1, Object xC2, Object xC3, Object xC4, Object xC5, Object xC6, Object xC7,
    Object xC8, Object xC9, Object xCA, Object xCB, Object xCC, Object xCD, Object xCE, Object xCF,
    Object xD0, Object xD1, Object xD2, Object xD3, Object xD4, Object xD5, Object xD6, Object xD7,
    Object xD8, Object xD9, Object xDA, Object xDB, Object xDC, Object xDD, Object xDE, Object xDF,
    Object xE0, Object xE1, Object xE2, Object xE3, Object xE4, Object xE5, Object xE6, Object xE7,
    Object xE8, Object xE9, Object xEA, Object xEB, Object xEC, Object xED, Object xEE, Object xEF,
    Object xF0, Object xF1, Object xF2, Object xF3, Object xF4, Object xF5, Object xF6, Object xF7,
    // </editor-fold>
    Object xF8, Object xF9, Object xFA, Object xFB) {
        return Objects.hash(
    // <editor-fold defaultstate="collapsed" desc="x00, x01, x02, x03, x04, ...">
    x00, x01, x02, x03, x04, x05, x06, x07, x08, x09, x0A, x0B, x0C, x0D, x0E, x0F,
    x10, x11, x12, x13, x14, x15, x16, x17, x18, x19, x1A, x1B, x1C, x1D, x1E, x1F,
    x20, x21, x22, x23, x24, x25, x26, x27, x28, x29, x2A, x2B, x2C, x2D, x2E, x2F,
    x30, x31, x32, x33, x34, x35, x36, x37, x38, x39, x3A, x3B, x3C, x3D, x3E, x3F,
    x40, x41, x42, x43, x44, x45, x46, x47, x48, x49, x4A, x4B, x4C, x4D, x4E, x4F,
    x50, x51, x52, x53, x54, x55, x56, x57, x58, x59, x5A, x5B, x5C, x5D, x5E, x5F,
    x60, x61, x62, x63, x64, x65, x66, x67, x68, x69, x6A, x6B, x6C, x6D, x6E, x6F,
    x70, x71, x72, x73, x74, x75, x76, x77, x78, x79, x7A, x7B, x7C, x7D, x7E, x7F,
    x80, x81, x82, x83, x84, x85, x86, x87, x88, x89, x8A, x8B, x8C, x8D, x8E, x8F,
    x90, x91, x92, x93, x94, x95, x96, x97, x98, x99, x9A, x9B, x9C, x9D, x9E, x9F,
    xA0, xA1, xA2, xA3, xA4, xA5, xA6, xA7, xA8, xA9, xAA, xAB, xAC, xAD, xAE, xAF,
    xB0, xB1, xB2, xB3, xB4, xB5, xB6, xB7, xB8, xB9, xBA, xBB, xBC, xBD, xBE, xBF,
    xC0, xC1, xC2, xC3, xC4, xC5, xC6, xC7, xC8, xC9, xCA, xCB, xCC, xCD, xCE, xCF,
    xD0, xD1, xD2, xD3, xD4, xD5, xD6, xD7, xD8, xD9, xDA, xDB, xDC, xDD, xDE, xDF,
    xE0, xE1, xE2, xE3, xE4, xE5, xE6, xE7, xE8, xE9, xEA, xEB, xEC, xED, xEE, xEF,
    xF0, xF1, xF2, xF3, xF4, xF5, xF6, xF7,
    // </editor-fold>
    xF8, xF9, xFA, xFB);
    }
    static Object hashArguments_252_a(Object x00, Object[] x01_FA, Object xFB) {
        return Objects.hash(
                // <editor-fold defaultstate="collapsed" desc="x00, x01_FA[0], x01_FA[1], x01_FA[2], ...">
                x00, x01_FA[0], x01_FA[1], x01_FA[2], x01_FA[3], x01_FA[4], x01_FA[5], x01_FA[6], x01_FA[7], x01_FA[8],
                x01_FA[9], x01_FA[10], x01_FA[11], x01_FA[12], x01_FA[13], x01_FA[14], x01_FA[15], x01_FA[16],
                x01_FA[17], x01_FA[18], x01_FA[19], x01_FA[20], x01_FA[21], x01_FA[22], x01_FA[23], x01_FA[24],
                x01_FA[25], x01_FA[26], x01_FA[27], x01_FA[28], x01_FA[29], x01_FA[30], x01_FA[31], x01_FA[32],
                x01_FA[33], x01_FA[34], x01_FA[35], x01_FA[36], x01_FA[37], x01_FA[38], x01_FA[39], x01_FA[40],
                x01_FA[41], x01_FA[42], x01_FA[43], x01_FA[44], x01_FA[45], x01_FA[46], x01_FA[47], x01_FA[48],
                x01_FA[49], x01_FA[50], x01_FA[51], x01_FA[52], x01_FA[53], x01_FA[54], x01_FA[55], x01_FA[56],
                x01_FA[57], x01_FA[58], x01_FA[59], x01_FA[60], x01_FA[61], x01_FA[62], x01_FA[63], x01_FA[64],
                x01_FA[65], x01_FA[66], x01_FA[67], x01_FA[68], x01_FA[69], x01_FA[70], x01_FA[71], x01_FA[72],
                x01_FA[73], x01_FA[74], x01_FA[75], x01_FA[76], x01_FA[77], x01_FA[78], x01_FA[79], x01_FA[80],
                x01_FA[81], x01_FA[82], x01_FA[83], x01_FA[84], x01_FA[85], x01_FA[86], x01_FA[87], x01_FA[88],
                x01_FA[89], x01_FA[90], x01_FA[91], x01_FA[92], x01_FA[93], x01_FA[94], x01_FA[95], x01_FA[96],
                x01_FA[97], x01_FA[98], x01_FA[99], x01_FA[100], x01_FA[101], x01_FA[102], x01_FA[103], x01_FA[104],
                x01_FA[105], x01_FA[106], x01_FA[107], x01_FA[108], x01_FA[109], x01_FA[110], x01_FA[111], x01_FA[112],
                x01_FA[113], x01_FA[114], x01_FA[115], x01_FA[116], x01_FA[117], x01_FA[118], x01_FA[119], x01_FA[120],
                x01_FA[121], x01_FA[122], x01_FA[123], x01_FA[124], x01_FA[125], x01_FA[126], x01_FA[127], x01_FA[128],
                x01_FA[129], x01_FA[130], x01_FA[131], x01_FA[132], x01_FA[133], x01_FA[134], x01_FA[135], x01_FA[136],
                x01_FA[137], x01_FA[138], x01_FA[139], x01_FA[140], x01_FA[141], x01_FA[142], x01_FA[143], x01_FA[144],
                x01_FA[145], x01_FA[146], x01_FA[147], x01_FA[148], x01_FA[149], x01_FA[150], x01_FA[151], x01_FA[152],
                x01_FA[153], x01_FA[154], x01_FA[155], x01_FA[156], x01_FA[157], x01_FA[158], x01_FA[159], x01_FA[160],
                x01_FA[161], x01_FA[162], x01_FA[163], x01_FA[164], x01_FA[165], x01_FA[166], x01_FA[167], x01_FA[168],
                x01_FA[169], x01_FA[170], x01_FA[171], x01_FA[172], x01_FA[173], x01_FA[174], x01_FA[175], x01_FA[176],
                x01_FA[177], x01_FA[178], x01_FA[179], x01_FA[180], x01_FA[181], x01_FA[182], x01_FA[183], x01_FA[184],
                x01_FA[185], x01_FA[186], x01_FA[187], x01_FA[188], x01_FA[189], x01_FA[190], x01_FA[191], x01_FA[192],
                x01_FA[193], x01_FA[194], x01_FA[195], x01_FA[196], x01_FA[197], x01_FA[198], x01_FA[199], x01_FA[200],
                x01_FA[201], x01_FA[202], x01_FA[203], x01_FA[204], x01_FA[205], x01_FA[206], x01_FA[207], x01_FA[208],
                x01_FA[209], x01_FA[210], x01_FA[211], x01_FA[212], x01_FA[213], x01_FA[214], x01_FA[215], x01_FA[216],
                x01_FA[217], x01_FA[218], x01_FA[219], x01_FA[220], x01_FA[221], x01_FA[222], x01_FA[223], x01_FA[224],
                x01_FA[225], x01_FA[226], x01_FA[227], x01_FA[228], x01_FA[229], x01_FA[230], x01_FA[231], x01_FA[232],
                x01_FA[233], x01_FA[234], x01_FA[235], x01_FA[236], x01_FA[237], x01_FA[238], x01_FA[239], x01_FA[240],
                x01_FA[241], x01_FA[242], x01_FA[243], x01_FA[244], x01_FA[245], x01_FA[246], x01_FA[247], x01_FA[248],
                // </editor-fold>
                x01_FA[249], xFB);
    }

    @Test
    public void test252() throws Throwable {
        final int ARITY = 252;
        System.out.println("test"+ARITY);
        Object[] a = testArgs(ARITY);
        Object r0 = hashArguments(a);
        Object r;
        r = hashArguments_252(
    // <editor-fold defaultstate="collapsed" desc="a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], ...">
    a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], a[0x05], a[0x06], a[0x07], a[0x08], a[0x09], a[0x0A], a[0x0B], a[0x0C], a[0x0D], a[0x0E], a[0x0F],
    a[0x10], a[0x11], a[0x12], a[0x13], a[0x14], a[0x15], a[0x16], a[0x17], a[0x18], a[0x19], a[0x1A], a[0x1B], a[0x1C], a[0x1D], a[0x1E], a[0x1F],
    a[0x20], a[0x21], a[0x22], a[0x23], a[0x24], a[0x25], a[0x26], a[0x27], a[0x28], a[0x29], a[0x2A], a[0x2B], a[0x2C], a[0x2D], a[0x2E], a[0x2F],
    a[0x30], a[0x31], a[0x32], a[0x33], a[0x34], a[0x35], a[0x36], a[0x37], a[0x38], a[0x39], a[0x3A], a[0x3B], a[0x3C], a[0x3D], a[0x3E], a[0x3F],
    a[0x40], a[0x41], a[0x42], a[0x43], a[0x44], a[0x45], a[0x46], a[0x47], a[0x48], a[0x49], a[0x4A], a[0x4B], a[0x4C], a[0x4D], a[0x4E], a[0x4F],
    a[0x50], a[0x51], a[0x52], a[0x53], a[0x54], a[0x55], a[0x56], a[0x57], a[0x58], a[0x59], a[0x5A], a[0x5B], a[0x5C], a[0x5D], a[0x5E], a[0x5F],
    a[0x60], a[0x61], a[0x62], a[0x63], a[0x64], a[0x65], a[0x66], a[0x67], a[0x68], a[0x69], a[0x6A], a[0x6B], a[0x6C], a[0x6D], a[0x6E], a[0x6F],
    a[0x70], a[0x71], a[0x72], a[0x73], a[0x74], a[0x75], a[0x76], a[0x77], a[0x78], a[0x79], a[0x7A], a[0x7B], a[0x7C], a[0x7D], a[0x7E], a[0x7F],
    a[0x80], a[0x81], a[0x82], a[0x83], a[0x84], a[0x85], a[0x86], a[0x87], a[0x88], a[0x89], a[0x8A], a[0x8B], a[0x8C], a[0x8D], a[0x8E], a[0x8F],
    a[0x90], a[0x91], a[0x92], a[0x93], a[0x94], a[0x95], a[0x96], a[0x97], a[0x98], a[0x99], a[0x9A], a[0x9B], a[0x9C], a[0x9D], a[0x9E], a[0x9F],
    a[0xA0], a[0xA1], a[0xA2], a[0xA3], a[0xA4], a[0xA5], a[0xA6], a[0xA7], a[0xA8], a[0xA9], a[0xAA], a[0xAB], a[0xAC], a[0xAD], a[0xAE], a[0xAF],
    a[0xB0], a[0xB1], a[0xB2], a[0xB3], a[0xB4], a[0xB5], a[0xB6], a[0xB7], a[0xB8], a[0xB9], a[0xBA], a[0xBB], a[0xBC], a[0xBD], a[0xBE], a[0xBF],
    a[0xC0], a[0xC1], a[0xC2], a[0xC3], a[0xC4], a[0xC5], a[0xC6], a[0xC7], a[0xC8], a[0xC9], a[0xCA], a[0xCB], a[0xCC], a[0xCD], a[0xCE], a[0xCF],
    a[0xD0], a[0xD1], a[0xD2], a[0xD3], a[0xD4], a[0xD5], a[0xD6], a[0xD7], a[0xD8], a[0xD9], a[0xDA], a[0xDB], a[0xDC], a[0xDD], a[0xDE], a[0xDF],
    a[0xE0], a[0xE1], a[0xE2], a[0xE3], a[0xE4], a[0xE5], a[0xE6], a[0xE7], a[0xE8], a[0xE9], a[0xEA], a[0xEB], a[0xEC], a[0xED], a[0xEE], a[0xEF],
    a[0xF0], a[0xF1], a[0xF2], a[0xF3], a[0xF4], a[0xF5], a[0xF6], a[0xF7],
    // </editor-fold>
    a[0xF8], a[0xF9], a[0xFA], a[0xFB]); // hashArguments_252
        assertEquals(r0, r);
        MethodType mt = MethodType.genericMethodType(ARITY);
        MethodHandle mh = MethodHandles.lookup().findStatic(BigArityTest.class, "hashArguments_"+ARITY, mt);
        test252(mh, a, r0);
        MethodHandle mh_CA = MH_hashArguments_VA.asFixedArity().asCollector(Object[].class, ARITY);
        test252(mh_CA, a, r0);
        MethodHandle mh_a = MethodHandles.lookup().findStatic(BigArityTest.class, "hashArguments_"+ARITY+"_a", MT_A).asCollector(1, Object[].class, ARITY-2);
        test252(mh_a, a, r0);
    }
    public void test252(MethodHandle mh, Object[] a, Object r0) throws Throwable {
        Object r;
        r = mh.invokeExact(
    // <editor-fold defaultstate="collapsed" desc="a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], ...">
    a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], a[0x05], a[0x06], a[0x07], a[0x08], a[0x09], a[0x0A], a[0x0B], a[0x0C], a[0x0D], a[0x0E], a[0x0F],
    a[0x10], a[0x11], a[0x12], a[0x13], a[0x14], a[0x15], a[0x16], a[0x17], a[0x18], a[0x19], a[0x1A], a[0x1B], a[0x1C], a[0x1D], a[0x1E], a[0x1F],
    a[0x20], a[0x21], a[0x22], a[0x23], a[0x24], a[0x25], a[0x26], a[0x27], a[0x28], a[0x29], a[0x2A], a[0x2B], a[0x2C], a[0x2D], a[0x2E], a[0x2F],
    a[0x30], a[0x31], a[0x32], a[0x33], a[0x34], a[0x35], a[0x36], a[0x37], a[0x38], a[0x39], a[0x3A], a[0x3B], a[0x3C], a[0x3D], a[0x3E], a[0x3F],
    a[0x40], a[0x41], a[0x42], a[0x43], a[0x44], a[0x45], a[0x46], a[0x47], a[0x48], a[0x49], a[0x4A], a[0x4B], a[0x4C], a[0x4D], a[0x4E], a[0x4F],
    a[0x50], a[0x51], a[0x52], a[0x53], a[0x54], a[0x55], a[0x56], a[0x57], a[0x58], a[0x59], a[0x5A], a[0x5B], a[0x5C], a[0x5D], a[0x5E], a[0x5F],
    a[0x60], a[0x61], a[0x62], a[0x63], a[0x64], a[0x65], a[0x66], a[0x67], a[0x68], a[0x69], a[0x6A], a[0x6B], a[0x6C], a[0x6D], a[0x6E], a[0x6F],
    a[0x70], a[0x71], a[0x72], a[0x73], a[0x74], a[0x75], a[0x76], a[0x77], a[0x78], a[0x79], a[0x7A], a[0x7B], a[0x7C], a[0x7D], a[0x7E], a[0x7F],
    a[0x80], a[0x81], a[0x82], a[0x83], a[0x84], a[0x85], a[0x86], a[0x87], a[0x88], a[0x89], a[0x8A], a[0x8B], a[0x8C], a[0x8D], a[0x8E], a[0x8F],
    a[0x90], a[0x91], a[0x92], a[0x93], a[0x94], a[0x95], a[0x96], a[0x97], a[0x98], a[0x99], a[0x9A], a[0x9B], a[0x9C], a[0x9D], a[0x9E], a[0x9F],
    a[0xA0], a[0xA1], a[0xA2], a[0xA3], a[0xA4], a[0xA5], a[0xA6], a[0xA7], a[0xA8], a[0xA9], a[0xAA], a[0xAB], a[0xAC], a[0xAD], a[0xAE], a[0xAF],
    a[0xB0], a[0xB1], a[0xB2], a[0xB3], a[0xB4], a[0xB5], a[0xB6], a[0xB7], a[0xB8], a[0xB9], a[0xBA], a[0xBB], a[0xBC], a[0xBD], a[0xBE], a[0xBF],
    a[0xC0], a[0xC1], a[0xC2], a[0xC3], a[0xC4], a[0xC5], a[0xC6], a[0xC7], a[0xC8], a[0xC9], a[0xCA], a[0xCB], a[0xCC], a[0xCD], a[0xCE], a[0xCF],
    a[0xD0], a[0xD1], a[0xD2], a[0xD3], a[0xD4], a[0xD5], a[0xD6], a[0xD7], a[0xD8], a[0xD9], a[0xDA], a[0xDB], a[0xDC], a[0xDD], a[0xDE], a[0xDF],
    a[0xE0], a[0xE1], a[0xE2], a[0xE3], a[0xE4], a[0xE5], a[0xE6], a[0xE7], a[0xE8], a[0xE9], a[0xEA], a[0xEB], a[0xEC], a[0xED], a[0xEE], a[0xEF],
    a[0xF0], a[0xF1], a[0xF2], a[0xF3], a[0xF4], a[0xF5], a[0xF6], a[0xF7],
    // </editor-fold>
    a[0xF8], a[0xF9], a[0xFA], a[0xFB]);
        assertEquals(r0, r);
        r = mh.invokeWithArguments(a);
        assertEquals(r0, r);
        MethodHandle ximh = MethodHandles.exactInvoker(mh.type());
        r = ximh.invokeExact(mh,
    // <editor-fold defaultstate="collapsed" desc="a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], ...">
    a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], a[0x05], a[0x06], a[0x07], a[0x08], a[0x09], a[0x0A], a[0x0B], a[0x0C], a[0x0D], a[0x0E], a[0x0F],
    a[0x10], a[0x11], a[0x12], a[0x13], a[0x14], a[0x15], a[0x16], a[0x17], a[0x18], a[0x19], a[0x1A], a[0x1B], a[0x1C], a[0x1D], a[0x1E], a[0x1F],
    a[0x20], a[0x21], a[0x22], a[0x23], a[0x24], a[0x25], a[0x26], a[0x27], a[0x28], a[0x29], a[0x2A], a[0x2B], a[0x2C], a[0x2D], a[0x2E], a[0x2F],
    a[0x30], a[0x31], a[0x32], a[0x33], a[0x34], a[0x35], a[0x36], a[0x37], a[0x38], a[0x39], a[0x3A], a[0x3B], a[0x3C], a[0x3D], a[0x3E], a[0x3F],
    a[0x40], a[0x41], a[0x42], a[0x43], a[0x44], a[0x45], a[0x46], a[0x47], a[0x48], a[0x49], a[0x4A], a[0x4B], a[0x4C], a[0x4D], a[0x4E], a[0x4F],
    a[0x50], a[0x51], a[0x52], a[0x53], a[0x54], a[0x55], a[0x56], a[0x57], a[0x58], a[0x59], a[0x5A], a[0x5B], a[0x5C], a[0x5D], a[0x5E], a[0x5F],
    a[0x60], a[0x61], a[0x62], a[0x63], a[0x64], a[0x65], a[0x66], a[0x67], a[0x68], a[0x69], a[0x6A], a[0x6B], a[0x6C], a[0x6D], a[0x6E], a[0x6F],
    a[0x70], a[0x71], a[0x72], a[0x73], a[0x74], a[0x75], a[0x76], a[0x77], a[0x78], a[0x79], a[0x7A], a[0x7B], a[0x7C], a[0x7D], a[0x7E], a[0x7F],
    a[0x80], a[0x81], a[0x82], a[0x83], a[0x84], a[0x85], a[0x86], a[0x87], a[0x88], a[0x89], a[0x8A], a[0x8B], a[0x8C], a[0x8D], a[0x8E], a[0x8F],
    a[0x90], a[0x91], a[0x92], a[0x93], a[0x94], a[0x95], a[0x96], a[0x97], a[0x98], a[0x99], a[0x9A], a[0x9B], a[0x9C], a[0x9D], a[0x9E], a[0x9F],
    a[0xA0], a[0xA1], a[0xA2], a[0xA3], a[0xA4], a[0xA5], a[0xA6], a[0xA7], a[0xA8], a[0xA9], a[0xAA], a[0xAB], a[0xAC], a[0xAD], a[0xAE], a[0xAF],
    a[0xB0], a[0xB1], a[0xB2], a[0xB3], a[0xB4], a[0xB5], a[0xB6], a[0xB7], a[0xB8], a[0xB9], a[0xBA], a[0xBB], a[0xBC], a[0xBD], a[0xBE], a[0xBF],
    a[0xC0], a[0xC1], a[0xC2], a[0xC3], a[0xC4], a[0xC5], a[0xC6], a[0xC7], a[0xC8], a[0xC9], a[0xCA], a[0xCB], a[0xCC], a[0xCD], a[0xCE], a[0xCF],
    a[0xD0], a[0xD1], a[0xD2], a[0xD3], a[0xD4], a[0xD5], a[0xD6], a[0xD7], a[0xD8], a[0xD9], a[0xDA], a[0xDB], a[0xDC], a[0xDD], a[0xDE], a[0xDF],
    a[0xE0], a[0xE1], a[0xE2], a[0xE3], a[0xE4], a[0xE5], a[0xE6], a[0xE7], a[0xE8], a[0xE9], a[0xEA], a[0xEB], a[0xEC], a[0xED], a[0xEE], a[0xEF],
    a[0xF0], a[0xF1], a[0xF2], a[0xF3], a[0xF4], a[0xF5], a[0xF6], a[0xF7],
    // </editor-fold>
    a[0xF8], a[0xF9], a[0xFA], a[0xFB]);
        assertEquals(r0, r);
        r = ximh.invokeWithArguments(cat(mh,a));
        assertEquals(r0, r);
        MethodHandle gimh = MethodHandles.invoker(mh.type());
        r = gimh.invoke(mh,
    // <editor-fold defaultstate="collapsed" desc="(Number) a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], ...">
    (Number)
    a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], a[0x05], a[0x06], a[0x07], a[0x08], a[0x09], a[0x0A], a[0x0B], a[0x0C], a[0x0D], a[0x0E], a[0x0F],
    a[0x10], a[0x11], a[0x12], a[0x13], a[0x14], a[0x15], a[0x16], a[0x17], a[0x18], a[0x19], a[0x1A], a[0x1B], a[0x1C], a[0x1D], a[0x1E], a[0x1F],
    a[0x20], a[0x21], a[0x22], a[0x23], a[0x24], a[0x25], a[0x26], a[0x27], a[0x28], a[0x29], a[0x2A], a[0x2B], a[0x2C], a[0x2D], a[0x2E], a[0x2F],
    a[0x30], a[0x31], a[0x32], a[0x33], a[0x34], a[0x35], a[0x36], a[0x37], a[0x38], a[0x39], a[0x3A], a[0x3B], a[0x3C], a[0x3D], a[0x3E], a[0x3F],
    a[0x40], a[0x41], a[0x42], a[0x43], a[0x44], a[0x45], a[0x46], a[0x47], a[0x48], a[0x49], a[0x4A], a[0x4B], a[0x4C], a[0x4D], a[0x4E], a[0x4F],
    a[0x50], a[0x51], a[0x52], a[0x53], a[0x54], a[0x55], a[0x56], a[0x57], a[0x58], a[0x59], a[0x5A], a[0x5B], a[0x5C], a[0x5D], a[0x5E], a[0x5F],
    a[0x60], a[0x61], a[0x62], a[0x63], a[0x64], a[0x65], a[0x66], a[0x67], a[0x68], a[0x69], a[0x6A], a[0x6B], a[0x6C], a[0x6D], a[0x6E], a[0x6F],
    a[0x70], a[0x71], a[0x72], a[0x73], a[0x74], a[0x75], a[0x76], a[0x77], a[0x78], a[0x79], a[0x7A], a[0x7B], a[0x7C], a[0x7D], a[0x7E], a[0x7F],
    a[0x80], a[0x81], a[0x82], a[0x83], a[0x84], a[0x85], a[0x86], a[0x87], a[0x88], a[0x89], a[0x8A], a[0x8B], a[0x8C], a[0x8D], a[0x8E], a[0x8F],
    a[0x90], a[0x91], a[0x92], a[0x93], a[0x94], a[0x95], a[0x96], a[0x97], a[0x98], a[0x99], a[0x9A], a[0x9B], a[0x9C], a[0x9D], a[0x9E], a[0x9F],
    a[0xA0], a[0xA1], a[0xA2], a[0xA3], a[0xA4], a[0xA5], a[0xA6], a[0xA7], a[0xA8], a[0xA9], a[0xAA], a[0xAB], a[0xAC], a[0xAD], a[0xAE], a[0xAF],
    a[0xB0], a[0xB1], a[0xB2], a[0xB3], a[0xB4], a[0xB5], a[0xB6], a[0xB7], a[0xB8], a[0xB9], a[0xBA], a[0xBB], a[0xBC], a[0xBD], a[0xBE], a[0xBF],
    a[0xC0], a[0xC1], a[0xC2], a[0xC3], a[0xC4], a[0xC5], a[0xC6], a[0xC7], a[0xC8], a[0xC9], a[0xCA], a[0xCB], a[0xCC], a[0xCD], a[0xCE], a[0xCF],
    a[0xD0], a[0xD1], a[0xD2], a[0xD3], a[0xD4], a[0xD5], a[0xD6], a[0xD7], a[0xD8], a[0xD9], a[0xDA], a[0xDB], a[0xDC], a[0xDD], a[0xDE], a[0xDF],
    a[0xE0], a[0xE1], a[0xE2], a[0xE3], a[0xE4], a[0xE5], a[0xE6], a[0xE7], a[0xE8], a[0xE9], a[0xEA], a[0xEB], a[0xEC], a[0xED], a[0xEE], a[0xEF],
    a[0xF0], a[0xF1], a[0xF2], a[0xF3], a[0xF4], a[0xF5], a[0xF6], a[0xF7],
    // </editor-fold>
    a[0xF8], a[0xF9], a[0xFA], a[0xFB]);
        assertEquals(r0, r);
        r = gimh.invokeWithArguments(cat(mh,a));
        assertEquals(r0, r);
        mh = mh.asType(mh.type().changeParameterType(0x10, Integer.class));
        //System.out.println("type="+mh.type().toString().replaceAll("Object", ""));
        r = mh.invokeExact(
    // <editor-fold defaultstate="collapsed" desc="a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], ... (Integer) a[0x10], ...">
    a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], a[0x05], a[0x06], a[0x07], a[0x08], a[0x09], a[0x0A], a[0x0B], a[0x0C], a[0x0D], a[0x0E], a[0x0F],
    (Integer)
    a[0x10], a[0x11], a[0x12], a[0x13], a[0x14], a[0x15], a[0x16], a[0x17], a[0x18], a[0x19], a[0x1A], a[0x1B], a[0x1C], a[0x1D], a[0x1E], a[0x1F],
    a[0x20], a[0x21], a[0x22], a[0x23], a[0x24], a[0x25], a[0x26], a[0x27], a[0x28], a[0x29], a[0x2A], a[0x2B], a[0x2C], a[0x2D], a[0x2E], a[0x2F],
    a[0x30], a[0x31], a[0x32], a[0x33], a[0x34], a[0x35], a[0x36], a[0x37], a[0x38], a[0x39], a[0x3A], a[0x3B], a[0x3C], a[0x3D], a[0x3E], a[0x3F],
    a[0x40], a[0x41], a[0x42], a[0x43], a[0x44], a[0x45], a[0x46], a[0x47], a[0x48], a[0x49], a[0x4A], a[0x4B], a[0x4C], a[0x4D], a[0x4E], a[0x4F],
    a[0x50], a[0x51], a[0x52], a[0x53], a[0x54], a[0x55], a[0x56], a[0x57], a[0x58], a[0x59], a[0x5A], a[0x5B], a[0x5C], a[0x5D], a[0x5E], a[0x5F],
    a[0x60], a[0x61], a[0x62], a[0x63], a[0x64], a[0x65], a[0x66], a[0x67], a[0x68], a[0x69], a[0x6A], a[0x6B], a[0x6C], a[0x6D], a[0x6E], a[0x6F],
    a[0x70], a[0x71], a[0x72], a[0x73], a[0x74], a[0x75], a[0x76], a[0x77], a[0x78], a[0x79], a[0x7A], a[0x7B], a[0x7C], a[0x7D], a[0x7E], a[0x7F],
    a[0x80], a[0x81], a[0x82], a[0x83], a[0x84], a[0x85], a[0x86], a[0x87], a[0x88], a[0x89], a[0x8A], a[0x8B], a[0x8C], a[0x8D], a[0x8E], a[0x8F],
    a[0x90], a[0x91], a[0x92], a[0x93], a[0x94], a[0x95], a[0x96], a[0x97], a[0x98], a[0x99], a[0x9A], a[0x9B], a[0x9C], a[0x9D], a[0x9E], a[0x9F],
    a[0xA0], a[0xA1], a[0xA2], a[0xA3], a[0xA4], a[0xA5], a[0xA6], a[0xA7], a[0xA8], a[0xA9], a[0xAA], a[0xAB], a[0xAC], a[0xAD], a[0xAE], a[0xAF],
    a[0xB0], a[0xB1], a[0xB2], a[0xB3], a[0xB4], a[0xB5], a[0xB6], a[0xB7], a[0xB8], a[0xB9], a[0xBA], a[0xBB], a[0xBC], a[0xBD], a[0xBE], a[0xBF],
    a[0xC0], a[0xC1], a[0xC2], a[0xC3], a[0xC4], a[0xC5], a[0xC6], a[0xC7], a[0xC8], a[0xC9], a[0xCA], a[0xCB], a[0xCC], a[0xCD], a[0xCE], a[0xCF],
    a[0xD0], a[0xD1], a[0xD2], a[0xD3], a[0xD4], a[0xD5], a[0xD6], a[0xD7], a[0xD8], a[0xD9], a[0xDA], a[0xDB], a[0xDC], a[0xDD], a[0xDE], a[0xDF],
    a[0xE0], a[0xE1], a[0xE2], a[0xE3], a[0xE4], a[0xE5], a[0xE6], a[0xE7], a[0xE8], a[0xE9], a[0xEA], a[0xEB], a[0xEC], a[0xED], a[0xEE], a[0xEF],
    a[0xF0], a[0xF1], a[0xF2], a[0xF3], a[0xF4], a[0xF5], a[0xF6], a[0xF7],
    // </editor-fold>
    a[0xF8], a[0xF9], a[0xFA], a[0xFB]);
        assertEquals(r0, r);
        r = mh.invoke(
    // <editor-fold defaultstate="collapsed" desc="(Comparable) a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], ...">
    (Comparable<?>)
    a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], a[0x05], a[0x06], a[0x07], a[0x08], a[0x09], a[0x0A], a[0x0B], a[0x0C], a[0x0D], a[0x0E], a[0x0F],
    a[0x10], a[0x11], a[0x12], a[0x13], a[0x14], a[0x15], a[0x16], a[0x17], a[0x18], a[0x19], a[0x1A], a[0x1B], a[0x1C], a[0x1D], a[0x1E], a[0x1F],
    a[0x20], a[0x21], a[0x22], a[0x23], a[0x24], a[0x25], a[0x26], a[0x27], a[0x28], a[0x29], a[0x2A], a[0x2B], a[0x2C], a[0x2D], a[0x2E], a[0x2F],
    a[0x30], a[0x31], a[0x32], a[0x33], a[0x34], a[0x35], a[0x36], a[0x37], a[0x38], a[0x39], a[0x3A], a[0x3B], a[0x3C], a[0x3D], a[0x3E], a[0x3F],
    a[0x40], a[0x41], a[0x42], a[0x43], a[0x44], a[0x45], a[0x46], a[0x47], a[0x48], a[0x49], a[0x4A], a[0x4B], a[0x4C], a[0x4D], a[0x4E], a[0x4F],
    a[0x50], a[0x51], a[0x52], a[0x53], a[0x54], a[0x55], a[0x56], a[0x57], a[0x58], a[0x59], a[0x5A], a[0x5B], a[0x5C], a[0x5D], a[0x5E], a[0x5F],
    a[0x60], a[0x61], a[0x62], a[0x63], a[0x64], a[0x65], a[0x66], a[0x67], a[0x68], a[0x69], a[0x6A], a[0x6B], a[0x6C], a[0x6D], a[0x6E], a[0x6F],
    a[0x70], a[0x71], a[0x72], a[0x73], a[0x74], a[0x75], a[0x76], a[0x77], a[0x78], a[0x79], a[0x7A], a[0x7B], a[0x7C], a[0x7D], a[0x7E], a[0x7F],
    a[0x80], a[0x81], a[0x82], a[0x83], a[0x84], a[0x85], a[0x86], a[0x87], a[0x88], a[0x89], a[0x8A], a[0x8B], a[0x8C], a[0x8D], a[0x8E], a[0x8F],
    a[0x90], a[0x91], a[0x92], a[0x93], a[0x94], a[0x95], a[0x96], a[0x97], a[0x98], a[0x99], a[0x9A], a[0x9B], a[0x9C], a[0x9D], a[0x9E], a[0x9F],
    a[0xA0], a[0xA1], a[0xA2], a[0xA3], a[0xA4], a[0xA5], a[0xA6], a[0xA7], a[0xA8], a[0xA9], a[0xAA], a[0xAB], a[0xAC], a[0xAD], a[0xAE], a[0xAF],
    a[0xB0], a[0xB1], a[0xB2], a[0xB3], a[0xB4], a[0xB5], a[0xB6], a[0xB7], a[0xB8], a[0xB9], a[0xBA], a[0xBB], a[0xBC], a[0xBD], a[0xBE], a[0xBF],
    a[0xC0], a[0xC1], a[0xC2], a[0xC3], a[0xC4], a[0xC5], a[0xC6], a[0xC7], a[0xC8], a[0xC9], a[0xCA], a[0xCB], a[0xCC], a[0xCD], a[0xCE], a[0xCF],
    a[0xD0], a[0xD1], a[0xD2], a[0xD3], a[0xD4], a[0xD5], a[0xD6], a[0xD7], a[0xD8], a[0xD9], a[0xDA], a[0xDB], a[0xDC], a[0xDD], a[0xDE], a[0xDF],
    a[0xE0], a[0xE1], a[0xE2], a[0xE3], a[0xE4], a[0xE5], a[0xE6], a[0xE7], a[0xE8], a[0xE9], a[0xEA], a[0xEB], a[0xEC], a[0xED], a[0xEE], a[0xEF],
    a[0xF0], a[0xF1], a[0xF2], a[0xF3], a[0xF4], a[0xF5], a[0xF6], a[0xF7],
    // </editor-fold>
    a[0xF8], a[0xF9], a[0xFA], a[0xFB]);
        assertEquals(r0, r);
    }

    static Object hashArguments_253(
    // <editor-fold defaultstate="collapsed" desc="Object x00, Object x01, Object x02, Object x03, Object x04, ...">
    Object x00, Object x01, Object x02, Object x03, Object x04, Object x05, Object x06, Object x07,
    Object x08, Object x09, Object x0A, Object x0B, Object x0C, Object x0D, Object x0E, Object x0F,
    Object x10, Object x11, Object x12, Object x13, Object x14, Object x15, Object x16, Object x17,
    Object x18, Object x19, Object x1A, Object x1B, Object x1C, Object x1D, Object x1E, Object x1F,
    Object x20, Object x21, Object x22, Object x23, Object x24, Object x25, Object x26, Object x27,
    Object x28, Object x29, Object x2A, Object x2B, Object x2C, Object x2D, Object x2E, Object x2F,
    Object x30, Object x31, Object x32, Object x33, Object x34, Object x35, Object x36, Object x37,
    Object x38, Object x39, Object x3A, Object x3B, Object x3C, Object x3D, Object x3E, Object x3F,
    Object x40, Object x41, Object x42, Object x43, Object x44, Object x45, Object x46, Object x47,
    Object x48, Object x49, Object x4A, Object x4B, Object x4C, Object x4D, Object x4E, Object x4F,
    Object x50, Object x51, Object x52, Object x53, Object x54, Object x55, Object x56, Object x57,
    Object x58, Object x59, Object x5A, Object x5B, Object x5C, Object x5D, Object x5E, Object x5F,
    Object x60, Object x61, Object x62, Object x63, Object x64, Object x65, Object x66, Object x67,
    Object x68, Object x69, Object x6A, Object x6B, Object x6C, Object x6D, Object x6E, Object x6F,
    Object x70, Object x71, Object x72, Object x73, Object x74, Object x75, Object x76, Object x77,
    Object x78, Object x79, Object x7A, Object x7B, Object x7C, Object x7D, Object x7E, Object x7F,
    Object x80, Object x81, Object x82, Object x83, Object x84, Object x85, Object x86, Object x87,
    Object x88, Object x89, Object x8A, Object x8B, Object x8C, Object x8D, Object x8E, Object x8F,
    Object x90, Object x91, Object x92, Object x93, Object x94, Object x95, Object x96, Object x97,
    Object x98, Object x99, Object x9A, Object x9B, Object x9C, Object x9D, Object x9E, Object x9F,
    Object xA0, Object xA1, Object xA2, Object xA3, Object xA4, Object xA5, Object xA6, Object xA7,
    Object xA8, Object xA9, Object xAA, Object xAB, Object xAC, Object xAD, Object xAE, Object xAF,
    Object xB0, Object xB1, Object xB2, Object xB3, Object xB4, Object xB5, Object xB6, Object xB7,
    Object xB8, Object xB9, Object xBA, Object xBB, Object xBC, Object xBD, Object xBE, Object xBF,
    Object xC0, Object xC1, Object xC2, Object xC3, Object xC4, Object xC5, Object xC6, Object xC7,
    Object xC8, Object xC9, Object xCA, Object xCB, Object xCC, Object xCD, Object xCE, Object xCF,
    Object xD0, Object xD1, Object xD2, Object xD3, Object xD4, Object xD5, Object xD6, Object xD7,
    Object xD8, Object xD9, Object xDA, Object xDB, Object xDC, Object xDD, Object xDE, Object xDF,
    Object xE0, Object xE1, Object xE2, Object xE3, Object xE4, Object xE5, Object xE6, Object xE7,
    Object xE8, Object xE9, Object xEA, Object xEB, Object xEC, Object xED, Object xEE, Object xEF,
    Object xF0, Object xF1, Object xF2, Object xF3, Object xF4, Object xF5, Object xF6, Object xF7,
    // </editor-fold>
    Object xF8, Object xF9, Object xFA, Object xFB, Object xFC) {
        return Objects.hash(
    // <editor-fold defaultstate="collapsed" desc="x00, x01, x02, x03, x04, ...">
    x00, x01, x02, x03, x04, x05, x06, x07, x08, x09, x0A, x0B, x0C, x0D, x0E, x0F,
    x10, x11, x12, x13, x14, x15, x16, x17, x18, x19, x1A, x1B, x1C, x1D, x1E, x1F,
    x20, x21, x22, x23, x24, x25, x26, x27, x28, x29, x2A, x2B, x2C, x2D, x2E, x2F,
    x30, x31, x32, x33, x34, x35, x36, x37, x38, x39, x3A, x3B, x3C, x3D, x3E, x3F,
    x40, x41, x42, x43, x44, x45, x46, x47, x48, x49, x4A, x4B, x4C, x4D, x4E, x4F,
    x50, x51, x52, x53, x54, x55, x56, x57, x58, x59, x5A, x5B, x5C, x5D, x5E, x5F,
    x60, x61, x62, x63, x64, x65, x66, x67, x68, x69, x6A, x6B, x6C, x6D, x6E, x6F,
    x70, x71, x72, x73, x74, x75, x76, x77, x78, x79, x7A, x7B, x7C, x7D, x7E, x7F,
    x80, x81, x82, x83, x84, x85, x86, x87, x88, x89, x8A, x8B, x8C, x8D, x8E, x8F,
    x90, x91, x92, x93, x94, x95, x96, x97, x98, x99, x9A, x9B, x9C, x9D, x9E, x9F,
    xA0, xA1, xA2, xA3, xA4, xA5, xA6, xA7, xA8, xA9, xAA, xAB, xAC, xAD, xAE, xAF,
    xB0, xB1, xB2, xB3, xB4, xB5, xB6, xB7, xB8, xB9, xBA, xBB, xBC, xBD, xBE, xBF,
    xC0, xC1, xC2, xC3, xC4, xC5, xC6, xC7, xC8, xC9, xCA, xCB, xCC, xCD, xCE, xCF,
    xD0, xD1, xD2, xD3, xD4, xD5, xD6, xD7, xD8, xD9, xDA, xDB, xDC, xDD, xDE, xDF,
    xE0, xE1, xE2, xE3, xE4, xE5, xE6, xE7, xE8, xE9, xEA, xEB, xEC, xED, xEE, xEF,
    xF0, xF1, xF2, xF3, xF4, xF5, xF6, xF7,
    // </editor-fold>
    xF8, xF9, xFA, xFB, xFC);
    }
    static Object hashArguments_253_a(Object x00, Object[] x01_FB, Object xFC) {
        return Objects.hash(
                // <editor-fold defaultstate="collapsed" desc="x00, x01_FB[0], x01_FB[1], x01_FB[2], ...">
                x00, x01_FB[0], x01_FB[1], x01_FB[2], x01_FB[3], x01_FB[4], x01_FB[5], x01_FB[6], x01_FB[7], x01_FB[8],
                x01_FB[9], x01_FB[10], x01_FB[11], x01_FB[12], x01_FB[13], x01_FB[14], x01_FB[15], x01_FB[16],
                x01_FB[17], x01_FB[18], x01_FB[19], x01_FB[20], x01_FB[21], x01_FB[22], x01_FB[23], x01_FB[24],
                x01_FB[25], x01_FB[26], x01_FB[27], x01_FB[28], x01_FB[29], x01_FB[30], x01_FB[31], x01_FB[32],
                x01_FB[33], x01_FB[34], x01_FB[35], x01_FB[36], x01_FB[37], x01_FB[38], x01_FB[39], x01_FB[40],
                x01_FB[41], x01_FB[42], x01_FB[43], x01_FB[44], x01_FB[45], x01_FB[46], x01_FB[47], x01_FB[48],
                x01_FB[49], x01_FB[50], x01_FB[51], x01_FB[52], x01_FB[53], x01_FB[54], x01_FB[55], x01_FB[56],
                x01_FB[57], x01_FB[58], x01_FB[59], x01_FB[60], x01_FB[61], x01_FB[62], x01_FB[63], x01_FB[64],
                x01_FB[65], x01_FB[66], x01_FB[67], x01_FB[68], x01_FB[69], x01_FB[70], x01_FB[71], x01_FB[72],
                x01_FB[73], x01_FB[74], x01_FB[75], x01_FB[76], x01_FB[77], x01_FB[78], x01_FB[79], x01_FB[80],
                x01_FB[81], x01_FB[82], x01_FB[83], x01_FB[84], x01_FB[85], x01_FB[86], x01_FB[87], x01_FB[88],
                x01_FB[89], x01_FB[90], x01_FB[91], x01_FB[92], x01_FB[93], x01_FB[94], x01_FB[95], x01_FB[96],
                x01_FB[97], x01_FB[98], x01_FB[99], x01_FB[100], x01_FB[101], x01_FB[102], x01_FB[103], x01_FB[104],
                x01_FB[105], x01_FB[106], x01_FB[107], x01_FB[108], x01_FB[109], x01_FB[110], x01_FB[111], x01_FB[112],
                x01_FB[113], x01_FB[114], x01_FB[115], x01_FB[116], x01_FB[117], x01_FB[118], x01_FB[119], x01_FB[120],
                x01_FB[121], x01_FB[122], x01_FB[123], x01_FB[124], x01_FB[125], x01_FB[126], x01_FB[127], x01_FB[128],
                x01_FB[129], x01_FB[130], x01_FB[131], x01_FB[132], x01_FB[133], x01_FB[134], x01_FB[135], x01_FB[136],
                x01_FB[137], x01_FB[138], x01_FB[139], x01_FB[140], x01_FB[141], x01_FB[142], x01_FB[143], x01_FB[144],
                x01_FB[145], x01_FB[146], x01_FB[147], x01_FB[148], x01_FB[149], x01_FB[150], x01_FB[151], x01_FB[152],
                x01_FB[153], x01_FB[154], x01_FB[155], x01_FB[156], x01_FB[157], x01_FB[158], x01_FB[159], x01_FB[160],
                x01_FB[161], x01_FB[162], x01_FB[163], x01_FB[164], x01_FB[165], x01_FB[166], x01_FB[167], x01_FB[168],
                x01_FB[169], x01_FB[170], x01_FB[171], x01_FB[172], x01_FB[173], x01_FB[174], x01_FB[175], x01_FB[176],
                x01_FB[177], x01_FB[178], x01_FB[179], x01_FB[180], x01_FB[181], x01_FB[182], x01_FB[183], x01_FB[184],
                x01_FB[185], x01_FB[186], x01_FB[187], x01_FB[188], x01_FB[189], x01_FB[190], x01_FB[191], x01_FB[192],
                x01_FB[193], x01_FB[194], x01_FB[195], x01_FB[196], x01_FB[197], x01_FB[198], x01_FB[199], x01_FB[200],
                x01_FB[201], x01_FB[202], x01_FB[203], x01_FB[204], x01_FB[205], x01_FB[206], x01_FB[207], x01_FB[208],
                x01_FB[209], x01_FB[210], x01_FB[211], x01_FB[212], x01_FB[213], x01_FB[214], x01_FB[215], x01_FB[216],
                x01_FB[217], x01_FB[218], x01_FB[219], x01_FB[220], x01_FB[221], x01_FB[222], x01_FB[223], x01_FB[224],
                x01_FB[225], x01_FB[226], x01_FB[227], x01_FB[228], x01_FB[229], x01_FB[230], x01_FB[231], x01_FB[232],
                x01_FB[233], x01_FB[234], x01_FB[235], x01_FB[236], x01_FB[237], x01_FB[238], x01_FB[239], x01_FB[240],
                x01_FB[241], x01_FB[242], x01_FB[243], x01_FB[244], x01_FB[245], x01_FB[246], x01_FB[247], x01_FB[248],
                // </editor-fold>
                x01_FB[249], x01_FB[250], xFC);
    }

    @Test
    public void test253() throws Throwable {
        final int ARITY = 253;
        System.out.println("test"+ARITY);
        Object[] a = testArgs(ARITY);
        Object r0 = hashArguments(a);
        Object r;
        r = hashArguments_253(
    // <editor-fold defaultstate="collapsed" desc="a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], ...">
    a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], a[0x05], a[0x06], a[0x07], a[0x08], a[0x09], a[0x0A], a[0x0B], a[0x0C], a[0x0D], a[0x0E], a[0x0F],
    a[0x10], a[0x11], a[0x12], a[0x13], a[0x14], a[0x15], a[0x16], a[0x17], a[0x18], a[0x19], a[0x1A], a[0x1B], a[0x1C], a[0x1D], a[0x1E], a[0x1F],
    a[0x20], a[0x21], a[0x22], a[0x23], a[0x24], a[0x25], a[0x26], a[0x27], a[0x28], a[0x29], a[0x2A], a[0x2B], a[0x2C], a[0x2D], a[0x2E], a[0x2F],
    a[0x30], a[0x31], a[0x32], a[0x33], a[0x34], a[0x35], a[0x36], a[0x37], a[0x38], a[0x39], a[0x3A], a[0x3B], a[0x3C], a[0x3D], a[0x3E], a[0x3F],
    a[0x40], a[0x41], a[0x42], a[0x43], a[0x44], a[0x45], a[0x46], a[0x47], a[0x48], a[0x49], a[0x4A], a[0x4B], a[0x4C], a[0x4D], a[0x4E], a[0x4F],
    a[0x50], a[0x51], a[0x52], a[0x53], a[0x54], a[0x55], a[0x56], a[0x57], a[0x58], a[0x59], a[0x5A], a[0x5B], a[0x5C], a[0x5D], a[0x5E], a[0x5F],
    a[0x60], a[0x61], a[0x62], a[0x63], a[0x64], a[0x65], a[0x66], a[0x67], a[0x68], a[0x69], a[0x6A], a[0x6B], a[0x6C], a[0x6D], a[0x6E], a[0x6F],
    a[0x70], a[0x71], a[0x72], a[0x73], a[0x74], a[0x75], a[0x76], a[0x77], a[0x78], a[0x79], a[0x7A], a[0x7B], a[0x7C], a[0x7D], a[0x7E], a[0x7F],
    a[0x80], a[0x81], a[0x82], a[0x83], a[0x84], a[0x85], a[0x86], a[0x87], a[0x88], a[0x89], a[0x8A], a[0x8B], a[0x8C], a[0x8D], a[0x8E], a[0x8F],
    a[0x90], a[0x91], a[0x92], a[0x93], a[0x94], a[0x95], a[0x96], a[0x97], a[0x98], a[0x99], a[0x9A], a[0x9B], a[0x9C], a[0x9D], a[0x9E], a[0x9F],
    a[0xA0], a[0xA1], a[0xA2], a[0xA3], a[0xA4], a[0xA5], a[0xA6], a[0xA7], a[0xA8], a[0xA9], a[0xAA], a[0xAB], a[0xAC], a[0xAD], a[0xAE], a[0xAF],
    a[0xB0], a[0xB1], a[0xB2], a[0xB3], a[0xB4], a[0xB5], a[0xB6], a[0xB7], a[0xB8], a[0xB9], a[0xBA], a[0xBB], a[0xBC], a[0xBD], a[0xBE], a[0xBF],
    a[0xC0], a[0xC1], a[0xC2], a[0xC3], a[0xC4], a[0xC5], a[0xC6], a[0xC7], a[0xC8], a[0xC9], a[0xCA], a[0xCB], a[0xCC], a[0xCD], a[0xCE], a[0xCF],
    a[0xD0], a[0xD1], a[0xD2], a[0xD3], a[0xD4], a[0xD5], a[0xD6], a[0xD7], a[0xD8], a[0xD9], a[0xDA], a[0xDB], a[0xDC], a[0xDD], a[0xDE], a[0xDF],
    a[0xE0], a[0xE1], a[0xE2], a[0xE3], a[0xE4], a[0xE5], a[0xE6], a[0xE7], a[0xE8], a[0xE9], a[0xEA], a[0xEB], a[0xEC], a[0xED], a[0xEE], a[0xEF],
    a[0xF0], a[0xF1], a[0xF2], a[0xF3], a[0xF4], a[0xF5], a[0xF6], a[0xF7],
    // </editor-fold>
    a[0xF8], a[0xF9], a[0xFA], a[0xFB], a[0xFC]); // hashArguments_253
        assertEquals(r0, r);
        MethodType mt = MethodType.genericMethodType(ARITY);
        MethodHandle mh = MethodHandles.lookup().findStatic(BigArityTest.class, "hashArguments_"+ARITY, mt);
        test253(mh, a, r0);
        MethodHandle mh_CA = MH_hashArguments_VA.asFixedArity().asCollector(Object[].class, ARITY);
        test253(mh_CA, a, r0);
        MethodHandle mh_a = MethodHandles.lookup().findStatic(BigArityTest.class, "hashArguments_"+ARITY+"_a", MT_A).asCollector(1, Object[].class, ARITY-2);
        test253(mh_a, a, r0);
    }
    public void test253(MethodHandle mh, Object[] a, Object r0) throws Throwable {
        Object r;
        r = mh.invokeExact(
    // <editor-fold defaultstate="collapsed" desc="a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], ...">
    a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], a[0x05], a[0x06], a[0x07], a[0x08], a[0x09], a[0x0A], a[0x0B], a[0x0C], a[0x0D], a[0x0E], a[0x0F],
    a[0x10], a[0x11], a[0x12], a[0x13], a[0x14], a[0x15], a[0x16], a[0x17], a[0x18], a[0x19], a[0x1A], a[0x1B], a[0x1C], a[0x1D], a[0x1E], a[0x1F],
    a[0x20], a[0x21], a[0x22], a[0x23], a[0x24], a[0x25], a[0x26], a[0x27], a[0x28], a[0x29], a[0x2A], a[0x2B], a[0x2C], a[0x2D], a[0x2E], a[0x2F],
    a[0x30], a[0x31], a[0x32], a[0x33], a[0x34], a[0x35], a[0x36], a[0x37], a[0x38], a[0x39], a[0x3A], a[0x3B], a[0x3C], a[0x3D], a[0x3E], a[0x3F],
    a[0x40], a[0x41], a[0x42], a[0x43], a[0x44], a[0x45], a[0x46], a[0x47], a[0x48], a[0x49], a[0x4A], a[0x4B], a[0x4C], a[0x4D], a[0x4E], a[0x4F],
    a[0x50], a[0x51], a[0x52], a[0x53], a[0x54], a[0x55], a[0x56], a[0x57], a[0x58], a[0x59], a[0x5A], a[0x5B], a[0x5C], a[0x5D], a[0x5E], a[0x5F],
    a[0x60], a[0x61], a[0x62], a[0x63], a[0x64], a[0x65], a[0x66], a[0x67], a[0x68], a[0x69], a[0x6A], a[0x6B], a[0x6C], a[0x6D], a[0x6E], a[0x6F],
    a[0x70], a[0x71], a[0x72], a[0x73], a[0x74], a[0x75], a[0x76], a[0x77], a[0x78], a[0x79], a[0x7A], a[0x7B], a[0x7C], a[0x7D], a[0x7E], a[0x7F],
    a[0x80], a[0x81], a[0x82], a[0x83], a[0x84], a[0x85], a[0x86], a[0x87], a[0x88], a[0x89], a[0x8A], a[0x8B], a[0x8C], a[0x8D], a[0x8E], a[0x8F],
    a[0x90], a[0x91], a[0x92], a[0x93], a[0x94], a[0x95], a[0x96], a[0x97], a[0x98], a[0x99], a[0x9A], a[0x9B], a[0x9C], a[0x9D], a[0x9E], a[0x9F],
    a[0xA0], a[0xA1], a[0xA2], a[0xA3], a[0xA4], a[0xA5], a[0xA6], a[0xA7], a[0xA8], a[0xA9], a[0xAA], a[0xAB], a[0xAC], a[0xAD], a[0xAE], a[0xAF],
    a[0xB0], a[0xB1], a[0xB2], a[0xB3], a[0xB4], a[0xB5], a[0xB6], a[0xB7], a[0xB8], a[0xB9], a[0xBA], a[0xBB], a[0xBC], a[0xBD], a[0xBE], a[0xBF],
    a[0xC0], a[0xC1], a[0xC2], a[0xC3], a[0xC4], a[0xC5], a[0xC6], a[0xC7], a[0xC8], a[0xC9], a[0xCA], a[0xCB], a[0xCC], a[0xCD], a[0xCE], a[0xCF],
    a[0xD0], a[0xD1], a[0xD2], a[0xD3], a[0xD4], a[0xD5], a[0xD6], a[0xD7], a[0xD8], a[0xD9], a[0xDA], a[0xDB], a[0xDC], a[0xDD], a[0xDE], a[0xDF],
    a[0xE0], a[0xE1], a[0xE2], a[0xE3], a[0xE4], a[0xE5], a[0xE6], a[0xE7], a[0xE8], a[0xE9], a[0xEA], a[0xEB], a[0xEC], a[0xED], a[0xEE], a[0xEF],
    a[0xF0], a[0xF1], a[0xF2], a[0xF3], a[0xF4], a[0xF5], a[0xF6], a[0xF7],
    // </editor-fold>
    a[0xF8], a[0xF9], a[0xFA], a[0xFB], a[0xFC]);
        assertEquals(r0, r);
        r = mh.invokeWithArguments(a);
        assertEquals(r0, r);
        MethodHandle ximh = MethodHandles.exactInvoker(mh.type());
        r = ximh.invokeExact(mh,
    // <editor-fold defaultstate="collapsed" desc="a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], ...">
    a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], a[0x05], a[0x06], a[0x07], a[0x08], a[0x09], a[0x0A], a[0x0B], a[0x0C], a[0x0D], a[0x0E], a[0x0F],
    a[0x10], a[0x11], a[0x12], a[0x13], a[0x14], a[0x15], a[0x16], a[0x17], a[0x18], a[0x19], a[0x1A], a[0x1B], a[0x1C], a[0x1D], a[0x1E], a[0x1F],
    a[0x20], a[0x21], a[0x22], a[0x23], a[0x24], a[0x25], a[0x26], a[0x27], a[0x28], a[0x29], a[0x2A], a[0x2B], a[0x2C], a[0x2D], a[0x2E], a[0x2F],
    a[0x30], a[0x31], a[0x32], a[0x33], a[0x34], a[0x35], a[0x36], a[0x37], a[0x38], a[0x39], a[0x3A], a[0x3B], a[0x3C], a[0x3D], a[0x3E], a[0x3F],
    a[0x40], a[0x41], a[0x42], a[0x43], a[0x44], a[0x45], a[0x46], a[0x47], a[0x48], a[0x49], a[0x4A], a[0x4B], a[0x4C], a[0x4D], a[0x4E], a[0x4F],
    a[0x50], a[0x51], a[0x52], a[0x53], a[0x54], a[0x55], a[0x56], a[0x57], a[0x58], a[0x59], a[0x5A], a[0x5B], a[0x5C], a[0x5D], a[0x5E], a[0x5F],
    a[0x60], a[0x61], a[0x62], a[0x63], a[0x64], a[0x65], a[0x66], a[0x67], a[0x68], a[0x69], a[0x6A], a[0x6B], a[0x6C], a[0x6D], a[0x6E], a[0x6F],
    a[0x70], a[0x71], a[0x72], a[0x73], a[0x74], a[0x75], a[0x76], a[0x77], a[0x78], a[0x79], a[0x7A], a[0x7B], a[0x7C], a[0x7D], a[0x7E], a[0x7F],
    a[0x80], a[0x81], a[0x82], a[0x83], a[0x84], a[0x85], a[0x86], a[0x87], a[0x88], a[0x89], a[0x8A], a[0x8B], a[0x8C], a[0x8D], a[0x8E], a[0x8F],
    a[0x90], a[0x91], a[0x92], a[0x93], a[0x94], a[0x95], a[0x96], a[0x97], a[0x98], a[0x99], a[0x9A], a[0x9B], a[0x9C], a[0x9D], a[0x9E], a[0x9F],
    a[0xA0], a[0xA1], a[0xA2], a[0xA3], a[0xA4], a[0xA5], a[0xA6], a[0xA7], a[0xA8], a[0xA9], a[0xAA], a[0xAB], a[0xAC], a[0xAD], a[0xAE], a[0xAF],
    a[0xB0], a[0xB1], a[0xB2], a[0xB3], a[0xB4], a[0xB5], a[0xB6], a[0xB7], a[0xB8], a[0xB9], a[0xBA], a[0xBB], a[0xBC], a[0xBD], a[0xBE], a[0xBF],
    a[0xC0], a[0xC1], a[0xC2], a[0xC3], a[0xC4], a[0xC5], a[0xC6], a[0xC7], a[0xC8], a[0xC9], a[0xCA], a[0xCB], a[0xCC], a[0xCD], a[0xCE], a[0xCF],
    a[0xD0], a[0xD1], a[0xD2], a[0xD3], a[0xD4], a[0xD5], a[0xD6], a[0xD7], a[0xD8], a[0xD9], a[0xDA], a[0xDB], a[0xDC], a[0xDD], a[0xDE], a[0xDF],
    a[0xE0], a[0xE1], a[0xE2], a[0xE3], a[0xE4], a[0xE5], a[0xE6], a[0xE7], a[0xE8], a[0xE9], a[0xEA], a[0xEB], a[0xEC], a[0xED], a[0xEE], a[0xEF],
    a[0xF0], a[0xF1], a[0xF2], a[0xF3], a[0xF4], a[0xF5], a[0xF6], a[0xF7],
    // </editor-fold>
    a[0xF8], a[0xF9], a[0xFA], a[0xFB], a[0xFC]);
        assertEquals(r0, r);
        r = ximh.invokeWithArguments(cat(mh,a));
        assertEquals(r0, r);
        MethodHandle gimh = MethodHandles.invoker(mh.type());
        r = gimh.invoke(mh,
    // <editor-fold defaultstate="collapsed" desc="(Number) a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], ...">
    (Number)
    a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], a[0x05], a[0x06], a[0x07], a[0x08], a[0x09], a[0x0A], a[0x0B], a[0x0C], a[0x0D], a[0x0E], a[0x0F],
    a[0x10], a[0x11], a[0x12], a[0x13], a[0x14], a[0x15], a[0x16], a[0x17], a[0x18], a[0x19], a[0x1A], a[0x1B], a[0x1C], a[0x1D], a[0x1E], a[0x1F],
    a[0x20], a[0x21], a[0x22], a[0x23], a[0x24], a[0x25], a[0x26], a[0x27], a[0x28], a[0x29], a[0x2A], a[0x2B], a[0x2C], a[0x2D], a[0x2E], a[0x2F],
    a[0x30], a[0x31], a[0x32], a[0x33], a[0x34], a[0x35], a[0x36], a[0x37], a[0x38], a[0x39], a[0x3A], a[0x3B], a[0x3C], a[0x3D], a[0x3E], a[0x3F],
    a[0x40], a[0x41], a[0x42], a[0x43], a[0x44], a[0x45], a[0x46], a[0x47], a[0x48], a[0x49], a[0x4A], a[0x4B], a[0x4C], a[0x4D], a[0x4E], a[0x4F],
    a[0x50], a[0x51], a[0x52], a[0x53], a[0x54], a[0x55], a[0x56], a[0x57], a[0x58], a[0x59], a[0x5A], a[0x5B], a[0x5C], a[0x5D], a[0x5E], a[0x5F],
    a[0x60], a[0x61], a[0x62], a[0x63], a[0x64], a[0x65], a[0x66], a[0x67], a[0x68], a[0x69], a[0x6A], a[0x6B], a[0x6C], a[0x6D], a[0x6E], a[0x6F],
    a[0x70], a[0x71], a[0x72], a[0x73], a[0x74], a[0x75], a[0x76], a[0x77], a[0x78], a[0x79], a[0x7A], a[0x7B], a[0x7C], a[0x7D], a[0x7E], a[0x7F],
    a[0x80], a[0x81], a[0x82], a[0x83], a[0x84], a[0x85], a[0x86], a[0x87], a[0x88], a[0x89], a[0x8A], a[0x8B], a[0x8C], a[0x8D], a[0x8E], a[0x8F],
    a[0x90], a[0x91], a[0x92], a[0x93], a[0x94], a[0x95], a[0x96], a[0x97], a[0x98], a[0x99], a[0x9A], a[0x9B], a[0x9C], a[0x9D], a[0x9E], a[0x9F],
    a[0xA0], a[0xA1], a[0xA2], a[0xA3], a[0xA4], a[0xA5], a[0xA6], a[0xA7], a[0xA8], a[0xA9], a[0xAA], a[0xAB], a[0xAC], a[0xAD], a[0xAE], a[0xAF],
    a[0xB0], a[0xB1], a[0xB2], a[0xB3], a[0xB4], a[0xB5], a[0xB6], a[0xB7], a[0xB8], a[0xB9], a[0xBA], a[0xBB], a[0xBC], a[0xBD], a[0xBE], a[0xBF],
    a[0xC0], a[0xC1], a[0xC2], a[0xC3], a[0xC4], a[0xC5], a[0xC6], a[0xC7], a[0xC8], a[0xC9], a[0xCA], a[0xCB], a[0xCC], a[0xCD], a[0xCE], a[0xCF],
    a[0xD0], a[0xD1], a[0xD2], a[0xD3], a[0xD4], a[0xD5], a[0xD6], a[0xD7], a[0xD8], a[0xD9], a[0xDA], a[0xDB], a[0xDC], a[0xDD], a[0xDE], a[0xDF],
    a[0xE0], a[0xE1], a[0xE2], a[0xE3], a[0xE4], a[0xE5], a[0xE6], a[0xE7], a[0xE8], a[0xE9], a[0xEA], a[0xEB], a[0xEC], a[0xED], a[0xEE], a[0xEF],
    a[0xF0], a[0xF1], a[0xF2], a[0xF3], a[0xF4], a[0xF5], a[0xF6], a[0xF7],
    // </editor-fold>
    a[0xF8], a[0xF9], a[0xFA], a[0xFB], a[0xFC]);
        assertEquals(r0, r);
        r = gimh.invokeWithArguments(cat(mh,a));
        assertEquals(r0, r);
        mh = mh.asType(mh.type().changeParameterType(0x10, Integer.class));
        //System.out.println("type="+mh.type().toString().replaceAll("Object", ""));
        r = mh.invokeExact(
    // <editor-fold defaultstate="collapsed" desc="a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], ... (Integer) a[0x10], ...">
    a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], a[0x05], a[0x06], a[0x07], a[0x08], a[0x09], a[0x0A], a[0x0B], a[0x0C], a[0x0D], a[0x0E], a[0x0F],
    (Integer)
    a[0x10], a[0x11], a[0x12], a[0x13], a[0x14], a[0x15], a[0x16], a[0x17], a[0x18], a[0x19], a[0x1A], a[0x1B], a[0x1C], a[0x1D], a[0x1E], a[0x1F],
    a[0x20], a[0x21], a[0x22], a[0x23], a[0x24], a[0x25], a[0x26], a[0x27], a[0x28], a[0x29], a[0x2A], a[0x2B], a[0x2C], a[0x2D], a[0x2E], a[0x2F],
    a[0x30], a[0x31], a[0x32], a[0x33], a[0x34], a[0x35], a[0x36], a[0x37], a[0x38], a[0x39], a[0x3A], a[0x3B], a[0x3C], a[0x3D], a[0x3E], a[0x3F],
    a[0x40], a[0x41], a[0x42], a[0x43], a[0x44], a[0x45], a[0x46], a[0x47], a[0x48], a[0x49], a[0x4A], a[0x4B], a[0x4C], a[0x4D], a[0x4E], a[0x4F],
    a[0x50], a[0x51], a[0x52], a[0x53], a[0x54], a[0x55], a[0x56], a[0x57], a[0x58], a[0x59], a[0x5A], a[0x5B], a[0x5C], a[0x5D], a[0x5E], a[0x5F],
    a[0x60], a[0x61], a[0x62], a[0x63], a[0x64], a[0x65], a[0x66], a[0x67], a[0x68], a[0x69], a[0x6A], a[0x6B], a[0x6C], a[0x6D], a[0x6E], a[0x6F],
    a[0x70], a[0x71], a[0x72], a[0x73], a[0x74], a[0x75], a[0x76], a[0x77], a[0x78], a[0x79], a[0x7A], a[0x7B], a[0x7C], a[0x7D], a[0x7E], a[0x7F],
    a[0x80], a[0x81], a[0x82], a[0x83], a[0x84], a[0x85], a[0x86], a[0x87], a[0x88], a[0x89], a[0x8A], a[0x8B], a[0x8C], a[0x8D], a[0x8E], a[0x8F],
    a[0x90], a[0x91], a[0x92], a[0x93], a[0x94], a[0x95], a[0x96], a[0x97], a[0x98], a[0x99], a[0x9A], a[0x9B], a[0x9C], a[0x9D], a[0x9E], a[0x9F],
    a[0xA0], a[0xA1], a[0xA2], a[0xA3], a[0xA4], a[0xA5], a[0xA6], a[0xA7], a[0xA8], a[0xA9], a[0xAA], a[0xAB], a[0xAC], a[0xAD], a[0xAE], a[0xAF],
    a[0xB0], a[0xB1], a[0xB2], a[0xB3], a[0xB4], a[0xB5], a[0xB6], a[0xB7], a[0xB8], a[0xB9], a[0xBA], a[0xBB], a[0xBC], a[0xBD], a[0xBE], a[0xBF],
    a[0xC0], a[0xC1], a[0xC2], a[0xC3], a[0xC4], a[0xC5], a[0xC6], a[0xC7], a[0xC8], a[0xC9], a[0xCA], a[0xCB], a[0xCC], a[0xCD], a[0xCE], a[0xCF],
    a[0xD0], a[0xD1], a[0xD2], a[0xD3], a[0xD4], a[0xD5], a[0xD6], a[0xD7], a[0xD8], a[0xD9], a[0xDA], a[0xDB], a[0xDC], a[0xDD], a[0xDE], a[0xDF],
    a[0xE0], a[0xE1], a[0xE2], a[0xE3], a[0xE4], a[0xE5], a[0xE6], a[0xE7], a[0xE8], a[0xE9], a[0xEA], a[0xEB], a[0xEC], a[0xED], a[0xEE], a[0xEF],
    a[0xF0], a[0xF1], a[0xF2], a[0xF3], a[0xF4], a[0xF5], a[0xF6], a[0xF7],
    // </editor-fold>
    a[0xF8], a[0xF9], a[0xFA], a[0xFB], a[0xFC]);
        assertEquals(r0, r);
        r = mh.invoke(
    // <editor-fold defaultstate="collapsed" desc="(Comparable) a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], ...">
    (Comparable<?>)
    a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], a[0x05], a[0x06], a[0x07], a[0x08], a[0x09], a[0x0A], a[0x0B], a[0x0C], a[0x0D], a[0x0E], a[0x0F],
    a[0x10], a[0x11], a[0x12], a[0x13], a[0x14], a[0x15], a[0x16], a[0x17], a[0x18], a[0x19], a[0x1A], a[0x1B], a[0x1C], a[0x1D], a[0x1E], a[0x1F],
    a[0x20], a[0x21], a[0x22], a[0x23], a[0x24], a[0x25], a[0x26], a[0x27], a[0x28], a[0x29], a[0x2A], a[0x2B], a[0x2C], a[0x2D], a[0x2E], a[0x2F],
    a[0x30], a[0x31], a[0x32], a[0x33], a[0x34], a[0x35], a[0x36], a[0x37], a[0x38], a[0x39], a[0x3A], a[0x3B], a[0x3C], a[0x3D], a[0x3E], a[0x3F],
    a[0x40], a[0x41], a[0x42], a[0x43], a[0x44], a[0x45], a[0x46], a[0x47], a[0x48], a[0x49], a[0x4A], a[0x4B], a[0x4C], a[0x4D], a[0x4E], a[0x4F],
    a[0x50], a[0x51], a[0x52], a[0x53], a[0x54], a[0x55], a[0x56], a[0x57], a[0x58], a[0x59], a[0x5A], a[0x5B], a[0x5C], a[0x5D], a[0x5E], a[0x5F],
    a[0x60], a[0x61], a[0x62], a[0x63], a[0x64], a[0x65], a[0x66], a[0x67], a[0x68], a[0x69], a[0x6A], a[0x6B], a[0x6C], a[0x6D], a[0x6E], a[0x6F],
    a[0x70], a[0x71], a[0x72], a[0x73], a[0x74], a[0x75], a[0x76], a[0x77], a[0x78], a[0x79], a[0x7A], a[0x7B], a[0x7C], a[0x7D], a[0x7E], a[0x7F],
    a[0x80], a[0x81], a[0x82], a[0x83], a[0x84], a[0x85], a[0x86], a[0x87], a[0x88], a[0x89], a[0x8A], a[0x8B], a[0x8C], a[0x8D], a[0x8E], a[0x8F],
    a[0x90], a[0x91], a[0x92], a[0x93], a[0x94], a[0x95], a[0x96], a[0x97], a[0x98], a[0x99], a[0x9A], a[0x9B], a[0x9C], a[0x9D], a[0x9E], a[0x9F],
    a[0xA0], a[0xA1], a[0xA2], a[0xA3], a[0xA4], a[0xA5], a[0xA6], a[0xA7], a[0xA8], a[0xA9], a[0xAA], a[0xAB], a[0xAC], a[0xAD], a[0xAE], a[0xAF],
    a[0xB0], a[0xB1], a[0xB2], a[0xB3], a[0xB4], a[0xB5], a[0xB6], a[0xB7], a[0xB8], a[0xB9], a[0xBA], a[0xBB], a[0xBC], a[0xBD], a[0xBE], a[0xBF],
    a[0xC0], a[0xC1], a[0xC2], a[0xC3], a[0xC4], a[0xC5], a[0xC6], a[0xC7], a[0xC8], a[0xC9], a[0xCA], a[0xCB], a[0xCC], a[0xCD], a[0xCE], a[0xCF],
    a[0xD0], a[0xD1], a[0xD2], a[0xD3], a[0xD4], a[0xD5], a[0xD6], a[0xD7], a[0xD8], a[0xD9], a[0xDA], a[0xDB], a[0xDC], a[0xDD], a[0xDE], a[0xDF],
    a[0xE0], a[0xE1], a[0xE2], a[0xE3], a[0xE4], a[0xE5], a[0xE6], a[0xE7], a[0xE8], a[0xE9], a[0xEA], a[0xEB], a[0xEC], a[0xED], a[0xEE], a[0xEF],
    a[0xF0], a[0xF1], a[0xF2], a[0xF3], a[0xF4], a[0xF5], a[0xF6], a[0xF7],
    // </editor-fold>
    a[0xF8], a[0xF9], a[0xFA], a[0xFB], a[0xFC]);
        assertEquals(r0, r);
    }

    static Object hashArguments_254(
    // <editor-fold defaultstate="collapsed" desc="Object x00, Object x01, Object x02, Object x03, Object x04, ...">
    Object x00, Object x01, Object x02, Object x03, Object x04, Object x05, Object x06, Object x07,
    Object x08, Object x09, Object x0A, Object x0B, Object x0C, Object x0D, Object x0E, Object x0F,
    Object x10, Object x11, Object x12, Object x13, Object x14, Object x15, Object x16, Object x17,
    Object x18, Object x19, Object x1A, Object x1B, Object x1C, Object x1D, Object x1E, Object x1F,
    Object x20, Object x21, Object x22, Object x23, Object x24, Object x25, Object x26, Object x27,
    Object x28, Object x29, Object x2A, Object x2B, Object x2C, Object x2D, Object x2E, Object x2F,
    Object x30, Object x31, Object x32, Object x33, Object x34, Object x35, Object x36, Object x37,
    Object x38, Object x39, Object x3A, Object x3B, Object x3C, Object x3D, Object x3E, Object x3F,
    Object x40, Object x41, Object x42, Object x43, Object x44, Object x45, Object x46, Object x47,
    Object x48, Object x49, Object x4A, Object x4B, Object x4C, Object x4D, Object x4E, Object x4F,
    Object x50, Object x51, Object x52, Object x53, Object x54, Object x55, Object x56, Object x57,
    Object x58, Object x59, Object x5A, Object x5B, Object x5C, Object x5D, Object x5E, Object x5F,
    Object x60, Object x61, Object x62, Object x63, Object x64, Object x65, Object x66, Object x67,
    Object x68, Object x69, Object x6A, Object x6B, Object x6C, Object x6D, Object x6E, Object x6F,
    Object x70, Object x71, Object x72, Object x73, Object x74, Object x75, Object x76, Object x77,
    Object x78, Object x79, Object x7A, Object x7B, Object x7C, Object x7D, Object x7E, Object x7F,
    Object x80, Object x81, Object x82, Object x83, Object x84, Object x85, Object x86, Object x87,
    Object x88, Object x89, Object x8A, Object x8B, Object x8C, Object x8D, Object x8E, Object x8F,
    Object x90, Object x91, Object x92, Object x93, Object x94, Object x95, Object x96, Object x97,
    Object x98, Object x99, Object x9A, Object x9B, Object x9C, Object x9D, Object x9E, Object x9F,
    Object xA0, Object xA1, Object xA2, Object xA3, Object xA4, Object xA5, Object xA6, Object xA7,
    Object xA8, Object xA9, Object xAA, Object xAB, Object xAC, Object xAD, Object xAE, Object xAF,
    Object xB0, Object xB1, Object xB2, Object xB3, Object xB4, Object xB5, Object xB6, Object xB7,
    Object xB8, Object xB9, Object xBA, Object xBB, Object xBC, Object xBD, Object xBE, Object xBF,
    Object xC0, Object xC1, Object xC2, Object xC3, Object xC4, Object xC5, Object xC6, Object xC7,
    Object xC8, Object xC9, Object xCA, Object xCB, Object xCC, Object xCD, Object xCE, Object xCF,
    Object xD0, Object xD1, Object xD2, Object xD3, Object xD4, Object xD5, Object xD6, Object xD7,
    Object xD8, Object xD9, Object xDA, Object xDB, Object xDC, Object xDD, Object xDE, Object xDF,
    Object xE0, Object xE1, Object xE2, Object xE3, Object xE4, Object xE5, Object xE6, Object xE7,
    Object xE8, Object xE9, Object xEA, Object xEB, Object xEC, Object xED, Object xEE, Object xEF,
    Object xF0, Object xF1, Object xF2, Object xF3, Object xF4, Object xF5, Object xF6, Object xF7,
    // </editor-fold>
    Object xF8, Object xF9, Object xFA, Object xFB, Object xFC, Object xFD) {
        return Objects.hash(
    // <editor-fold defaultstate="collapsed" desc="x00, x01, x02, x03, x04, ...">
    x00, x01, x02, x03, x04, x05, x06, x07, x08, x09, x0A, x0B, x0C, x0D, x0E, x0F,
    x10, x11, x12, x13, x14, x15, x16, x17, x18, x19, x1A, x1B, x1C, x1D, x1E, x1F,
    x20, x21, x22, x23, x24, x25, x26, x27, x28, x29, x2A, x2B, x2C, x2D, x2E, x2F,
    x30, x31, x32, x33, x34, x35, x36, x37, x38, x39, x3A, x3B, x3C, x3D, x3E, x3F,
    x40, x41, x42, x43, x44, x45, x46, x47, x48, x49, x4A, x4B, x4C, x4D, x4E, x4F,
    x50, x51, x52, x53, x54, x55, x56, x57, x58, x59, x5A, x5B, x5C, x5D, x5E, x5F,
    x60, x61, x62, x63, x64, x65, x66, x67, x68, x69, x6A, x6B, x6C, x6D, x6E, x6F,
    x70, x71, x72, x73, x74, x75, x76, x77, x78, x79, x7A, x7B, x7C, x7D, x7E, x7F,
    x80, x81, x82, x83, x84, x85, x86, x87, x88, x89, x8A, x8B, x8C, x8D, x8E, x8F,
    x90, x91, x92, x93, x94, x95, x96, x97, x98, x99, x9A, x9B, x9C, x9D, x9E, x9F,
    xA0, xA1, xA2, xA3, xA4, xA5, xA6, xA7, xA8, xA9, xAA, xAB, xAC, xAD, xAE, xAF,
    xB0, xB1, xB2, xB3, xB4, xB5, xB6, xB7, xB8, xB9, xBA, xBB, xBC, xBD, xBE, xBF,
    xC0, xC1, xC2, xC3, xC4, xC5, xC6, xC7, xC8, xC9, xCA, xCB, xCC, xCD, xCE, xCF,
    xD0, xD1, xD2, xD3, xD4, xD5, xD6, xD7, xD8, xD9, xDA, xDB, xDC, xDD, xDE, xDF,
    xE0, xE1, xE2, xE3, xE4, xE5, xE6, xE7, xE8, xE9, xEA, xEB, xEC, xED, xEE, xEF,
    xF0, xF1, xF2, xF3, xF4, xF5, xF6, xF7,
    // </editor-fold>
    xF8, xF9, xFA, xFB, xFC, xFD);
    }
    static Object hashArguments_254_a(Object x00, Object[] x01_FC, Object xFD) {
        return Objects.hash(
                // <editor-fold defaultstate="collapsed" desc="x00, x01_FC[0], x01_FC[1], x01_FC[2], ...">
                x00, x01_FC[0], x01_FC[1], x01_FC[2], x01_FC[3], x01_FC[4], x01_FC[5], x01_FC[6], x01_FC[7], x01_FC[8],
                x01_FC[9], x01_FC[10], x01_FC[11], x01_FC[12], x01_FC[13], x01_FC[14], x01_FC[15], x01_FC[16],
                x01_FC[17], x01_FC[18], x01_FC[19], x01_FC[20], x01_FC[21], x01_FC[22], x01_FC[23], x01_FC[24],
                x01_FC[25], x01_FC[26], x01_FC[27], x01_FC[28], x01_FC[29], x01_FC[30], x01_FC[31], x01_FC[32],
                x01_FC[33], x01_FC[34], x01_FC[35], x01_FC[36], x01_FC[37], x01_FC[38], x01_FC[39], x01_FC[40],
                x01_FC[41], x01_FC[42], x01_FC[43], x01_FC[44], x01_FC[45], x01_FC[46], x01_FC[47], x01_FC[48],
                x01_FC[49], x01_FC[50], x01_FC[51], x01_FC[52], x01_FC[53], x01_FC[54], x01_FC[55], x01_FC[56],
                x01_FC[57], x01_FC[58], x01_FC[59], x01_FC[60], x01_FC[61], x01_FC[62], x01_FC[63], x01_FC[64],
                x01_FC[65], x01_FC[66], x01_FC[67], x01_FC[68], x01_FC[69], x01_FC[70], x01_FC[71], x01_FC[72],
                x01_FC[73], x01_FC[74], x01_FC[75], x01_FC[76], x01_FC[77], x01_FC[78], x01_FC[79], x01_FC[80],
                x01_FC[81], x01_FC[82], x01_FC[83], x01_FC[84], x01_FC[85], x01_FC[86], x01_FC[87], x01_FC[88],
                x01_FC[89], x01_FC[90], x01_FC[91], x01_FC[92], x01_FC[93], x01_FC[94], x01_FC[95], x01_FC[96],
                x01_FC[97], x01_FC[98], x01_FC[99], x01_FC[100], x01_FC[101], x01_FC[102], x01_FC[103], x01_FC[104],
                x01_FC[105], x01_FC[106], x01_FC[107], x01_FC[108], x01_FC[109], x01_FC[110], x01_FC[111], x01_FC[112],
                x01_FC[113], x01_FC[114], x01_FC[115], x01_FC[116], x01_FC[117], x01_FC[118], x01_FC[119], x01_FC[120],
                x01_FC[121], x01_FC[122], x01_FC[123], x01_FC[124], x01_FC[125], x01_FC[126], x01_FC[127], x01_FC[128],
                x01_FC[129], x01_FC[130], x01_FC[131], x01_FC[132], x01_FC[133], x01_FC[134], x01_FC[135], x01_FC[136],
                x01_FC[137], x01_FC[138], x01_FC[139], x01_FC[140], x01_FC[141], x01_FC[142], x01_FC[143], x01_FC[144],
                x01_FC[145], x01_FC[146], x01_FC[147], x01_FC[148], x01_FC[149], x01_FC[150], x01_FC[151], x01_FC[152],
                x01_FC[153], x01_FC[154], x01_FC[155], x01_FC[156], x01_FC[157], x01_FC[158], x01_FC[159], x01_FC[160],
                x01_FC[161], x01_FC[162], x01_FC[163], x01_FC[164], x01_FC[165], x01_FC[166], x01_FC[167], x01_FC[168],
                x01_FC[169], x01_FC[170], x01_FC[171], x01_FC[172], x01_FC[173], x01_FC[174], x01_FC[175], x01_FC[176],
                x01_FC[177], x01_FC[178], x01_FC[179], x01_FC[180], x01_FC[181], x01_FC[182], x01_FC[183], x01_FC[184],
                x01_FC[185], x01_FC[186], x01_FC[187], x01_FC[188], x01_FC[189], x01_FC[190], x01_FC[191], x01_FC[192],
                x01_FC[193], x01_FC[194], x01_FC[195], x01_FC[196], x01_FC[197], x01_FC[198], x01_FC[199], x01_FC[200],
                x01_FC[201], x01_FC[202], x01_FC[203], x01_FC[204], x01_FC[205], x01_FC[206], x01_FC[207], x01_FC[208],
                x01_FC[209], x01_FC[210], x01_FC[211], x01_FC[212], x01_FC[213], x01_FC[214], x01_FC[215], x01_FC[216],
                x01_FC[217], x01_FC[218], x01_FC[219], x01_FC[220], x01_FC[221], x01_FC[222], x01_FC[223], x01_FC[224],
                x01_FC[225], x01_FC[226], x01_FC[227], x01_FC[228], x01_FC[229], x01_FC[230], x01_FC[231], x01_FC[232],
                x01_FC[233], x01_FC[234], x01_FC[235], x01_FC[236], x01_FC[237], x01_FC[238], x01_FC[239], x01_FC[240],
                x01_FC[241], x01_FC[242], x01_FC[243], x01_FC[244], x01_FC[245], x01_FC[246], x01_FC[247], x01_FC[248],
                // </editor-fold>
                x01_FC[249], x01_FC[250], x01_FC[251], xFD);
    }

    @Test
    public void test254() throws Throwable {
        final int ARITY = 254;
        System.out.println("test"+ARITY);
        Object[] a = testArgs(ARITY);
        Object r0 = hashArguments(a);
        Object r;
        r = hashArguments_254(
    // <editor-fold defaultstate="collapsed" desc="a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], ...">
    a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], a[0x05], a[0x06], a[0x07], a[0x08], a[0x09], a[0x0A], a[0x0B], a[0x0C], a[0x0D], a[0x0E], a[0x0F],
    a[0x10], a[0x11], a[0x12], a[0x13], a[0x14], a[0x15], a[0x16], a[0x17], a[0x18], a[0x19], a[0x1A], a[0x1B], a[0x1C], a[0x1D], a[0x1E], a[0x1F],
    a[0x20], a[0x21], a[0x22], a[0x23], a[0x24], a[0x25], a[0x26], a[0x27], a[0x28], a[0x29], a[0x2A], a[0x2B], a[0x2C], a[0x2D], a[0x2E], a[0x2F],
    a[0x30], a[0x31], a[0x32], a[0x33], a[0x34], a[0x35], a[0x36], a[0x37], a[0x38], a[0x39], a[0x3A], a[0x3B], a[0x3C], a[0x3D], a[0x3E], a[0x3F],
    a[0x40], a[0x41], a[0x42], a[0x43], a[0x44], a[0x45], a[0x46], a[0x47], a[0x48], a[0x49], a[0x4A], a[0x4B], a[0x4C], a[0x4D], a[0x4E], a[0x4F],
    a[0x50], a[0x51], a[0x52], a[0x53], a[0x54], a[0x55], a[0x56], a[0x57], a[0x58], a[0x59], a[0x5A], a[0x5B], a[0x5C], a[0x5D], a[0x5E], a[0x5F],
    a[0x60], a[0x61], a[0x62], a[0x63], a[0x64], a[0x65], a[0x66], a[0x67], a[0x68], a[0x69], a[0x6A], a[0x6B], a[0x6C], a[0x6D], a[0x6E], a[0x6F],
    a[0x70], a[0x71], a[0x72], a[0x73], a[0x74], a[0x75], a[0x76], a[0x77], a[0x78], a[0x79], a[0x7A], a[0x7B], a[0x7C], a[0x7D], a[0x7E], a[0x7F],
    a[0x80], a[0x81], a[0x82], a[0x83], a[0x84], a[0x85], a[0x86], a[0x87], a[0x88], a[0x89], a[0x8A], a[0x8B], a[0x8C], a[0x8D], a[0x8E], a[0x8F],
    a[0x90], a[0x91], a[0x92], a[0x93], a[0x94], a[0x95], a[0x96], a[0x97], a[0x98], a[0x99], a[0x9A], a[0x9B], a[0x9C], a[0x9D], a[0x9E], a[0x9F],
    a[0xA0], a[0xA1], a[0xA2], a[0xA3], a[0xA4], a[0xA5], a[0xA6], a[0xA7], a[0xA8], a[0xA9], a[0xAA], a[0xAB], a[0xAC], a[0xAD], a[0xAE], a[0xAF],
    a[0xB0], a[0xB1], a[0xB2], a[0xB3], a[0xB4], a[0xB5], a[0xB6], a[0xB7], a[0xB8], a[0xB9], a[0xBA], a[0xBB], a[0xBC], a[0xBD], a[0xBE], a[0xBF],
    a[0xC0], a[0xC1], a[0xC2], a[0xC3], a[0xC4], a[0xC5], a[0xC6], a[0xC7], a[0xC8], a[0xC9], a[0xCA], a[0xCB], a[0xCC], a[0xCD], a[0xCE], a[0xCF],
    a[0xD0], a[0xD1], a[0xD2], a[0xD3], a[0xD4], a[0xD5], a[0xD6], a[0xD7], a[0xD8], a[0xD9], a[0xDA], a[0xDB], a[0xDC], a[0xDD], a[0xDE], a[0xDF],
    a[0xE0], a[0xE1], a[0xE2], a[0xE3], a[0xE4], a[0xE5], a[0xE6], a[0xE7], a[0xE8], a[0xE9], a[0xEA], a[0xEB], a[0xEC], a[0xED], a[0xEE], a[0xEF],
    a[0xF0], a[0xF1], a[0xF2], a[0xF3], a[0xF4], a[0xF5], a[0xF6], a[0xF7],
    // </editor-fold>
    a[0xF8], a[0xF9], a[0xFA], a[0xFB], a[0xFC], a[0xFD]); // hashArguments_254
        assertEquals(r0, r);
        MethodType mt = MethodType.genericMethodType(ARITY);
        MethodHandle mh = MethodHandles.lookup().findStatic(BigArityTest.class, "hashArguments_"+ARITY, mt);
        test254(mh, a, r0);
        MethodHandle mh_CA = MH_hashArguments_VA.asFixedArity().asCollector(Object[].class, ARITY);
        test254(mh_CA, a, r0);
        MethodHandle mh_a = MethodHandles.lookup().findStatic(BigArityTest.class, "hashArguments_"+ARITY+"_a", MT_A).asCollector(1, Object[].class, ARITY-2);
        test254(mh_a, a, r0);
    }
    public void test254(MethodHandle mh, Object[] a, Object r0) throws Throwable {
        Object r;
        r = mh.invokeExact(
    // <editor-fold defaultstate="collapsed" desc="a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], ...">
    a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], a[0x05], a[0x06], a[0x07], a[0x08], a[0x09], a[0x0A], a[0x0B], a[0x0C], a[0x0D], a[0x0E], a[0x0F],
    a[0x10], a[0x11], a[0x12], a[0x13], a[0x14], a[0x15], a[0x16], a[0x17], a[0x18], a[0x19], a[0x1A], a[0x1B], a[0x1C], a[0x1D], a[0x1E], a[0x1F],
    a[0x20], a[0x21], a[0x22], a[0x23], a[0x24], a[0x25], a[0x26], a[0x27], a[0x28], a[0x29], a[0x2A], a[0x2B], a[0x2C], a[0x2D], a[0x2E], a[0x2F],
    a[0x30], a[0x31], a[0x32], a[0x33], a[0x34], a[0x35], a[0x36], a[0x37], a[0x38], a[0x39], a[0x3A], a[0x3B], a[0x3C], a[0x3D], a[0x3E], a[0x3F],
    a[0x40], a[0x41], a[0x42], a[0x43], a[0x44], a[0x45], a[0x46], a[0x47], a[0x48], a[0x49], a[0x4A], a[0x4B], a[0x4C], a[0x4D], a[0x4E], a[0x4F],
    a[0x50], a[0x51], a[0x52], a[0x53], a[0x54], a[0x55], a[0x56], a[0x57], a[0x58], a[0x59], a[0x5A], a[0x5B], a[0x5C], a[0x5D], a[0x5E], a[0x5F],
    a[0x60], a[0x61], a[0x62], a[0x63], a[0x64], a[0x65], a[0x66], a[0x67], a[0x68], a[0x69], a[0x6A], a[0x6B], a[0x6C], a[0x6D], a[0x6E], a[0x6F],
    a[0x70], a[0x71], a[0x72], a[0x73], a[0x74], a[0x75], a[0x76], a[0x77], a[0x78], a[0x79], a[0x7A], a[0x7B], a[0x7C], a[0x7D], a[0x7E], a[0x7F],
    a[0x80], a[0x81], a[0x82], a[0x83], a[0x84], a[0x85], a[0x86], a[0x87], a[0x88], a[0x89], a[0x8A], a[0x8B], a[0x8C], a[0x8D], a[0x8E], a[0x8F],
    a[0x90], a[0x91], a[0x92], a[0x93], a[0x94], a[0x95], a[0x96], a[0x97], a[0x98], a[0x99], a[0x9A], a[0x9B], a[0x9C], a[0x9D], a[0x9E], a[0x9F],
    a[0xA0], a[0xA1], a[0xA2], a[0xA3], a[0xA4], a[0xA5], a[0xA6], a[0xA7], a[0xA8], a[0xA9], a[0xAA], a[0xAB], a[0xAC], a[0xAD], a[0xAE], a[0xAF],
    a[0xB0], a[0xB1], a[0xB2], a[0xB3], a[0xB4], a[0xB5], a[0xB6], a[0xB7], a[0xB8], a[0xB9], a[0xBA], a[0xBB], a[0xBC], a[0xBD], a[0xBE], a[0xBF],
    a[0xC0], a[0xC1], a[0xC2], a[0xC3], a[0xC4], a[0xC5], a[0xC6], a[0xC7], a[0xC8], a[0xC9], a[0xCA], a[0xCB], a[0xCC], a[0xCD], a[0xCE], a[0xCF],
    a[0xD0], a[0xD1], a[0xD2], a[0xD3], a[0xD4], a[0xD5], a[0xD6], a[0xD7], a[0xD8], a[0xD9], a[0xDA], a[0xDB], a[0xDC], a[0xDD], a[0xDE], a[0xDF],
    a[0xE0], a[0xE1], a[0xE2], a[0xE3], a[0xE4], a[0xE5], a[0xE6], a[0xE7], a[0xE8], a[0xE9], a[0xEA], a[0xEB], a[0xEC], a[0xED], a[0xEE], a[0xEF],
    a[0xF0], a[0xF1], a[0xF2], a[0xF3], a[0xF4], a[0xF5], a[0xF6], a[0xF7],
    // </editor-fold>
    a[0xF8], a[0xF9], a[0xFA], a[0xFB], a[0xFC], a[0xFD]);
        assertEquals(r0, r);
        r = mh.invokeWithArguments(a);
        assertEquals(r0, r);
        try {
            MethodHandle ximh = MethodHandles.exactInvoker(mh.type());
            throw new AssertionError("should have thrown IAE; cannot have 1+1+254 arguments");
        } catch (IllegalArgumentException ex) {
            System.out.println("OK: "+ex);
        }
        mh = mh.asType(mh.type().changeParameterType(0x10, Integer.class));
        //System.out.println("type="+mh.type().toString().replaceAll("Object", ""));
        r = mh.invokeExact(
    // <editor-fold defaultstate="collapsed" desc="a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], ... (Integer) a[0x10], ...">
    a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], a[0x05], a[0x06], a[0x07], a[0x08], a[0x09], a[0x0A], a[0x0B], a[0x0C], a[0x0D], a[0x0E], a[0x0F],
    (Integer)
    a[0x10], a[0x11], a[0x12], a[0x13], a[0x14], a[0x15], a[0x16], a[0x17], a[0x18], a[0x19], a[0x1A], a[0x1B], a[0x1C], a[0x1D], a[0x1E], a[0x1F],
    a[0x20], a[0x21], a[0x22], a[0x23], a[0x24], a[0x25], a[0x26], a[0x27], a[0x28], a[0x29], a[0x2A], a[0x2B], a[0x2C], a[0x2D], a[0x2E], a[0x2F],
    a[0x30], a[0x31], a[0x32], a[0x33], a[0x34], a[0x35], a[0x36], a[0x37], a[0x38], a[0x39], a[0x3A], a[0x3B], a[0x3C], a[0x3D], a[0x3E], a[0x3F],
    a[0x40], a[0x41], a[0x42], a[0x43], a[0x44], a[0x45], a[0x46], a[0x47], a[0x48], a[0x49], a[0x4A], a[0x4B], a[0x4C], a[0x4D], a[0x4E], a[0x4F],
    a[0x50], a[0x51], a[0x52], a[0x53], a[0x54], a[0x55], a[0x56], a[0x57], a[0x58], a[0x59], a[0x5A], a[0x5B], a[0x5C], a[0x5D], a[0x5E], a[0x5F],
    a[0x60], a[0x61], a[0x62], a[0x63], a[0x64], a[0x65], a[0x66], a[0x67], a[0x68], a[0x69], a[0x6A], a[0x6B], a[0x6C], a[0x6D], a[0x6E], a[0x6F],
    a[0x70], a[0x71], a[0x72], a[0x73], a[0x74], a[0x75], a[0x76], a[0x77], a[0x78], a[0x79], a[0x7A], a[0x7B], a[0x7C], a[0x7D], a[0x7E], a[0x7F],
    a[0x80], a[0x81], a[0x82], a[0x83], a[0x84], a[0x85], a[0x86], a[0x87], a[0x88], a[0x89], a[0x8A], a[0x8B], a[0x8C], a[0x8D], a[0x8E], a[0x8F],
    a[0x90], a[0x91], a[0x92], a[0x93], a[0x94], a[0x95], a[0x96], a[0x97], a[0x98], a[0x99], a[0x9A], a[0x9B], a[0x9C], a[0x9D], a[0x9E], a[0x9F],
    a[0xA0], a[0xA1], a[0xA2], a[0xA3], a[0xA4], a[0xA5], a[0xA6], a[0xA7], a[0xA8], a[0xA9], a[0xAA], a[0xAB], a[0xAC], a[0xAD], a[0xAE], a[0xAF],
    a[0xB0], a[0xB1], a[0xB2], a[0xB3], a[0xB4], a[0xB5], a[0xB6], a[0xB7], a[0xB8], a[0xB9], a[0xBA], a[0xBB], a[0xBC], a[0xBD], a[0xBE], a[0xBF],
    a[0xC0], a[0xC1], a[0xC2], a[0xC3], a[0xC4], a[0xC5], a[0xC6], a[0xC7], a[0xC8], a[0xC9], a[0xCA], a[0xCB], a[0xCC], a[0xCD], a[0xCE], a[0xCF],
    a[0xD0], a[0xD1], a[0xD2], a[0xD3], a[0xD4], a[0xD5], a[0xD6], a[0xD7], a[0xD8], a[0xD9], a[0xDA], a[0xDB], a[0xDC], a[0xDD], a[0xDE], a[0xDF],
    a[0xE0], a[0xE1], a[0xE2], a[0xE3], a[0xE4], a[0xE5], a[0xE6], a[0xE7], a[0xE8], a[0xE9], a[0xEA], a[0xEB], a[0xEC], a[0xED], a[0xEE], a[0xEF],
    a[0xF0], a[0xF1], a[0xF2], a[0xF3], a[0xF4], a[0xF5], a[0xF6], a[0xF7],
    // </editor-fold>
    a[0xF8], a[0xF9], a[0xFA], a[0xFB], a[0xFC], a[0xFD]);
        assertEquals(r0, r);
        mh = mh.asType(mh.type().changeParameterType(0xE0, Number.class));
        //System.out.println("type="+mh.type().toString().replaceAll("Object", ""));
        r = mh.invokeExact(
    // <editor-fold defaultstate="collapsed" desc="a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], ... (Integer) a[0x10], ... (Number) a[0xE0], ...">
    a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], a[0x05], a[0x06], a[0x07], a[0x08], a[0x09], a[0x0A], a[0x0B], a[0x0C], a[0x0D], a[0x0E], a[0x0F],
    (Integer)
    a[0x10], a[0x11], a[0x12], a[0x13], a[0x14], a[0x15], a[0x16], a[0x17], a[0x18], a[0x19], a[0x1A], a[0x1B], a[0x1C], a[0x1D], a[0x1E], a[0x1F],
    a[0x20], a[0x21], a[0x22], a[0x23], a[0x24], a[0x25], a[0x26], a[0x27], a[0x28], a[0x29], a[0x2A], a[0x2B], a[0x2C], a[0x2D], a[0x2E], a[0x2F],
    a[0x30], a[0x31], a[0x32], a[0x33], a[0x34], a[0x35], a[0x36], a[0x37], a[0x38], a[0x39], a[0x3A], a[0x3B], a[0x3C], a[0x3D], a[0x3E], a[0x3F],
    a[0x40], a[0x41], a[0x42], a[0x43], a[0x44], a[0x45], a[0x46], a[0x47], a[0x48], a[0x49], a[0x4A], a[0x4B], a[0x4C], a[0x4D], a[0x4E], a[0x4F],
    a[0x50], a[0x51], a[0x52], a[0x53], a[0x54], a[0x55], a[0x56], a[0x57], a[0x58], a[0x59], a[0x5A], a[0x5B], a[0x5C], a[0x5D], a[0x5E], a[0x5F],
    a[0x60], a[0x61], a[0x62], a[0x63], a[0x64], a[0x65], a[0x66], a[0x67], a[0x68], a[0x69], a[0x6A], a[0x6B], a[0x6C], a[0x6D], a[0x6E], a[0x6F],
    a[0x70], a[0x71], a[0x72], a[0x73], a[0x74], a[0x75], a[0x76], a[0x77], a[0x78], a[0x79], a[0x7A], a[0x7B], a[0x7C], a[0x7D], a[0x7E], a[0x7F],
    a[0x80], a[0x81], a[0x82], a[0x83], a[0x84], a[0x85], a[0x86], a[0x87], a[0x88], a[0x89], a[0x8A], a[0x8B], a[0x8C], a[0x8D], a[0x8E], a[0x8F],
    a[0x90], a[0x91], a[0x92], a[0x93], a[0x94], a[0x95], a[0x96], a[0x97], a[0x98], a[0x99], a[0x9A], a[0x9B], a[0x9C], a[0x9D], a[0x9E], a[0x9F],
    a[0xA0], a[0xA1], a[0xA2], a[0xA3], a[0xA4], a[0xA5], a[0xA6], a[0xA7], a[0xA8], a[0xA9], a[0xAA], a[0xAB], a[0xAC], a[0xAD], a[0xAE], a[0xAF],
    a[0xB0], a[0xB1], a[0xB2], a[0xB3], a[0xB4], a[0xB5], a[0xB6], a[0xB7], a[0xB8], a[0xB9], a[0xBA], a[0xBB], a[0xBC], a[0xBD], a[0xBE], a[0xBF],
    a[0xC0], a[0xC1], a[0xC2], a[0xC3], a[0xC4], a[0xC5], a[0xC6], a[0xC7], a[0xC8], a[0xC9], a[0xCA], a[0xCB], a[0xCC], a[0xCD], a[0xCE], a[0xCF],
    a[0xD0], a[0xD1], a[0xD2], a[0xD3], a[0xD4], a[0xD5], a[0xD6], a[0xD7], a[0xD8], a[0xD9], a[0xDA], a[0xDB], a[0xDC], a[0xDD], a[0xDE], a[0xDF],
    (Number)
    a[0xE0], a[0xE1], a[0xE2], a[0xE3], a[0xE4], a[0xE5], a[0xE6], a[0xE7], a[0xE8], a[0xE9], a[0xEA], a[0xEB], a[0xEC], a[0xED], a[0xEE], a[0xEF],
    a[0xF0], a[0xF1], a[0xF2], a[0xF3], a[0xF4], a[0xF5], a[0xF6], a[0xF7],
    // </editor-fold>
    a[0xF8], a[0xF9], a[0xFA], a[0xFB], a[0xFC], a[0xFD]);
        assertEquals(r0, r);
        r = mh.invoke(
    // <editor-fold defaultstate="collapsed" desc="(Comparable) a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], ...">
    (Comparable<?>)
    a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], a[0x05], a[0x06], a[0x07], a[0x08], a[0x09], a[0x0A], a[0x0B], a[0x0C], a[0x0D], a[0x0E], a[0x0F],
    a[0x10], a[0x11], a[0x12], a[0x13], a[0x14], a[0x15], a[0x16], a[0x17], a[0x18], a[0x19], a[0x1A], a[0x1B], a[0x1C], a[0x1D], a[0x1E], a[0x1F],
    a[0x20], a[0x21], a[0x22], a[0x23], a[0x24], a[0x25], a[0x26], a[0x27], a[0x28], a[0x29], a[0x2A], a[0x2B], a[0x2C], a[0x2D], a[0x2E], a[0x2F],
    a[0x30], a[0x31], a[0x32], a[0x33], a[0x34], a[0x35], a[0x36], a[0x37], a[0x38], a[0x39], a[0x3A], a[0x3B], a[0x3C], a[0x3D], a[0x3E], a[0x3F],
    a[0x40], a[0x41], a[0x42], a[0x43], a[0x44], a[0x45], a[0x46], a[0x47], a[0x48], a[0x49], a[0x4A], a[0x4B], a[0x4C], a[0x4D], a[0x4E], a[0x4F],
    a[0x50], a[0x51], a[0x52], a[0x53], a[0x54], a[0x55], a[0x56], a[0x57], a[0x58], a[0x59], a[0x5A], a[0x5B], a[0x5C], a[0x5D], a[0x5E], a[0x5F],
    a[0x60], a[0x61], a[0x62], a[0x63], a[0x64], a[0x65], a[0x66], a[0x67], a[0x68], a[0x69], a[0x6A], a[0x6B], a[0x6C], a[0x6D], a[0x6E], a[0x6F],
    a[0x70], a[0x71], a[0x72], a[0x73], a[0x74], a[0x75], a[0x76], a[0x77], a[0x78], a[0x79], a[0x7A], a[0x7B], a[0x7C], a[0x7D], a[0x7E], a[0x7F],
    a[0x80], a[0x81], a[0x82], a[0x83], a[0x84], a[0x85], a[0x86], a[0x87], a[0x88], a[0x89], a[0x8A], a[0x8B], a[0x8C], a[0x8D], a[0x8E], a[0x8F],
    a[0x90], a[0x91], a[0x92], a[0x93], a[0x94], a[0x95], a[0x96], a[0x97], a[0x98], a[0x99], a[0x9A], a[0x9B], a[0x9C], a[0x9D], a[0x9E], a[0x9F],
    a[0xA0], a[0xA1], a[0xA2], a[0xA3], a[0xA4], a[0xA5], a[0xA6], a[0xA7], a[0xA8], a[0xA9], a[0xAA], a[0xAB], a[0xAC], a[0xAD], a[0xAE], a[0xAF],
    a[0xB0], a[0xB1], a[0xB2], a[0xB3], a[0xB4], a[0xB5], a[0xB6], a[0xB7], a[0xB8], a[0xB9], a[0xBA], a[0xBB], a[0xBC], a[0xBD], a[0xBE], a[0xBF],
    a[0xC0], a[0xC1], a[0xC2], a[0xC3], a[0xC4], a[0xC5], a[0xC6], a[0xC7], a[0xC8], a[0xC9], a[0xCA], a[0xCB], a[0xCC], a[0xCD], a[0xCE], a[0xCF],
    a[0xD0], a[0xD1], a[0xD2], a[0xD3], a[0xD4], a[0xD5], a[0xD6], a[0xD7], a[0xD8], a[0xD9], a[0xDA], a[0xDB], a[0xDC], a[0xDD], a[0xDE], a[0xDF],
    a[0xE0], a[0xE1], a[0xE2], a[0xE3], a[0xE4], a[0xE5], a[0xE6], a[0xE7], a[0xE8], a[0xE9], a[0xEA], a[0xEB], a[0xEC], a[0xED], a[0xEE], a[0xEF],
    a[0xF0], a[0xF1], a[0xF2], a[0xF3], a[0xF4], a[0xF5], a[0xF6], a[0xF7],
    // </editor-fold>
    a[0xF8], a[0xF9], a[0xFA], a[0xFB], a[0xFC], a[0xFD]);
        assertEquals(r0, r);
    }

    static Object hashArguments_255(
    // <editor-fold defaultstate="collapsed" desc="Object x00, Object x01, Object x02, Object x03, Object x04, ...">
    Object x00, Object x01, Object x02, Object x03, Object x04, Object x05, Object x06, Object x07,
    Object x08, Object x09, Object x0A, Object x0B, Object x0C, Object x0D, Object x0E, Object x0F,
    Object x10, Object x11, Object x12, Object x13, Object x14, Object x15, Object x16, Object x17,
    Object x18, Object x19, Object x1A, Object x1B, Object x1C, Object x1D, Object x1E, Object x1F,
    Object x20, Object x21, Object x22, Object x23, Object x24, Object x25, Object x26, Object x27,
    Object x28, Object x29, Object x2A, Object x2B, Object x2C, Object x2D, Object x2E, Object x2F,
    Object x30, Object x31, Object x32, Object x33, Object x34, Object x35, Object x36, Object x37,
    Object x38, Object x39, Object x3A, Object x3B, Object x3C, Object x3D, Object x3E, Object x3F,
    Object x40, Object x41, Object x42, Object x43, Object x44, Object x45, Object x46, Object x47,
    Object x48, Object x49, Object x4A, Object x4B, Object x4C, Object x4D, Object x4E, Object x4F,
    Object x50, Object x51, Object x52, Object x53, Object x54, Object x55, Object x56, Object x57,
    Object x58, Object x59, Object x5A, Object x5B, Object x5C, Object x5D, Object x5E, Object x5F,
    Object x60, Object x61, Object x62, Object x63, Object x64, Object x65, Object x66, Object x67,
    Object x68, Object x69, Object x6A, Object x6B, Object x6C, Object x6D, Object x6E, Object x6F,
    Object x70, Object x71, Object x72, Object x73, Object x74, Object x75, Object x76, Object x77,
    Object x78, Object x79, Object x7A, Object x7B, Object x7C, Object x7D, Object x7E, Object x7F,
    Object x80, Object x81, Object x82, Object x83, Object x84, Object x85, Object x86, Object x87,
    Object x88, Object x89, Object x8A, Object x8B, Object x8C, Object x8D, Object x8E, Object x8F,
    Object x90, Object x91, Object x92, Object x93, Object x94, Object x95, Object x96, Object x97,
    Object x98, Object x99, Object x9A, Object x9B, Object x9C, Object x9D, Object x9E, Object x9F,
    Object xA0, Object xA1, Object xA2, Object xA3, Object xA4, Object xA5, Object xA6, Object xA7,
    Object xA8, Object xA9, Object xAA, Object xAB, Object xAC, Object xAD, Object xAE, Object xAF,
    Object xB0, Object xB1, Object xB2, Object xB3, Object xB4, Object xB5, Object xB6, Object xB7,
    Object xB8, Object xB9, Object xBA, Object xBB, Object xBC, Object xBD, Object xBE, Object xBF,
    Object xC0, Object xC1, Object xC2, Object xC3, Object xC4, Object xC5, Object xC6, Object xC7,
    Object xC8, Object xC9, Object xCA, Object xCB, Object xCC, Object xCD, Object xCE, Object xCF,
    Object xD0, Object xD1, Object xD2, Object xD3, Object xD4, Object xD5, Object xD6, Object xD7,
    Object xD8, Object xD9, Object xDA, Object xDB, Object xDC, Object xDD, Object xDE, Object xDF,
    Object xE0, Object xE1, Object xE2, Object xE3, Object xE4, Object xE5, Object xE6, Object xE7,
    Object xE8, Object xE9, Object xEA, Object xEB, Object xEC, Object xED, Object xEE, Object xEF,
    Object xF0, Object xF1, Object xF2, Object xF3, Object xF4, Object xF5, Object xF6, Object xF7,
    // </editor-fold>
    Object xF8, Object xF9, Object xFA, Object xFB, Object xFC, Object xFD, Object xFE) {
        return Objects.hash(
    // <editor-fold defaultstate="collapsed" desc="x00, x01, x02, x03, x04, ...">
    x00, x01, x02, x03, x04, x05, x06, x07, x08, x09, x0A, x0B, x0C, x0D, x0E, x0F,
    x10, x11, x12, x13, x14, x15, x16, x17, x18, x19, x1A, x1B, x1C, x1D, x1E, x1F,
    x20, x21, x22, x23, x24, x25, x26, x27, x28, x29, x2A, x2B, x2C, x2D, x2E, x2F,
    x30, x31, x32, x33, x34, x35, x36, x37, x38, x39, x3A, x3B, x3C, x3D, x3E, x3F,
    x40, x41, x42, x43, x44, x45, x46, x47, x48, x49, x4A, x4B, x4C, x4D, x4E, x4F,
    x50, x51, x52, x53, x54, x55, x56, x57, x58, x59, x5A, x5B, x5C, x5D, x5E, x5F,
    x60, x61, x62, x63, x64, x65, x66, x67, x68, x69, x6A, x6B, x6C, x6D, x6E, x6F,
    x70, x71, x72, x73, x74, x75, x76, x77, x78, x79, x7A, x7B, x7C, x7D, x7E, x7F,
    x80, x81, x82, x83, x84, x85, x86, x87, x88, x89, x8A, x8B, x8C, x8D, x8E, x8F,
    x90, x91, x92, x93, x94, x95, x96, x97, x98, x99, x9A, x9B, x9C, x9D, x9E, x9F,
    xA0, xA1, xA2, xA3, xA4, xA5, xA6, xA7, xA8, xA9, xAA, xAB, xAC, xAD, xAE, xAF,
    xB0, xB1, xB2, xB3, xB4, xB5, xB6, xB7, xB8, xB9, xBA, xBB, xBC, xBD, xBE, xBF,
    xC0, xC1, xC2, xC3, xC4, xC5, xC6, xC7, xC8, xC9, xCA, xCB, xCC, xCD, xCE, xCF,
    xD0, xD1, xD2, xD3, xD4, xD5, xD6, xD7, xD8, xD9, xDA, xDB, xDC, xDD, xDE, xDF,
    xE0, xE1, xE2, xE3, xE4, xE5, xE6, xE7, xE8, xE9, xEA, xEB, xEC, xED, xEE, xEF,
    xF0, xF1, xF2, xF3, xF4, xF5, xF6, xF7,
    // </editor-fold>
    xF8, xF9, xFA, xFB, xFC, xFD, xFE);
    }
    static Object hashArguments_255_a(Object x00, Object[] x01_FD, Object xFE) {
        return Objects.hash(
                // <editor-fold defaultstate="collapsed" desc="x00, x01_FD[0], x01_FD[1], x01_FD[2], ...">
                x00, x01_FD[0], x01_FD[1], x01_FD[2], x01_FD[3], x01_FD[4], x01_FD[5], x01_FD[6], x01_FD[7], x01_FD[8],
                x01_FD[9], x01_FD[10], x01_FD[11], x01_FD[12], x01_FD[13], x01_FD[14], x01_FD[15], x01_FD[16],
                x01_FD[17], x01_FD[18], x01_FD[19], x01_FD[20], x01_FD[21], x01_FD[22], x01_FD[23], x01_FD[24],
                x01_FD[25], x01_FD[26], x01_FD[27], x01_FD[28], x01_FD[29], x01_FD[30], x01_FD[31], x01_FD[32],
                x01_FD[33], x01_FD[34], x01_FD[35], x01_FD[36], x01_FD[37], x01_FD[38], x01_FD[39], x01_FD[40],
                x01_FD[41], x01_FD[42], x01_FD[43], x01_FD[44], x01_FD[45], x01_FD[46], x01_FD[47], x01_FD[48],
                x01_FD[49], x01_FD[50], x01_FD[51], x01_FD[52], x01_FD[53], x01_FD[54], x01_FD[55], x01_FD[56],
                x01_FD[57], x01_FD[58], x01_FD[59], x01_FD[60], x01_FD[61], x01_FD[62], x01_FD[63], x01_FD[64],
                x01_FD[65], x01_FD[66], x01_FD[67], x01_FD[68], x01_FD[69], x01_FD[70], x01_FD[71], x01_FD[72],
                x01_FD[73], x01_FD[74], x01_FD[75], x01_FD[76], x01_FD[77], x01_FD[78], x01_FD[79], x01_FD[80],
                x01_FD[81], x01_FD[82], x01_FD[83], x01_FD[84], x01_FD[85], x01_FD[86], x01_FD[87], x01_FD[88],
                x01_FD[89], x01_FD[90], x01_FD[91], x01_FD[92], x01_FD[93], x01_FD[94], x01_FD[95], x01_FD[96],
                x01_FD[97], x01_FD[98], x01_FD[99], x01_FD[100], x01_FD[101], x01_FD[102], x01_FD[103], x01_FD[104],
                x01_FD[105], x01_FD[106], x01_FD[107], x01_FD[108], x01_FD[109], x01_FD[110], x01_FD[111], x01_FD[112],
                x01_FD[113], x01_FD[114], x01_FD[115], x01_FD[116], x01_FD[117], x01_FD[118], x01_FD[119], x01_FD[120],
                x01_FD[121], x01_FD[122], x01_FD[123], x01_FD[124], x01_FD[125], x01_FD[126], x01_FD[127], x01_FD[128],
                x01_FD[129], x01_FD[130], x01_FD[131], x01_FD[132], x01_FD[133], x01_FD[134], x01_FD[135], x01_FD[136],
                x01_FD[137], x01_FD[138], x01_FD[139], x01_FD[140], x01_FD[141], x01_FD[142], x01_FD[143], x01_FD[144],
                x01_FD[145], x01_FD[146], x01_FD[147], x01_FD[148], x01_FD[149], x01_FD[150], x01_FD[151], x01_FD[152],
                x01_FD[153], x01_FD[154], x01_FD[155], x01_FD[156], x01_FD[157], x01_FD[158], x01_FD[159], x01_FD[160],
                x01_FD[161], x01_FD[162], x01_FD[163], x01_FD[164], x01_FD[165], x01_FD[166], x01_FD[167], x01_FD[168],
                x01_FD[169], x01_FD[170], x01_FD[171], x01_FD[172], x01_FD[173], x01_FD[174], x01_FD[175], x01_FD[176],
                x01_FD[177], x01_FD[178], x01_FD[179], x01_FD[180], x01_FD[181], x01_FD[182], x01_FD[183], x01_FD[184],
                x01_FD[185], x01_FD[186], x01_FD[187], x01_FD[188], x01_FD[189], x01_FD[190], x01_FD[191], x01_FD[192],
                x01_FD[193], x01_FD[194], x01_FD[195], x01_FD[196], x01_FD[197], x01_FD[198], x01_FD[199], x01_FD[200],
                x01_FD[201], x01_FD[202], x01_FD[203], x01_FD[204], x01_FD[205], x01_FD[206], x01_FD[207], x01_FD[208],
                x01_FD[209], x01_FD[210], x01_FD[211], x01_FD[212], x01_FD[213], x01_FD[214], x01_FD[215], x01_FD[216],
                x01_FD[217], x01_FD[218], x01_FD[219], x01_FD[220], x01_FD[221], x01_FD[222], x01_FD[223], x01_FD[224],
                x01_FD[225], x01_FD[226], x01_FD[227], x01_FD[228], x01_FD[229], x01_FD[230], x01_FD[231], x01_FD[232],
                x01_FD[233], x01_FD[234], x01_FD[235], x01_FD[236], x01_FD[237], x01_FD[238], x01_FD[239], x01_FD[240],
                x01_FD[241], x01_FD[242], x01_FD[243], x01_FD[244], x01_FD[245], x01_FD[246], x01_FD[247], x01_FD[248],
                // </editor-fold>
                x01_FD[249], x01_FD[250], x01_FD[251], x01_FD[252], xFE);
    }

    @Test
    public void test255() throws Throwable {
        final int ARITY = 255;
        System.out.println("test"+ARITY);
        Object[] a = testArgs(ARITY);
        Object r0 = hashArguments(a);
        Object r;
        r = hashArguments_255(
    // <editor-fold defaultstate="collapsed" desc="a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], ...">
    a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], a[0x05], a[0x06], a[0x07], a[0x08], a[0x09], a[0x0A], a[0x0B], a[0x0C], a[0x0D], a[0x0E], a[0x0F],
    a[0x10], a[0x11], a[0x12], a[0x13], a[0x14], a[0x15], a[0x16], a[0x17], a[0x18], a[0x19], a[0x1A], a[0x1B], a[0x1C], a[0x1D], a[0x1E], a[0x1F],
    a[0x20], a[0x21], a[0x22], a[0x23], a[0x24], a[0x25], a[0x26], a[0x27], a[0x28], a[0x29], a[0x2A], a[0x2B], a[0x2C], a[0x2D], a[0x2E], a[0x2F],
    a[0x30], a[0x31], a[0x32], a[0x33], a[0x34], a[0x35], a[0x36], a[0x37], a[0x38], a[0x39], a[0x3A], a[0x3B], a[0x3C], a[0x3D], a[0x3E], a[0x3F],
    a[0x40], a[0x41], a[0x42], a[0x43], a[0x44], a[0x45], a[0x46], a[0x47], a[0x48], a[0x49], a[0x4A], a[0x4B], a[0x4C], a[0x4D], a[0x4E], a[0x4F],
    a[0x50], a[0x51], a[0x52], a[0x53], a[0x54], a[0x55], a[0x56], a[0x57], a[0x58], a[0x59], a[0x5A], a[0x5B], a[0x5C], a[0x5D], a[0x5E], a[0x5F],
    a[0x60], a[0x61], a[0x62], a[0x63], a[0x64], a[0x65], a[0x66], a[0x67], a[0x68], a[0x69], a[0x6A], a[0x6B], a[0x6C], a[0x6D], a[0x6E], a[0x6F],
    a[0x70], a[0x71], a[0x72], a[0x73], a[0x74], a[0x75], a[0x76], a[0x77], a[0x78], a[0x79], a[0x7A], a[0x7B], a[0x7C], a[0x7D], a[0x7E], a[0x7F],
    a[0x80], a[0x81], a[0x82], a[0x83], a[0x84], a[0x85], a[0x86], a[0x87], a[0x88], a[0x89], a[0x8A], a[0x8B], a[0x8C], a[0x8D], a[0x8E], a[0x8F],
    a[0x90], a[0x91], a[0x92], a[0x93], a[0x94], a[0x95], a[0x96], a[0x97], a[0x98], a[0x99], a[0x9A], a[0x9B], a[0x9C], a[0x9D], a[0x9E], a[0x9F],
    a[0xA0], a[0xA1], a[0xA2], a[0xA3], a[0xA4], a[0xA5], a[0xA6], a[0xA7], a[0xA8], a[0xA9], a[0xAA], a[0xAB], a[0xAC], a[0xAD], a[0xAE], a[0xAF],
    a[0xB0], a[0xB1], a[0xB2], a[0xB3], a[0xB4], a[0xB5], a[0xB6], a[0xB7], a[0xB8], a[0xB9], a[0xBA], a[0xBB], a[0xBC], a[0xBD], a[0xBE], a[0xBF],
    a[0xC0], a[0xC1], a[0xC2], a[0xC3], a[0xC4], a[0xC5], a[0xC6], a[0xC7], a[0xC8], a[0xC9], a[0xCA], a[0xCB], a[0xCC], a[0xCD], a[0xCE], a[0xCF],
    a[0xD0], a[0xD1], a[0xD2], a[0xD3], a[0xD4], a[0xD5], a[0xD6], a[0xD7], a[0xD8], a[0xD9], a[0xDA], a[0xDB], a[0xDC], a[0xDD], a[0xDE], a[0xDF],
    a[0xE0], a[0xE1], a[0xE2], a[0xE3], a[0xE4], a[0xE5], a[0xE6], a[0xE7], a[0xE8], a[0xE9], a[0xEA], a[0xEB], a[0xEC], a[0xED], a[0xEE], a[0xEF],
    a[0xF0], a[0xF1], a[0xF2], a[0xF3], a[0xF4], a[0xF5], a[0xF6], a[0xF7],
    // </editor-fold>
    a[0xF8], a[0xF9], a[0xFA], a[0xFB], a[0xFC], a[0xFD], a[0xFE]); // hashArguments_255
        assertEquals(r0, r);
        MethodType mt = MethodType.genericMethodType(ARITY);
        MethodHandle mh;
        try {
            mh = MethodHandles.lookup().findStatic(BigArityTest.class, "hashArguments_"+ARITY, mt);
            throw new AssertionError("should not create an arity 255 method handle");
        } catch (IllegalArgumentException ex) {
            System.out.println("OK: "+ex);
            mh = MethodHandles.lookup().findStatic(BigArityTest.class, "hashArguments_"+(ARITY-1), mt.dropParameterTypes(ARITY-1, ARITY));
        }
        try {
            r = mh.invokeExact(
    // <editor-fold defaultstate="collapsed" desc="a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], ...">
    a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], a[0x05], a[0x06], a[0x07], a[0x08], a[0x09], a[0x0A], a[0x0B], a[0x0C], a[0x0D], a[0x0E], a[0x0F],
    a[0x10], a[0x11], a[0x12], a[0x13], a[0x14], a[0x15], a[0x16], a[0x17], a[0x18], a[0x19], a[0x1A], a[0x1B], a[0x1C], a[0x1D], a[0x1E], a[0x1F],
    a[0x20], a[0x21], a[0x22], a[0x23], a[0x24], a[0x25], a[0x26], a[0x27], a[0x28], a[0x29], a[0x2A], a[0x2B], a[0x2C], a[0x2D], a[0x2E], a[0x2F],
    a[0x30], a[0x31], a[0x32], a[0x33], a[0x34], a[0x35], a[0x36], a[0x37], a[0x38], a[0x39], a[0x3A], a[0x3B], a[0x3C], a[0x3D], a[0x3E], a[0x3F],
    a[0x40], a[0x41], a[0x42], a[0x43], a[0x44], a[0x45], a[0x46], a[0x47], a[0x48], a[0x49], a[0x4A], a[0x4B], a[0x4C], a[0x4D], a[0x4E], a[0x4F],
    a[0x50], a[0x51], a[0x52], a[0x53], a[0x54], a[0x55], a[0x56], a[0x57], a[0x58], a[0x59], a[0x5A], a[0x5B], a[0x5C], a[0x5D], a[0x5E], a[0x5F],
    a[0x60], a[0x61], a[0x62], a[0x63], a[0x64], a[0x65], a[0x66], a[0x67], a[0x68], a[0x69], a[0x6A], a[0x6B], a[0x6C], a[0x6D], a[0x6E], a[0x6F],
    a[0x70], a[0x71], a[0x72], a[0x73], a[0x74], a[0x75], a[0x76], a[0x77], a[0x78], a[0x79], a[0x7A], a[0x7B], a[0x7C], a[0x7D], a[0x7E], a[0x7F],
    a[0x80], a[0x81], a[0x82], a[0x83], a[0x84], a[0x85], a[0x86], a[0x87], a[0x88], a[0x89], a[0x8A], a[0x8B], a[0x8C], a[0x8D], a[0x8E], a[0x8F],
    a[0x90], a[0x91], a[0x92], a[0x93], a[0x94], a[0x95], a[0x96], a[0x97], a[0x98], a[0x99], a[0x9A], a[0x9B], a[0x9C], a[0x9D], a[0x9E], a[0x9F],
    a[0xA0], a[0xA1], a[0xA2], a[0xA3], a[0xA4], a[0xA5], a[0xA6], a[0xA7], a[0xA8], a[0xA9], a[0xAA], a[0xAB], a[0xAC], a[0xAD], a[0xAE], a[0xAF],
    a[0xB0], a[0xB1], a[0xB2], a[0xB3], a[0xB4], a[0xB5], a[0xB6], a[0xB7], a[0xB8], a[0xB9], a[0xBA], a[0xBB], a[0xBC], a[0xBD], a[0xBE], a[0xBF],
    a[0xC0], a[0xC1], a[0xC2], a[0xC3], a[0xC4], a[0xC5], a[0xC6], a[0xC7], a[0xC8], a[0xC9], a[0xCA], a[0xCB], a[0xCC], a[0xCD], a[0xCE], a[0xCF],
    a[0xD0], a[0xD1], a[0xD2], a[0xD3], a[0xD4], a[0xD5], a[0xD6], a[0xD7], a[0xD8], a[0xD9], a[0xDA], a[0xDB], a[0xDC], a[0xDD], a[0xDE], a[0xDF],
    a[0xE0], a[0xE1], a[0xE2], a[0xE3], a[0xE4], a[0xE5], a[0xE6], a[0xE7], a[0xE8], a[0xE9], a[0xEA], a[0xEB], a[0xEC], a[0xED], a[0xEE], a[0xEF],
    a[0xF0], a[0xF1], a[0xF2], a[0xF3], a[0xF4], a[0xF5], a[0xF6], a[0xF7],
    // </editor-fold>
    a[0xF8], a[0xF9], a[0xFA], a[0xFB], a[0xFC], a[0xFD], a[0xFE]);
            throw new AssertionError("should not call an arity 255 method handle");
        } catch (LinkageError ex) {
            System.out.println("OK: "+ex);
        }
        try {
            MethodHandle ximh = MethodHandles.exactInvoker(mt);
            throw new AssertionError("should have thrown IAE; cannot have 1+1+255 arguments");
        } catch (IllegalArgumentException ex) {
            System.out.println("OK: "+ex);
        }
        MethodHandle mh_a;
        try {
            mh_a = MethodHandles.lookup().findStatic(BigArityTest.class, "hashArguments_"+ARITY+"_a", MT_A).asCollector(1, Object[].class, ARITY-2);
            throw new AssertionError("should not create an arity 255 collector method handle");
        } catch (IllegalArgumentException ex) {
            System.out.println("OK: "+ex);
            mh_a = MethodHandles.lookup().findStatic(BigArityTest.class, "hashArguments_"+ARITY+"_a", MT_A).asCollector(1, Object[].class, ARITY-3);
        }
        try {
            r = mh_a.invokeExact(
                    // <editor-fold defaultstate="collapsed" desc="a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], ...">
                    a[0x00], a[0x01], a[0x02], a[0x03], a[0x04], a[0x05], a[0x06], a[0x07], a[0x08], a[0x09], a[0x0A], a[0x0B], a[0x0C], a[0x0D], a[0x0E], a[0x0F],
                    a[0x10], a[0x11], a[0x12], a[0x13], a[0x14], a[0x15], a[0x16], a[0x17], a[0x18], a[0x19], a[0x1A], a[0x1B], a[0x1C], a[0x1D], a[0x1E], a[0x1F],
                    a[0x20], a[0x21], a[0x22], a[0x23], a[0x24], a[0x25], a[0x26], a[0x27], a[0x28], a[0x29], a[0x2A], a[0x2B], a[0x2C], a[0x2D], a[0x2E], a[0x2F],
                    a[0x30], a[0x31], a[0x32], a[0x33], a[0x34], a[0x35], a[0x36], a[0x37], a[0x38], a[0x39], a[0x3A], a[0x3B], a[0x3C], a[0x3D], a[0x3E], a[0x3F],
                    a[0x40], a[0x41], a[0x42], a[0x43], a[0x44], a[0x45], a[0x46], a[0x47], a[0x48], a[0x49], a[0x4A], a[0x4B], a[0x4C], a[0x4D], a[0x4E], a[0x4F],
                    a[0x50], a[0x51], a[0x52], a[0x53], a[0x54], a[0x55], a[0x56], a[0x57], a[0x58], a[0x59], a[0x5A], a[0x5B], a[0x5C], a[0x5D], a[0x5E], a[0x5F],
                    a[0x60], a[0x61], a[0x62], a[0x63], a[0x64], a[0x65], a[0x66], a[0x67], a[0x68], a[0x69], a[0x6A], a[0x6B], a[0x6C], a[0x6D], a[0x6E], a[0x6F],
                    a[0x70], a[0x71], a[0x72], a[0x73], a[0x74], a[0x75], a[0x76], a[0x77], a[0x78], a[0x79], a[0x7A], a[0x7B], a[0x7C], a[0x7D], a[0x7E], a[0x7F],
                    a[0x80], a[0x81], a[0x82], a[0x83], a[0x84], a[0x85], a[0x86], a[0x87], a[0x88], a[0x89], a[0x8A], a[0x8B], a[0x8C], a[0x8D], a[0x8E], a[0x8F],
                    a[0x90], a[0x91], a[0x92], a[0x93], a[0x94], a[0x95], a[0x96], a[0x97], a[0x98], a[0x99], a[0x9A], a[0x9B], a[0x9C], a[0x9D], a[0x9E], a[0x9F],
                    a[0xA0], a[0xA1], a[0xA2], a[0xA3], a[0xA4], a[0xA5], a[0xA6], a[0xA7], a[0xA8], a[0xA9], a[0xAA], a[0xAB], a[0xAC], a[0xAD], a[0xAE], a[0xAF],
                    a[0xB0], a[0xB1], a[0xB2], a[0xB3], a[0xB4], a[0xB5], a[0xB6], a[0xB7], a[0xB8], a[0xB9], a[0xBA], a[0xBB], a[0xBC], a[0xBD], a[0xBE], a[0xBF],
                    a[0xC0], a[0xC1], a[0xC2], a[0xC3], a[0xC4], a[0xC5], a[0xC6], a[0xC7], a[0xC8], a[0xC9], a[0xCA], a[0xCB], a[0xCC], a[0xCD], a[0xCE], a[0xCF],
                    a[0xD0], a[0xD1], a[0xD2], a[0xD3], a[0xD4], a[0xD5], a[0xD6], a[0xD7], a[0xD8], a[0xD9], a[0xDA], a[0xDB], a[0xDC], a[0xDD], a[0xDE], a[0xDF],
                    a[0xE0], a[0xE1], a[0xE2], a[0xE3], a[0xE4], a[0xE5], a[0xE6], a[0xE7], a[0xE8], a[0xE9], a[0xEA], a[0xEB], a[0xEC], a[0xED], a[0xEE], a[0xEF],
                    a[0xF0], a[0xF1], a[0xF2], a[0xF3], a[0xF4], a[0xF5], a[0xF6], a[0xF7],
                    // </editor-fold>
                    a[0xF8], a[0xF9], a[0xFA], a[0xFB], a[0xFC], a[0xFD], a[0xFE]);
            throw new AssertionError("should not call an arity 255 collector method handle");
        } catch (LinkageError ex) {
            System.out.println("OK: "+ex);
        }
    }
}
