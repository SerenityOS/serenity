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
 * @summary converted from VM Testbase nsk/jvmti/GetLocalVariable/getlocal001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test exercise JVMTI functions which allow to get local
 *     variable value:
 *     GetLocalObject, GetLocalInt, GetLocalLong, GetLocalFloat, GetLocalDouble
 *     The test cases include:
 *     - call point: from event hook function on method exit and breakpoint
 *     - method local variable, method parameter
 *     - various types: object, int array, long, float, double, int, short,
 *       char, byte, boolean
 * COMMENTS
 *     Fixed according to the rfe 4388972.
 *     Fixed according to the rfe 4513985.
 *     Ported from JVMDI.
 *     Fixed according to 4910192 bug.
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment make sure getlocal001 is compiled with full debug info
 * @build nsk.jvmti.GetLocalVariable.getlocal001
 * @clean nsk.jvmti.GetLocalVariable.getlocal001
 * @compile -g:lines,source,vars ../getlocal001.java
 *
 * @run main/othervm/native -agentlib:getlocal001 nsk.jvmti.GetLocalVariable.getlocal001
 */

