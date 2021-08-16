/*
 * Copyright (c) 2001, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4180282
 * @summary RMI needs a mechanism to dynamically change a VMs RMI
 * serverHostname.  If the java.rmi.server.hostname property is
 * changed dynamically, newly exported objects should be exported
 * with the new hostname instead of the value of the
 * java.rmi.server.hostname property when the first object was exported.
 *
 * @author Ann Wollrath
 *
 * @build ChangeHostName ChangeHostName_Stub
 * @run main/othervm ChangeHostName
 */

import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;

public class ChangeHostName
    extends UnicastRemoteObject
    implements Receiver
{
    public ChangeHostName() throws RemoteException {
    }

    public void receive(Remote obj) {
        System.err.println("received: " + obj.toString());
    }

    public static void main(String[] args) throws Exception {

        InetAddress localAddress = InetAddress.getLocalHost();
        String[] hostlist = new String[] {
            localAddress.getHostAddress(), localAddress.getHostName() };

        for (int i = 0; i < hostlist.length; i++) {

            System.setProperty("java.rmi.server.hostname", hostlist[i]);
            Remote impl = new ChangeHostName();
            System.err.println("\ncreated impl extending URO: " + impl);

            Receiver stub = (Receiver) RemoteObject.toStub(impl);
            System.err.println("stub for impl: " + stub);

            System.err.println("invoking method on stub");
            stub.receive(stub);

            UnicastRemoteObject.unexportObject(impl, true);
            System.err.println("unexported impl");

            if (stub.toString().indexOf(hostlist[i]) >= 0) {
                System.err.println("stub's ref contains hostname: " +
                                   hostlist[i]);
            } else {
                throw new RuntimeException(
                    "TEST FAILED: stub's ref doesn't contain hostname: " +
                    hostlist[i]);
            }
        }
        System.err.println("TEST PASSED");
    }
}

interface Receiver extends Remote {
    void receive(Remote obj) throws RemoteException;
}
