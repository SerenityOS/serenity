/*
 * Copyright (c) 2002, 2017, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4504153
 * @summary RMI implementation should not log to two loggers where one is
 * an ancestor of the other, to avoid unintended or duplicate logging
 * @author Peter Jones
 *
 * @library ../../../../../java/rmi/testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build JavaVM
 * @run main/othervm Test4504153
 */

import java.io.ByteArrayOutputStream;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.RemoteServer;

public class Test4504153 {

    private final static String DONE = "Done!";

    public static void main(String[] args) throws Exception {

        System.err.println("\nRegression test for bug 4504153\n");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        JavaVM vm = new JavaVM(StartRegistry.class.getName(),
                               "-Dsun.rmi.transport.logLevel=v", "", out, err);
        try {
            vm.execute();
        } finally {
            vm.destroy();
        }

        String errString = err.toString();

        System.err.println(
            "child process's standard error output:\n\n" + err + "\n");

        if (errString.indexOf(DONE) < 0) {
            throw new RuntimeException("TEST FAILED: " +
                "failed to collect expected child process output");
        }

        if (errString.indexOf("TCPEndpoint") >= 0) {
            throw new RuntimeException("TEST FAILED: " +
                "unrequested logging output detected");
        }

        System.err.println("TEST PASSED");
    }

    public static class StartRegistry {
        public static void main(String[] args) throws Exception {
            LocateRegistry.createRegistry(0);
            System.err.println(DONE);
        }
    }
}
