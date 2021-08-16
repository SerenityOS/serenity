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
 * @summary converted from VM Testbase nsk/jvmti/AttachOnDemand/attach002a.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, redefine, feature_282, vm6, jdk]
 * VM Testbase readme:
 * Description :
 *     Test tries to load jvmti agent to the VM after the VM has started using
 *     Attach API (com.sun.tools.attach).
 *     Test is based on the nsk.share.aod framework. In the terms of this framework
 *     java application running in the VM where agent is loaded to is called 'target application'.
 *     This is regression test for "5002268: Allow class sharing use with RedefineClasses".
 *     Test tries to redefine class java.lang.InterruptedException (class from the system jar which
 *     is included in the shared archive).
 *     (5002268 was fixed in JDK6 b86, with earlier builds this test fails because of JVMTI function
 *     RedefineClasses fails with error JVMTI_ERROR_UNMODIFIABLE_CLASS).
 *     Test scenario:
 *         - during initialization agent tries to get capabilities 'can_generate_vm_object_alloc_events'
 *         and 'can_redefine_classes' and enables VMObjectAlloc events
 *         - target application creates instance of InterruptedException using InterruptedException.class.newInstance,
 *         after this agent should receive VMObjectAlloc event
 *         - agent receives VMObjectAlloc event, redefines InterruptedException class and finishes work
 *         - target application creates one more instance of Interrupted exception and checks that redefinition
 *         really had effect
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.share.aod.AODTestRunner
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver ExecDriver --cmd
 *      ${compile.jdk}/bin/javac
 *      --patch-module java.base=${test.src}/newclass00/java.base
 *      -d bin/newclass00
 *      ${test.src}/newclass00/java.base/java/lang/InterruptedException.java
 *
 * @build nsk.jvmti.AttachOnDemand.attach002a.attach002aTarget
 * @run main/othervm/native
 *      nsk.share.aod.AODTestRunner
 *      -jdk ${test.jdk}
 *      -target nsk.jvmti.AttachOnDemand.attach002a.attach002aTarget
 *      -javaOpts="-XX:+UsePerfData ${test.vm.opts} ${test.java.opts}"
 *      -na attach002aAgent00=-pathToNewByteCode=./bin/newclass00
 */

