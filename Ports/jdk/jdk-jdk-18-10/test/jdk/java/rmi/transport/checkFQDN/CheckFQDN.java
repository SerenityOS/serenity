/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4115683
 * @summary Endpoint hostnames should always be fully qualified or
 *          should be an ip address.  When references to remote
 *          objects are passed outside of the local domain their
 *          endpoints may contain hostnames that are not fully
 *          qualified.  Hence remote clients won't be able to contact
 *          the referenced remote obect.
 *
 * @author Laird Dornin
 *
 * @library ../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary CheckFQDNClient CheckFQDN_Stub TellServerName
 * @run main/othervm/timeout=120 CheckFQDN
 */

/**
 * Get the hostname used by rmi using different rmi properities:
 *
 * if set java.rmi.server.hostname, hostname should equal this
 * property.
 *
 * if set java.rmi.server.useLocalHostname, hostname must contain a '.'
 *
 * if set no properties hostname should be an ipaddress.
 *
 * if set java.rmi.server.hostname, hostname should equal this
 * property even if set java.rmi.server.useLocalHostname is true.
 *
 */

import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.io.*;

/**
 * Export a remote object through which the exec'ed client vm can
 * inform the main test what its host name is.
 */
public class CheckFQDN extends UnicastRemoteObject
    implements TellServerName {
    public static int REGISTRY_PORT =-1;
    static String propertyBeingTested = null;
    static String propertyBeingTestedValue = null;

    public static void main(String args[]) {

        Object dummy = new Object();
        CheckFQDN checkFQDN = null;
        try {
            checkFQDN = new CheckFQDN();

            System.err.println
                ("\nRegression test for bug/rfe 4115683\n");

            Registry registry = TestLibrary.createRegistryOnEphemeralPort();
            REGISTRY_PORT = TestLibrary.getRegistryPort(registry);
            registry.bind("CheckFQDN", checkFQDN);

            /* test the host name scheme in different environments.*/
            testProperty("java.rmi.server.useLocalHostname", "true", "");
            testProperty("java.rmi.server.hostname", "thisIsJustAnRMITest", "");
            testProperty("java.rmi.server.hostname", "thisIsJustAnRMITest",
                         " -Djava.rmi.server.useLocalHostname=true ");
            testProperty("", "", "");

        } catch (Exception e) {
            TestLibrary.bomb(e);
        } finally {
            if (checkFQDN != null) {
                TestLibrary.unexport(checkFQDN);
            }
        }
        System.err.println("\nTest for bug/ref 4115683 passed.\n");
    }

    /**
     * Spawn a vm and feed it a property which sets the client's rmi
     * hostname.
     */
    public static void testProperty(String property,
                                    String propertyValue,
                                    String extraProp)
    {
        JavaVM jvm = null;
        try {
            String propOption = "";
            String equal = "";
            if (!property.equals("")) {
                propOption = " -D";
                equal = "=";
            }

            // create a client to tell checkFQDN what its rmi name is.
            jvm = new JavaVM("CheckFQDNClient",
                                    propOption + property +
                                    equal +
                                    propertyValue + extraProp +
                                    " --add-exports=java.rmi/sun.rmi.registry=ALL-UNNAMED" +
                                    " --add-exports=java.rmi/sun.rmi.server=ALL-UNNAMED" +
                                    " --add-exports=java.rmi/sun.rmi.transport=ALL-UNNAMED" +
                                    " --add-exports=java.rmi/sun.rmi.transport.tcp=ALL-UNNAMED" +
                                    " -Drmi.registry.port=" +
                                    REGISTRY_PORT,
                                    "");

            propertyBeingTested=property;
            propertyBeingTestedValue=propertyValue;

            if (jvm.execute() != 0) {
                TestLibrary.bomb("Test failed, error in client.");
            }

        } catch (Exception e) {
            TestLibrary.bomb(e);
        } finally {
            if (jvm != null) {
                jvm.destroy();
            }
        }
    }

    CheckFQDN() throws RemoteException { }

    /**
     * Remote method to allow client vm to tell the main test what its
     * host name is .
     */
    public void tellServerName(String serverName)
        throws RemoteException {

        if (propertyBeingTested.equals("java.rmi.server.hostname")) {
            if ( !propertyBeingTestedValue.equals(serverName)) {
                TestLibrary.bomb(propertyBeingTested +
                     ":\n Client rmi server name does " +
                     "not equal the one specified " +
                     "by java.rmi.server.hostname: " +
                     serverName +" != " +
                     propertyBeingTestedValue);
            }

            /** use local host name, must contain a '.' */
        } else if (propertyBeingTested.equals
                   ("java.rmi.server.useLocalHostname")) {
            if (serverName.indexOf('.') < 0) {
                TestLibrary.bomb(propertyBeingTested +
                     ":\nThe client servername contains no '.'");
            }
        } else {
            // no propety set, must be ip address
            if ((serverName.indexOf('.') < 0) ||
                (!Character.isDigit(serverName.charAt(0)))) {
                TestLibrary.bomb("Default name scheme:\n"+
                     " The client servername contains no '.'"+
                     "or is not an ip address");
            }
        }
        System.err.println("Servername used: " + serverName);
    }
}
