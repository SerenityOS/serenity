/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import test.java.lang.invoke.lib.Helper;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enumeration containing information about methods from
 * {@code j.l.i.MethodHandles} class that are used for testing lambda forms
 * caching.
 *
 * @author kshefov
 */
public enum TestMethods {

    FOLD_ARGUMENTS("foldArguments") {
        @Override
        public Map<String, Object> getTestCaseData() {
            Map<String, Object> data = new HashMap<>();
            int desiredArity = Helper.RNG.nextInt(super.maxArity);
            MethodType mtTarget = TestMethods.randomMethodTypeGenerator(desiredArity);
            data.put("mtTarget", mtTarget);
            // Arity after reducing because of long and double take 2 slots.
            int realArity = mtTarget.parameterCount();
            int modifierMHArgNum = Helper.RNG.nextInt(realArity + 1);
            data.put("modifierMHArgNum", modifierMHArgNum);
            Class<?> combinerReturnType;
            if (realArity == 0) {
                combinerReturnType = void.class;
            } else {
                combinerReturnType = Helper.RNG.nextBoolean() ?
                        void.class : mtTarget.parameterType(0);
            }
            data.put("combinerReturnType", combinerReturnType);
            return data;
        }

        @Override
        protected MethodHandle getMH(Map<String, Object> data, TestMethods.Kind kind)
                throws NoSuchMethodException, IllegalAccessException {
            MethodType mtTarget = (MethodType) data.get("mtTarget");
            Class<?> combinerReturnType = (Class) data.get("combinerReturnType");
            int modifierMHArgNum = (int) data.get("modifierMHArgNum");
            MethodHandle target = TestMethods.methodHandleGenerator(mtTarget.returnType(),
                    mtTarget.parameterList(), kind);
            Class<?> rType = mtTarget.returnType();
            int combListStart = (combinerReturnType == void.class) ? 0 : 1;
            if (modifierMHArgNum < combListStart) {
                modifierMHArgNum = combListStart;
            }
            MethodHandle combiner = TestMethods.methodHandleGenerator(combinerReturnType,
                    mtTarget.parameterList().subList(combListStart,
                            modifierMHArgNum), kind);
            return MethodHandles.foldArguments(target, combiner);
        }
    },
    DROP_ARGUMENTS("dropArguments") {
        @Override
        public Map<String, Object> getTestCaseData() {
            Map<String, Object> data = new HashMap<>();
            int desiredArity = Helper.RNG.nextInt(super.maxArity);
            MethodType mtTarget = TestMethods.randomMethodTypeGenerator(desiredArity);
            data.put("mtTarget", mtTarget);
            // Arity after reducing because of long and double take 2 slots.
            int realArity = mtTarget.parameterCount();
            int dropArgsPos = Helper.RNG.nextInt(realArity + 1);
            data.put("dropArgsPos", dropArgsPos);
            MethodType mtDropArgs = TestMethods.randomMethodTypeGenerator(
                    Helper.RNG.nextInt(super.maxArity - realArity));
            data.put("mtDropArgs", mtDropArgs);
            return data;
        }

        @Override
        protected MethodHandle getMH(Map<String, Object> data, TestMethods.Kind kind)
                throws NoSuchMethodException, IllegalAccessException {
            MethodType mtTarget = (MethodType) data.get("mtTarget");
            MethodType mtDropArgs = (MethodType) data.get("mtDropArgs");
            int dropArgsPos = (int) data.get("dropArgsPos");
            MethodHandle target = TestMethods.methodHandleGenerator(mtTarget.returnType(),
                    mtTarget.parameterList(), kind);
            int mtTgtSlotsCount = TestMethods.argSlotsCount(mtTarget);
            int mtDASlotsCount = TestMethods.argSlotsCount(mtDropArgs);
            List<Class<?>> fakeParList;
            if (mtTgtSlotsCount + mtDASlotsCount > super.maxArity - 1) {
                fakeParList = TestMethods.reduceArgListToSlotsCount(mtDropArgs.parameterList(),
                        super.maxArity - mtTgtSlotsCount - 1);
            } else {
                fakeParList = mtDropArgs.parameterList();
            }
            return MethodHandles.dropArguments(target, dropArgsPos, fakeParList);
        }
    },
    EXPLICIT_CAST_ARGUMENTS("explicitCastArguments", Helper.MAX_ARITY / 2) {
        @Override
        public Map<String, Object> getTestCaseData() {
            Map<String, Object> data = new HashMap<>();
            int desiredArity = Helper.RNG.nextInt(super.maxArity);
            MethodType mtTarget = TestMethods.randomMethodTypeGenerator(desiredArity);
            data.put("mtTarget", mtTarget);
            // Arity after reducing because of long and double take 2 slots.
            int realArity = mtTarget.parameterCount();
            MethodType mtExcplCastArgs = TestMethods.randomMethodTypeGenerator(realArity);
            if (mtTarget.returnType() == void.class) {
                mtExcplCastArgs = MethodType.methodType(void.class,
                        mtExcplCastArgs.parameterArray());
            }
            if (mtExcplCastArgs.returnType() == void.class) {
                mtExcplCastArgs = MethodType.methodType(mtTarget.returnType(),
                        mtExcplCastArgs.parameterArray());
            }
            data.put("mtExcplCastArgs", mtExcplCastArgs);
            return data;
        }

        @Override
        protected MethodHandle getMH(Map<String, Object> data, TestMethods.Kind kind)
                throws NoSuchMethodException, IllegalAccessException {
            MethodType mtTarget = (MethodType) data.get("mtTarget");
            MethodType mtExcplCastArgs = (MethodType) data.get("mtExcplCastArgs");
            MethodHandle target = TestMethods.methodHandleGenerator(mtTarget.returnType(),
                    mtTarget.parameterList(), kind);
            return MethodHandles.explicitCastArguments(target, mtExcplCastArgs);
        }
    },
    FILTER_ARGUMENTS("filterArguments", Helper.MAX_ARITY / 2) {
        @Override
        public Map<String, Object> getTestCaseData() {
            Map<String, Object> data = new HashMap<>();
            int desiredArity = Helper.RNG.nextInt(super.maxArity);
            MethodType mtTarget = TestMethods.randomMethodTypeGenerator(desiredArity);
            data.put("mtTarget", mtTarget);
            // Arity after reducing because of long and double take 2 slots.
            int realArity = mtTarget.parameterCount();
            int filterArgsPos = Helper.RNG.nextInt(realArity + 1);
            data.put("filterArgsPos", filterArgsPos);
            int filtersArgsArrayLength = Helper.RNG.nextInt(realArity + 1 - filterArgsPos);
            data.put("filtersArgsArrayLength", filtersArgsArrayLength);
            MethodType mtFilter = TestMethods.randomMethodTypeGenerator(filtersArgsArrayLength);
            data.put("mtFilter", mtFilter);
            return data;
        }

        @Override
        protected MethodHandle getMH(Map<String, Object> data, TestMethods.Kind kind)
                throws NoSuchMethodException, IllegalAccessException {
            MethodType mtTarget = (MethodType) data.get("mtTarget");
            MethodType mtFilter = (MethodType) data.get("mtFilter");
            int filterArgsPos = (int) data.get("filterArgsPos");
            int filtersArgsArrayLength = (int) data.get("filtersArgsArrayLength");
            MethodHandle target = TestMethods.methodHandleGenerator(mtTarget.returnType(),
                    mtTarget.parameterList(), kind);
            MethodHandle[] filters = new MethodHandle[filtersArgsArrayLength];
            for (int i = 0; i < filtersArgsArrayLength; i++) {
                filters[i] = TestMethods.filterGenerator(mtFilter.parameterType(i),
                        mtTarget.parameterType(filterArgsPos + i), kind);
            }
            return MethodHandles.filterArguments(target, filterArgsPos, filters);
        }
    },
    FILTER_RETURN_VALUE("filterReturnValue") {
        @Override
        public Map<String, Object> getTestCaseData() {
            Map<String, Object> data = new HashMap<>();
            int desiredArity = Helper.RNG.nextInt(super.maxArity);
            MethodType mtTarget = TestMethods.randomMethodTypeGenerator(desiredArity);
            data.put("mtTarget", mtTarget);
            // Arity after reducing because of long and double take 2 slots.
            int realArity = mtTarget.parameterCount();
            int filterArgsPos = Helper.RNG.nextInt(realArity + 1);
            int filtersArgsArrayLength = Helper.RNG.nextInt(realArity + 1 - filterArgsPos);
            MethodType mtFilter = TestMethods.randomMethodTypeGenerator(filtersArgsArrayLength);
            data.put("mtFilter", mtFilter);
            return data;
        }

        @Override
        protected MethodHandle getMH(Map<String, Object> data, TestMethods.Kind kind)
                throws NoSuchMethodException, IllegalAccessException {
            MethodType mtTarget = (MethodType) data.get("mtTarget");
            MethodType mtFilter = (MethodType) data.get("mtFilter");
            MethodHandle target = TestMethods.methodHandleGenerator(mtTarget.returnType(),
                    mtTarget.parameterList(), kind);
            MethodHandle filter = TestMethods.filterGenerator(mtTarget.returnType(),
                    mtFilter.returnType(), kind);
            return MethodHandles.filterReturnValue(target, filter);
        }
    },
    INSERT_ARGUMENTS("insertArguments", Helper.MAX_ARITY - 3) {
        @Override
        public Map<String, Object> getTestCaseData() {
            Map<String, Object> data = new HashMap<>();
            int desiredArity = Helper.RNG.nextInt(super.maxArity);
            MethodType mtTarget = TestMethods.randomMethodTypeGenerator(desiredArity);
            data.put("mtTarget", mtTarget);
            // Arity after reducing because of long and double take 2 slots.
            int realArity = mtTarget.parameterCount();
            int insertArgsPos = Helper.RNG.nextInt(realArity + 1);
            data.put("insertArgsPos", insertArgsPos);
            int insertArgsArrayLength = Helper.RNG.nextInt(realArity + 1 - insertArgsPos);
            MethodType mtInsertArgs = MethodType.methodType(void.class, mtTarget.parameterList()
                    .subList(insertArgsPos, insertArgsPos + insertArgsArrayLength));
            data.put("mtInsertArgs", mtInsertArgs);
            return data;
        }

        @Override
        protected MethodHandle getMH(Map<String, Object> data, TestMethods.Kind kind)
                throws NoSuchMethodException, IllegalAccessException {
            MethodType mtTarget = (MethodType) data.get("mtTarget");
            MethodType mtInsertArgs = (MethodType) data.get("mtInsertArgs");
            int insertArgsPos = (int) data.get("insertArgsPos");
            MethodHandle target = TestMethods.methodHandleGenerator(mtTarget.returnType(),
                    mtTarget.parameterList(), kind);
            Object[] insertList = Helper.randomArgs(mtInsertArgs.parameterList());
            return MethodHandles.insertArguments(target, insertArgsPos, insertList);
        }
    },
    PERMUTE_ARGUMENTS("permuteArguments", Helper.MAX_ARITY / 2) {
        @Override
        public Map<String, Object> getTestCaseData() {
            Map<String, Object> data = new HashMap<>();
            int desiredArity = Helper.RNG.nextInt(super.maxArity);
            MethodType mtTarget = TestMethods.randomMethodTypeGenerator(desiredArity);
            // Arity after reducing because of long and double take 2 slots.
            int realArity = mtTarget.parameterCount();
            int[] permuteArgsReorderArray = new int[realArity];
            int mtPermuteArgsNum = Helper.RNG.nextInt(Helper.MAX_ARITY);
            mtPermuteArgsNum = mtPermuteArgsNum == 0 ? 1 : mtPermuteArgsNum;
            MethodType mtPermuteArgs = TestMethods.randomMethodTypeGenerator(mtPermuteArgsNum);
            mtTarget = mtTarget.changeReturnType(mtPermuteArgs.returnType());
            for (int i = 0; i < realArity; i++) {
                int mtPermuteArgsParNum = Helper.RNG.nextInt(mtPermuteArgs.parameterCount());
                permuteArgsReorderArray[i] = mtPermuteArgsParNum;
                mtTarget = mtTarget.changeParameterType(
                        i, mtPermuteArgs.parameterType(mtPermuteArgsParNum));
            }
            data.put("mtTarget", mtTarget);
            data.put("permuteArgsReorderArray", permuteArgsReorderArray);
            data.put("mtPermuteArgs", mtPermuteArgs);
            return data;
        }

        @Override
        protected MethodHandle getMH(Map<String, Object> data, TestMethods.Kind kind)
                throws NoSuchMethodException, IllegalAccessException {
            MethodType mtTarget = (MethodType) data.get("mtTarget");
            MethodType mtPermuteArgs = (MethodType) data.get("mtPermuteArgs");
            int[] permuteArgsReorderArray = (int[]) data.get("permuteArgsReorderArray");
            MethodHandle target = TestMethods.methodHandleGenerator(mtTarget.returnType(),
                    mtTarget.parameterList(), kind);
            return MethodHandles.permuteArguments(target, mtPermuteArgs, permuteArgsReorderArray);
        }
    },
    THROW_EXCEPTION("throwException") {
        @Override
        public Map<String, Object> getTestCaseData() {
            Map<String, Object> data = new HashMap<>();
            int desiredArity = Helper.RNG.nextInt(super.maxArity);
            MethodType mtTarget = TestMethods.randomMethodTypeGenerator(desiredArity);
            data.put("mtTarget", mtTarget);
            return data;
        }

        @Override
        protected MethodHandle getMH(Map<String, Object> data, TestMethods.Kind kind) {
            MethodType mtTarget = (MethodType) data.get("mtTarget");
            Class<?> rType = mtTarget.returnType();
            return MethodHandles.throwException(rType, Exception.class
            );
        }
    },
    GUARD_WITH_TEST("guardWithTest") {
        @Override
        public Map<String, Object> getTestCaseData() {
            Map<String, Object> data = new HashMap<>();
            int desiredArity = Helper.RNG.nextInt(super.maxArity);
            MethodType mtTarget = TestMethods.randomMethodTypeGenerator(desiredArity);
            data.put("mtTarget", mtTarget);
            // Arity after reducing because of long and double take 2 slots.
            int realArity = mtTarget.parameterCount();
            int modifierMHArgNum = Helper.RNG.nextInt(realArity + 1);
            data.put("modifierMHArgNum", modifierMHArgNum);
            return data;
        }

        @Override
        protected MethodHandle getMH(Map<String, Object> data, TestMethods.Kind kind)
                throws NoSuchMethodException, IllegalAccessException {
            MethodType mtTarget = (MethodType) data.get("mtTarget");
            int modifierMHArgNum = (int) data.get("modifierMHArgNum");
            TestMethods.Kind targetKind;
            TestMethods.Kind fallbackKind;
            if (kind.equals(TestMethods.Kind.ONE)) {
                targetKind = TestMethods.Kind.ONE;
                fallbackKind = TestMethods.Kind.TWO;
            } else {
                targetKind = TestMethods.Kind.TWO;
                fallbackKind = TestMethods.Kind.ONE;
            }
            MethodHandle target = TestMethods.methodHandleGenerator(mtTarget.returnType(),
                    mtTarget.parameterList(), targetKind);
            MethodHandle fallback = TestMethods.methodHandleGenerator(mtTarget.returnType(),
                    mtTarget.parameterList(), fallbackKind);
            MethodHandle test = TestMethods.methodHandleGenerator(boolean.class,
                    mtTarget.parameterList().subList(0, modifierMHArgNum), kind);
            return MethodHandles.guardWithTest(test, target, fallback);
        }
    },
    CATCH_EXCEPTION("catchException") {
        @Override
        public Map<String, Object> getTestCaseData() {
            Map<String, Object> data = new HashMap<>();
            int desiredArity = Helper.RNG.nextInt(super.maxArity);
            MethodType mtTarget = TestMethods.randomMethodTypeGenerator(desiredArity);
            data.put("mtTarget", mtTarget);
            // Arity after reducing because of long and double take 2 slots.
            int realArity = mtTarget.parameterCount();
            int modifierMHArgNum = Helper.RNG.nextInt(realArity + 1);
            data.put("modifierMHArgNum", modifierMHArgNum);
            return data;
        }

        @Override
        protected MethodHandle getMH(Map<String, Object> data, TestMethods.Kind kind)
                throws NoSuchMethodException, IllegalAccessException {
            MethodType mtTarget = (MethodType) data.get("mtTarget");
            int modifierMHArgNum = (int) data.get("modifierMHArgNum");
            MethodHandle target;
            if (kind.equals(TestMethods.Kind.ONE)) {
                target = TestMethods.methodHandleGenerator(mtTarget.returnType(),
                        mtTarget.parameterList(), TestMethods.Kind.ONE);
            } else {
                target = TestMethods.methodHandleGenerator(mtTarget.returnType(),
                        mtTarget.parameterList(), TestMethods.Kind.EXCEPT);
            }
            List<Class<?>> handlerParamList = new ArrayList<>(mtTarget.parameterCount() + 1);
            handlerParamList.add(Exception.class);
            handlerParamList.addAll(mtTarget.parameterList().subList(0, modifierMHArgNum));
            MethodHandle handler = TestMethods.methodHandleGenerator(
                    mtTarget.returnType(), handlerParamList, TestMethods.Kind.TWO);
            return MethodHandles.catchException(target, Exception.class, handler);
        }
    },
    INVOKER("invoker", Helper.MAX_ARITY - 1) {
        @Override
        public Map<String, Object> getTestCaseData() {
            Map<String, Object> data = new HashMap<>();
            int desiredArity = Helper.RNG.nextInt(super.maxArity);
            MethodType mtTarget = TestMethods.randomMethodTypeGenerator(desiredArity);
            data.put("mtTarget", mtTarget);
            return data;
        }

        @Override
        protected MethodHandle getMH(Map<String, Object> data, TestMethods.Kind kind) {
            MethodType mtTarget = (MethodType) data.get("mtTarget");
            return MethodHandles.invoker(mtTarget);
        }
    },
    EXACT_INVOKER("exactInvoker", Helper.MAX_ARITY - 1) {
        @Override
        public Map<String, Object> getTestCaseData() {
            Map<String, Object> data = new HashMap<>();
            int desiredArity = Helper.RNG.nextInt(super.maxArity);
            MethodType mtTarget = TestMethods.randomMethodTypeGenerator(desiredArity);
            data.put("mtTarget", mtTarget);
            return data;
        }

        @Override
        protected MethodHandle getMH(Map<String, Object> data, TestMethods.Kind kind) {
            MethodType mtTarget = (MethodType) data.get("mtTarget");
            return MethodHandles.exactInvoker(mtTarget);
        }
    },
    SPREAD_INVOKER("spreadInvoker", Helper.MAX_ARITY - 1) {
        @Override
        public Map<String, Object> getTestCaseData() {
            Map<String, Object> data = new HashMap<>();
            int desiredArity = Helper.RNG.nextInt(super.maxArity);
            MethodType mtTarget = TestMethods.randomMethodTypeGenerator(desiredArity);
            data.put("mtTarget", mtTarget);
            // Arity after reducing because of long and double take 2 slots.
            int realArity = mtTarget.parameterCount();
            int modifierMHArgNum = Helper.RNG.nextInt(realArity + 1);
            data.put("modifierMHArgNum", modifierMHArgNum);
            return data;
        }

        @Override
        protected MethodHandle getMH(Map<String, Object> data, TestMethods.Kind kind) {
            MethodType mtTarget = (MethodType) data.get("mtTarget");
            int modifierMHArgNum = (int) data.get("modifierMHArgNum");
            return MethodHandles.spreadInvoker(mtTarget, modifierMHArgNum);
        }
    },
    ARRAY_ELEMENT_GETTER("arrayElementGetter") {
        @Override
        public Map<String, Object> getTestCaseData() {
            Map<String, Object> data = new HashMap<>();
            int desiredArity = Helper.RNG.nextInt(super.maxArity);
            MethodType mtTarget = TestMethods.randomMethodTypeGenerator(desiredArity);
            data.put("mtTarget", mtTarget);
            return data;
        }

        @Override
        protected MethodHandle getMH(Map<String, Object> data, TestMethods.Kind kind) {
            MethodType mtTarget = (MethodType) data.get("mtTarget");
            Class<?> rType = mtTarget.returnType();
            if (rType == void.class) {
                rType = Object.class;
            }
            return MethodHandles.arrayElementGetter(Array.newInstance(rType, 2).getClass());
        }
    },
    ARRAY_ELEMENT_SETTER("arrayElementSetter") {
        @Override
        public Map<String, Object> getTestCaseData() {
            Map<String, Object> data = new HashMap<>();
            int desiredArity = Helper.RNG.nextInt(super.maxArity);
            MethodType mtTarget = TestMethods.randomMethodTypeGenerator(desiredArity);
            data.put("mtTarget", mtTarget);
            return data;
        }

        @Override
        protected MethodHandle getMH(Map<String, Object> data, TestMethods.Kind kind) {
            MethodType mtTarget = (MethodType) data.get("mtTarget");
            Class<?> rType = mtTarget.returnType();
            if (rType == void.class) {
                rType = Object.class;
            }
            return MethodHandles.arrayElementSetter(Array.newInstance(rType, 2).getClass());
        }
    },
    CONSTANT("constant") {
        @Override
        public Map<String, Object> getTestCaseData() {
            Map<String, Object> data = new HashMap<>();
            int desiredArity = Helper.RNG.nextInt(super.maxArity);
            MethodType mtTarget = TestMethods.randomMethodTypeGenerator(desiredArity);
            data.put("mtTarget", mtTarget);
            return data;
        }

        @Override
        protected MethodHandle getMH(Map<String, Object> data, TestMethods.Kind kind) {
            MethodType mtTarget = (MethodType) data.get("mtTarget");
            Class<?> rType = mtTarget.returnType();
            if (rType == void.class) {
                rType = Object.class;
            }
            if (rType.equals(boolean.class)) {
                // There should be the same return values because for default values there are special "zero" forms
                return MethodHandles.constant(rType, true);
            } else {
                return MethodHandles.constant(rType, kind.getValue(rType));
            }
        }
    },
IDENTITY("identity") {
        @Override
        public Map<String, Object> getTestCaseData() {
            Map<String, Object> data = new HashMap<>();
            int desiredArity = Helper.RNG.nextInt(super.maxArity);
            MethodType mtTarget = TestMethods.randomMethodTypeGenerator(desiredArity);
            data.put("mtTarget", mtTarget);
            return data;
        }

        @Override
        protected MethodHandle getMH(Map<String, Object> data, TestMethods.Kind kind) {
            MethodType mtTarget = (MethodType) data.get("mtTarget");
            Class<?> rType = mtTarget.returnType();
            if (rType == void.class) {
                rType = Object.class;
            }
            return MethodHandles.identity(rType);
        }
    };

