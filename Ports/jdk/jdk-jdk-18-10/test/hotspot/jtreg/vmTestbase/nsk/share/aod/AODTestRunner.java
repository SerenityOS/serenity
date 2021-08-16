/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import nsk.share.*;
import nsk.share.jpda.SocketIOPipe;

/*
 Class AODTestRunner is part of the framework used in the AttachOnDemand tests
 (tests against Attach API, API from package com.sun.tools.attach).

 AODTestRunner is used as main class in AttachOnDemand tests, it performs following
 actions:
 - starts target application

 - finds VM id for target VM (this id is needed for dynamic attach)

 - by default AODTestRunner tries to attach specified via command line agents to target VM
 (subclasses can override this default behavior)

 - waits for target application completion

Target application class, agents that should be attached, JDK used to run target application and
VM options passed to target VM should be specified via command line.
 */
public class AODTestRunner {

    public static final String targetAppIdProperty = "vmsqe.aod.targetAppId";
    public static final String appIdProperty = "vmsqe.aod.AppId";

    public static final long TARGET_APP_CONNECT_TIMEOUT = 5 * 60 * 1000; // 5 min

    public static final long TARGET_APP_WORK_TIMEOUT = 30 * 60 * 1000; // 30 min (standard VM testbase test timeout)

    protected Log log;

    protected SocketIOPipe pipe;

    protected ProcessExecutor targetAppExecutor;

    // target application ready for attach
    public static final String SIGNAL_READY_FOR_ATTACH = "ready";

    // target application may finish execution
    public static final String SIGNAL_FINISH = "finish";

    protected AODRunnerArgParser argParser;

    protected AODTestRunner(String[] args) {
        log = new Log(System.out, true);

        argParser = createArgParser(args);
    }

    /*
     * This method is introduced to let subclasses to create its own parsers
     */
    protected AODRunnerArgParser createArgParser(String[] args) {
        return new AODRunnerArgParser(args);
    }

    protected void doTestActions(String targetVMId) throws Throwable {
        AgentsAttacher attacher = new AgentsAttacher(targetVMId, argParser.getAgents(), log);
        attacher.attachAgents();
    }

    protected String getCurrentVMId() {
        String currentVMId = "" + ProcessHandle.current().pid();
        log.display("Current VM id was identified: " + currentVMId);

        return currentVMId;
    }

    protected void runTest() {

        try {
            String targetAppId = System.getProperty(targetAppIdProperty);
            if(targetAppId == null || targetAppId.isEmpty()) {
                // use PID as default appID
                targetAppId = "" + ProcessHandle.current().pid();
            }
            /*
             * Create target application id required by the Utils.findVMIdUsingJPS
             */
            String targetAppCmd =
                    // path to java
                    argParser.getTestedJDK() + File.separator + "bin" + File.separator + "java " +
                            // VM property to identify VM running target application
                            "-D" + appIdProperty + "=" + targetAppId +  " " +
                            // VM opts
                            argParser.getJavaOpts() + " -XX:+EnableDynamicAgentLoading " +
                            // target application class
                            argParser.getTargetApp() + " " +
                            // additional target application parameter - number of
                            // agents that will be attached
                            "-" + AODTargetArgParser.agentsNumberParam + " " + argParser.getAgents().size();

            pipe = SocketIOPipe.createServerIOPipe(log, 0, TARGET_APP_CONNECT_TIMEOUT);
            targetAppCmd += " -" + AODTargetArgParser.socketPortParam + " " + pipe.getPort();

            log.display("Starting target application: " + targetAppCmd);
            targetAppExecutor = new ProcessExecutor(targetAppCmd, TARGET_APP_WORK_TIMEOUT, "TargetApp");
            targetAppExecutor.startProcess();

            /*
             * Don't try to attach agents until target application isn't initialized
             */
            String signal = pipe.readln();
            log.display("Signal received: '" + signal + "'");
            if ((signal == null) || !signal.equals(SIGNAL_READY_FOR_ATTACH))
                throw new TestBug("Unexpected TargetApplication signal: '" + signal + "'");

            String targetVMId = Long.toString(targetAppExecutor.pid());
            log.display("Target VM id was identified: " + targetVMId);

            doTestActions(targetVMId);

            /*
             * When test actions finished let target application finish execution
             */
            log.display("Sending signal: '" + SIGNAL_FINISH + "'");
            pipe.println(SIGNAL_FINISH);

            targetAppExecutor.waitForProcess();

            File file = new File(targetAppId);
            if (file.exists()) {
                file.deleteOnExit();
            }

            if (targetAppExecutor.getExitCode() != 0) {
                throw new Failure("Target application finished with non-zero code " + targetAppExecutor.getExitCode());
            }

            postTargetExitHook();

        } catch (Failure f) {
            throw f;
        } catch (Throwable t) {
            throw new Failure("Unexpected exception during test execution: " + t, t);
        } finally {
            if (pipe != null) {
                pipe.close();
            }
            if (targetAppExecutor != null) {
                targetAppExecutor.destroyProcess();
            }
        }
    }

    /*
     * Allow users of this class to specify actions to be taken after the target exits
     */
    protected void postTargetExitHook() {
        // do nothing by default
    }

    public static String createApplicationId() {
        return Long.valueOf(System.currentTimeMillis()).toString();
    }

    public static void main(String[] args) {
        new AODTestRunner(args).runTest();
    }
}
