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
 * @summary converted from VM Testbase nsk/jdi/ReferenceType/instances/instances004.
 * VM Testbase keywords: [jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *      The test scenario is following:
 *         - Debugger VM
 *         for refererence_type in <Strong, JNI_Local_Ref, JNI_Global_Ref, JNI_Weak_Ref, PhantomReference, SoftReference, WeakReference>
 *                 - initiate creation test class instances  of type 'refererence_type' in debuggee VM
 *                 - prevent some instances from being garbage collected using ObjectReference.disableCollection
 *                 - initiate GarbageCollection in Debuggee VM
 *                 - check the number of instances is left is correct
 *                 - enables Garbage Collection for instances for which it were previously disabled using ObjectReference.enableCollection
 *                 - initiate GarbageCollection in Debuggee VM
 *                 - check the number of instances is 0
 *         done
 *         Test is executed for following sublcass of ObjectReference: ArrayReference
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ReferenceType.instances.instances003.instances003
 *        nsk.share.jdi.TestClass1
 * @run main/othervm/native
 *      nsk.jdi.ReferenceType.instances.instances003.instances003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="-Xmx128M ${test.vm.opts} ${test.java.opts}"
 *      -testClassNames nsk.share.jdi.TestClass1:boolean[]:float[]
 */

