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
 * @summary converted from VM Testbase nsk/jdi/ArrayReference/length/length001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks the length() method of ArrayReference interface of
 *     com.sun.jdi package.
 *     The method spec:
 *     public int length()
 *     Returns the number of components in this array.
 *     Returns: the integer count of components in this array.
 *     Throws: ObjectCollectedException - if this object has been garbage
 *             collected.
 *     nsk/jdi/ArrayReference/length/length001 checks assertion:
 *     public int length()
 *     1. Returns the number of components in this array.
 *     Debuggee defines a number of array fields with component types:
 *         - primitive types,
 *         - interface type,
 *         - classes,
 *         - array of primitive types,
 *         - array of component types,
 *         - array of classes.
 *     Debugger gets each field from debuggee by name, gets field's value and
 *     casts it to ArrayReference. Then the test gets length of this object
 *     invoking the method length(). Debugger already knows the length of the
 *     array (FIELD_NAME defines it) and compares returned and expected lengths
 *     of the array.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ArrayReference.length.length001
 *        nsk.jdi.ArrayReference.length.length001a
 * @run main/othervm
 *      nsk.jdi.ArrayReference.length.length001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

