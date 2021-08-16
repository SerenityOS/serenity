/*
 * Copyright (c) 2019, Red Hat, Inc.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.test.lib.compiler.CompilerUtils;
import jdk.tools.jlink.internal.ResourcePoolManager;
import jdk.tools.jlink.internal.plugins.StripNativeDebugSymbolsPlugin;
import jdk.tools.jlink.internal.plugins.StripNativeDebugSymbolsPlugin.ObjCopyCmdBuilder;
import jdk.tools.jlink.plugin.ResourcePool;
import jdk.tools.jlink.plugin.ResourcePoolEntry;

/*
 * @test
 * @requires os.family == "linux"
 * @bug 8214796
 * @summary Test --strip-native-debug-symbols plugin
 * @library /test/lib
 * @modules jdk.compiler
 *          jdk.jlink/jdk.tools.jlink.internal.plugins
 *          jdk.jlink/jdk.tools.jlink.internal
 *          jdk.jlink/jdk.tools.jlink.plugin
 * @build jdk.test.lib.compiler.CompilerUtils FakeObjCopy
 * @run main/othervm -Xmx1g StripNativeDebugSymbolsPluginTest
 */
public class StripNativeDebugSymbolsPluginTest {

    private static final String OBJCOPY = "objcopy";
    private static final String DEFAULT_OBJCOPY_CMD = OBJCOPY;
    private static final String PLUGIN_NAME = "strip-native-debug-symbols";
    private static final String MODULE_NAME_WITH_NATIVE = "fib";
    private static final String JAVA_HOME = System.getProperty("java.home");
    private static final String NATIVE_LIB_NAME = "libFib.so";
    private static final Path JAVA_LIB_PATH = Paths.get(System.getProperty("java.library.path"));
    private static final Path LIB_FIB_SRC = JAVA_LIB_PATH.resolve(NATIVE_LIB_NAME);
    private static final String FIBJNI_CLASS_NAME = "FibJNI.java";
    private static final Path JAVA_SRC_DIR = Paths.get(System.getProperty("test.src"))
                                                  .resolve("src")
                                                  .resolve(MODULE_NAME_WITH_NATIVE);
    private static final Path FIBJNI_JAVA_CLASS = JAVA_SRC_DIR.resolve(FIBJNI_CLASS_NAME);
    private static final String DEBUG_EXTENSION = "debug";
    private static final long ORIG_LIB_FIB_SIZE = LIB_FIB_SRC.toFile().length();
    private static final String FAKE_OBJ_COPY_LOG_FILE = "objcopy.log";
    private static final String OBJCOPY_ONLY_DEBUG_SYMS_OPT = "-g";
    private static final String OBJCOPY_ONLY_KEEP_DEBUG_SYMS_OPT = "--only-keep-debug";
    private static final String OBJCOPY_ADD_DEBUG_LINK_OPT = "--add-gnu-debuglink";

    ///////////////////////////////////////////////////////////////////////////
    //
    // Tests which do NOT rely on objcopy being present on the test system
    //
    ///////////////////////////////////////////////////////////////////////////

    public void testPluginLoaded() {
        List<String> output =
            JLink.run("--list-plugins").output();
        if (output.stream().anyMatch(s -> s.contains(PLUGIN_NAME))) {
            System.out.println("DEBUG: " + PLUGIN_NAME + " plugin loaded as expected.");
        } else {
            throw new AssertionError("strip-native-debug-symbols plugin not in " +
                                     "--list-plugins output.");
        }
    }

    public void testConfigureFakeObjCopy() throws Exception {
        configureConflictingOptions();
        configureObjcopyWithOmit();
        configureObjcopyWithKeep();
        configureUnknownOptions();
        configureMultipleTimesSamePlugin();
        System.out.println("Test testConfigureFakeObjCopy() PASSED!");
    }

    private void configureMultipleTimesSamePlugin() throws Exception {
        Map<String, String> keepDebug = Map.of(
                StripNativeDebugSymbolsPlugin.NAME, "keep-debuginfo-files"
        );
        Map<String, String> excludeDebug = Map.of(
                StripNativeDebugSymbolsPlugin.NAME, "exclude-debuginfo-files"
        );
        StripNativeDebugSymbolsPlugin plugin = createAndConfigPlugin(keepDebug);
        try {
            plugin.doConfigure(false, excludeDebug);
            throw new AssertionError("should have thrown IAE for broken config: " +
                                     keepDebug + " and " + excludeDebug);
        } catch (IllegalArgumentException e) {
            // pass
            System.out.println("DEBUG: test threw IAE " + e.getMessage() +
                               " as expected.");
        }
    }

