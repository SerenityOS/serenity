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
 * @summary converted from VM Testbase nsk/jdi/Method/argumentTypeNames/argumenttypenames003.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *  A test for argumentTypeNames() method of Method interface.
 *  The test checks if the method returns string representing array
 *  of the type before the ellipsis for any mirrored method which has
 *  varargs argument.
 *  The test consists of a debugger program (argumenttypenames003.java)
 *  and debuggee application (argumenttypenames003a.java).
 *  Package name is nsk.jdi.Method.argumentTypeNames .
 *  The test works as follows.
 *  The debugger uses nsk.jdi.share framework classes to
 *  establish connection with debuggee. The debugger and debuggee
 *  synchronize with each other using special commands over
 *  communication channel provided by framework classes.
 *  Upon receiving the signal of readiness from debuggee,
 *  the debugger finds mirrors of debuggee's methods 'foo'
 *  which have varargs arguments and calls argumentTypeNames() method
 *  for each found method mirror. The test checks if returned
 *  list contains a string representing expected array type
 *  of vararg argument.
 * COMMENTS:
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.Method.argumentTypeNames.argumenttypenames003
 *        nsk.jdi.Method.argumentTypeNames.argumenttypenames003a
 *
 * @comment make sure argumenttypenames003a is compiled with full debug info
 * @clean nsk.jdi.Method.argumentTypeNames.argumenttypenames003a
 * @compile -g:lines,source,vars ../argumenttypenames003a.java
 *
 * @run main/othervm
 *      nsk.jdi.Method.argumentTypeNames.argumenttypenames003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

