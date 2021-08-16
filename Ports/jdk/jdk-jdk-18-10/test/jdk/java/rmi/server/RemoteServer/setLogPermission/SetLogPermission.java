/*
 * Copyright (c) 2002, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4533390
 * @summary SecurityException can be obtained but is not specified.
 * The RemoteServer.setLog method requires
 * java.util.log.LoggingPermission("control").
 * @author Ann Wollrath
 * @run main/othervm/policy=security.policy SetLogPermission
 */

import java.rmi.server.RemoteServer;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.security.*;
import java.security.cert.Certificate;

public class SetLogPermission {

    public static void main(String[] args) throws Exception {

        System.err.println("\nRegression test for bug 4533390\n");

        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        CodeSource codesource = new CodeSource(null, (Certificate[]) null);
        Permissions perms = null;
        ProtectionDomain pd = new ProtectionDomain(codesource, perms);
        AccessControlContext acc =
            new AccessControlContext(new ProtectionDomain[] { pd });

        java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction() {
            public Object run() {
                try {
                    System.err.println(
                        "Attempt to set log without permission");
                    RemoteServer.setLog(new ByteArrayOutputStream());
                    throw new RuntimeException(
                        "TEST FAILED: set log without permission");
                } catch (SecurityException e) {
                    System.err.println(
                        "TEST PASSED: unable to set log without permission");
                }
                return null;
            }}, acc);

        try {
            System.err.println("Attempt to set log with permission");
            RemoteServer.setLog(new ByteArrayOutputStream());
            System.err.println(
                "TEST PASSED: sufficient permission to set log");
        } catch (SecurityException e) {
            System.err.println("TEST FAILED: unable to set log");
            throw e;
        }
    }
}
