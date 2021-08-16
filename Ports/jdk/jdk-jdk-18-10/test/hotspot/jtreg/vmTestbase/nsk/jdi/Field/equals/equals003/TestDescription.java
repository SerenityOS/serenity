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
 * @summary converted from VM Testbase nsk/jdi/Field/equals/equals003.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks the equals() method of Field interface of
 *     com.sun.jdi package.
 *     nsk/jdi/Field/equals003 checks assertion:
 *     public boolean equals(java.lang.Object obj)
 *     1. Returns false if the Object and Field are in different classes
 *     but mirror the same field.
 *     There are MainClass, two classes (SameClass1, SameClass2) that extends
 *     it and do not override fields from super class, OverridenClass that
 *     extends MainClass and overrides all fields. Three comparisons are made:
 *     - Two fields from MainClass and SameClass1, that mirror the same field
 *       are compared. Expected result - true.
 *     - Two fields from MainClass and OverridenClass, that mirror the same field
 *       are compared. Expected result - false.
 *     - Two fields from SameClass1 and SameClass2, that mirror the same field
 *       are compared. Expected result - true.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.Field.equals.equals003
 *        nsk.jdi.Field.equals.equals003a
 * @run main/othervm
 *      nsk.jdi.Field.equals.equals003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

