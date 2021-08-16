/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8008678
 * @summary JSR 292: constant pool reconstitution must support pseudo strings
 * @requires vm.jvmti
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.instrument
 *          java.management
 *          jdk.jartool/sun.tools.jar
 * @compile -XDignore.symbol.file TestLambdaFormRetransformation.java
 * @run driver TestLambdaFormRetransformation
 */

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.Arrays;

import jdk.test.lib.process.ExitCode;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestLambdaFormRetransformation {
    private static String MANIFEST = String.format("Manifest-Version: 1.0\n" +
                                                   "Premain-Class: %s\n" +
                                                   "Can-Retransform-Classes: true\n",
                                                   Agent.class.getName());

    private static String CP = System.getProperty("test.classes");

    public static void main(String args[]) throws Throwable {
        Path agent = TestLambdaFormRetransformation.buildAgent();
        OutputAnalyzer oa = ProcessTools.executeTestJvm("-javaagent:" +
                                agent.toAbsolutePath().toString(), "-version");
        oa.shouldHaveExitValue(ExitCode.OK.value);
    }

    private static Path buildAgent() throws IOException {
        Path manifest = TestLambdaFormRetransformation.createManifest();
        Path jar = Files.createTempFile(Paths.get("."), null, ".jar");

        String[] args = new String[] {
            "-cfm",
            jar.toAbsolutePath().toString(),
            manifest.toAbsolutePath().toString(),
            "-C",
            TestLambdaFormRetransformation.CP,
            Agent.class.getName() + ".class"
        };

        sun.tools.jar.Main jarTool = new sun.tools.jar.Main(System.out, System.err, "jar");

        if (!jarTool.run(args)) {
            throw new Error("jar failed: args=" + Arrays.toString(args));
        }
        return jar;
    }

    private static Path createManifest() throws IOException {
        Path manifest = Files.createTempFile(Paths.get("."), null, ".mf");
        byte[] manifestBytes = TestLambdaFormRetransformation.MANIFEST.getBytes();
        Files.write(manifest, manifestBytes);
        return manifest;
    }
}

class Agent implements ClassFileTransformer {
    private static Runnable lambda = () -> {
        System.out.println("I'll crash you!");
    };

    public static void premain(String args, Instrumentation instrumentation) {
        if (!instrumentation.isRetransformClassesSupported()) {
            System.out.println("Class retransformation is not supported.");
            return;
        }
        System.out.println("Calling lambda to ensure that lambda forms were created");

        Agent.lambda.run();

        System.out.println("Registering class file transformer");

        instrumentation.addTransformer(new Agent());

        for (Class c : instrumentation.getAllLoadedClasses()) {
            if (c.getName().contains("LambdaForm") &&
                instrumentation.isModifiableClass(c)) {
                System.out.format("We've found a modifiable lambda form: %s%n", c.getName());
                try {
                    instrumentation.retransformClasses(c);
                } catch (UnmodifiableClassException e) {
                    throw new AssertionError("Modification of modifiable class " +
                                             "caused UnmodifiableClassException", e);
                }
            }
        }
    }

    public static void main(String args[]) {
    }

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer
                           ) throws IllegalClassFormatException {
        System.out.println("Transforming " + className);
        return classfileBuffer.clone();
    }
}
