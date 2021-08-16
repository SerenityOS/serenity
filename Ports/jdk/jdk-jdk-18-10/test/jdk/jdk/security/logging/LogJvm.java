/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.security.logging;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public final class LogJvm {
    private final static String LOGGING_ENABLED= "LOGGING_ENABLED";
    private final static String LOGGING_DISABLED= "LOGGING_DISABLED";

    private final static boolean debug = false;

    private final List<String> expectedLogMessages = new ArrayList<>();
    private final Class<?> clazz;
    private final boolean loggingEnabled;

    public LogJvm(Class<?> clazz, String[] args) {
        this.clazz = clazz;
        this.loggingEnabled = Arrays.asList(args).contains(LOGGING_ENABLED);
        ensureLogging(args);
    }

    private void ensureLogging(String[] args) {
        for(String s : args) {
            if (s.equals(LOGGING_ENABLED) || s.equals(LOGGING_DISABLED)) {
                return;
            }
        }
        throw new RuntimeException(LogJvm.class.getName() +
            " requires command line parameter " + LOGGING_ENABLED +
            " or " + LOGGING_DISABLED);
    }

    public void addExpected(String logMsg) {
        expectedLogMessages.add(logMsg);
    }

    public void testExpected() throws Exception {
        OutputAnalyzer out = launchJVM();
        if (debug) {
            System.out.println("STDOUT DEBUG:\n " + out.getStdout());
            System.out.println("\nSTDERR DEBUG:\n " + out.getStderr());
        }
        if (loggingEnabled) {
            testLoggingEnabled(out);
        } else {
            testLoggingDisabled(out);
        }
    }

    public OutputAnalyzer launchJVM() throws Exception {
        List<String> args = new ArrayList<>();
        if (loggingEnabled) {
            args.add("-Djava.util.logging.config.file=" +
                Paths.get(System.getProperty("test.src", "."), "logging.properties"));
        }
        args.add("--add-exports");
        args.add("java.base/jdk.internal.event=ALL-UNNAMED");
        args.add(clazz.getName());
        System.out.println(args);
        OutputAnalyzer out = ProcessTools.executeTestJava(args.toArray(new String[0]));
        out.shouldHaveExitValue(0);
        return out;
    }

    private void testLoggingDisabled(OutputAnalyzer out) {
        for (String expected : expectedLogMessages) {
            out.shouldNotContain(expected);
        }
    }

    private void testLoggingEnabled(OutputAnalyzer out) {
        for (String expected : expectedLogMessages) {
            out.shouldContain(expected);
        }
    }
}
