/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase nsk/jvmti/AttachOnDemand/attach050.
 * VM Testbase keywords: [jpda, jvmti, noras, feature_282, vm6, jdk]
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.AttachOnDemand.attach050.attach050TestRunner
 *        nsk.share.aod.TargetApplicationWaitingAgents
 * @run main/othervm/native
 *      nsk.jvmti.AttachOnDemand.attach050.attach050TestRunner
 *      -jdk ${test.jdk}
 *      -target nsk.share.aod.TargetApplicationWaitingAgents
 *      -javaOpts="-XX:+UsePerfData -XX:+EnableDynamicAgentLoading ${test.vm.opts} ${test.java.opts}"
 *      -na attach050Agent00
 */

package nsk.jvmti.AttachOnDemand.attach050;

import nsk.share.Failure;
import nsk.share.aod.*;

public class attach050TestRunner extends AODTestRunner {

    private static final String ON_UNLOAD_MARKER = "attach050.on_unload";

    attach050TestRunner(String[] args) {
        super(args);
    }

    protected void postTargetExitHook() {

        for (String targetAppOutLine : targetAppExecutor.getProcessOut())
            if (targetAppOutLine.contains(ON_UNLOAD_MARKER))
                return;

        throw new Failure("Agent_OnUnload has not been called");
    }

    public static void main(String[] args) {
        new attach050TestRunner(args).runTest();
    }
}
