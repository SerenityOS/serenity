/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package vm.runtime.defmeth.shared.builder;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

import nsk.share.Pair;

import static jdk.internal.org.objectweb.asm.Opcodes.*;

import nsk.share.TestFailure;
import vm.runtime.defmeth.shared.ClassFileGenerator;
import vm.runtime.defmeth.shared.Constants;
import vm.runtime.defmeth.shared.DefMethTest;
import vm.runtime.defmeth.shared.DefMethTestFailure;
import vm.runtime.defmeth.shared.ExecutionMode;
import vm.runtime.defmeth.shared.executor.GeneratedTest;
import vm.runtime.defmeth.shared.MemoryClassLoader;
import vm.runtime.defmeth.shared.executor.MHInvokeWithArgsTest;
import vm.runtime.defmeth.shared.Printer;
import vm.runtime.defmeth.shared.executor.ReflectionTest;
import vm.runtime.defmeth.shared.executor.TestExecutor;
import vm.runtime.defmeth.shared.Util;

import vm.runtime.defmeth.shared.data.Clazz;
import vm.runtime.defmeth.shared.data.ConcreteClass;
import vm.runtime.defmeth.shared.data.ConcreteClassLazyAdapter;
import vm.runtime.defmeth.shared.data.Interface;
import vm.runtime.defmeth.shared.data.InterfaceLazyAdapter;
import vm.runtime.defmeth.shared.data.Tester;

/**
 * Builder for test cases.
 *
 * Simplifies construction of test cases.
 *
 * Example:
 * <code>
 * TestBuilder b = new TestBuilder();
 *
 * Interface I = b.intf("I").build();
 * ConcreteClazz C = b.clazz("C").implements(I).build();
 *
 * b.test().callSite(I, C, "hashCode","()I").returns(0).build();
 *
 * b.run();
 * </code>
 *
 * produces
 *
 * <code>
 * interface I {}
 *
 * class C implements I {}
 *
 * class Test1 {
 *   public void test() {
 *     I i = new C();
 *     if (c.hashCode() != 0) {
 *       throw new RuntimeException("Expected 0");
 *     }
 *   }
 * }
 * </code>
 */
public class TestBuilder {
    // Class file major version
    // Used for separate compilation scenarios
    public final int minMajorVer;

    // Additional access flags for all methods
    public final int accFlags;

    // Redefine classes as part of testing
    public final boolean redefineClasses;

    // Redefine classes using Retransformation API
    public final boolean retransformClasses;

    public final ExecutionMode executionMode;

    // GeneratedTest counter which is used to name the tests.
    private int testNo = 0;

    // List of classes constructed for the testcase so far
    private List<Clazz> elements = new ArrayList<>();
    public List<Clazz> getElements() {
        return new ArrayList<>(elements);
    }

    // List of tests constructed as part of the testcase
    private List<Tester> tests = new ArrayList<>();

    // Elements under construction
    private Set<Builder<?>> underConstruction = new LinkedHashSet<>();

    private DefMethTest testInstance;

    TestBuilder(DefMethTest testInstance, int minMajorVer, int accFlags,
                boolean redefineClasses, boolean retransformClasses, ExecutionMode executionMode) {
        this.testInstance = testInstance;
        this.minMajorVer = minMajorVer;
        this.accFlags = accFlags;
        this.redefineClasses = redefineClasses;
        this.retransformClasses = retransformClasses;
        this.executionMode = executionMode;
    }

    /**
     * Factory method for Interface builder.
     *
     * @param name
     * @return class builder
     */
    public InterfaceBuilder intf(String name) {
        InterfaceBuilder b = new InterfaceBuilder(this).name(name);
        underConstruction.add(b);
        return b;
    }

    /**
     * Factory method for Clazz builder.
     *
     * @param name
     * @return class builder
     */
    public ConcreteClassBuilder clazz(String name) {
        ConcreteClassBuilder b = new ConcreteClassBuilder(this).name(name).ver(minMajorVer);
        underConstruction.add(b);
        return b;
    }

    /**
     * Factory method for Tester builder.
     *
     * @return test builder
     */
    public TesterBuilder test() {
        TesterBuilder b = new TesterBuilder(++testNo, this);
        underConstruction.add(b);
        return b;
    }

