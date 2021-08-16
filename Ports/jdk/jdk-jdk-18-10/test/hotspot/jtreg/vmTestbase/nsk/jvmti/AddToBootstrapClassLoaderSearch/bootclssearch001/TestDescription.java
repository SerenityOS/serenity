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
 * @summary converted from VM Testbase nsk/jvmti/AddToBootstrapClassLoaderSearch/bootclssearch001.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_logic, noras, no_cds]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test exercises JVMTI thread function AddToBootstrapClassLoaderSearch().
 *     This test checks that AddToBootstrapClassLoaderSearch() adds segment
 *     to bootstrap class search path in OnLoad phase and bootstrap classloader
 *     will find debuggee class using that segment.
 *     Debuggee class does not have direct superclass to be loaded too.
 *     The following checks are performed by the test:
 *         - AddToBootstrapClassLoaderSearch() returns no errors in OnLoad phase
 *         - debuggee class will be found by bootstrap classloader using added segment
 *     Source of debuggee class 'bootclssearch001' is located in special subdirectory
 *     'newclass' and thus will be compiled to '$COMMON_CLASSES_LOCATION/newclass'
 *     subdirectory, where it usually can not be found by bootstrap classloader.
 *     The agent adds this subdirectory to the bootstrap class search path
 *     in Agent_OnLoad(), and thus the debuggee class should be found by
 *     bootstrap classloader and executed normally.
 *     If AddToBootstrapClassLoaderSearch() returns error in Agent_OnLoad(),
 *     then agent returns JNI_ERR and VM fails on initialization.
 *     If AddToBootstrapClassLoaderSearch() does not adds segment to bootstrap
 *     class search path, then debuggee class is not found and VM fails on
 *     initialization.
 *     Otherwise, if debuggee class is found, loaded, executed, it returns PASS
 *     and the test passes with exit code 95.
 * COMMENTS
 *     Made code cleanup: nsk/share/jvmti/AddToBootstrapClassLoaderSearch/bootclssearch_agent
 *                        agent is used now.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.share.Consts
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass
 *
 * @comment ExecDriver is used b/c main class isn't on source/class path
 * @run main/othervm/native ExecDriver --java
 *      -agentlib:bootclssearch_agent=-waittime=5,phasetocheck=onload,segment1=./bin/newclass
 *      nsk.jvmti.AddToBootstrapClassLoaderSearch.bootclssearch001
 */

