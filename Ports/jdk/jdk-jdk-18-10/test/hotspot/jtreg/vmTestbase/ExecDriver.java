/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Starts a new process to execute a command.
 * <p>Usage: --java|--cmd|--launcher <arg>+
 * <p>If {@code --cmd} flag is specified, the arguments are treated as
 * a program to run and its arguments. Non-zero exit code of the created process
 * will be reported as an {@link AssertionError}.
 * <p>If {@code --java} flag is specified, the arguments are passed to {@code java}
 * from JDK under test. If exit code doesn't equal to 0 or 95, {@link AssertionError}
 * will be thrown.
 * <p>If {@code --launcher} flag is specified, the arguments treated similar as
 * for {@code --cmd}, but the started process will have the directory which
 * contains {@code jvm.so} in dynamic library path, and {@code test.class.path}
 * as CLASSPATH environment variable. Exit codes are checked as in
 * {@code --java}, i.e. 0 or 95 means pass.
 */
public class ExecDriver {
    // copied from jdk.test.lib.Utils.TEST_CLASS_PATH
    private static final String TEST_CLASS_PATH = System.getProperty("test.class.path", ".");
    // copied from jdk.test.lib.Utils.TEST_CLASS_PATH
    private static final String TEST_JDK = System.getProperty("test.jdk");
    public static void main(String[] args) throws IOException, InterruptedException {
        boolean java = false;
        boolean launcher = false;

        String type = args[0];
        switch (type) {
            case "--java":
                String[] oldArgs = args;
                int count;
                String libraryPath = System.getProperty("test.nativepath");
                if (libraryPath != null && !libraryPath.isEmpty()) {
                    count = 4;
                    args = new String[args.length + 3];
                    args[3] = "-Djava.library.path=" + libraryPath;
                } else {
                    count = 3;
                    args = new String[args.length + 2];
                }
                args[0] = javaBin();
                args[1] = "-cp";
                args[2] = TEST_CLASS_PATH;
                System.arraycopy(oldArgs, 1, args, count, oldArgs.length - 1);
                java = true;
                break;
            case "--launcher":
                java = true;
                launcher = true;
            case "--cmd":
                args = Arrays.copyOfRange(args, 1, args.length);
                break;
            default:
                throw new Error("unknown type: " + type);
        }
        // adding 'test.vm.opts' and 'test.java.opts'
        if (java) {
            String[] oldArgs = args;
            String[] testJavaOpts = getTestJavaOpts();
            if (testJavaOpts.length > 0) {
                args = new String[args.length + testJavaOpts.length];
                // bin/java goes before options
                args[0] = oldArgs[0];
                // then external java options
                System.arraycopy(testJavaOpts, 0, args, 1, testJavaOpts.length);
                // and then options and args from a test
                System.arraycopy(oldArgs, 1, args, 1 + testJavaOpts.length, oldArgs.length - 1);
            }
        }
        String command = Arrays.toString(args);
        System.out.println("exec " + command);

        ProcessBuilder pb = new ProcessBuilder(args);
        // adding jvm.so to library path
        if (launcher) {
            Path dir = Paths.get(TEST_JDK);
            String value;
            String name = sharedLibraryPathVariableName();
            // if (jdk.test.lib.Platform.isWindows()) {
            if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
                value = dir.resolve("bin")
                           .resolve(variant())
                           .toAbsolutePath()
                           .toString();
                value += File.pathSeparator;
                value += dir.resolve("bin")
                            .toAbsolutePath()
                            .toString();
            } else {
                value = dir.resolve("lib")
                           .resolve(variant())
                           .toAbsolutePath()
                           .toString();
            }

            System.out.println("  with " + name + " = " +
                    pb.environment()
                      .merge(name, value, (x, y) -> y + File.pathSeparator + x));
            System.out.println("  with CLASSPATH = " +
                    pb.environment()
                      .put("CLASSPATH", TEST_CLASS_PATH));
        }
        Process p = pb.start();
        // inheritIO does not work as expected for @run driver
        new Thread(() -> copy(p.getInputStream(), System.out)).start();
        new Thread(() -> copy(p.getErrorStream(), System.out)).start();
        int exitCode = p.waitFor();

        if (exitCode != 0 && (!java || exitCode != 95)) {
            throw new AssertionError(command + " exit code is " + exitCode);
        }
    }

    // copied from jdk.test.lib.Platform::sharedLibraryPathVariableName
    private static String sharedLibraryPathVariableName() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.startsWith("win")) {
            return "PATH";
        } else if (osName.startsWith("mac")) {
            return "DYLD_LIBRARY_PATH";
        } else if (osName.startsWith("aix")) {
            return "LIBPATH";
        } else {
            return "LD_LIBRARY_PATH";
        }
    }

    // copied from jdk.test.lib.Utils::getTestJavaOpts()
    private static String[] getTestJavaOpts() {
        List<String> opts = new ArrayList<String>();
        {
            String v = System.getProperty("test.vm.opts", "").trim();
            if (!v.isEmpty()) {
                Collections.addAll(opts, v.split("\\s+"));
            }
        }
        {
            String v = System.getProperty("test.java.opts", "").trim();
            if (!v.isEmpty()) {
                Collections.addAll(opts, v.split("\\s+"));
            }
        }
        return opts.toArray(new String[0]);
    }

    // copied jdk.test.lib.Platform::variant
    private static String variant() {
        String vmName = System.getProperty("java.vm.name");
        if (vmName.endsWith(" Server VM")) {
            return "server";
        } else if (vmName.endsWith(" Client VM")) {
            return "client";
        } else if (vmName.endsWith(" Minimal VM")) {
            return "minimal";
        } else {
            throw new Error("TESTBUG: unsuppported vm variant");
        }
    }

    private static void copy(InputStream is, OutputStream os) {
        byte[] buffer = new byte[1024];
        int n;
        try (InputStream close = is) {
            while ((n = is.read(buffer)) != -1) {
                os.write(buffer, 0, n);
            }
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String javaBin() {
        return Paths.get(TEST_JDK)
                    .resolve("bin")
                    .resolve("java")
                    .toAbsolutePath()
                    .toString();
    }
}

