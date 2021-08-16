/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.Serializable;
import java.lang.invoke.*;
import java.util.concurrent.Callable;

/**
 * @test
 * @summary Test input invariants for StringConcatFactory
 * @bug 8246152 8247681
 *
 * @compile StringConcatFactoryInvariants.java
 *
 * @run main/othervm -Xverify:all StringConcatFactoryInvariants
 *
*/
public class StringConcatFactoryInvariants {

    private static final char TAG_ARG   = '\u0001';
    private static final char TAG_CONST = '\u0002';

    public static void main(String[] args) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        String methodName = "foo";
        MethodType mt = MethodType.methodType(String.class, String.class, int.class);
        String recipe = "" + TAG_ARG + TAG_ARG + TAG_CONST;
        Object[][] constants = new Object[][] {
                new String[] { "" },
                new String[] { "bar" },
                new Integer[] { 1 },
                new Short[] { 2 },
                new Long[] { 3L },
                new Boolean[] { true },
                new Character[] { 'a' },
                new Byte[] { -128 },
                new Class[] { String.class },
                new MethodHandle[] { MethodHandles.constant(String.class, "constant") },
                new MethodType[] { MethodType.methodType(String.class) }
        };
        // The string representation that should end up if the corresponding
        // Object[] in constants is used as an argument to makeConcatWithConstants
        String[] constantString = new String[] {
                "",
                "bar",
                "1",
                "2",
                "3",
                "true",
                "a",
                "-128",
                "class java.lang.String",
                "MethodHandle()String",
                "()String"
        };


        final int LIMIT = 200;

        // Simple factory: check for dynamic arguments overflow
        Class<?>[] underThreshold = new Class<?>[LIMIT - 1];
        Class<?>[] threshold      = new Class<?>[LIMIT];
        Class<?>[] overThreshold  = new Class<?>[LIMIT + 1];

        StringBuilder sbUnderThreshold = new StringBuilder();
        sbUnderThreshold.append(TAG_CONST);
        for (int c = 0; c < LIMIT - 1; c++) {
            underThreshold[c] = int.class;
            threshold[c] = int.class;
            overThreshold[c] = int.class;
            sbUnderThreshold.append(TAG_ARG);
        }
        threshold[LIMIT - 1] = int.class;
        overThreshold[LIMIT - 1] = int.class;
        overThreshold[LIMIT] = int.class;

        String recipeEmpty = "";
        String recipeUnderThreshold = sbUnderThreshold.toString();
        String recipeThreshold = sbUnderThreshold.append(TAG_ARG).toString();
        String recipeOverThreshold = sbUnderThreshold.append(TAG_ARG).toString();

        MethodType mtEmpty = MethodType.methodType(String.class);
        MethodType mtUnderThreshold = MethodType.methodType(String.class, underThreshold);
        MethodType mtThreshold = MethodType.methodType(String.class, threshold);
        MethodType mtOverThreshold = MethodType.methodType(String.class, overThreshold);


        // Check the basic functionality is working
        {
            CallSite cs = StringConcatFactory.makeConcat(lookup, methodName, mt);
            test("foo42", (String) cs.getTarget().invokeExact("foo", 42));
        }

        {
            for (int i = 0; i < constants.length; i++) {
                CallSite cs = StringConcatFactory.makeConcatWithConstants(lookup, methodName, mt, recipe, constants[i]);
                test("foo42".concat(constantString[i]), (String) cs.getTarget().invokeExact("foo", 42));
            }
        }

        // Check unary expressions with pre- and postfix constants
        {
            String constArgRecipe = "" + TAG_CONST + TAG_ARG;
            String argConstRecipe = "" + TAG_ARG + TAG_CONST;
            MethodType unaryMt = MethodType.methodType(String.class, String.class);

            for (int i = 0; i < constants.length; i++) {
                CallSite prefixCS = StringConcatFactory.makeConcatWithConstants(lookup, methodName, unaryMt, constArgRecipe, constants[i]);
                test(constantString[i].concat("foo"), (String) prefixCS.getTarget().invokeExact("foo"));

                CallSite postfixCS = StringConcatFactory.makeConcatWithConstants(lookup, methodName, unaryMt, argConstRecipe, constants[i]);
                test("foo".concat(constantString[i]), (String) postfixCS.getTarget().invokeExact("foo"));
            }
        }

        // Simple factory, check for nulls:
        failNPE("Lookup is null",
                () -> StringConcatFactory.makeConcat(null, methodName, mt));

        failNPE("Method name is null",
                () -> StringConcatFactory.makeConcat(lookup, null, mt));

        failNPE("MethodType is null",
                () -> StringConcatFactory.makeConcat(lookup, methodName, null));

