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
 * @summary converted from VM Testbase nsk/jvmti/unit/functions/AddToBootstrapClassLoaderSearch/JvmtiTest.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_logic, noras, no_cds]
 * VM Testbase readme:
 * DESCRIPTION
 *     Unit test for JVMTI AddToBootstrapClassLoaderSearch function.
 * COMMENTS
 *     Fixed the 5009201 bug.
 *     Fixed due to spec change according to the bug:
 *     5062403 TEST_BUG: AddToBootstrapClassLoaderSearch and SetSystemProperties only OnLoad
 *     Fixed TEST_BUG 6254598.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.unit.functions.AddToBootstrapClassLoaderSearch.JvmtiTest
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass
 *
 * @run main/othervm/native
 *      -agentlib:AddToBootstrapClassLoaderSearch=./bin
 *      nsk.jvmti.unit.functions.AddToBootstrapClassLoaderSearch.JvmtiTest
 */