    private void configureUnknownOptions() throws Exception {
        Map<String, String> config = Map.of(
                StripNativeDebugSymbolsPlugin.NAME, "foobar"
        );
        doConfigureUnknownOption(config);
        config = Map.of(
                StripNativeDebugSymbolsPlugin.NAME, "keep-debuginfo-files",
                "foo", "bar" // unknown value
        );
        doConfigureUnknownOption(config);
    }

    private void doConfigureUnknownOption(Map<String, String> config) throws Exception {
        try {
            createAndConfigPlugin(config);
            throw new AssertionError("should have thrown IAE for broken config: " + config);
        } catch (IllegalArgumentException e) {
            // pass
            System.out.println("DEBUG: test threw IAE " + e.getMessage() +
                               " as expected.");
        }
    }

    private void configureObjcopyWithKeep() throws Exception {
        String objcopyPath = "foobar";
        String debugExt = "debuginfo"; // that's the default value
        Map<String, String> config = Map.of(
                StripNativeDebugSymbolsPlugin.NAME, "keep-debuginfo-files",
                "objcopy", objcopyPath
        );
        doKeepDebugInfoFakeObjCopyTest(config, debugExt, objcopyPath);
        // Do it again combining options the other way round
        debugExt = "testme";
        config = Map.of(
                StripNativeDebugSymbolsPlugin.NAME, "objcopy=" + objcopyPath,
                "keep-debuginfo-files", debugExt
        );
        doKeepDebugInfoFakeObjCopyTest(config, debugExt, objcopyPath);
        System.out.println("DEBUG: configureObjcopyWithKeep() PASSED!");
    }

    private void configureObjcopyWithOmit() throws Exception {
        String objcopyPath = "something-non-standard";
        Map<String, String> config = Map.of(
                StripNativeDebugSymbolsPlugin.NAME, "exclude-debuginfo-files",
                "objcopy", objcopyPath
        );
        doOmitDebugInfoFakeObjCopyTest(config, objcopyPath);
        System.out.println("DEBUG: configureObjcopyWithOmit() PASSED!");
    }

    private void configureConflictingOptions() throws Exception {
        Map<String, String> config = Map.of(
                StripNativeDebugSymbolsPlugin.NAME, "exclude-debuginfo-files",
                "keep-debuginfo-files", "foo-ext"
        );
        doConfigureConflictingOptions(config);
        config = Map.of(
                StripNativeDebugSymbolsPlugin.NAME, "exclude-debuginfo-files=bar",
                "keep-debuginfo-files", "foo-ext"
        );
        doConfigureConflictingOptions(config);
    }

    private void doConfigureConflictingOptions(Map<String, String> config) throws Exception {
        try {
            createAndConfigPlugin(config);
            throw new AssertionError("keep-debuginfo-files and exclude-debuginfo-files " +
                                     " should have conflicted!");
        } catch (IllegalArgumentException e) {
            // pass
            if (e.getMessage().contains("keep-debuginfo-files") &&
                    e.getMessage().contains("exclude-debuginfo-files")) {
                System.out.println("DEBUG: test threw IAE " + e.getMessage() +
                               " as expected.");
            } else {
                throw new AssertionError("Unexpected IAE", e);
            }
        }
    }

    public void testTransformFakeObjCopyNoDebugInfoFiles() throws Exception {
        Map<String, String> defaultConfig = Map.of(
                                 StripNativeDebugSymbolsPlugin.NAME, "exclude-debuginfo-files"
                                 );
        doOmitDebugInfoFakeObjCopyTest(defaultConfig, DEFAULT_OBJCOPY_CMD);
        System.out.println("testTransformFakeObjCopyNoDebugInfoFiles() PASSED!");
    }

