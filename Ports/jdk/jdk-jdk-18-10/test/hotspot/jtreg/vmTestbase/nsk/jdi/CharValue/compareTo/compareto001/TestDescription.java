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
 * @summary converted from VM Testbase nsk/jdi/CharValue/compareTo/compareto001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *   The test for the compareTo(Object o) method of com.sun.jdi.CharValue
 *   interface. This method is inherited from java.lang.Comparable interface.
 *   The test checks the following assertions which imply from spec for
 *   Comparable.compareTo(Object o):
 *    - (x.compareTo(y) == 0) is identical to (x.equals(y) == true);
 *    - (x.compareTo(y) == 0) is identical to (y.compareTo(x) == 0);
 *    - if (x.compareTo(y) == i) then (y.compareTo(x) == -i);
 *    - if (x.compareTo(y) > 0) and (y.compareTo(z) > 0), then (x.compareTo(z) > 0);
 *    - if an argument is null, then NullPointerException is thrown;
 *    - if an argument is not of CharValue type, then a ClassCastException is thrown.
 *   where 'x', 'y' and 'z' denote CharValue object.
 *   The debugger program - nsk.jdi.CharValue.compareto.compareto001;
 *   the debuggee program - nsk.jdi.CharValue.compareto.compareto001a;
 *   The test works as follows:
 *   Using nsk.jdi.share classes, the debugger connects to the debuggee.
 *   Then the debugger gets a list of char fields of debuggee's object of
 *   compareto001aClassToCheck type. For every field a mirror of CharValue type
 *   is created and the assertions are checked. A various values for comparison
 *   are got from values of mirrors of debuggee's static fields.
 *   In case of error the test produces the return value 97 and a corresponding
 *   error message(s). Otherwise, the test is passed and produces the return
 *   value 95 and no message.
 * COMMENTS:
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.CharValue.compareTo.compareto001
 *        nsk.jdi.CharValue.compareTo.compareto001a
 * @run main/othervm
 *      nsk.jdi.CharValue.compareTo.compareto001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