    /**
     * Test method's name.
     */
    public final String name;

    private final int maxArity;

    private TestMethods(String name, int maxArity) {
        this.name = name;
        this.maxArity = maxArity;
    }

    private TestMethods(String name) {
        this(name, Helper.MAX_ARITY);
    }

    protected MethodHandle getMH(Map<String, Object> data, TestMethods.Kind kind)
            throws NoSuchMethodException, IllegalAccessException {
        throw new UnsupportedOperationException(
                "TESTBUG: getMH method is not implemented for test method " + this);
    }

    /**
     * Creates an adapter method handle depending on a test method from
     * MethodHandles class. Adapter is what is returned by the test method. This
     * method is able to create two kinds of adapters, their type will be the
     * same, but return values are different.
     *
     * @param data a Map containing data to create a method handle, can be
     * obtained by {@link #getTestCaseData} method
     * @param kind defines whether adapter ONE or adapter TWO will be
     * initialized. Should be equal to TestMethods.Kind.ONE or
     * TestMethods.Kind.TWO
     * @return Method handle adapter that behaves according to
     * TestMethods.Kind.ONE or TestMethods.Kind.TWO
     * @throws java.lang.NoSuchMethodException
     * @throws java.lang.IllegalAccessException
     */
    public MethodHandle getTestCaseMH(Map<String, Object> data, TestMethods.Kind kind)
            throws NoSuchMethodException, IllegalAccessException {
        if (data == null) {
            throw new Error(String.format("TESTBUG: Data for test method %s is not prepared",
                    this.name));
        }
        if (!kind.equals(TestMethods.Kind.ONE) && !kind.equals(TestMethods.Kind.TWO)) {
            throw new IllegalArgumentException("TESTBUG: Wrong \"kind\" (" + kind
                    + ") arg to getTestCaseMH function."
                    + " Should be Kind.ONE or Kind.TWO");
        }
        return getMH(data, kind);
    }

