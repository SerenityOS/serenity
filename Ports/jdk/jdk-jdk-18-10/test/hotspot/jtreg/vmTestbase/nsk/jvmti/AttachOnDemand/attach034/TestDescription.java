/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase nsk/jvmti/AttachOnDemand/attach034.
 * VM Testbase keywords: [jpda, jvmti, noras, feature_282, vm6, jdk, quarantine]
 * VM Testbase comments: 8042145
 * VM Testbase readme:
 * Description :
 *     Test tries to load 2 java agents to the VM after the VM has started using
 *     Attach API (com.sun.tools.attach).
 *     Test is based on the nsk.share.aod framework. In this framework each java
 *     agent starts new thread from the 'agentmain' method, and all test checks are executed
 *     in this thread.
 *     In this test class of the second attached agent extends class of the first attached agent.
 *     Test just checks that agents can be loaded and run without problems,
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.share.aod.AODTestRunner
 *        nsk.share.aod.TargetApplicationWaitingAgents
 *
 * @comment create attach034Agent00.jar in current directory
 * @build nsk.jvmti.AttachOnDemand.attach034.attach034Agent00
 * @run driver jdk.test.lib.helpers.ClassFileInstaller nsk.jvmti.AttachOnDemand.attach034.attach034Agent00
 * @run driver ExecDriver --cmd
 *      ${compile.jdk}/bin/jar
 *      -cfm attach034Agent00.jar ${test.src}/attach034Agent00.mf
 *      nsk/jvmti/AttachOnDemand/attach034/attach034Agent00.class
 *
 * @comment create AgentParent.jar in current directory
 * @build nsk.jvmti.AttachOnDemand.attach034.AgentParent
 * @run driver jdk.test.lib.helpers.ClassFileInstaller nsk.jvmti.AttachOnDemand.attach034.AgentParent
 * @run driver ExecDriver --cmd
 *      ${compile.jdk}/bin/jar
 *      -cfm AgentParent.jar ${test.src}/AgentParent.mf
 *      nsk/jvmti/AttachOnDemand/attach034/AgentParent.class
 *
 * @run main/othervm
 *      nsk.share.aod.AODTestRunner
 *      -jdk ${test.jdk}
 *      -javaOpts="-XX:+UsePerfData ${test.vm.opts} ${test.java.opts}"
 *      -target nsk.share.aod.TargetApplicationWaitingAgents
 *      -ja AgentParent.jar,attach034Agent00.jar
 */

