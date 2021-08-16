/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8050281
 * @summary Test limited doprivileged action with trhead calls.
 * @run main/othervm/policy=policy LimitedDoPrivilegedWithThread
 */
import java.io.FilePermission;
import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.PropertyPermission;

public class LimitedDoPrivilegedWithThread {

    private static final Permission PROPERTYPERM
            = new PropertyPermission("user.name", "read");
    private static final Permission FILEPERM
            = new FilePermission("*", "read");
    private static final AccessControlContext ACC
            = new AccessControlContext(
                    new ProtectionDomain[]{new ProtectionDomain(null, null)});

    public static void main(String args[]) {
        //parent thread without any permission
        AccessController.doPrivileged(
                (PrivilegedAction) () -> {
                    Thread ct = new Thread(
                            new ChildThread(PROPERTYPERM, FILEPERM));
                    ct.start();
                    try {
                        ct.join();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        ie.printStackTrace();
                        throw new RuntimeException("Unexpected InterruptedException");
                    }
                    return null;
                }, ACC);
    }
}

class ChildThread implements Runnable {

    private final Permission P1;
    private final Permission P2;
    private boolean catchACE = false;

    public ChildThread(Permission p1, Permission p2) {
        this.P1 = p1;
        this.P2 = p2;
    }

    @Override
    public void run() {
        //Verified that child thread has permission p1,
        runTest(null, P1, false, 1);
        //Verified that child thread inherits parent thread's access control context
        AccessControlContext childAcc = AccessController.getContext();
        runTest(childAcc, P1, true, 2);
        //Verified that we can give permision p2 to limit the "privilege" of the
        //class calling doprivileged action, stack walk will continue
        runTest(null, P2, true, 3);

    }

    public void runTest(AccessControlContext acc, Permission perm,
            boolean expectACE, int id) {

        AccessController.doPrivileged(
                (PrivilegedAction) () -> {
                    try {
                        AccessController.getContext().checkPermission(P1);
                    } catch (AccessControlException ace) {
                        catchACE = true;
                    }
                    if (catchACE ^ expectACE) {
                        throw new RuntimeException("test" + id + " failed");
                    }
                    return null;
                }, acc, perm);
    }
}
