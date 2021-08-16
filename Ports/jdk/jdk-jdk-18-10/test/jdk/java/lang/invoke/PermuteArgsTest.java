/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib /java/lang/invoke/common
 * @run testng/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:-VerifyDependencies -ea -esa -DPermuteArgsTest.MAX_ARITY=8 test.java.lang.invoke.PermuteArgsTest
 */

/* Examples of manual runs:
 * java -DPermuteArgsTest.{DRY_RUN=true,MAX_ARITY=253} test.java.lang.invoke.PermuteArgsTest
 * java -DPermuteArgsTest.{VERBOSE=true,MAX_ARITY=5} test.java.lang.invoke.PermuteArgsTest
 * java test.java.lang.invoke.PermuteArgsTest list3I[2,0,1] listJLJ[2,0,1]
 */

package test.java.lang.invoke;

import org.testng.annotations.Test;
import test.java.lang.invoke.lib.CodeCacheOverflowProcessor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.lang.invoke.MethodHandles.Lookup;
import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodHandles.permuteArguments;
import static java.lang.invoke.MethodType.methodType;

public class PermuteArgsTest {
    private static final Class<?> CLASS = PermuteArgsTest.class;
    private static final int MAX_ARITY = Integer.getInteger(CLASS.getSimpleName()+".MAX_ARITY", 8);
    private static final boolean DRY_RUN = Boolean.getBoolean(CLASS.getSimpleName()+".DRY_RUN");
    private static final boolean VERBOSE = Boolean.getBoolean(CLASS.getSimpleName()+".VERBOSE") || DRY_RUN;

    static Object list2I(int x, int y) {
        return Arrays.asList(x, y);
    }
    static Object list3I(int x, int y, int z) {
        return Arrays.asList(x, y, z);
    }
    static Object list4I(int w, int x, int y, int z) {
        return Arrays.asList(w, x, y, z);
    }
    static Object list2J(long x, long y) {
        return Arrays.asList(x, y);
    }
    static Object list3J(long x, long y, long z) {
        return Arrays.asList(x, y, z);
    }
    static Object list4J(long w, long x, long y, long z) {
        return Arrays.asList(w, x, y, z);
    }
    static Object list2I2J(int w, int x, long y, long z) {
        return Arrays.asList(w, x, y, z);
    }
    static Object list2J2I(long w, long x, int y, int z) {
        return Arrays.asList(w, x, y, z);
    }
    static Object listLJJ(Object x, long y, long z) {
        return Arrays.asList(x, y, z);
    }
    static Object listJLJ(long x, Object y, long z) {
        return Arrays.asList(x, y, z);
    }
    static Object listJJL(long x, long y, Object z) {
        return Arrays.asList(x, y, z);
    }
    static Object listJLL(long x, Object y, Object z) {
        return Arrays.asList(x, y, z);
    }
    static Object listLJL(Object x, long y, Object z) {
        return Arrays.asList(x, y, z);
    }
    static Object listLLJ(Object x, Object y, long z) {
        return Arrays.asList(x, y, z);
    }
    static Object listJLLJ(long w, Object x, Object y, long z) {
        return Arrays.asList(w, x, y, z);
    }
    static Object listLJJL(Object w, long x, long y, Object z) {
        return Arrays.asList(w, x, y, z);
    }
    static Object listI_etc(int... va) {
        ArrayList<Object> res = new ArrayList<>();
        for (int x : va)  res.add(x);
        return res;
    }
    static Object listIJL_etc(int x, long y, Object z, Object... va) {
        ArrayList<Object> res = new ArrayList<>();
        res.addAll(Arrays.asList(x, y, z));
        res.addAll(Arrays.asList(va));
        return res;
    }

    public static void main(String argv[]) throws Throwable {
        if (argv.length > 0) {
            for (String arg : argv) {
                // arg ::= name[n,...]
                int k = arg.indexOf('[');
                String mhName = arg.substring(0, k).trim();
                String permString = arg.substring(k);
                testOnePermutation(mhName, permString);
            }
            return;
        }
        new PermuteArgsTest().test();
    }

    static int testCases;

