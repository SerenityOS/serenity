/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.util.List;
import java.util.ArrayList;
import jdk.jpackage.internal.IOUtils;
import jdk.jpackage.test.TKit;
import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.PackageType;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.Annotations.Parameter;
import jdk.jpackage.test.Annotations.Parameters;
import jdk.jpackage.test.JPackageCommand;

/*
 * @test usage of scripts from resource dir
 * @summary jpackage with
 * @library ../helpers
 * @build jdk.jpackage.test.*
 * @requires (os.family == "windows")
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile WinScriptTest.java
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=WinScriptTest
 */

public class WinScriptTest {

    public WinScriptTest(PackageType type) {
        this.packageType = type;

        test = new PackageTest()
        .forTypes(type)
        .configureHelloApp()
        .addInitializer(cmd -> {
            cmd.setFakeRuntime().saveConsoleOutput(true);
        });
    }

    @Parameters
    public static List<Object[]> data() {
        return List.of(new Object[][]{
            {PackageType.WIN_MSI},
            {PackageType.WIN_EXE}
        });
    }

    @Test
    @Parameter("0")
    @Parameter("10")
    public void test(int wsfExitCode) throws IOException {
        final ScriptData appImageScriptData;
        if (wsfExitCode != 0 && packageType == PackageType.WIN_EXE) {
            appImageScriptData = new ScriptData(PackageType.WIN_MSI, 0);
        } else {
            appImageScriptData = new ScriptData(PackageType.WIN_MSI, wsfExitCode);
        }

        final ScriptData msiScriptData = new ScriptData(PackageType.WIN_EXE, wsfExitCode);

        test.setExpectedExitCode(wsfExitCode == 0 ? 0 : 1);

        final Path tempDir = TKit.createTempDirectory("resources");

        test.addInitializer(cmd -> {
            cmd.addArguments("--resource-dir", tempDir);

            appImageScriptData.createScript(cmd);
            msiScriptData.createScript(cmd);
        });

        switch (packageType) {
            case WIN_MSI:
                test.addBundleVerifier((cmd, result) -> {
                    appImageScriptData.assertJPackageOutput(result.getOutput());
                });
                break;

            case WIN_EXE:
                test.addBundleVerifier((cmd, result) -> {
                    appImageScriptData.assertJPackageOutput(result.getOutput());
                    msiScriptData.assertJPackageOutput(result.getOutput());
                });
                break;
        }

        test.run();
    }

    private static class ScriptData {
        ScriptData(PackageType scriptType, int wsfExitCode) {
            if (scriptType == PackageType.WIN_MSI) {
                echoText = "post app image wsf";
                envVarName = "JpAppImageDir";
                scriptSuffixName = "post-image";
            } else {
                echoText = "post msi wsf";
                envVarName = "JpMsiFile";
                scriptSuffixName = "post-msi";
            }
            this.wsfExitCode = wsfExitCode;
        }

        void assertJPackageOutput(List<String> output) {
            TKit.assertTextStream(String.format("    jp: %s", echoText))
                    .predicate(String::equals)
                    .apply(output.stream());

            String cwdPattern = String.format("    jp: CWD(%s)=", envVarName);
            TKit.assertTextStream(cwdPattern)
                    .predicate(String::startsWith)
                    .apply(output.stream());
            String cwd = output.stream().filter(line -> line.startsWith(
                    cwdPattern)).findFirst().get().substring(cwdPattern.length());

            String envVarPattern = String.format("    jp: %s=", envVarName);
            TKit.assertTextStream(envVarPattern)
                    .predicate(String::startsWith)
                    .apply(output.stream());
            String envVar = output.stream().filter(line -> line.startsWith(
                    envVarPattern)).findFirst().get().substring(envVarPattern.length());

            TKit.assertTrue(envVar.startsWith(cwd), String.format(
                    "Check value of %s environment variable [%s] starts with the current directory [%s] set for %s script",
                    envVarName, envVar, cwd, echoText));
        }

        void createScript(JPackageCommand cmd) throws IOException {
           IOUtils.createXml(Path.of(cmd.getArgumentValue("--resource-dir"),
                    String.format("%s-%s.wsf", cmd.name(), scriptSuffixName)), xml -> {
                xml.writeStartElement("job");
                xml.writeAttribute("id", "main");
                xml.writeStartElement("script");
                xml.writeAttribute("language", "JScript");
                xml.writeCData(String.join("\n", List.of(
                    "var shell = new ActiveXObject('WScript.Shell')",
                    "WScript.Echo('jp: " + envVarName + "=' + shell.ExpandEnvironmentStrings('%" + envVarName + "%'))",
                    "WScript.Echo('jp: CWD(" + envVarName + ")=' + shell.CurrentDirectory)",
                    String.format("WScript.Echo('jp: %s')", echoText),
                    String.format("WScript.Quit(%d)", wsfExitCode)
                )));
                xml.writeEndElement();
                xml.writeEndElement();
            });
        }

        private final int wsfExitCode;
        private final String scriptSuffixName;
        private final String echoText;
        private final String envVarName;
    }

    private final PackageType packageType;
    private PackageTest test;
}
