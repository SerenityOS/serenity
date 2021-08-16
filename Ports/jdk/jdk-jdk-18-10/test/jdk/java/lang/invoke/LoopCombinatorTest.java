/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * @bug 8139885
 * @bug 8150635
 * @bug 8150956
 * @bug 8150957
 * @bug 8151179
 * @bug 8152667
 * @bug 8153637
 * @bug 8154751
 * @bug 8154754
 * @bug 8167974
 * @run testng/othervm -ea -esa test.java.lang.invoke.LoopCombinatorTest
 */

package test.java.lang.invoke;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.*;

import static java.lang.invoke.MethodType.methodType;

import static org.testng.AssertJUnit.*;

import org.testng.annotations.*;

/**
 * Tests for the loop combinators introduced in JEP 274.
 */
public class LoopCombinatorTest {

    static final Lookup LOOKUP = MethodHandles.lookup();

    @Test
    public static void testLoopFac() throws Throwable {
        MethodHandle[] counterClause = new MethodHandle[]{Fac.MH_zero, Fac.MH_inc};
        MethodHandle[] accumulatorClause = new MethodHandle[]{Fac.MH_one, Fac.MH_mult, Fac.MH_pred, Fac.MH_fin};
        MethodHandle loop = MethodHandles.loop(counterClause, accumulatorClause);
        assertEquals(Fac.MT_fac, loop.type());
        assertEquals(120, loop.invoke(5));
    }

    @Test
    public static void testLoopFacNullInit() throws Throwable {
        // null initializer for counter, should initialize to 0
        MethodHandle[] counterClause = new MethodHandle[]{null, Fac.MH_inc};
        MethodHandle[] accumulatorClause = new MethodHandle[]{Fac.MH_one, Fac.MH_mult, Fac.MH_pred, Fac.MH_fin};
        MethodHandle loop = MethodHandles.loop(counterClause, accumulatorClause);
        assertEquals(Fac.MT_fac, loop.type());
        assertEquals(120, loop.invoke(5));
    }

    @Test
    public static void testLoopNullInit() throws Throwable {
        // null initializer for counter, should initialize to 0, one-clause loop
        MethodHandle[] counterClause = new MethodHandle[]{null, Loop.MH_inc, Loop.MH_pred, Loop.MH_fin};
        MethodHandle loop = MethodHandles.loop(counterClause);
        assertEquals(Loop.MT_loop, loop.type());
        assertEquals(10, loop.invoke(10));
    }

    @Test
    public static void testLoopVoid1() throws Throwable {
        // construct a post-checked loop that only does one iteration and has a void body and void local state
        MethodHandle loop = MethodHandles.loop(new MethodHandle[]{Empty.MH_f, Empty.MH_f, Empty.MH_pred, null});
        assertEquals(MethodType.methodType(void.class), loop.type());
        loop.invoke();
    }

    @Test
    public static void testLoopVoid2() throws Throwable {
        // construct a post-checked loop that only does one iteration and has a void body and void local state,
        // initialized implicitly from the step type
        MethodHandle loop = MethodHandles.loop(new MethodHandle[]{null, Empty.MH_f, Empty.MH_pred, null});
        assertEquals(MethodType.methodType(void.class), loop.type());
        loop.invoke();
    }

    @Test
    public static void testLoopVoid3() throws Throwable {
        // construct a post-checked loop that only does one iteration and has a void body and void local state,
        // and that has a void finalizer
        MethodHandle loop = MethodHandles.loop(new MethodHandle[]{null, Empty.MH_f, Empty.MH_pred, Empty.MH_f});
        assertEquals(MethodType.methodType(void.class), loop.type());
        loop.invoke();
    }

    @Test
    public static void testLoopFacWithVoidState() throws Throwable {
        // like testLoopFac, but with additional void state that outputs a dot
        MethodHandle[] counterClause = new MethodHandle[]{Fac.MH_zero, Fac.MH_inc};
        MethodHandle[] accumulatorClause = new MethodHandle[]{Fac.MH_one, Fac.MH_mult, Fac.MH_pred, Fac.MH_fin};
        MethodHandle[] dotClause = new MethodHandle[]{null, Fac.MH_dot};
        MethodHandle loop = MethodHandles.loop(counterClause, accumulatorClause, dotClause);
        assertEquals(Fac.MT_fac, loop.type());
        assertEquals(120, loop.invoke(5));
    }

    @Test
    public static void testLoopVoidInt() throws Throwable {
        // construct a post-checked loop that only does one iteration and has a void body and void local state,
        // and that returns a constant
        MethodHandle loop = MethodHandles.loop(new MethodHandle[]{null, Empty.MH_f, Empty.MH_pred, Empty.MH_c});
        assertEquals(MethodType.methodType(int.class), loop.type());
        assertEquals(23, loop.invoke());
    }

    @Test
    public static void testLoopWithVirtuals() throws Throwable {
        // construct a loop (to calculate factorial) that uses a mix of static and virtual methods
        MethodHandle[] counterClause = new MethodHandle[]{null, LoopWithVirtuals.permute(LoopWithVirtuals.MH_inc)};
        MethodHandle[] accumulatorClause = new MethodHandle[]{
                // init function must indicate the loop arguments (there is no other means to determine them)
                MethodHandles.dropArguments(LoopWithVirtuals.MH_one, 0, LoopWithVirtuals.class),
                LoopWithVirtuals.permute(LoopWithVirtuals.MH_mult),
                LoopWithVirtuals.permute(LoopWithVirtuals.MH_pred),
                LoopWithVirtuals.permute(LoopWithVirtuals.MH_fin)
        };
        MethodHandle loop = MethodHandles.loop(counterClause, accumulatorClause);
        assertEquals(LoopWithVirtuals.MT_loop, loop.type());
        assertEquals(120, loop.invoke(new LoopWithVirtuals(), 5));
    }

    @Test
    public static void testLoopOmitPred() throws Throwable {
        // construct a loop to calculate factorial that omits a predicate
        MethodHandle[] counterClause = new MethodHandle[]{null, Fac.MH_inc, null, Fac.MH_fin};
        MethodHandle[] accumulatorClause = new MethodHandle[]{Fac.MH_one, Fac.MH_mult, Fac.MH_pred, Fac.MH_fin};
        MethodHandle loop = MethodHandles.loop(counterClause, accumulatorClause);
        assertEquals(Fac.MT_fac, loop.type());
        assertEquals(120, loop.invoke(5));
    }

