/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package combo;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTaskPool;

/**
 * An helper class for defining combinatorial (aka "combo" tests). A combo test is made up of one
 * or more 'dimensions' - each of which represent a different axis of the test space. For instance,
 * if we wanted to test class/interface declaration, one dimension could be the keyword used for
 * the declaration (i.e. 'class' vs. 'interface') while another dimension could be the class/interface
 * modifiers (i.e. 'public', 'pachake-private' etc.). A combo test consists in running a test instance
 * for each point in the test space; that is, for any combination of the combo test dimension:
 * <p>
 * 'public' 'class'
 * 'public' interface'
 * 'package-private' 'class'
 * 'package-private' 'interface'
 * ...
 * <p>
 * A new test instance {@link ComboInstance} is created, and executed, after its dimensions have been
 * initialized accordingly. Each instance can either pass, fail or throw an unexpected error; this helper
 * class defines several policies for how failures should be handled during a combo test execution
 * (i.e. should errors be ignored? Do we want the first failure to result in a failure of the whole
 * combo test?).
 * <p>
 * Additionally, this helper class allows to specify filter methods that can be used to throw out
 * illegal combinations of dimensions - for instance, in the example above, we might want to exclude
 * all combinations involving 'protected' and 'private' modifiers, which are disallowed for toplevel
 * declarations.
 * <p>
 * While combo tests can be used for a variety of workloads, typically their main task will consist
 * in performing some kind of javac compilation. For this purpose, this framework defines an optimized
 * javac context {@link ReusableContext} which can be shared across multiple combo instances,
 * when the framework detects it's safe to do so. This allows to reduce the overhead associated with
 * compiler initialization when the test space is big.
 */
public class ComboTestHelper<X extends ComboInstance<X>> {

    /** Failure mode. */
    FailMode failMode = FailMode.FAIL_FAST;

    /** Ignore mode. */
    IgnoreMode ignoreMode = IgnoreMode.IGNORE_NONE;

    /** Combo test instance filter. */
    Optional<Predicate<X>> optFilter = Optional.empty();

    /** Combo test dimensions. */
    List<DimensionInfo<?>> dimensionInfos = new ArrayList<>();

    /** Combo test stats. */
    Info info = new Info();

    /** Shared JavaCompiler used across all combo test instances. */
    JavaCompiler comp = ToolProvider.getSystemJavaCompiler();

