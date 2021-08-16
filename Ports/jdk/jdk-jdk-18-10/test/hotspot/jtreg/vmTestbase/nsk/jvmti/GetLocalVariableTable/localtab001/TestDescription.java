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
 * @summary converted from VM Testbase nsk/jvmti/GetLocalVariableTable/localtab001.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_logic, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test exercise JVMTI function GetLocalVariableTable.
 *     The test cases include:
 *     - method without parameters and locals,
 *     - method local variables, method parameters
 *     - various types of local variables: object, int array, long, float,
 *       double, int, short, char, byte, boolean
 *     - various scopes of visibility,
 *     - long local's name.
 * COMMENTS
 *     Fixed according to the rfe 4388972.
 *     Ported from JVMDI.
 *
 * @library /vmTestbase
 *          /test/lib
 * @compile localtab001.jcod
 * @run main/othervm/native -agentlib:localtab001 nsk.jvmti.GetLocalVariableTable.localtab001
 */

