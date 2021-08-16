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
 * @summary converted from VM Testbase nsk/jvmti/RedefineClasses/redefclass030.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, redefine]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test exercises that the JVMTI function RedefineClasses()
 *     enable to redefine a local inner class properly.
 *     Redefiniton is performed in asynchronous manner from a separate
 *     thread when the VM is provoked to switch into compiled mode.
 *     The test works as follows. Two threads are started. One is a java
 *     thread creating and executing a local class in an instance
 *     invoker method of the outer class. Second is a native thread
 *     executing an agent code. Then the local class method
 *     'redefclass030HotMethod()' is provoked to be compiled (optimized),
 *     and thus the JVMTI event 'CompiledMethodLoad' should be sent. After
 *     that, the agent redefines the local class.
 *     Different kinds of outer fields and a local variable of the outer
 *     invoker method are accessed from executing methods in both versions
 *     of the local class. Upon the redefinition, the main test thread verifies
 *     via the outer fields/local variable values that the inner methods
 *     have been redefined. It also verifies that the outer class
 *     is still can access the local class fields after the redefinition.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.RedefineClasses.redefclass030
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass
 *
 * @run main/othervm/native
 *      -agentlib:redefclass030=-waittime=5
 *      nsk.jvmti.RedefineClasses.redefclass030
 *      1000 ./bin
 */