    /** Shared file manager used across all combo test instances. */
    StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null);

    /** JavacTask pool shared across all combo instances. */
    JavacTaskPool pool = new JavacTaskPool(1);

    /**
     * Set failure mode for this combo test.
     */
    public ComboTestHelper<X> withFailMode(FailMode failMode) {
        this.failMode = failMode;
        return this;
    }

    /**
     * Set ignore mode for this combo test.
     */
    public ComboTestHelper<X> withIgnoreMode(IgnoreMode ignoreMode) {
        this.ignoreMode = ignoreMode;
        return this;
    }

    /**
     * Set a filter for combo test instances to be ignored.
     */
    public ComboTestHelper<X> withFilter(Predicate<X> filter) {
        optFilter = Optional.of(optFilter.map(filter::and).orElse(filter));
        return this;
    }

    /**
     * Adds a new dimension to this combo test, with a given name an array of values.
     */
    @SafeVarargs
    public final <D> ComboTestHelper<X> withDimension(String name, D... dims) {
        return withDimension(name, null, dims);
    }

    /**
     * Adds a new dimension to this combo test, with a given name, an array of values and a
     * coresponding setter to be called in order to set the dimension value on the combo test instance
     * (before test execution).
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public final <D> ComboTestHelper<X> withDimension(String name, DimensionSetter<X, D> setter, D... dims) {
        dimensionInfos.add(new DimensionInfo<>(name, dims, setter));
        return this;
    }

    /**
     * Adds a new array dimension to this combo test, with a given base name. This allows to specify
     * multiple dimensions at once; the names of the underlying dimensions will be generated from the
     * base name, using standard array bracket notation - i.e. "DIM[0]", "DIM[1]", etc.
     */
    @SafeVarargs
    public final <D> ComboTestHelper<X> withArrayDimension(String name, int size, D... dims) {
        return withArrayDimension(name, null, size, dims);
    }

    /**
     * Adds a new array dimension to this combo test, with a given base name, an array of values and a
     * coresponding array setter to be called in order to set the dimension value on the combo test
     * instance (before test execution). This allows to specify multiple dimensions at once; the names
     * of the underlying dimensions will be generated from the base name, using standard array bracket
     * notation - i.e. "DIM[0]", "DIM[1]", etc.
     */
    @SafeVarargs
    public final <D> ComboTestHelper<X> withArrayDimension(String name, ArrayDimensionSetter<X, D> setter, int size, D... dims) {
        for (int i = 0 ; i < size ; i++) {
            dimensionInfos.add(new ArrayDimensionInfo<>(name, dims, i, setter));
        }
        return this;
    }

    /**
     * Returns the stat object associated with this combo test.
     */
    public Info info() {
        return info;
    }

    /**
     * Runs this combo test. This will generate the combinatorial explosion of all dimensions, and
     * execute a new test instance (built using given supplier) for each such combination.
     */
    public void run(Supplier<X> instanceBuilder) {
        run(instanceBuilder, null);
    }

    /**
     * Runs this combo test. This will generate the combinatorial explosion of all dimensions, and
     * execute a new test instance (built using given supplier) for each such combination. Before
     * executing the test instance entry point, the supplied initialization method is called on
     * the test instance; this is useful for ad-hoc test instance initialization once all the dimension
     * values have been set.
     */
    public void run(Supplier<X> instanceBuilder, Consumer<X> initAction) {
        runInternal(0, new Stack<>(), instanceBuilder, Optional.ofNullable(initAction));
        end();
    }

    /**
     * Generate combinatorial explosion of all dimension values and create a new test instance
     * for each combination.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void runInternal(int index, Stack<DimensionBinding<?>> bindings, Supplier<X> instanceBuilder, Optional<Consumer<X>> initAction) {
        if (index == dimensionInfos.size()) {
            runCombo(instanceBuilder, initAction, bindings);
        } else {
            DimensionInfo<?> dinfo = dimensionInfos.get(index);
            for (Object d : dinfo.dims) {
                bindings.push(new DimensionBinding(d, dinfo));
                runInternal(index + 1, bindings, instanceBuilder, initAction);
                bindings.pop();
            }
        }
    }

    /**
     * Run a new test instance using supplied dimension bindings. All required setters and initialization
     * method are executed before calling the instance main entry point. Also checks if the instance
     * is compatible with the specified test filters; if not, the test is simply skipped.
     */
    @SuppressWarnings("unchecked")
    private void runCombo(Supplier<X> instanceBuilder, Optional<Consumer<X>> initAction, List<DimensionBinding<?>> bindings) {
        X x = instanceBuilder.get();
        for (DimensionBinding<?> binding : bindings) {
            binding.init(x);
        }
        initAction.ifPresent(action -> action.accept(x));
        info.comboCount++;
        if (!optFilter.isPresent() || optFilter.get().test(x)) {
            x.run(new Env(bindings));
            if (failMode.shouldStop(ignoreMode, info)) {
                end();
            }
        } else {
            info.skippedCount++;
        }
    }

    /**
     * This method is executed upon combo test completion (either normal or erroneous). Closes down
     * all pending resources and dumps useful stats info.
     */
    private void end() {
        try {
            fm.close();
            if (info.hasFailures()) {
                throw new AssertionError("Failure when executing combo:" + info.lastFailure.orElse(""));
            } else if (info.hasErrors()) {
                throw new AssertionError("Unexpected exception while executing combo", info.lastError.get());
            }
        } catch (IOException ex) {
            throw new AssertionError("Failure when closing down shared file manager; ", ex);
        } finally {
            info.dump(this);
        }
    }

    /**
     * Functional interface for specifying combo test instance setters.
     */
    public interface DimensionSetter<X extends ComboInstance<X>, D> {
        void set(X x, D d);
    }

    /**
     * Functional interface for specifying combo test instance array setters. The setter method
     * receives an extra argument for the index of the array element to be set.
     */
    public interface ArrayDimensionSetter<X extends ComboInstance<X>, D> {
        void set(X x, D d, int index);
    }

    /**
     * Dimension descriptor; each dimension has a name, an array of value and an optional setter
     * to be called on the associated combo test instance.
     */
    class DimensionInfo<D> {
        String name;
        D[] dims;
        boolean isParameter;
        Optional<DimensionSetter<X, D>> optSetter;

        DimensionInfo(String name, D[] dims, DimensionSetter<X, D> setter) {
            this.name = name;
            this.dims = dims;
            this.optSetter = Optional.ofNullable(setter);
            this.isParameter = dims[0] instanceof ComboParameter;
        }
    }

    /**
     * Array dimension descriptor. The dimension name is derived from a base name and an index using
     * standard bracket notation; ; the setter accepts an additional 'index' argument to point
     * to the array element to be initialized.
     */
    class ArrayDimensionInfo<D> extends DimensionInfo<D> {
        public ArrayDimensionInfo(String name, D[] dims, int index, ArrayDimensionSetter<X, D> setter) {
            super(String.format("%s[%d]", name, index), dims,
                    setter != null ? (x, d) -> setter.set(x, d, index) : null);
        }
    }

    /**
     * Failure policies for a combo test run.
     */
    public enum FailMode {
        /** Combo test fails when first failure is detected. */
        FAIL_FAST,
        /** Combo test fails after all instances have been executed. */
        FAIL_AFTER;

        boolean shouldStop(IgnoreMode ignoreMode, Info info) {
            switch (this) {
                case FAIL_FAST:
                    return !ignoreMode.canIgnore(info);
                default:
                    return false;
            }
        }
    }

    /**
     * Ignore policies for a combo test run.
     */
    public enum IgnoreMode {
        /** No error or failure is ignored. */
        IGNORE_NONE,
        /** Only errors are ignored. */
        IGNORE_ERRORS,
        /** Only failures are ignored. */
        IGNORE_FAILURES,
        /** Both errors and failures are ignored. */
        IGNORE_ALL;

        boolean canIgnore(Info info) {
            switch (this) {
                case IGNORE_ERRORS:
                    return info.failCount == 0;
                case IGNORE_FAILURES:
                    return info.errCount == 0;
                case IGNORE_ALL:
                    return true;
                default:
                    return info.failCount == 0 && info.errCount == 0;
            }
        }
    }

    /**
     * A dimension binding. This is essentially a pair of a dimension value and its corresponding
     * dimension info.
     */
    class DimensionBinding<D> {
        D d;
        DimensionInfo<D> info;

        DimensionBinding(D d, DimensionInfo<D> info) {
            this.d = d;
            this.info = info;
        }

        void init(X x) {
            info.optSetter.ifPresent(setter -> setter.set(x, d));
        }

        public String toString() {
            return String.format("(%s -> %s)", info.name, d);
        }
    }

    /**
     * This class is used to keep track of combo tests stats; info such as numbero of failures/errors,
     * number of times a context has been shared/dropped are all recorder here.
     */
    public static class Info {
        int failCount;
        int errCount;
        int passCount;
        int comboCount;
        int skippedCount;
        Optional<String> lastFailure = Optional.empty();
        Optional<Throwable> lastError = Optional.empty();

        void dump(ComboTestHelper<?> helper) {
            System.err.println(String.format("%d total checks executed", comboCount));
            System.err.println(String.format("%d successes found", passCount));
            System.err.println(String.format("%d failures found", failCount));
            System.err.println(String.format("%d errors found", errCount));
            System.err.println(String.format("%d skips found", skippedCount));
            helper.pool.printStatistics(System.err);
        }

        public boolean hasFailures() {
            return failCount != 0;
        }

        public boolean hasErrors() {
            return errCount != 0;
        }
    }

    /**
     * The execution environment for a given combo test instance. An environment contains the
     * bindings for all the dimensions, along with the combo parameter cache (this is non-empty
     * only if one or more dimensions are subclasses of the {@code ComboParameter} interface).
     */
    class Env {
        List<DimensionBinding<?>> bindings;
        Map<String, ComboParameter> parametersCache = new HashMap<>();

        @SuppressWarnings({"Unchecked", "rawtypes"})
        Env(List<DimensionBinding<?>> bindings) {
            this.bindings = bindings;
            for (DimensionBinding<?> binding : bindings) {
                if (binding.info.isParameter) {
                    parametersCache.put(binding.info.name, (ComboParameter)binding.d);
                };
            }
        }

        Info info() {
            return ComboTestHelper.this.info();
        }

        StandardJavaFileManager fileManager() {
            return fm;
        }

        JavaCompiler javaCompiler() {
            return comp;
        }

        JavacTaskPool pool() {
            return pool;
        }
    }
}



