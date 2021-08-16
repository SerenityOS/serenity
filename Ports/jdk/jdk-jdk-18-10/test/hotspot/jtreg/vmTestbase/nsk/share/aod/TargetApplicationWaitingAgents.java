/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.share.aod;

import nsk.share.*;
import nsk.share.jpda.SocketIOPipe;

import java.util.*;

/*
Class TargetApplicationWaitingAgents is part of the framework used in the AttachOnDemand tests
(tests against Attach API, API from package com.sun.tools.attach).

Attach API allows to load so called 'agents' in the running VM. In terms of this framework
application running in the VM where agent is loaded to is called 'target application'.

TargetApplicationWaitingAgents is base class for target applications used in the AttachOnDemand tests
(also TargetApplicationWaitingAgents can be used without modifications in tests where target
application shouldn't execute some test-specific actions).

AttachOnDemand tests requires synchronization between test agents and target application:
    - before target application can start to execute test-specific actions it
    should wait when all tests agents will be loaded

    - target application shouldn't finish until all agents finish their work

TargetApplicationWaitingAgents incapsulates actions common for all target applications. TargetApplicationWaitingAgents
provides 2 methods: 'agentLoaded' and 'agentFinished', test agents use these methods to notify
target application about its status. When target application based on the TargetApplicationWaitingAgents
starts it first waits when all test agents will be loaded (number of expected test agents is
passed via parameter -agentsNumber). After this target application executes test-specific actions,
waits when all test agents will finish, checks agent's status (passed of failed) and
finishes.

In most cases test target applications should override only method 'targetApplicationActions' and
its 'main' method should contain only call of the method 'TargetApplicationWaitingAgents.runTargetApplication'.

Typical target application class looks like this:

public class targetExample extends TargetApplicationWaitingAgents {

    protected void targetApplicationActions() {
        // do test-specific actions
    }

    public static void main(String[] args) {
        new targetExample().runTargetApplication(args);
    }
}
*/
public class TargetApplicationWaitingAgents {

    private volatile static boolean testFailed = false;

    private static long AGENTS_CONNECTION_TIMEOUT = 5 * 60 * 1000; // 5 min

    private static long AGENTS_FINISHING_TIMEOUT = 5 * 60 * 1000; // 5 min

    private static boolean allAgentsAttached;

    private static List<String> attachedAgents = new ArrayList<String>();

    private static boolean allAgentsFinished;

    private static List<String> finishedAgents = new ArrayList<String>();

    private static boolean targetApplicationInitialized;

    static protected AODTargetArgParser argParser;

    protected static Log log;

    static private Object monitor = new Object();

    /*
     * Methods
     *  - agentLoaded(String agentName) and
     *  - agentFinished(String agentName, boolean finishedSuccessfully)
     *  are called from test agents to notify target application about its status
     */

    public static void agentLoaded(String agentName) {
        synchronized (monitor) {
            if (!targetApplicationInitialized)
                waitForTargetApplicationInitialization();

            // check test logic
            if (attachedAgents.contains(agentName)) {
                setStatusFailed("Agent '" + agentName + "' already attached");

                // let TargetApplication complete execution in case of error
                allAgentsAttached = true;
                monitor.notifyAll();

                throw new TestBug("Agent '" + agentName + "' calls method 'agentLoaded' more than 1 time");
            } else {
                attachedAgents.add(agentName);

                log.display("Agent '" + agentName + "' was loaded");

                allAgentsAttached = (attachedAgents.size() == argParser.getExpectedAgentsNumber());

                if (allAgentsAttached)
                    monitor.notifyAll();

                // check test logic
                if (attachedAgents.size() > argParser.getExpectedAgentsNumber()) {
                    setStatusFailed("Unexpected agent attached (expected agents number: " +
                            argParser.getExpectedAgentsNumber() +
                            ", but " + attachedAgents.size() + " agents were loaded)");

                    throw new TestBug("More agents attached than it was expected" +
                                " (expected: " + argParser.getExpectedAgentsNumber() +
                                ", attached: " + attachedAgents.size() + ")");
                }
            }
        }
    }

    public static void agentFinished(String agentName, boolean finishedSuccessfully) {
        synchronized (monitor) {
            // check test logic
            if (!targetApplicationInitialized)
                throw new TestBug("Method 'agentFinished' was called before TargetApplication was initialized");

            boolean algorithmError = false;
            String errorMessage = "Test algorithm error:";

            if (!attachedAgents.contains(agentName)) {
                algorithmError = true;
                errorMessage += " agent '" + agentName + "' didn't call method 'agentLoaded';";
                log.complain(errorMessage);
            }

            if (finishedAgents.contains(agentName)) {
                algorithmError = true;
                errorMessage += " agent '" + agentName + "' already called method 'agentFinished';";
                log.complain(errorMessage);
            }

            if (algorithmError) {
                // let TargetApplication complete execution in case of error
                allAgentsFinished = true;
                monitor.notifyAll();

                throw new TestBug(errorMessage);
            } else {
                finishedAgents.add(agentName);

                log.display("Agent '" + agentName + "' finished execution (finishedSuccessfully: " + finishedSuccessfully + ")");

                if (!finishedSuccessfully)
                    setStatusFailed("Agent '" + agentName + " finished with error status");

                allAgentsFinished = (finishedAgents.size() == argParser.getExpectedAgentsNumber());

                if (allAgentsAttached)
                    monitor.notifyAll();
            }
        }
    }

