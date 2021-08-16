/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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


/*
 * @test
 *
 * @summary converted from VM Testbase nsk/jvmti/Agent_OnUnload/agentonunload001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test exercises JVMTI thread function Agent_OnUnload()().
 *     This test checks that Agent_OnUnload() is invoked on shutdown,
 *     after class execution. This test does not create JVMTI environment.
 *     The test uses native method checkLoadStatus() to check value of
 *     internal native variable 'status' set by JVM_OnUnload(). Also the test
 *     uses sh script to check if JVM_OnUnload() was invoked and printed
 *     key message to stdout stream.
 *     If JVM_OnUnload() was invoked before class has been executed, then
 *     checkLoadStatus() returns FAILED and the test fails with exit status 97.
 *     If JVM_OnUnload was not executed and no key message was printed to
 *     stdout stream, then test fails with exit status 97.
 *     Otherwise, the test passes with exit status 95.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native TestDriver
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestDriver {
    public static void main(String[] args) throws Exception {
        OutputAnalyzer oa = ProcessTools.executeTestJvm(
                "-agentlib:agentonunload001=-waittime=5",
                nsk.jvmti.Agent_OnUnload.agentonunload001.class.getName());
        oa.shouldHaveExitValue(95);
        oa.stdoutShouldContain("KEY PHRASE: Agent_OnUnload() was invoked");
    }
}

