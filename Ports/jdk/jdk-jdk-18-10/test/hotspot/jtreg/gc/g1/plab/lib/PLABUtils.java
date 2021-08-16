/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package gc.g1.plab.lib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Utils;

/**
 * Utilities for PLAB testing.
 */
public class PLABUtils {

    /**
     * PLAB tests default options list
     */
    private final static String[] GC_TUNE_OPTIONS = {
        "-XX:+UseG1GC",
        "-XX:G1HeapRegionSize=1m",
        "-XX:OldSize=64m",
        "-XX:-UseAdaptiveSizePolicy",
        "-XX:MaxTenuringThreshold=1",
        "-XX:-UseTLAB",
        "-XX:SurvivorRatio=1"
    };

    /**
     * GC logging options list.
     */
    private final static String G1_PLAB_LOGGING_OPTIONS[] = {
        "-Xlog:gc=debug,gc+plab=debug,gc+heap=debug"
    };

    /**
     * List of options required to use WhiteBox.
     */
    private final static String WB_DIAGNOSTIC_OPTIONS[] = {
        "-Xbootclasspath/a:.",
        "-XX:+UnlockDiagnosticVMOptions",
        "-XX:+WhiteBoxAPI"
    };

    /**
     * Prepares options for testing.
     *
     * @param options - additional options for testing
     * @return List of options
     */
    public static List<String> prepareOptions(List<String> options) {
        if (options == null) {
            throw new IllegalArgumentException("Options cannot be null");
        }
        List<String> executionOtions = new ArrayList<>(
                Arrays.asList(Utils.getTestJavaOpts())
        );
        Collections.addAll(executionOtions, WB_DIAGNOSTIC_OPTIONS);
        Collections.addAll(executionOtions, G1_PLAB_LOGGING_OPTIONS);
        Collections.addAll(executionOtions, GC_TUNE_OPTIONS);
        executionOtions.addAll(options);
        return executionOtions;
    }

    /**
     * Common check for test PLAB application's results.
     * @param out OutputAnalyzer for checking
     * @throws RuntimeException
     */
    public static void commonCheck(OutputAnalyzer out) throws RuntimeException {
        if (out.getExitValue() != 0) {
            System.out.println(out.getOutput());
            throw new RuntimeException("Exit code is not 0");
        }
        // Test expects only WhiteBox initiated GC.
        out.shouldNotContain("Pause Young (G1 Evacuation Pause)");
    }
}