    /**
     * Returns a data Map needed for {@link #getTestCaseMH} method.
     *
     * @return data Map needed for {@link #getTestCaseMH} method
     */
    public Map<String, Object> getTestCaseData() {
        throw new UnsupportedOperationException(
                "TESTBUG: getTestCaseData method is not implemented for test method " + this);
    }

    /**
     * Enumeration used in methodHandleGenerator to define whether a MH returned
     * by this method returns "2" in different type representations, "4", or
     * throw an Exception.
     */
    public static enum Kind {

        ONE(2),
        TWO(4),
        EXCEPT(0);

        private final int value;

        private Object getValue(Class<?> cl) {
            return Helper.castToWrapper(value, cl);
        }

        private MethodHandle getBasicMH(Class<?> rType)
                throws NoSuchMethodException, IllegalAccessException {
            MethodHandle result = null;
            switch (this) {
                case ONE:
                case TWO:
                    if (rType.equals(void.class)) {
                        result = MethodHandles.lookup().findVirtual(Kind.class,
                                "returnVoid", MethodType.methodType(void.class));
                        result = MethodHandles.insertArguments(result, 0, this);
                    } else {
                        result = MethodHandles.constant(rType, getValue(rType));
                    }
                    break;
                case EXCEPT:
                    result = MethodHandles.throwException(rType, Exception.class);
                    result = MethodHandles.insertArguments(result, 0, new Exception());
                    break;
            }
            return result;
        }