    @DataProvider
    static Object[][] negativeTestData() {
        MethodHandle i0 = MethodHandles.constant(int.class, 0);
        MethodHandle ii = MethodHandles.dropArguments(i0, 0, int.class, int.class);
        MethodHandle id = MethodHandles.dropArguments(i0, 0, int.class, double.class);
        MethodHandle i3 = MethodHandles.dropArguments(i0, 0, int.class, int.class, int.class);
        List<MethodHandle> inits = Arrays.asList(ii, id, i3);
        List<Class<?>> ints3 = Arrays.asList(int.class, int.class, int.class);
        List<Class<?>> ints4 = Arrays.asList(int.class, int.class, int.class, int.class);
        List<MethodHandle> finis = Arrays.asList(Fac.MH_fin, Fac.MH_inc, Counted.MH_step);
        List<MethodHandle> preds1 = Arrays.asList(null, null, null);
        List<MethodHandle> preds2 = Arrays.asList(null, Fac.MH_fin, null);
        MethodHandle eek = MethodHandles.dropArguments(i0, 0, int.class, int.class, double.class);
        List<MethodHandle> nesteps = Arrays.asList(Fac.MH_inc, eek, Fac.MH_dot);
        List<MethodHandle> nepreds = Arrays.asList(null, Fac.MH_pred, null);
        List<MethodHandle> nefinis = Arrays.asList(null, Fac.MH_fin, null);
        List<MethodHandle> lvsteps = Arrays.asList(LoopWithVirtuals.MH_inc, LoopWithVirtuals.MH_mult);
        List<MethodHandle> lvpreds = Arrays.asList(null, LoopWithVirtuals.MH_pred);
        List<MethodHandle> lvfinis = Arrays.asList(null, LoopWithVirtuals.MH_fin);
        return new Object[][] {
                {null, "null or no clauses passed"},
                {new MethodHandle[][]{}, "null or no clauses passed"},
                {new MethodHandle[][]{{null, Fac.MH_inc}, {Fac.MH_one, null, Fac.MH_mult, Fac.MH_pred, Fac.MH_fin}},
                        "All loop clauses must be represented as MethodHandle arrays with at most 4 elements."},
                {new MethodHandle[][]{{null, Fac.MH_inc}, null}, "null clauses are not allowed"},
                {new MethodHandle[][]{{Fac.MH_zero, Fac.MH_dot}},
                        "clause 0: init and step return types must match: int != void"},
                {new MethodHandle[][]{{ii}, {id}, {i3}},
                        "found non-effectively identical init parameter type lists: " + inits +
                                " (common suffix: " + ints3 + ")"},
                {new MethodHandle[][]{{null, Fac.MH_inc, null, Fac.MH_fin}, {null, Fac.MH_inc, null, Fac.MH_inc},
                        {null, Counted.MH_start, null, Counted.MH_step}},
                        "found non-identical finalizer return types: " + finis + " (return type: int)"},
                {new MethodHandle[][]{{Fac.MH_zero, Fac.MH_inc}, {Fac.MH_one, Fac.MH_mult, null, Fac.MH_fin},
                        {null, Fac.MH_dot}}, "no predicate found: " + preds1},
                {new MethodHandle[][]{{Fac.MH_zero, Fac.MH_inc}, {Fac.MH_one, Fac.MH_mult, Fac.MH_fin, Fac.MH_fin},
                        {null, Fac.MH_dot}}, "predicates must have boolean return type: " + preds2},
                {new MethodHandle[][]{{Fac.MH_zero, Fac.MH_inc}, {Fac.MH_one, eek, Fac.MH_pred, Fac.MH_fin},
                        {null, Fac.MH_dot}},
                        "found non-effectively identical parameter type lists:\nstep: " + nesteps +
                                "\npred: " + nepreds + "\nfini: " + nefinis + " (common parameter sequence: " + ints3 + ")"},
                {new MethodHandle[][]{{null, LoopWithVirtuals.MH_inc},
                        {LoopWithVirtuals.MH_one, LoopWithVirtuals.MH_mult, LoopWithVirtuals.MH_pred, LoopWithVirtuals.MH_fin}},
                        "found non-effectively identical parameter type lists:\nstep: " + lvsteps +
                                "\npred: " + lvpreds + "\nfini: " + lvfinis + " (common parameter sequence: " + ints4 + ")"}
        };
    }

    static final MethodHandle MH_loop;

