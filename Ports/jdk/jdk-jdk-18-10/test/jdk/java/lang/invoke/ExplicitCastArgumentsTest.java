/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

import sun.invoke.util.Wrapper;
import test.java.lang.invoke.lib.Helper;

import java.io.File;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/*
 * @test
 * @bug 8060483 8066746
 * @key randomness
 * @library /test/lib /java/lang/invoke/common
 * @modules java.base/sun.invoke.util
 * @summary unit tests for MethodHandles.explicitCastArguments()
 * @run main ExplicitCastArgumentsTest
 */

/**
 * Tests for MethodHandles.explicitCastArguments().
 */
public class ExplicitCastArgumentsTest {

    private static final boolean VERBOSE = Helper.IS_VERBOSE;
    private static final Class<?> THIS_CLASS = ExplicitCastArgumentsTest.class;
    private static final Random RNG = Helper.RNG;
    private static final Map<Wrapper, Object> RANDOM_VALUES = new HashMap<>(9);

    static {
        RANDOM_VALUES.put(Wrapper.BOOLEAN, RNG.nextBoolean());
        RANDOM_VALUES.put(Wrapper.BYTE, (byte) RNG.nextInt());
        RANDOM_VALUES.put(Wrapper.SHORT, (short) RNG.nextInt());
        RANDOM_VALUES.put(Wrapper.CHAR, (char) RNG.nextInt());
        RANDOM_VALUES.put(Wrapper.INT, RNG.nextInt());
        RANDOM_VALUES.put(Wrapper.LONG, RNG.nextLong());
        RANDOM_VALUES.put(Wrapper.FLOAT, RNG.nextFloat());
        RANDOM_VALUES.put(Wrapper.DOUBLE, RNG.nextDouble());
        RANDOM_VALUES.put(Wrapper.OBJECT, new Object());
    }

    public static void main(String[] args) throws Throwable {
        testVarargsCollector();
        testNullRef2Prim();
        testRef2Prim();
        testPrim2Ref();
        testPrim2Prim();
        testNonBCPRef2NonBCPRef();
        testBCPRef2BCPRef();
        testNonBCPRef2BCPRef();
        testReturnAny2Void();
        testReturnVoid2Any();
        testMultipleArgs();
        System.out.println("TEST PASSED");
    }

    /**
     * Dummy method used in {@link #testVarargsCollector} test to form a method
     * handle.
     *
     * @param args - any args
     * @return - returns args
     */
    public static String[] f(String... args) {
        return args;
    }

    /**
     * Tests that MHs.explicitCastArguments does incorrect type checks for
     * VarargsCollector. Bug 8066746.
     *
     * @throws java.lang.Throwable
     */
    public static void testVarargsCollector() throws Throwable {
        MethodType mt = MethodType.methodType(String[].class, String[].class);
        MethodHandle mh = MethodHandles.publicLookup()
                .findStatic(THIS_CLASS, "f", mt);
        mh = MethodHandles.explicitCastArguments(mh,
                MethodType.methodType(Object.class, Object.class));
        mh.invokeWithArguments((Object) (new String[]{"str1", "str2"}));
    }

    /**
     * Tests that null wrapper reference is successfully converted to primitive
     * types. Converted result should be zero for a primitive. Bug 8060483.
     */
    public static void testNullRef2Prim() {
        for (Wrapper from : Wrapper.values()) {
            for (Wrapper to : Wrapper.values()) {
                if (from == Wrapper.VOID || to == Wrapper.VOID) {
                    continue;
                }
                // MHs.eCA javadoc:
                //    If T0 is a reference and T1 a primitive, and if the reference
                //    is null at runtime, a zero value is introduced.
                for (TestConversionMode mode : TestConversionMode.values()) {
                    testConversion(mode, from.wrapperType(),
                            to.primitiveType(), null, to.zero(), false, null);
                }
            }
        }
    }

    /**
     * Tests that non-null wrapper reference is successfully converted to
     * primitive types.
     */
    public static void testRef2Prim() {
        for (Wrapper from : Wrapper.values()) {
            for (Wrapper to : Wrapper.values()) {
                if (from == Wrapper.VOID || to == Wrapper.VOID
                        || to == Wrapper.OBJECT) {
                    continue;
                }
                Object value = RANDOM_VALUES.get(from);
                for (TestConversionMode mode : TestConversionMode.values()) {
                    if (from != Wrapper.OBJECT) {
                        Object convValue = to.wrap(value);
                        testConversion(mode, from.wrapperType(),
                                to.primitiveType(), value, convValue, false, null);
                    } else {
                        testConversion(mode, from.wrapperType(),
                                to.primitiveType(), value, null,
                                true, ClassCastException.class);
                    }
                }
            }
        }
    }

