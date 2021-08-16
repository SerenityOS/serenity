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
 * @summary converted from VM Testbase nsk/jdi/ReferenceType/allMethods/allmethods001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *         nsk/jdi/ReferenceType/allMethods/allmethods001 test
 *         checks the allMethods() method of ReferenceType interface
 *         of the com.sun.jdi package :
 *        the ReferenceType.allMethods() method is checked for methods of
 *        debugee's class which extends super class and implements interface.
 *        Debugee's class has both hidden and overridden methods which should
 *        present in Method list returned by the allMethods() method..
 *        Each method is checked for method name and declaring class name.
 * COMMENTS
 *         Fixed test due to bug:
 *         Incorrect initialization of Binder object with argv instead of argHandler.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ReferenceType.allMethods.allmethods001
 *        nsk.jdi.ReferenceType.allMethods.allmethods001a
 * @run main/othervm
 *      nsk.jdi.ReferenceType.allMethods.allmethods001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

