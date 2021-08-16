/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4155894
 *
 * @summary remote references can't be downloaded
 * @author Ann Wollrath
 *
 * test currently needs RMISecurityManager because of
 * 4180392
 *
 * @library ../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary Ping UseCustomRef_Stub UseCustomRef_Skel
 * @run main/othervm/policy=security.policy/secure=java.rmi.RMISecurityManager/timeout=120 UseCustomRef
 *
 * This test was failing to run because the synthetic access
 * control context used by the application class loader to find and define
 * CustomServerRef does not have accessClassInPackage.sun.rmi.server runtime
 * permission necessary to load its superclass sun.rmi.server.UnicastServerRef,
 * even though this test's code is granted that permission in its policy file.
 * That bug number is 4256530
 */

import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import sun.rmi.transport.LiveRef;

public class UseCustomRef
        extends RemoteServer
        implements Ping
{

    public UseCustomRef() throws RemoteException {
        exportObject();
    }

    public void exportObject() throws RemoteException {
        ref = new CustomServerRef(new LiveRef(0));
        ((ServerRef) ref).exportObject(this, null);
    }

    public RemoteRef getRef() { return ref; }

    public void ping() {}

    public void receiveAndPing(Ping p) throws RemoteException {
        p.ping();
    }

    public static void main(String[] args) {
        Ping obj = null;
        Registry registry = null;

        try {
            /*
             * create registry
             */
            TestLibrary.suggestSecurityManager("java.rmi.RMISecurityManager");

            System.err.println("creating Registry...");

            registry = TestLibrary.createRegistryOnEphemeralPort();
            int port = TestLibrary.getRegistryPort(registry);
            /*
             * create object with custom ref and bind in registry
             */
            System.err.println("creating UseCustomRef...");
            UseCustomRef cr = new UseCustomRef();
            RemoteRef ref = cr.getRef();
            if (!(ref instanceof CustomServerRef)) {
                TestLibrary.bomb("test failed: reference not " +
                                "instanceof CustomServerRef");
            }

            String name = "//:" + port + "/UseCustomRef";
            //      String name = "UseCustomRef";
            System.err.println("binding object in registry...");
            Naming.rebind(name, cr);

            /*
             * look up object and invoke its ping method
             */
            System.err.println("ping object...");
            obj = (Ping) Naming.lookup(name);
            obj.ping();

            /*
             * pass object with custom ref in remote call
             */
            System.err.println("pass object in remote call...");
            obj.receiveAndPing(cr);

            /*
             * write remote object with custom ref to output stream
             */
            System.err.println("writing remote object to stream...");
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bout);
            out.writeObject(cr);
            out.flush();
            out.close();

            /*
             * read back remote object from output stream
             */
            System.err.println("reading remote object from stream...");
            ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bout.toByteArray()));
            cr = (UseCustomRef) in.readObject();

            /*
             * re-export object and ping
             */
            System.err.println("re-export object read...");
            cr.exportObject();
            System.err.println("look up object again...");
            Naming.rebind(name, cr);
            System.err.println("ping object read...");
            obj = (Ping) Naming.lookup(name);
            obj.ping();
            System.err.println("TEST PASSED");
            Naming.unbind(name);
            cr = null;

        } catch (Exception e) {
            TestLibrary.bomb("test failed with exception: ", e);
        } finally {
            TestLibrary.unexport(obj);
            TestLibrary.unexport(registry);

            registry = null;
            obj = null;
        }
    }

    public static class CustomServerRef
        extends sun.rmi.server.UnicastServerRef
    {
        public CustomServerRef() {}

        public CustomServerRef(LiveRef ref) {
            super(ref);
        }
        /*****
        public CustomServerRef(int port,
                               RMIClientSocketFactory csf,
                               RMIServerSocketFactory ssf)
        {
            super (new LiveRef(port, csf, ssf));
        }
        *****/

        public String getRefClass(ObjectOutput out) {
            return "";
        }

        protected void unmarshalCustomCallData(ObjectInput in)
            throws IOException, ClassNotFoundException
        {
            System.err.println("unmarshalling call data...");
            String s = (String) (in.readObject());
            System.err.println(s);
        }

        protected RemoteRef getClientRef() {
            return new CustomRef(ref);
        }
    }

    public static class CustomRef extends sun.rmi.server.UnicastRef {

        public CustomRef() {
        }

        public CustomRef(sun.rmi.transport.LiveRef ref) {
            super(ref);
        }

        protected void marshalCustomCallData(ObjectOutput out)
            throws IOException
        {
            // this custom data ensures that a custom server
            // ref has written the relevant information.
            System.err.println("marshalling call data...");
            out.writeObject("hello there.");
        }

        public String getRefClass(ObjectOutput out) {
            return "";
        }

    }
}