    /**
     * Tests that primitive is successfully converted to wrapper reference
     * types, to the Number type (if possible) and to the Object type.
     */
    public static void testPrim2Ref() {
        for (Wrapper from : Wrapper.values()) {
            for (Wrapper to : Wrapper.values()) {
                if (from == Wrapper.VOID || from == Wrapper.OBJECT
                        || to == Wrapper.VOID || to == Wrapper.OBJECT) {
                    continue;
                }
                Object value = RANDOM_VALUES.get(from);
                for (TestConversionMode mode : TestConversionMode.values()) {
                    if (from == to) {
                        testConversion(mode, from.primitiveType(),
                                to.wrapperType(), value, value, false, null);
                    } else {
                        testConversion(mode, from.primitiveType(),
                                to.wrapperType(), value, null, true, ClassCastException.class);
                    }
                    if (from != Wrapper.BOOLEAN && from != Wrapper.CHAR) {
                        testConversion(mode, from.primitiveType(),
                                Number.class, value, value, false, null);
                    } else {
                        testConversion(mode, from.primitiveType(),
                                Number.class, value, null,
                                true, ClassCastException.class);
                    }
                    testConversion(mode, from.primitiveType(),
                            Object.class, value, value, false, null);
                }
            }
        }
    }

    /**
     * Tests that primitive is successfully converted to other primitive type.
     */
    public static void testPrim2Prim() {
        for (Wrapper from : Wrapper.values()) {
            for (Wrapper to : Wrapper.values()) {
                if (from == Wrapper.VOID || to == Wrapper.VOID
                        || from == Wrapper.OBJECT || to == Wrapper.OBJECT) {
                    continue;
                }
                Object value = RANDOM_VALUES.get(from);
                Object convValue = to.wrap(value);
                for (TestConversionMode mode : TestConversionMode.values()) {
                    testConversion(mode, from.primitiveType(),
                            to.primitiveType(), value, convValue, false, null);
                }
            }
        }
    }

    /**
     * Dummy interface for {@link #testNonBCPRef2Ref} test.
     */
    public static interface TestInterface {}

    /**
     * Dummy class for {@link #testNonBCPRef2Ref} test.
     */
    public static class TestSuperClass implements TestInterface {}

    /**
     * Dummy class for {@link #testNonBCPRef2Ref} test.
     */
    public static class TestSubClass1 extends TestSuperClass {}

    /**
     * Dummy class for {@link #testNonBCPRef2Ref} test.
     */
    public static class TestSubClass2 extends TestSuperClass {}

    /**
     * Tests non-bootclasspath reference to reference conversions.
     *
     * @throws java.lang.Throwable
     */
    public static void testNonBCPRef2NonBCPRef() throws Throwable {
        Class testInterface = TestInterface.class;
        Class testSuperClass = TestSuperClass.class;
        Class testSubClass1 = TestSubClass1.class;
        Class testSubClass2 = TestSubClass2.class;
        Object testSuperObj = new TestSuperClass();
        Object testObj01 = new TestSubClass1();
        Object testObj02 = new TestSubClass2();
        Class[] parents = {testInterface, testSuperClass};
        Class[] children = {testSubClass1, testSubClass2};
        Object[] childInst = {testObj01, testObj02};
        for (TestConversionMode mode : TestConversionMode.values()) {
            for (Class parent : parents) {
                for (int j = 0; j < children.length; j++) {
                    // Child type to parent type non-null conversion, shoud succeed
                    testConversion(mode, children[j], parent, childInst[j],
                            childInst[j], false, null);
                    // Child type to parent type null conversion, shoud succeed
                    testConversion(mode, children[j], parent, null,
                            null, false, null);
                    // Parent type to child type non-null conversion with parent
                    // type instance, should fail
                    testConversion(mode, parent, children[j], testSuperObj,
                            null, true, ClassCastException.class);
                    // Parent type to child type non-null conversion with child
                    // type instance, should succeed
                    testConversion(mode, parent, children[j], childInst[j],
                            childInst[j], false, null);
                    // Parent type to child type null conversion, should succeed
                    testConversion(mode, parent, children[j], null,
                            null, false, null);
                }
                // Parent type to child type non-null conversion with sibling
                // type instance, should fail
                testConversion(mode, parent, testSubClass1, testObj02,
                        null, true, ClassCastException.class);
            }
            // Sibling type non-null conversion, should fail
            testConversion(mode, testSubClass1,
                    testSubClass2, testObj01, null, true,
                    ClassCastException.class);
            // Sibling type null conversion, should succeed
            testConversion(mode, testSubClass1,
                    testSubClass2, null, null, false, null);
        }
    }