    private void doOmitDebugInfoFakeObjCopyTest(Map<String, String> config,
                                                String expectedObjCopy) throws Exception {
        StripNativeDebugSymbolsPlugin plugin = createAndConfigPlugin(config, expectedObjCopy);
        String binFile = "mybin";
        String path = "/fib/bin/" + binFile;
        ResourcePoolEntry debugEntry = createMockEntry(path,
                                                       ResourcePoolEntry.Type.NATIVE_CMD);
        ResourcePoolManager inResources = new ResourcePoolManager();
        ResourcePoolManager outResources = new ResourcePoolManager();
        inResources.add(debugEntry);
        ResourcePool output = plugin.transform(
                                        inResources.resourcePool(),
                                        outResources.resourcePoolBuilder());
        // expect entry to be present
        if (output.findEntry(path).isPresent()) {
            System.out.println("DEBUG: File " + path + " present as exptected.");
        } else {
            throw new AssertionError("Test failed. Binary " + path +
                                     " not present after stripping!");
        }
        verifyFakeObjCopyCalled(binFile);
    }

    public void testTransformFakeObjCopyKeepDebugInfoFiles() throws Exception {
        Map<String, String> defaultConfig = Map.of(
                                 StripNativeDebugSymbolsPlugin.NAME,
                                 "keep-debuginfo-files=" + DEBUG_EXTENSION
                                 );
        doKeepDebugInfoFakeObjCopyTest(defaultConfig,
                                       DEBUG_EXTENSION,
                                       DEFAULT_OBJCOPY_CMD);
        System.out.println("testTransformFakeObjCopyKeepDebugInfoFiles() PASSED!");
    }

