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
 * @key randomness
 *
 * @summary converted from VM Testbase nsk/jvmti/AttachOnDemand/attach020.
 * VM Testbase keywords: [jpda, jvmti, noras, feature_282, vm6, jdk, nonconcurrent]
 * VM Testbase readme:
 * Description :
 *     Test tries to load jvmti agent to the VM after the VM has started using
 *     Attach API (com.sun.tools.attach).
 *     Test is based on the nsk.share.aod framework. In the terms of this framework
 *     java application running in the VM where agent is loaded to is called 'target application'.
 *     Test scenario:
 *         - during initialization (from function Agent_OnAttach) agent starts auxiliary thread waiting on
 *         raw monitor and enables GarbageCollectionStart and GarbageCollectionFinish events
 *         - target application provokes garbage collection (calls System.gc())
 *         - agent receives event GarbageCollectionStart
 *         - agent receives event GarbageCollectionFinish event and notifies waiting auxiliary thread
 *         - notified auxiliary thread notifies target application that agent finished its work
 *         (auxiliary thread is needed because of to notify target application about agent finishing JNI
 *         functions are used, and these functions can't be called from GarbageCollectionFinish event handler)
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.share.aod.AODTestRunner
 *        nsk.jvmti.AttachOnDemand.attach020.attach020Target
 * @run main/othervm/native
 *      nsk.share.aod.AODTestRunner
 *      -jdk ${test.jdk}
 *      -target nsk.jvmti.AttachOnDemand.attach020.attach020Target
 *      -javaOpts="-XX:+UsePerfData ${test.vm.opts} ${test.java.opts}"
 *      -na attach020Agent00
 */