    /**
     * Dummy interface for {@link #testNonBCPRef2BCPRef} test.
     */
    public static interface TestSerializableInterface extends Serializable {}

    /**
     * Dummy class for {@link #testNonBCPRef2BCPRef} test.
     */
    public static class TestSerializableClass
            implements TestSerializableInterface {}

    /**
     * Dummy class for {@link #testNonBCPRef2BCPRef} test.
     */
    public static class TestFileChildClass extends File
            implements TestSerializableInterface {
        public TestFileChildClass(String pathname) {
            super(pathname);
        }
    }

    /**
     * Tests non-bootclasspath reference to bootclasspath reference conversions
     * and vice-versa.
     *
     * @throws java.lang.Throwable
     */
    public static void testNonBCPRef2BCPRef() throws Throwable {
        Class bcpInterface = Serializable.class;
        Class bcpSuperClass = File.class;
        Class nonBcpInterface = TestSerializableInterface.class;
        Class nonBcpSuperSiblingClass = TestSerializableClass.class;
        Class nonBcpSubClass = TestFileChildClass.class;
        Object bcpSuperObj = new File(".");
        Object testSuperSiblingObj = new TestSerializableClass();
        Object testSubObj = new TestFileChildClass(".");
        Class[] parents = {bcpInterface, bcpSuperClass};
        for (TestConversionMode mode : TestConversionMode.values()) {
            for (Class parent : parents) {
                // Child type to parent type non-null conversion, shoud succeed
                testConversion(mode, nonBcpSubClass, parent, testSubObj,
                        testSubObj, false, null);
                // Child type to parent type null conversion, shoud succeed
                testConversion(mode, nonBcpSubClass, parent, null, null,
                        false, null);
                // Parent type to child type non-null conversion with parent
                // type instance, should fail
                testConversion(mode, parent, nonBcpSubClass, bcpSuperObj, null,
                        true, ClassCastException.class);
                // Parent type to child type non-null conversion with child
                // type instance, should succeed
                testConversion(mode, parent, nonBcpSubClass, testSubObj,
                        testSubObj, false, null);
                // Parent type to child type null conversion, should succeed
                testConversion(mode, parent, nonBcpSubClass, null, null,
                        false, null);
            }
            // Parent type to child type non-null conversion with
            // super sibling type instance, should fail
            testConversion(mode, bcpInterface, nonBcpSubClass,
                    testSuperSiblingObj, null, true, ClassCastException.class);
            Class[] siblings = {nonBcpSubClass, bcpSuperClass};
            for (Class sibling : siblings) {
                // Non-bcp class to bcp/non-bcp sibling class non-null
                // conversion with nonBcpSuperSiblingClass instance, should fail
                testConversion(mode, nonBcpSuperSiblingClass, sibling,
                        testSuperSiblingObj, null, true, ClassCastException.class);
                // Non-bcp class to bcp/non-bcp sibling class null conversion,
                // should succeed
                testConversion(mode, nonBcpSuperSiblingClass, sibling,
                        null, null, false, null);
                // Non-bcp interface to bcp/non-bcp sibling class non-null
                // conversion with nonBcpSubClass instance, should succeed
                testConversion(mode, nonBcpInterface, sibling, testSubObj,
                        testSubObj, false, null);
                // Non-bcp interface to bcp/non-bcp sibling class
                // null conversion, should succeed
                testConversion(mode, nonBcpInterface, sibling, null, null,
                        false, null);
                // Non-bcp interface to bcp/non-bcp sibling class non-null
                // conversion with nonBcpSuperSiblingClass instance, should fail
                testConversion(mode, nonBcpInterface, sibling,
                        testSuperSiblingObj, testSubObj,
                        true, ClassCastException.class);
            }
        }
    }

