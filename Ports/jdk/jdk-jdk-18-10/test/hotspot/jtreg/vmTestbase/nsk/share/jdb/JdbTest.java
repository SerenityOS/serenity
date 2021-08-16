/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.jdb;

import nsk.share.*;
import nsk.share.jpda.*;

import java.io.*;
import java.util.*;

public abstract class JdbTest {
    public static final int PASSED = 0;            // Exit code for passed test
    public static final int FAILED = 2;            // Exit code for failed test
    public static final int JCK_STATUS_BASE = 95;  // Standard JCK-compatible exit code bias

    /* Flag if the test passes */
    protected boolean success = true;

    /* Flag if debuggee should fail in a test */
    protected static boolean debuggeeShouldFail = false;

    /* Handler of command line arguments. */
    protected static JdbArgumentHandler argumentHandler = null;

    /* Log class to print log messages. */
    protected static Log log = null;

    protected static Jdb jdb = null;
    protected static Debuggee debuggee = null;
    protected static Launcher launcher = null;
    protected static String debuggeeClass = "";
    protected static String firstBreak = "";
    protected static String lastBreak = "";
    protected static String compoundPromptIdent = null;

    /* Constructors */
    protected JdbTest (boolean debuggeeShouldFail) {
        this.debuggeeShouldFail = debuggeeShouldFail;
    }

    protected JdbTest () {
        this.debuggeeShouldFail = false;
    }

    abstract protected void runCases();

    protected boolean shouldPass() {
        return false;
    }

    protected void failure(String errMessage) {
        success = false;
        log.complain(errMessage);
    }

    protected void display(String message) {
        log.display(message);
    }

    protected void launchJdbAndDebuggee(String debuggeeClass) throws Exception {
        launcher = new Launcher(argumentHandler, log);
        launcher.launchJdbAndDebuggee(debuggeeClass);
        jdb = launcher.getJdb();

        if (jdb == null) {
           throw new Failure("jdb object points to null");
        }
        if (debuggeeClass != null) {
            if (jdb.terminated()) {
                throw new Failure("jdb exited before testing with code " + jdb.waitFor());
            }

            if (argumentHandler.isAttachingConnector() || argumentHandler.isListeningConnector()) {
                debuggee = launcher.getDebuggee();

                if (debuggee.terminated()) {
                   throw new Failure("Debuggee exited before testing");
                }
            }
        }
    }

    protected void initJdb() {
        String[] reply;

        jdb.setCompoundPromptIdent(compoundPromptIdent);

        // wait for prompts after connection established
        if (argumentHandler.isAttachingConnector() || argumentHandler.isListeningConnector()) {
            // wait for two prompts (after connection established and VM_INIT received)
            jdb.waitForPrompt(0, false, 2);
        } else if (argumentHandler.isLaunchingConnector()) {
            // wait for one prompt (after connection established)
            jdb.waitForPrompt(0, false);
        } else {
            throw new TestBug("Unexpected connector kind: " + argumentHandler.getConnectorType());
        }

        display("Setting first breakpoint");
        jdb.setDeferredBreakpointInMethod(firstBreak);

        display("Starting debuggee class");
        jdb.startDebuggeeClass();
    }

    protected void afterJdbExit() {
    }

    protected int runTest(String argv[], PrintStream out) {
        try {
            argumentHandler = new JdbArgumentHandler(argv);
            log = new Log(out, argumentHandler);

            if (shouldPass()) {
                log.println("TEST PASSED");
                return PASSED;
            }

            try {
                launchJdbAndDebuggee(debuggeeClass);

                try {
                    initJdb();

                    /* START OF TEST CASES */
                    display("Test cases starts.");

                    runCases();

                    display("Test cases ends.");
                    /* END OF TEST CASES */

                } catch (DebuggeeUncaughtException ex) {
                    jdb.quit();
                    throw new TestFailure(ex);
                } catch (Exception e) {
                    failure("Caught unexpected exception while executing the test: " + e);
                    e.printStackTrace(log.getOutStream());
                } finally {
                    display("Waiting for jdb exits");
                    int code = jdb.waitFor(argumentHandler.getWaitTime() * 60 * 1000);
                    if (code == PASSED) {
                        display("jdb normally exited");
                        afterJdbExit();
                    } else if (code == LocalProcess.PROCESS_IS_ALIVE) {
                        failure("jdb did not exit after timeout.");
                        if (!jdb.terminated()) {
                           display("Sending quit command to jdb.");
                           jdb.quit();
                        } else {
                           throw new TestBug("code PROCESS_IS_ALIVE is returned for terminated jdb");
                        }
                    } else {
                        failure("jdb abnormally exited with code: " + code);
                    }
                    jdb = null;

                    if (debuggee != null
                            && (argumentHandler.isAttachingConnector()
                                    || argumentHandler.isListeningConnector())) {
                        display("Waiting for debuggee exits");
                        code = debuggee.waitForDebuggee();
                        if (debuggeeShouldFail) {
                            if (code == JCK_STATUS_BASE + PASSED) {
                                failure("Debuggee PASSED with exit code: " + code + " but should fail");
                            } else {
                                display("Debuggee FAILED as expected with exit code: " + code);
                            }
                        } else {
                            if (code == JCK_STATUS_BASE + PASSED) {
                                display("Debuggee PASSED with exit code: " + code);
                            } else {
                                failure("Debuggee FAILED with exit code: " + code);
                            }
                        }
//                        debuggee = null;
                    }
                }

            } catch (Exception e) {
                failure("Caught unexpected exception: " + e);
                e.printStackTrace(out);

                if (jdb != null) {
                    try {
                        jdb.finalize();
                    } catch (Throwable ex) {
                        failure("Caught exception/error while finalization of jdb:\n\t" + ex);
                        ex.printStackTrace(log.getOutStream());
                    }
                } else {
                    log.complain("jdb reference is null, cannot run jdb.finalize() method");
                }

                if (debuggee != null) {
                    debuggee.killDebuggee();
                } else {
                    log.complain("debuggee reference is null, cannot run debuggee.finalize() method");
                }

            }

            if (!success) {
                log.complain("TEST FAILED");
                return FAILED;
            }

        } catch (Exception e) {
            out.println("Caught unexpected exception while starting the test: " + e);
            e.printStackTrace(out);
            out.println("TEST FAILED");
            return FAILED;
        }
        out.println("TEST PASSED");
        return PASSED;
    }
}
