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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/general_functions/GF04/gf04t001.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_logic, noras, no_cds]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test implements the GF04 scenario of test plan for General
 *     Functions:
 *         Set calbacks for ClassPrepare and ClassLoad events. Add segment
 *         with path to class file of class A by AddToBootstrapClassLoaderSearch.
 *         Create instance of class A. Check that ClassPrepare and ClassLoad
 *         events are generated for class A.
 *     Source of debuggee class 'gf04t001' is located in special subdirectory
 *     'newclass' and will be compiled to '$COMMON_CLASSES_LOCATION/loadclass',
 *     where it usually can not be found by bootstrap classloader.
 *     The test agent adds this subdirectory to the bootstrap class search path
 *     in Agent_Onload callback, and thus the debuggee class should be found by
 *     bootstrap classloader.
 *     Also the agent sets callbacks for ClassLoad and ClassPrepare events and
 *     enables them. The signature of a loaded/prepared class passed in
 *     to the callbacks is compared with expected one of 'gf04t001' class.
 * COMMENTS
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
 *      -agentlib:gf04t001=-waittime=5,segment=./bin/newclass
 *      nsk.jvmti.scenarios.general_functions.GF04.gf04t001
 */

