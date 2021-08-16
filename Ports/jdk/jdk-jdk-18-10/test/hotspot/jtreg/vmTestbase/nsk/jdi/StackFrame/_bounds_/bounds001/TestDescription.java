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
 * @summary converted from VM Testbase nsk/jdi/StackFrame/_bounds_/bounds001.
 * VM Testbase keywords: [jpda, jdi, quarantine]
 * VM Testbase comments: 6604963
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test checks up the methods:
 *         com.sun.jdi.StackFrame.setValue(Field, Object)
 *         com.sun.jdi.StackFrame.getValue(Field)
 *     for boundry values of primitive types
 *     Test checks up the following assertion:
 *          Primitive arguments must be either assignment compatible with
 *          the field type or must be convertible to the field type
 *          without loss of information.
 *     for every primitive type.
 * COMMENTS:
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.StackFrame._bounds_.bounds001
 *        nsk.jdi.StackFrame._bounds_.bounds001a
 *
 * @comment make sure bounds001a is compiled with full debug info
 * @clean nsk.jdi.StackFrame._bounds_.bounds001a
 * @compile -g:lines,source,vars ../bounds001a.java
 *
 * @run main/othervm
 *      nsk.jdi.StackFrame._bounds_.bounds001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

