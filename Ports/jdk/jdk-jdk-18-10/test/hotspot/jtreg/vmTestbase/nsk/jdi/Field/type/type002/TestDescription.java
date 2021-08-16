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
 * @summary converted from VM Testbase nsk/jdi/Field/type/type002.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks the type() method of Field interface of
 *     com.sun.jdi package.
 *     nsk/jdi/Field/type002 checks assertion:
 *     public Type type() throws ClassNotLoadedException
 *     1. For object fields, the appropriate ReferenceType is returned if it
 *     has been loaded through the enclosing type's class loader.
 *     Constructor for each class and interface are triggered, so all
 *     classes are loaded in debugee and ClassNotLoadedException are not
 *     thrown. Each field of reference type of debugee should be casted
 *     to apropriate ReferenceType (ArrayType, ClassType, InterfaceType)
 *     in debuger without exceptions. Also, each component type name should
 *     exist in signature of the Type.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.Field.type.type002
 *        nsk.jdi.Field.type.type002a
 * @run main/othervm
 *      nsk.jdi.Field.type.type002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

