/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8167108 8266130
 * @summary converted from VM Testbase nsk/jvmti/PopFrame/popframe011.
 * VM Testbase keywords: [jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     Same test as popframe002 with additional calls to PopFrame()
 *     while threads are exiting.
 *     Failing criteria for the test are:
 *       - failures of used JVMTI functions.
 * COMMENTS
 *     Derived from nsk/jvmti/PopFrame/popframe002.
 *
 * @requires vm.jvmti
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -agentlib:popframe011
 *      nsk.jvmti.PopFrame.popframe011
 */

