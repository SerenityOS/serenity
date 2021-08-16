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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/hotswap/HS201/hs201t001.
 * VM Testbase keywords: [jpda, jvmti, onload_only_caps, noras, redefine, nonconcurrent]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test is for HS201 scenario of "HotSwap" class file replacement
 *     area.
 *     The test exercises the RedefineClasses, IsMethodObsolete and PopFrame
 *     jvmti's functions when an user-defined exception is being thrown
 *     for a case when original and redefined methods are NOT equivalent.
 *     Checked statements:
 *         - redefined method having active stack frames continues to run
 *           with original bytecodes, in this case IsMethodObsolete returns true
 *         - after popping a frame, the execution state is reset to the state
 *           immediately before the called method was invoked, when the thread is
 *           resumed
 *     The test does the following steps:
 *         1) enables events ClassLoad, Breakpoint, Exception, ExceptionCatch;
 *         2) starts new thread which invokes a method throwing a not-yet-loaded
 *            user-defined exception;
 *         3) upon ClassLoad occurrence for the exception, sets a breakpoint
 *            in its constructor.
 *         4) upon reaching the breakpoint, enables SingleStep.
 *         5) redefines the exception within SingleStep callback, new bytecode
 *            contains the changed constructor;
 *         6) checks that stepping is continued in the obsolete constructor;
 *         7) suspends started thread and pops a currently executing frame;
 *         8) after resuming checks that stepping is continued in the redefined
 *            constructor;
 *         9) redefines the exception once more within triggered callbacks
 *            Exception, ExceptionCatch caused by throwing in the method.
 * COMMENTS
 *     Test fixed according to test bug:
 *     5068937 TEST_BUG: JVMTI HotSwap tests hs201t00* use wrong JNI environment
 *     Fixed 6281402: TEST_BUG: race condition in test nsk/jvmti/scenarios/hotswap/HS201/hs201t001
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.hotswap.HS201.hs201t001
 *
 * @comment compile newclassXX to bin/newclassXX
 *          with full debug info
 * @run driver nsk.share.ExtraClassesBuilder
 *      -g:lines,source,vars
 *      newclass02 newclass03 newclass newclass01
 *
 * @run main/othervm/native
 *      -agentlib:hs201t001=pathToNewByteCode=./bin,-waittime=5
 *      nsk.jvmti.scenarios.hotswap.HS201.hs201t001
 */

