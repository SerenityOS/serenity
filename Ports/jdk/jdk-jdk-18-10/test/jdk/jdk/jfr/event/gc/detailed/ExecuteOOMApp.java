/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.gc.detailed;

import jdk.test.lib.jfr.AppExecutorHelper;
import jdk.test.lib.process.OutputAnalyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExecuteOOMApp {
    /**
     * Executes OOM application with JFR recording and returns true if OOM error happened in the
     * test thread. Adds -XX:-UseGCOverheadLimit option to avoid "java.lang.OutOfMemoryError: GC overhead limit exceeded".
     *
     * @param settings JFR settings file
     * @param jfrFilename JFR resulting recording filename
     * @param additionalVmFlags additional VM flags passed to the java
     * @param bytesToAllocate number of bytes to allocate in new object every cycle in OOM application
     * @return true - OOM application is finished as expected,i.e. OOM happened in the test thead
     *         false - OOM application is finished with OOM error which happened in the non test thread
     */
    public static boolean execute(String settings, String jfrFilename, String[] additionalVmFlags, int bytesToAllocate) throws Exception {
        List<String> additionalVmFlagsList = new ArrayList<>(Arrays.asList(additionalVmFlags));
        additionalVmFlagsList.add("-XX:-UseGCOverheadLimit");

        OutputAnalyzer out = AppExecutorHelper.executeAndRecord(settings, jfrFilename, additionalVmFlagsList.toArray(new String[0]),
                                                                OOMApp.class.getName(), String.valueOf(bytesToAllocate));

        if ((out.getExitValue() == 1 && out.getOutput().contains("java.lang.OutOfMemoryError"))) {
            return false;
        }

        out.shouldHaveExitValue(0);
        System.out.println(out.getOutput());

        return true;
    }
}
