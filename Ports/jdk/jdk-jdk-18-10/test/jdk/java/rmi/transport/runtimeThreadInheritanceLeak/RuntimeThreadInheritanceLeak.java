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
 * @bug 4404702 8216528
 * @summary When the RMI runtime (lazily) spawns system threads that could
 * outlive the application context in which they were (happened to be)
 * created, such threads should not inherit (thread local) data specific to
 * such an application context for various isolation reasons (see 4219095).
 * While there is not yet a practical means for a general solution to this
 * problem, the particular problem documented in 4404702-- the inheritance
 * of the parent thread's context class loader, preventing that loader from
 * being garbage collected in the future-- can be easily fixed.  This test
 * verifies that the context class loader in effect when the first remote
 * object is exported (and thus when some long-lived RMI daemon threads are
 * created) can be garbage collected after the remote object has been
 * unexported.  [Note that this test is somewhat at the mercy of other J2SE
 * subsystems also not holding on to the loader in their daemon threads.]
 * @author Peter Jones
 *
 * @build RuntimeThreadInheritanceLeak_Stub
 * @run main/othervm RuntimeThreadInheritanceLeak
 */

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.Map;

public class RuntimeThreadInheritanceLeak implements Remote {

    private static final int TIMEOUT = 20000;

    public static void main(String[] args) {

        System.err.println("\nRegression test for bug 4404702\n");

        /*
         * HACK: Work around the fact that java.util.logging.LogManager's
         * (singleton) construction also has this bug-- it will register a
         * "shutdown hook", i.e. a thread, which will inherit and pin the
         * current thread's context class loader for the lifetime of the VM--
         * by causing the LogManager to be initialized now, instead of by
         * RMI when our special context class loader is set.
         */
        java.util.logging.LogManager.getLogManager();

        /*
         * HACK: Work around the fact that the non-native, thread-based
         * SecureRandom seed generator (ThreadedSeedGenerator) seems to
         * have this bug too (which had been causing this test to fail
         * when run with jtreg on Windows XP-- see 4910382).
         */
        (new java.security.SecureRandom()).nextInt();

        RuntimeThreadInheritanceLeak obj = new RuntimeThreadInheritanceLeak();

        try {
            ClassLoader loader = URLClassLoader.newInstance(new URL[0]);
            ReferenceQueue refQueue = new ReferenceQueue();
            Reference loaderRef = new WeakReference(loader, refQueue);
            System.err.println("created loader: " + loader);

            Thread.currentThread().setContextClassLoader(loader);
            UnicastRemoteObject.exportObject(obj);
            Thread.currentThread().setContextClassLoader(
                ClassLoader.getSystemClassLoader());
            System.err.println(
                "exported remote object with loader as context class loader");

            loader = null;
            System.err.println("nulled strong reference to loader");

            UnicastRemoteObject.unexportObject(obj, true);
            System.err.println("unexported remote object");

            /*
             * HACK: Work around the fact that the sun.misc.GC daemon thread
             * also has this bug-- it will have inherited our loader as its
             * context class loader-- by giving it a chance to pass away.
             */
            Thread.sleep(2000);
            while (loaderRef.get() != null) {
                System.gc();
                Thread.sleep(100);
            }

            System.err.println(
                "waiting to be notified of loader being weakly reachable...");
            Reference dequeued = refQueue.remove(TIMEOUT);
            if (dequeued == null) {
                System.err.println(
                    "TEST FAILED: loader not deteced weakly reachable");
                dumpThreads();
                throw new RuntimeException(
                    "TEST FAILED: loader not detected weakly reachable");
            }

            System.err.println(
                "TEST PASSED: loader detected weakly reachable");
            dumpThreads();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("TEST FAILED: unexpected exception", e);
        } finally {
            try {
                UnicastRemoteObject.unexportObject(obj, true);
            } catch (RemoteException e) {
            }
        }
    }

    /**
     * Dumps information about all live threads to System.err,
     * including their context class loaders.
     **/
    private static void dumpThreads() {
        System.err.println(
            "current live threads and their context class loaders:");
        Map threads = Thread.getAllStackTraces();
        for (Iterator iter = threads.entrySet().iterator(); iter.hasNext();) {
            Map.Entry e = (Map.Entry) iter.next();
            Thread t = (Thread) e.getKey();
            System.err.println("  thread: " + t);
            System.err.println("  context class loader: " +
                               t.getContextClassLoader());
            StackTraceElement[] trace = (StackTraceElement[]) e.getValue();
            for (int i = 0; i < trace.length; i++) {
                System.err.println("    " + trace[i]);
            }
        }
    }
}
