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
 * @summary converted from VM Testbase nsk/jvmti/GetJNIFunctionTable/getjniftab001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test exercises the JVMTI function GetJNIFunctionTable().
 *     It checks the following spec assertion:
 *         If SetJNIFunctionTable has been called, the modified (not
 *         the original) function table is returned.
 *     The assertion is verified twice with modified version and original
 *     version of the JNI function GetVersion().
 *     Upon the function redirection, the test obtains the function table
 *     through the GetJNIFunctionTable() and calls GetVersion() expecting
 *     its modified version. Then, upon restoring the original JNI function
 *     table, the same check is performed expecting the original version of
 *     GetVersion() to be called.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native -agentlib:getjniftab001 nsk.jvmti.GetJNIFunctionTable.getjniftab001
 */

