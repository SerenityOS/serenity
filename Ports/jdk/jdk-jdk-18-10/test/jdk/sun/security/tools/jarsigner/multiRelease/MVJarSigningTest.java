/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8047305 8075618
 * @summary Tests jarsigner tool and JarSigner API work with multi-release JAR files.
 * @library /test/lib
 * @build jdk.test.lib.compiler.CompilerUtils
 *        jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 * @run main MVJarSigningTest
 */

import jdk.security.jarsigner.JarSigner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.Utils;
import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;


public class MVJarSigningTest {

    private static final String TEST_SRC = System.getProperty("test.src", ".");
    private static final String USR_DIR = System.getProperty("user.dir", ".");
    private static final String JAR_NAME = "MV.jar";
    private static final String KEYSTORE = "keystore.jks";
    private static final String ALIAS = "JavaTest";
    private static final String STOREPASS = "changeit";
    private static final String KEYPASS = "changeit";
    private static final String SIGNED_JAR = "Signed.jar";
    private static final String POLICY_FILE = "SignedJar.policy";
    private static final String VERSION = Integer.toString(10);
    private static final String VERSION_MESSAGE = "I am running on version " + VERSION;

    public static void main(String[] args) throws Throwable {
        // compile java files in jarContent directory
        compile("jarContent");

        // create multi-release jar
        Path classes = Paths.get("classes");
        jar("cf", JAR_NAME, "-C", classes.resolve("base").toString(), ".",
                "--release", "9", "-C", classes.resolve("v9").toString(), ".",
                "--release", "10", "-C", classes.resolve("v10").toString(), ".")
            .shouldHaveExitValue(0);

        genKey();
        signJar(JAR_NAME)
            .shouldHaveExitValue(0)
            .shouldMatch("signing.*META-INF/versions/9/version/Version.class")
            .shouldMatch("signing.*META-INF/versions/10/version/Version.class")
            .shouldMatch("signing.*version/Main.class")
            .shouldMatch("signing.*version/Version.class");
        verify(SIGNED_JAR);

        // test with JarSigner API
        Files.deleteIfExists(Paths.get(SIGNED_JAR));
        signWithJarSignerAPI(JAR_NAME);
        verify(SIGNED_JAR);

        // test Permission granted
        File keypass = new File("keypass");
        try (FileOutputStream fos = new FileOutputStream(keypass)) {
            fos.write(KEYPASS.getBytes());
        }
        String[] cmd = {
                "-classpath", SIGNED_JAR,
                "-Djava.security.manager",
                "-Djava.security.policy=" +
                TEST_SRC + File.separator + POLICY_FILE,
                "version.Main"};
        ProcessTools.executeTestJvm(cmd)
            .shouldHaveExitValue(0)
            .shouldContain(VERSION_MESSAGE);
    }

    private static void compile (String jarContent_path) throws Throwable {
        Path classes = Paths.get(USR_DIR, "classes", "base");
        Path source = Paths.get(TEST_SRC, jarContent_path, "base", "version");
        CompilerUtils.compile(source, classes);

        classes = Paths.get(USR_DIR, "classes", "v9");
        source = Paths.get(TEST_SRC, jarContent_path , "v9", "version");
        CompilerUtils.compile(source, classes);

        classes = Paths.get(USR_DIR, "classes", "v10");
        source = Paths.get(TEST_SRC, jarContent_path, "v10", "version");
        CompilerUtils.compile(source, classes);
    }

    private static OutputAnalyzer jar(String...args) throws Throwable {
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("jar");
        Stream.of(args).forEach(launcher::addToolArg);
        return ProcessTools.executeCommand(launcher.getCommand());
    }

    private static void genKey() throws Throwable {
        String keytool = JDKToolFinder.getJDKTool("keytool");
        Files.deleteIfExists(Paths.get(KEYSTORE));
        ProcessTools.executeCommand(keytool,
                "-J-Duser.language=en",
                "-J-Duser.country=US",
                "-genkey",
                "-keyalg", "dsa",
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
        args.add("-signedjar");
        args.add(SIGNED_JAR);
        args.add(jarName);
        args.add(ALIAS);

        return jarsigner(args);
    }

    private static void verify(String signedJarName) throws Throwable {
        verifyJar(signedJarName)
            .shouldHaveExitValue(0)
            .shouldContain("jar verified")
            .shouldMatch("smk.*META-INF/versions/9/version/Version.class")
            .shouldMatch("smk.*META-INF/versions/10/version/Version.class")
            .shouldMatch("smk.*version/Main.class")
            .shouldMatch("smk.*version/Version.class");
    }

    private static OutputAnalyzer verifyJar(String signedJarName) throws Throwable {
        List<String> args = new ArrayList<>();
        args.add("-verbose");
        args.add("-verify");
        args.add(signedJarName);

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

    private static void signWithJarSignerAPI(String jarName)
            throws Throwable {
        // Get JarSigner
        try (FileInputStream fis = new FileInputStream(KEYSTORE)) {
                KeyStore ks = KeyStore.getInstance("JKS");
                ks.load(fis, STOREPASS.toCharArray());
                PrivateKey pk = (PrivateKey)ks.getKey(ALIAS, KEYPASS.toCharArray());
                Certificate cert = ks.getCertificate(ALIAS);
                JarSigner signer = new JarSigner.Builder(pk,
                        CertificateFactory.getInstance("X.509").generateCertPath(
                                Collections.singletonList(cert)))
                        .build();
            // Sign jar
            try (ZipFile src = new JarFile(jarName);
                    FileOutputStream out = new FileOutputStream(SIGNED_JAR)) {
                signer.sign(src,out);
            }
        }
    }

}