        private void returnVoid() {
        }

        private Kind(int value) {
            this.value = value;
        }
    }

    /**
     * Routine used to obtain a randomly generated method type.
     *
     * @param arity Arity of returned method type.
     * @return MethodType generated randomly.
     */
    private static MethodType randomMethodTypeGenerator(int arity) {
        return Helper.randomMethodTypeGenerator(arity);
    }

    /**
     * Routine used to obtain a method handles of a given type an kind (return
     * value).
     *
     * @param returnType Type of MH return value.
     * @param argTypes Types of MH args.
     * @param kind Defines whether the obtained MH returns "1" or "2".
     * @return Method handle of the given type.
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     */
    private static MethodHandle methodHandleGenerator(Class<?> returnType,
            List<Class<?>> argTypes, TestMethods.Kind kind)
            throws NoSuchMethodException, IllegalAccessException {
        MethodHandle result;
        result = kind.getBasicMH(returnType);
        return Helper.addTrailingArgs(result, argTypes.size(), argTypes);
    }

    /**
     * Routine that generates filter method handles to test
     * MethodHandles.filterArguments method.
     *
     * @param inputType Filter's argument type.
     * @param returnType Filter's return type.
     * @param kind Filter's return value definer.
     * @return A filter method handle, that takes one argument.
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     */
    private static MethodHandle filterGenerator(Class<?> inputType, Class<?> returnType,
            TestMethods.Kind kind) throws NoSuchMethodException, IllegalAccessException {
        MethodHandle tmpMH = kind.getBasicMH(returnType);
        if (inputType.equals(void.class)) {
            return tmpMH;
        }
        ArrayList<Class<?>> inputTypeList = new ArrayList<>(1);
        inputTypeList.add(inputType);
        return Helper.addTrailingArgs(tmpMH, 1, inputTypeList);
    }

    private static int argSlotsCount(MethodType mt) {
        int result = 0;
        for (Class cl : mt.parameterArray()) {
            if (cl.equals(long.class) || cl.equals(double.class)) {
                result += 2;
            } else {
                result++;
            }
        }
        return result;
    }

    private static List<Class<?>> reduceArgListToSlotsCount(List<Class<?>> list,
            int desiredSlotCount) {
        List<Class<?>> result = new ArrayList<>(desiredSlotCount);
        int count = 0;
        for (Class<?> cl : list) {
            if (count >= desiredSlotCount) {
                break;
            }
            if (cl.equals(long.class) || cl.equals(double.class)) {
                count += 2;
            } else {
                count++;
            }
            result.add(cl);
        }
        return result;
    }
}