    private void doKeepDebugInfoFakeObjCopyTest(Map<String, String> config,
                                                String debugExt,
                                                String expectedObjCopy) throws Exception {
        StripNativeDebugSymbolsPlugin plugin = createAndConfigPlugin(config, expectedObjCopy);
        String sharedLib = "myLib.so";
        String path = "/fib/lib/" + sharedLib;
        ResourcePoolEntry debugEntry = createMockEntry(path,
                                                       ResourcePoolEntry.Type.NATIVE_LIB);
        ResourcePoolManager inResources = new ResourcePoolManager();
        ResourcePoolManager outResources = new ResourcePoolManager();
        inResources.add(debugEntry);
        ResourcePool output = plugin.transform(
                                        inResources.resourcePool(),
                                        outResources.resourcePoolBuilder());
        // expect entry + debug info entry to be present
        String debugPath = path + "." + debugExt;
        if (output.findEntry(path).isPresent() &&
            output.findEntry(debugPath).isPresent()) {
            System.out.println("DEBUG: Files " + path + "{,." + debugExt +
                               "} present as exptected.");
        } else {
            throw new AssertionError("Test failed. Binary files " + path +
                                     "{,." + debugExt +"} not present after " +
                                     "stripping!");
        }
        verifyFakeObjCopyCalledMultiple(sharedLib, debugExt);
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    // Tests which DO rely on objcopy being present on the test system.
    // Skipped otherwise.
    //
    ///////////////////////////////////////////////////////////////////////////

    public void testStripNativeLibraryDefaults() throws Exception {
        if (!hasJmods()) return;

        Path libFibJmod = createLibFibJmod();

        Path imageDir = Paths.get("stripped-native-libs");
        JLink.run("--output", imageDir.toString(),
                "--verbose",
                "--module-path", modulePathWith(libFibJmod),
                "--add-modules", MODULE_NAME_WITH_NATIVE,
                "--strip-native-debug-symbols=exclude-debuginfo-files").output();
        Path libDir = imageDir.resolve("lib");
        Path postStripLib = libDir.resolve(NATIVE_LIB_NAME);
        long postStripSize = postStripLib.toFile().length();

        if (postStripSize == 0) {
            throw new AssertionError("Lib file size 0. Test error?!");
        }
        // Heuristic: libLib.so is smaller post debug info stripping
        if (postStripSize >= ORIG_LIB_FIB_SIZE) {
            throw new AssertionError("Expected native library stripping to " +
                                     "reduce file size. Expected < " +
                                     ORIG_LIB_FIB_SIZE + ", got: " + postStripSize);
        } else {
            System.out.println("DEBUG: File size of " + postStripLib.toString() +
                    " " + postStripSize + " < " + ORIG_LIB_FIB_SIZE + " as expected." );
        }
        verifyFibModule(imageDir); // Sanity check fib module which got libFib.so stripped
        System.out.println("DEBUG: testStripNativeLibraryDefaults() PASSED!");
    }

    public void testOptionsInvalidObjcopy() throws Exception {
        if (!hasJmods()) return;

        Path libFibJmod = createLibFibJmod();

        String notExists = "/do/not/exist/objcopy";

        Path imageDir = Paths.get("invalid-objcopy-command");
        String[] jlinkCmdArray = new String[] {
                JAVA_HOME + File.separator + "bin" + File.separator + "jlink",
                "--output", imageDir.toString(),
                "--verbose",
                "--module-path", modulePathWith(libFibJmod),
                "--add-modules", MODULE_NAME_WITH_NATIVE,
                "--strip-native-debug-symbols", "objcopy=" + notExists,
        };
        List<String> jlinkCmd = Arrays.asList(jlinkCmdArray);
        System.out.println("Debug: command: " + jlinkCmd.stream().collect(
                                                    Collectors.joining(" ")));
        ProcessBuilder builder = new ProcessBuilder(jlinkCmd);
        Process p = builder.start();
        int status = p.waitFor();
        if (status == 0) {
            throw new AssertionError("Expected jlink to fail!");
        } else {
            verifyInvalidObjcopyError(p.getInputStream(), notExists);
            System.out.println("DEBUG: testOptionsInvalidObjcopy() PASSED!");
        }
    }

    public void testStripNativeLibsDebugSymsIncluded() throws Exception {
        if (!hasJmods()) return;

        Path libFibJmod = createLibFibJmod();

        Path imageDir = Paths.get("stripped-native-libs-with-debug");
        JLink.run("--output", imageDir.toString(),
                "--verbose",
                "--module-path", modulePathWith(libFibJmod),
                "--add-modules", MODULE_NAME_WITH_NATIVE,
                "--strip-native-debug-symbols",
                "keep-debuginfo-files=" + DEBUG_EXTENSION);

        Path libDir = imageDir.resolve("lib");
        Path postStripLib = libDir.resolve(NATIVE_LIB_NAME);
        long postStripSize = postStripLib.toFile().length();

        if (postStripSize == 0) {
            throw new AssertionError("Lib file size 0. Test error?!");
        }
        // Heuristic: libLib.so is smaller post debug info stripping
        if (postStripSize >= ORIG_LIB_FIB_SIZE) {
            throw new AssertionError("Expected native library stripping to " +
                                     "reduce file size. Expected < " +
                                     ORIG_LIB_FIB_SIZE + ", got: " + postStripSize);
        } else {
            System.out.println("DEBUG: File size of " + postStripLib.toString() +
                    " " + postStripSize + " < " + ORIG_LIB_FIB_SIZE + " as expected." );
        }
        // stripped with option to preserve debug symbols file
        verifyDebugInfoSymbolFilePresent(imageDir);
        System.out.println("DEBUG: testStripNativeLibsDebugSymsIncluded() PASSED!");
    }

    private void verifyFakeObjCopyCalledMultiple(String expectedFile,
                                                 String dbgExt) throws Exception {
        // transform of the StripNativeDebugSymbolsPlugin created objcopy.log
        // with our stubbed FakeObjCopy. See FakeObjCopy.java
        List<String> allLines = Files.readAllLines(Paths.get(FAKE_OBJ_COPY_LOG_FILE));
        if (allLines.size() != 3) {
            throw new AssertionError("Expected 3 calls to objcopy");
        }
        // 3 calls to objcopy are as follows:
        //    1. Only keep debug symbols
        //    2. Strip debug symbols
        //    3. Add debug link to stripped file
        String onlyKeepDebug = allLines.get(0);
        String stripSymbolsLine = allLines.get(1);
        String addGnuDebugLink = allLines.get(2);
        System.out.println("DEBUG: Inspecting fake objcopy calls: " + allLines);
        boolean passed = stripSymbolsLine.startsWith(OBJCOPY_ONLY_DEBUG_SYMS_OPT);
        passed &= stripSymbolsLine.endsWith(expectedFile);
        String[] tokens = onlyKeepDebug.split("\\s");
        passed &= tokens[0].equals(OBJCOPY_ONLY_KEEP_DEBUG_SYMS_OPT);
        passed &= tokens[1].endsWith(expectedFile);
        passed &= tokens[2].endsWith(expectedFile + "." + dbgExt);
        tokens = addGnuDebugLink.split("\\s");
        String[] addDbgTokens = tokens[0].split("=");
        passed &= addDbgTokens[1].equals(expectedFile + "." + dbgExt);
        passed &= addDbgTokens[0].equals(OBJCOPY_ADD_DEBUG_LINK_OPT);
        passed &= tokens[1].endsWith(expectedFile);
        if (!passed) {
            throw new AssertionError("Test failed! objcopy not properly called " +
                                     "with expected options!");
        }
    }

    private void verifyFakeObjCopyCalled(String expectedFile) throws Exception {
        // transform of the StripNativeDebugSymbolsPlugin created objcopy.log
        // with our stubbed FakeObjCopy. See FakeObjCopy.java
        List<String> allLines = Files.readAllLines(Paths.get(FAKE_OBJ_COPY_LOG_FILE));
        if (allLines.size() != 1) {
            throw new AssertionError("Expected 1 call to objcopy only");
        }
        String optionLine = allLines.get(0);
        System.out.println("DEBUG: Inspecting fake objcopy arguments: " + optionLine);
        boolean passed = optionLine.startsWith(OBJCOPY_ONLY_DEBUG_SYMS_OPT);
        passed &= optionLine.endsWith(expectedFile);
        if (!passed) {
            throw new AssertionError("Test failed! objcopy not called with " +
                                     "expected options!");
        }
    }

    private ResourcePoolEntry createMockEntry(String path,
                                              ResourcePoolEntry.Type type) {
        byte[] mockContent = new byte[] { 0, 1, 2, 3 };
        ResourcePoolEntry entry = ResourcePoolEntry.create(
                path,
                type,
                mockContent);
        return entry;
    }

    private StripNativeDebugSymbolsPlugin createAndConfigPlugin(
                                            Map<String, String> config,
                                            String expectedObjcopy)
                                            throws IOException {
        TestObjCopyCmdBuilder cmdBuilder = new TestObjCopyCmdBuilder(expectedObjcopy);
        return createAndConfigPlugin(config, cmdBuilder);
    }

    private StripNativeDebugSymbolsPlugin createAndConfigPlugin(
            Map<String, String> config) throws IOException {
        TestObjCopyCmdBuilder cmdBuilder = new TestObjCopyCmdBuilder();
        return createAndConfigPlugin(config, cmdBuilder);
    }

    private StripNativeDebugSymbolsPlugin createAndConfigPlugin(
                                Map<String, String> config,
                                TestObjCopyCmdBuilder builder) throws IOException {
        StripNativeDebugSymbolsPlugin plugin =
                                     new StripNativeDebugSymbolsPlugin(builder);
        plugin.doConfigure(false, config);
        return plugin;
    }

    // Create the jmod with the native library
    private Path createLibFibJmod() throws IOException {
        JmodFileBuilder jmodBuilder = new JmodFileBuilder(MODULE_NAME_WITH_NATIVE);
        jmodBuilder.javaClass(FIBJNI_JAVA_CLASS);
        jmodBuilder.nativeLib(LIB_FIB_SRC);
        return jmodBuilder.build();
    }

    private String modulePathWith(Path jmod) {
        return Paths.get(JAVA_HOME, "jmods").toString() +
                    File.pathSeparator + jmod.getParent().toString();
    }

    private boolean hasJmods() {
        if (!Files.exists(Paths.get(JAVA_HOME, "jmods"))) {
            System.err.println("Test skipped. NO jmods directory");
            return false;
        }
        return true;
    }

    private void verifyInvalidObjcopyError(InputStream errInput, String match) {
        boolean foundMatch = false;
        try (Scanner scanner = new Scanner(errInput)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                System.out.println("DEBUG: >>>> " + line);
                if (line.contains(match)) {
                    foundMatch = true;
                    break;
                }
            }
        }
        if (!foundMatch) {
            throw new AssertionError("Expected to find " + match +
                                    " in error stream.");
        } else {
            System.out.println("DEBUG: Found string " + match + " as expected.");
        }
    }