    /**
     * Find previously constructed class by it's name.
     *
     * The method is considered safe: if it fails to find a class, it throws
     * IllegalArgumentException.
     *
     * @param name
     * @return
     */
    public Clazz lookup(String name) {
        for (Clazz clz : elements) {
            if (clz.name().equals(name)) {
                return clz;
            }
        }

        throw new IllegalArgumentException("Unknown element: " + name);
    }

    /**
     * Lazy binding of {@code Clazz} instance
     *
     * @param name
     * @return
     */
    public ConcreteClass clazzByName(String name) {
        return new ConcreteClassLazyAdapter(this, name);
    }

    /**
     * Lazy binding of {@code Interface} instance
     *
     * @param name
     * @return
     */
    public Interface intfByName(String name) {
        return new InterfaceLazyAdapter(this, name);
    }

    /**
     * Construct corresponding {@code Clazz} instance for a {@code Class}.
     *
     * @param cls
     * @return
     */
    public Clazz toClazz(Class cls) {
        String name = cls.getName();

        if (hasElement(name)) {
            return lookup(name);
        } else {
            return clazz(name).build();
        }
    }

    /**
     * Construct corresponding {@code ConcreteClass} instance for a {@code Class}.
     *
     * Throws {@code IllegalArgumentException} if {@code Class} can't be
     * represented as {@code ConcreteClass}
     *
     * @param cls
     * @return
     */
    public ConcreteClass toConcreteClass(Class cls) {
        if (!cls.isInterface()) {
            return (ConcreteClass)toClazz(cls);
        } else {
            throw new IllegalArgumentException(cls.getName());
        }
    }

    /**
     * Factory method for Method builder.
     *
     * @return  method builder
     */
    /* package-private */ MethodBuilder method() {
        return new MethodBuilder(this);
    }

    /**
     * Factory method for Data.DefaultMethod builder.
     *
     * @param name method name
     * @return
     */
    public MethodBuilder defaultMethod(String name) {
        return method().name(name).type(MethodType.DEFAULT);
    }

    /**
     * Factory method for Data.AbstractMethod builder.
     *
     * @param name
     * @return
     */
    public MethodBuilder abstractMethod(String name) {
        return method().name(name).type(MethodType.ABSTRACT);
    }

    /**
     * Factory method for Data.ConcreteMethod builder.
     *
     * @param name
     * @return
     */
    public MethodBuilder concreteMethod(String name) {
        return method().name(name).type(MethodType.CONCRETE);
    }