    @Test
    public void test() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::test0);
    }

    public void test0() throws Throwable {
        testCases = 0;
        Lookup lookup = lookup();
        for (Method m : lookup.lookupClass().getDeclaredMethods()) {
            if (m.getName().startsWith("list") &&
                Modifier.isStatic(m.getModifiers())) {
                test(m.getName(), lookup.unreflect(m));
            }
        }
        System.out.println("ran a total of "+testCases+" test cases");
    }

    static int jump(int i, int min, int max) {
        if (i >= min && i <= max-1) {
            // jump faster
            int len = max-min;
            if (i < min + len/2)
                i = min + len/2;
            else
                i = max-1;
        }
        return i;
    }

    static void test(String name, MethodHandle mh) throws Throwable {
        if (VERBOSE)
            System.out.println("mh = "+name+" : "+mh+" { "
                               +Arrays.toString(junkArgs(mh.type().parameterArray())));
        int testCases0 = testCases;
        if (!mh.isVarargsCollector()) {
            // normal case
            testPermutations(mh);
        } else {
            // varargs case; add params up to MAX_ARITY
            MethodType mt = mh.type();
            int posArgs = mt.parameterCount() - 1;
            int arity0 = Math.max(3, posArgs);
            for (int arity = arity0; arity <= MAX_ARITY; arity++) {
                MethodHandle mh1;
                try {
                    mh1 = adjustArity(mh, arity);
                } catch (IllegalArgumentException ex) {
                    System.out.println("*** mh = "+name+" : "+mh+"; arity = "+arity+" => "+ex);
                    ex.printStackTrace(System.out);
                    break;  // cannot get this arity for this type
                }
                test("("+arity+")"+name, mh1);
                arity = jump(arity, arity0*2, MAX_ARITY);
            }
        }
        if (VERBOSE)
            System.out.println("ran "+(testCases - testCases0)+" test cases for "+name+" }");
    }

    static MethodHandle adjustArity(MethodHandle mh, int arity) {
        MethodType mt = mh.type();
        int posArgs = mt.parameterCount() - 1;
        Class<?> reptype = mt.parameterType(posArgs).getComponentType();
        MethodType mt1 = mt.dropParameterTypes(posArgs, posArgs+1);
        while (mt1.parameterCount() < arity) {
            Class<?> pt = reptype;
            if (pt == Object.class && posArgs > 0)
                // repeat types cyclically if possible:
                pt = mt1.parameterType(mt1.parameterCount() - posArgs);
            mt1 = mt1.appendParameterTypes(pt);
        }
        try {
            return mh.asType(mt1);
        } catch (WrongMethodTypeException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("cannot convert to type "+mt1+" from "+mh, ex);
        }
    }
    static MethodHandle findTestMH(String name, int[] perm)
            throws ReflectiveOperationException {
        int arity = perm.length;
        Lookup lookup = lookup();
        for (Method m : lookup.lookupClass().getDeclaredMethods()) {
            if (m.getName().equals(name) &&
                Modifier.isStatic(m.getModifiers())) {
                MethodHandle mh = lookup.unreflect(m);
                int mhArity = mh.type().parameterCount();
                if (mh.isVarargsCollector()) {
                    if (mhArity-1 <= arity)
                        return adjustArity(mh, arity);
                } else if (mhArity == arity) {
                    return mh;
                }
            }
        }
        throw new RuntimeException("no such method for arity "+arity+": "+name);
    }

    static void testPermutations(MethodHandle mh) throws Throwable {
        HashSet<String> done = new HashSet<>();
        MethodType mt = mh.type();
        int[] perm = nullPerm(mt.parameterCount());
        final int MARGIN = (perm.length <= 10 ? 2 : 0);
        int testCases0 = testCases;
        for (int j = 0; j <= 1; j++) {
            int maxStart = perm.length-1;
            if (j != 0)  maxStart /= 2;
            for (int start = 0; start <= maxStart; start++) {
                int maxOmit = (maxStart - start) / 2;
                if (start != 0)  maxOmit = 2;
                if (j != 0)  maxOmit = 1;
                for (int omit = 0; omit <= maxOmit; omit++) {
                    int end = perm.length - omit;
                    if (end - start >= 2) {
                        //System.out.println("testPermutations"+Arrays.asList(start, end)+(j == 0 ? "" : " (reverse)"));
                        testPermutations(mh, perm, start, end, done);
                    }
                    omit = jump(omit, (start == 0 && j == 0 ? MARGIN : 0), maxOmit);
                }
                start = jump(start, (j == 0 ? MARGIN : 0), maxStart);
            }
            // do everything in reverse:
            reverse(perm, 0, perm.length);
        }
        switch (perm.length) {
        case 2: assert(testCases - testCases0 == 2); break;
        case 3: assert(testCases - testCases0 == 6); break;
        case 4: assert(testCases - testCases0 == 24); break;
        case 5: assert(testCases - testCases0 == 120); break;
        case 6: assert(testCases - testCases0 > 720/3); break;
        }
    }

    static void testPermutations(MethodHandle mh, int[] perm, int start, int end,
                                 Set<String> done) throws Throwable {
        if (end - start <= 1)  return;
        for (int j = 0; j <= 1; j++) {
            testRotations(mh, perm, start, end, done);
            if (end - start <= 2)  return;
            reverse(perm, start, end);
        }
        if (end - start <= 3)  return;
        int excess4 = (end - start) - 4;
        // composed rotations:
        int start2 = start + 1 + excess4/3;
        int end2   = end       - excess4/3;
        end2 = start2 + Math.min(start == 0 ? 4 : 3, end2 - start2);
        int skips = (perm.length+3)/5;
        for (int i = start; i < end; i++) {
            rotate(perm, start, end);
            if (skips > 1 && ((i-start) + (i-start)/7) % skips != 0)  continue;
            for (int j = 0; j <= 1; j++) {
                testPermutations(mh, perm, start2, end2, done);
                reverse(perm, start, end);
            }
        }
    }

    static void testRotations(MethodHandle mh, int[] perm, int start, int end,
                              Set<String> done) throws Throwable {
        Object[] args = junkArgs(mh.type().parameterArray());
        for (int i = start; i < end; i++) {
            if (done.add(Arrays.toString(perm)))
                testOnePermutation(mh, perm, args);
            rotate(perm, start, end);
        }
    }

    static void testOnePermutation(MethodHandle mh, int[] perm, Object[] args)
            throws Throwable {
        MethodType mt = mh.type();
        MethodType pmt = methodType(mt.returnType(),
                unpermuteArgs(perm, mt.parameterArray(), Class[].class));
        if (VERBOSE)
            System.out.println(Arrays.toString(perm));
        testCases += 1;
        if (DRY_RUN)
            return;
        Object res = permuteArguments(mh, pmt, perm).invokeWithArguments(unpermuteArgs(perm, args));
        String str = String.valueOf(res);
        if (!Arrays.toString(args).equals(str)) {
            System.out.println(Arrays.toString(perm)+" "+str+" *** WRONG ***");
        }
    }

    // For reproducing failures:
    static void testOnePermutation(String mhName, String permString) throws Throwable {
        String s = permString;
        s = s.replace('[', ' ').replace(']', ' ').replace(',', ' ');  // easier to trim spaces
        s = s.trim();
        int[] perm = new int[s.length()];
        int arity = 0;
        while (!s.isEmpty()) {
            int k = s.indexOf(' ');
            if (k < 0)  k = s.length();
            perm[arity++] = Integer.parseInt(s.substring(0, k));
            s = s.substring(k).trim();
        }
        perm = Arrays.copyOf(perm, arity);
        testOnePermutation(mhName, perm);
    }
    static void testOnePermutation(String mhName, int[] perm) throws Throwable {
        MethodHandle mh = findTestMH(mhName, perm);
        System.out.println("mh = "+mhName+" : "+mh+" { "
                           +Arrays.toString(junkArgs(mh.type().parameterArray())));
        Object[] args = junkArgs(mh.type().parameterArray());
        testOnePermutation(mh, perm, args);
        System.out.println("}");
    }

    static Object[] junkArgs(Class<?>[] ptypes) {
        Object[] args = new Object[ptypes.length];
        for (int i = 0; i < ptypes.length; i++) {
            Class<?> pt = ptypes[i];
            Object arg;
            if (pt == Void.class)       arg = null;
            else if (pt == int.class)   arg = i + 101;
            else if (pt == long.class)  arg = i + 10_000_000_001L;
            else                        arg = "#" + (i + 1);
            args[i] = arg;
        }
        return args;
    }

    static int[] nullPerm(int len) {
        int[] perm = new int[len];
        for (int i = 0; i < len; i++)
            perm[i] = i;
        return perm;
    }
    static void rotate(int[] perm) {
        rotate(perm, 0, perm.length);
    }
    static void rotate(int[] perm, int start, int end) {
        int x = perm[end-1];
        for (int j = start; j < end; j++) {
            int y = perm[j]; perm[j] = x; x = y;
        }
    }
    static void reverse(int[] perm) {
        reverse(perm, 0, perm.length);
    }
    static void reverse(int[] perm, int start, int end) {
        int mid = start + (end - start)/2;
        for (int j = start; j < mid; j++) {
            int k = (end-1) - j;
            int x = perm[j]; perm[j] = perm[k]; perm[k] = x;
        }
    }
    // Permute the args according to the inverse of perm.
    static Object[] unpermuteArgs(int[] perm, Object[] args) {
        return unpermuteArgs(perm, args, Object[].class);
    }
    static <T> T[] unpermuteArgs(int[] perm, T[] args, Class<T[]> Tclass) {
        T[] res = Arrays.copyOf(new Object[0], perm.length, Tclass);
        for (int i = 0; i < perm.length; i++)
            res[perm[i]] = args[i];
        return res;
    }
}
