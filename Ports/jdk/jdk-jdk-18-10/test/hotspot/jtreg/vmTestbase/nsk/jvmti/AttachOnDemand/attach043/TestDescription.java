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
 * @summary converted from VM Testbase nsk/jvmti/AttachOnDemand/attach043.
 * VM Testbase keywords: [jpda, jvmti, noras, feature_282, vm6, jdk]
 * VM Testbase readme:
 * Description :
 *     Test tries to load 1 jvmti and 2 java agents to the VM after the VM has started using
 *     Attach API (com.sun.tools.attach).
 *     Test is based on the nsk.share.aod framework. In this framework each java
 *     agent starts new thread from the 'agentmain' method, and all test checks are executed
 *     in this thread. Also in the terms of this framework java application running in the VM where
 *     agent are loaded to is called 'target application'.
 *     This test uses target application and native agent from test attach042, and in addition to native
 *     agent test also loads to the same target application 2 java agents. Test uses java agents from tests
 *     attach003, attach031.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.share.aod.AODTestRunner
 *        nsk.jvmti.AttachOnDemand.attach042.attach042Target
 *
 * @comment create SimpleAgent00.jar in current directory
 * @build nsk.jvmti.AttachOnDemand.sharedAgents.SimpleAgent00
 * @run driver jdk.test.lib.helpers.ClassFileInstaller nsk.jvmti.AttachOnDemand.sharedAgents.SimpleAgent00
 * @run driver ExecDriver --cmd
 *      ${compile.jdk}/bin/jar
 *      -cfm SimpleAgent00.jar ${test.src}/../sharedAgents/SimpleAgent00.mf
 *      nsk/jvmti/AttachOnDemand/sharedAgents/SimpleAgent00.class
 *
 * @comment create attach0031Agent00.jar in current directory
 * @build nsk.jvmti.AttachOnDemand.attach031.attach031Agent00
 * @run driver jdk.test.lib.helpers.ClassFileInstaller nsk.jvmti.AttachOnDemand.attach031.attach031Agent00
 * @run driver ExecDriver --cmd
 *      ${compile.jdk}/bin/jar
 *      -cfm attach031Agent00.jar ${test.src}/../attach031/attach031Agent00.mf
 *      nsk/jvmti/AttachOnDemand/attach031/attach031Agent00.class
 *
 * @run main/othervm/native
 *      nsk.share.aod.AODTestRunner
 *      -jdk ${test.jdk}
 *      -target nsk.jvmti.AttachOnDemand.attach042.attach042Target
 *      -javaOpts="-XX:+UsePerfData ${test.vm.opts} ${test.java.opts}"
 *      -na attach042Agent00
 *      -ja SimpleAgent00.jar,attach031Agent00.jar
 */

