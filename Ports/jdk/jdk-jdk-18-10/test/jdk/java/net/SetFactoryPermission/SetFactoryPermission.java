/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8048052
 * @summary Test a series of methods which requires "setFactory" runtime permission
 * @modules java.rmi
 * @run main/othervm SetFactoryPermission success
 * @run main/othervm/policy=policy.fail SetFactoryPermission fail
 * @run main/othervm/policy=policy.success SetFactoryPermission success
 */
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.server.RMISocketFactory;
import java.security.AccessControlException;

public class SetFactoryPermission {
    static boolean success = false;

    interface Runner {
        public void run() throws Exception;
    }

    public static void main (String[] args) throws Exception {
        if (args.length > 0) {
            success = System.getSecurityManager() == null || args[0].equals("success");
        }

        doTest(()->{
            System.out.println("Verify URLConnection.setContentHandlerFactor()");
            URLConnection.setContentHandlerFactory(null);
        });
        doTest(()->{
            System.out.println("Verify URL.setURLStreamHandlerFactory()");
            URL.setURLStreamHandlerFactory(null);
        });
        doTest(()->{
            System.out.println("Verify ServerSocket.setSocketFactory()");
            ServerSocket.setSocketFactory(null);
        });
        doTest(()->{
            System.out.println("Verify Socket.setSocketImplFactory()");
            Socket.setSocketImplFactory(null);
        });
        doTest(()->{
            System.out.println("Verify RMISocketFactory.setSocketFactory()");
            RMISocketFactory.setSocketFactory(null);
        });
    }

    static void doTest(Runner func) throws Exception {
        try {
            func.run();
            if (!success) {
                throw new RuntimeException("AccessControlException is not thrown. Test failed");
            }
        } catch (SecurityException e) {
            if (success) {
                e.printStackTrace();
                throw new RuntimeException("AccessControlException is thrown unexpectedly. Test failed");
            }
        }
    }
}
