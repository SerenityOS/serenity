/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
package gc.g1.unloading.loading;

import gc.g1.unloading.ExecutionTask;
import gc.g1.unloading.bytecode.*;
import gc.g1.unloading.check.Assertion;
import gc.g1.unloading.check.ClassAssertion;
import gc.g1.unloading.check.PhantomizedAssertion;

import gc.g1.unloading.check.FinalizedAssertion;
import gc.g1.unloading.check.PhantomizationServiceThread;
import gc.g1.unloading.check.cleanup.UnusedThreadKiller;
import gc.g1.unloading.classloaders.DoItYourselfClassLoader;
import gc.g1.unloading.classloaders.FinalizableClassloader;
import gc.g1.unloading.classloaders.JNIClassloader;
import gc.g1.unloading.classloaders.ReflectionClassloader;
import gc.g1.unloading.configuration.ClassloadingMethod;
import gc.g1.unloading.configuration.KeepRefMode;
import gc.g1.unloading.configuration.TestConfiguration;
import gc.g1.unloading.keepref.*;
import nsk.share.test.ExecutionController;
import sun.hotspot.WhiteBox;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;

import java.lang.ref.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;

/**
 * This helper performs dirty job: loads classes, instantiate objects, performs redefinition etc...
 */
public class ClassLoadingHelper {

    private static final int NATIVE_VERBOSITY = 2;

    private static final Object[] NO_CP_PATCHES = new Object[0];

    private static BytecodeFactory bf;

    private ExecutionController executionController;

    private PhantomizationServiceThread phantomizationServiceThread;

    private Random random;

    private TestConfiguration configuration;

    // This should be TRUE for bytecode generators that use ASM to create the bytecodes
    // instead of creating Java source and then compiling with InMemoryJavaCompiler.
    private boolean prepend_package = true;

    /**
     * Constructor that creates instance of helper. All arguments are self-explaining.
     * @param executionController
     * @param randomSeed
     * @param testConfiguration
     */
    public ClassLoadingHelper(ExecutionController executionController,
                              long randomSeed, TestConfiguration testConfiguration) {
        random = new Random(randomSeed);
        this.executionController = executionController;
        this.configuration = testConfiguration;

        phantomizationServiceThread = new PhantomizationServiceThread(executionController);
        Thread thread = new Thread(phantomizationServiceThread);
        thread.setDaemon(true);
        thread.start();

        if (configuration.isInMemoryCompilation() && !configuration.isHumongousClass() && !(configuration.getKeepRefMode() == KeepRefMode.THREAD_ITSELF)) {
            prepend_package = false;
            bf = new BytecodeGeneratorFactory(random.nextLong());
        } else {
            if (configuration.isHumongousClass()) {
                bf = new BytecodeMutatorFactory(HumongousTemplateClass.class.getName());
            } else if (configuration.getKeepRefMode() == KeepRefMode.THREAD_ITSELF) {
                bf = new BytecodeMutatorFactory(ThreadTemplateClass.class.getName());
            } else {
                bf = new BytecodeMutatorFactory();
            }
        }
    }

