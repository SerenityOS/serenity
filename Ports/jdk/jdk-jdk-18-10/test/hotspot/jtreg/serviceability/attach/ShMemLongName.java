/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8049695
 * @summary Ensure shmem transport works with long names
 * @requires os.family == "windows"
 * @library /test/lib
 * @run main/othervm ShMemLongName
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import jdk.test.lib.process.ProcessTools;


public class ShMemLongName {

    private static final int maxShMemLength = 49;

    private static final String transport = "dt_shmem";

    public static void main(String[] args) throws Exception {
        // test with the maximum supported shmem name length
        String shmemName = ("ShMemLongName" + ProcessHandle.current().pid()
                                    + String.join("", Collections.nCopies(maxShMemLength, "x"))
                                 ).substring(0, maxShMemLength);
        Process target = getTarget(shmemName).start();
        try {
            waitForReady(target);

            log("attaching to the VM...");
            AttachingConnector ac = Bootstrap.virtualMachineManager().attachingConnectors()
                    .stream()
                    .filter(c -> transport.equals(c.transport().name()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Failed to find transport " + transport));
            Map<String, Connector.Argument> acArgs = ac.defaultArguments();
            acArgs.get("name").setValue(shmemName);

            VirtualMachine vm = ac.attach(acArgs);

            log("attached. test(1) PASSED.");

            vm.dispose();
        } finally {
            target.destroy();
            target.waitFor();
        }

        // extra test: ensure using of too-long name fails gracefully
        // (shmemName + "X") is expected to be "too long".
        ProcessTools.executeProcess(getTarget(shmemName + "X"))
                .shouldContain("address strings longer than")
                .shouldHaveExitValue(2);
        log("test(2) PASSED.");
    }

    private static void log(String s) {
        System.out.println(s);
        System.out.flush();
    }

    // creates target process builder for the specified shmem transport name
    private static ProcessBuilder getTarget(String shmemName) throws IOException {
        log("starting target with shmem name: '" + shmemName + "'...");
        return ProcessTools.createJavaProcessBuilder(
                "-Xdebug",
                "-Xrunjdwp:transport=" + transport + ",server=y,suspend=n,address=" + shmemName,
                "ShMemLongName$Target");
    }

    private static void waitForReady(Process target) throws Exception {
        InputStream os = target.getInputStream();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(os))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals(Target.readyString)) {
                    return;
                }
            }
        }
    }

    public static class Target {
        public static final String readyString = "Ready";
        public static void main(String[] args) throws Exception {
            log(readyString);
            while (true) {
                Thread.sleep(1000);
            }
        }
    }
}
