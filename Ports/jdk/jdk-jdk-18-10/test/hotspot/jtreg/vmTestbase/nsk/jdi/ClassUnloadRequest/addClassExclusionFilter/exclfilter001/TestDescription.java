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
 * @summary converted from VM Testbase nsk/jdi/ClassUnloadRequest/addClassExclusionFilter/exclfilter001.
 * VM Testbase keywords: [diehard, jpda, jdi, nonconcurrent]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test checks up the method
 *     com.sun.jdi.ClassUnloadRequest.addClassExclusionFilter(String)
 *     on the following assertion:
 *          Restricts the events generated by this request to the unloading
 *          of reference types whose name does not match a restricted regular
 *          expression.
 *     The cases to test include a pattern that begins with '*' and ends with '*'.
 *     The test works as follows:
 *     The debugger program -
 *         nsk.jdi.ClassUnloadRequest.addClassExclusionFilter.exclfilter001;
 *     the debuggee program -
 *         nsk.jdi.ClassUnloadRequest.addClassExclusionFilter.exclfilter001a.
 *     Using nsk.jdi.share classes, the debugger gets the debuggee running on
 *     another JavaVM and controls the classes loading/unloading on debugee.
 *     Each event is checked class name, which generates this event, to do not
 *     match to defined filter.
 *     When class name matches to defined filter, the test fails and produces
 *     a corresponding error message(s).
 *     Note: In case no events occured, test is considered as passed.
 * COMMENTS:
 *     Fixed due to the bug 4677256
 *     The test was modified to comply with new execution scheme
 *     to have separate directory for precompiled classes:
 *       - all classes to be loaded by ClassUnloader were separated and
 *         moved into 'loadclass' subdirectory;
 *       - ${COMMON_CLASSES_LOCATION} instead of ${TESTDIR} in .cfg file;
 *       - ClassUnloader seekes for classes to load class in
 *         ${COMMON_CLASSES_LOCATION}/loadclass directory.
 *     Test updated to prevent possible VMDisconnectedException on VMDeathEvent:
 *     - throwing Failure exception on VMDisconnect is used in event handling loop
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ClassUnloadRequest.addClassExclusionFilter.exclfilter001
 *        nsk.jdi.ClassUnloadRequest.addClassExclusionFilter.exclfilter001a
 *
 * @comment compile loadclassXX to bin/loadclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      loadclass
 *
 * @run main/othervm/timeout=420
 *      nsk.jdi.ClassUnloadRequest.addClassExclusionFilter.exclfilter001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}" ./bin
 */