    /**
     * Load class that's supposed to live. Method returns collection of assertions to check it will live.
     * @param className_
     * @return
     */
    public Collection<Assertion> loadClassThatGonnaLive(String className_) {
        // if generating the bytecodes using ASM then prepend the package name to
        // the classname so that, when created as a hidden class, it will be in the
        // same package as its lookup class.
        Bytecode kit = bf.createBytecode(prepend_package ?
                                         "gc.g1.unloading.loading." + className_ : className_);
        String className = kit.getClassName();
        byte[] bytecode = kit.getBytecode();
        Class<?> clazz = loadClass(className, bytecode);
        Object object = instantiateObject(clazz);
        Object referenceToKeep = configuration.getWhatToKeep().decideUponRefToKeep(clazz, clazz.getClassLoader(), object);

        redefineIfNeeded(bytecode, clazz);

        warmUpClassIfNeeded(object);
        Assertion assertion;
        assertion = new ClassAssertion(className, true);

        switch (configuration.getKeepRefMode()) {
            case STRONG_REFERENCE:
                assertion.keepLink(referenceToKeep);
                break;
            case SOFT_REFERENCE:
                assertion.keepLink(new SoftReference<Object>(referenceToKeep));
                break;
            case STATIC_FIELD:
                RefHolder holder1 = new InStaticFieldHolder();
                assertion.keepLink(holder1.hold(referenceToKeep));
                break;
            case STACK_LOCAL:
                RefHolder holder2 = new InStackLocalHolder(); // UnusedThreadKiller
                assertion.keepLink(holder2.hold(referenceToKeep));
                break;
            case THREAD_FIELD:
                RefHolder holder3 = new InThreadFieldHolder(); // UnusedThreadKiller
                assertion.keepLink(holder3.hold(referenceToKeep));
                break;
            case THREAD_ITSELF:
                Thread objectThread = (Thread) object;
                objectThread.setDaemon(true);
                objectThread.start();
                assertion.keepLink(new UnusedThreadKiller(objectThread.getId())); // UnusedThreadKiller
                break;
            case STATIC_FIELD_OF_ROOT_CLASS:
                RefHolder holder4 = new NullClassloaderHolder(random.nextLong());
                Object keep = holder4.hold(referenceToKeep);
                if (keep != null) {
                    assertion.keepLink(keep);
                }
                break;
            case JNI_GLOBAL_REF:
                JNIGlobalRefHolder holder5 = new JNIGlobalRefHolder();
                assertion.keepLink(holder5.hold(referenceToKeep));
                break;
            case JNI_LOCAL_REF:
                JNILocalRefHolder holder6 = new JNILocalRefHolder();
                assertion.keepLink(holder6.hold(referenceToKeep));
                break;
        }

        Collection<Assertion> returnValue = new LinkedList<>();
        returnValue.add(assertion);
        return returnValue;
    }

    /**
     * Load class that's supposed to be unloaded. Method returns collection of assertions to check it will be unloaded.
     * @param className_
     * @return
     */
    public Collection<Assertion> loadClassThatGonnaDie(String className_) {
        Collection<Assertion> returnValue = new LinkedList<>();
        // if generating the bytecodes using ASM then prepend the package name to
        // the classname so that, when created as a hidden class, it will be in the
        // same package as its lookup class.
        Bytecode kit = bf.createBytecode(prepend_package ?
                                         "gc.g1.unloading.loading." + className_ : className_);
        String className = kit.getClassName();
        byte[] bytecode = kit.getBytecode();
        Class<?> clazz = loadClass(className, bytecode);
        FinalizableClassloader cl = null;
        if (clazz.getClassLoader() instanceof FinalizableClassloader) {
            cl = (FinalizableClassloader) clazz.getClassLoader();
        }
        Object object = instantiateObject(clazz);
        Object referenceToKeep = configuration.getWhatToKeep().decideUponRefToKeep(clazz, clazz.getClassLoader(), object);

        redefineIfNeeded(bytecode, clazz);

        warmUpClassIfNeeded(object);
        Assertion assertion;
        assertion = new ClassAssertion(className, false);
        switch (configuration.getReleaseRefMode()) {
            case WEAK:
                assertion.keepLink(new WeakReference<Object>(referenceToKeep));
                break;
            case PHANTOM:
                final ReferenceQueue queue = new ReferenceQueue<Object>();
                assertion.keepLink(new PhantomReference<Object>(referenceToKeep, queue));
                new Thread(new ReferenceCleaningThread(executionController, queue)).start();
                break;
        }
        returnValue.add(assertion);

        if (cl != null) {
            // Check that classloader will be finalized
            FinalizedAssertion finalizedAssertion = new FinalizedAssertion();
            cl.setFinalizedAssertion(finalizedAssertion);
            returnValue.add(finalizedAssertion);

            // Check that classloader will be phantomized
            PhantomizedAssertion phantomizedAssertion = new PhantomizedAssertion();
            PhantomReference phantomReference = new PhantomReference<Object>(cl, phantomizationServiceThread.getQueue());
            phantomizationServiceThread.add(phantomReference, phantomizedAssertion);
            returnValue.add(phantomizedAssertion);
        }
        return returnValue;
    }

