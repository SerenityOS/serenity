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
 * @summary converted from VM Testbase nsk/jvmti/PopFrame/popframe007.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test exercises JVMTI function PopFrame(thread).
 *     The test creates a new thread setting breakpoint to the thread's
 *     method C then starts the thread which do the following:
 *         - call method A
 *         - call native method B
 *         - call method C
 *     This trigger the breakpoint. Catching breakpoint event
 *     the test calls the function PopFrame expecting
 *     JVMTI_ERROR_OPAQUE_FRAME to be returned.
 * COMMENTS
 *     This is a regression test on
 *         4472844 popFrame of native frame crashes - should skip native frames
 *     Ported from JVMDI.
 *     Fixed according to 4912302 bug.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native -agentlib:popframe007 nsk.jvmti.PopFrame.popframe007
 */

