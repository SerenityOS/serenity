/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary The juicer is the classic RMI stress test.  The juicer makes
 * a large number of concurrent, long running, remote method invocations
 * between many threads which have exported remote objects.  These
 * threads use remote objects that carry on deep "two party"
 * recursion.  The juicer relies on Distributed Garbage Collection to
 * unexport these remote objects when no more references are held to them.
 * The two parties in the recursion are OrangeImpl and
 * OrangeEchoImpl.  OrangeImpl checks the base case of the recursion
 * so that the program will exit.
 *
 * When the AppleUserImpl.main() method is invoked, the class binds an
 * instance of itself in a registry.  A second server process,
 * an ApplicationServer, is started which looks up the recently
 * bound AppleUser object.  This server is either started up in
 * the same VM or can optionally be started in a separate VM on the
 * same host or on a different host. When this test is run on the
 * RMI profile, ApplicationServer must be started by AppleUserImpl
 * and the complete juicer runs in a single process.
 *
 * The second server process instructs the AppleUserImpl to "use" some apples.
 * AppleUserImpl creates a new thread for each apple.  These threads
 * initiate the two party recursion.
 *
 * Each recursive call nests to a depth determined by this
 * expression: (2 + Math.abs(random.nextInt() % (maxLevel + 1)),
 * where maxLevel is a command line parameter.  Thus each recursive
 * call nests a random number of levels between 2 and maxLevel.
 *
 * The test ends when an exception is encountered or the stop time
 * has been reached.
 *
 * @library ../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary
 *     Apple AppleEvent AppleImpl
 *     Orange OrangeEcho OrangeEchoImpl OrangeImpl
 *     ApplicationServer
 *
 * @run main/othervm/policy=security.policy AppleUserImpl -seconds 30
 *
 * @author Peter Jones, Nigel Daley
 */

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The AppleUserImpl class implements the behavior of the remote
 * "apple user" objects exported by the server.  The application server
 * passes each of its remote "apple" objects to an apple user, and an
 * AppleUserThread is created for each apple.
 */
public class AppleUserImpl extends UnicastRemoteObject implements AppleUser {
    private static int registryPort = -1;
    private static final Logger logger =
        Logger.getLogger("reliability.appleuser");
    private static int threadNum = 0;
    private static long testDuration = 0;
    private static int maxLevel = 7;
    private static Exception status = null;
    private static boolean finished = false;
    private static boolean startTestNotified = false;
    private static final Random random = new Random();
    private static final Object lock = new Object();

    public AppleUserImpl() throws RemoteException {
    }

    /**
     * Allows the other server process to indicate that it is ready
     * to start "juicing".
     */
    public synchronized void startTest() throws RemoteException {
        startTestNotified = true;
        this.notifyAll();
    }

    /**
     * Allows the other server process to report an exception to this
     * process and thereby terminate the test.
     */
    public void reportException(Exception status) throws RemoteException {
        synchronized (lock) {
            this.status = status;
            lock.notifyAll();
        }
    }

    /**
     * "Use" supplied apple object.  Create an AppleUserThread to
     * stress it out.
     */
    public synchronized void useApple(Apple apple) throws RemoteException {
        String threadName = Thread.currentThread().getName();
        logger.log(Level.FINEST,
            threadName + ": AppleUserImpl.useApple(): BEGIN");

        AppleUserThread t =
            new AppleUserThread("AppleUserThread-" + (++threadNum), apple);
        t.start();

        logger.log(Level.FINEST,
            threadName + ": AppleUserImpl.useApple(): END");
    }

    /**
     * The AppleUserThread class repeatedly invokes calls on its associated
     * Apple object to stress the RMI system.
     */
    class AppleUserThread extends Thread {

        final Apple apple;

        public AppleUserThread(String name, Apple apple) {
            super(name);
            this.apple = apple;
        }

        public void run() {
            int orangeNum = 0;
            long stopTime = System.currentTimeMillis() + testDuration;
            Logger logger = Logger.getLogger("reliability.appleuserthread");

            try {
                do { // loop until stopTime is reached

                    /*
                     * Notify apple with some apple events.  This tests
                     * serialization of arrays.
                     */
                    int numEvents = Math.abs(random.nextInt() % 5);
                    AppleEvent[] events = new AppleEvent[numEvents];
                    for (int i = 0; i < events.length; i++) {
                        events[i] = new AppleEvent(orangeNum % 3);
                    }
                    apple.notify(events);

                    /*
                     * Request a new orange object be created in
                     * the application server.
                     */
                    Orange orange = apple.newOrange(
                        "Orange(" + getName() + ")-" + (++orangeNum));

                    /*
                     * Create a large message of random ints to pass to orange.
                     */
                    int msgLength = 1000 + Math.abs(random.nextInt() % 3000);
                    int[] message = new int[msgLength];
                    for (int i = 0; i < message.length; i++) {
                        message[i] = random.nextInt();
                    }

                    /*
                     * Invoke recursive call on the orange.  Base case
                     * of recursion inverts messgage.
                     */
                    OrangeEchoImpl echo = new OrangeEchoImpl(
                        "OrangeEcho(" + getName() + ")-" + orangeNum);
                    int[] response = orange.recurse(echo, message,
                        2 + Math.abs(random.nextInt() % (maxLevel + 1)));

                    /*
                     * Verify message was properly inverted and not corrupted
                     * through all the recursive method invocations.
                     */
                    if (response.length != message.length) {
                        throw new RuntimeException(
                            "ERROR: CORRUPTED RESPONSE: " +
                            "wrong length of returned array " + "(should be " +
                            message.length + ", is " + response.length + ")");
                    }
                    for (int i = 0; i < message.length; i++) {
                        if (~message[i] != response[i]) {
                            throw new RuntimeException(
                                "ERROR: CORRUPTED RESPONSE: " +
                                "at element " + i + "/" + message.length +
                                " of returned array (should be " +
                                Integer.toHexString(~message[i]) + ", is " +
                                Integer.toHexString(response[i]) + ")");
                        }
                    }

                    try {
                        Thread.sleep(Math.abs(random.nextInt() % 10) * 1000);
                    } catch (InterruptedException e) {
                    }

                } while (System.currentTimeMillis() < stopTime);

            } catch (Exception e) {
                status = e;
            }
            finished = true;
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }

    private static void usage() {
        System.err.println("Usage: AppleUserImpl [-hours <hours> | " +
                                                 "-seconds <seconds>]");
        System.err.println("                     [-maxLevel <maxLevel>]");
        System.err.println("                     [-othervm]");
        System.err.println("                     [-exit]");
        System.err.println("  hours    The number of hours to run the juicer.");
        System.err.println("           The default is 0 hours.");
        System.err.println("  seconds  The number of seconds to run the juicer.");
        System.err.println("           The default is 0 seconds.");
        System.err.println("  maxLevel The maximum number of levels to ");
        System.err.println("           recurse on each call.");
        System.err.println("           The default is 7 levels.");
        System.err.println("  othervm  If present, the VM will wait for the");
        System.err.println("           ApplicationServer to start in");
        System.err.println("           another process.");
        System.err.println("           The default is to run everything in");
        System.err.println("           a single VM.");
        System.err.println("  exit     If present, the VM will call");
        System.err.println("           System.exit() when main() finishes.");
        System.err.println("           The default is to not call");
        System.err.println("           System.exit().");
        System.err.println();
    }

    /**
     * Entry point for the "juicer" server process.  Create and export
     * an apple user implementation in an rmiregistry running on localhost.
     */
    public static void main(String[] args) {
        String durationString = null;
        boolean othervm = false;
        boolean exit = false;
        try {
            // parse command line args
            for (int i = 0; i < args.length ; i++ ) {
                String arg = args[i];
                if (arg.equals("-hours")) {
                    if (durationString != null) {
                        usage();
                    }
                    i++;
                    int hours = Integer.parseInt(args[i]);
                    durationString = hours + " hours";
                    testDuration = hours * 60 * 60 * 1000;
                } else if (arg.equals("-seconds")) {
                    if (durationString != null) {
                        usage();
                    }
                    i++;
                    long seconds = Long.parseLong(args[i]);
                    durationString = seconds + " seconds";
                    testDuration = seconds * 1000;
                } else if (arg.equals("-maxLevel")) {
                    i++;
                    maxLevel = Integer.parseInt(args[i]);
                } else if (arg.equals("-othervm")) {
                    othervm = true;
                } else if (arg.equals("-exit")) {
                    exit = true;
                } else {
                    usage();
                }
            }
            if (durationString == null) {
                durationString = testDuration + " milliseconds";
            }
        } catch (Throwable t) {
            usage();
            throw new RuntimeException("TEST FAILED: Bad argument");
        }

        AppleUserImpl user = null;
        long startTime = 0;
        Thread server = null;
        int exitValue = 0;
        try {
            user = new AppleUserImpl();

            synchronized (user) {
                // create new registry and bind new AppleUserImpl in registry
                Registry registry = TestLibrary.createRegistryOnEphemeralPort();
                registryPort = TestLibrary.getRegistryPort(registry);
                LocateRegistry.getRegistry(registryPort).rebind("AppleUser",
                                                                 user);

                // start the other server if applicable
                if (othervm) {
                    // the other server must be running in a separate process
                    logger.log(Level.INFO, "Application server must be " +
                        "started in separate process");
                } else {
                    Class app = Class.forName("ApplicationServer");
                    java.lang.reflect.Constructor appConstructor =
                            app.getDeclaredConstructor(new Class[] {Integer.TYPE});
                    server = new Thread((Runnable) appConstructor.newInstance(registryPort));
                    logger.log(Level.INFO, "Starting application server " +
                        "in same process");
                    server.start();
                }

                // wait for other server to call startTest method
                logger.log(Level.INFO, "Waiting for application server " +
                    "process to start");
                while (!startTestNotified) {
                   user.wait();
                }
            }

            startTime = System.currentTimeMillis();
            logger.log(Level.INFO, "Test starting");

            // wait for exception to be reported or first thread to complete
            logger.log(Level.INFO, "Waiting " + durationString + " for " +
                "test to complete or exception to be thrown");

            synchronized (lock) {
                while (status == null && !finished) {
                    lock.wait();
                }
            }

            if (status != null) {
                throw new RuntimeException("TEST FAILED: "
                    + "juicer server reported an exception", status);
            } else {
                logger.log(Level.INFO, "TEST PASSED");
            }
        } catch (Exception e) {
            logger.log(Level.INFO, "TEST FAILED");
            exitValue = 1;
            if (exit) {
                e.printStackTrace();
            }
            throw new RuntimeException("TEST FAILED: "
                    + "unexpected exception", e);
        } finally {
            long actualDuration = System.currentTimeMillis() - startTime;
            logger.log(Level.INFO, "Test finished");
            try {
                UnicastRemoteObject.unexportObject(user, true);
            } catch (NoSuchObjectException ignore) {
            }
            logger.log(Level.INFO, "Test duration was " +
                (actualDuration/1000) + " seconds " +
                "(" + (actualDuration/3600000) + " hours)");
            System.gc(); System.gc();
            if (exit) {
                System.exit(exitValue);
            }
        }
    }
}