        // Advanced factory, check for nulls:
        for (int i = 0; i < constants.length; i++) {
            final Object[] consts = constants[i];

            failNPE("Lookup is null",
                    () -> StringConcatFactory.makeConcatWithConstants(null, methodName, mt, recipe, consts));

            failNPE("Method name is null",
                    () -> StringConcatFactory.makeConcatWithConstants(lookup, null, mt, recipe, consts));

            failNPE("MethodType is null",
                    () -> StringConcatFactory.makeConcatWithConstants(lookup, methodName, null, recipe, consts));

            failNPE("Recipe is null",
                    () -> StringConcatFactory.makeConcatWithConstants(lookup, methodName, mt, null, consts));
        }

        failNPE("Constants vararg is null",
                () -> StringConcatFactory.makeConcatWithConstants(lookup, methodName, mt, recipe, (Object[]) null));

        failNPE("Constant argument is null",
                () -> StringConcatFactory.makeConcatWithConstants(lookup, methodName, mt, recipe, new Object[] { null }));

        // Simple factory, check for return type
        fail("Return type: void",
                () -> StringConcatFactory.makeConcat(lookup, methodName, MethodType.methodType(void.class, String.class, int.class)));

        fail("Return type: int",
                () -> StringConcatFactory.makeConcat(lookup, methodName, MethodType.methodType(int.class, String.class, int.class)));

        fail("Return type: StringBuilder",
                () -> StringConcatFactory.makeConcat(lookup, methodName, MethodType.methodType(StringBuilder.class, String.class, int.class)));

        ok("Return type: Object",
                () -> StringConcatFactory.makeConcat(lookup, methodName, MethodType.methodType(Object.class, String.class, int.class)));

        ok("Return type: CharSequence",
                () -> StringConcatFactory.makeConcat(lookup, methodName, MethodType.methodType(CharSequence.class, String.class, int.class)));

        ok("Return type: Serializable",
                () -> StringConcatFactory.makeConcat(lookup, methodName, MethodType.methodType(Serializable.class, String.class, int.class)));

        // Advanced factory, check for return types
        for (int i = 0; i < constants.length; i++) {
            final Object[] consts = constants[i];
            fail("Return type: void",
                    () -> StringConcatFactory.makeConcatWithConstants(lookup, methodName, MethodType.methodType(void.class, String.class, int.class), recipe, consts));

            fail("Return type: int",
                    () -> StringConcatFactory.makeConcatWithConstants(lookup, methodName, MethodType.methodType(int.class, String.class, int.class), recipe, consts));

            fail("Return type: StringBuilder",
                    () -> StringConcatFactory.makeConcatWithConstants(lookup, methodName, MethodType.methodType(StringBuilder.class, String.class, int.class), recipe, consts));

            ok("Return type: Object",
                    () -> StringConcatFactory.makeConcatWithConstants(lookup, methodName, MethodType.methodType(Object.class, String.class, int.class), recipe, consts));

            ok("Return type: CharSequence",
                    () -> StringConcatFactory.makeConcatWithConstants(lookup, methodName, MethodType.methodType(CharSequence.class, String.class, int.class), recipe, consts));

            ok("Return type: Serializable",
                    () -> StringConcatFactory.makeConcatWithConstants(lookup, methodName, MethodType.methodType(Serializable.class, String.class, int.class), recipe, consts));
        }

        // Simple factory: check for dynamic arguments overflow
        ok("Dynamic arguments is under limit",
                () -> StringConcatFactory.makeConcat(lookup, methodName, mtUnderThreshold));

        ok("Dynamic arguments is at the limit",
                () -> StringConcatFactory.makeConcat(lookup, methodName, mtThreshold));

        fail("Dynamic arguments is over the limit",
                () -> StringConcatFactory.makeConcat(lookup, methodName, mtOverThreshold));

        // Advanced factory: check for dynamic arguments overflow
        ok("Dynamic arguments is under limit",
                () -> StringConcatFactory.makeConcatWithConstants(lookup, methodName, mtUnderThreshold, recipeUnderThreshold, constants[0]));

        ok("Dynamic arguments is at the limit",
                () -> StringConcatFactory.makeConcatWithConstants(lookup, methodName, mtThreshold, recipeThreshold, constants[0]));

        fail("Dynamic arguments is over the limit",
                () -> StringConcatFactory.makeConcatWithConstants(lookup, methodName, mtOverThreshold, recipeOverThreshold, constants[0]));

        // Advanced factory: check for mismatched recipe and Constants
        ok("Static arguments and recipe match",
                () -> StringConcatFactory.makeConcatWithConstants(lookup, methodName, mtThreshold, recipeThreshold, "bar"));

        fail("Static arguments and recipe mismatch: too few",
                () -> StringConcatFactory.makeConcatWithConstants(lookup, methodName, mtThreshold, recipeThreshold));

        fail("Static arguments and recipe mismatch: too many",
                () -> StringConcatFactory.makeConcatWithConstants(lookup, methodName, mtThreshold, recipeThreshold, "bar", "baz"));