    private void verifyDebugInfoSymbolFilePresent(Path image)
                                    throws IOException, InterruptedException {
        Path debugSymsFile = image.resolve("lib/libFib.so.debug");
        if (!Files.exists(debugSymsFile)) {
            throw new AssertionError("Expected stripped debug info file " +
                                        debugSymsFile.toString() + " to exist.");
        }
        long debugSymsSize = debugSymsFile.toFile().length();
        if (debugSymsSize <= 0) {
            throw new AssertionError("sanity check for fib.FibJNI failed " +
                                     "post-stripping!");
        } else {
            System.out.println("DEBUG: Debug symbols stripped from libFib.so " +
                               "present (" + debugSymsFile.toString() + ") as expected.");
        }
    }

    private void verifyFibModule(Path image)
                                throws IOException, InterruptedException {
        System.out.println("DEBUG: sanity checking fib module...");
        Path launcher = image.resolve("bin/java");
        List<String> args = new ArrayList<>();
        args.add(launcher.toString());
        args.add("--add-modules");
        args.add(MODULE_NAME_WITH_NATIVE);
        args.add("fib.FibJNI");
        args.add("7");
        args.add("13"); // fib(7) == 13
        System.out.println("DEBUG: [command] " +
                                args.stream().collect(Collectors.joining(" ")));
        Process proc = new ProcessBuilder(args).inheritIO().start();
        int status = proc.waitFor();
        if (status == 0) {
            System.out.println("DEBUG: sanity checking fib module... PASSED!");
        } else {
            throw new AssertionError("sanity check for fib.FibJNI failed post-" +
                                     "stripping!");
        }
    }

