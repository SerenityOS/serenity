/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.failurehandler.jtreg;

import com.sun.javatest.regtest.TimeoutHandler;
import jdk.test.failurehandler.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;

/**
 * A timeout handler for jtreg, which gathers information about the timed out
 * process and its children.
 */
public class GatherProcessInfoTimeoutHandler extends TimeoutHandler {
    private static final boolean HAS_NATIVE_LIBRARY;
    static {
        boolean value = true;
        try {
            System.loadLibrary("timeoutHandler");
        } catch (UnsatisfiedLinkError ignore) {
            // not all os need timeoutHandler native-library
            value = false;
        }
        HAS_NATIVE_LIBRARY = value;
    }
    private static final String LOG_FILENAME = "processes.log";
    private static final String OUTPUT_FILENAME = "processes.html";

    public GatherProcessInfoTimeoutHandler(PrintWriter jtregLog, File outputDir,
                                           File testJdk) {
        super(jtregLog, outputDir, testJdk);
    }

    /**
     * Runs various actions for jtreg timeout handler.
     *
     * <p>Please see method code for the actions.
     */
    @Override
    protected void runActions(Process process, long pid)
            throws InterruptedException {
        Path workDir = outputDir.toPath();

        String name = getClass().getName();
        PrintWriter actionsLog;
        try {
            // try to open a separate file for action log
            actionsLog = new PrintWriter(new FileWriter(
                    workDir.resolve(LOG_FILENAME).toFile(), true), true);
        } catch (IOException e) {
            // use jtreg log as a fallback
            actionsLog = log;
            actionsLog.printf("ERROR: %s cannot open log file %s : %s", name,
                    LOG_FILENAME, e.getMessage());
        }
        try {
            actionsLog.printf("%s ---%n", name);

            File output = workDir.resolve(OUTPUT_FILENAME).toFile();
            try {
                PrintWriter pw = new PrintWriter(new FileWriter(output, true), true);
                runGatherer(name, workDir, actionsLog, pw, pid);
            } catch (IOException e) {
                actionsLog.printf("IOException: cannot open output file[%s] : %s",
                        output, e.getMessage());
                e.printStackTrace(actionsLog);
            }
        } finally {
            actionsLog.printf("--- %s%n", name);
            // don't close jtreg log
            if (actionsLog != log) {
                actionsLog.close();
            } else {
                log.flush();
            }
        }
    }

    private void runGatherer(String name, Path workDir, PrintWriter log,
                             PrintWriter out, long pid) {
        try (HtmlPage html = new HtmlPage(out)) {
            ProcessInfoGatherer gatherer = new GathererFactory(
                    OS.current().family,
                    workDir, log, testJdk.toPath()).getProcessInfoGatherer();
            try (ElapsedTimePrinter timePrinter
                         = new ElapsedTimePrinter(new Stopwatch(), name, log)) {
                gatherer.gatherProcessInfo(html.getRootSection(), pid);
            }
        } catch (Throwable e) {
            log.printf("ERROR: exception in timeout handler %s:", name);
            e.printStackTrace(log);
        }
    }
}
