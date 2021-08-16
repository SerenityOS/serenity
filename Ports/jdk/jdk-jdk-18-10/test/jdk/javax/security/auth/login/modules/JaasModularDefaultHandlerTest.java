/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.io.File;
import java.io.OutputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Builder;
import jdk.internal.module.ModuleInfoWriter;
import java.util.stream.Stream;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.util.JarUtils;

/*
 * @test
 * @bug 8151654 8183310
 * @summary Test default callback handler with all possible modular option.
 * @library /test/lib
 * @modules java.base/jdk.internal.module
 * @build jdk.test.lib.util.JarUtils
 * @build TestCallbackHandler TestLoginModule JaasClientWithDefaultHandler
 * @run main JaasModularDefaultHandlerTest
 */
public class JaasModularDefaultHandlerTest {

    private static final Path SRC = Paths.get(System.getProperty("test.src"));
    private static final Path TEST_CLASSES
            = Paths.get(System.getProperty("test.classes"));
    private static final Path ARTIFACT_DIR = Paths.get("jars");
    private static final String PS = File.pathSeparator;
    private static final String H_TYPE = "handler.TestCallbackHandler";
    private static final String C_TYPE = "login.JaasClientWithDefaultHandler";

    /**
     * Here is the naming convention followed for each jar.
     * h.jar   - Unnamed handler jar.
     * mh.jar  - Modular handler jar.
     * c.jar   - Unnamed client jar.
     * mc.jar  - Modular client jar.
     * amc.jar - Modular client used for automatic handler jar.
     */
    private static final Path H_JAR = artifact("h.jar");
    private static final Path MH_JAR = artifact("mh.jar");
    private static final Path C_JAR = artifact("c.jar");
    private static final Path MC_JAR = artifact("mc.jar");
    private static final Path AMC_JAR = artifact("amc.jar");

    private final String unnH;
    private final String modH;
    private final String unnC;
    private final String modC;
    private final String autoMC;
    // Common set of VM arguments used in all test cases
    private final List<String> commonArgs;

    public JaasModularDefaultHandlerTest() {

        List<String> argList = new LinkedList<>();
        argList.add("-Djava.security.auth.login.config="
                + toAbsPath(SRC.resolve("jaas.conf")));
        commonArgs = Collections.unmodifiableList(argList);

        // Based on Testcase, select unnamed/modular jar files to use.
        unnH = toAbsPath(H_JAR);
        modH = toAbsPath(MH_JAR);
        unnC = toAbsPath(C_JAR);
        modC = toAbsPath(MC_JAR);
        autoMC = toAbsPath(AMC_JAR);
    }

    /*
     * Test cases are based on the following logic,
     * for (clientType : {"NAMED", "AUTOMATIC", "UNNAMED"}) {
     *     for (handlerType : {"NAMED", "AUTOMATIC", "UNNAMED"}) {
     *         Create and run java command for each possible case
     *     }
     * }
     */
    public static void main(String[] args) throws Exception {

        // Generates unnamed and modular jars.
        setUp();
        JaasModularDefaultHandlerTest jt = new JaasModularDefaultHandlerTest();
        jt.process();
    }