        failNPE("Static arguments and recipe mismatch, too many, overflowing constant is null",
                () -> StringConcatFactory.makeConcatWithConstants(lookup, methodName, mtThreshold, recipeThreshold, "bar", null));

        // Advanced factory: check for mismatched recipe and dynamic arguments
        fail("Dynamic arguments and recipe mismatch",
                () -> StringConcatFactory.makeConcatWithConstants(lookup, methodName, mtThreshold, recipeUnderThreshold, constants[0]));

        ok("Dynamic arguments and recipe match",
                () -> StringConcatFactory.makeConcatWithConstants(lookup, methodName, mtThreshold, recipeThreshold, constants[0]));

        fail("Dynamic arguments and recipe mismatch",
                () -> StringConcatFactory.makeConcatWithConstants(lookup, methodName, mtThreshold, recipeOverThreshold, constants[0]));

        // Test passing array as constant
        {
            Object[] arg = {"boo", "bar"};

            CallSite cs1 = StringConcatFactory.makeConcatWithConstants(lookup, methodName, MethodType.methodType(String.class, int.class), "" + TAG_ARG + TAG_CONST + TAG_CONST, arg);
            test("42boobar", (String) cs1.getTarget().invokeExact(42));
        }

        // Test passing null constant
        ok("Can pass regular constants",
                () -> StringConcatFactory.makeConcatWithConstants(lookup, methodName, MethodType.methodType(String.class, int.class), "" + TAG_ARG + TAG_CONST, "foo"));

        failNPE("Cannot pass null constants",
                () -> StringConcatFactory.makeConcatWithConstants(lookup, methodName, MethodType.methodType(String.class, int.class), "" + TAG_ARG + TAG_CONST, new Object[]{null}));

        // Simple factory: test empty arguments
        ok("Ok to pass empty arguments",
                () -> StringConcatFactory.makeConcat(lookup, methodName, mtEmpty));

        // Advanced factory: test empty arguments
        ok("Ok to pass empty arguments",
                () -> StringConcatFactory.makeConcatWithConstants(lookup, methodName, mtEmpty, recipeEmpty));

        // Simple factory: public Lookup is rejected
        fail("Passing public Lookup",
                () -> StringConcatFactory.makeConcat(MethodHandles.publicLookup(), methodName, mtEmpty));

        // Advanced factory: public Lookup is rejected
        fail("Passing public Lookup",
                () -> StringConcatFactory.makeConcatWithConstants(MethodHandles.publicLookup(), methodName, mtEmpty, recipeEmpty));

        // Zero inputs
        {
            MethodType zero = MethodType.methodType(String.class);
            CallSite cs = StringConcatFactory.makeConcat(lookup, methodName, zero);
            test("", (String) cs.getTarget().invokeExact());

            cs = StringConcatFactory.makeConcatWithConstants(lookup, methodName, zero, "");
            test("", (String) cs.getTarget().invokeExact());
        }

        // One input
        {
            MethodType zero = MethodType.methodType(String.class);
            MethodType one = MethodType.methodType(String.class, String.class);
            CallSite cs = StringConcatFactory.makeConcat(lookup, methodName, one);
            test("A", (String) cs.getTarget().invokeExact("A"));

            cs = StringConcatFactory.makeConcatWithConstants(lookup, methodName, one, "\1");
            test("A", (String) cs.getTarget().invokeExact("A"));

            cs = StringConcatFactory.makeConcatWithConstants(lookup, methodName, zero, "\2", "A");
            test("A", (String) cs.getTarget().invokeExact());
        }
    }

    public static void ok(String msg, Callable runnable) {
        try {
            runnable.call();
        } catch (Throwable e) {
            e.printStackTrace();
            throw new IllegalStateException(msg + ", should have passed", e);
        }
    }

    public static void fail(String msg, Callable runnable) {
        boolean expected = false;
        try {
            runnable.call();
        } catch (StringConcatException e) {
            expected = true;
        } catch (Throwable e) {
            e.printStackTrace();
        }

        if (!expected) {
            throw new IllegalStateException(msg + ", should have failed with StringConcatException");
        }
    }


    public static void failNPE(String msg, Callable runnable) {
        boolean expected = false;
        try {
            runnable.call();
        } catch (NullPointerException e) {
            expected = true;
        } catch (Throwable e) {
            e.printStackTrace();
        }

        if (!expected) {
            throw new IllegalStateException(msg + ", should have failed with NullPointerException");
        }
    }

    public static void test(String expected, String actual) {
       // Fingers crossed: String concat should work.
       if (!expected.equals(actual)) {
           StringBuilder sb = new StringBuilder();
           sb.append("Expected = ");
           sb.append(expected);
           sb.append(", actual = ");
           sb.append(actual);
           throw new IllegalStateException(sb.toString());
       }
    }

}