    private static boolean isObjcopyPresent() throws Exception {
        String[] objcopyVersion = new String[] {
                OBJCOPY, "--version",
        };
        List<String> command = Arrays.asList(objcopyVersion);
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.inheritIO();
            Process p = builder.start();
            int status = p.waitFor();
            if (status != 0) {
                System.out.println("Debug: objcopy binary doesn't seem to be " +
                                   "present or functional.");
                return false;
            }
        } catch (IOException e) {
            System.out.println("Debug: objcopy binary doesn't seem to be present " +
                               "or functional.");
            return false;
        }
        return true;
    }

    public static void main(String[] args) throws Exception {
        StripNativeDebugSymbolsPluginTest test = new StripNativeDebugSymbolsPluginTest();
        if (isObjcopyPresent()) {
            test.testStripNativeLibraryDefaults();
            test.testStripNativeLibsDebugSymsIncluded();
            test.testOptionsInvalidObjcopy();
        } else {
            System.out.println("DEBUG: objcopy binary not available. " +
                               "Running reduced set of tests.");
        }
        test.testTransformFakeObjCopyNoDebugInfoFiles();
        test.testTransformFakeObjCopyKeepDebugInfoFiles();
        test.testConfigureFakeObjCopy();
        test.testPluginLoaded();
    }

    static class JLink {
        static final ToolProvider JLINK_TOOL = ToolProvider.findFirst("jlink")
            .orElseThrow(() ->
                new RuntimeException("jlink tool not found")
            );

        static JLink run(String... options) {
            JLink jlink = new JLink();
            if (jlink.execute(options) != 0) {
                throw new AssertionError("Jlink expected to exit with 0 return code");
            }
            return jlink;
        }

        final List<String> output = new ArrayList<>();
        private int execute(String... options) {
            System.out.println("jlink " +
                Stream.of(options).collect(Collectors.joining(" ")));

            StringWriter writer = new StringWriter();
            PrintWriter pw = new PrintWriter(writer);
            int rc = JLINK_TOOL.run(pw, pw, options);
            System.out.println(writer.toString());
            Stream.of(writer.toString().split("\\v"))
                  .map(String::trim)
                  .forEach(output::add);
            return rc;
        }

        boolean contains(String s) {
            return output.contains(s);
        }

        List<String> output() {
            return output;
        }
    }

    /**
     * Builder to create JMOD file
     */
    private static class JmodFileBuilder {

        private static final ToolProvider JMOD_TOOL = ToolProvider
                .findFirst("jmod")
                .orElseThrow(() ->
                    new RuntimeException("jmod tool not found")
                );
        private static final Path SRC_DIR = Paths.get("src");
        private static final Path MODS_DIR = Paths.get("mod");
        private static final Path JMODS_DIR = Paths.get("jmods");
        private static final Path LIBS_DIR = Paths.get("libs");

        private final String name;
        private final List<Path> nativeLibs = new ArrayList<>();
        private final List<Path> javaClasses = new ArrayList<>();

        private JmodFileBuilder(String name) throws IOException {
            this.name = name;

            deleteDirectory(MODS_DIR);
            deleteDirectory(SRC_DIR);
            deleteDirectory(LIBS_DIR);
            deleteDirectory(JMODS_DIR);
            Path msrc = SRC_DIR.resolve(name);
            if (Files.exists(msrc)) {
                deleteDirectory(msrc);
            }
        }

        JmodFileBuilder nativeLib(Path libFileSrc) {
            nativeLibs.add(libFileSrc);
            return this;
        }

        JmodFileBuilder javaClass(Path srcPath) {
            javaClasses.add(srcPath);
            return this;
        }

        Path build() throws IOException {
            compileModule();
            return createJmodFile();
        }

        private void compileModule() throws IOException  {
            Path msrc = SRC_DIR.resolve(name);
            Files.createDirectories(msrc);
            // copy class using native lib to expected path
            if (javaClasses.size() > 0) {
                for (Path srcPath: javaClasses) {
                    Path targetPath = msrc.resolve(srcPath.getFileName());
                    Files.copy(srcPath, targetPath);
                }
            }
            // generate module-info file.
            Path minfo = msrc.resolve("module-info.java");
            try (BufferedWriter bw = Files.newBufferedWriter(minfo);
                 PrintWriter writer = new PrintWriter(bw)) {
                writer.format("module %s { }%n", name);
            }

            if (!CompilerUtils.compile(msrc, MODS_DIR,
                                             "--module-source-path",
                                             SRC_DIR.toString())) {

            }
        }

        private Path createJmodFile() throws IOException {
            Path mclasses = MODS_DIR.resolve(name);
            Files.createDirectories(JMODS_DIR);
            Path outfile = JMODS_DIR.resolve(name + ".jmod");
            List<String> args = new ArrayList<>();
            args.add("create");
            // add classes
            args.add("--class-path");
            args.add(mclasses.toString());
            // native libs
            if (nativeLibs.size() > 0) {
                // Copy the JNI library to the expected path
                Files.createDirectories(LIBS_DIR);
                for (Path srcLib: nativeLibs) {
                    Path targetLib = LIBS_DIR.resolve(srcLib.getFileName());
                    Files.copy(srcLib, targetLib);
                }
                args.add("--libs");
                args.add(LIBS_DIR.toString());
            }
            args.add(outfile.toString());

            if (Files.exists(outfile)) {
                Files.delete(outfile);
            }

            System.out.println("jmod " +
                args.stream().collect(Collectors.joining(" ")));

            int rc = JMOD_TOOL.run(System.out, System.out,
                                   args.toArray(new String[args.size()]));
            if (rc != 0) {
                throw new AssertionError("jmod failed: rc = " + rc);
            }
            return outfile;
        }

        private static void deleteDirectory(Path dir) throws IOException {
            try {
                Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file,
                                                     BasicFileAttributes attrs)
                        throws IOException
                    {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir,
                                                              IOException exc)
                        throws IOException
                    {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (NoSuchFileException e) {
                // ignore non-existing files
            }
        }
    }

    private static class TestObjCopyCmdBuilder implements ObjCopyCmdBuilder {

        private final String expectedObjCopy;
        private final String logFile;

        TestObjCopyCmdBuilder() {
            this(DEFAULT_OBJCOPY_CMD);
        }
        TestObjCopyCmdBuilder(String exptectedObjCopy) {
            Path logFilePath = Paths.get(FAKE_OBJ_COPY_LOG_FILE);
            try {
                Files.deleteIfExists(logFilePath);
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.logFile = logFilePath.toFile().getAbsolutePath();
            this.expectedObjCopy = exptectedObjCopy;
        }

        @Override
        public List<String> build(String objCopy, String... options) {
            if (!expectedObjCopy.equals(objCopy)) {
                throw new AssertionError("Expected objcopy to be '" +
                                         expectedObjCopy + "' but was '" +
                                         objCopy);
            }
            List<String> fakeObjCopy = new ArrayList<>();
            fakeObjCopy.add(JAVA_HOME + File.separator + "bin" + File.separator + "java");
            fakeObjCopy.add("-cp");
            fakeObjCopy.add(System.getProperty("test.classes"));
            fakeObjCopy.add("FakeObjCopy");
            // Note that adding the gnu debug link changes the PWD of the
            // java process calling FakeObjCopy. As such we need to pass in the
            // log file path this way. Relative paths won't work as it would be
            // relative to the temporary directory which gets deleted post
            // adding the debug link
            fakeObjCopy.add(logFile);
            if (options.length > 0) {
                fakeObjCopy.addAll(Arrays.asList(options));
            }
            return fakeObjCopy;
        }

    }
}
