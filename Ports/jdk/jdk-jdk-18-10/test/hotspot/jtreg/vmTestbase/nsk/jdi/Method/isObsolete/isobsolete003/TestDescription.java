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
 * @summary converted from VM Testbase nsk/jdi/Method/isObsolete/isobsolete003.
 * VM Testbase keywords: [quick, jpda, jdi, redefine]
 * VM Testbase readme:
 * DESCRIPTION:
 *   The test for the implementation of an object of the type
 *   Method.
 *   The test checks the following assertions:
 *     if Method object is obsolete, then
 *       - all method of Method class which are specified to return the List,
 *         must return zero size list or throw AbsentInformationException,
 *       - bytecodes() method must return empty array,
 *       - location() method can not return Location equal to one
 *         before redefinition,
 *       - locationOfCodeIndex(long codeIndex) method must return null,
 *       - returnTypeName() method can not return String equal to one
 *         before redefinition,
 *       - returnType() method must return null or throw ClassNotLoadedException,
 *       - hashCode() method can not return int value equal to one before
 *         redefinition.
 *   The debugger program - nsk.jdi.Method.isObsolete.isobsolete003;
 *   the debuggee program - nsk.jdi.Method.isObsolete.isobsolete003a;
 *   the class to be redefined - nsk.jdi.Method.isObsolete.isobsolete003b.
 *   The test contains the precompiled class file isobsolete003b.klass   .
 *   Its source file is isobsolete003b.java.rdf and presents class which
 *   redefines isobsolete003b class.
 *   The test works as follows:
 *   Using nsk.jdi.share classes, the debugger connects to the debuggee.
 *   Upon getting the debuggee VM started, the debugger creates the special
 *   requests for event when isobsolete003a class is prepared and for event
 *   when isobsolete003b is load into debuggee VM. This actions are needed
 *   to take return values on some methods for further comparisons.
 *   Then debugger creates breakpoint request at line of 'isobsolete003b.foo()'
 *   method and resumes debuggee. After receiving of requested breakpoint event,
 *   the debuggee's main thread is suspended and 'foo()' method is at its top
 *   frame.. At this moment, the debugger redefines isobsolete003b class.
 *   The isobsolete003b.klass which redefines isobsolete003b class, contains
 *   non-equivalent version of 'foo()' method. Thus, after redefinition,
 *   new mirror of 'foo()' method is obsolete. After this, the debugger performs
 *   the checks of assertions listed above.
 *   In case of error the test produces the return value 97 and a corresponding
 *   error message(s). Otherwise, the test is passed and produces the return
 *   value 95 and no message.
 * COMMENTS:
 *   The test is aimed to increase jdi source code coverage and checks
 *   the method which were not yet covered by previous tests for isObsolete()
 *   method. The coverage analysis was done for jdk1.4.0-b92 build.
 *     Test was updated according to rfe:
 *     4691123 TEST: some jdi tests contain precompiled .klass files undes SCCS.
 *     isobsolete003b.java.rdf was moved into newclass directory and renamed
 *     to isobsolete003b.java.
 *     The precompiled class file is created during test base build process.
 *     Test was fixed according to test bug:
 *     4778296 TEST_BUG: debuggee VM intemittently hangs after resuming
 *     - handling VMStartEvent was removed from the debugger part of the test
 *     Test fixed according to test bug:
 *     4798088 TEST_BUG: setBreakpoint() method depends of the locations implementation
 *     - using standard Debugee.makeBreakpoint() method for setting breakpoint
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.Method.isObsolete.isobsolete003
 *        nsk.jdi.Method.isObsolete.isobsolete003a
 *
 * @comment compile newclassXX to bin/newclassXX
 *          with full debug info
 * @run driver nsk.share.ExtraClassesBuilder
 *      -g:lines,source,vars
 *      newclass
 *
 * @run main/othervm
 *      nsk.jdi.Method.isObsolete.isobsolete003
 *      ./bin
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

