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

/*
 * @test
 * @bug 8087112
 * @library /test/lib
 * @modules java.net.http
 *          java.logging
 *          jdk.httpserver
 * @build jdk.test.lib.net.SimpleSSLContext jdk.test.lib.Utils
 * @compile ../../../../com/sun/net/httpserver/LogFilter.java
 * @compile ../../../../com/sun/net/httpserver/FileServerHandler.java
 * @compile ../ProxyServer.java
 * @build Security
 *
 * @run main/othervm Driver
 */

/**
 * driver required for allocating free portnumbers and putting this number
 * into security policy file used in some tests.
 *
 * The tests are in Security.java and port number supplied in -Dport.number
 * and -Dport.number1 for tests that require a second free port
 */
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import jdk.test.lib.Utils;

/**
 * Driver for tests
 */
public class Driver {
    // change the default value to "true" to get the subprocess traces.
    final static boolean DEBUG = Boolean.parseBoolean(System.getProperty("test.debug", "true"));

    public static void main(String[] args) throws Throwable {
        System.out.println("Starting Driver");
        runtest("1.policy", "1");
        runtest("10.policy", "10");
        runtest("11.policy", "11");
        runtest("12.policy", "12");
        runtest("16.policy", "16", "-Djdk.httpclient.allowRestrictedHeaders=Host");
        runtest("17.policy", "17", "-Djdk.httpclient.allowRestrictedHeaders=Host");
        System.out.println("DONE");
    }

    static final Path CWD = Paths.get(".");

    static class Logger extends Thread {
        private final OutputStream ps;
        private final InputStream stdout;

        Logger(String cmdLine, Process p) throws IOException {
            super();
            setDaemon(true);
            cmdLine = "Command line = [" + cmdLine + "]\n";
            stdout = p.getInputStream();
            File f = Files.createTempFile(CWD, "debug", ".txt").toFile();
            ps = new FileOutputStream(f);
            ps.write(cmdLine.getBytes());
            ps.flush();
            if (DEBUG) {
                System.out.print(cmdLine);
                System.out.flush();
            }
        }

        public void run() {
            try {
                byte[] buf = new byte[128];
                int c;
                while ((c = stdout.read(buf)) != -1) {
                    if (DEBUG) {
                        System.out.write(buf, 0, c);
                        System.out.flush();
                    }
                    ps.write(buf, 0, c);
                    ps.flush();
                }
                ps.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static void runtest(String policy, String testnum) throws Throwable {
        runtest(policy, testnum, null);
    }


    public static void runtest(String policy, String testnum, String addProp) throws Throwable {
        String testJdk = System.getProperty("test.jdk", "?");
        String testSrc = System.getProperty("test.src", "?");
        String testClassPath = System.getProperty("test.class.path", "?");
        String testClasses = System.getProperty("test.classes", "?");
        String sep = System.getProperty("file.separator", "?");
        String javaCmd = testJdk + sep + "bin" + sep + "java";
        int retval = 10; // 10 is special exit code denoting a bind error
                         // in which case, we retry
        while (retval == 10) {
            List<String> cmd = new ArrayList<>();
            cmd.add(javaCmd);
            cmd.add("-ea");
            cmd.add("-esa");
            cmd.add("-Dtest.jdk=" + testJdk);
            cmd.add("-Dtest.src=" + testSrc);
            cmd.add("-Dtest.classes=" + testClasses);
            cmd.add("-Djava.security.manager");
            cmd.add("-Djava.security.policy=" + testSrc + sep + policy);
            cmd.add("-Dport.number=" + Integer.toString(Utils.getFreePort()));
            cmd.add("-Dport.number1=" + Integer.toString(Utils.getFreePort()));
            cmd.add("-Djdk.httpclient.HttpClient.log=all,frames:all");
            if (addProp != null) {
                cmd.add(addProp);
            }
            cmd.add("-cp");
            cmd.add(testClassPath);
            cmd.add("Security");
            cmd.add(testnum);

            ProcessBuilder processBuilder = new ProcessBuilder(cmd)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectErrorStream(true);

            String cmdLine = cmd.stream().collect(Collectors.joining(" "));
            long start = System.currentTimeMillis();
            Process child = processBuilder.start();
            Logger log = new Logger(cmdLine, child);
            log.start();
            retval = child.waitFor();
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("Security " + testnum
                               + ": retval = " + retval
                               + ", duration=" + elapsed+" ms");
        }
        if (retval != 0) {
            Thread.sleep(2000);
            throw new RuntimeException("Non zero return value");
        }
    }
}
