/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

package nsk.monitoring.stress.thread;

import java.io.*;
import java.lang.management.*;

import nsk.share.*;
import nsk.monitoring.share.*;

/**
 * The test starts recursive threads, switches them  to the various
 * state after reaching defined depth and checks up their stack traces
 * and states gotten via the ThreadMXBean interface. The test may be executed
 * with the following parameters:
 * <ul>
 *      <li>
 *          <code>depth</code> specifies depth of the recursion.
 *      <li>
 *          <code>threadCount</code> specifies amount of the threads.
 * </ul>
 *
 * <p>The other parameters which may have an influence on running test are:
 * <ul>
 *      <li>
 *          <code>testMode</code> defines modes access to MBean:
 *          <ul>
 *              <li><code>DIRECTLY</code> - directly access to MBean(default
 *                  value),
 *              <li><code>SERVER</code> - an access through MBeanServer
 *          </ul>
 *      <li>
 *          <code>MBeanServer</code> defines a MBean server implemetation
 *          under which test is executed:
 *          <ul>
 *              <li><code>DEFAULT</code> - default JMX implementation of
 *                  MBeanServer (default value);
 *              <li><code>CUSTOM</code> - implementation provided by NSK J2SE
 *                  SQE Team
 *          </ul>
 *      <li>
 *          <code>invocationType</code> defines the following kinds of recursive
 *                method:
 *          <ul>
 *              <li><code>java</code> - pure java ones
 *              <li><code>native</code> - native recursive ones
 *          </ul>
 *  Note:  The recursion are performed by invoking the follow kinds of methods:
 * </ul>
 * For details about arguments, see the {@link
 * nsk.monitoring.share.ArgumentHandler ArgumentHamdler} description .
 *
 * <p>The test starts recursive threads via {@link
 * nsk.monitoring.share.ThreadController ThreadController} according to
 * specified parameters and switches them to the various state. The test sets
 * every thread should be switched in one of the following states:
 * <code>BLOCKED</code>, <code>WAITING</code>, <code>SLEEPING</code> or
 * <code>RUNNING</code>.
 * <p>After threads are reaching the specified state, test checks up their stack
 * traces and states gotten via the ThreadMXBean interface.
 * <p>The test fails if state of some thread gotten via the ThreadMXBean
 * interface doesn't correspond to the state specified by the test or stack
 * trace contains an extra element. Expected stack trace elements are specified
 * by {@link nsk.monitoring.share.ThreadMonitor ThreadMonitor}
 *
 * @see ThreadMonitor
 * @see ThreadController
 * @see ArgumentHandler
 *
 */
public class strace010 {

    public static void main(String[] argv) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String[] argv, PrintStream out) {
        return new strace010().runIt(argv, out);
    }

    public int runIt(String [] args, PrintStream out) {
        boolean res = true;

        ArgumentHandler argHandler = new ArgumentHandler(args);

        Log log = new Log(out, argHandler);

        int threadCount = argHandler.getThreadCount();
        int maxDepth = argHandler.getThreadDepth();


        ThreadMonitor threadMonitor = Monitor.getThreadMonitor(log, argHandler);
        ThreadController controller = new ThreadController(log, threadCount, maxDepth,
                                                argHandler.getInvocationType());

        log.display("\nStarting threads.\n");
        controller.run();
        log.display("\nStates of the threads are culminated.");

        long[] threadIDs = threadMonitor.getAllThreadIds();
        ThreadInfo[] info = new ThreadInfo[threadIDs.length];    //info from MBean

        int kinds = controller.getThreadKindCount();
        int[] threadCounts = new int[kinds];

        for (int i = 0; i < kinds; i++) {
            threadCounts[i] = 0;
        }
        for(int i = 0; i < threadIDs.length; i++) {
            try {
                info[i] = threadMonitor.getThreadInfo(threadIDs[i], Integer.MAX_VALUE);
            } catch (Exception e) {
                log.complain("\tUnexpected " + e);
                return Consts.TEST_FAILED;
            }

            if (info[i] == null) continue;
            int err = controller.checkThreadInfo(info[i]);

            switch (err) {
            case ThreadController.ERR_THREAD_NOTFOUND:
                log.complain("\tThread not found:" + info[i].getThreadName());
                res = false;
                break;

            case ThreadController.ERR_STATE:
                log.complain("\tThread " + info[i].getThreadName()
                                    + " wrong thread state: "
                                    + info[i].getThreadState());
                res = false;
                break;

            case ThreadController.ERR_STACKTRACE:

                log.complain("\nWrong stack trace for thread name: "
                                        + info[i].getThreadName());
                log.complain("----------------------------------");
                log.complain("\tthread ID:" + info[i].getThreadId()
                                    + "(" + threadIDs[i] + ")");
                StackTraceElement[] elements = info[i].getStackTrace();
                for (int j = 0; j < (elements.length<5?elements.length:5); j++)
                    log.complain("\t\t" + elements[j]);
                res = false;
                break;
            }

            if ( controller.findThread(info[i].getThreadId()) != null) {

                if (info[i].getThreadState() == Thread.State.BLOCKED) {
                    threadCounts[ThreadController.BLOCKED_IDX]++;

                } else if (info[i].getThreadState() == Thread.State.WAITING) {
                    threadCounts[ThreadController.WAITING_IDX]++;

                } else if (info[i].getThreadState() == Thread.State.TIMED_WAITING) {
                    threadCounts[ThreadController.SLEEPING_IDX]++;

                } else if (info[i].getThreadState() == Thread.State.RUNNABLE) {
                    threadCounts[ThreadController.RUNNING_IDX]++;
                }
            }
        }
        controller.reset();

        log.display("");
        for (int i = 0; i < kinds; i++) {
            Thread.State state =  ThreadController.THREAD_KINDS[i];
            log.display("Checked " + threadCounts[i] + " " + state + " threads");
            if (controller.getThreadCount(state) != threadCounts[i]) {
                log.complain("Expected amount: " + controller.getThreadCount(state)
                           + " for " + state
                           + " threads" + " actual: " + threadCounts[i]);
                res = false;
            }
        }
        log.display("");
        controller.reset();

        if (res) {
            log.display("\nTest PASSED");
            return Consts.TEST_PASSED;
        }

        log.display("\nTest FAILED");
        return Consts.TEST_FAILED;
    }
}
