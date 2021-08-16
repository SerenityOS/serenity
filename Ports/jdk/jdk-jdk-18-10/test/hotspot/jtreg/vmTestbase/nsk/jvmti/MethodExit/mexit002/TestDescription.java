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
 * @summary converted from VM Testbase nsk/jvmti/MethodExit/mexit002.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test exercises JVMTI event callback function MethodExit.
 *     The test checks the following:
 *       - if clazz, method and frame parameters of the function
 *         contain expected values for events generated upon exit
 *         from Java and native methods.
 *       - if GetFrameLocation indentifies the executable location
 *         in the returning method, immediately prior to the return.
 *     The test is the same as mexit001 one. The only difference is
 *     the METHOD_EXIT event enable is moved from method chain()
 *     to method check().
 * COMMENTS
 *     Ported from JVMDI.
 *     Fixed the 5004632 bug.
 *
 * @library /vmTestbase
 *          /test/lib
 * @compile mexit002a.jasm
 * @run main/othervm/native -agentlib:mexit002 nsk.jvmti.MethodExit.mexit002
 */

