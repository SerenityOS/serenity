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
 * @summary converted from VM Testbase nsk/jvmti/NotifyFramePop/nframepop001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test exercise JVMTI function NotifyFramePop.
 *     The test debug agent calls the function from the hook
 *     function and native method for the test cases:
 *     - current and other threads;
 *     - method terminates normally and by throwing an exception
 * COMMENTS
 *     To fix bug 4463667, one code fragment with "Thread.sleep(500);"
 *     is replaced with following one:
 *         Object obj = new Object();
 *         *
 *         *
 *         synchronized (obj) {
 *             obj.wait(500);
 *         }
 *     Fixed according to 4669812 bug.
 *     Ported from JVMDI.
 *     Fixed the 5007997 bug.
 *     Fixed the 5031355 bug.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native -agentlib:nframepop001 nsk.jvmti.NotifyFramePop.nframepop001
 */

