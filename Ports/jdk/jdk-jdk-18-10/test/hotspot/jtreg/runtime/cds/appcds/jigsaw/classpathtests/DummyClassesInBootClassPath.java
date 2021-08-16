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
 *
 */

/*
 * @test
 * @summary Ensure that classes found in jimage takes precedence over classes found in -Xbootclasspath/a.
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @compile ../../test-classes/DummyClassHelper.java
 * @compile ../../test-classes/java/net/HttpCookie.jasm
 * @compile ../../../javax/annotation/processing/FilerException.jasm
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver DummyClassesInBootClassPath
 */

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import jdk.test.lib.process.OutputAnalyzer;

public class DummyClassesInBootClassPath {
    private static final String METHOD_NAME = "thisClassIsDummy()";

    static void checkOutput(OutputAnalyzer output, String[] classNames) throws Exception {
        for (int i = 0; i < classNames.length; i++) {
            String cn = classNames[i].replace('/', '.');
            TestCommon.checkExec(output,
                "java.lang.NoSuchMethodException: " + cn + "." +
                METHOD_NAME);
            output.shouldNotContain(cn + ".class should be in shared space.");
        }
    }

    public static void main(String[] args) throws Exception {
        String classNames[] = { "java/net/HttpCookie",
                                "javax/annotation/processing/FilerException"};
        JarBuilder.build("dummyClasses", classNames[0], classNames[1]);

        String appJar = TestCommon.getTestJar("dummyClasses.jar");
        OutputAnalyzer dumpOutput = TestCommon.dump(
            appJar, classNames, "-Xbootclasspath/a:" + appJar);
        TestCommon.checkDump(dumpOutput);

        List<String> argsList = new ArrayList<String>();
        for (int i = 0; i < classNames.length; i++) {
            argsList.add(classNames[i].replace('/', '.'));
        }
        String[] arguments = new String[argsList.size()];
        arguments = argsList.toArray(arguments);
        TestCommon.run(
            "-Xbootclasspath/a:" + appJar,
            "DummyClassHelper", arguments[0], arguments[1])
          .assertNormalExit(output -> checkOutput(output, classNames));

        JarBuilder.build(true, "WhiteBox", "sun/hotspot/WhiteBox");
        String whiteBoxJar = TestCommon.getTestJar("WhiteBox.jar");
        String bootClassPath = "-Xbootclasspath/a:" + appJar +
            File.pathSeparator + whiteBoxJar;
        dumpOutput = TestCommon.dump(
            appJar, classNames, bootClassPath);
        argsList.add("testWithWhiteBox");
        arguments = new String[argsList.size()];
        arguments = argsList.toArray(arguments);
        String[] opts = {"-XX:+UnlockDiagnosticVMOptions", "-XX:+WhiteBoxAPI",
            bootClassPath, "-Xlog:class+path=trace",
            "DummyClassHelper", arguments[0], arguments[1], arguments[2]};
        TestCommon.run(opts)
          .assertNormalExit(output -> checkOutput(output, classNames));
    }
}

