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
 * @summary converted from VM Testbase nsk/jvmti/AttachOnDemand/attach001.
 * VM Testbase keywords: [jpda, jvmti, noras, feature_282, vm6, jdk]
 * VM Testbase readme:
 * Description :
 *     Test tries to load java and jvmti agents to the VM after the VM has started using
 *     Attach API (com.sun.tools.attach).
 *     Test is based on the nsk.share.aod framework. In the terms of this framework
 *     java application running in the VM where agent is loaded to is called 'target application'.
 *     In this test target application tries to attach java and native agents to the same VM where target application
 *     is running. Test just checks that agents can connect and run (agents don't perform any specific
 *     checks).
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.AttachOnDemand.attach001.attach001TestRunner
 *
 * @comment create SimpleAgent00.jar in current directory
 * @build nsk.jvmti.AttachOnDemand.sharedAgents.SimpleAgent00
 * @run driver jdk.test.lib.helpers.ClassFileInstaller nsk.jvmti.AttachOnDemand.sharedAgents.SimpleAgent00
 * @run driver ExecDriver --cmd
 *      ${compile.jdk}/bin/jar
 *      -cfm SimpleAgent00.jar ${test.src}/../sharedAgents/SimpleAgent00.mf
 *      nsk/jvmti/AttachOnDemand/sharedAgents/SimpleAgent00.class
 *
 * @run main/othervm/native
 *      -XX:+UsePerfData
 *      -Djdk.attach.allowAttachSelf
 *      nsk.jvmti.AttachOnDemand.attach001.attach001TestRunner
 *      -jdk ${test.jdk}
 *      -javaOpts="-XX:+UsePerfData -Djdk.attach.allowAttachSelf ${test.vm.opts} ${test.java.opts}"
 *      -ja SimpleAgent00.jar
 *      -na simpleAgent00
 */

package nsk.jvmti.AttachOnDemand.attach001;

import nsk.share.Failure;
import nsk.share.aod.*;
import nsk.share.test.TestUtils;

/*
 * Test tries to attach agents to the same VM where target application is running
 * (test starts target application class in the separate thread instead of separate process).
 */
public class attach001TestRunner extends AODTestRunner {
    attach001TestRunner(String[] args) {
        super(args);
    }

    class TargetThread extends Thread {
        boolean finishedSuccessfully;

        public void run() {
            try {
                new TargetApplicationWaitingAgents().runTargetApplication(new String[] {
                        "-" + AODTargetArgParser.agentsNumberParam,
                        Integer.valueOf(argParser.getAgents().size()).toString() });

                finishedSuccessfully = true;
            } catch (Throwable t) {
                log.complain("Unexpected exception: " + t);
                t.printStackTrace(log.getOutStream());
            }
        }
    }

    protected void runTest() {
        try {
            TargetThread thread = new TargetThread();
            thread.start();

            // default test actions - attach agents to the given VM
            doTestActions(getCurrentVMId());

            thread.join();

            if (!thread.finishedSuccessfully) {
                TestUtils.testFailed("Unexpected error during test execution (see log for details)");
            }
        } catch (Failure f) {
            throw f;
        } catch (Throwable t) {
            TestUtils.unexpctedException(t);
        }
    }

    public static void main(String[] args) {
        new attach001TestRunner(args).runTest();
    }
}
