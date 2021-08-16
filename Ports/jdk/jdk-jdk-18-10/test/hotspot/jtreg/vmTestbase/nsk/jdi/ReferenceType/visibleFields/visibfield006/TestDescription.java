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
 * @summary converted from VM Testbase nsk/jdi/ReferenceType/visibleFields/visibfield006.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *  A test for visibleFields() method of ReferenceType interface.
 *  The test checks if the method returns all enum constants
 *  declared in mirrored enum type and does not return any
 *  ambiguous inherited field.
 *  The test consists of a debugger program (visibfield006.java)
 *  and debuggee application (visibfield006a.java).
 *  Package name is nsk.jdi.ReferenceType.visibleFields .
 *  The test works as follows.
 *  The debugger uses nsk.jdi.share framework classes to
 *  establish connection with debuggee. The debugger and debuggee
 *  synchronize with each other using special commands over
 *  communication channel provided by framework classes.
 *  Upon receiving the signal of readiness from debuggee,
 *  the debugger calls visibleFields() method for each field
 *  of enum type declared in visibfield006a class. Every of these
 *  enum types has the same enum constants, i.e fields: e1 and e2,
 *  which must be included in result list. Some of checked enum
 *  types implement visibfield006i1 and visibfield006i2 interfaces.
 *  These interfaces have int field of same name i1. These fields
 *  are ambigous in enum types and should not be returned by
 *  visibleFields() method.
 * COMMENTS:
 *  5029502 TEST_BUG: jdi tests against enum should not use abstract
 *          modifier
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ReferenceType.visibleFields.visibfield006
 *        nsk.jdi.ReferenceType.visibleFields.visibfield006a
 * @run main/othervm
 *      nsk.jdi.ReferenceType.visibleFields.visibfield006
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

