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

/* @test
 * @bug 4118600
 * @summary RMI UnmarshallException, interaction on stopping a thread.
 *
 * @bug 4177704
 * @summary RuntimeExceptions can corrupt call connections that may be reused.
 *
 * @author Laird Dornin
 *
 * @library ../../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary CheckUnmarshal CheckUnmarshalOnStopThread_Stub
 *     PoisonPill RuntimeExceptionParameter
 * @run main/othervm/timeout=480 CheckUnmarshalOnStopThread
 */

import java.rmi.*;
import java.rmi.server.*;
import java.io.*;
import java.rmi.registry.*;

/**
 * Description for 4118600:
 *
 * If an rmi call thread is stopped while unmarshalling a return
 * value), java.lang.ThreadDeath will be thrown during
 * UnicastRef.invoke(...).  If rmi handles the Error properly, the
 * remote method connection will not be reused.  Otherwise the
 * connection can be freed and reused in a corrupted state, which will
 * lead to the throwing of an UnmarshalException the next time the
 * connection is used.
 *
 * To test RMI Error handling, the test invokes the remote call,
 * getPoisonPill, a number of times.  This method returns an object
 * which throws an Error on return value deserialization (from its
 * readObject method). If RMI handles the error correctly, another
 * remote call, ping, should execute correctly (i.e. with no
 * exceptions).  The test fails if the ping method throws an
 * UnmarshalException.
 *
 * The old way that the test used to operate:
 *
 * Iterate a large number of times: each iteration spawns a thread
 * that makes multiple rmi calls, sleep for 10 milliseconds, then stop
 * the thread that is making the rmi calls (hopefully during return
 * value Unmarshalling).
 *
 * Count the number of UnmarshalExceptions that occur during test
 * iterations.  If this number is > 10, then the test fails.
 *
 * Note: Even if rmi is catching java.lang.ThreadDeath properly, other
 * types of exceptions (often related to monitor state, etc.) can
 * occur.  This test is only written to track UnmarshalExceptions;
 * success/failure does not depend on other types of problems.
 *
 * Description for 4177704:
 *
 * Similar situation as for 4177704 except that instead of just
 * ensuring that RMI properly handles Errors, the second part of the
 * test ensures that RMI deals with RuntimeExceptions correctly.
 *
 * Test also ensures that call connections are freed without reuse
 * when RuntimeExceptions are thrown during the marshalling of call
 * parameters.  An object that throws a RuntimeException in its
 * writeObject method helps to carry out this part of the test.
 */
public class CheckUnmarshalOnStopThread
    extends UnicastRemoteObject
    implements CheckUnmarshal
{
    final static int RUNTIME_PILL = 1;
    public static int typeToThrow = 0;

    /*
     * remote object implementation
     */

    CheckUnmarshalOnStopThread() throws RemoteException { }

    public PoisonPill getPoisonPill() throws RemoteException {
        return new PoisonPill(new Integer(0));
    }

    public Object ping() throws RemoteException {
        return (Object) new Integer(0);
    }

    public void passRuntimeExceptionParameter(
        RuntimeExceptionParameter rep) throws RemoteException
    {
        // will never be called
    }

    public static void main(String [] args) {

        Object dummy = new Object();
        CheckUnmarshal cu = null;
        CheckUnmarshalOnStopThread cuonst = null;

        System.err.println("\nregression test for bugs: " +
                           "4118600 and 4177704\n");

        try {
            cuonst = new CheckUnmarshalOnStopThread();
            cu = (CheckUnmarshal) UnicastRemoteObject.toStub(cuonst);

            // make sure that RMI will free connections appropriately
            // under several situations:

            // when Errors are thrown during parameter unmarshalling
            System.err.println("testing to see if RMI will handle errors");
            ensureConnectionsAreFreed(cu, true);

            // when RuntimeExceptions are thrown during parameter unmarshalling
            System.err.println("testing to see if RMI will handle " +
                               "runtime exceptions");
            typeToThrow = RUNTIME_PILL;
            ensureConnectionsAreFreed(cu, true);

            // when RuntimeExceptions are thrown during parameter marshalling
            System.err.println("testing to see if RMI will handle " +
                               "runtime exceptions thrown during " +
                               "parameter marshalling");
            ensureConnectionsAreFreed(cu, false);

            System.err.println
                ("\nsuccess: CheckUnmarshalOnStopThread test passed ");

        } catch (Exception e) {
            TestLibrary.bomb(e);
        } finally {
            cu = null;
            deactivate(cuonst);
        }
    }

    static void ensureConnectionsAreFreed(CheckUnmarshal cu, boolean getPill)
        throws Exception
    {
        // invoke a remote call that will corrupt a call connection
        // that will not be freed (if the bug is not fixed)

        for (int i = 0 ; i < 250 ; i++) {
            try {
                Object test = cu.ping();
                if (getPill) {
                    cu.getPoisonPill();
                } else {
                    cu.passRuntimeExceptionParameter(
                        new RuntimeExceptionParameter());
                }
            } catch (Error e) {
                // expect an Error from call unmarshalling, ignore it
            } catch (RuntimeException e) {
                // " RuntimeException "
            }
        }

        System.err.println("remote calls passed, received no " +
                           "unmarshal exceptions\n\n");
    }

    static void deactivate(RemoteServer r) {
        // make sure that the object goes away
        try {
            System.err.println("deactivating object.");
            UnicastRemoteObject.unexportObject(r, true);
        } catch (Exception e) {
            e.getMessage();
            e.printStackTrace();
        }
    }
}
