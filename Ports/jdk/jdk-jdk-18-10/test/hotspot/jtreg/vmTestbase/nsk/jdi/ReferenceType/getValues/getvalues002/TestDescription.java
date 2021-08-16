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
 * @summary converted from VM Testbase nsk/jdi/ReferenceType/getValues/getvalues002.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks that the JDI method
 *        com.sun.jdi.ReferenceType.getValues()
 *     properly throws IllegalArgumentException - if any field is
 *     not valid for this object's class.
 *     Debuggee part of the test consists of the following classes:
 *     "getvalues002t", "getvalues002tDummySuperCls", and "getvalues002tDummyCls"
 *     which extends getvalues002tDummySuperCls.
 *     The exception is provoked to be thrown by getting a Map of values
 *     which correspond to a list containing some wrong fields such as:
 *       - fields obtained from the getvalues002tDummyCls but applied to
 *         reference type of the getvalues002t;
 *       - some non-static fields from the getvalues002tDummyCls applied to
 *         its reference type.
 * COMMENTS
 *     4772094 JDI: ReferenceType.getValue() allows to get value of a private field
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ReferenceType.getValues.getvalues002
 *        nsk.jdi.ReferenceType.getValues.getvalues002t
 * @run main/othervm
 *      nsk.jdi.ReferenceType.getValues.getvalues002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

