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
 * @summary converted from VM Testbase nsk/jdi/MethodEntryRequest/_bounds_/filters001.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the boundary value of the parameters.
 *     The test checks up that the methods
 *     com.sun.jdi.request.MethodEntryRequest.addThreadFilter(ThreadReference)
 *     com.sun.jdi.request.MethodEntryRequest.addInstanceFilter(ObjectReference)
 *     com.sun.jdi.request.MethodEntryRequest.addClassFilter(ReferenceType)
 *     com.sun.jdi.request.MethodEntryRequest.addClassFilter(String)
 *     com.sun.jdi.request.MethodEntryRequest.addClassExclusionFilter(String)
 *     correctly work for the boundary value of parameter and throw described
 *     exceptions.
 *     The test check up this methods with <null> argument value. In any cases
 *     NullPointerException is expected.
 * COMMENTS:
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.MethodEntryRequest._bounds_.filters001
 *        nsk.jdi.MethodEntryRequest._bounds_.filters001a
 * @run main/othervm
 *      nsk.jdi.MethodEntryRequest._bounds_.filters001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

