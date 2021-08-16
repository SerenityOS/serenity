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
 * @summary converted from VM Testbase nsk/jvmti/unit/GetLocalVariable/getlocal004.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_caps, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This is unit test for new jvmdi/jvmti bug XXXXXXX:
 *     Synopsis: SIGBUS in Get/SetLocalLong and Get/SetLocalDouble functions
 *     The test exercise JVMTI functions which allow to get local
 *     variable value when there is no Local Variable Table is available:
 *         GetLocalInt, GetLocalLong, GetLocalDouble
 *     The *.java file is compiled with flag: -g:none.
 *     The test checks if the functions properly return error codes:
 *         JVMTI_ERROR_INVALID_SLOT
 *         JVMTI_ERROR_NONE
 *     The test checks if there is a problem when we are trying to
 *     retrieve Long or Double value for slot number max_locals() - 1.
 *     We expect JVMTI_ERROR_INVALID_SLOT in this case.
 * COMMENTS
 *     The test was cloned from nsk/jvmti/GetLocalVariable/getlocal002
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment make sure getlocal004 is compiled with no debug info
 * @build nsk.jvmti.unit.GetLocalVariable.getlocal004
 * @clean nsk.jvmti.unit.GetLocalVariable.getlocal004
 * @compile -g:none ../getlocal004.java
 *
 * @run main/othervm/native -agentlib:getlocal004 nsk.jvmti.unit.GetLocalVariable.getlocal004
 */

