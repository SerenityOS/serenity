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
 * @bug 4465315
 * @summary The RMI runtime's server-side DGC implementation object should
 * not be exported with the arbitrary access control context that was in
 * effect when it gets lazily created.  For example, calls to it should not
 * fail due to the "accept" SocketPermission check simply because of the
 * access control context that it was exported with.
 * @author Peter Jones
 *
 * @library ../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary DGCImplInsulation_Stub
 * @run main/othervm/policy=security.policy DGCImplInsulation
 */

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.SocketPermission;
import java.rmi.MarshalledObject;
import java.rmi.Remote;
import java.rmi.server.UnicastRemoteObject;
import java.security.AccessControlContext;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;

public class DGCImplInsulation implements java.rmi.Remote {

    private static final long TIMEOUT = 5000;

    public static void main(String[] args) throws Exception {

        TestLibrary.suggestSecurityManager(null);

        Permissions perms = new Permissions();
        perms.add(new SocketPermission("*:1024-", "listen"));
        AccessControlContext acc =
            new AccessControlContext(new ProtectionDomain[] {
                new ProtectionDomain(
                    new CodeSource(null, (Certificate[]) null), perms) });

        Remote impl = new DGCImplInsulation();;

        try {
            Remote stub = (Remote) java.security.AccessController.doPrivileged(
                new ExportAction(impl));
            System.err.println("exported remote object; local stub: " + stub);

            MarshalledObject mobj = new MarshalledObject(stub);
            stub = (Remote) mobj.get();
            System.err.println("marshalled/unmarshalled stub: " + stub);

            ReferenceQueue refQueue = new ReferenceQueue();
            Reference weakRef = new WeakReference(impl, refQueue);
            impl = null;
            System.gc();
            if (refQueue.remove(TIMEOUT) == weakRef) {
                throw new RuntimeException(
                    "TEST FAILED: remote object garbage collected");
            } else {
                System.err.println("TEST PASSED");
                stub = null;
                System.gc();
                Thread.sleep(2000);
                System.gc();
            }
        } finally {
            try {
                UnicastRemoteObject.unexportObject(impl, true);
            } catch (Exception e) {
            }
        }
    }

    private static class ExportAction implements PrivilegedExceptionAction {
        private final Remote impl;
        ExportAction(Remote impl) {
            this.impl = impl;
        }
        public Object run() throws Exception {
            return UnicastRemoteObject.exportObject(impl);
        }
    }
}
