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

package test.java.lang.invoke.MethodHandles;

import jdk.test.lib.TimeLimitedRunner;
import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;
import test.java.lang.invoke.lib.CodeCacheOverflowProcessor;
import test.java.lang.invoke.lib.Helper;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/* @test
 * @library /java/lang/invoke/common /test/lib
 * @build jdk.test.lib.TimeLimitedRunner
 * @compile CatchExceptionTest.java
 * @run main/othervm -esa test.java.lang.invoke.MethodHandles.CatchExceptionTest
 * @key intermittent randomness
 */
public class CatchExceptionTest {
    private static final List<Class<?>> ARGS_CLASSES;
    protected static final int MAX_ARITY = Helper.MAX_ARITY - 1;

    static {
        Class<?> classes[] = {
                Object.class,
                long.class,
                int.class,
                byte.class,
                Integer[].class,
                double[].class,
                String.class,
        };
        ARGS_CLASSES = Collections.unmodifiableList(
                Helper.randomClasses(classes, MAX_ARITY));
    }

    private final TestCase testCase;
    private final int nargs;
    private final int argsCount;
    private final MethodHandle catcher;
    private int dropped;
    private MethodHandle thrower;

    public CatchExceptionTest(TestCase testCase, final boolean isVararg,
                              final int argsCount, final int catchDrops) {
        this.testCase = testCase;
        this.dropped = catchDrops;
        MethodHandle thrower = testCase.thrower;
        int throwerLen = thrower.type().parameterCount();
        List<Class<?>> classes;
        int extra = Math.max(0, argsCount - throwerLen);
        classes = getThrowerParams(isVararg, extra);
        this.argsCount = throwerLen + classes.size();
        thrower = Helper.addTrailingArgs(thrower, this.argsCount, classes);
        if (isVararg && argsCount > throwerLen) {
            MethodType mt = thrower.type();
            Class<?> lastParam = mt.parameterType(mt.parameterCount() - 1);
            thrower = thrower.asVarargsCollector(lastParam);
        }
        this.thrower = thrower;
        this.dropped = Math.min(this.argsCount, catchDrops);
        catcher = testCase.getCatcher(getCatcherParams());
        nargs = Math.max(2, this.argsCount);
    }

    public static void main(String[] args) throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(CatchExceptionTest::test);
    }

    public static void test() throws Throwable {
        System.out.println("classes = " + ARGS_CLASSES);

        TestFactory factory = new TestFactory();
        long timeout = Helper.IS_THOROUGH ? 0L : Utils.adjustTimeout(Utils.DEFAULT_TEST_TIMEOUT);
        // subtract vm init time and reserve time for vm exit
        timeout *= 0.9;
        TimeLimitedRunner runner = new TimeLimitedRunner(timeout, 2.0d,
                () -> {
                    CatchExceptionTest test = factory.nextTest();
                    if (test != null) {
                        test.runTest();
                        return true;
                    }
                    return false;
                });
        for (CatchExceptionTest test : TestFactory.MANDATORY_TEST_CASES) {
            test.runTest();
        }
        runner.call();
    }

    private List<Class<?>> getThrowerParams(boolean isVararg, int argsCount) {
        return Helper.getParams(ARGS_CLASSES, isVararg, argsCount);
    }

    private List<Class<?>> getCatcherParams() {
        int catchArgc = 1 + this.argsCount - dropped;
        List<Class<?>> result = new ArrayList<>(
                thrower.type().parameterList().subList(0, catchArgc - 1));
        // prepend throwable
        result.add(0, testCase.throwableClass);
        return result;
    }

    private void runTest() {
        if (Helper.IS_VERBOSE) {
            System.out.printf("CatchException(%s, isVararg=%b argsCount=%d " +
                            "dropped=%d)%n",
                    testCase, thrower.isVarargsCollector(), argsCount, dropped);
        }

        Helper.clear();

        Object[] args = Helper.randomArgs(
                argsCount, thrower.type().parameterArray());
        Object arg0 = Helper.MISSING_ARG;
        Object arg1 = testCase.thrown;
        if (argsCount > 0) {
            arg0 = args[0];
        }
        if (argsCount > 1) {
            args[1] = arg1;
        }
        Asserts.assertEQ(nargs, thrower.type().parameterCount());
        if (argsCount < nargs) {
            Object[] appendArgs = {arg0, arg1};
            appendArgs = Arrays.copyOfRange(appendArgs, argsCount, nargs);
            thrower = MethodHandles.insertArguments(
                    thrower, argsCount, appendArgs);
        }
        Asserts.assertEQ(argsCount, thrower.type().parameterCount());

        MethodHandle target = MethodHandles.catchException(
                testCase.filter(thrower), testCase.throwableClass,
                testCase.filter(catcher));

        Asserts.assertEQ(thrower.type(), target.type());
        Asserts.assertEQ(argsCount, target.type().parameterCount());

        Object returned;
        try {
            returned = target.invokeWithArguments(args);
        } catch (Throwable ex) {
            if (CodeCacheOverflowProcessor.isThrowableCausedByVME(ex)) {
                // This error will be treated by CodeCacheOverflowProcessor
                // to prevent the test from failing because of code cache overflow.
                throw new Error(ex);
            }
            testCase.assertCatch(ex);
            returned = ex;
        }

        testCase.assertReturn(returned, arg0, arg1, dropped, args);
    }
}

