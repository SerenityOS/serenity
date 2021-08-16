/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8168423
 * @summary Different types of ClassLoader running with(out) SecurityManager and
 *          (in)valid security policy file.
 * @library /test/lib
 * @modules java.base/jdk.internal.module
 * @build jdk.test.lib.util.JarUtils
 * @build TestClassLoader TestClient
 * @run main ClassLoaderTest -noPolicy
 * @run main ClassLoaderTest -validPolicy
 * @run main ClassLoaderTest -invalidPolicy
 * @run main ClassLoaderTest -noPolicy      -customSCL
 * @run main ClassLoaderTest -validPolicy   -customSCL
 * @run main ClassLoaderTest -invalidPolicy -customSCL
 */
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;
import java.lang.module.ModuleDescriptor;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import jdk.internal.module.ModuleInfoWriter;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.util.JarUtils;

public class ClassLoaderTest {

    private static final String SRC = System.getProperty("test.src");
    private static final Path TEST_CLASSES =
            Paths.get(System.getProperty("test.classes"));
    private static final Path ARTIFACT_DIR = Paths.get("jars");
    private static final Path VALID_POLICY = Paths.get(SRC, "valid.policy");
    private static final Path INVALID_POLICY
            = Paths.get(SRC, "malformed.policy");
    /*
     * Here is the naming convention followed for each jar.
     * cl.jar   - Regular custom class loader jar.
     * mcl.jar  - Modular custom class loader jar.
     * c.jar    - Regular client jar.
     * mc.jar   - Modular client jar.
     * amc.jar  - Modular client referring automated custom class loader jar.
     */
    private static final Path CL_JAR = ARTIFACT_DIR.resolve("cl.jar");
    private static final Path MCL_JAR = ARTIFACT_DIR.resolve("mcl.jar");
    private static final Path C_JAR = ARTIFACT_DIR.resolve("c.jar");
    private static final Path MC_JAR = ARTIFACT_DIR.resolve("mc.jar");
    private static final Path AMC_JAR = ARTIFACT_DIR.resolve("amc.jar");

    // Expected output messages
    private static final String MISSING_MODULE =
            "Module cl not found, required by mc";
    private static final String POLICY_ERROR =
            "java.security.policy: error parsing file";
    private static final String SYSTEM_CL_MSG =
            "jdk.internal.loader.ClassLoaders$AppClassLoader";
    private static final String CUSTOM_CL_MSG = "cl.TestClassLoader";

    // Member vars
    private final boolean useSCL;       // Use default system loader, or custom
    private final String smMsg;         // Security manager message, or ""
    private final String autoAddModArg; // Flag to add cl modules, or ""
    private final String addmodArg;     // Flag to add mcl modules, or ""
    private final String expectedStatus;// Expected exit status from client
    private final String expectedMsg;   // Expected output message from client

    // Common set of VM arguments used in all test cases
    private final List<String> commonArgs;

    public ClassLoaderTest(Path policy, boolean useSCL) {
        this.useSCL = useSCL;

        List<String> argList = new LinkedList<>();
        argList.add("-Duser.language=en");
        argList.add("-Duser.region=US");

        boolean malformedPolicy = false;
        if (policy == null) {
            smMsg = "Without SecurityManager";
        } else {
            malformedPolicy = policy.equals(INVALID_POLICY);
            argList.add("-Djava.security.manager");
            argList.add("-Djava.security.policy=" +
                    policy.toFile().getAbsolutePath());
            smMsg = "With SecurityManager";
        }

        if (useSCL) {
            autoAddModArg = "";
            addmodArg = "";
        } else {
            argList.add("-Djava.system.class.loader=cl.TestClassLoader");
            autoAddModArg = "--add-modules=cl";
            addmodArg = "--add-modules=mcl";
        }

        if (malformedPolicy) {
            expectedStatus = "FAIL";
            expectedMsg = POLICY_ERROR;
        } else if (useSCL) {
            expectedStatus = "PASS";
            expectedMsg = SYSTEM_CL_MSG;
        } else {
            expectedStatus = "PASS";
            expectedMsg = CUSTOM_CL_MSG;
        }
        commonArgs = Collections.unmodifiableList(argList);
    }

    public static void main(String[] args) throws Exception {
        Path policy;
        if (args[0].equals("-noPolicy")) {
            policy = null;
        } else if (args[0].equals("-validPolicy")) {
            policy = VALID_POLICY;
        } else if (args[0].equals("-invalidPolicy")) {
            policy = INVALID_POLICY;
        } else {
            throw new RuntimeException("Unknown policy arg: " + args[0]);
        }

        boolean useSystemLoader = true;
        if (args.length > 1) {
            if (args[1].equals("-customSCL")) {
                useSystemLoader = false;
            } else {
                throw new RuntimeException("Unknown custom loader arg: " + args[1]);
            }
        }

        ClassLoaderTest test = new ClassLoaderTest(policy, useSystemLoader);
        setUp();
        test.processForPolicyFile();
    }

