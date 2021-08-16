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
 *
 */

/*
 * @test
 * @bug 8257241
 * @summary Run the LambdaEagerInitTest.java test in static CDS archive mode.
 *          Create a custom base archive with the -Djdk.internal.lambda.disableEagerInitialization=true property.
 *          Run with the custom base archive with and without specifying the property.
 *          With the disableEagerInit set to true during dump time, lambda proxy classes
 *          will not be archived. During runtime, lambda proxy classes will not be loaded
 *          from the archive.
 *          Run with the default CDS archive, lambda proxy classes will be loaded
 *          from the archive if the property is not set.
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds test-classes
 * @compile ../../../../../lib/jdk/test/lib/Asserts.java
 * @run main/othervm LambdaEagerInit
 */

import java.io.File;

import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;

public class LambdaEagerInit {
    public static void main(String[] args) throws Exception {
        createArchiveWithEagerInitializationEnabled();
        testWithEagerInitializationEnabled();
        testWithEagerInitializationDisabled();
        // Skip testing with default CDS archive on aarch64 platform because
        // default archive isn't being generated on that platform.
        if (!("aarch64".equals(System.getProperty("os.arch")))) {
            testDefaultArchiveWithEagerInitializationEnabled();
            testDefaultArchiveWithEagerInitializationDisabled();
        }
    }

    private static final String classDir = System.getProperty("test.classes");
    private static final String mainClass = LambdaEagerInitTest.class.getName();
    private static final String testProperty = "-Djdk.internal.lambda.disableEagerInitialization=true";
    private static final String lambdaNotLoadedFromArchive =
        ".class.load. java.util.stream.Collectors[$][$]Lambda[$].*/0x.*source:.*java.*util.*stream.*Collectors";
    private static final String lambdaLoadedFromArchive =
        ".class.load. java.util.stream.Collectors[$][$]Lambda[$].*/0x.*source:.*shared.*objects.*file";
    private static final String cdsLoadedLambdaProxy = ".cds.*Loaded.*lambda.*proxy";
    private static final String archiveName = mainClass + ".jsa";
    private static String appJar;

    static void createArchiveWithEagerInitializationEnabled() throws Exception {
        appJar = JarBuilder.build("lambda_eager", new File(classDir), null);

        // create base archive with the -Djdk.internal.lambda.disableEagerInitialization=true property
        CDSOptions opts = (new CDSOptions())
            .addPrefix(testProperty,
                       "-Xlog:class+load,cds")
            .setArchiveName(archiveName);
        CDSTestUtils.createArchiveAndCheck(opts);
    }

    static void testWithEagerInitializationEnabled() throws Exception {
        // run with custom base archive with the -Djdk.internal.lambda.disableEagerInitialization=true property
        CDSOptions runOpts = (new CDSOptions())
            .addPrefix("-cp", appJar, testProperty,  "-Xlog:class+load,cds=debug")
            .setArchiveName(archiveName)
            .setUseVersion(false)
            .addSuffix(mainClass);
        OutputAnalyzer output = CDSTestUtils.runWithArchive(runOpts);
        output.shouldMatch(lambdaNotLoadedFromArchive)
              .shouldNotMatch(cdsLoadedLambdaProxy)
              .shouldHaveExitValue(0);
    }

    static void testWithEagerInitializationDisabled() throws Exception {
        // run with custom base archive without the -Djdk.internal.lambda.disableEagerInitialization=true property
        CDSOptions runOpts = (new CDSOptions())
            .addPrefix("-cp", appJar, "-Xlog:class+load,cds=debug")
            .setArchiveName(archiveName)
            .setUseVersion(false)
            .addSuffix(mainClass);
        OutputAnalyzer output = CDSTestUtils.runWithArchive(runOpts);
        output.shouldMatch(lambdaNotLoadedFromArchive)
              .shouldNotMatch(cdsLoadedLambdaProxy)
              .shouldHaveExitValue(0);
    }

    static void testDefaultArchiveWithEagerInitializationEnabled() throws Exception {
        // run with default CDS archive with the -Djdk.internal.lambda.disableEagerInitialization=true property
        CDSOptions runOpts = (new CDSOptions())
            .addPrefix("-cp", appJar, testProperty,  "-Xlog:class+load,cds=debug")
            .setUseSystemArchive(true)
            .setUseVersion(false)
            .addSuffix(mainClass);
        OutputAnalyzer output = CDSTestUtils.runWithArchive(runOpts);
        output.shouldMatch(lambdaNotLoadedFromArchive)
              .shouldNotMatch(cdsLoadedLambdaProxy)
              .shouldHaveExitValue(0);
    }

    static void testDefaultArchiveWithEagerInitializationDisabled() throws Exception {
        // run with default CDS archive without the -Djdk.internal.lambda.disableEagerInitialization=true property
        CDSOptions runOpts = (new CDSOptions())
            .addPrefix("-cp", appJar, "-Xlog:class+load,cds=debug")
            .setUseSystemArchive(true)
            .setUseVersion(false)
            .addSuffix(mainClass);
        OutputAnalyzer output = CDSTestUtils.runWithArchive(runOpts);
        output.shouldMatch(lambdaLoadedFromArchive)
              .shouldMatch(cdsLoadedLambdaProxy)
              .shouldHaveExitValue(0);
    }
}
