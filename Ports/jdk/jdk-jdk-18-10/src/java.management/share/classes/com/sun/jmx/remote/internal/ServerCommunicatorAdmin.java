/*
 * Copyright (c) 2003, 2004, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.jmx.remote.internal;


import com.sun.jmx.remote.util.ClassLogger;

public abstract class ServerCommunicatorAdmin {
    public ServerCommunicatorAdmin(long timeout) {
        if (logger.traceOn()) {
            logger.trace("Constructor",
                         "Creates a new ServerCommunicatorAdmin object "+
                         "with the timeout "+timeout);
        }

        this.timeout = timeout;

        timestamp = 0;
        if (timeout < Long.MAX_VALUE) {
            Runnable timeoutTask = new Timeout();
            final Thread t = new Thread(null,
                                        timeoutTask,
                                        "JMX-Server-Admin-Timeout",
                                        0,
                                        false);
            t.setName("JMX server connection timeout " + t.getId());
            // If you change this name you will need to change a unit test
            // (NoServerTimeoutTest)
            t.setDaemon(true);
            t.start();
        }
    }

    /**
     * Tells that a new request message is received.
     * A caller of this method should always call the method
     * <code>rspOutgoing</code> to inform that a response is sent out
     * for the received request.
     * @return the value of the termination flag:
     *         true if the connection is already being terminated,
     *         false otherwise.
     */
    public boolean reqIncoming() {
        if (logger.traceOn()) {
            logger.trace("reqIncoming", "Receive a new request.");
        }

        synchronized(lock) {
            if (terminated) {
                logger.warning("reqIncoming",
                               "The server has decided to close " +
                               "this client connection.");
            }
            ++currentJobs;

            return terminated;
        }
    }

    /**
     * Tells that a response is sent out for a received request.
     * @return the value of the termination flag:
     *         true if the connection is already being terminated,
     *         false otherwise.
     */
    public boolean rspOutgoing() {
        if (logger.traceOn()) {
            logger.trace("reqIncoming", "Finish a request.");
        }

        synchronized(lock) {
            if (--currentJobs == 0) {
                timestamp = System.currentTimeMillis();
                logtime("Admin: Timestamp=",timestamp);
                // tells the adminor to restart waiting with timeout
                lock.notify();
            }
            return terminated;
        }
    }

    /**
     * Called by this class to tell an implementation to do stop.
     */
    protected abstract void doStop();

    /**
     * Terminates this object.
     * Called only by outside, so do not need to call doStop
     */
    public void terminate() {
        if (logger.traceOn()) {
            logger.trace("terminate",
                         "terminate the ServerCommunicatorAdmin object.");
        }

        synchronized(lock) {
            if (terminated) {
                return;
            }

            terminated = true;

            // tell Timeout to terminate
            lock.notify();
        }
    }

// --------------------------------------------------------------
// private classes
// --------------------------------------------------------------
    private class Timeout implements Runnable {
        public void run() {
            boolean stopping = false;

            synchronized(lock) {
                if (timestamp == 0) timestamp = System.currentTimeMillis();
                logtime("Admin: timeout=",timeout);
                logtime("Admin: Timestamp=",timestamp);

                while(!terminated) {
                    try {
                        // wait until there is no more job
                        while(!terminated && currentJobs != 0) {
                            if (logger.traceOn()) {
                                logger.trace("Timeout-run",
                                             "Waiting without timeout.");
                            }

                            lock.wait();
                        }

                        if (terminated) return;

                        final long remaining =
                            timeout - (System.currentTimeMillis() - timestamp);

                        logtime("Admin: remaining timeout=",remaining);

                        if (remaining > 0) {

                            if (logger.traceOn()) {
                                logger.trace("Timeout-run",
                                             "Waiting with timeout: "+
                                             remaining + " ms remaining");
                            }

                            lock.wait(remaining);
                        }

                        if (currentJobs > 0) continue;

                        final long elapsed =
                            System.currentTimeMillis() - timestamp;
                        logtime("Admin: elapsed=",elapsed);

                        if (!terminated && elapsed > timeout) {
                            if (logger.traceOn()) {
                                logger.trace("Timeout-run",
                                             "timeout elapsed");
                            }
                            logtime("Admin: timeout elapsed! "+
                                    elapsed+">",timeout);
                                // stopping
                            terminated = true;

                            stopping = true;
                            break;
                        }
                    } catch (InterruptedException ire) {
                        logger.warning("Timeout-run","Unexpected Exception: "+
                                       ire);
                        logger.debug("Timeout-run",ire);
                        return;
                    }
                }
            }

            if (stopping) {
                if (logger.traceOn()) {
                    logger.trace("Timeout-run", "Call the doStop.");
                }

                doStop();
            }
        }
    }

    private void logtime(String desc,long time) {
        timelogger.trace("synchro",desc+time);
    }

// --------------------------------------------------------------
// private variables
// --------------------------------------------------------------
    private long    timestamp;

    private final int[] lock = new int[0];
    private int currentJobs = 0;

    private long timeout;

    // state issue
    private boolean terminated = false;

    private static final ClassLogger logger =
        new ClassLogger("javax.management.remote.misc",
                        "ServerCommunicatorAdmin");
    private static final ClassLogger timelogger =
        new ClassLogger("javax.management.remote.timeout",
                        "ServerCommunicatorAdmin");
}
