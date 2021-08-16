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
 * @summary converted from VM Testbase nsk/jdi/ClassLoaderReference/visibleClasses/visibleclasses002.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 * COMMENTS:
 *    The test checks up that an implementation of the
 *    com.sun.jdi.ClassLoaderReference.visibleClasses method conforms
 *    with its spec.
 *    The test verifies an assertion:
 *       public List visibleClasses()
 *       Returns a list of all classes for which this class loader has been
 *       recorded as the initiating loader in the target VM. The list contains
 *       ReferenceTypes defined directly by this loader (as returned by
 *       definedClasses()) and any types for which loading was delegated by this
 *       class loader to another class loader.
 *    The test consists of:
 *      debugger - nsk.jdi.ClassLoaderReference.visibleClasses.visibleclasses002
 *      debuggee - nsk.jdi.ClassLoaderReference.visibleClasses.visibleclasses002a
 *    The main method of debuggee class defines local variables of single- and
 *    two-dimensional array of 'visibleclasses002a' type. The debugger gets
 *    a ClassLoaderReference of debuggee class loader. The tests fails if
 *    either debuggee class reference or array type references are not found
 *    in the list returned by visibleClasses() method.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ClassLoaderReference.visibleClasses.visibleclasses002
 *        nsk.jdi.ClassLoaderReference.visibleClasses.visibleclasses002a
 * @run main/othervm
 *      nsk.jdi.ClassLoaderReference.visibleClasses.visibleclasses002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