    static {
        try {
            MH_loop = LOOKUP.findStatic(MethodHandles.class, "loop", methodType(MethodHandle.class, MethodHandle[][].class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Test(dataProvider = "negativeTestData")
    public static void testLoopNegative(MethodHandle[][] clauses, String expectedMessage) throws Throwable {
        boolean caught = false;
        try {
            MH_loop.invokeWithArguments((Object[]) clauses);
        } catch (IllegalArgumentException iae) {
            assertEquals(expectedMessage, iae.getMessage());
            caught = true;
        }
        assertTrue(caught);
    }

    @Test(dataProvider = "whileLoopTestData")
    public static void testWhileLoop(MethodHandle MH_zero,
                                     MethodHandle MH_pred,
                                     MethodHandle MH_step,
                                     String messageOrNull) throws Throwable {
        // int i = 0; while (i < limit) { ++i; } return i; => limit
        try {
            MethodHandle loop = MethodHandles.whileLoop(MH_zero, MH_pred, MH_step);
            assert messageOrNull == null;
            if (MH_step.type().equals(While.MH_step.type()))
                assertEquals(While.MT_while, loop.type());
            assertEquals(MH_step.type().dropParameterTypes(0, 1), loop.type());
            while (loop.type().parameterCount() > 1)  loop = snip(loop);
            assertEquals(23, loop.invoke(23));
        } catch (IllegalArgumentException iae) {
            assert messageOrNull != null;
            assertEqualsFIXME(messageOrNull, iae.getMessage());
        }
    }

    static void assertEqualsFIXME(String expect, String actual) {
        if (!expect.equals(actual)) {
            // just issue a warning
            System.out.println("*** "+actual+"\n != "+expect);
        }
    }

    @DataProvider
    static Object[][] whileLoopTestData() {
        MethodHandle
            zeroI = While.MH_zero,
            zeroX = snip(zeroI),
            zeroIB = slap(zeroI, byte.class),
            predII = While.MH_pred,
            predIX = snip(predII),
            predIIB = slap(predII, byte.class),
            stepII = While.MH_step,
            stepIX = snip(stepII),
            stepIIB = slap(stepII, byte.class)
            ;
        return new Object[][] {
            // normal while loop clauses, perhaps with effectively-identical reductions
            {zeroI, predII, stepII, null},
            {zeroX, predII, stepII, null},
            {null, predII, stepII, null},
            // expanded while loop clauses
            {zeroIB, predIIB, stepIIB, null},
            {zeroI, predIIB, stepIIB, null},
            {null, predIIB, stepIIB, null},
            {zeroIB, predII, stepIIB, null},
            {zeroX, predII, stepIIB, null},
            {null, predII, stepIIB, null},
            // short step clauses cause errors
            {zeroI, predII, stepIX, "loop predicate must match: (int,int)boolean != (int)boolean"},
            {zeroIB, predIX, stepIX, "loop initializer must match: (int,byte)int != ()int"},
            // bad body type
            {zeroI, predII, tweak(stepII, -1, char.class), "body function must match: (int,int)char != (char,int,int)char"},
            {zeroI, predII, tweak(stepII,  0, char.class), "body function must match: (char,int)int != (int,char,int)int"},
            // bad pred type
            {zeroI, tweak(predII, -1, char.class), stepII, "loop predicate must match: (int,int)char != (int,int)boolean"},
            {zeroI, tweak(predII,  0, char.class), stepII, "loop predicate must match: (char,int)boolean != (int,int)boolean"},
            // bad init type
            {tweak(zeroI, -1, char.class), predII, stepII, "loop initializer must match: (int)char != (int)int"},
            {tweak(zeroI,  0, char.class), predII, stepII, "loop initializer must match: (char)int != (int)int"},
        };
    }

    // tweak the type of an MH
    static MethodHandle tweak(MethodHandle mh, int argPos, Class<?> type) {
        MethodType mt = mh.type();
        if (argPos == -1)
            mt = mt.changeReturnType(type);
        else
            mt = mt.changeParameterType(argPos, type);
        return MethodHandles.explicitCastArguments(mh, mt);
    }
    // snip off an MH argument, hard-wiring to zero
    static MethodHandle snip(MethodHandle mh, int argPos) {
        if (argPos < 0)  return null;  // special case for optional args
        Class<?> argType = mh.type().parameterType(argPos);
        Object zero;
        try {
            zero = MethodHandles.zero(argType).invoke();
        } catch (Throwable ex) {
            throw new AssertionError(ex);
        }
        return MethodHandles.insertArguments(mh, argPos, zero);
    }
    static MethodHandle snip(MethodHandle mh) {
        return snip(mh, mh.type().parameterCount()-1);
    }
    // slap on an extra type on the end of the MH
    static MethodHandle slap(MethodHandle mh, Class<?> addType) {
        return MethodHandles.dropArguments(mh, mh.type().parameterCount(), addType);
    }

    @Test
    public static void testWhileLoopNoIteration() throws Throwable {
        // a while loop that never executes its body because the predicate evaluates to false immediately
        MethodHandle loop = MethodHandles.whileLoop(While.MH_initString, While.MH_predString, While.MH_stepString);
        assertEquals(While.MT_string, loop.type());
        assertEquals("a", loop.invoke());
    }

    @Test(dataProvider = "whileLoopTestData")
    public static void testDoWhileLoop(MethodHandle MH_zero,
                                       MethodHandle MH_pred,
                                       MethodHandle MH_step,
                                       String messageOrNull) throws Throwable {
        // int i = 0; do { ++i; } while (i < limit); return i; => limit
        try {
            MethodHandle loop = MethodHandles.doWhileLoop(MH_zero, MH_step, MH_pred);
            assert messageOrNull == null;
            if (MH_step.type().equals(While.MH_step.type()))
                assertEquals(While.MT_while, loop.type());
            assertEquals(MH_step.type().dropParameterTypes(0, 1), loop.type());
            while (loop.type().parameterCount() > 1)  loop = snip(loop);
            assertEquals(23, loop.invoke(23));
        } catch (IllegalArgumentException iae) {
            assert messageOrNull != null;
            if (!messageOrNull.equals(iae.getMessage())) {
                // just issue a warning
                System.out.println("*** "+messageOrNull+"\n != "+iae.getMessage());
            }
        }
    }

    @Test
    public static void testDoWhileBadInit() throws Throwable {
        boolean caught = false;
        try {
            While w = new While();
            MethodHandle loop = MethodHandles.doWhileLoop(MethodHandles.empty(methodType(char.class)),
                                                          While.MH_voidBody.bindTo(w),
                                                          While.MH_voidPred.bindTo(w));
        } catch (IllegalArgumentException iae) {
            assertEquals("loop initializer must match: ()char != (int)void", iae.getMessage());
            caught = true;
        }
        assertTrue(caught);
    }

    @Test
    public static void testWhileZip() throws Throwable {
        MethodHandle loop = MethodHandles.doWhileLoop(While.MH_zipInitZip, While.MH_zipStep, While.MH_zipPred);
        assertEquals(While.MT_zip, loop.type());
        List<String> a = Arrays.asList("a", "b", "c", "d");
        List<String> b = Arrays.asList("e", "f", "g", "h");
        List<String> zipped = Arrays.asList("a", "e", "b", "f", "c", "g", "d", "h");
        assertEquals(zipped, (List<String>) loop.invoke(a.iterator(), b.iterator()));
    }

    @Test
    public static void testWhileBadInit() throws Throwable {
        boolean caught = false;
        try {
            While w = new While();
            MethodHandle loop = MethodHandles.whileLoop(MethodHandles.empty(methodType(void.class, char.class)),
                                                        While.MH_voidPred.bindTo(w),
                                                        While.MH_voidBody.bindTo(w));
        } catch (IllegalArgumentException iae) {
            assertEquals("loop initializer must match: (char)void != (int)void", iae.getMessage());
            caught = true;
        }
        assertTrue(caught);
    }

    @Test
    public static void testWhileVoidInit() throws Throwable {
        While w = new While();
        int v = 5;
        MethodHandle loop = MethodHandles.whileLoop(While.MH_voidInit.bindTo(w), While.MH_voidPred.bindTo(w),
                While.MH_voidBody.bindTo(w));
        assertEquals(While.MT_void, loop.type());
        loop.invoke(v);
        assertEquals(v, w.i);
    }

    @Test
    public static void testDoWhileVoidInit() throws Throwable {
        While w = new While();
        int v = 5;
        MethodHandle loop = MethodHandles.doWhileLoop(While.MH_voidInit.bindTo(w), While.MH_voidBody.bindTo(w),
                While.MH_voidPred.bindTo(w));
        assertEquals(While.MT_void, loop.type());
        loop.invoke(v);
        assertEquals(v, w.i);
    }

    @DataProvider
    static Object[][] nullArgs() {
        MethodHandle c = MethodHandles.constant(int.class, 1);
        return new Object[][]{{null, c}, {c, null}};
    }

    @Test(dataProvider = "nullArgs", expectedExceptions = NullPointerException.class)
    public static void testWhileNullArgs(MethodHandle pred, MethodHandle body) {
        MethodHandles.whileLoop(null, pred, body);
    }

    @Test(dataProvider = "nullArgs", expectedExceptions = NullPointerException.class)
    public static void testDoWhileNullArgs(MethodHandle body, MethodHandle pred) {
        MethodHandles.whileLoop(null, body, pred);
    }

    @Test
    public static void testCountedLoop() throws Throwable {
        // String s = "Lambdaman!"; for (int i = 0; i < 13; ++i) { s = "na " + s; } return s; => a variation on a well known theme
        MethodHandle fit13 = MethodHandles.dropArguments(MethodHandles.constant(int.class, 13), 0, String.class);
        MethodHandle loop = MethodHandles.countedLoop(fit13, Counted.MH_start, Counted.MH_step);
        assertEquals(Counted.MT_counted, loop.type());
        assertEquals("na na na na na na na na na na na na na Lambdaman!", loop.invoke("Lambdaman!"));
    }

    @Test
    public static void testCountedLoopVoidInit() throws Throwable {
        MethodHandle fit5 = MethodHandles.constant(int.class, 5);
        for (int i = 0; i < 8; i++) {
            MethodHandle zero = MethodHandles.zero(void.class);
            MethodHandle init = fit5;
            MethodHandle body = Counted.MH_printHello;
            boolean useNull = (i & 1) != 0, addInitArg = (i & 2) != 0, addBodyArg = (i & 4) != 0;
            if (useNull)    zero = null;
            if (addInitArg) init = MethodHandles.dropArguments(init, 0, int.class);
            if (addBodyArg) body = MethodHandles.dropArguments(body, 1, int.class);
            System.out.println("testCountedLoopVoidInit i="+i+" : "+Arrays.asList(init, zero, body));
            MethodHandle loop = MethodHandles.countedLoop(init, zero, body);
            MethodType expectedType = Counted.MT_countedPrinting;
            if (addInitArg || addBodyArg)
                expectedType = expectedType.insertParameterTypes(0, int.class);
            assertEquals(expectedType, loop.type());
            if (addInitArg || addBodyArg)
                loop.invoke(99);
            else
                loop.invoke();
        }
    }

    @Test
    public static void testCountedArrayLoop() throws Throwable {
        // int[] a = new int[]{0}; for (int i = 0; i < 13; ++i) { ++a[0]; } => a[0] == 13
        MethodHandle fit13 = MethodHandles.dropArguments(MethodHandles.constant(int.class, 13), 0, int[].class);
        MethodHandle loop = MethodHandles.countedLoop(fit13, null, Counted.MH_stepUpdateArray);
        assertEquals(Counted.MT_arrayCounted, loop.type());
        int[] a = new int[]{0};
        loop.invoke(a);
        assertEquals(13, a[0]);
    }

    @Test
    public static void testCountedPrintingLoop() throws Throwable {
        MethodHandle fit5 = MethodHandles.constant(int.class, 5);
        MethodHandle loop = MethodHandles.countedLoop(fit5, null, Counted.MH_printHello);
        assertEquals(Counted.MT_countedPrinting, loop.type());
        loop.invoke();
    }

    @Test(expectedExceptions = NullPointerException.class)
    public static void testCountedLoopNullBody() throws Throwable {
        MethodHandle h5 = MethodHandles.constant(int.class, 5);
        MethodHandle h13 = MethodHandles.constant(int.class, 13);
        MethodHandle loop = MethodHandles.countedLoop(h5, h13, null);
        assertEquals(methodType(int.class), loop.type());
        assertEquals(13, loop.invoke());
    }

    @Test(expectedExceptions = NullPointerException.class)
    public static void testCountedLoopNullIterations() throws Throwable {
        MethodHandle loop = MethodHandles.countedLoop(null, null, null);
        assertEquals(methodType(void.class), loop.type());
        loop.invoke();
    }

    @Test(expectedExceptions = NullPointerException.class)
    public static void testCountedLoopNullInitAndBody() throws Throwable {
        MethodHandle loop = MethodHandles.countedLoop(MethodHandles.constant(int.class, 5), null, null);
        assertEquals(methodType(void.class), loop.type());
        loop.invoke();
    }

    @DataProvider
    static Object[][] countedLoopBodyParameters() {
        Class<?> V = String.class, I = int.class, A = List.class;
        // return types are of these forms:
        //    {count = int(A...), init = V(A...), body = V(V, I, A...)}
        return new Object[][] {
            // body leads determining A...
            {methodType(I), methodType(V), methodType(V, V, I)},
            {methodType(I), methodType(V), methodType(V, V, I, A)},
            {methodType(I,A), methodType(V), methodType(V, V, I, A)},
            {methodType(I), methodType(V,A), methodType(V, V, I, A)},
            // body leads, with void V
            {methodType(I), methodType(void.class), methodType(void.class, I)},
            {methodType(I), methodType(void.class), methodType(void.class, I, A)},
            {methodType(I,A), methodType(void.class), methodType(void.class, I, A)},
            {methodType(I), methodType(void.class,A), methodType(void.class, I, A)},
            // count leads determining A..., but only if body drops all A...
            {methodType(I,A), methodType(V), methodType(V, V, I)},
            {methodType(I,A), methodType(V,A), methodType(V, V, I)},
            // count leads, with void V
            {methodType(I,A), methodType(void.class), methodType(void.class, I)},
            {methodType(I,A), methodType(void.class,A), methodType(void.class, I)},
        };
    }

    @Test(dataProvider = "countedLoopBodyParameters")
    public static void testCountedLoopBodyParameters(MethodType countType, MethodType initType, MethodType bodyType) throws Throwable {
        MethodHandle loop = MethodHandles.countedLoop(
                MethodHandles.empty(countType),
                initType == null ? null : MethodHandles.empty(initType),
                MethodHandles.empty(bodyType));
        // The rule:  If body takes the minimum number of parameters, then take what countType offers.
        // The initType has to just roll with whatever the other two agree on.
        int innerParams = (bodyType.returnType() == void.class ? 1 : 2);
        MethodType expectType = bodyType.dropParameterTypes(0, innerParams);
        if (expectType.parameterCount() == 0)
            expectType = expectType.insertParameterTypes(0, countType.parameterList());
        assertEquals(expectType, loop.type());
    }

    @Test(dataProvider = "countedLoopBodyParameters")
    public static void testCountedLoopBodyParametersNullInit(MethodType countType, MethodType initType, MethodType bodyType) throws Throwable {
        testCountedLoopBodyParameters(countType, null, bodyType);
    }

    @Test
    public static void testCountedLoopStateInitializedToNull() throws Throwable {
        MethodHandle loop = MethodHandles.countedLoop(MethodHandles.constant(int.class, 5),
                MethodHandles.empty(methodType(String.class)), Counted.MH_stateBody);
        assertEquals(Counted.MT_bodyDeterminesState, loop.type());
        assertEquals("sssssnull01234", loop.invoke());
    }

    @Test
    public static void testCountedLoopArgsDefinedByIterations() throws Throwable {
        MethodHandle iterations =
                MethodHandles.dropArguments(MethodHandles.constant(int.class, 3), 0, String.class);
        MethodHandle loop = MethodHandles.countedLoop(iterations,
                MethodHandles.empty(iterations.type().changeReturnType(String.class)), Counted.MH_append);
        assertEquals(Counted.MT_iterationsDefineArgs, loop.type());
        assertEquals("hello012", loop.invoke("hello"));
    }

    @Test
    public static void testCountedRangeLoop() throws Throwable {
        // String s = "Lambdaman!"; for (int i = -5; i < 8; ++i) { s = "na " + s; } return s; => a well known theme
        MethodHandle fitm5 = MethodHandles.dropArguments(Counted.MH_m5, 0, String.class);
        MethodHandle fit8 = MethodHandles.dropArguments(Counted.MH_8, 0, String.class);
        MethodHandle loop = MethodHandles.countedLoop(fitm5, fit8, Counted.MH_start, Counted.MH_step);
        assertEquals(Counted.MT_counted, loop.type());
        assertEquals("na na na na na na na na na na na na na Lambdaman!", loop.invoke("Lambdaman!"));
    }

    @Test
    public static void testCountedLoopCounterInit() throws Throwable {
        // int x = 0; for (int i = 0; i < 5; ++i) { x += i; } return x; => 10
        // (only if counter's first value in body is 0)
        MethodHandle iter = MethodHandles.constant(int.class, 5);
        MethodHandle init = MethodHandles.constant(int.class, 0);
        MethodHandle body = Counted.MH_addCounter;
        MethodHandle loop = MethodHandles.countedLoop(iter, init, body);
        assertEquals(Counted.MT_counterInit, loop.type());
        assertEquals(10, loop.invoke());
    }

    @Test
    public static void testCountedLoopEmpty() throws Throwable {
        // for (int i = 0; i < 5; ++i) { /* empty */ }
        MethodHandle loop = MethodHandles.countedLoop(MethodHandles.constant(int.class, 5), null,
                MethodHandles.empty(methodType(void.class, int.class)));
        assertEquals(methodType(void.class), loop.type());
        loop.invoke();
    }

    @Test
    public static void testCountedRangeLoopEmpty() throws Throwable {
        // for (int i = -5; i < 5; ++i) { /* empty */ }
        MethodHandle loop = MethodHandles.countedLoop(MethodHandles.constant(int.class, -5),
                MethodHandles.constant(int.class, 5), null, MethodHandles.empty(methodType(void.class, int.class)));
        assertEquals(methodType(void.class), loop.type());
        loop.invoke();
    }

    @DataProvider
    static Object[][] countedLoopNegativeData() {
        MethodHandle dummy = MethodHandles.zero(void.class);
        MethodHandle one = MethodHandles.constant(int.class, 1);
        MethodHandle oneString = MethodHandles.dropArguments(one, 0, String.class);
        MethodHandle oneDouble = MethodHandles.dropArguments(one, 0, double.class);
        return new Object[][]{
                {dummy, one, dummy, dummy, String.format("start/end must return int %s, %s", dummy, one)},
                {one, dummy, dummy, dummy, String.format("start/end must return int %s, %s", one, dummy)},
                {oneString, oneDouble, dummy, dummy,
                        String.format("start and end parameter types must match: %s != %s", oneString.type(),
                                oneDouble.type())},
                {oneString, oneString, dummy, dummy,
                        String.format("start/end and init parameter types must match: %s != %s", oneString.type(),
                                dummy.type())},
                {one, one, null, dummy, String.format("actual and expected body signatures must match: %s != %s",
                        dummy.type(), dummy.type().appendParameterTypes(int.class))}
        };
    }

    @Test(dataProvider = "countedLoopNegativeData")
    public static void testCountedLoopNegative(MethodHandle start, MethodHandle end, MethodHandle init,
                                               MethodHandle body, String msg) {
        if (true)  return;  //%%%FIXME%%%%
        boolean caught = false;
        try {
            MethodHandles.countedLoop(start, end, init, body);
        } catch (IllegalArgumentException iae) {
            assertEquals(msg, iae.getMessage());
            caught = true;
        }
        assertTrue(caught);
    }

    @Test
    public static void testIterateSum() throws Throwable {
        // Integer[] a = new Integer[]{1,2,3,4,5,6}; int sum = 0; for (int e : a) { sum += e; } return sum; => 21
        MethodHandle loop = MethodHandles.iteratedLoop(Iterate.MH_sumIterator, Iterate.MH_sumInit, Iterate.MH_sumStep);
        assertEquals(Iterate.MT_sum, loop.type());
        assertEquals(21, loop.invoke(new Integer[]{1, 2, 3, 4, 5, 6}));
    }

    @DataProvider
    static Object[][] iteratorInits() {
        return new Object[][]{{Iterate.MH_iteratorFromList}, {Iterate.MH_iteratorFromIterable}, {null}};
    }

    @Test(dataProvider = "iteratorInits")
    public static void testIterateReverse(MethodHandle iterator) throws Throwable {
        // this test uses List as its loop state type; don't try to change that
        if (iterator != null)
            iterator = iterator.asType(iterator.type().changeParameterType(0, List.class));
        for (int i = 0; i < 4; i++) {
            MethodHandle init = Iterate.MH_reverseInit, body = Iterate.MH_reverseStep;
            boolean snipInit = (i & 1) != 0, snipBody = (i & 2) != 0;
            if (snipInit)  init = snip(init);
            if (snipBody)  body = snip(body);
            if (!snipInit && snipBody && iterator == null) {
                // Body does not determine (A...), so the default guy just picks Iterable.
                // If body insisted on (List), the default guy would adjust himself.
                // Init has no authority to change the (A...), so must patch init.
                // All according to plan!
                init = slap(snip(init), Iterable.class);
            }
            System.out.println("testIterateReverse i="+i+" : "+Arrays.asList(iterator, init, body));
            MethodHandle loop = MethodHandles.iteratedLoop(iterator, init, body);
            MethodType expectedType = Iterate.MT_reverse;
            if (iterator == null && i >= 2)
                expectedType = expectedType.changeParameterType(0, Iterable.class);
            assertEquals(expectedType, loop.type());
            List<String> list = Arrays.asList("a", "b", "c", "d", "e");
            List<String> reversedList = Arrays.asList("e", "d", "c", "b", "a");
            assertEquals(reversedList, (List<String>) loop.invoke(list));
        }
    }

    @Test(dataProvider = "iteratorInits")
    public static void testIterateLength(MethodHandle iterator) throws Throwable {
        MethodHandle body = Iterate.MH_lengthStep;
        MethodHandle init = Iterate.MH_lengthInit;
        MethodType expectedType = Iterate.MT_length;
        int barity = body.type().parameterCount();
        Class<?> iteratorSource = iterator == null ? null : iterator.type().parameterType(0);
        if (iterator != null && iteratorSource != body.type().parameterType(barity-1)) {
            // adjust body to accept the other type
            body = body.asType(body.type().changeParameterType(barity-1, iteratorSource));
            init = init.asType(init.type().changeParameterType(0, iteratorSource));
            expectedType = expectedType.changeParameterType(0, iteratorSource);
        }
        for (;; init = snip(init)) {
            System.out.println("testIterateLength.init = "+init);
            MethodHandle loop = MethodHandles.iteratedLoop(iterator, init, body);
            assertEquals(expectedType, loop.type());
            List<Double> list = Arrays.asList(23.0, 148.0, 42.0);
            assertEquals(list.size(), (int) loop.invoke(list));
            if (init == null)  break;
        }
    }

    @Test(dataProvider = "iteratorInits")
    public static void testIterateMap(MethodHandle iterator) throws Throwable {
        MethodHandle body = Iterate.MH_mapStep;
        MethodHandle init = Iterate.MH_mapInit;
        MethodType expectedType = Iterate.MT_map;
        int barity = body.type().parameterCount();
        Class<?> iteratorSource = iterator == null ? null : iterator.type().parameterType(0);
        if (iterator != null && iteratorSource != body.type().parameterType(barity-1)) {
            // adjust body to accept the other type
            body = body.asType(body.type().changeParameterType(barity-1, iteratorSource));
            init = init.asType(init.type().changeParameterType(0, iteratorSource));
            expectedType = expectedType.changeParameterType(0, iteratorSource);
        }
        for (; init != null; init = snip(init)) {
            System.out.println("testIterateMap.init = "+init);
            MethodHandle loop = MethodHandles.iteratedLoop(iterator, init, body);
            assertEquals(expectedType, loop.type());
            List<String> list = Arrays.asList("Hello", "world", "!");
            List<String> upList = Arrays.asList("HELLO", "WORLD", "!");
            assertEquals(upList, (List<String>) loop.invoke(list));
        }
    }

    @Test(dataProvider = "iteratorInits")
    public static void testIteratePrint(MethodHandle iterator) throws Throwable {
        MethodHandle body = Iterate.MH_printStep;
        MethodType expectedType = Iterate.MT_print;
        int barity = body.type().parameterCount();
        Class<?> iteratorSource = iterator == null ? null : iterator.type().parameterType(0);
        if (iterator != null && iteratorSource != body.type().parameterType(barity-1)) {
            // adjust body to accept the other type
            body = body.asType(body.type().changeParameterType(barity-1, iteratorSource));
            expectedType = expectedType.changeParameterType(0, iteratorSource);
        }
        MethodHandle loop = MethodHandles.iteratedLoop(iterator, null, body);
        assertEquals(expectedType, loop.type());
        loop.invoke(Arrays.asList("hello", "world"));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public static void testIterateNullBody() {
        MethodHandles.iteratedLoop(MethodHandles.empty(methodType(Iterator.class, int.class)),
                MethodHandles.identity(int.class), null);
    }

    @DataProvider
    static Object[][] wrongIteratorTypes() {
        return new Object[][]{{void.class}, {Object.class}, {Iterable.class}};
    }

    @Test(dataProvider = "wrongIteratorTypes")
    public static void testIterateVoidIterator(Class<?> it) {
        boolean caught = false;
        MethodType v = methodType(it);
        try {
            MethodHandles.iteratedLoop(MethodHandles.empty(v), null, MethodHandles.empty(v));
        } catch(IllegalArgumentException iae) {
            assertEqualsFIXME("iteratedLoop first argument must have Iterator return type", iae.getMessage());
            caught = true;
        }
        assertTrue(caught);
    }

    @Test(dataProvider = "iteratorInits")
    public static void testIterateVoidInit(MethodHandle iterator) throws Throwable {
        // this test uses List as its loop state type; don't try to change that
        if (iterator != null)
            iterator = iterator.asType(iterator.type().changeParameterType(0, List.class));
        MethodHandle loop = MethodHandles.iteratedLoop(iterator, Iterate.MH_voidInit, Iterate.MH_printStep);
        assertEquals(Iterate.MT_print, loop.type());
        loop.invoke(Arrays.asList("hello", "world"));
    }

    @DataProvider
    static Object[][] iterateParameters() {
        MethodType i = methodType(int.class);
        MethodType sil_v = methodType(void.class, String.class, int.class, List.class);
        MethodType isl_i = methodType(int.class, int.class, String.class, List.class);
        MethodType isli_i = methodType(int.class, int.class, String.class, List.class, int.class);
        MethodType sl_v = methodType(void.class, String.class, List.class);
        MethodType sli_v = methodType(void.class, String.class, List.class, int.class);
        MethodType l_it = methodType(Iterator.class, List.class);
        MethodType li_i = methodType(int.class, List.class, int.class);
        MethodType li_it = methodType(Iterator.class, List.class, int.class);
        MethodType il_it = methodType(Iterator.class, int.class, List.class);
        MethodType l_i = methodType(int.class, List.class);
        return new Object[][]{
                {l_it, null, sl_v, ""},
                {l_it, l_i, isl_i, ""},
                {l_it, null, sl_v, ""},
                {li_it, li_i, isli_i, ""},
                {null, null, sil_v, "inferred first loop argument must inherit from Iterable: int"},
                {il_it, null, sil_v, ""},
                {li_it, null, sli_v, ""},
                {sl_v, null, sl_v, "iteratedLoop first argument must have Iterator return type"},
                {li_it, l_it, sl_v,
                        String.format("iterator and init parameter lists must match: %s != %s", li_it, l_it)},
                {li_it, li_i, isl_i,
                        String.format("body types (regard parameter types after index 0, and result type) must match: %s != %s",
                                isl_i, isl_i.dropParameterTypes(0, 1).appendParameterTypes(int.class))}
        };
    }

    @Test(dataProvider = "iterateParameters")
    public static void testIterateParameters(MethodType it, MethodType in, MethodType bo, String msg) {
        boolean negative = !msg.isEmpty();
        MethodHandle iterator = it == null ? null : MethodHandles.empty(it);
        MethodHandle init = in == null ? null : MethodHandles.empty(in);
        boolean caught = false;
        MethodHandle loop = null;
        try {
            loop = MethodHandles.iteratedLoop(iterator, init, MethodHandles.empty(bo));
        } catch (Throwable t) {
            if (!negative) {
                throw t;
            }
            assertEqualsFIXME(msg, t.getMessage());
            caught = true;
        }
        if (negative) {
            assertTrue(caught);
        } else {
            MethodType lt = loop.type();
            if (it == null && in == null) {
                assertEquals(bo.dropParameterTypes(0, 1), lt);
            } else if (it == null) {
                if (in.parameterCount() == 0) {
                    assertEquals(bo.dropParameterTypes(0, in.returnType() == void.class ? 1 : 2), lt);
                } else {
                    assertEquals(methodType(bo.returnType(), in.parameterArray()), lt);
                }
            } else if (in == null) {
                assertEquals(methodType(bo.returnType(), it.parameterArray()), lt);
            } else if (it.parameterCount() > in.parameterCount()) {
                assertEquals(methodType(bo.returnType(), it.parameterArray()), lt);
            } else if (it.parameterCount() < in.parameterCount()) {
                assertEquals(methodType(bo.returnType(), in.parameterArray()), lt);
            } else {
                // both it, in present; with equal parameter list lengths
                assertEquals(it.parameterList(), lt.parameterList());
                assertEquals(in.parameterList(), lt.parameterList());
                assertEquals(bo.returnType(), lt.returnType());
            }
        }
    }

    @Test
    public static void testIteratorSubclass() throws Throwable {
        MethodHandle loop = MethodHandles.iteratedLoop(MethodHandles.empty(methodType(BogusIterator.class, List.class)),
                null, MethodHandles.empty(methodType(void.class, String.class, List.class)));
        assertEquals(methodType(void.class, List.class), loop.type());
    }

    static class BogusIterator implements Iterator {
        @Override
        public boolean hasNext() {
            return false;
        }
        @Override
        public Object next() {
            return null;
        }
    }

    static class Empty {

        static void f() { }

        static boolean pred() {
            return false;
        }

        static int c() {
            return 23;
        }

        static final Class<Empty> EMPTY = Empty.class;

        static final MethodType MT_f = methodType(void.class);
        static final MethodType MT_pred = methodType(boolean.class);
        static final MethodType MT_c = methodType(int.class);

        static final MethodHandle MH_f;
        static final MethodHandle MH_pred;
        static final MethodHandle MH_c;

        static {
            try {
                MH_f = LOOKUP.findStatic(EMPTY, "f", MT_f);
                MH_pred = LOOKUP.findStatic(EMPTY, "pred", MT_pred);
                MH_c = LOOKUP.findStatic(EMPTY, "c", MT_c);
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    static class Fac {

        static int zero(int k) {
            return 0;
        }

        static int one(int k) {
            return 1;
        }

        static boolean pred(int i, int acc, int k) {
            return i < k;
        }

        static int inc(int i, int acc, int k) {
            return i + 1;
        }

        static int mult(int i, int acc, int k) {
            return i * acc;
        }

        static void dot(int i, int acc, int k) {
            System.out.print('.');
        }

        static int fin(int i, int acc, int k) {
            return acc;
        }

        static final Class<Fac> FAC = Fac.class;

        static final MethodType MT_init = methodType(int.class, int.class);
        static final MethodType MT_fn = methodType(int.class, int.class, int.class, int.class);
        static final MethodType MT_dot = methodType(void.class, int.class, int.class, int.class);
        static final MethodType MT_pred = methodType(boolean.class, int.class, int.class, int.class);

        static final MethodHandle MH_zero;
        static final MethodHandle MH_one;
        static final MethodHandle MH_pred;
        static final MethodHandle MH_inc;
        static final MethodHandle MH_mult;
        static final MethodHandle MH_dot;
        static final MethodHandle MH_fin;

        static final MethodType MT_fac = methodType(int.class, int.class);

        static {
            try {
                MH_zero = LOOKUP.findStatic(FAC, "zero", MT_init);
                MH_one = LOOKUP.findStatic(FAC, "one", MT_init);
                MH_pred = LOOKUP.findStatic(FAC, "pred", MT_pred);
                MH_inc = LOOKUP.findStatic(FAC, "inc", MT_fn);
                MH_mult = LOOKUP.findStatic(FAC, "mult", MT_fn);
                MH_dot = LOOKUP.findStatic(FAC, "dot", MT_dot);
                MH_fin = LOOKUP.findStatic(FAC, "fin", MT_fn);
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }

    }

    static class Loop {

        static int inc(int i, int k) {
            return i + 1;
        }

        static boolean pred(int i, int k) {
            return i < k;
        }

        static int fin(int i, int k) {
            return k;
        }

        static final Class<Loop> LOOP = Loop.class;

        static final MethodType MT_inc = methodType(int.class, int.class, int.class);
        static final MethodType MT_pred = methodType(boolean.class, int.class, int.class);
        static final MethodType MT_fin = methodType(int.class, int.class, int.class);

        static final MethodHandle MH_inc;
        static final MethodHandle MH_pred;
        static final MethodHandle MH_fin;

        static final MethodType MT_loop = methodType(int.class, int.class);

        static {
            try {
                MH_inc = LOOKUP.findStatic(LOOP, "inc", MT_inc);
                MH_pred = LOOKUP.findStatic(LOOP, "pred", MT_pred);
                MH_fin = LOOKUP.findStatic(LOOP, "fin", MT_fin);
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }

    }

    static class LoopWithVirtuals {

        static int one(int k) {
            return 1;
        }

        int inc(int i, int acc, int k) {
            return i + 1;
        }

        int mult(int i, int acc, int k) {
            return i * acc;
        }

        boolean pred(int i, int acc, int k) {
            return i < k;
        }

        int fin(int i, int acc, int k) {
            return acc;
        }

        static final Class<LoopWithVirtuals> LOOP_WITH_VIRTUALS = LoopWithVirtuals.class;

        static final MethodType MT_one = methodType(int.class, int.class);
        static final MethodType MT_inc = methodType(int.class, int.class, int.class, int.class);
        static final MethodType MT_mult = methodType(int.class, int.class, int.class, int.class);
        static final MethodType MT_pred = methodType(boolean.class, int.class, int.class, int.class);
        static final MethodType MT_fin = methodType(int.class, int.class, int.class, int.class);

        static final MethodHandle MH_one;
        static final MethodHandle MH_inc;
        static final MethodHandle MH_mult;
        static final MethodHandle MH_pred;
        static final MethodHandle MH_fin;

        static final MethodType MT_loop = methodType(int.class, LOOP_WITH_VIRTUALS, int.class);

        static {
            try {
                MH_one = LOOKUP.findStatic(LOOP_WITH_VIRTUALS, "one", MT_one);
                MH_inc = LOOKUP.findVirtual(LOOP_WITH_VIRTUALS, "inc", MT_inc);
                MH_mult = LOOKUP.findVirtual(LOOP_WITH_VIRTUALS, "mult", MT_mult);
                MH_pred = LOOKUP.findVirtual(LOOP_WITH_VIRTUALS, "pred", MT_pred);
                MH_fin = LOOKUP.findVirtual(LOOP_WITH_VIRTUALS, "fin", MT_fin);
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        static MethodHandle permute(MethodHandle h) {
            // The handles representing virtual methods need to be rearranged to match the required order of arguments
            // (loop-local state comes first, then loop arguments). As the receiver comes first in the signature but is
            // a loop argument, it must be moved to the appropriate position in the signature.
            return MethodHandles.permuteArguments(h,
                    methodType(h.type().returnType(), int.class, int.class, LOOP_WITH_VIRTUALS, int.class), 2, 0, 1, 3);
        }

    }

    static class While {

        static int zero(int limit) {
            return 0;
        }

        static boolean pred(int i, int limit) {
            return i < limit;
        }

        static int step(int i, int limit) {
            return i + 1;
        }

        static String initString() {
            return "a";
        }

        static boolean predString(String s) {
            return s.length() != 1;
        }

        static String stepString(String s) {
            return s + "a";
        }

        static List<String> zipInitZip(Iterator<String> a, Iterator<String> b) {
            return new ArrayList<>();
        }

        static boolean zipPred(List<String> zip, Iterator<String> a, Iterator<String> b) {
            return a.hasNext() && b.hasNext();
        }

        static List<String> zipStep(List<String> zip, Iterator<String> a, Iterator<String> b) {
            zip.add(a.next());
            zip.add(b.next());
            return zip;
        }

        private int i = 0;

        void voidInit(int k) {
            // empty
        }

        void voidBody(int k) {
            ++i;
        }

        boolean voidPred(int k) {
            return i < k;
        }

        static final Class<While> WHILE = While.class;

        static final MethodType MT_zero = methodType(int.class, int.class);
        static final MethodType MT_pred = methodType(boolean.class, int.class, int.class);
        static final MethodType MT_fn = methodType(int.class, int.class, int.class);
        static final MethodType MT_initString = methodType(String.class);
        static final MethodType MT_predString = methodType(boolean.class, String.class);
        static final MethodType MT_stepString = methodType(String.class, String.class);
        static final MethodType MT_zipInitZip = methodType(List.class, Iterator.class, Iterator.class);
        static final MethodType MT_zipPred = methodType(boolean.class, List.class, Iterator.class, Iterator.class);
        static final MethodType MT_zipStep = methodType(List.class, List.class, Iterator.class, Iterator.class);
        static final MethodType MT_voidInit = methodType(void.class, int.class);
        static final MethodType MT_voidBody = methodType(void.class, int.class);
        static final MethodType MT_voidPred = methodType(boolean.class, int.class);

        static final MethodHandle MH_zero;
        static final MethodHandle MH_pred;
        static final MethodHandle MH_step;
        static final MethodHandle MH_initString;
        static final MethodHandle MH_predString;
        static final MethodHandle MH_stepString;
        static final MethodHandle MH_zipInitZip;
        static final MethodHandle MH_zipPred;
        static final MethodHandle MH_zipStep;
        static final MethodHandle MH_voidInit;
        static final MethodHandle MH_voidBody;
        static final MethodHandle MH_voidPred;

        static final MethodType MT_while = methodType(int.class, int.class);
        static final MethodType MT_string = methodType(String.class);
        static final MethodType MT_zip = methodType(List.class, Iterator.class, Iterator.class);
        static final MethodType MT_void = methodType(void.class, int.class);

        static {
            try {
                MH_zero = LOOKUP.findStatic(WHILE, "zero", MT_zero);
                MH_pred = LOOKUP.findStatic(WHILE, "pred", MT_pred);
                MH_step = LOOKUP.findStatic(WHILE, "step", MT_fn);
                MH_initString = LOOKUP.findStatic(WHILE, "initString", MT_initString);
                MH_predString = LOOKUP.findStatic(WHILE, "predString", MT_predString);
                MH_stepString = LOOKUP.findStatic(WHILE, "stepString", MT_stepString);
                MH_zipInitZip = LOOKUP.findStatic(WHILE, "zipInitZip", MT_zipInitZip);
                MH_zipPred = LOOKUP.findStatic(WHILE, "zipPred", MT_zipPred);
                MH_zipStep = LOOKUP.findStatic(WHILE, "zipStep", MT_zipStep);
                MH_voidInit = LOOKUP.findVirtual(WHILE, "voidInit", MT_voidInit);
                MH_voidBody = LOOKUP.findVirtual(WHILE, "voidBody", MT_voidBody);
                MH_voidPred = LOOKUP.findVirtual(WHILE, "voidPred", MT_voidPred);
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }

    }

    static class Counted {

        static String start(String arg) {
            return arg;
        }

        static String step(String v, int counter) {
            return "na " + v;
        }

        static void stepUpdateArray(int counter, int[] a) {
            ++a[0];
        }

        static void printHello(int counter) {
            System.out.print("hello");
        }

        static int addCounter(int x, int counter) {
            return x + counter;
        }

        static String stateBody(String s, int counter) {
            return "s" + s + counter;
        }

        static String append(String localState, int counter, String loopArg) {
            if (null == localState) {
                return loopArg + counter;
            }
            return localState + counter;
        }

        static final Class<Counted> COUNTED = Counted.class;

        static final MethodType MT_start = methodType(String.class, String.class);
        static final MethodType MT_step = methodType(String.class, String.class, int.class);
        static final MethodType MT_stepUpdateArray = methodType(void.class, int.class, int[].class);
        static final MethodType MT_printHello = methodType(void.class, int.class);
        static final MethodType MT_addCounter = methodType(int.class, int.class, int.class);
        static final MethodType MT_stateBody = methodType(String.class, String.class, int.class);
        static final MethodType MT_append = methodType(String.class, String.class, int.class, String.class);

        static final MethodHandle MH_13;
        static final MethodHandle MH_m5;
        static final MethodHandle MH_8;
        static final MethodHandle MH_start;
        static final MethodHandle MH_step;
        static final MethodHandle MH_stepUpdateArray;
        static final MethodHandle MH_printHello;
        static final MethodHandle MH_addCounter;
        static final MethodHandle MH_stateBody;
        static final MethodHandle MH_append;

        static final MethodType MT_counted = methodType(String.class, String.class);
        static final MethodType MT_arrayCounted = methodType(void.class, int[].class);
        static final MethodType MT_countedPrinting = methodType(void.class);
        static final MethodType MT_counterInit = methodType(int.class);
        static final MethodType MT_bodyDeterminesState = methodType(String.class);
        static final MethodType MT_iterationsDefineArgs = methodType(String.class, String.class);

        static {
            try {
                MH_13 = MethodHandles.constant(int.class, 13);
                MH_m5 = MethodHandles.constant(int.class, -5);
                MH_8 = MethodHandles.constant(int.class, 8);
                MH_start = LOOKUP.findStatic(COUNTED, "start", MT_start);
                MH_step = LOOKUP.findStatic(COUNTED, "step", MT_step);
                MH_stepUpdateArray = LOOKUP.findStatic(COUNTED, "stepUpdateArray", MT_stepUpdateArray);
                MH_printHello = LOOKUP.findStatic(COUNTED, "printHello", MT_printHello);
                MH_addCounter = LOOKUP.findStatic(COUNTED, "addCounter", MT_addCounter);
                MH_stateBody = LOOKUP.findStatic(COUNTED, "stateBody", MT_stateBody);
                MH_append = LOOKUP.findStatic(COUNTED, "append", MT_append);
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }

    }

    static class Iterate {

        static Iterator<Integer> sumIterator(Integer[] a) {
            return Arrays.asList(a).iterator();
        }

        static int sumInit(Integer[] a) {
            return 0;
        }

        static int sumStep(int s, int e, Integer[] a) {
            return s + e;
        }

        static List<String> reverseInit(List<String> l) {
            return new ArrayList<>();
        }

        static List<String> reverseStep(List<String> r, String e, List<String> l) {
            r.add(0, e);
            return r;
        }

        static int lengthInit(List<Double> l) {
            return 0;
        }

        static int lengthStep(int len, Object o, List<Double> l) {
            return len + 1;
        }

        static List<String> mapInit(List<String> l) {
            return new ArrayList<>();
        }

        static List<String> mapStep(List<String> r, String e, List<String> l) {
            r.add(e.toUpperCase());
            return r;
        }

        static void printStep(String s, List<String> l) {
            System.out.print(s);
        }

        static void voidInit(List<String> l) {
            // empty
        }

        static ListIterator<?> iteratorFromList(List<?> l) {
            return l.listIterator();
        }

        static Iterator<?> iteratorFromIterable(Iterable<?> l) {
            return l.iterator();
        }

        static final Class<Iterate> ITERATE = Iterate.class;

        static final MethodType MT_sumIterator = methodType(Iterator.class, Integer[].class);

        static final MethodType MT_sumInit = methodType(int.class, Integer[].class);
        static final MethodType MT_reverseInit = methodType(List.class, List.class);
        static final MethodType MT_lenghInit = methodType(int.class, List.class);
        static final MethodType MT_mapInit = methodType(List.class, List.class);

        static final MethodType MT_sumStep = methodType(int.class, int.class, int.class, Integer[].class);
        static final MethodType MT_reverseStep = methodType(List.class, List.class, String.class, List.class);
        static final MethodType MT_lengthStep = methodType(int.class, int.class, Object.class, List.class);
        static final MethodType MT_mapStep = methodType(List.class, List.class, String.class, List.class);
        static final MethodType MT_printStep = methodType(void.class, String.class, List.class);

        static final MethodType MT_voidInit = methodType(void.class, List.class);

        static final MethodType MT_iteratorFromList = methodType(ListIterator.class, List.class);
        static final MethodType MT_iteratorFromIterable = methodType(Iterator.class, Iterable.class);

        static final MethodHandle MH_sumIterator;
        static final MethodHandle MH_sumInit;
        static final MethodHandle MH_sumStep;
        static final MethodHandle MH_printStep;

        static final MethodHandle MH_reverseInit;
        static final MethodHandle MH_reverseStep;

        static final MethodHandle MH_lengthInit;
        static final MethodHandle MH_lengthStep;

        static final MethodHandle MH_mapInit;
        static final MethodHandle MH_mapStep;

        static final MethodHandle MH_voidInit;

        static final MethodHandle MH_iteratorFromList;
        static final MethodHandle MH_iteratorFromIterable;

        static final MethodType MT_sum = methodType(int.class, Integer[].class);
        static final MethodType MT_reverse = methodType(List.class, List.class);
        static final MethodType MT_length = methodType(int.class, List.class);
        static final MethodType MT_map = methodType(List.class, List.class);
        static final MethodType MT_print = methodType(void.class, List.class);

        static {
            try {
                MH_sumIterator = LOOKUP.findStatic(ITERATE, "sumIterator", MT_sumIterator);
                MH_sumInit = LOOKUP.findStatic(ITERATE, "sumInit", MT_sumInit);
                MH_sumStep = LOOKUP.findStatic(ITERATE, "sumStep", MT_sumStep);
                MH_reverseInit = LOOKUP.findStatic(ITERATE, "reverseInit", MT_reverseInit);
                MH_reverseStep = LOOKUP.findStatic(ITERATE, "reverseStep", MT_reverseStep);
                MH_lengthInit = LOOKUP.findStatic(ITERATE, "lengthInit", MT_lenghInit);
                MH_lengthStep = LOOKUP.findStatic(ITERATE, "lengthStep", MT_lengthStep);
                MH_mapInit = LOOKUP.findStatic(ITERATE, "mapInit", MT_mapInit);
                MH_mapStep = LOOKUP.findStatic(ITERATE, "mapStep", MT_mapStep);
                MH_printStep = LOOKUP.findStatic(ITERATE, "printStep", MT_printStep);
                MH_voidInit = LOOKUP.findStatic(ITERATE, "voidInit", MT_voidInit);
                MH_iteratorFromList = LOOKUP.findStatic(ITERATE, "iteratorFromList", MT_iteratorFromList);
                MH_iteratorFromIterable = LOOKUP.findStatic(ITERATE, "iteratorFromIterable", MT_iteratorFromIterable);
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }

    }

}
