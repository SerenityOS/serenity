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
 * @summary converted from VM Testbase nsk/jdi/ReferenceType/isInitialized/isinit001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *         nsk/jdi/ReferenceType/isInitialized/isinit001 test
 *         checks the isInitialized() method of ReferenceType interface
 *         of the com.sun.jdi package for ClassType, InterfaceType:
 *         For each checked debugee's class the test gets ReferenceType
 *         instance for this class and then calls the isInitialized()
 *         method for this ReferenceType instance.
 *         Some checked debugee's classes have been initialized but other have not.
 *         The test expects that returned boolean value should be equal to the
 *         expected boolean value.
 * COMMENTS
 * The test is corrected and updated as follows:
 * - the checks for NotInitializedClass and NotInitializedInterface are removed
 *   because the logic of other checks doesn't allow to get
 *   corresponding objects in the debuggee;
 * - statements for establishing LogHandler and Binder are updated
 *   to comply with new version of the shared classes;
 * - a number of statements "print_log_anyway" are replaced with "print_log_on_verbose".
 * -------------------------------------------------------
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ReferenceType.isInitialized.isinit001
 *        nsk.jdi.ReferenceType.isInitialized.isinit001a
 * @run main/othervm
 *      nsk.jdi.ReferenceType.isInitialized.isinit001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

