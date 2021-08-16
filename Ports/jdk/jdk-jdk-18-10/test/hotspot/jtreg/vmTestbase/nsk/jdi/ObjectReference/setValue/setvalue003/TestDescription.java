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
 * @summary converted from VM Testbase nsk/jdi/ObjectReference/setValue/setvalue003.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks that the JDI method
 *     com.sun.jdi.ObjectReference.setValue() properly throws
 *     InvalidTypeException - if the Field is valid for this
 *     ObjectReference, but there is no a widening reference conversion
 *     from this object to the field's type.
 *     The debuggee part of the test has static and instance fields of
 *     different types. The debugger part provokes the InvalidTypeException
 *     trying to set value of:
 *      - type not matched with the field's one, and without the widening
 *        reference conversion as well;
 *      - boolean type which can be converted only to boolean.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ObjectReference.setValue.setvalue003
 *        nsk.jdi.ObjectReference.setValue.setvalue003t
 *
 * @comment make sure setvalue003t is compiled with full debug info
 * @clean nsk.jdi.ObjectReference.setValue.setvalue003t
 * @compile -g:lines,source,vars ../setvalue003t.java
 *
 * @run main/othervm
 *      nsk.jdi.ObjectReference.setValue.setvalue003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

