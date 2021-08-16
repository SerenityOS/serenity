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
 * @summary converted from VM Testbase nsk/jdi/TypeComponent/declaringType/decltype008.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks the declaringType() method of TypeComponent interface of
 *     com.sun.jdi package.
 *     The method spec:
 *     public ReferenceType declaringType()
 *     Returns the type in which this component was declared. The returned
 *     ReferenceType mirrors either a class or an interface in the target VM.
 *     nsk/jdi/TypeComponent/declaringType/declType008 checks assertion:
 *     public java.lang.String declaringType()
 *     1. Returns the type in which a method was declared if the type mirrors
 *        a class in the target VM.
 *     There are classes MainClass and OtherClass that extends MainClass in
 *     target VM. Both classes have constructors and static initializers.
 *     Debugger gets all visible methods from debuggee (OtherClass), finds
 *     constructors and static initializers and checks their declaring types. For
 *     all initializers declaring type must be OtherClass. Each constructor
 *     that signature begins with reference to Long object must have declaring
 *     type MainClass. Each constructor that signature begins with reference to
 *     String object must have declaring type OtherClass.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.TypeComponent.declaringType.decltype008
 *        nsk.jdi.TypeComponent.declaringType.decltype008a
 * @run main/othervm
 *      nsk.jdi.TypeComponent.declaringType.decltype008
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

