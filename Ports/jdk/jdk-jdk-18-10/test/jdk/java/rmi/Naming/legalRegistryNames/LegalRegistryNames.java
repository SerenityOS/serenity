/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4254808
 * @summary Naming assumes '/' is present in relative URL; change in URL causes regression
 * @author Dana Burns
 * @library ../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary Legal LegalRegistryNames_Stub
 * @run main/othervm LegalRegistryNames
 * @key intermittent
 */

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Ensure that all legal forms of Naming URLs operate with the
 * java.rmi.Naming interface.  This test requires using the default RMI Registry
 * port as it tests all of the RMI naming URL's, including the ones which do not
 * take a port (and therefore uses the default port).
 */
public class LegalRegistryNames extends UnicastRemoteObject
    implements Legal
{

    public LegalRegistryNames() throws java.rmi.RemoteException {}

    public static void main(String args[]) throws RuntimeException {

        System.err.println("\nRegression test for bug/rfe 4254808\n");

        Registry registry = null;
        LegalRegistryNames legal = null;

        boolean oneFormFailed = false;
        String[] names = null;
        Vector legalForms = getLegalForms();
        Remote shouldFind = null;

        // create a registry and the test object
        try {
            legal = new LegalRegistryNames();

            System.err.println("Starting registry on default port");
            registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
        } catch (Exception e) {
            TestLibrary.bomb("registry already running on test port");
        }

        // enumerate through all legal URLs to verify that a remote
        // object can be bound and unbound
        String s = null;
        Enumeration en = legalForms.elements();
        while (en.hasMoreElements()) {
            s = (String) en.nextElement();

            System.err.println("\ntesting form: " + s);

            try {
                Naming.rebind(s, legal);
                names = registry.list();

                // ensure that the name in the registry is what is expected
                if ((names.length > 0) &&
                    (names[0].compareTo("MyName") != 0))
                {
                    oneFormFailed = true;
                    System.err.println("\tRegistry entry for form: " +
                                       s + " is incorrect: " + names[0]);
                }

                // ensure that the object can be unbound under the URL string
                shouldFind = Naming.lookup(s);
                Naming.unbind(s);
                System.err.println("\tform " + s + " OK");

            } catch (Exception e) {

                e.printStackTrace();
                oneFormFailed = true;
                System.err.println("\tunexpected lookup or unbind " +
                                   "exception for form: " + s + e.getMessage() );
            }
        }
        if (oneFormFailed) {
            TestLibrary.bomb("Test failed");
        }

        // get the test to exit quickly
        TestLibrary.unexport(legal);
    }

    /**
     * return a vector of valid legal RMI naming URLs.
     */
    private static Vector getLegalForms() {
        String localHostAddress = null;
        String localHostName = null;

        // get the local host name and address
        try {
            localHostName = InetAddress.getLocalHost().getHostName();
            localHostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch(UnknownHostException e) {
            TestLibrary.bomb("Test failed: unexpected exception", e);
        }

        Vector legalForms = new Vector();
        legalForms.add("///MyName");
        legalForms.add("//:" + Registry.REGISTRY_PORT + "/MyName");
        legalForms.add("//" + localHostAddress + "/MyName");
        legalForms.add("//" + localHostAddress + ":" +
                       Registry.REGISTRY_PORT + "/MyName");
        legalForms.add("//localhost/MyName");
        legalForms.add("//localhost:" + Registry.REGISTRY_PORT + "/MyName");
        legalForms.add("//" + localHostName + "/MyName");
        legalForms.add("//" + localHostName + ":" + Registry.REGISTRY_PORT +
                       "/MyName");
        legalForms.add("MyName");
        legalForms.add("/MyName");
        legalForms.add("rmi:///MyName");
        legalForms.add("rmi://:" + Registry.REGISTRY_PORT + "/MyName");
        legalForms.add("rmi://" + localHostAddress + "/MyName");
        legalForms.add("rmi://" + localHostAddress + ":" +
                       Registry.REGISTRY_PORT + "/MyName");
        legalForms.add("rmi://localhost/MyName");
        legalForms.add("rmi://localhost:" + Registry.REGISTRY_PORT + "/MyName");
        legalForms.add("rmi://" + localHostName + "/MyName");
        legalForms.add("rmi://" + localHostName + ":" +
                       Registry.REGISTRY_PORT + "/MyName");
        return legalForms;
    }
}
