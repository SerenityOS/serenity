/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Arrays;
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
 * @bug 8078813 8183310
 * @summary Test custom JAAS login module with all possible modular option.
 * @library /test/lib
 * @modules java.base/jdk.internal.module
 * @build jdk.test.lib.util.JarUtils
 * @build TestLoginModule JaasClient
 * @run main JaasModularClientTest false
 * @run main JaasModularClientTest true
 */
public class JaasModularClientTest {

    private static final Path SRC = Paths.get(System.getProperty("test.src"));
    private static final Path TEST_CLASSES
            = Paths.get(System.getProperty("test.classes"));
    private static final Path ARTIFACT_DIR = Paths.get("jars");
    private static final String PS = File.pathSeparator;
    private static final String L_TYPE = "login.TestLoginModule";
    private static final String C_TYPE = "client.JaasClient";

    /**
     * Here is the naming convention followed.
     * l.jar    - Unnamed login module jar.
     * ml.jar   - Modular login module jar.
     * msl.jar  - Modular login module jar provides login module service
     *            through module-info
     * c.jar    - Unnamed client jar.
     * mc.jar   - Modular client jar.
     * mcs.jar  - Modular client jar uses login module service through
     *            module-info.
     * amc.jar  - Modular client used for automatic login module jar.
     * amcs.jar - Modular client used for automatic login module jar and uses
     *            login module service through module-info.
     */
    private static final Path L_JAR = artifact("l.jar");
    private static final Path ML_JAR = artifact("ml.jar");
    private static final Path MSL_JAR = artifact("msl.jar");
    private static final Path C_JAR = artifact("c.jar");
    private static final Path MC_JAR = artifact("mc.jar");
    private static final Path MCS_JAR = artifact("mcs.jar");
    private static final Path AMC_JAR = artifact("amc.jar");
    private static final Path AMCS_JAR = artifact("amcs.jar");

    private final String unnL;
    private final String modL;
    private final String unnC;
    private final String modC;
    private final String autoMC;
    // Common set of VM arguments used in all test cases
    private final List<String> commonArgs;

    public JaasModularClientTest(boolean service) {

        System.out.printf("%n*** Login Module defined as service in "
                + "module-info: %s ***%n%n", service);
        List<String> argList = new LinkedList<>();
        argList.add("-Djava.security.auth.login.config="
                + toAbsPath(SRC.resolve("jaas.conf")));
        commonArgs = Collections.unmodifiableList(argList);

        // Based on Testcase, select unnamed/modular jar files to use.
        unnL = toAbsPath(L_JAR);
        modL = toAbsPath(service ? MSL_JAR : ML_JAR);
        unnC = toAbsPath(C_JAR);
        modC = toAbsPath(service ? MCS_JAR : MC_JAR);
        autoMC = toAbsPath(service ? AMCS_JAR : AMC_JAR);
    }

    /*
     * Test cases are based on the following logic,
     * for (definedAs : {"Service in module-info", "Class Type"}) {
     *     for (clientType : {"NAMED", "AUTOMATIC", "UNNAMED"}) {
     *         for (loginModuleType : {"NAMED", "AUTOMATIC", "UNNAMED"}) {
     *             Create and run java command for each possible case
     *         }
     *     }
     * }
     */
    public static void main(String[] args) throws Exception {

        // Generates unnamed and modular jars.
        setUp();
        boolean service = Boolean.valueOf(args[0]);
        JaasModularClientTest test = new JaasModularClientTest(service);
        test.process();
    }

