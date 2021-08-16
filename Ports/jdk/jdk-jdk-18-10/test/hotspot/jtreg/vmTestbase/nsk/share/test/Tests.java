/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.test;

import nsk.share.log.*;
import nsk.share.runner.*;
import nsk.share.TestFailure;

public class Tests {
        protected static class TestRunner {
                protected String[] args;
                private Log log;
                private MultiRunner runner;
                private RunParams runParams;
                private Test test;

                public TestRunner(Test test, String[] args) {
                        this.args = args;
                        this.test = test;
                }

                public synchronized Log getLog() {
                        if (log == null) {
                                log = new LogSupport(System.out);
                        }
                        return log;
                }

                private synchronized RunParams getRunParams() {
                        if (runParams == null) {
                                runParams = RunParams.getInstance();
                                runParams.parseCommandLine(args);
                        }
                        return runParams;
                }

                public void configure(Object o) {
                        if (o instanceof LogAware)
                                ((LogAware) o).setLog(getLog());
                        if (o instanceof MultiRunnerAware)
                                ((MultiRunnerAware) o).setRunner(getRunner());
                        if (o instanceof RunParamsAware)
                                ((RunParamsAware) o).setRunParams(getRunParams());
                }

                private synchronized MultiRunner getRunner() {
                        if (runner == null) {
                                runner = new ThreadsRunner();
                                configure(runner);
                        }
                        return runner;
                }


                public void execute(Object o) {
                        if (o instanceof Initializable)
                                ((Initializable) o).initialize();
                        int exitCode = 0;
                        try {
                                if (o instanceof Runnable)
                                        ((Runnable) o).run();
                                if (o instanceof TestExitCode)
                                        exitCode = ((TestExitCode) o).getExitCode();
                        } catch (RuntimeException t) {
                                getLog().error(t);
                                exitCode = 97;
                        }
                        if (exitCode != 95 && exitCode != 0)
                                throw new TestFailure("Test exit code: " + exitCode);
                        //System.exit(exitCode);
                }

                public void run() {
                        configure(test);
                        execute(test);
                }
        }


        public static void runTest(Test test, String[] args) {
                new TestRunner(test, args).run();
        }
}