class TestFactory {
    public static final List<CatchExceptionTest> MANDATORY_TEST_CASES = new ArrayList<>();

    private static final int MIN_TESTED_ARITY = 10;

    static {
        for (int[] args : new int[][]{
                {0, 0},
                {MIN_TESTED_ARITY, 0},
                {MIN_TESTED_ARITY, MIN_TESTED_ARITY},
                {CatchExceptionTest.MAX_ARITY, 0},
                {CatchExceptionTest.MAX_ARITY, CatchExceptionTest.MAX_ARITY},
        }) {
                MANDATORY_TEST_CASES.addAll(createTests(args[0], args[1]));
        }
    }

    private int count;
    private int args;
    private int dropArgs;
    private int currentMaxDrops;
    private int maxArgs;
    private int maxDrops;
    private int constructor;
    private int constructorSize;
    private boolean isVararg;

    public TestFactory() {
        if (Helper.IS_THOROUGH) {
            maxArgs = maxDrops = CatchExceptionTest.MAX_ARITY;
        } else {
            maxArgs = MIN_TESTED_ARITY
                    + Helper.RNG.nextInt(CatchExceptionTest.MAX_ARITY
                            - MIN_TESTED_ARITY)
                    + 1;
            maxDrops = MIN_TESTED_ARITY
                    + Helper.RNG.nextInt(maxArgs - MIN_TESTED_ARITY)
                    + 1;
            args = 1;
        }

        System.out.printf("maxArgs = %d%nmaxDrops = %d%n", maxArgs, maxDrops);
        constructorSize = TestCase.CONSTRUCTORS.size();
    }

    private static List<CatchExceptionTest> createTests(int argsCount,
            int catchDrops) {
        if (catchDrops > argsCount || argsCount < 0 || catchDrops < 0) {
            throw new IllegalArgumentException("argsCount = " + argsCount
                    + ", catchDrops = " + catchDrops
            );
        }
        List<CatchExceptionTest> result = new ArrayList<>(
                TestCase.CONSTRUCTORS.size());
        for (Supplier<TestCase> constructor : TestCase.CONSTRUCTORS) {
            result.add(new CatchExceptionTest(constructor.get(),
                    /* isVararg = */ true,
                    argsCount,
                    catchDrops));
            result.add(new CatchExceptionTest(constructor.get(),
                    /* isVararg = */ false,
                    argsCount,
                    catchDrops));
        }
        return result;
    }

