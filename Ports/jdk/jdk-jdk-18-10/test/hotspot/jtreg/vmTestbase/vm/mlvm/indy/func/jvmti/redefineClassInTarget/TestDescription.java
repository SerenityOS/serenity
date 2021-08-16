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
 * @summary converted from VM Testbase vm/mlvm/indy/func/jvmti/redefineClassInTarget.
 * VM Testbase keywords: [feature_mlvm, jvmti, redefine, jdk, noJFR]
 * VM Testbase readme:
 * DESCRIPTION
 *     Test calls a boostrap and a target methods via InvokeDynamic call, monitoring that
 *     a method in the debuggee class (Dummy0.redefineNow()) is called (monitoring is done
 *     via MethodEntry event). At this moment, Dummy0 class is redefined using RedefineClasses
 *     function to Dummy1 class (in its classfile every occurence of "Dummy1" is replaced with
 *     "Dummy0") and PopFrame function is called to reenter the method.
 *     The test verifies that when class is redefined in a target method (at that moment,
 *     the call site is linked) and frame is popped, the new target method is executed and
 *     the site is relinked.
 *     CR 6929027
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment build dummy class
 * @build vm.mlvm.indy.func.jvmti.redefineClassInTarget.INDIFY_Dummy0
 *
 * @comment compile newclass to bin/newclass
 * @run driver nsk.share.ExtraClassesBuilder newclass
 * @run driver vm.mlvm.share.IndifiedClassesBuilder bin/newclass
 *
 *
 * @comment build test class and indify classes
 * @build vm.mlvm.indy.func.jvmti.share.IndyRedefineTest
 * @run driver vm.mlvm.share.IndifiedClassesBuilder
 *
 * @run main/othervm/native
 *      -agentlib:IndyRedefineClass=verbose=~pathToNewByteCode=./bin/newclass
 *      vm.mlvm.indy.func.jvmti.share.IndyRedefineTest
 *      -dummyClassName=vm.mlvm.indy.func.jvmti.redefineClassInTarget.INDIFY_Dummy0
 */

