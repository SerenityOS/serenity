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
 * @summary converted from VM Testbase nsk/jvmti/AddToBootstrapClassLoaderSearch/bootclssearch003.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_logic, noras, no_cds]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test exercises JVMTI thread function AddToBootstrapClassLoaderSearch().
 *     This test checks that AddToBootstrapClassLoaderSearch() adds segment
 *     to bootstrap class search path in OnLoad phase, but debuggee class
 *     located by this segment will no be loaded if this class is available
 *     from the original bootstrap classpath.
 *     The following checks are performed by the test:
 *         - AddToBootstrapClassLoaderSearch() returns no errors in OnLoad phase
 *         - debuggee class located by the original bootstrap classpath will be loaded
 *         - debuggee class located by the added segment will NOT be loaded
 *     There are two different impelmentations of debuggee class 'bootclssearch003'.
 *     Source of "positive" implementation is located in subdirectory 'loadclass'.
 *     It will be compiled to '$COMMON_CLASSES_LOCATION/loadclass' and added to
 *     original bootstrap classpath using option +Xbootclasspath/a.
 *     This implementation returns PASS status.
 *     Source of "negative" implementation is located in subdirectory 'newclass'.
 *     It will be compiled to '$COMMON_CLASSES_LOCATION/newclass' and added
 *     to bootstrap class search path with AddToBootstrapClassLoaderSearch().
 *     This implementation returns prints error and returns FAIL status.
 *     The agent adds subdirectory '$COMMON_CLASSES_LOCATION/newclass' to
 *     the bootstrap classloader search path in Agent_OnLoad(), and expects
 *     that positive implementation of debuggee class will be loaded and
 *     executed.
 *     If AddToBootstrapClassLoaderSearch() returns error in Agent_OnLoad(),
 *     then agent returns JNI_ERR and VM fails on initialization.
 *     If negative version of debuggee class will be loaded and executed,
 *     then it returns FAIL and test fails with exit code 97.
 *     Otherwise, if positive version of debuggee class will be loaded and executed,
 *     then it returns PASS and test passes with exit code 95.
 * COMMENTS
 *     Made code cleanup: nsk/share/jvmti/AddToBootstrapClassLoaderSearch/bootclssearch_agent
 *                        agent is used now.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.share.Consts
 *
 * @comment compile loadclassXX to bin/loadclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      loadclass
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass
 *
 * @comment ExecDriver is used b/c main class isn't on source/class path
 * @run main/othervm/native ExecDriver --java
 *      -Xbootclasspath/a:./bin/loadclass
 *      -agentlib:bootclssearch_agent=-waittime=5,phasetocheck=onload,segment1=./bin/newclass
 *      nsk.jvmti.AddToBootstrapClassLoaderSearch.bootclssearch003
 */

