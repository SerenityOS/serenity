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
import jdk.test.lib.Utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.BindException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @test
 * @bug 6228231
 * @summary Test that RMI registry uses SSL.
 * @author Luis-Miguel Alventosa, Taras Ledkov
 *
 * @library /test/lib
 *
 * @build RmiRegistrySslTestApp
 * @run main/timeout=300 RmiRegistrySslTest
 */
public class RmiRegistrySslTest {
    private final String TEST_CLASS_PATH = System.getProperty("test.class.path");
    private final String TEST_CLASSES = System.getProperty("test.classes");
    private final String TEST_SRC = System.getProperty("test.src");
    private final FileSystem FS = FileSystems.getDefault();

    private final Path libDir = FS.getPath(TEST_CLASSES, "lib");
    private final Path rmiRegistryTemplate = FS.getPath(TEST_SRC, "rmiregistry.properties");
    private final Path rmiRegistrySslTemplate = FS.getPath(TEST_SRC, "rmiregistryssl.properties");
    private final Path rmiRegistryFile = libDir.resolve("rmiregistry.properties");
    private final Path rmiRegistrySslFile = libDir.resolve("rmiregistryssl.properties");
    private final String className = "RmiRegistrySslTestApp";
    private int failures = 0;
    private int port = 4444;
    private static int MAX_GET_FREE_PORT_TRIES = 10;
    private Map<String, Object> model = new HashMap<>();

    private RmiRegistrySslTest() {
        try {
            MAX_GET_FREE_PORT_TRIES = Integer.parseInt(System.getProperty("test.getfreeport.max.tries", "10"));
        } catch (NumberFormatException ex) {
        }
    }

    private void initPort() {
        try {
            port = Utils.getFreePort();
        } catch (Exception e) {
        }
        model.put("${getFreePort}", new Integer(port));
    }

    private void initTestEnvironment() throws IOException {
        initPort();

        Files.deleteIfExists(rmiRegistryFile);
        Files.deleteIfExists(rmiRegistrySslFile);
        libDir.toFile().mkdir();
        createFileByTemplate(rmiRegistryTemplate, rmiRegistryFile, model);
        createFileByTemplate(rmiRegistrySslTemplate, rmiRegistrySslFile, model);
    }

    public static void createFileByTemplate(Path template, Path out, Map<String, Object> model) throws IOException {
        if (Files.exists(out) && Files.isRegularFile(out)) {
            try {
                Files.delete(out);
            } catch (Exception ex) {
                System.out.println("WARNING: " + out.toFile().getAbsolutePath() + " already exists - unable to remove old copy");
                ex.printStackTrace();
            }
        }

        try (BufferedReader br = Files.newBufferedReader(template, Charset.defaultCharset());
             BufferedWriter bw = Files.newBufferedWriter(out, Charset.defaultCharset())) {
            String line;
            while ((line = br.readLine()) != null) {
                if (model != null) {
                    for (Map.Entry<String, Object> macro : model.entrySet()) {
                        line = line.replaceAll(Pattern.quote(macro.getKey()), macro.getValue().toString());
                    }
                }

                bw.write(line, 0, line.length());
                bw.newLine();
            }
        }
    }

    public void runTest(String[] args) throws Exception {

        test1();
        test2();
        test3();

        if (failures == 0) {
            System.out.println("All test(s) passed");
        } else {
            throw new Error(String.format("%d test(s) failed", failures));
        }
    }

    private void test1() throws Exception {
        System.out.println("-------------------------------------------------------------");
        System.out.println(getClass().getName() + " : Non SSL RMIRegistry - Non SSL Lookup");
        System.out.println("-------------------------------------------------------------");

        int res = doTest("-DtestID=Test1",
                "-Dcom.sun.management.config.file=" + rmiRegistryFile.toFile().getAbsolutePath());

        if (res != 0) {
            ++failures;
        }
    }

    private void test2() throws Exception {
        System.out.println("-------------------------------------------------------------");
        System.out.println(getClass().getName() + " : SSL RMIRegistry - Non SSL Lookup");
        System.out.println("-------------------------------------------------------------");

        int res = doTest("-DtestID=Test2",
                "-Dcom.sun.management.config.file=" + rmiRegistrySslFile.toFile().getAbsolutePath());

        if (res != 0) {
            ++failures;
        }
    }

    private void test3() throws Exception {

        System.out.println("-------------------------------------------------------------");
        System.out.println(getClass().getName() + " : SSL RMIRegistry - SSL Lookup");
        System.out.println("-------------------------------------------------------------");

        int res = doTest("-DtestID=Test3",
                "-Djavax.net.ssl.keyStore=" + FS.getPath(TEST_SRC, "ssl", "keystore").toFile().getAbsolutePath(),
                "-Djavax.net.ssl.keyStorePassword=password",
                "-Djavax.net.ssl.trustStore=" + FS.getPath(TEST_SRC, "ssl", "truststore").toFile().getAbsolutePath(),
                "-Djavax.net.ssl.trustStorePassword=trustword",
                "-Dcom.sun.management.config.file=" + rmiRegistrySslFile.toFile().getAbsolutePath());

        if (res != 0) {
            ++failures;
        }
    }

    private int doTest(String... args) throws Exception {

        for (int i = 0; i < MAX_GET_FREE_PORT_TRIES; ++i) {

            initTestEnvironment();

            List<String> command = new ArrayList<>();
            Collections.addAll(command, Utils.getTestJavaOpts());
            command.add("-Dtest.src=" + TEST_SRC);
            command.add("-Dtest.rmi.port=" + port);
            command.addAll(Arrays.asList(args));
            command.add("-cp");
            command.add(TEST_CLASS_PATH);
            command.add(className);

            ProcessBuilder processBuilder = ProcessTools.createJavaProcessBuilder(command.toArray(new String[command.size()]));

            OutputAnalyzer output = ProcessTools.executeProcess(processBuilder);

            System.out.println("test output:");
            System.out.println(output.getOutput());

            if (!output.getOutput().contains("Exception thrown by the agent : " +
                    "java.rmi.server.ExportException: Port already in use")) {
                return output.getExitValue();
            }
        }
        throw new Error("Cannot find free port");
    }

    public static void main(String[] args) throws Exception {
        RmiRegistrySslTest test = new RmiRegistrySslTest();

        test.runTest(args);
    }
}
