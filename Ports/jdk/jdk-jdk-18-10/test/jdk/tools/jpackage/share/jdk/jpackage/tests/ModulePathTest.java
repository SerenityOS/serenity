/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jpackage.tests;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.jpackage.test.TKit;
import jdk.jpackage.test.JavaAppDesc;
import jdk.jpackage.test.HelloApp;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.PackageType;
import jdk.jpackage.test.Annotations.Parameter;
import jdk.jpackage.test.Annotations.Parameters;
import jdk.jpackage.test.Annotations.Test;


/*
 * @test
 * @summary jpackage with --module-path testing
 * @library ../../../../helpers
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile ModulePathTest.java
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=jdk.jpackage.tests.ModulePathTest
 */

public final class ModulePathTest {

    @Parameters
    public static Collection data() {
        return List.of(new String[][]{
            {GOOD_PATH, EMPTY_DIR, NON_EXISTING_DIR},
            {EMPTY_DIR, NON_EXISTING_DIR, GOOD_PATH},
            {GOOD_PATH + "/a/b/c/d", GOOD_PATH},
            {String.join(File.pathSeparator, EMPTY_DIR, NON_EXISTING_DIR,
                GOOD_PATH)},
            {String.join(File.pathSeparator, EMPTY_DIR, NON_EXISTING_DIR),
                String.join(File.pathSeparator, EMPTY_DIR, NON_EXISTING_DIR,
                GOOD_PATH)},
            {},
            {EMPTY_DIR}
        });
    }

    public ModulePathTest(String... modulePathArgs) {
        this.modulePathArgs = List.of(modulePathArgs);
    }

    @Test
    @Parameter("benvenuto.jar:com.jar.foo/com.jar.foo.Hello")
    @Parameter("benvenuto.jmod:com.jmod.foo/com.jmod.foo.JModHello")
    public void test(String javaAppDesc) throws IOException {
        JavaAppDesc appDesc = JavaAppDesc.parse(javaAppDesc);

        Path goodModulePath = TKit.createTempDirectory("modules");

        Path appBundle = HelloApp.createBundle(appDesc, goodModulePath);

        JPackageCommand cmd = new JPackageCommand()
                .setArgumentValue("--dest", TKit.workDir().resolve("output"))
                .setDefaultAppName()
                .setPackageType(PackageType.IMAGE);

        if (TKit.isWindows()) {
            cmd.addArguments("--win-console");
        }

        cmd.addArguments("--module", String.join("/", appDesc.moduleName(),
                appDesc.className()));

        // Ignore runtime that can be set for all tests. Usually if default
        // runtime is set, it is fake one to save time on running jlink and
        // copying megabytes of data from Java home to application image.
        // We need proper runtime for this test.
        cmd.ignoreDefaultRuntime(true);

        Path emptyDir = TKit.createTempDirectory("empty-dir");
        Path nonExistingDir = TKit.withTempDirectory("non-existing-dir", x -> {});

        Function<String, String> substitute = str -> {
            String v = str;
            v = v.replace(GOOD_PATH, goodModulePath.toString());
            v = v.replace(EMPTY_DIR, emptyDir.toString());
            v = v.replace(NON_EXISTING_DIR, nonExistingDir.toString());
            return v;
        };

        boolean withGoodPath = modulePathArgs.stream().anyMatch(
                s -> s.contains(GOOD_PATH));

        cmd.addArguments(modulePathArgs.stream().map(arg -> Stream.of(
                "--module-path", substitute.apply(arg))).flatMap(s -> s).collect(
                Collectors.toList()));

        if (withGoodPath) {
            cmd.executeAndAssertHelloAppImageCreated();
        } else {
            final String expectedErrorMessage;
            if (modulePathArgs.isEmpty()) {
                expectedErrorMessage = "Error: Missing argument: --runtime-image or --module-path";
            } else {
                expectedErrorMessage = String.format(
                        "Failed to find %s module in module path", appDesc.moduleName());
            }

            List<String> output = cmd
                    .saveConsoleOutput(true)
                    .execute(1)
                    .getOutput();
            TKit.assertTextStream(expectedErrorMessage).apply(output.stream());
        }
    }

    private final List<String> modulePathArgs;

    private final static String GOOD_PATH = "@GoodPath@";
    private final static String EMPTY_DIR = "@EmptyDir@";
    private final static String NON_EXISTING_DIR = "@NonExistingDir@";
}