    /**
     * Tests bootclasspath reference to reference conversions.
     */
    public static void testBCPRef2BCPRef() {
        Class bcpInterface = CharSequence.class;
        Class bcpSubClass1 = String.class;
        Class bcpSubClass2 = StringBuffer.class;
        Object testObj01 = new String("test");
        Object testObj02 = new StringBuffer("test");
        Class[] children = {bcpSubClass1, bcpSubClass2};
        Object[] childInst = {testObj01, testObj02};
        for (TestConversionMode mode : TestConversionMode.values()) {
            for (int i = 0; i < children.length; i++) {
                // Child type to parent type non-null conversion, shoud succeed
                testConversion(mode, children[i], bcpInterface, childInst[i],
                        childInst[i], false, null);
                // Child type to parent type null conversion, shoud succeed
                testConversion(mode, children[i], bcpInterface, null,
                        null, false, null);
                // Parent type to child type non-null conversion with child
                // type instance, should succeed
                testConversion(mode, bcpInterface,
                        children[i], childInst[i], childInst[i], false, null);
                // Parent type to child type null conversion, should succeed
                testConversion(mode, bcpInterface,
                        children[i], null, null, false, null);
            }
            // Sibling type non-null conversion, should fail
            testConversion(mode, bcpSubClass1,
                    bcpSubClass2, testObj01, null, true,
                    ClassCastException.class);
            // Sibling type null conversion, should succeed
            testConversion(mode, bcpSubClass1,
                    bcpSubClass2, null, null, false, null);
            // Parent type to child type non-null conversion with sibling
            // type instance, should fail
            testConversion(mode, bcpInterface, bcpSubClass1, testObj02,
                    null, true, ClassCastException.class);
        }
    }

    /**
     * Dummy method used in {@link #testReturnAny2Void} and
     * {@link #testReturnVoid2Any} tests to form a method handle.
     */
    public static void retVoid() {}

    /**
     * Tests that non-null any return is successfully converted to non-type
     * void.
     */
    public static void testReturnAny2Void() {
        for (Wrapper from : Wrapper.values()) {
            testConversion(TestConversionMode.RETURN_VALUE, from.wrapperType(),
                    void.class, RANDOM_VALUES.get(from),
                    null, false, null);
            testConversion(TestConversionMode.RETURN_VALUE, from.primitiveType(),
                    void.class, RANDOM_VALUES.get(from),
                    null, false, null);
        }
    }

    /**
     * Tests that void return is successfully converted to primitive and
     * reference. Result should be zero for primitives and null for references.
     */
    public static void testReturnVoid2Any() {
        for (Wrapper to : Wrapper.values()) {
            testConversion(TestConversionMode.RETURN_VALUE, void.class,
                    to.primitiveType(), null,
                    to.zero(), false, null);
            testConversion(TestConversionMode.RETURN_VALUE, void.class,
                    to.wrapperType(), null,
                    null, false, null);
        }
    }

    private static void checkForWrongMethodTypeException(MethodHandle mh, MethodType mt) {
        try {
            MethodHandles.explicitCastArguments(mh, mt);
            throw new AssertionError("Expected WrongMethodTypeException is not thrown");
        } catch (WrongMethodTypeException wmte) {
            if (VERBOSE) {
                System.out.printf("Expected exception %s: %s\n",
                        wmte.getClass(), wmte.getMessage());
            }
        }
    }

    /**
     * Tests that MHs.eCA method works correctly with MHs with multiple arguments.
     * @throws Throwable
     */
    public static void testMultipleArgs() throws Throwable {
        int arity = 1 + RNG.nextInt(Helper.MAX_ARITY / 2 - 2);
        int arityMinus = RNG.nextInt(arity);
        int arityPlus = arity + RNG.nextInt(Helper.MAX_ARITY / 2 - arity) + 1;
        MethodType mType = Helper.randomMethodTypeGenerator(arity);
        MethodType mTypeNew = Helper.randomMethodTypeGenerator(arity);
        MethodType mTypeNewMinus = Helper.randomMethodTypeGenerator(arityMinus);
        MethodType mTypeNewPlus = Helper.randomMethodTypeGenerator(arityPlus);
        Class<?> rType = mType.returnType();
        MethodHandle original;
        if (rType.equals(void.class)) {
            MethodType mt = MethodType.methodType(void.class);
            original = MethodHandles.publicLookup()
                    .findStatic(THIS_CLASS, "retVoid", mt);
        } else {
            Object rValue = Helper.castToWrapper(1, rType);
            original = MethodHandles.constant(rType, rValue);
        }
        original = Helper.addTrailingArgs(original, arity, mType.parameterList());
        MethodHandle target = MethodHandles
                    .explicitCastArguments(original, mTypeNew);
        Object[] parList = Helper.randomArgs(mTypeNew.parameterList());
        for (int i = 0; i < parList.length; i++) {
            if (parList[i] instanceof String) {
                parList[i] = null; //getting rid of Stings produced by randomArgs
            }
        }
        target.invokeWithArguments(parList);
        checkForWrongMethodTypeException(original, mTypeNewMinus);
        checkForWrongMethodTypeException(original, mTypeNewPlus);
    }

