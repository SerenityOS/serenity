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
 * @bug 4487532
 * @summary When the system property "sun.rmi.server.suppressStackTraces" is
 * set to boolean true, then the RMI runtime should take positive action to
 * counteract the new feature in 1.4 of an exception's stack trace being part
 * of its serialized form so that the server-side stack trace of an exception
 * that occurs during the execution of a remote method invocation gets
 * marshalled to the client.  In most cases, this feature-- along with the
 * final fix to 4010355 to make the server-side stack trace data available
 * at the RMI client application level-- is highly desirable, but this system
 * property provides an opportunity to suppress the server-side stack trace
 * data of exceptions thrown by remote methods from being marshalled, perhaps
 * for reasons of performance or confidentiality requirements.
 * @author Peter Jones
 *
 * @build SuppressStackTraces Impl2_Stub Impl1_Stub Impl1_Skel
 * @run main/othervm SuppressStackTraces
 */

import java.io.IOException;
import java.io.ObjectInputStream;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;

interface Pong extends Remote {
    void pong() throws PongException, RemoteException;
}

class PongException extends Exception {
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();

        /*
         * Verify right at unmarshalling time that this exception instance
         * contains no stack trace data from the server (regardless of whether
         * or not it would be apparent at the RMI client application level).
         */
        StackTraceElement[] trace = getStackTrace();
        if (trace.length > 0) {
            throw new RuntimeException(
                "TEST FAILED: exception contained non-empty stack trace: " +
                Arrays.asList(trace));
        }
    }
}

                // stub class generated with "rmic -v1.2 ..."
class Impl2 implements Pong {
    public void pong() throws PongException { __BAR__(); }
    public void __BAR__() throws PongException { throw new PongException(); }
}

                // stub and skeleton classes generated with "rmic -v1.1 ..."
class Impl1 implements Pong {
    public void pong() throws PongException { __BAR__(); }
    public void __BAR__() throws PongException { throw new PongException(); }
}

public class SuppressStackTraces {

    private static void __FOO__(Pong stub)
        throws PongException, RemoteException
    {
        stub.pong();
    }

    public static void main(String[] args) throws Exception {

        System.err.println("\nRegression test for RFE 4487532\n");

        System.setProperty("sun.rmi.server.suppressStackTraces", "true");

        Remote impl2 = new Impl2();
        Remote impl1 = new Impl1();

        try {
            /*
             * Verify behavior for both -v1.1 and -v1.2 stub protocols.
             */
            verifySuppression((Pong) UnicastRemoteObject.exportObject(impl2));
            verifySuppression((Pong) UnicastRemoteObject.exportObject(impl1));

            System.err.println(
                "TEST PASSED (server-side stack trace data suppressed)");
        } finally {
            try {
                UnicastRemoteObject.unexportObject(impl1, true);
            } catch (Exception e) {
            }
            try {
                UnicastRemoteObject.unexportObject(impl2, true);
            } catch (Exception e) {
            }
        }
    }

    private static void verifySuppression(Pong stub) throws Exception {
        System.err.println("testing stub for exported object: " + stub);

        StackTraceElement[] remoteTrace;
        try {
            __FOO__(stub);
            throw new RuntimeException("TEST FAILED: no exception caught");
        } catch (PongException e) {
            System.err.println(
                "trace of exception thrown by remote call:");
            e.printStackTrace();
            System.err.println();
            remoteTrace = e.getStackTrace();
        }

        int fooIndex = -1;
        for (int i = 0; i < remoteTrace.length; i++) {
            StackTraceElement e = remoteTrace[i];
            if (e.getMethodName().equals("__FOO__")) {
                if (fooIndex != -1) {
                    throw new RuntimeException(
                        "TEST FAILED: trace contains more than one __FOO__");
                }
                fooIndex = i;
            } else if (e.getMethodName().equals("__BAR__")) {
                throw new RuntimeException(
                    "TEST FAILED: trace contains __BAR__");
            }
        }
        if (fooIndex == -1) {
            throw new RuntimeException(
                "TEST FAILED: trace lacks client-side method __FOO__");
        }
    }
}
