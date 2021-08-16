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

import java.io.IOException;
import java.nio.file.Path;
import jdk.jpackage.test.TKit;
import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.PackageType;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.Annotations.Parameters;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import jdk.jpackage.test.Executor;

/*
 * @test
 * @summary Custom l10n of msi installers in jpackage
 * @library ../helpers
 * @key jpackagePlatformPackage
 * @requires (jpackage.test.SQETest == null)
 * @build jdk.jpackage.test.*
 * @requires (os.family == "windows")
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile WinL10nTest.java
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=WinL10nTest
 */

public class WinL10nTest {

    public WinL10nTest(WixFileInitializer wxlFileInitializers[],
            String expectedCulture, String expectedErrorMessage) {
        this.wxlFileInitializers = wxlFileInitializers;
        this.expectedCulture = expectedCulture;
        this.expectedErrorMessage = expectedErrorMessage;
    }

    @Parameters
    public static List<Object[]> data() {
        return List.of(new Object[][]{
            {null, "en-us", null},
            {new WixFileInitializer[] {
                WixFileInitializer.create("a.wxl", "en-us")
            }, "en-us", null},
            {new WixFileInitializer[] {
                WixFileInitializer.create("a.wxl", "fr")
            }, "fr;en-us", null},
            {new WixFileInitializer[] {
                WixFileInitializer.create("a.wxl", "fr"),
                WixFileInitializer.create("b.wxl", "fr")
            }, "fr;en-us", null},
            {new WixFileInitializer[] {
                WixFileInitializer.create("a.wxl", "it"),
                WixFileInitializer.create("b.wxl", "fr")
            }, "it;fr;en-us", null},
            {new WixFileInitializer[] {
                WixFileInitializer.create("c.wxl", "it"),
                WixFileInitializer.create("b.wxl", "fr")
            }, "fr;it;en-us", null},
            {new WixFileInitializer[] {
                WixFileInitializer.create("a.wxl", "fr"),
                WixFileInitializer.create("b.wxl", "it"),
                WixFileInitializer.create("c.wxl", "fr"),
                WixFileInitializer.create("d.wxl", "it")
            }, "fr;it;en-us", null},
            {new WixFileInitializer[] {
                WixFileInitializer.create("c.wxl", "it"),
                WixFileInitializer.createMalformed("b.wxl")
            }, null, null}
        });
    }

    private final static Stream<String> getLightCommandLine(
            Executor.Result result) {
        return result.getOutput().stream()
                .filter(s -> s.trim().startsWith("light.exe"));
    }

    @Test
    public void test() throws IOException {

        final boolean allWxlFilesValid;
        if (wxlFileInitializers != null) {
            allWxlFilesValid = Stream.of(wxlFileInitializers).allMatch(
                    WixFileInitializer::isValid);
        } else {
            allWxlFilesValid = true;
        }

        PackageTest test = new PackageTest()
        .forTypes(PackageType.WINDOWS)
        .configureHelloApp()
        .addInitializer(cmd -> {
            // 1. Set fake run time to save time by skipping jlink step of jpackage.
            // 2. Instruct test to save jpackage output.
            cmd.setFakeRuntime().saveConsoleOutput(true);
        })
        .addBundleVerifier((cmd, result) -> {
            if (expectedCulture != null) {
                TKit.assertTextStream("-cultures:" + expectedCulture).apply(
                        getLightCommandLine(result));
            }

            if (expectedErrorMessage != null) {
                TKit.assertTextStream(expectedErrorMessage)
                        .apply(result.getOutput().stream());
            }

            if (wxlFileInitializers != null) {
                if (allWxlFilesValid) {
                    for (var v : wxlFileInitializers) {
                        v.createCmdOutputVerifier(resourceDir).apply(
                                getLightCommandLine(result));
                    }
                } else {
                    Stream.of(wxlFileInitializers)
                            .filter(Predicate.not(WixFileInitializer::isValid))
                            .forEach(v -> v.createCmdOutputVerifier(
                                    resourceDir).apply(result.getOutput().stream()));
                    TKit.assertFalse(
                            getLightCommandLine(result).findAny().isPresent(),
                            "Check light.exe was not invoked");
                }
            }
        });

        if (wxlFileInitializers != null) {
            test.addInitializer(cmd -> {
                resourceDir = TKit.createTempDirectory("resources");

                cmd.addArguments("--resource-dir", resourceDir);

                for (var v : wxlFileInitializers) {
                    v.apply(resourceDir);
                }
            });
        }

        if (expectedErrorMessage != null || !allWxlFilesValid) {
            test.setExpectedExitCode(1);
        }

        test.run();
    }

    final private WixFileInitializer wxlFileInitializers[];
    final private String expectedCulture;
    final private String expectedErrorMessage;
    private Path resourceDir;

    private static class WixFileInitializer {
        static WixFileInitializer create(String name, String culture) {
            return new WixFileInitializer(name, culture);
        }

        static WixFileInitializer createMalformed(String name) {
            return new WixFileInitializer(name, null) {
                @Override
                public void apply(Path root) throws IOException {
                    TKit.createTextFile(root.resolve(name), List.of(
                            "<?xml version=\"1.0\" encoding=\"utf-8\"?>",
                            "<WixLocalization>"));
                }

                @Override
                public String toString() {
                    return String.format("name=%s; malformed xml", name);
                }

                @Override
                boolean isValid() {
                    return false;
                }

                @Override
                TKit.TextStreamVerifier createCmdOutputVerifier(Path root) {
                    return TKit.assertTextStream(String.format(
                            "Failed to parse %s file",
                            root.resolve("b.wxl").toAbsolutePath()));
                }
            };
        }

        private WixFileInitializer(String name, String culture) {
            this.name = name;
            this.culture = culture;
        }

        void apply(Path root) throws IOException {
            TKit.createTextFile(root.resolve(name), List.of(
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>",
                    culture == null ? "<WixLocalization/>" : "<WixLocalization Culture=\""
                            + culture
                            + "\" xmlns=\"http://schemas.microsoft.com/wix/2006/localization\" Codepage=\"1252\"/>"));
        }

        TKit.TextStreamVerifier createCmdOutputVerifier(Path root) {
            return TKit.assertTextStream(
                    root.resolve(name).toAbsolutePath().toString());
        }

        boolean isValid() {
            return true;
        }

        @Override
        public String toString() {
            return String.format("name=%s; culture=%s", name, culture);
        }

        private final String name;
        private final String culture;
    }
}