    private void process() throws Exception {

        // Case: NAMED-NAMED, NAMED-AUTOMATIC, NAMED-UNNAMED
        System.out.println("Case: Modular Client and Modular Handler");
        execute(String.format("--module-path %s%s%s -m mc/%s %s",
                modC, PS, modH, C_TYPE, H_TYPE));
        System.out.println("Case: Modular Client and automatic Handler");
        execute(String.format("--module-path %s%s%s --add-modules=h -m mc/%s %s",
                autoMC, PS, unnH, C_TYPE, H_TYPE));
        System.out.println("Case: Modular Client and unnamed Handler");
        execute(String.format("--module-path %s -cp %s -m mc/%s %s", autoMC,
                unnH, C_TYPE, H_TYPE));

        // Case: AUTOMATIC-NAMED, AUTOMATIC-AUTOMATIC, AUTOMATIC-UNNAMED
        System.out.println("Case: Automatic Client and modular Handler");
        execute(String.format("--module-path %s%s%s --add-modules=mh -m c/%s %s",
                unnC, PS, modH, C_TYPE, H_TYPE));
        System.out.println("Case: Automatic Client and automatic Handler");
        execute(String.format("--module-path %s%s%s --add-modules=h -m c/%s %s",
                unnC, PS, unnH, C_TYPE, H_TYPE));
        System.out.println("Case: Automatic Client and unnamed Handler");
        execute(String.format("--module-path %s -cp %s -m c/%s %s", unnC,
                unnH, C_TYPE, H_TYPE));

        // Case: UNNAMED-NAMED, UNNAMED-AUTOMATIC, UNNAMED-UNNAMED
        System.out.println("Case: Unnamed Client and modular Handler");
        execute(String.format("-cp %s --module-path %s --add-modules=mh %s %s",
                unnC, modH, C_TYPE, H_TYPE));
        System.out.println("Case: Unnamed Client and automatic Handler");
        execute(String.format("-cp %s --module-path %s --add-modules=h %s %s",
                unnC, unnH, C_TYPE, H_TYPE));
        System.out.println("Case: Unnamed Client and unnamed Handler");
        execute(String.format("-cp %s%s%s %s %s", unnC, PS, unnH, C_TYPE,
                H_TYPE));

        // Case: unnamed jars in --module-path and modular jars in -cp.
        System.out.println("Case: Unnamed Client and Handler in modulepath");
        execute(String.format("--module-path %s%s%s --add-modules=h -m c/%s %s",
                unnC, PS, unnH, C_TYPE, H_TYPE));
        System.out.println("Case: Modular Client and Provider in classpath");
        execute(String.format("-cp %s%s%s %s %s",
                modC, PS, modH, C_TYPE, H_TYPE));
    }

    /**
     * Execute with command arguments and process the result.
     */
    private void execute(String args) throws Exception {

        String[] safeArgs = Stream.concat(commonArgs.stream(),
                Stream.of(args.split("\\s+"))).filter(s -> {
            if (s.contains(" ")) {
                throw new RuntimeException("No spaces in args");
            }
            return !s.isEmpty();
        }).toArray(String[]::new);
        OutputAnalyzer out = ProcessTools.executeTestJvm(safeArgs);
        // Handle response.
        if (out.getExitValue() != 0) {
            System.out.printf("OUTPUT: %s", out.getOutput());
            throw new RuntimeException("FAIL: Unknown failure occured.");
        } else {
            System.out.println("Passed.");
        }
    }

    /**
     * Creates Unnamed/modular jar files for TestClient and TestClassLoader.
     */
    private static void setUp() throws Exception {

        if (ARTIFACT_DIR.toFile().exists()) {
            System.out.println("Skipping setup: Artifacts already exists.");
            return;
        }
        // Generate unnamed handler jar file.
        JarUtils.createJarFile(H_JAR, TEST_CLASSES,
                "handler/TestCallbackHandler.class");
        // Generate unnamed client jar file.
        JarUtils.createJarFile(C_JAR, TEST_CLASSES,
                "login/TestLoginModule.class",
                "login/JaasClientWithDefaultHandler.class");

        Builder mBuilder = ModuleDescriptor.newModule("mh");
        // Modular jar exports package to let the handler type accessible.
        generateJar(H_JAR, MH_JAR, mBuilder.exports("handler").build());

        mBuilder = ModuleDescriptor.newModule("mc").exports("login")
                .requires("jdk.security.auth");
        // Generate modular client jar file to use automatic handler jar.
        generateJar(C_JAR, AMC_JAR, mBuilder.build());
        // Generate modular client jar file to use modular handler jar.
        generateJar(C_JAR, MC_JAR, mBuilder.requires("mh").build());
    }

    /**
     * Update Unnamed jars and include module descriptor files.
     */
    private static void generateJar(Path sjar, Path djar,
            ModuleDescriptor mDesc) throws Exception {

        Files.copy(sjar, djar, StandardCopyOption.REPLACE_EXISTING);
        Path dir = Files.createTempDirectory("tmp");
        if (mDesc != null) {
            Path mi = dir.resolve("module-info.class");
            try (OutputStream out = Files.newOutputStream(mi)) {
                ModuleInfoWriter.write(mDesc, out);
            }
            System.out.format("Added 'module-info.class' in '%s'%n", djar);
        }
        JarUtils.updateJarFile(djar, dir);
    }

    /**
     * Look for file path in generated jars.
     */
    private static Path artifact(String file) {
        return ARTIFACT_DIR.resolve(file);
    }

    /**
     * Convert to absolute file path.
     */
    private static String toAbsPath(Path path) {
        return path.toFile().getAbsolutePath();
    }
}
