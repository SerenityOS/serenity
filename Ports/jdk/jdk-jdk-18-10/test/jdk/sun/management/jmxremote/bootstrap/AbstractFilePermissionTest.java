/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Platform;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Change file permission for out-of-the-box management an do test used by
 * PasswordFilePermissionTest and SSLConfigFilePermissionTest tests
 *
 * @author Taras Ledkov
 */
public abstract class AbstractFilePermissionTest {
    private final String TEST_CLASS_PATH = System.getProperty("test.class.path");
    protected final String TEST_CLASSES = System.getProperty("test.classes");
    protected final FileSystem FS = FileSystems.getDefault();
    private int MAX_GET_FREE_PORT_TRIES = 10;

    protected final Path libDir = FS.getPath(TEST_CLASSES, "lib");
    protected final Path mgmt = libDir.resolve("management.properties");
    private final String mp = "-Dcom.sun.management.config.file=" + mgmt.toFile().getAbsolutePath();
    private final String className = "Dummy";
    private int failures = 0;

    protected final Path file2PermissionTest;

    protected AbstractFilePermissionTest(String fileName2PermissionTest) {
        this.file2PermissionTest = libDir.resolve(fileName2PermissionTest);

        try {
            MAX_GET_FREE_PORT_TRIES = Integer.parseInt(System.getProperty("test.getfreeport.max.tries", "10"));
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
        }
    }


    public static void createFile(Path path, String... content) throws IOException {
        if (Files.exists(path) && Files.isRegularFile(path)) {
            try {
                Files.delete(path);
            } catch (Exception ex) {
                System.out.println("WARNING: " + path.toFile().getAbsolutePath() + " already exists - unable to remove old copy");
                ex.printStackTrace();
            }
        }

        try (BufferedWriter bw = Files.newBufferedWriter(path, Charset.defaultCharset())) {
            for (String str : content) {
                bw.write(str, 0, str.length());
                bw.newLine();
            }
        }
    }

    public boolean skipTest() {
        if ((TEST_CLASSES == null) || ("".equals(TEST_CLASSES))) {
            System.out.println("Test is designed to be run from jtreg only");
            return true;
        }

        if (!Platform.isLinux()) {
            System.out.println("Test not designed to run on this operating system, skipping...");
            return true;
        }
        return false;
    }

    protected abstract void testSetup() throws IOException;

    public void runTest(String[] args) throws Exception {

        if (skipTest()) {
            return;
        }

        Files.deleteIfExists(mgmt);
        Files.deleteIfExists(file2PermissionTest);
        libDir.toFile().mkdir();

        testSetup();

        try {
            test1();
            test2();

            if (failures == 0) {
                System.out.println("All test(s) passed");
            } else {
                throw new Error(String.format("%d test(s) failed", failures));
            }
        } finally {
            resetPasswordFilePermission();
        }
    }

    /**
     * Test 1 - SSL config file is secure - VM should start
     */
    private void test1() throws Exception {
        final Set<PosixFilePermission> perms_0700 = new HashSet<>();
        perms_0700.add(PosixFilePermission.OWNER_WRITE);
        perms_0700.add(PosixFilePermission.OWNER_READ);
        perms_0700.add(PosixFilePermission.OWNER_EXECUTE);
        Files.setPosixFilePermissions(file2PermissionTest, perms_0700);

        if (doTest() != 0) {
            ++failures;
        }
    }

    /**
     * Test 1 - SSL config file is secure - VM should start
     */
    private void test2() throws Exception {
        final Set<PosixFilePermission> perms = Files.getPosixFilePermissions(file2PermissionTest);
        perms.add(PosixFilePermission.OTHERS_READ);
        perms.add(PosixFilePermission.OTHERS_EXECUTE);
        Files.setPosixFilePermissions(file2PermissionTest, perms);

        if (doTest() == 0) {
            ++failures;
        }
    }

    private int doTest() throws Exception {

        for (int i = 0; i < MAX_GET_FREE_PORT_TRIES; ++i) {
            final String pp = "-Dcom.sun.management.jmxremote.port=" + jdk.test.lib.Utils.getFreePort();

            List<String> command = new ArrayList<>();
            Collections.addAll(command, jdk.test.lib.Utils.getTestJavaOpts());
            command.add(mp);
            command.add(pp);
            command.add("-cp");
            command.add(TEST_CLASSES);
            command.add(className);


            ProcessBuilder processBuilder = ProcessTools.createJavaProcessBuilder(
                    command.toArray(new String[command.size()]));

            System.out.println("test cmdline: " + Arrays.toString(processBuilder.command().toArray()).replace(",", ""));
            OutputAnalyzer output = ProcessTools.executeProcess(processBuilder);

            System.out.println("test output:");
            System.out.println(output.getOutput());

            if ((output.getExitValue() == 0)  ||
                !output.getOutput().contains("Exception thrown by the agent : " +
                        "java.rmi.server.ExportException: Port already in use")) {
                return output.getExitValue();
            }
        }

        return -1;
    }

    private void resetPasswordFilePermission() throws Exception {
        final Set<PosixFilePermission> perms_0777 = new HashSet<>();
        Arrays.asList(PosixFilePermission.values()).stream().forEach(p -> {
            perms_0777.add(p);
        });
        Files.setPosixFilePermissions(file2PermissionTest, perms_0777);
    }
}
