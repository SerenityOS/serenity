/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
 */

import javax.management.remote.rmi.RMIJRMPServerImpl;
import javax.management.remote.rmi.RMIServer;

public class ImplVersionCommand {

    public static void main(String[] args) throws Exception {

        // Create RMIJRMPServerImpl
        //
        System.out.println("Create RMIJRMPServerImpl");
        RMIServer server = new RMIJRMPServerImpl(0, null, null, null);

        // Get the JMX Remote impl version from RMIServer
        //
        System.out.println("Get JMX Remote implementation version from RMIServer");
        String full_version = server.getVersion();
        System.out.println("RMIServer.getVersion() = "+ full_version);
        String impl_version = full_version.substring(
            full_version.indexOf("java_runtime_")+"java_runtime_".length());

        // Display JMX Remote impl version and Java Runtime version
        //
        System.out.println("JMX Remote implementation version   = " +
                           impl_version);
        System.out.println("Java Runtime implementation version = " +
                           args[0]);

        // Check JMX Remote impl version vs. Java Runtime  version
        //
        if (!impl_version.equals(args[0])) {
            // Test FAILED
            throw new IllegalArgumentException(
                "***FAILED: JMX Remote and Java Runtime versions do NOT match***");
        }
        // Test OK!
        System.out.println("JMX Remote and Java Runtime versions match.");
        System.out.println("Bye! Bye!");
    }
}
