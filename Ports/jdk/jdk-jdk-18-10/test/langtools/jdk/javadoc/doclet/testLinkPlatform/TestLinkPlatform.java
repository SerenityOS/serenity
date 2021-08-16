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
 * @bug 8216497
 * @summary javadoc should auto-link to platform classes
 * @library /tools/lib ../../lib
 * @modules
 *      jdk.javadoc/jdk.javadoc.internal.tool
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox javadoc.tester.*
 * @run main TestLinkPlatform
 */

import javadoc.tester.JavadocTester;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Formatter;

import builder.ClassBuilder;
import builder.ClassBuilder.*;
import toolbox.ModuleBuilder;
import toolbox.ToolBox;

import javax.lang.model.SourceVersion;

public class TestLinkPlatform extends JavadocTester {

    final static String OLD_PLATFORM_URL = "https://docs.oracle.com/javase/%d/docs/api/java/lang/Object.html";
    final static String NEW_PLATFORM_URL = "https://docs.oracle.com/en/java/javase/%d/docs/api/java.base/java/lang/Object.html";
    final static String PRE_PLATFORM_URL = "https://download.java.net/java/early_access/jdk%d/docs/api/java.base/java/lang/Object.html";

    final static String NON_MODULAR_CUSTOM_PLATFORM_URL = "https://example.com/%d/api/java/lang/Object.html";
    final static String MODULAR_CUSTOM_PLATFORM_URL = "https://example.com/%d/api/java.base/java/lang/Object.html";

    final static int EARLIEST_VERSION = 7;
    final static int LATEST_VERSION = Integer.parseInt(SourceVersion.latest().name().substring(8));

    /**
     * The entry point of the test.
     *
     * @param args the array of command line arguments.
     */
    public static void main(String... args) throws Exception {
        TestLinkPlatform tester = new TestLinkPlatform();
        tester.runTests(m -> new Object[]{Paths.get(m.getName())});
    }

    final ToolBox tb;
    private final Path packageSrc;

    TestLinkPlatform() throws Exception {
        tb = new ToolBox();
        packageSrc = Paths.get("src", "packages");
        initCode();
    }

    @Test
    public void testPlatformLinkWithReleaseOption(Path base) throws Exception {
        testPlatformLinkWithSupportedVersions(base, "--release");
    }

    @Test
    public void testPlatformLinkWithSourceOption(Path base) throws Exception {
        testPlatformLinkWithSupportedVersions(base, "--source");
    }

    private void testPlatformLinkWithSupportedVersions(Path base, String versionOption) throws Exception {
        for (int version = EARLIEST_VERSION; version <= LATEST_VERSION; version++) {
            Path out = base.resolve("out_" + version);

            javadoc("-d", out.toString(),
                    "-sourcepath", packageSrc.toString(),
                    versionOption, Integer.toString(version),
                    "p.q");

            checkExit(Exit.OK);
            // Make sure there is no message about missing element-list resource
            checkOutput(Output.OUT, false, "element-list");
            String url = getPlatformUrlString(version);
            checkOutput("p/q/A.html", true,
                    "<a href=\"" + url + "\"",
                    "<a href=\"" + url + "#clone()\" title=\"class or interface in java.lang\" class=\"external-link\">",
                    "<a href=\"" + url + "#equals(java.lang.Object)\" title=\"class or interface in java.lang\" class=\"external-link\">",
                    "<a href=\"" + url + "#finalize()\" title=\"class or interface in java.lang\" class=\"external-link\">");
        }
    }

    @Test
    public void testPlatformLinkWithCustomPropertyURL(Path base) throws Exception {
        Path customProps = writeCustomProperties(base);
        for (int version = EARLIEST_VERSION; version <= LATEST_VERSION; version++) {
            Path out = base.resolve("out_" + version);

            javadoc("-d", out.toString(),
                    "-sourcepath", packageSrc.toString(),
                    "--release", Integer.toString(version),
                    "--link-platform-properties", customProps.toUri().toString(),
                    "p.q");

            checkExit(Exit.OK);
            String url = getCustomPlatformUrlString(version);
            checkOutput("p/q/A.html", true,
                    "<a href=\"" + url + "\"",
                    "<a href=\"" + url + "#clone()\" title=\"class or interface in java.lang\" class=\"external-link\">",
                    "<a href=\"" + url + "#equals(java.lang.Object)\" title=\"class or interface in java.lang\" class=\"external-link\">",
                    "<a href=\"" + url + "#finalize()\" title=\"class or interface in java.lang\" class=\"external-link\">");
        }
    }

    @Test
    public void testPlatformLinkWithCustomPropertyFile(Path base) throws Exception {
        Path customProps = writeCustomProperties(base);
        for (int version = EARLIEST_VERSION; version <= LATEST_VERSION; version++) {
            Path out = base.resolve("out_" + version);

            javadoc("-d", out.toString(),
                    "-sourcepath", packageSrc.toString(),
                    "--release", Integer.toString(version),
                    "--link-platform-properties", customProps.toString(),
                    "p.q");

            checkExit(Exit.OK);
            String url = getCustomPlatformUrlString(version);
            checkOutput("p/q/A.html", true,
                    "<a href=\"" + url + "\"",
                    "<a href=\"" + url + "#clone()\" title=\"class or interface in java.lang\" class=\"external-link\">",
                    "<a href=\"" + url + "#equals(java.lang.Object)\" title=\"class or interface in java.lang\" class=\"external-link\">",
                    "<a href=\"" + url + "#finalize()\" title=\"class or interface in java.lang\" class=\"external-link\">");
        }
    }

    private Path writeCustomProperties(Path base) throws IOException {
        ToolBox tb = new ToolBox();
        StringBuilder sb = new StringBuilder();
        for (int version = EARLIEST_VERSION; version <= LATEST_VERSION; version++) {
            sb.append(String.format("doclet.platform.docs.%1$d= https://example.com/%1$d/api/\n", version));
        }
        Path path = base.resolve("linkplatform.properties");
        tb.writeFile(path, sb.toString());
        return path;
    }

    @Test
    public void testPlatformLinkWithInvalidPropertyFile(Path base) throws Exception {
        for (int version = EARLIEST_VERSION; version <= LATEST_VERSION; version++) {
            Path out = base.resolve("out_" + version);

            javadoc("-d", out.toString(),
                    "-sourcepath", packageSrc.toString(),
                    "--release", Integer.toString(version),
                    "--link-platform-properties", testSrc("invalid-properties-file"),
                    "p.q");

            checkExit(Exit.ERROR);
            checkOutput(Output.OUT, true, "Error reading file");
            checkOutput("p/q/A.html", false);
        }
    }

    void initCode() throws Exception {
        new ClassBuilder(tb, "p.q.A")
                .setModifiers("public","class")
                .write(packageSrc);
    }

    String getPlatformUrlString(int version) {
        String urlString;
        Runtime.Version runtimeVersion = Runtime.version();
        if (version == runtimeVersion.feature() && runtimeVersion.pre().isPresent()) {
            urlString = PRE_PLATFORM_URL;
        } else {
            urlString = version <= 10 ? OLD_PLATFORM_URL : NEW_PLATFORM_URL;
        }
        return urlString.formatted(version);
    }

    String getCustomPlatformUrlString(int version) {
        return version <= 10
                ? NON_MODULAR_CUSTOM_PLATFORM_URL.formatted(version)
                : MODULAR_CUSTOM_PLATFORM_URL.formatted(version);
    }
}