    private void process() throws Exception {

        // Case: NAMED-NAMED, NAMED-AUTOMATIC, NAMED-UNNAMED
        System.out.println("Case: Modular Client and Modular Login module.");
        execute(String.format("--module-path %s%s%s -m mc/%s",
                modC, PS, modL, C_TYPE));
        System.out.println("Case: Modular Client and automatic Login module.");
        execute(String.format("--module-path %s%s%s --add-modules=l -m mc/%s",
                autoMC, PS, unnL, C_TYPE));
        System.out.println("Case: Modular Client and unnamed Login module.");
        execute(String.format("--module-path %s -cp %s -m mc/%s", autoMC,
                unnL, C_TYPE));

        // Case: AUTOMATIC-NAMED, AUTOMATIC-AUTOMATIC, AUTOMATIC-UNNAMED
        System.out.println("Case: Automatic Client and modular Login module.");
        execute(String.format("--module-path %s%s%s --add-modules=ml -m c/%s",
                unnC, PS, modL, C_TYPE));
        System.out.println("Case: Automatic Client and automatic Login module");
        execute(String.format("--module-path %s%s%s --add-modules=l -m c/%s",
                unnC, PS, unnL, C_TYPE));
        System.out.println("Case: Automatic Client and unnamed Login module.");
        execute(String.format("--module-path %s -cp %s -m c/%s", unnC,
                unnL, C_TYPE));

        // Case: UNNAMED-NAMED, UNNAMED-AUTOMATIC, UNNAMED-UNNAMED
        System.out.println("Case: Unnamed Client and modular Login module.");
        execute(String.format("-cp %s --module-path %s --add-modules=ml %s",
                unnC, modL, C_TYPE));
        System.out.println("Case: Unnamed Client and automatic Login module.");
        execute(String.format("-cp %s --module-path %s --add-modules=l %s",
                unnC, unnL, C_TYPE));
        System.out.println("Case: Unnamed Client and unnamed Login module.");
        execute(String.format("-cp %s%s%s %s", unnC, PS, unnL, C_TYPE));

        // Case: unnamed jars in --module-path and modular jars in -cp.
        System.out.println(
                "Case: Unnamed Client and Login module from modulepath.");
        execute(String.format("--module-path %s%s%s --add-modules=l -m c/%s",
                unnC, PS, unnL, C_TYPE));
        System.out.println(
                "Case: Modular Client and Login module in classpath.");
        execute(String.format("-cp %s%s%s %s", modC, PS, modL, C_TYPE));
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
        // Generate unnamed login module jar file.
        JarUtils.createJarFile(L_JAR, TEST_CLASSES,
                "login/TestLoginModule.class");
        // Generate unnamed client jar.
        JarUtils.createJarFile(C_JAR, TEST_CLASSES, "client/JaasClient.class",
                "client/JaasClient$MyCallbackHandler.class");

        Builder mBuilder = ModuleDescriptor.newModule("ml")
                .requires("jdk.security.auth");
        // Modular jar exports package to let the login module type accessible.
        generateJar(L_JAR, ML_JAR, mBuilder.exports("login").build());

        mBuilder = ModuleDescriptor.newModule("ml")
                .requires("jdk.security.auth")
                .provides("javax.security.auth.spi.LoginModule",
                        Arrays.asList(L_TYPE));
        // Modular login module as Service in module-info does not need to
        // export service package.
        generateJar(L_JAR, MSL_JAR, mBuilder.build());

        mBuilder = ModuleDescriptor.newModule("mc").exports("client")
                .requires("jdk.security.auth");
        // Generate modular client jar to use automatic login module jar.
        generateJar(C_JAR, AMC_JAR, mBuilder.build());
        // Generate modular client jar to use modular login module jar.
        generateJar(C_JAR, MC_JAR, mBuilder.requires("ml").build());

        mBuilder = ModuleDescriptor.newModule("mc").exports("client")
                .requires("jdk.security.auth")
                .uses("javax.security.auth.spi.LoginModule");
        // Generate modular client jar to use automatic login module service.
        generateJar(C_JAR, AMCS_JAR, mBuilder.build());
        // Generate modular client jar using modular login module service.
        generateJar(C_JAR, MCS_JAR, mBuilder.requires("ml").build());
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