    /**
     * Test cases are based on the following logic,
     *  given: a policyFile in {none, valid, malformed} and
     *         a classLoader in {SystemClassLoader, CustomClassLoader}:
     *  for (clientModule : {"NAMED", "UNNAMED"}) {
     *      for (classLoaderModule : {"NAMED", "UNNAMED"}) {
     *          Create and run java command for each possible Test case
     *      }
     *  }
     */
    private void processForPolicyFile() throws Exception {
        final String regLoaderLoc = CL_JAR.toFile().getAbsolutePath();
        final String modLoadrLoc = MCL_JAR.toFile().getAbsolutePath();
        final String regClientLoc = C_JAR.toFile().getAbsolutePath();
        final String modClientLoc = MC_JAR.toFile().getAbsolutePath();
        final String autoModCloc = AMC_JAR.toFile().getAbsolutePath();
        final String separator = File.pathSeparator;

        // NAMED-NAMED:
        System.out.println("Case:- Modular Client and " +
                ((useSCL) ? "SystemClassLoader"
                        : "Modular CustomClassLoader") + " " + smMsg);
        execute("--module-path", modClientLoc + separator + modLoadrLoc, "-m",
                "mc/c.TestClient");

        // NAMED-UNNAMED:
        System.out.println("Case:- Modular Client and " + ((useSCL)
                ? "SystemClassLoader"
                : "Unknown modular CustomClassLoader") + " " + smMsg);
        execute(new String[] {"--module-path", autoModCloc, "-cp", regLoaderLoc,
                "-m", "mc/c.TestClient"},
                "FAIL", MISSING_MODULE);

        // UNNAMED-NAMED:
        System.out.println("Case:- Unknown modular Client and " +
                ((useSCL) ? "SystemClassLoader"
                      : "Modular CustomClassLoader") + " " + smMsg);
        execute("-cp", regClientLoc, "--module-path", modLoadrLoc, addmodArg,
                "c.TestClient");

        // UNNAMED-UNNAMED:
        System.out.println("Case:- Unknown modular Client and " +
                ((useSCL) ? "SystemClassLoader"
                        : "Unknown modular CustomClassLoader") + " " + smMsg);
        execute("-cp", regClientLoc + separator + regLoaderLoc, "c.TestClient");

        // Regular jars in module-path
        System.out.println("Case:- Regular Client and " + ((useSCL)
                ? "SystemClassLoader"
                : "Unknown modular CustomClassLoader") +
                " inside --module-path " + smMsg);
        execute("--module-path", regClientLoc + separator + regLoaderLoc,
                autoAddModArg, "-m", "c/c.TestClient");

        // Modular jars in class-path
        System.out.println("Case:- Modular Client and " +
                ((useSCL) ? "SystemClassLoader"
                        : "Modular CustomClassLoader") + " in -cp " + smMsg);
        execute("-cp", modClientLoc + separator + modLoadrLoc, "c.TestClient");
    }

    private void execute(String... args) throws Exception {
        execute(args, this.expectedStatus, this.expectedMsg);
    }

    /**
     * Execute with command arguments and process the result.
     */
    private void execute(String[] args, String status, String msg) throws Exception {

        // Combine with commonArgs, and perform sanity check
        String[] safeArgs = Stream.concat(commonArgs.stream(), Stream.of(args))
                .filter(s -> {
                    if (s.contains(" ")) { throw new RuntimeException("No spaces in args");}
                    return !s.isEmpty();
                }).toArray(String[]::new);
        String out = ProcessTools.executeTestJvm(safeArgs).getOutput();
        // Handle response.
        if ("PASS".equals(status) && out.contains(msg)) {
            System.out.println("PASS: Expected Result: " + msg);
        } else if ("FAIL".equals(status) && out.contains(msg)) {
            System.out.printf("PASS: Expected Failure: " +  msg);
        } else if (out.contains("Exception") || out.contains("Error")) {
            System.out.printf("OUTPUT: %s", out);
            throw new RuntimeException("FAIL: Unknown Exception.");
        } else {
            System.out.printf("OUTPUT: %s", out);
            throw new RuntimeException("FAIL: Unknown Test case found");
        }
    }

    /**
     * Creates regular/modular jar files for TestClient and TestClassLoader.
     */
    private static void setUp() throws Exception {

        // Generate regular jar files for TestClient and TestClassLoader
        JarUtils.createJarFile(CL_JAR, TEST_CLASSES,
                               "cl/TestClassLoader.class");
        JarUtils.createJarFile(C_JAR, TEST_CLASSES,
                               "c/TestClient.class");
        // Generate modular jar files for TestClient and TestClassLoader with
        // their corresponding ModuleDescriptor.
        Files.copy(CL_JAR, MCL_JAR,
                StandardCopyOption.REPLACE_EXISTING);
        updateModuleDescr(MCL_JAR, ModuleDescriptor.newModule("mcl")
                .exports("cl").requires("java.base").build());
        Files.copy(C_JAR, MC_JAR,
                StandardCopyOption.REPLACE_EXISTING);
        updateModuleDescr(MC_JAR, ModuleDescriptor.newModule("mc")
                .exports("c").requires("java.base").requires("mcl").build());
        Files.copy(C_JAR, AMC_JAR,
                StandardCopyOption.REPLACE_EXISTING);
        updateModuleDescr(AMC_JAR, ModuleDescriptor.newModule("mc")
                .exports("c").requires("java.base").requires("cl").build());
    }

    /**
     * Update regular jars and include module-info.class inside it to make
     * modular jars.
     */
    private static void updateModuleDescr(Path jar, ModuleDescriptor mDescr)
            throws Exception {
        if (mDescr != null) {
            Path dir = Files.createTempDirectory("tmp");
            Path mi = dir.resolve("module-info.class");
            try (OutputStream out = Files.newOutputStream(mi)) {
                ModuleInfoWriter.write(mDescr, out);
            }
            System.out.format("Adding 'module-info.class' to jar '%s'%n", jar);
            JarUtils.updateJarFile(jar, dir);
        }
    }
}
