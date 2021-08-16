/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.java.lang.instrument;

import java.lang.RuntimeException;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class NegativeAgentRunner {

    public static void main(String argv[]) throws Exception {
        if (argv.length != 2) {
            throw new RuntimeException("Agent and exception class names are expected in arguments");
        }
        String agentClassName = argv[0];
        String excepClassName = argv[1];
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "-javaagent:" + agentClassName + ".jar", "-Xmx128m", "-XX:-CreateCoredumpOnCrash",
                agentClassName);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain(excepClassName);
        if (0 == output.getExitValue()) {
            throw new RuntimeException("Expected error but got exit value 0");
        }
    }
}