    /**
     * @return next test from test matrix:
     * {varArgs, noVarArgs} x TestCase.rtypes x TestCase.THROWABLES x {1, .., maxArgs } x {0, .., maxDrops}
     */
    public CatchExceptionTest nextTest() {
        if (constructor < constructorSize) {
            return createTest();
        }
        constructor = 0;
        count++;
        if (!Helper.IS_THOROUGH && count > Helper.TEST_LIMIT) {
            System.out.println("test limit is exceeded");
            return null;
        }
        if (dropArgs <= currentMaxDrops) {
            if (dropArgs == 0) {
                if (Helper.IS_THOROUGH || Helper.RNG.nextBoolean()) {
                    ++dropArgs;
                    return createTest();
                } else if (Helper.IS_VERBOSE) {
                    System.out.printf(
                            "argsCount=%d : \"drop\" scenarios are skipped%n",
                            args);
                }
            } else {
                ++dropArgs;
                return createTest();
            }
        }

        if (args < maxArgs) {
            dropArgs = 0;
            currentMaxDrops = Math.min(args, maxDrops);
            ++args;
            return createTest();
        }
        return null;
    }

    private CatchExceptionTest createTest() {
        if (!Helper.IS_THOROUGH) {
            return new CatchExceptionTest(
                    TestCase.CONSTRUCTORS.get(constructor++).get(),
                    Helper.RNG.nextBoolean(), args, dropArgs);
        } else {
           if (isVararg) {
               isVararg = false;
               return new CatchExceptionTest(
                       TestCase.CONSTRUCTORS.get(constructor++).get(),
                       isVararg, args, dropArgs);
           } else {
               isVararg = true;
               return new CatchExceptionTest(
                       TestCase.CONSTRUCTORS.get(constructor).get(),
                       isVararg, args, dropArgs);
           }
        }
    }
}

class TestCase<T> {
    private static enum ThrowMode {
        NOTHING,
        CAUGHT,
        UNCAUGHT,
        ADAPTER
    }

