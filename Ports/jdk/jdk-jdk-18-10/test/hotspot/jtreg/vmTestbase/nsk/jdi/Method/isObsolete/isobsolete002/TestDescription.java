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
 * @summary converted from VM Testbase nsk/jdi/Method/isObsolete/isobsolete002.
 * VM Testbase keywords: [jpda, jdi, redefine]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     Method.
 *     The test checks up that a result of the method
 *     com.sun.jdi.Method.isObsolete()
 *     complies with its spec:
 *     public boolean isObsolete()
 *      Determine if this method is obsolete.
 *      Returns: true if this method has been replaced by
 *               a non-equivalent method using
 *               VirtualMachine.redefineClasses(java.util.Map).
 *     The test checks that isObsolete() returns true if
 *     the body of the tested method has been fully replaced.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.Method.isObsolete.isobsolete002;
 *     the debuggee program - nsk.jdi.Method.isObsolete.isobsolete002a.
 *     Using nsk.jdi.share classes,
 *     the debugger gets the debuggee running on another JavaVM,
 *     creates the object debuggee.VM, and waits for VMStartEvent.
 *     Upon getting the debuggee VM started,
 *     the debugger calls corresponding debuggee.VM methods to get
 *     needed data and to perform checks.
 *     In case of error the test produces the return value 97 and
 *     a corresponding error message(s).
 *     Otherwise, the test is passed and produces
 *     the return value 95 and no message.
 * COMMENTS:
 *     The test suite contains
 *     the precompiled class file isobsolete002b.klass .
 *     Its source file is isobsolete002b.ja .
 *     The test was modified die to fix of the bug:
 *     4514956 Method.isObsolete() returns false for redefined method
 *     Test was updated according to rfe:
 *     4691123 TEST: some jdi tests contain precompiled .klass files undes SCCS.
 *     isobsolete002b.ja was moved into newclass directory and renamed
 *     to isobsolete002b.java.
 *     The precompiled class file is created during test base build process.
 *     Test fixed according to test bug:
 *     4798088 TEST_BUG: setBreakpoint() method depends of the locations implementation
 *     - using standard Debugee.makeBreakpoint() method
 *     - adjusting source line numbers used for setting breakpoint
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.Method.isObsolete.isobsolete002
 *        nsk.jdi.Method.isObsolete.isobsolete002a
 *
 * @comment compile newclassXX to bin/newclassXX
 *          with full debug info
 * @run driver nsk.share.ExtraClassesBuilder
 *      -g:lines,source,vars
 *      newclass
 *
 * @run main/othervm
 *      nsk.jdi.Method.isObsolete.isobsolete002
 *      ./bin
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

