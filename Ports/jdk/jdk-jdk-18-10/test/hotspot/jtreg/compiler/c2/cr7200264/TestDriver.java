/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

package compiler.c2.cr7200264;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestDriver {
    private final Map<String, Long> expectedVectorizationNumbers
            = new HashMap<>();

    public void addExpectedVectorization(String v, long num) {
        expectedVectorizationNumbers.put(v, num);
    }

    public void run() throws Throwable {
        verifyVectorizationNumber(executeApplication());
    }

    private List<String> executeApplication() throws Throwable {
        OutputAnalyzer outputAnalyzer = ProcessTools.executeTestJvm(
            "-Xbatch",
            "-XX:-TieredCompilation",
            "-XX:+PrintCompilation",
            "-XX:+TraceNewVectors",
            TestIntVect.class.getName());
        outputAnalyzer.shouldHaveExitValue(0);
        return outputAnalyzer.asLines();
    }

    private void verifyVectorizationNumber(List<String> vectorizationLog) {
        for (Map.Entry<String, Long> entry : expectedVectorizationNumbers.entrySet()) {
            String v = entry.getKey();
            long actualNum = vectorizationLog.stream()
                    .filter(s -> s.contains(v)).count();
            long expectedNum = entry.getValue();
            Asserts.assertGTE(actualNum, expectedNum,
                              "Unexpected " + entry.getKey() + " number");
        }
    }
}
