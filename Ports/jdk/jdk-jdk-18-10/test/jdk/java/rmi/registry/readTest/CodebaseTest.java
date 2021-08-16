/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7102369 7094468 7100592
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @library ../../testlibrary
 * @build TestLibrary RMIRegistryRunner RegistryVM JavaVM testPkg.* RegistryLookup
 * @summary remove java.rmi.server.codebase property parsing from registyimpl
 * @run main/othervm CodebaseTest
*/

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class CodebaseTest {

    public static void main(String args[]) throws Exception {
        RegistryVM rmiregistry = null;
        JavaVM client = null;
        try {
            File src = new File(System.getProperty("test.classes", "."), "testPkg");
            File dest = new File(System.getProperty("user.dir", "."), "testPkg");
            Files.move(src.toPath(), dest.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);

            File rmiregistryDir =
                new File(System.getProperty("user.dir", "."), "rmi_tmp");
            rmiregistryDir.mkdirs();
            rmiregistry = RegistryVM.createRegistryVMWithRunner(
                    "RMIRegistryRunner",
                    " -Djava.rmi.server.useCodebaseOnly=false -Djava.security.manager=allow"
                    + " -Duser.dir=" + rmiregistryDir.getAbsolutePath());
            rmiregistry.start();
            int port = rmiregistry.getPort();

            File srcReadTest = new File(System.getProperty("test.classes", "."),
                                    "RegistryLookup.class");
            File destReadTest = new File(System.getProperty("user.dir", "."),
                                    "RegistryLookup.class");
            Files.move(srcReadTest.toPath(), destReadTest.toPath(),
                                    StandardCopyOption.REPLACE_EXISTING);

            File codebase = new File(System.getProperty("user.dir", "."));
            client = new JavaVM("RegistryLookup",
                    " -Djava.rmi.server.codebase=" + codebase.toURI().toURL()
                    + " -cp ." + File.pathSeparator + System.getProperty("test.class.path"),
                    Integer.toString(port));
            int exit = client.execute();
            if (exit == RegistryLookup.EXIT_FAIL) {
                throw new RuntimeException("Test Fails");
            }
        } finally {
            if (rmiregistry != null) {
                rmiregistry.cleanup();
            }
            if (client != null) {
                client.cleanup();
            }
        }
    }
}