    /**
     * Enumeration of test conversion modes.
     */
    public enum TestConversionMode {
        RETURN_VALUE,
        ARGUMENT;
    }

    /**
     * Tests type and value conversion. Comparing with the given expected result.
     *
     * @param mode - test conversion mode. See {@link #TestConversionMode}.
     * @param from - source type.
     * @param to - destination type.
     * @param param - value to be converted.
     * @param expectedResult - expected value after conversion.
     * @param failureExpected - true if conversion failure expected.
     * @param expectedException - expected exception class if
     * {@code failureExpected} is true.
     */
    public static void testConversion(TestConversionMode mode,
            Class<?> from, Class<?> to, Object param,
            Object expectedResult, boolean failureExpected,
            Class<? extends Throwable> expectedException) {
        if (VERBOSE) {
            System.out.printf("Testing return value conversion: "
                    + "%-10s => %-10s: %5s: ", from.getSimpleName(),
                    to.getSimpleName(), param);
        }
        MethodHandle original = null;
        MethodType newType = null;
        switch (mode) {
            case RETURN_VALUE:
                if (from.equals(void.class)) {
                    MethodType mt = MethodType.methodType(void.class);
                    try {
                        original = MethodHandles.publicLookup()
                                .findStatic(THIS_CLASS, "retVoid", mt);
                    } catch (NoSuchMethodException | IllegalAccessException ex) {
                        throw new Error("Unexpected issue", ex);
                    }
                } else {
                    original = MethodHandles.constant(from, param);
                }
                newType = original.type().changeReturnType(to);
                break;
            case ARGUMENT:
                if (from.equals(void.class) || to.equals(void.class)) {
                    throw new Error("Test issue: argument conversion does not"
                            + " work with non-type void");
                }
                original = MethodHandles.identity(to);
                newType = original.type().changeParameterType(0, from);
                break;
            default:
                String msg = String.format("Test issue: unknown test"
                        + " convertion mode %s.", mode.name());
                throw new Error(msg);
        }
        try {
            MethodHandle target = MethodHandles
                    .explicitCastArguments(original, newType);
            Object result;
            switch (mode) {
                case RETURN_VALUE:
                    result = target.invokeWithArguments();
                    break;
                case ARGUMENT:
                    result = target.invokeWithArguments(param);
                    break;
                default:
                    String msg = String.format("Test issue: unknown test"
                            + " convertion mode %s.", mode.name());
                    throw new Error(msg);
            }
            if (!failureExpected
                    && (expectedResult != null && !expectedResult.equals(result)
                    || expectedResult == null && result != null)) {
                String msg = String.format("Conversion result %s is not equal"
                        + " to the expected result %10s",
                        result, expectedResult);
                throw new AssertionError(msg);
            }
            if (VERBOSE) {
                String resultStr;
                if (result != null) {
                    resultStr = String.format("Converted value and type are"
                            + " %10s (%10s)", "'" + result + "'",
                            result.getClass().getSimpleName());
                } else {
                    resultStr = String.format("Converted value is %10s", result);
                }
                System.out.println(resultStr);
            }
            if (failureExpected) {
                String msg = String.format("No exception thrown while testing"
                        + " return value conversion: %10s => %10s;"
                        + " parameter: %10s",
                        from, to, param);
                throw new AssertionError(msg);
            }
        } catch (AssertionError e) {
            throw e; // report test failure
        } catch (Throwable e) {
            if (VERBOSE) {
                System.out.printf("%s: %s\n", e.getClass(), e.getMessage());
            }
            if (!failureExpected || !e.getClass().equals(expectedException)) {
                String msg = String.format("Unexpected exception was thrown"
                        + " while testing return value conversion:"
                        + " %s => %s; parameter: %s", from, to, param);
                throw new AssertionError(msg, e);
            }
        }
    }
}
