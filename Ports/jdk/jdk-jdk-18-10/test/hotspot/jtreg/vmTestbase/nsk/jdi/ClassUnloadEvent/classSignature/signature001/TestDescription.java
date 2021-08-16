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
 * @summary converted from VM Testbase nsk/jdi/ClassUnloadEvent/classSignature/signature001.
 * VM Testbase keywords: [jpda, jdi, nonconcurrent]
 * VM Testbase readme:
 * DESCRIPTION
 *   The test exercises
 *     com.sun.jdi.event.ClassUnloadEvent.classSignature() method.
 *   The test checks the following assertions:
 *     - ClassUnloadEvent is always received by debugger
 *        for unloaded class, interface and array type in target VM,
 *     - the string returned by classSignature() is not empty and comply
 *        to JNI-style signature.
 *    A debugger class - nsk.jdi.ClassUnloadEvent.classSignature.signature001  ;
 *    a debuggee class - nsk.jdi.ClassUnloadEvent.classSignature.signature001a .
 *    The test uses supporting nsk/jdi/share classes for launching debuggee
 *    and for creating communication pipe between debugger and debuggee. The
 *    debugger and debugee communicates with special commands.
 *    The debugger forces debugge to load checked classes, creates and
 *    enables ClassUnloadRequest. Next, debugger forces debuggee to
 *    unload classes, using memory stressing techique, and waits for
 *    ClassUnloadEvent.
 *    If each expected ClassUnloadEvent occurs, debugger tests method
 *    ClassUnloadEvent.classSignature() and verifies that this event
 *    is for checked class or interface.
 *    If no expected ClassUnloadEvent received for WAITTIME period
 *    but class not found in debuggee, or ClassUnloadEvent received
 *    but class still in class list, debugger reports an error.
 *    Test pass even if class leave loaded in debuggee but no
 *    ClassUnloadEvent received,
 *    Finally, debugger disables event request to prevent further
 *    appearing of events, sends debuggee command to quit, clears
 *    event queue, wait for debugge terminated, and exits.
 *    The test fails if any of the checks failed.
 * PARAMETERS
 *    1: String  - current test directory.
 * COMMENTS
 *        Test fixed due to bugs:
 *        4455653 VMDisconnectedException on resume
 *        4463674: TEST_BUG: some JDI tests are timing dependent
 *        NOTE: testcase for array class is excluded from the test (commented out)
 *              because of difficulty to provide reliable techique to unload
 *              such a class.
 *        Test fixed due to bug:
 *        4419078 HS1.4: class isn't unloaded when its classloader is being finalized
 *        Test fixed due to bug:
 *        4642444 TEST_BUG: tests against ClassUnloadEvent fail due to internal ping timeout
 *        The test was modified to comply with new execution scheme
 *        to have separate directory for precompiled classes:
 *          - signature003b and signature003c classes were moved in
 *            'loadclass' subdirectory;
 *          - ${COMMON_CLASSES_LOCATION} instead of ${TESTDIR} in .cfg file;
 *          - ClassUnloader seekes for classes to load in
 *            ${COMMON_CLASSES_LOCATION}/loadclass directory.
 *         Standard method Debugee.endDebugee() is used instead of cleaning
 *         event queue on debuggee VM exit.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ClassUnloadEvent.classSignature.signature001
 *        nsk.jdi.ClassUnloadEvent.classSignature.signature001a
 *
 * @comment compile loadclassXX to bin/loadclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      loadclass
 *
 * @run main/othervm
 *      nsk.jdi.ClassUnloadEvent.classSignature.signature001
 *      ./bin
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

