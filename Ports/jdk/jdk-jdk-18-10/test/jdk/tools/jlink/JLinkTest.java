/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jdk.tools.jlink.plugin.Plugin;
import jdk.tools.jlink.internal.PluginRepository;
import tests.Helper;
import tests.JImageGenerator;

/*
 * @test
 * @summary Test image creation
 * @bug 8189777
 * @bug 8194922
 * @bug 8206962
 * @bug 8240349
 * @author Jean-Francois Denise
 * @requires (vm.compMode != "Xcomp" & os.maxMemory >= 2g)
 * @library ../lib
 * @modules java.base/jdk.internal.jimage
 *          jdk.jdeps/com.sun.tools.classfile
 *          jdk.jlink/jdk.tools.jlink.internal
 *          jdk.jlink/jdk.tools.jlink.plugin
 *          jdk.jlink/jdk.tools.jimage
 *          jdk.compiler
 * @build tests.*
 * @run main/othervm -Xmx1g JLinkTest
 */
public class JLinkTest {
    static final ToolProvider JLINK_TOOL = ToolProvider.findFirst("jlink")
        .orElseThrow(() ->
            new RuntimeException("jlink tool not found")
        );

    // number of built-in plugins from jdk.jlink module
    private static int getNumJlinkPlugins() {
        ModuleDescriptor desc = Plugin.class.getModule().getDescriptor();
        return desc.provides().stream()
                .filter(p -> p.service().equals(Plugin.class.getName()))
                .map(p -> p.providers().size())
                .findAny()
                .orElse(0);
    }

    private static boolean isOfJLinkModule(Plugin p) {
        return p.getClass().getModule() == Plugin.class.getModule();
    }

