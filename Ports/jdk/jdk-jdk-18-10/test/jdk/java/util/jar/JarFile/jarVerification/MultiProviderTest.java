/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8234466
 * @summary attempt to trigger class loading from the classloader
 * during JAR file verification
 * @library /test/lib
 * @build jdk.test.lib.compiler.CompilerUtils
 *        jdk.test.lib.process.*
 *        jdk.test.lib.util.JarUtils
 *        jdk.test.lib.JDKToolLauncher
 *        MultiThreadLoad FooService
 * @modules java.base/jdk.internal.access:+open
 * @run main MultiProviderTest
 * @run main MultiProviderTest sign
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.Utils;
import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.util.JarUtils;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Arrays.asList;

public class MultiProviderTest {

    private static final String METAINFO = "META-INF/services/FooService";
    private static String COMBO_CP = Utils.TEST_CLASS_PATH + File.pathSeparator;
    private static String TEST_CLASS_PATH = System.getProperty("test.classes", ".");
    private static boolean signJars = false;
    static final int NUM_JARS = 5;


    private static final String KEYSTORE = "keystore.jks";
    private static final String ALIAS = "JavaTest";
    private static final String STOREPASS = "changeit";
    private static final String KEYPASS = "changeit";

    public static void main(String[] args) throws Throwable {
        signJars = args.length >=1 && args[0].equals("sign");
        initialize();
        List<String> cmds = new ArrayList<>();
        cmds.add(JDKToolFinder.getJDKTool("java"));
        cmds.addAll(asList(Utils.getTestJavaOpts()));
        cmds.addAll(List.of(
                "-cp",
                COMBO_CP,
                "--add-opens",
                "java.base/jdk.internal.access=ALL-UNNAMED",
                "-Djava.util.logging.config.file=" +
                Path.of(System.getProperty("test.src", "."), "logging.properties").toString(),
                "MultiThreadLoad",
                TEST_CLASS_PATH));

        try {
            OutputAnalyzer outputAnalyzer = ProcessTools.executeCommand(cmds.stream()
                    .filter(t -> !t.isEmpty())
                    .toArray(String[]::new))
                    .shouldHaveExitValue(0);
            System.out.println("Output:" + outputAnalyzer.getOutput());
        } catch (Throwable t) {
            throw new RuntimeException("Unexpected fail.", t);
        }
    }

    public static void initialize() throws Throwable {
        if (signJars) {
            genKey();
        }
        for (int i = 0; i < NUM_JARS; i++) {
            String p = "FooProvider" + i;
            String jarName = "FooProvider" + i + ".jar";
            Path javaPath = Path.of(p + ".java");
            Path jarPath = Path.of(p + ".jar");
            String contents = "public class FooProvider" + i + " extends FooService { }";
            Files.write(javaPath, contents.getBytes());
            CompilerUtils.compile(javaPath, Path.of(System.getProperty("test.classes")), "-cp", Utils.TEST_CLASS_PATH);
            createJar(jarPath, p, List.of(p));
            if (signJars) {
                signJar(TEST_CLASS_PATH + File.separator + jarName);
            }
            COMBO_CP += TEST_CLASS_PATH + File.separator + jarName + File.pathSeparator;
        }
    }

    private static void createProviderConfig(Path config, String providerName) throws Exception {
        Files.createDirectories(config.getParent());
        Files.write(config, providerName.getBytes(), CREATE);
    }

    private static void createJar(Path jar, String provider, List<String> files) throws Exception {
        Path xdir = Path.of(provider);
        createProviderConfig(xdir.resolve(METAINFO), provider);

        for (String f : files) {
            Path source = Path.of(Utils.TEST_CLASSES, f + ".class");
            Path target = xdir.resolve(source.getFileName());
            Files.copy(source, target, REPLACE_EXISTING);
        }
        JarUtils.createJarFile(Path.of(TEST_CLASS_PATH, jar.getFileName().toString()), xdir);
    }

    private static void genKey() throws Throwable {
        String keytool = JDKToolFinder.getJDKTool("keytool");
        Files.deleteIfExists(Paths.get(KEYSTORE));
        ProcessTools.executeCommand(keytool,
                "-J-Duser.language=en",
                "-J-Duser.country=US",
                "-genkey",
                "-keyalg", "rsa",
                "-alias", ALIAS,
                "-keystore", KEYSTORE,
                "-keypass", KEYPASS,
                "-dname", "cn=sample",
                "-storepass", STOREPASS
        ).shouldHaveExitValue(0);
    }


    private static OutputAnalyzer signJar(String jarName) throws Throwable {
        List<String> args = new ArrayList<>();
        args.add("-verbose");
        args.add(jarName);
        args.add(ALIAS);

        return jarsigner(args);
    }

    private static OutputAnalyzer jarsigner(List<String> extra)
            throws Throwable {
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("jarsigner")
                .addVMArg("-Duser.language=en")
                .addVMArg("-Duser.country=US")
                .addToolArg("-keystore")
                .addToolArg(KEYSTORE)
                .addToolArg("-storepass")
                .addToolArg(STOREPASS)
                .addToolArg("-keypass")
                .addToolArg(KEYPASS);
        for (String s : extra) {
            if (s.startsWith("-J")) {
                launcher.addVMArg(s.substring(2));
            } else {
                launcher.addToolArg(s);
            }
        }
        return ProcessTools.executeCommand(launcher.getCommand());
    }

}

