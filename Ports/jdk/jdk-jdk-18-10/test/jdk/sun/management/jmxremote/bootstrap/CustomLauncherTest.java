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

import jdk.test.lib.Utils;
import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @test
 * @bug 6434402 8004926
 * @author Jaroslav Bachorik
 *
 * @library /test/lib
 * @modules java.management
 *          jdk.attach
 *          jdk.management.agent/jdk.internal.agent
 *
 * @requires os.family == "linux"
 * @build TestManager TestApplication CustomLauncherTest
 * @run main/othervm/native CustomLauncherTest
 */
public class CustomLauncherTest {

    public static final String TEST_NATIVE_PATH = System.getProperty("test.nativepath");

    public static void main(String[] args) throws Exception {
        if (".".equals(Utils.TEST_CLASS_PATH)) {
            System.out.println("Test is designed to be run from jtreg only");
            return;
        }

        Path libjvm = Platform.jvmLibDir().resolve("libjvm.so");
        Process serverPrc = null, clientPrc = null;

        try {
            String launcher = getLauncher();

            System.out.println("Starting custom launcher:");
            System.out.println("=========================");
            System.out.println("  launcher  : " + launcher);
            System.out.println("  libjvm    : " + libjvm);
            System.out.println("  classpath : " + Utils.TEST_CLASS_PATH);
            ProcessBuilder server = new ProcessBuilder(
                launcher,
                libjvm.toString(),
                Utils.TEST_CLASS_PATH,
                "TestApplication"
            );

            final AtomicReference<String> port = new AtomicReference<>();

            serverPrc = ProcessTools.startProcess(
                "Launcher",
                server,
                (String line) -> {
                    if (line.startsWith("port:")) {
                         port.set(line.split("\\:")[1]);
                    } else if (line.startsWith("waiting")) {
                         return true;
                    }
                    return false;
                },
                5,
                TimeUnit.SECONDS
            );

            System.out.println("Attaching test manager:");
            System.out.println("=========================");
            System.out.println("  PID           : " + serverPrc.pid());
            System.out.println("  shutdown port : " + port.get());

            ProcessBuilder client = ProcessTools.createJavaProcessBuilder(
                "-cp",
                Utils.TEST_CLASS_PATH,
                "--add-exports", "jdk.management.agent/jdk.internal.agent=ALL-UNNAMED",
                "TestManager",
                String.valueOf(serverPrc.pid()),
                port.get(),
                "true"
            );

            clientPrc = ProcessTools.startProcess(
                "TestManager",
                client,
                (String line) -> line.startsWith("Starting TestManager for PID"),
                10,
                TimeUnit.SECONDS
            );

            int clientExitCode = clientPrc.waitFor();
            int serverExitCode = serverPrc.waitFor();

            if (clientExitCode != 0 || serverExitCode != 0) {
                throw new Error("Test failed");
            }
        } finally {
            if (clientPrc != null) {
                clientPrc.destroy();
                clientPrc.waitFor();
            }
            if (serverPrc != null) {
                serverPrc.destroy();
                serverPrc.waitFor();
            }
        }
    }

    private static String getLauncher() {
        Path launcherPath = Paths.get(TEST_NATIVE_PATH, "launcher");
        return launcherPath.toAbsolutePath().toString();
    }
}
