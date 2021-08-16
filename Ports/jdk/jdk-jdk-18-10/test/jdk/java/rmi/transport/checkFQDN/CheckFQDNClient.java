/*
 * Copyright (c) 1998, 2012, Oracle and/or its affiliates. All rights reserved.
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

/**
 *
 *
 * Client that will run in its own vm and tell the main CheckFQDN test
 * program what its rmi host name is, name obtained from TCPEndpoint.
 */

import java.rmi.*;
import java.rmi.server.*;
import sun.rmi.transport.tcp.TCPEndpoint;

/**
 * Get the local endpoint: make sure that this process does
 * not take too much time and that the resulting localhost
 * string is correct for the property set on an execed vm process.
 */
public class CheckFQDNClient implements Runnable {

    final static int NAME_SERVICE_TIME_OUT = 12000;

    static TCPEndpoint ep = null;

    /**
     * main, lookup remote object and tell it the rmi
     * hostname of this client vm.
     */
    public static void main (String args[]) {

        // start a registry and register a copy of this in it.
        TellServerName tell;
        String hostname = null;

        try {
            hostname = retrieveServerName();
            System.err.println("Client host name: " +
                               hostname);

            int registryPort = Integer.parseInt(System.getProperty("rmi.registry.port"));
            tell = (TellServerName) Naming.lookup("rmi://:" +
                                                  registryPort
                                                  + "/CheckFQDN");
            tell.tellServerName(hostname);
            System.err.println("client has exited");

        } catch (Exception e ) {
            throw new RuntimeException(e.getMessage());
        }
        System.exit(0);
    }

    /* what is the rmi hostname for this vm? */
    public static String retrieveServerName () {
        try {

            CheckFQDNClient chk = new CheckFQDNClient();

            synchronized(chk) {
                (new Thread (chk)).start();
                chk.wait(NAME_SERVICE_TIME_OUT);
            }

            if (ep == null) {
                throw new RuntimeException
                    ("Timeout getting the local endpoint.");
            }

            // this is the name used by rmi for the client hostname
            return ep.getHost();

        }catch (Exception e){
            throw new RuntimeException (e.getMessage());
        }
    }

    /* thread to geth the rmi hostname of this vm */
    public void run () {
        try {
            synchronized(this) {
                ep = TCPEndpoint.getLocalEndpoint(0);
            }
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            synchronized(this) {
                this.notify();
            }
        }
    }
}
