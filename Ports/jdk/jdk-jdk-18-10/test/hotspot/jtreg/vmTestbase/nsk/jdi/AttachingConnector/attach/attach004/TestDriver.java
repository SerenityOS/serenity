/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.AttachingConnector.attach.attach004;

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.Utils;
import jtreg.SkippedException;
import nsk.share.jdi.ArgumentHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestDriver {
    public static void main(String[] args) throws InterruptedException {
        String arch = args[0];
        String transport = args[1];
        String jdiTransport;
        // Need convert transport argument to string recognizable by nsk.share.jdi.ArgumentHandler
        switch (transport) {
            case "dt_socket":
                jdiTransport = "socket";
                break;
            case "dt_shmem":
                jdiTransport = "shmem";
                break;
            default:
                throw new Error("Unexpected transport " + args[0]
                        + ", expected is dt_socket or dt_shmem");
        }
        String[] jdiArgs = new String[]{
                "-connector=attaching",
                "-transport=" + jdiTransport,
                "-verbose",
                "-waittime=5",
                "-debugee.vmkind=java",
                "-transport.address=dynamic",
                "-arch=" + arch
        };

        if (!isTransportSupported(jdiArgs)) {
            throw new SkippedException("Transport isn't supported on this platform");
        }

        System.out.println("Transport is supported on this platform, execute test");
        String suspend = args[2];
        Process debuggee = startDebuggee(jdiArgs, transport, suspend);
        Process debugger = startDebugger(jdiArgs, Arrays.copyOfRange(args, 3, args.length), debuggee.pid());

        int debuggerExit = debugger.waitFor();
        if (debuggerExit != 95) {
            throw new Error("debugger exit code is " + debuggerExit);
        }

        int debuggeeExit = debuggee.waitFor();
        if (debuggeeExit != 95) {
            throw new Error("debuggee exit code is " + debuggeeExit);
        }
    }


    private static Process startDebuggee(String[] jdiArgs, String transport, String suspend) {
        List<String> cmd = new ArrayList<>();
        Class<?> debuggeeClass = attach004t.class;
        cmd.add(JDKToolFinder.getJDKTool("java"));
        Collections.addAll(cmd, Utils.prependTestJavaOpts(
                "-cp",
                Utils.TEST_CLASS_PATH,
                "-Xdebug",
                "-agentlib:jdwp=transport=" + transport + ",server=y,suspend=" + suspend,
                "-Dmy.little.cookie=" + ProcessHandle.current().pid(),
                debuggeeClass.getName()));
        Collections.addAll(cmd, jdiArgs);
        cmd.add("-testWorkDir");
        cmd.add(".");

        System.out.println("Starting debuggee [" + String.join(",", cmd) + "]");
        Process p;
        try {
            p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        } catch (IOException e) {
            throw new Error("can't start debuggee");
        }

        Thread t = new Thread(() -> dumpStream("debuggee>> ", p.getInputStream()));
        t.setDaemon(true);
        t.start();

        return p;
    }

    private static Process startDebugger(String[] jdiArgs, String[] debuggerArgs, long debuggeePid) {
        List<String> cmd = new ArrayList<>();
        Class<?> debuggerClass = attach004.class;
        cmd.add(JDKToolFinder.getJDKTool("java"));
        Collections.addAll(cmd, Utils.prependTestJavaOpts(
                "-cp",
                Utils.TEST_CLASS_PATH,
                debuggerClass.getName(),
                "-debuggeePID",
                "" + debuggeePid));
        Collections.addAll(cmd, debuggerArgs);
        Collections.addAll(cmd, jdiArgs);
        cmd.add("-testWorkDir");
        cmd.add(".");

        System.out.println("Starting debugger [" + String.join(",", cmd) + "]");
        Process p;
        try {
            p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        } catch (IOException e) {
            throw new Error("can't start debugger");
        }

        Thread t = new Thread(() -> dumpStream("debugger>> ", p.getInputStream()));
        t.setDaemon(true);
        t.start();

        return p;
    }

    private static void dumpStream(String prefix, InputStream is) {

        byte[] buffer = new byte[1024];
        int n;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = r.readLine()) != null) {
                System.out.println(prefix + line);
                System.out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean isTransportSupported(String[] jdiArgs) {
        ArgumentHandler argHandler = new ArgumentHandler(jdiArgs);

        boolean result;

        if (argHandler.isShmemTransport()) {
            result = !argHandler.shouldPass("dt_shmem");
        } else {
            result = !argHandler.shouldPass("dt_socket");
        }

        if (result) {
            System.out.println("Transport " + argHandler.getTransportType()
                    + " is result at " + argHandler.getArch());
        } else {
            System.out.println("Transport " + argHandler.getTransportType()
                    + " isn't result at " + argHandler.getArch());
        }

        return result;
    }
}
