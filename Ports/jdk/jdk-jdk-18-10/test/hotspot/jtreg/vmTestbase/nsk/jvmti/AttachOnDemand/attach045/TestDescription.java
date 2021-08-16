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
 * @summary converted from VM Testbase nsk/jvmti/AttachOnDemand/attach045.
 * VM Testbase keywords: [jpda, jvmti, noras, feature_282, vm6, jdk]
 * VM Testbase readme:
 * Description :
 *     Test tries to load 4 jvmti agents to the VM after the VM has started using
 *     Attach API (com.sun.tools.attach).
 *     Test is based on the nsk.share.aod framework. In the terms of this framework
 *     java application running in the VM where agent is loaded to is called 'target application'.
 *     This is stress test, 4 jvmti agents are loaded to the VM. Agents execute following actions:
 *         - agent045Agent00: enables ClassLoad event and from event handler calls GetClassSignature
 *         for loaded class
 *         - agent045Agent01: enables ClassPrepare event and from event handler calls GetClassSignature
 *         for loaded class and also class GetLoadedClasses
 *         - agent045Agent02: enables ThreadStart and ThreadEnd events, from event handlers calls GetThreadInfo
 *         for started or ended thread, and also calls GetAllThreads
 *         - agent045Agent03: enables ThreadStart and ThreadEnd events, from event handlers calls GetThreadInfo
 *         for started or ended thread, and also calls GetAllThreads
 *         - agent045Agent04: enables VMObjectAlloc event, from event handler calls GetThreadInfo for thread
 *         allocating object and calls GetClassSignature for allocated object class. Also after after each 10th event
 *         agent calls ForceGarbageCollection
 *     When each agent receives expected number of events agent finishes work (all events received by agents are
 *     provoked by the target application).
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.share.aod.AODTestRunner
 *        nsk.jvmti.AttachOnDemand.attach045.attach045Target
 *        nsk.jvmti.AttachOnDemand.attach045.ClassToLoad
 * @run main/othervm/native
 *      nsk.share.aod.AODTestRunner
 *      -jdk ${test.jdk}
 *      -target "nsk.jvmti.AttachOnDemand.attach045.attach045Target -classPath ${test.class.path}"
 *      -javaOpts="-XX:+UsePerfData ${test.vm.opts} ${test.java.opts}"
 *      -na attach045Agent00,attach045Agent01,attach045Agent02,attach045Agent03
 */

