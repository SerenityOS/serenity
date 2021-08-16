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

/*
 * @test
 *
 * @summary converted from VM Testbase nsk/jvmti/AttachOnDemand/attach036.
 * VM Testbase keywords: [jpda, jvmti, noras, feature_282, vm6, jdk]
 * VM Testbase readme:
 * Description :
 *     Test tries to load java agents to the VM after the VM has started using
 *     Attach API (com.sun.tools.attach).
 *     This is negative test, it checks that attempt to attach java agent fails if
 *     main agent class has no proper 'agentmain' method.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.AttachOnDemand.attach036.attach036TestRunner
 *
 * @comment create attach036Agent00.jar in current directory
 * @build nsk.jvmti.AttachOnDemand.attach036.attach036Agent00
 * @run driver jdk.test.lib.helpers.ClassFileInstaller nsk.jvmti.AttachOnDemand.attach036.attach036Agent00
 * @run driver ExecDriver --cmd
 *      ${compile.jdk}/bin/jar
 *      -cfm attach036Agent00.jar ${test.src}/attach036Agent00.mf
 *      nsk/jvmti/AttachOnDemand/attach036/attach036Agent00.class
 *
 * @run main/othervm
 *      -XX:+UsePerfData
 *      -Djdk.attach.allowAttachSelf
 *      nsk.jvmti.AttachOnDemand.attach036.attach036TestRunner
 *      -jdk ${test.jdk}
 *      -ja attach036Agent00.jar
 */

package nsk.jvmti.AttachOnDemand.attach036;

import com.sun.tools.attach.AgentInitializationException;
import nsk.share.*;
import nsk.share.aod.*;
import nsk.share.test.TestUtils;

/*
 * Negative test: checks that java agent fails to attach if main agent class
 * has no proper agentmain method
 * (test tries to attach java agent to the same VM where attach036TestRunner is running)
 */
public class attach036TestRunner extends AODTestRunner {

    public attach036TestRunner(String[] args) {
        super(args);
    }

    protected void runTest() {
        try {
            String currentVMId = getCurrentVMId();

            AgentsAttacher attacher = new AgentsAttacher(currentVMId, argParser.getAgents(), log);

            try {
                attacher.attachAgents();
                TestUtils.testFailed("Expected AgentLoadException wasn't thrown");
            } catch (Failure failure) {
                if (failure.getCause() != null) {
                    if (failure.getCause() instanceof AgentInitializationException)
                        log.display("Expected AgentInitializationException was thrown");
                    else
                        TestUtils.testFailed("Unexpected exception was thrown instead of AgentInitializationException: " + failure);
                } else
                    throw failure;
            }
        } catch (Failure f) {
            throw f;
        } catch (Throwable t) {
            TestUtils.unexpctedException(t);
        }
    }

    public static void main(String[] args) {
        new attach036TestRunner(args).runTest();
    }
}
