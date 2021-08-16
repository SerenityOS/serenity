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
 * @summary converted from VM Testbase nsk/jvmti/GetLocalVariableTable/localtab004.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_caps, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks that the JVMTI function GetLocalVariableTable()
 *     returns local variable information properly.
 *     The test creates a dummy instance of tested class 'localtab004a' which
 *     must be compiled with debugging info. Then an agent part verifies
 *     the local variable table for the following methods of localtab004a:
 *         - constructor
 *         - static method statMethod()
 *         - instance method finMethod()
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment make sure localtab004 is compiled with full debug info
 * @build nsk.jvmti.GetLocalVariableTable.localtab004
 * @clean nsk.jvmti.GetLocalVariableTable.localtab004
 * @compile -g:lines,source,vars ../localtab004.java
 *
 * @run main/othervm/native
 *      -agentlib:localtab004=-waittime=5
 *      nsk.jvmti.GetLocalVariableTable.localtab004
 */