    /**
     * Signal that {@code Builder<?>} instance won't be used anymore.
     *
     * @param builder
     */
    /* package-private */ void finishConstruction(Builder<?> builder) {
        if (underConstruction.contains(builder)) {
            underConstruction.remove(builder);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Register class with the test builder, so it'll be enumerated during
     * hierarchy traversals (e.g. class file generation, hierarchy printing).
     *
     * @param clz
     * @return
     */
    public TestBuilder register(Clazz clz) {
        elements.add(clz);
        return this;
    }

    /**
     * Register class with the test builder as a test, so it'll be enumerated during
     * hierarchy traversals and executed during test runs.
     *
     * @param test
     * @return
     */
    public TestBuilder register(Tester test) {
        tests.add(test);
        return this;
    }

    /**
     * Check whether a class with some name has been already constructed.
     *
     * @param name
     * @return whether a class with the same name has been already created
     */
    /* package-private */ boolean hasElement(String name) {
        for (Clazz clz : elements) {
            if (clz.name().equals(name)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Create all classes and return a class loader which can load them.
     *
     * @return class loader instance which loads all generated classes
     */
    public MemoryClassLoader build() {
        Map<String,byte[]> classes = produce();

        MemoryClassLoader cl = new MemoryClassLoader(classes);

        return cl;
    }

    /**
     * Produce class files for a set of {@code Clazz} instances.
     *
     * @return
     */
    public Map<String,byte[]> produce() {
        if (!underConstruction.isEmpty()) {
            throw new IllegalStateException("Some of the classes haven't been fully constructed");
        }

        List<Clazz> items = new ArrayList<>();
        items.addAll(elements);
        items.addAll(tests);

        return produce(52, items);
    }

    /**
     * Produce class files for {@Clazz} instances from {@code elements}.
     *
     * @param defaultMajorVer
     * @param elements
     * @return
     */
    private Map<String,byte[]> produce(int defaultMajorVer, List<? extends Clazz> elements) {
        LinkedHashMap<String,byte[]> classes = new LinkedHashMap<>();

        if (Constants.PRINT_TESTS) {
            System.out.printf("\nTEST: %s\n\n", Util.getTestName());
        }

        for (Clazz clazz : elements) {
            if (Constants.PRINT_TESTS) {
                System.out.println(Printer.print(clazz));
            }

            if (clazz instanceof Tester &&
                (executionMode == ExecutionMode.REFLECTION ||
                 executionMode == ExecutionMode.INVOKE_WITH_ARGS)) {
                // No need to generate testers for reflection cases
                continue;
            }

            Pair<String,byte[]> p = produceClassFile(defaultMajorVer, executionMode, clazz);
            classes.put(p.first, p.second);
        }

        if (Constants.PRINT_ASSEMBLY) {
            System.out.println("\nDISASSEMBLY");

            for (byte[] cf : classes.values()) {
                Util.printClassFile(cf);
                System.out.println();
            }
        }

        if (Constants.ASMIFY) {
            System.out.println("\nASM");

            for (byte[] cf : classes.values()) {
                Util.asmifyClassFile(cf);
                System.out.println();
            }
        }

        if (Constants.DUMP_CLASSES) {
            try {
                File dumpDir = new File("DUMP_CLASS_FILES", testInstance.shortTestName);
                if (!dumpDir.exists()) {
                    dumpDir.mkdirs();
                }

                for (Map.Entry<String,byte[]> entry : classes.entrySet()) {
                    String name = entry.getKey();
                    byte[] classFile = entry.getValue();
                    File dumpFile = new File(dumpDir, name+".class");
                    dumpFile.getParentFile().mkdirs();
                    try (FileOutputStream file = new FileOutputStream(dumpFile)) {
                        file.write(classFile);
                    }
                }
            } catch (Exception e) {
                throw new Error(e);
            }
        }

        return classes;
    }

    /**
     * Produce class file from {@code Clazz} instance.
     *
     * @param defaultMajorVer
     * @param clazz
     * @return
     */
    public static Pair<String,byte[]> produceClassFile(int defaultMajorVer,
                                                       ExecutionMode execMode, Clazz clazz) {
        int majorVer = clazz.ver() != 0 ? clazz.ver() : defaultMajorVer;

        ClassFileGenerator cfg = new ClassFileGenerator(majorVer, ACC_PUBLIC, execMode);
        clazz.visit(cfg);

        byte[] classFile = cfg.getClassFile();

        return Pair.of(clazz.name(), classFile);
    }

    /**
     * Make all preparations for execution.
     *
     * @return
     */
    public TestExecutor prepare() {
        return prepare(build());
    }

    private TestExecutor prepare(MemoryClassLoader cl) {
        if (redefineClasses) {
            try {
                cl.modifyClasses(/* retransform = */ false);
            } catch (TestFailure e) {
                testInstance.getLog().info(e.getMessage());
                throw e;
            }
        }

        if (retransformClasses) {
            try {
                cl.modifyClasses(/* redefine = */ true);
            } catch (TestFailure e) {
                testInstance.getLog().info(e.getMessage());
                throw e;
            }
        }

        switch (executionMode) {
            case DIRECT:
            case INDY:
            case INVOKE_EXACT:
            case INVOKE_GENERIC:
                // Run tests using direct invocation methods
                return new GeneratedTest(cl, testInstance, tests);
            case REFLECTION:
                // Use reflection for testing
                return new ReflectionTest(cl, this, testInstance, tests);
            case INVOKE_WITH_ARGS:
                return new MHInvokeWithArgsTest(cl, testInstance, tests);
            default:
                throw new Error("Unknown execution mode: " + executionMode);
        }
    }

    public interface LoaderConstructor {
        public MemoryClassLoader construct(Map< String,byte[]> classFiles);
    }

    /**
     * Customize class loader construction.
     *
     * @param constructLoader
     * @return
     */
    public TestExecutor prepare(LoaderConstructor constructLoader) {
        return prepare(constructLoader.construct(produce()));
    }

    /**
     * Construct a test with all necessary classes and execute all tests
     * from it.
     */
    public void run() {
        if (!underConstruction.isEmpty()) {
            throw new InternalError("Still under construction");
        }
        if (tests.isEmpty()) {
            throw new InternalError("No tests to run");
        }

        TestExecutor executor = prepare();

        List<Pair<Tester,Throwable>> errors = executor.run();

        if (!errors.isEmpty()) {
            throw new DefMethTestFailure(errors);
        }

    }
}
