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
 * @summary converted from VM Testbase nsk/jvmti/RedefineClasses/redefclass029.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, redefine]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test exercises that the JVMTI function RedefineClasses()
 *     enable to redefine a static inner (nested) class properly.
 *     Redefiniton is performed in asynchronous manner from a separate
 *     thread when the VM is provoked to switch into compiled mode.
 *     The test works as follows. Two threads are started: java thread
 *     executing a nested class to be redefined, and native one executing
 *     an agent code. Then the nested class method 'redefclass029HotMethod()'
 *     is provoked to be compiled (optimized), and thus the JVMTI event
 *     'CompiledMethodLoad' should be sent. After that, the agent redefines
 *     the nested class. Different kinds of outer fields are accessed from
 *     executing methods in both versions of the nested class.
 *     Upon the redefinition, the main test thread verifies via the outer
 *     fields values that the nested method 'run()' having an active stack
 *     frame stays obsolete but the other nested methods have been redefined.
 *     It also verifies that the outer class is still can access the nested
 *     class fields after the redefinition.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.RedefineClasses.redefclass029
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass
 *
 * @run main/othervm/native
 *      -XX:CompileThreshold=1000
 *      -XX:-TieredCompilation
 *      -agentlib:redefclass029=-waittime=5
 *      nsk.jvmti.RedefineClasses.redefclass029
 *      5000 ./bin
 */

