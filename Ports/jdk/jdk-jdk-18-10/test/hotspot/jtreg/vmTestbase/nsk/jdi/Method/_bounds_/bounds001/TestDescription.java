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
 * @summary converted from VM Testbase nsk/jdi/Method/_bounds_/bounds001.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test checks up the following methods of com.sun.jdi.Method:
 *     - locationsOfLine(int) for boundary values of Integer
 *     - locationsOfLine(String, String, int) for boundary values of
 *     Integer and various combinations of value of String arguments
 *     such as null, "", <bad_string>, where <bad_string> means the some
 *     names of nonexisting object.
 *     - variablesByName(String) for various combinations of value of
 *     String arguments (see the notes above)
 *     - locationOfCodeIndex(long) for boundary values of Long
 *     - equals(Object) for null-value
 *     - bytecodes() is checked on length of return array
 *     These checking are performed for native and non-native methods.
 * COMMENTS:
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.Method._bounds_.bounds001
 *        nsk.jdi.Method._bounds_.bounds001a
 *
 * @comment make sure bounds001a is compiled with full debug info
 * @clean nsk.jdi.Method._bounds_.bounds001a
 * @compile -g:lines,source,vars ../bounds001a.java
 *
 * @run main/othervm
 *      nsk.jdi.Method._bounds_.bounds001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