    public static void main(String[] args) throws Exception {

        Helper helper = Helper.newHelper();
        if (helper == null) {
            System.err.println("Test not run");
            return;
        }
        helper.generateDefaultModules();
        // expected num. of plugins from jdk.jlink module
        int expectedJLinkPlugins = getNumJlinkPlugins();
        int totalPlugins = 0;
        {
            // number of built-in plugins
            List<Plugin> builtInPlugins = new ArrayList<>();
            builtInPlugins.addAll(PluginRepository.getPlugins(ModuleLayer.boot()));
            totalPlugins = builtInPlugins.size();
            // actual num. of plugins loaded from jdk.jlink module
            int actualJLinkPlugins = 0;
            for (Plugin p : builtInPlugins) {
                p.getState();
                p.getType();
                if (isOfJLinkModule(p)) {
                    actualJLinkPlugins++;
                }
            }
            if (expectedJLinkPlugins != actualJLinkPlugins) {
                throw new AssertionError("Actual plugins loaded from jdk.jlink: " +
                        actualJLinkPlugins + " which doesn't match expected number : " +
                        expectedJLinkPlugins);
            }
        }

        {
             // No --module-path specified. $JAVA_HOME/jmods should be assumed.
             // The following should succeed as it uses only system modules.
             String imageDir = "bug818977-no-modulepath";
             JImageGenerator.getJLinkTask()
                     .output(helper.createNewImageDir(imageDir))
                     .addMods("jdk.jshell")
                     .call().assertSuccess();
        }

        {
             // invalid --module-path specified. java.base not found it it.
             // $JAVA_HOME/jmods should be added automatically.
             // The following should succeed as it uses only system modules.
             String imageDir = "bug8189777-invalid-modulepath";
             JImageGenerator.getJLinkTask()
                     .modulePath("does_not_exist_path")
                     .output(helper.createNewImageDir(imageDir))
                     .addMods("jdk.jshell")
                     .call().assertSuccess();
        }

        {
            // No --module-path specified. --add-modules ALL-MODULE-PATH specified.
            String imageDir = "bug8189777-all-module-path";
            JImageGenerator.getJLinkTask()
                    .output(helper.createNewImageDir(imageDir))
                    .addMods("ALL-MODULE-PATH")
                    .call().assertSuccess();
        }

        {
            String moduleName = "bug8134651";
            JImageGenerator.getJLinkTask()
                    .modulePath(helper.defaultModulePath())
                    .output(helper.createNewImageDir(moduleName))
                    .addMods("leaf1")
                    .call().assertSuccess();
            JImageGenerator.getJLinkTask()
                    .modulePath(helper.defaultModulePath())
                    .addMods("leaf1")
                    .option("--output")
                    .call().assertFailure("Error: no value given for --output");
            JImageGenerator.getJLinkTask()
                    .modulePath("")
                    .output(helper.createNewImageDir(moduleName))
                    .addMods("leaf1")
                    .call().assertFailure("Error: no value given for --module-path");
            // do not include standard module path - should be added automatically
            JImageGenerator.getJLinkTask()
                    .modulePath(helper.defaultModulePath(false))
                    .output(helper.createNewImageDir(moduleName))
                    .addMods("leaf1")
                    .call().assertSuccess();
            // no --module-path. default sys mod path is assumed - but that won't contain 'leaf1' module
            JImageGenerator.getJLinkTask()
                    .output(helper.createNewImageDir(moduleName))
                    .addMods("leaf1")
                    .call().assertFailure("Error: Module leaf1 not found");
        }

        {
            String moduleName = "m"; // 8163382
            Path jmod = helper.generateDefaultJModule(moduleName).assertSuccess();
            JImageGenerator.getJLinkTask()
                    .modulePath(helper.defaultModulePath())
                    .output(helper.createNewImageDir(moduleName))
                    .addMods("m")
                    .call().assertSuccess();
            moduleName = "mod";
            jmod = helper.generateDefaultJModule(moduleName).assertSuccess();
            JImageGenerator.getJLinkTask()
                    .modulePath(helper.defaultModulePath())
                    .output(helper.createNewImageDir(moduleName))
                    .addMods("m")
                    .call().assertSuccess();
        }

        {
            String moduleName = "m_8165735"; // JDK-8165735
            helper.generateDefaultJModule(moduleName+"dependency").assertSuccess();
            Path jmod = helper.generateDefaultJModule(moduleName, moduleName+"dependency").assertSuccess();
            JImageGenerator.getJLinkTask()
                    .modulePath(helper.defaultModulePath())
                    .repeatedModulePath(".") // second --module-path overrides the first one
                    .output(helper.createNewImageDir(moduleName))
                    .addMods(moduleName)
                    // second --module-path does not have that module
                    .call().assertFailure("Error: Module m_8165735 not found");

            JImageGenerator.getJLinkTask()
                    .modulePath(".") // first --module-path overridden later
                    .repeatedModulePath(helper.defaultModulePath())
                    .output(helper.createNewImageDir(moduleName))
                    .addMods(moduleName)
                    // second --module-path has that module
                    .call().assertSuccess();

            JImageGenerator.getJLinkTask()
                    .modulePath(helper.defaultModulePath())
                    .output(helper.createNewImageDir(moduleName))
                    .limitMods(moduleName)
                    .repeatedLimitMods("java.base") // second --limit-modules overrides first
                    .addMods(moduleName)
                    .call().assertFailure("Error: Module m_8165735dependency not found, required by m_8165735");

            JImageGenerator.getJLinkTask()
                    .modulePath(helper.defaultModulePath())
                    .output(helper.createNewImageDir(moduleName))
                    .limitMods("java.base")
                    .repeatedLimitMods(moduleName) // second --limit-modules overrides first
                    .addMods(moduleName)
                    .call().assertSuccess();
        }

        {
            // Help
            StringWriter writer = new StringWriter();
            PrintWriter pw = new PrintWriter(writer);
            JLINK_TOOL.run(pw, pw, "--help");
            String output = writer.toString();
            if (output.split("\n").length < 10) {
                System.err.println(output);
                throw new AssertionError("Help");
            }
        }

        {
            // List plugins
            StringWriter writer = new StringWriter();
            PrintWriter pw = new PrintWriter(writer);

            JLINK_TOOL.run(pw, pw, "--list-plugins");
            String output = writer.toString();
            List<String> commands = Stream.of(output.split("\\R"))
                    .filter((s) -> s.matches("  --.*"))
                    .collect(Collectors.toList());
            int number = commands.size();
            if (number != totalPlugins) {
                System.err.println(output);
                throw new AssertionError("Found: " + number + " expected " + totalPlugins);
            }

            boolean isSorted = IntStream.range(1, number)
                    .allMatch((int index) -> commands.get(index).compareTo(commands.get(index - 1)) >= 0);

            if(!isSorted) {
                throw new AssertionError("--list-plugins not presented in alphabetical order");
            }

        }

        // filter out files and resources + Skip debug + compress
        {
            String[] userOptions = {"--compress", "2", "--strip-debug",
                "--exclude-resources", "*.jcov, */META-INF/*", "--exclude-files",
                "*" + Helper.getDebugSymbolsExtension()};
            String moduleName = "excludezipskipdebugcomposite2";
            helper.generateDefaultJModule(moduleName, "composite2");
            String[] res = {".jcov", "/META-INF/"};
            String[] files = {Helper.getDebugSymbolsExtension()};
            Path imageDir = helper.generateDefaultImage(userOptions, moduleName).assertSuccess();
            helper.checkImage(imageDir, moduleName, res, files);
        }

        // filter out + Skip debug + compress with filter + sort resources
        {
            String[] userOptions2 = {"--compress=2:compress-filter=^/java.base/*",
                "--strip-debug", "--exclude-resources",
                "*.jcov, */META-INF/*", "--order-resources",
                "*/module-info.class,/sortcomposite2/*,*/javax/management/*"};
            String moduleName = "excludezipfilterskipdebugcomposite2";
            helper.generateDefaultJModule(moduleName, "composite2");
            String[] res = {".jcov", "/META-INF/"};
            Path imageDir = helper.generateDefaultImage(userOptions2, moduleName).assertSuccess();
            helper.checkImage(imageDir, moduleName, res, null);
        }

        // module-info.class should not be excluded
        {
            String[] userOptions = { "--exclude-resources", "/jdk_8194922/module-info.class" };
            String moduleName = "jdk_8194922";
            helper.generateDefaultJModule(moduleName);
            helper.generateDefaultImage(userOptions, moduleName).
                assertFailure("Cannot exclude /jdk_8194922/module-info.class");
        }

        // default compress
        {
            testCompress(helper, "compresscmdcomposite2", "--compress", "2");
        }

        {
            testCompress(helper, "compressfiltercmdcomposite2",
                    "--compress=2:filter=^/java.base/java/lang/*");
        }

        // compress 0
        {
            testCompress(helper, "compress0filtercmdcomposite2",
                    "--compress=0:filter=^/java.base/java/lang/*");
        }

        // compress 1
        {
            testCompress(helper, "compress1filtercmdcomposite2",
                    "--compress=1:filter=^/java.base/java/lang/*");
        }

        // compress 2
        {
            testCompress(helper, "compress2filtercmdcomposite2",
                    "--compress=2:filter=^/java.base/java/lang/*");
        }

        // invalid compress level
        {
            String[] userOptions = {"--compress", "invalid"};
            String moduleName = "invalidCompressLevel";
            helper.generateDefaultJModule(moduleName, "composite2");
            helper.generateDefaultImage(userOptions, moduleName).assertFailure("Error: Invalid compression level invalid");
        }

        // orphan argument - JDK-8166810
        {
            String[] userOptions = {"--compress", "2", "foo" };
            String moduleName = "orphanarg1";
            helper.generateDefaultJModule(moduleName, "composite2");
            helper.generateDefaultImage(userOptions, moduleName).assertFailure("Error: invalid argument: foo");
        }

        // orphan argument - JDK-8166810
        {
            String[] userOptions = {"--output", "foo", "bar" };
            String moduleName = "orphanarg2";
            helper.generateDefaultJModule(moduleName, "composite2");
            helper.generateDefaultImage(userOptions, moduleName).assertFailure("Error: invalid argument: bar");
        }

        // basic check for --help - JDK-8173717
        {
            JImageGenerator.getJLinkTask()
                    .option("--help")
                    .call().assertSuccess();
        }

        {
            String imageDir = "bug8206962";
            JImageGenerator.getJLinkTask()
                    .modulePath(helper.defaultModulePath())
                    .output(helper.createNewImageDir(imageDir))
                    .addMods("java.base")
                    .option("--release-info=del")
                    .call().assertFailure("Error: No key specified for delete");
        }
        {
            String imageDir = "bug8240349";
            Path imagePath = helper.createNewImageDir(imageDir);
            JImageGenerator.getJLinkTask()
                    .modulePath(helper.defaultModulePath())
                    .output(imagePath)
                    .addMods("java.base")
                    .option("--vm=client")
                    .call().assertFailure("Error: Selected VM client doesn't exist");
            if (!Files.notExists(imagePath)) {
                throw new RuntimeException("bug8240349 directory not deleted");
            }
        }
    }

    private static void testCompress(Helper helper, String moduleName, String... userOptions) throws IOException {
        helper.generateDefaultJModule(moduleName, "composite2");
        Path imageDir = helper.generateDefaultImage(userOptions, moduleName).assertSuccess();
        helper.checkImage(imageDir, moduleName, null, null);
    }
}
