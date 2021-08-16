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
 * @summary converted from VM Testbase nsk/jdi/ThreadReference/ownedMonitorsAndFrames/ownedMonitorsAndFrames008.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *         This test do the same things as 'ownedMonitorsAndFrames003', but run in debuggee VM several test threads.
 *         Default test threads count is 10, but number of test threads can be changed through test parameter '-testThreadsCount'
 *         (for example "-testThreadsCount 100").
 *         This test explicitly set following debuggee VM keys: -Xmixed -XX:CompileThreshold=2 to check that tested
 *         functionality was not broken after JIT compilation.
 *         Debuggee VM creates a number of threads and each test thread acquires 4 different monitors in following ways:
 *                 - entering synchronized method
 *                 - entering synchronized block on non-static object
 *                 - entering synchronized method for thread object itself
 *                 - entering synchronized block on static object
 *         Information about all monitors acquired by test thread is stored in debuggee VM and can be obtained through
 *         special field in debuggee class: OwnedMonitorsDebuggee.monitorsInfo.
 *         Debugger VM reads information about acquired monitors from 'OwnedMonitorsDebuggee.monitorsInfo'
 *         and checks that com.sun.jdi.ThreadReference.ownedMonitorsAndFrames returns correct list of MonitorInfo objects, this
 *         check is performed for all test threads.
 *         Debugger VM forces all test threads in debuggee VM sequentially free monitors through Object.wait(),
 *         updates debug information about acquired monitors and checks that com.sun.jdi.ThreadReference.ownedMonitorsAndFrames
 *         returns correct list of MonitorInfo objects for all test threads.
 *         Debugger VM forces all test threads in debuggee VM free all monitors(exit from all synchronized objects/blocks),
 *         updates debug information about acquired monitors and checks that com.sun.jdi.ThreadReference.ownedMonitorsAndFrames
 *         returns correct list of MonitorInfo objects for all test threads.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames007.ownedMonitorsAndFrames007
 * @run main/othervm/native
 *      nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames007.ownedMonitorsAndFrames007
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="-Xmixed
 *      -XX:CompileThreshold=2"
 */

