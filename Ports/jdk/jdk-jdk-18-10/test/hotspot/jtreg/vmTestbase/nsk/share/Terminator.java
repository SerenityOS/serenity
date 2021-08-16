/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share;

/**
 * Terminator is used to terminate a stress test with PASS exit status
 * before the test is terminated as timed out (and so failed).
 *
 * <p>Terminator class holds a thread which sleeps for the given amount
 * of time, and then wakes up and executes <tt>System.exit()</tt>
 * with the given exit status. That thread is daemon, so it doesn't
 * prevent application from exiting once all its threads finish
 * before it's time for termination. Appointing terminator in zero
 * delay implies immediate <tt>exit()</tt>.
 *
 * <p>There is a limitation: you may appoint no more than one terminator
 * per application.
 */
public class Terminator {
    /**
     * Use specific <tt>appoint()</tt> method to appoint terminator.
     *
     * @see #appoint(int)
     * @see #appoint(int,int)
     */
    protected Terminator() {}

    /**
     * One terminator per application, or <tt>null</tt> (by default).
     */
    private static Thread terminator = null;

    /**
     * <p>Return timeout (or waittime) value munus the margin
     * value (which is assumed 1 minute by default).
     *
     * <p>Treat <tt>args[0]</tt> as <tt>$TIMEOUT</tt> value, or seek for
     * <tt>-waittime=$WAITTIME</tt> value. If both parameters
     * (or either none of them) are assigned, throw an exception to
     * report parameters inconsistency.
     *
     * <p>Also, seek for <tt>-margin=...</tt> assignment, or assume margin
     * is 1 minute.
     *
     * @param args Is usually obtained via the application's command-line.
     *
     * @throws IllegalArgumentException If <tt>args[]</tt> is inconsistent.
     *
     * @see #appoint(int)
     * @see #appoint(int,int)
     */
    public static int parseAppointment(String args[]) {
        int timeout=-1, margin=1;
        int timeouts=0, waittimes=0, margins=0;
        for (int i=0; i<args.length; i++) {
            if (args[i].startsWith("-")) {
                if (args[i].startsWith("-waittime=")) {
                    timeout = Integer.parseInt(args[i].substring(10));
                    waittimes++;
                }
                if (args[i].startsWith("-margin=")) {
                    margin = Integer.parseInt(args[i].substring(8));
                    margins++;
                }
            } else {
                if (i == 0) {
                    timeout = Integer.parseInt(args[i]);
                    timeouts++;
                }
            }
        };
        if (timeouts==0 && waittimes==0)
            throw new IllegalArgumentException(
                "no $TIMEOUT, nor -waittime=$WAITTIME is set");
        if (waittimes > 1)
            throw new IllegalArgumentException(
                "more than one -waittime=... is set");
        if (margins > 1)
            throw new IllegalArgumentException(
                "more than one -margin=... is set");

        int result = timeout - margin;
        if (result <= 0)
            throw new IllegalArgumentException(
                "delay appointment must be greater than "+margin+" minutes");
        return result;
    }

    /**
     * Appoint terminator after the given amount of <tt>minutes</tt>,
     * so that exit status would be 95 (to simulate JCK-like PASS
     * status).
     *
     * @throws IllegalStateException If terminator is already appointed.
     *
     * @see #appoint(int,int)
     * @see #parseAppointment(String[])
     */
    public static void appoint(int minutes) {
        appoint(minutes,95); // JCK-like PASS status
    }

    /**
     * Appoint Terminator for the given amount of <tt>minutes</tt>,
     * so that the given <tt>status</tt> would be exited when time
     * is over.
     *
     * @throws IllegalStateException If terminator is already appointed.
     *
     * @see #appoint(int)
     * @see #parseAppointment(String[])
     */
    public static void appoint(int minutes, int status) {
        if (terminator != null)
            throw new IllegalStateException("Terminator is already appointed.");

        final long timeToExit = System.currentTimeMillis() + 60*1000L*minutes;
        final int  exitStatus = status;

        terminator = new Thread(Terminator.class.getName()) {
            public void run() {
                long timeToSleep = timeToExit - System.currentTimeMillis();
                if (timeToSleep > 0)
                    try {
                        //
                        // Use wait() instead of sleep(), because Java 2
                        // specification doesn't guarantee the method
                        // sleep() to yield to other threads.
                        //
                        Object someDummyObject = new Object();
                        synchronized (someDummyObject) {
                            someDummyObject.wait(timeToSleep);
                        }
                    } catch (InterruptedException exception) {
                        exception.printStackTrace(System.err);
                        return;
                    };
                //
                // OK, lets do it now:
                //
                System.err.println(
                    "#\n# Terminator: prescheduled program termination.\n#");
                System.exit(exitStatus); // terminator to all threads
            }
        };

        terminator.setPriority(Thread.MAX_PRIORITY);
        terminator.setDaemon(true);
        terminator.start();
    }
}