    @SuppressWarnings("unchecked")
    public static final List<Supplier<TestCase>> CONSTRUCTORS;
    private static final MethodHandle FAKE_IDENTITY;
    private static final MethodHandle THROW_OR_RETURN;
    private static final MethodHandle CATCHER;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            THROW_OR_RETURN = lookup.findStatic(
                    TestCase.class,
                    "throwOrReturn",
                    MethodType.methodType(Object.class, Object.class,
                            Throwable.class)
            );
            CATCHER = lookup.findStatic(
                    TestCase.class,
                    "catcher",
                    MethodType.methodType(Object.class, Object.class));
            FAKE_IDENTITY = lookup.findVirtual(
                    TestCase.class, "fakeIdentity",
                    MethodType.methodType(Object.class, Object.class));

        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new Error(e);
        }
        PartialConstructor[] constructors = {
                create(Object.class, Object.class::cast),
                create(String.class, Objects::toString),
                create(int[].class, x -> new int[]{Objects.hashCode(x)}),
                create(long.class,
                        x -> Objects.hashCode(x) & (-1L >>> 32)),
                create(void.class, TestCase::noop)};
        Throwable[] throwables = {
                new ClassCastException("testing"),
                new java.io.IOException("testing"),
                new LinkageError("testing")};
        List<Supplier<TestCase>> list = new ArrayList<>(constructors.length
                * throwables.length * ThrowMode.values().length);
        //noinspection unchecked
        for (PartialConstructor f : constructors) {
            for (ThrowMode mode : ThrowMode.values()) {
                for (Throwable t : throwables) {
                    list.add(f.apply(mode, t));
                }
            }
        }
        CONSTRUCTORS = Collections.unmodifiableList(list);
    }

    public final Class<T> rtype;
    public final ThrowMode throwMode;
    public final Throwable thrown;
    public final Class<? extends Throwable> throwableClass;
    /**
     * MH which takes 2 args (Object,Throwable), 1st is the return value,
     * 2nd is the exception which will be thrown, if it's supposed in current
     * {@link #throwMode}.
     */
    public final MethodHandle thrower;
    private final Function<Object, T> cast;
    protected MethodHandle filter;
    private int fakeIdentityCount;

    private TestCase(Class<T> rtype, Function<Object, T> cast,
            ThrowMode throwMode, Throwable thrown)
            throws NoSuchMethodException, IllegalAccessException {
        this.cast = cast;
        filter = MethodHandles.lookup().findVirtual(
                Function.class,
                "apply",
                MethodType.methodType(Object.class, Object.class))
                              .bindTo(cast);
        this.rtype = rtype;
        this.throwMode = throwMode;
        this.throwableClass = thrown.getClass();
        switch (throwMode) {
            case NOTHING:
                this.thrown = null;
                break;
            case ADAPTER:
            case UNCAUGHT:
                this.thrown = new Error("do not catch this");
                break;
            default:
                this.thrown = thrown;
        }

        MethodHandle throwOrReturn = THROW_OR_RETURN;
        if (throwMode == ThrowMode.ADAPTER) {
            MethodHandle fakeIdentity = FAKE_IDENTITY.bindTo(this);
            for (int i = 0; i < 10; ++i) {
                throwOrReturn = MethodHandles.filterReturnValue(
                        throwOrReturn, fakeIdentity);
            }
        }
        thrower = throwOrReturn.asType(MethodType.genericMethodType(2));
    }

    private static Void noop(Object x) {
        return null;
    }

    private static <T2> PartialConstructor create(
            Class<T2> rtype, Function<Object, T2> cast) {
        return (t, u) -> () -> {
            try {
                return new TestCase<>(rtype, cast, t, u);
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new Error(e);
            }
        };
    }

    private static <T extends Throwable>
    Object throwOrReturn(Object normal, T exception) throws T {
        if (exception != null) {
            Helper.called("throwOrReturn/throw", normal, exception);
            throw exception;
        }
        Helper.called("throwOrReturn/normal", normal, exception);
        return normal;
    }

    private static <T extends Throwable>
    Object catcher(Object o) {
        Helper.called("catcher", o);
        return o;
    }

    public MethodHandle filter(MethodHandle target) {
        return MethodHandles.filterReturnValue(target, filter);
    }

    public MethodHandle getCatcher(List<Class<?>> classes) {
        return MethodHandles.filterReturnValue(Helper.AS_LIST.asType(
                        MethodType.methodType(Object.class, classes)),
                CATCHER
        );
    }

    @Override
    public String toString() {
        return "TestCase{" +
                "rtype=" + rtype +
                ", throwMode=" + throwMode +
                ", throwableClass=" + throwableClass +
                '}';
    }

    public String callName() {
        return "throwOrReturn/" +
                (throwMode == ThrowMode.NOTHING
                        ? "normal"
                        : "throw");
    }

    public void assertReturn(Object returned, Object arg0, Object arg1,
            int catchDrops, Object... args) {
        int lag = 0;
        if (throwMode == ThrowMode.CAUGHT) {
            lag = 1;
        }
        Helper.assertCalled(lag, callName(), arg0, arg1);

        if (throwMode == ThrowMode.NOTHING) {
            assertEQ(cast.apply(arg0), returned);
        } else if (throwMode == ThrowMode.CAUGHT) {
            List<Object> catchArgs = new ArrayList<>(Arrays.asList(args));
            // catcher receives an initial subsequence of target arguments:
            catchArgs.subList(args.length - catchDrops, args.length).clear();
            // catcher also receives the exception, prepended:
            catchArgs.add(0, thrown);
            Helper.assertCalled("catcher", catchArgs);
            assertEQ(cast.apply(catchArgs), returned);
        }
        Asserts.assertEQ(0, fakeIdentityCount);
    }

    private void assertEQ(T t, Object returned) {
        if (rtype.isArray()) {
            Asserts.assertEQ(t.getClass(), returned.getClass());
            int n = Array.getLength(t);
            Asserts.assertEQ(n, Array.getLength(returned));
            for (int i = 0; i < n; ++i) {
                Asserts.assertEQ(Array.get(t, i), Array.get(returned, i));
            }
        } else {
            Asserts.assertEQ(t, returned);
        }
    }

    private Object fakeIdentity(Object x) {
        System.out.println("should throw through this!");
        ++fakeIdentityCount;
        return x;
    }

    public void assertCatch(Throwable ex) {
        try {
            Asserts.assertSame(thrown, ex,
                    "must get the out-of-band exception");
        } catch (Throwable t) {
            ex.printStackTrace();
        }
    }

    public interface PartialConstructor
            extends BiFunction<ThrowMode, Throwable, Supplier<TestCase>> {
    }
}
