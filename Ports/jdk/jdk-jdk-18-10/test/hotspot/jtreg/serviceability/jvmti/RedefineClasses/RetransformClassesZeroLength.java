/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8198393
 * @summary Instrumentation.retransformClasses(new Class[0]) should be NOP
 * @requires vm.jvmti
 * @requires vm.flagless
 * @library /test/lib
 * @modules java.instrument
 * @compile RetransformClassesZeroLength.java
 * @run driver RetransformClassesZeroLength
 */

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.helpers.ClassFileInstaller;

public class RetransformClassesZeroLength {

    private static String manifest =
            "Premain-Class: " + RetransformClassesZeroLength.Agent.class.getName() + "\n"
            + "Can-Retransform-Classes: true\n";

    private static String CP = System.getProperty("test.classes");

    public static void main(String args[]) throws Throwable {
        String agentJar = buildAgent();
        ProcessTools.executeProcess(
                ProcessTools.createJavaProcessBuilder(
                        "-javaagent:" + agentJar,
                        "-version")
        ).shouldHaveExitValue(0);
    }

    private static String buildAgent() throws Exception {
        Path jar = Files.createTempFile(Paths.get("."), null, ".jar");
        String jarPath = jar.toAbsolutePath().toString();
        ClassFileInstaller.writeJar(jarPath,
                ClassFileInstaller.Manifest.fromString(manifest),
                RetransformClassesZeroLength.class.getName());
        return jarPath;
    }


    public static class Agent implements ClassFileTransformer {
        public static void premain(String args, Instrumentation inst) {
            inst.addTransformer(new NoOpTransformer());
            try {
                inst.retransformClasses(new Class[0]);
            } catch (UnmodifiableClassException ex) {
                throw new AssertionError(ex);
            }
        }
    }

    private static class NoOpTransformer implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader,
                                String className,
                                Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain,
                                byte[] classfileBuffer
                                ) throws IllegalClassFormatException {
            return null;    // no transform
        }
    }
}
