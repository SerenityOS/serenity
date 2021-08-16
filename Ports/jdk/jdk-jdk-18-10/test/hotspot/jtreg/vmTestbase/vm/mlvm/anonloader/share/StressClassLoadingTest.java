/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

package vm.mlvm.anonloader.share;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import nsk.share.test.Stresser;
import vm.share.options.Option;
import vm.share.options.OptionSupport;
import vm.share.options.IgnoreUnknownArgumentsHandler;
import vm.mlvm.share.Env;
import vm.mlvm.share.MlvmTest;
import vm.mlvm.share.CustomClassLoaders;
import vm.share.FileUtils;
import vm.share.UnsafeAccess;

/**
 * Does stress-testing of class loading subsystem.
 * This class should be subclassed by the tests
 * to provide test class data.
 *
 * <p>StressClassLoadingTest performs a number of iterations
 * (the default value is 100 000).
 * Each iteration gets class bytes from the subclass
 * and loads it into JVM using either:
 * <ul>
 *    <li>a custom {@link java.lang.ClassLoader} implementation or
 *    <li>{@link java.lang.invoke.MethodHandles.Lookup#defineHiddenClass} call.
 * </ul>
 *
 * <p>Loading is done in a separate thread. If this thread is stuck,
 * it is killed after some timeout (default is 10 seconds, please see
 * -parseTimeout option). The class file is saved as hangXX.class, where XX
 * starts at 00 and is increased on every hangup.
 * A prefix can be added to the file name using {@link #setFileNamePrefix}
 *
 * <p>The test fails, if there were hangups.
 *
 * <p>By default, before loading class, the bytes are
 * saved to {@code _AnonkTestee01.class} file in the current directory.
 * If JVM crashes, the bytecodes can be analysed.
 * Class saving is controlled by -saveClassFile option.
 * A prefix can be added to the file name using {@link #setFileNamePrefix}
 * function.
 */
public abstract class StressClassLoadingTest extends MlvmTest {
    private static final String RESCUE_FILE_NAME = "_AnonkTestee01.class";
    private static final String HUNG_CLASS_FILE_NAME = "hang.class";

    @Option(name = "iterations", default_value = "100000",
            description = "How many times generate a class and parse it")
    private static int iterations;

    @Option(name = "saveClassFile", default_value = "true",
            description = "Save generated class file before loading."
                    + " Useful when VM crashes on loading")
    private static boolean saveClassFile;

    @Option(name = "parseTimeout", default_value = "10000",
            description = "Timeout in millisectionds to detect hung parser"
                    + " thread. The parser thread is killed after the timeout")
    private static int parseTimeout;

    @Option(name = "hiddenLoad", default_value = "false",
            description = "An option for adhoc experiments: load class as a hidden class.")
    private static boolean hiddenLoad;

    private String fileNamePrefix = "";

    private final static AtomicBoolean classFileMessagePrinted
            = new AtomicBoolean(false);

    /**
     * Sets prefix for names of the files, created by test:
     * _AnonkTestee01.class and hangXX.class.
     *
     * @param p a prefix to add before file name.
     * @throws java.lang.NullPointerException if p is null
     */
    public void setFileNamePrefix(String p) {
        Objects.requireNonNull(p);
        fileNamePrefix = p;
    }

    static volatile boolean optionsSetup = false;
    public static void setupOptions(Object instance) {
        if (!optionsSetup) {
            synchronized (StressClassLoadingTest.class) {
                if (!optionsSetup) {
                    OptionSupport.setup(instance, Env.getArgParser().getRawArguments(), new IgnoreUnknownArgumentsHandler());
                    optionsSetup = true;

                    Env.traceNormal("StressClassLoadingTest options: iterations: " + iterations);
                    Env.traceNormal("StressClassLoadingTest options: hiddenLoad: " + hiddenLoad);
                    Env.traceNormal("StressClassLoadingTest options: parseTimeout: " + parseTimeout);
                    Env.traceNormal("StressClassLoadingTest options: saveClassFile: " + saveClassFile);
                }
            }
        }
    }

    public boolean run() throws Exception {
        setupOptions(this);

        Stresser stresser = createStresser();
        stresser.start(iterations);

        while (stresser.continueExecution()) {
            stresser.iteration();

            byte[] classBytes = generateClassBytes();
            Class<?> hostClass = getHostClass();
            String className = hostClass.getName();
            File rescueFile = new File(String.format("%s_%d_%s",
                    fileNamePrefix, stresser.getIteration(), RESCUE_FILE_NAME));
            if (saveClassFile) {
                // Write out the class file being loaded.  It's useful
                // to have if the JVM crashes.
                FileUtils.writeBytesToFile(rescueFile, classBytes);
                if (classFileMessagePrinted.compareAndSet(false, true)) {
                    Env.traceImportant("If the JVM crashes then "
                            + "the class file causing the crash is saved as *_*_"
                            + RESCUE_FILE_NAME);
                }
            }

            Thread parserThread  = new Thread() {
                public void run() {
                    try {
                        Class<?> c;
                        if (hiddenLoad) {
                            Lookup lookup = MethodHandles.lookup();
                            c = lookup.defineHiddenClass(classBytes, true).lookupClass();

                        } else {
                            c = CustomClassLoaders.makeClassBytesLoader(classBytes, className)
                                    .loadClass(className);
                        }
                        UnsafeAccess.unsafe.ensureClassInitialized(c);
                    } catch (Throwable e) {
                        Env.traceVerbose(e, "parser caught exception");
                    }
                }
            };

            parserThread.start();
            parserThread.join(parseTimeout);

            if (parserThread.isAlive()) {
                Env.traceImportant("parser thread may be hung!");
                StackTraceElement[] stack = parserThread.getStackTrace();
                Env.traceImportant("parser thread stack len: " + stack.length);
                Env.traceImportant(parserThread + " stack trace:");
                for (int i = 0; i < stack.length; ++i) {
                    Env.traceImportant(parserThread + "\tat " + stack[i]);
                }

                Path savedClassPath = Paths.get(fileNamePrefix + HUNG_CLASS_FILE_NAME);

                if (saveClassFile) {
                    Files.move(rescueFile.toPath(), savedClassPath);
                    Env.traceImportant("There was a possible hangup during parsing."
                        + " The class file, which produced the possible hangup, was saved as "
                        + fileNamePrefix + HUNG_CLASS_FILE_NAME
                        + "... in the test directory. You may want to analyse it "
                        + "if this test times out.");
                }

                parserThread.join(); // Wait until either thread finishes or test times out.
                if (saveClassFile) {
                    savedClassPath.toFile().delete();
                }
            } else if (saveClassFile) {
                rescueFile.delete();
            }
        }

        stresser.finish();
        return true;
    }

    /**
     * Generated class bytes. The method is called for each iteration.
     *
     * @return Byte array with the generated class
     */
    protected abstract byte[] generateClassBytes();

    /**
     * Returns a host class for the generated class.
     *
     * @return A host class that for the generated class
     */
    protected abstract Class<?> getHostClass();
}
