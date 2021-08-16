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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/contention/TC05/tc05t001.
 * VM Testbase keywords: [jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test is for TC05 and TC06 scenarios of "thread contention profiling".
 *     The test sets callbacks for MonitorWait and MonitorWaited events and
 *     lets thread T to wait in monitor M for 1 millisec then checks that
 *     time frame between the events calculated via GetTime is equal or greater
 *     than 1 millisec if waiting of T was timed out (TC05).
 *     The test also checks that time frame returned by GetThreadCpuTime
 *     for T between the events is less than 1 millisec if waiting
 *     of T was timed out (TC06).
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -agentlib:tc05t001=verbose,waittime=5
 *      nsk.jvmti.scenarios.contention.TC05.tc05t001
 */