    private void redefineIfNeeded(byte[] bytecode, Class<?> clazz) {
        if (configuration.isRedefineClasses()) {
            BytecodePatcher.patch(bytecode);
            makeRedefinition(NATIVE_VERBOSITY, clazz, bytecode);

            // This will call class's method
            instantiateObject(clazz);
        }
    }

    private Class<?> loadClass(String className, byte[] bytecode) {
        try {
            switch (configuration.getClassloadingMethod()) {
                case PLAIN:
                    DoItYourselfClassLoader loader1 = new DoItYourselfClassLoader();
                    return loader1.defineClass(className, bytecode);
                case REFLECTION:
                    return Class.forName(className, true, new ReflectionClassloader(bytecode, className));
                case JNI:
                    return JNIClassloader.loadThroughJNI(className, bytecode);
                case HIDDEN_CLASSLOADER:
                    Lookup lookup = MethodHandles.lookup();
                    return lookup.defineHiddenClass(bytecode, true).lookupClass();
            }
            return null;
        } catch (ClassNotFoundException | IllegalAccessException e) {
            throw new RuntimeException("Test bug!", e);
        }
    }

    private Object instantiateObject(Class<?> clazz) {
        try {
            Object object = clazz.newInstance();

            // Call method just for fun
            for (Method m : clazz.getMethods()) {
                if (m.getName().equals("main")) {
                    m.invoke(object);
                }
            }
            return object;
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException("Test bug!", e);
        }
    }

    private void warmUpClassIfNeeded(Object object) {
        if (configuration.getCompilationLevel() < 1 || configuration.getCompilationNumber() == 0) {
            return;
        }
        Method m = null;
        for (Method method : object.getClass().getMethods()) {
            if (method.getName().equalsIgnoreCase("methodForCompilation")) {
                m = method;
            }
        }
        WhiteBox wb = WhiteBox.getWhiteBox();
        if (!wb.isMethodCompilable(m)) {
            throw new RuntimeException("Test bug! Method occured to be not compilable. Requires investigation.");
        }

        for (int i = configuration.getCompilationNumber(); i >= 0 && executionController.continueExecution(); i--) {
            if (!wb.isMethodCompilable(m, configuration.getCompilationLevel())) {
              continue;
            }
            if (!wb.enqueueMethodForCompilation(m, configuration.getCompilationLevel())) {
                throw new RuntimeException("Method could not be enqueued for compilation at level " + configuration.getCompilationLevel());
            }
            while (!wb.isMethodCompiled(m) && executionController.continueExecution()) {
                sleep(50);
                try {
                    m.invoke(object, new Object());
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    throw new RuntimeException("Something went wrong while compilation", e);
                }
            }
            if (i > 0) {
                wb.deoptimizeMethod(m);
            }
        }
    }

    native static int makeRedefinition0(int verbose, Class<?> redefClass, byte[] classBytes);

    private static void makeRedefinition(int verbose, Class<?> redefClass, byte[] classBytes) {
        new LibLoader().hashCode();
        if (makeRedefinition0(verbose, redefClass, classBytes) != 0) {
            throw new RuntimeException("Test bug: native method \"makeRedefinition\" return nonzero");
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException("Got InterruptedException while sleeping.", e);
        }
    }

}

class ReferenceCleaningThread extends ExecutionTask {

    private ReferenceQueue<?> queue;

    public ReferenceCleaningThread(ExecutionController executionController, ReferenceQueue<?> queue) {
        super(executionController);
        this.queue = queue;
    }

    @Override
    protected void task() throws Exception {
        Reference<?> ref = queue.remove(100);
        if (ref != null) {
            ref.clear();
            return;
        }
    }

}
