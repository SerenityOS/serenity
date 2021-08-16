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
 * @bug 4010355
 * @summary When an exception is thrown by a remote method invocation, the
 * stack trace of the exception catchable by the client application should
 * comprise both the client-side trace as well as the server-side trace, as
 * serialized with the Throwable from the server.
 * @author Peter Jones
 *
 * @build ServerStackTrace ServerStackTrace_Stub
 * @run main/othervm ServerStackTrace
 */

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

interface Ping extends Remote {
    void ping() throws PingException, RemoteException;
}

class PingException extends Exception {
}

public class ServerStackTrace implements Ping {

    public void ping() throws PingException {
        __BAR__();
    }

    private void __BAR__() throws PingException {
        throw new PingException();
    }

    private static void __FOO__(Ping stub)
        throws PingException, RemoteException
    {
        stub.ping();
    }

    public static void main(String[] args) throws Exception {

        System.err.println("\nRegression test for RFE 4010355\n");

        ServerStackTrace impl = new ServerStackTrace();

        try {
            Ping stub = (Ping) UnicastRemoteObject.exportObject(impl);

            StackTraceElement[] remoteTrace;
            try {
                __FOO__(stub);
                throw new RuntimeException("TEST FAILED: no exception caught");
            } catch (PingException e) {
                System.err.println(
                    "trace of exception thrown by remote call:");
                e.printStackTrace();
                System.err.println();
                remoteTrace = e.getStackTrace();
            }

            int fooIndex = -1;
            int barIndex = -1;
            for (int i = 0; i < remoteTrace.length; i++) {
                StackTraceElement e = remoteTrace[i];
                if (e.getMethodName().equals("__FOO__")) {
                    if (fooIndex != -1) {
                        throw new RuntimeException("TEST FAILED: " +
                            "trace contains more than one __FOO__");
                    }
                    fooIndex = i;
                } else if (e.getMethodName().equals("__BAR__")) {
                    if (barIndex != -1) {
                        throw new RuntimeException("TEST FAILED: " +
                            "trace contains more than one __BAR__");
                    }
                    barIndex = i;
                }
            }
            if (fooIndex == -1) {
                throw new RuntimeException(
                   "TEST FAILED: trace lacks client-side method __FOO__");
            }
            if (barIndex == -1) {
                throw new RuntimeException(
                   "TEST FAILED: trace lacks server-side method __BAR__");
            }
            if (fooIndex < barIndex) {
                throw new RuntimeException(
                   "TEST FAILED: trace contains client-side method __FOO__ " +
                   "before server-side method __BAR__");
            }
            System.err.println("TEST PASSED");
        } finally {
            try {
                UnicastRemoteObject.unexportObject(impl, true);
            } catch (Exception e) {
            }
        }
    }
}
