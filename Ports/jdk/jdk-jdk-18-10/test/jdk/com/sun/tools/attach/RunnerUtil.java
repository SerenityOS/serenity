/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

import jdk.test.lib.thread.ProcessThread;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Utils;

/*
 * Utility functions for test runners.
 * (Test runner = class that launch a test)
 */
public class RunnerUtil {

    /**
     * The Application process must be run concurrently with our tests since
     * the tests will attach to the Application.
     * We will run the Application process in a separate thread.
     *
     * The Application must be started with flag "-Xshare:off" for the Retransform
     * test in TestBasics to pass on all platforms.
     *
     * The Application will write its pid and shutdownPort in the given outFile.
     */
    public static ProcessThread startApplication(String... additionalOpts) throws Throwable {
        String classpath = System.getProperty("test.class.path", ".");
        String[] myArgs = concat(additionalOpts, new String [] {
            "-XX:+UsePerfData", "-XX:+EnableDynamicAgentLoading",
            "-Dattach.test=true", "-classpath", classpath, "Application"
        });
        String[] args = Utils.addTestJavaOpts(myArgs);
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(args);
        ProcessThread pt = new ProcessThread("runApplication", (line) -> line.equals(Application.READY_MSG), pb);
        pt.start();
        return pt;
    }

    public static String[] concat(String[] a, String[] b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        int aLen = a.length;
        int bLen = b.length;
        String[] c = new String[aLen + bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
     }

    /**
     * Will stop the running Application.
     * First tries to shutdown nicely by connecting to the shut down port.
     * If that fails, the process will be killed hard with stopProcess().
     *
     * If the nice shutdown fails, then an Exception is thrown and the test should fail.
     *
     * @param processThread The process to stop.
     */
    public static void stopApplication(ProcessThread processThread) throws Throwable {
        if (processThread == null) {
            System.out.println("RunnerUtil.stopApplication ignored since proc is null");
            return;
        }
        try {
            System.out.println("RunnerUtil.stopApplication waiting for shutdown");
            processThread.sendMessage(Application.SHUTDOWN_MSG);
            processThread.joinAndThrow();
            processThread.getOutput().shouldHaveExitValue(0);
        } catch (Throwable t) {
            System.out.println("RunnerUtil.stopApplication failed. Will kill it hard: " + t);
            processThread.stopProcess();
            throw t;
        }
    }

    /**
     * Creates a jar file.
     * @param args Command to the jar tool.
     */
    public static void createJar(String... args) {
        System.out.println("Running: jar " + Arrays.toString(args));
        sun.tools.jar.Main jar = new sun.tools.jar.Main(System.out, System.err, "jar");
        if (!jar.run(args)) {
            throw new RuntimeException("jar failed: args=" + Arrays.toString(args));
        }
    }

    /**
     * Read the content of a file.
     * @param file The file to read.
     * @return The file content or null if file does not exists.
     */
    public static String readFile(File file) throws IOException {
        if (!file.exists()) {
            return null;
        }
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String content = new String(bytes);
            return content;
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }
}
