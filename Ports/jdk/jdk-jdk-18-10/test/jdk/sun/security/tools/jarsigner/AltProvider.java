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
 * @bug 4906940 8130302 8194152
 * @summary -providerPath, -providerClass, -addprovider, and -providerArg
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @build jdk.test.lib.util.JarUtils
 *        jdk.test.lib.compiler.CompilerUtils
 *        jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 * @run main AltProvider
 */

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.util.JarUtils;
import jdk.test.lib.compiler.CompilerUtils;

import java.nio.file.*;

public class AltProvider {

    private static final String TEST_SRC =
            Paths.get(System.getProperty("test.src")).toString();

    private static final Path MOD_SRC_DIR = Paths.get(TEST_SRC, "alt");
    private static final Path MOD_DEST_DIR = Paths.get("mods");

    private static final String ktCommand = "-keystore x.jks " +
            "-storepass changeit -storetype dummyks -list -debug";

    private static final String jsCommand = "-keystore x.jks " +
            "-storepass changeit -storetype dummyks -debug x.jar x";

    public static void main(String[] args) throws Throwable {

        // Compile the provider
        CompilerUtils.compile(
                MOD_SRC_DIR, MOD_DEST_DIR,
                "--module-source-path",
                MOD_SRC_DIR.toString());

        // Create a keystore
        tool("keytool", "-keystore x.jks -storetype jks -genkeypair -keyalg dsa" +
                " -storepass changeit -keypass changeit -alias x -dname CN=X")
                .shouldHaveExitValue(0);

        // Create a jar file
        JarUtils.createJar("x.jar", "x.jks");

        // Test starts here

        // Without new provider
        testBoth("", 1, "DUMMYKS not found");

        // legacy use (-providerPath only supported by keytool)
        testKeytool("-providerPath mods/test.dummy " +
                "-providerClass org.test.dummy.DummyProvider -providerArg full",
                0, "loadProviderByClass: org.test.dummy.DummyProvider");

        // legacy, on classpath
        testBoth("-J-cp -Jmods/test.dummy " +
                "-providerClass org.test.dummy.DummyProvider -providerArg full",
                0, "loadProviderByClass: org.test.dummy.DummyProvider");

        // Wrong name
        testBoth("-J-cp -Jmods/test.dummy " +
                "-providerClass org.test.dummy.Dummy -providerArg full",
                1, "Provider \"org.test.dummy.Dummy\" not found");

        // Not a provider name
        testBoth("-J-cp -Jmods/test.dummy " +
                "-providerClass java.lang.Object -providerArg full",
                1, "java.lang.Object not a provider");

        // without arg
        testBoth("-J-cp -Jmods/test.dummy " +
                "-providerClass org.test.dummy.DummyProvider",
                1, "DUMMYKS not found");

        // old -provider still works
        testBoth("-J-cp -Jmods/test.dummy " +
                "-provider org.test.dummy.DummyProvider -providerArg full",
                0, "loadProviderByClass: org.test.dummy.DummyProvider");

        // name in a module
        testBoth("-J--module-path=mods " +
                "-addprovider Dummy -providerArg full",
                0, "loadProviderByName: Dummy");

        // -providerClass does not work
        testBoth("-J--module-path=mods " +
                "-providerClass org.test.dummy.DummyProvider -providerArg full",
                1, "Provider \"org.test.dummy.DummyProvider\" not found");

        // -addprovider with class does not work
        testBoth("-J--module-path=mods " +
                "-addprovider org.test.dummy.DummyProvider -providerArg full",
                1, "Provider named \"org.test.dummy.DummyProvider\" not found");

        // -addprovider without arg does not work
        testBoth("-J--module-path=mods " +
                "-addprovider Dummy",
                1, "DUMMYKS not found");
    }

    // Test both tools with the same extra options
    private static void testBoth(String args, int exitValue, String contains)
            throws Throwable {
        testKeytool(args, exitValue, contains);
        testJarsigner(args, exitValue, contains);
    }

    // Test keytool with extra options and check exitValue and output
    private static void testKeytool(String args, int exitValue, String contains)
            throws Throwable {
        tool("keytool", ktCommand + " " + args)
                .shouldHaveExitValue(exitValue)
                .shouldContain(contains);
    }

    // Test jarsigner with extra options and check exitValue and output
    private static void testJarsigner(String args, int exitValue, String contains)
            throws Throwable {
        tool("jarsigner", jsCommand + " " + args)
                .shouldHaveExitValue(exitValue)
                .shouldContain(contains);
    }

    // Launch a tool with args (space separated string)
    private static OutputAnalyzer tool(String tool, String args)
            throws Throwable {
        JDKToolLauncher l = JDKToolLauncher.createUsingTestJDK(tool);

        // Set locale to en-US so that the output are not translated into other languages.
        l.addVMArg("-Duser.language=en");
        l.addVMArg("-Duser.country=US");

        for (String a: args.split("\\s+")) {
            if (a.startsWith("-J")) {
                l.addVMArg(a.substring(2));
            } else {
                l.addToolArg(a);
            }
        }
        return ProcessTools.executeCommand(l.getCommand());
    }
}