    /*
     * This method is called from the method 'agentLoaded' in case
     * when target application isn't initialized yet at the moment
     * when agent is connecting
     */
    static private void waitForTargetApplicationInitialization() {
        synchronized (monitor) {
            while (!targetApplicationInitialized) {
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    // should never happen
                    exitAsFailed(e);
                }
            }
        }
    }

    /*
     * This method is introduced to let subclasses to create its own parsers
     */
    protected AODTargetArgParser createArgParser(String[] args) {
        return new AODTargetArgParser(args);
    }

    /*
     * Target application initialization
     */
    private void initTargetApplication(String[] args) {
        synchronized (monitor) {
            if (targetApplicationInitialized)
                throw new TestBug("TargetApplication already initialized");

            log = new Log(System.out, true);

            argParser = createArgParser(args);

            // test-specific initialization
            init(args);

            targetApplicationInitialized = true;
            monitor.notifyAll();
        }
    }

    static private void waitAgentsConnection() {
        synchronized (monitor) {
            long waitFinishTime = System.currentTimeMillis() + AGENTS_CONNECTION_TIMEOUT;

            while (!allAgentsAttached && (System.currentTimeMillis() < waitFinishTime)) {
                try {
                    monitor.wait(AGENTS_CONNECTION_TIMEOUT);
                } catch (InterruptedException e) {
                    // should never happen
                    exitAsFailed(e);
                }
            }
        }

        if (!allAgentsAttached) {
            exitAsFailed("Agents didn't attach in " + AGENTS_CONNECTION_TIMEOUT + "ms, stop execution " +
                    "(expected agents number: " + argParser.getExpectedAgentsNumber() +
                    ", attached agents number: " + attachedAgents.size() + ")");
        }
    }

    static private void waitAgentsFinishing() {
        synchronized (monitor) {
            long waitFinishTime = System.currentTimeMillis() + AGENTS_FINISHING_TIMEOUT;

            while (!allAgentsFinished && (System.currentTimeMillis() < waitFinishTime)) {
                try {
                    monitor.wait(AGENTS_FINISHING_TIMEOUT);
                } catch (InterruptedException e) {
                    // should never happen
                    exitAsFailed(e);
                }
            }
        }

        if (!allAgentsFinished)
            exitAsFailed("Agents didn't finish in " + AGENTS_FINISHING_TIMEOUT + "ms, stop execution " +
                        "(attached agents number: " + attachedAgents.size() +
                        ", finished agents number: " + finishedAgents.size() + ")");
    }

    /*
     * Print error message and set failed status, but don't exit
     */

    static public void setStatusFailed(String message) {
        testFailed = true;
        log.complain(message);
    }

    static public void setStatusFailed(Throwable t) {
        testFailed = true;
        log.complain("Unexpected exception: " + t);
        t.printStackTrace(log.getOutStream());
    }

    /*
     * Print error message and exit with fail status
     */
    static protected void exitAsFailed(String errorMessage) {
        try {
            log.complain(errorMessage);
            log.complain("Stop execution");
        } finally {
            System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_FAILED);
        }
    }

    /*
     * Print error message and exit with fail status
     */
    static protected void exitAsFailed(Throwable t) {
        try {
            log.complain("Unexpected exception was thrown during TargetApplication execution: " + t);
            t.printStackTrace(log.getOutStream());
            log.display("Stop execution");
        } finally {
            System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_FAILED);
        }
    }

    /*
     * Following 3 methods can be overridden in subclasses:
     *
     *  - init(String[] args)
     *
     *  - targetApplicationActions()
     *
     *  - afterAgentsFinished()
     */

    /*
     * Test-specific initialization
     */
    protected void init(String[] args) {

    }

    protected void targetApplicationActions() throws Throwable {
        // do nothing by default
    }

    protected void afterAgentsFinished() {
        // do nothing by default
    }

    public final void runTargetApplication(String[] args) {
        initTargetApplication(args);

        SocketIOPipe pipe = null;

        try {
            if (argParser.getPort() > 0) {
                /*
                 * When target application initialized send signal to AODTestRunner
                 */
                pipe = SocketIOPipe.createClientIOPipe(log, "localhost", argParser.getPort(), 0);
                log.display("Sending signal '" + AODTestRunner.SIGNAL_READY_FOR_ATTACH + "'");
                pipe.println(AODTestRunner.SIGNAL_READY_FOR_ATTACH);
            }

            log.display("Waiting for agents connection");
            waitAgentsConnection();
            log.display("All expected agents connected");

            try {
                targetApplicationActions();
            } catch (Throwable e) {
                /*
                 * If something goes wrong during test execution it is better
                 * to exit without waiting for agents
                 */

                if (pipe != null)
                    pipe.close();

                exitAsFailed(e);
            }

            log.display("Waiting for agents finishing");
            waitAgentsFinishing();
            log.display("All agents finished execution");

            afterAgentsFinished();

            if (pipe != null) {
                /*
                 * Don't finish execution until AODTestRunner attached agents
                 */
                String signal = pipe.readln();
                log.display("Signal received: '" + signal + "'");
                if ((signal == null) || !signal.equals(AODTestRunner.SIGNAL_FINISH))
                    throw new TestBug("Unexpected AODTestRunner signal: '" + signal + "'");

                if (testFailed) {
                    if (pipe != null)
                        pipe.close();

                    exitAsFailed("Error happened during TargetApplication execution (see error messages for details)");
                } else {
                    log.display("Test passed");
                }
            }
        } finally {
            if (pipe != null)
                pipe.close();
        }
    }

    public static void main(String[] args) {
        new TargetApplicationWaitingAgents().runTargetApplication(args);
    }
}
