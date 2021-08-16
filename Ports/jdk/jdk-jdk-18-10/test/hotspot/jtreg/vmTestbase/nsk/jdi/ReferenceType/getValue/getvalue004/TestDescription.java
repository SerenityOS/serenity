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
 * @summary converted from VM Testbase nsk/jdi/ReferenceType/getValue/getvalue004.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks that the JDI method
 *        com.sun.jdi.ReferenceType.getValue()
 *     properly throws IllegalArgumentException - if the field is
 *     not valid for this object's class.
 *     Debuggee part of the test consists of the following classes:
 *     "getvalue004t", "getvalue004tDummySuperCls", and "getvalue004tDummyCls"
 *     which extends getvalue004tDummySuperCls.
 *     The exception is provoked to be thrown by getting a Value of:
 *         - field obtained from the getvalue004tDummyCls but applied to
 *           reference type of the getvalue004t;
 *         - private field obtained from the getvalue004tDummySuperCls
 *           but applied to reference type of the getvalue004t.
 * COMMENTS
 *     4772094 JDI: ReferenceType.getValue() allows to get value of a private field
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ReferenceType.getValue.getvalue004
 *        nsk.jdi.ReferenceType.getValue.getvalue004t
 * @run main/othervm
 *      nsk.jdi.ReferenceType.getValue.getvalue004
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

