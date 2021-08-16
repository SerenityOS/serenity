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
 * @summary converted from VM Testbase nsk/jdi/ConstantField/values001.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     Test checks up static fields of JDI interfaces according to
 *     specification of them:
 *             com.sun.jdi.ClassType
 *                 INVOKE_SINGLE_THREADED      = 1
 *             com.sun.jdi.ObjectReference
 *                 INVOKE_NONVIRTUAL           = 2
 *                 INVOKE_SINGLE_THREADED      = 1
 *             com.sun.jdi.ThreadReference
 *                 THREAD_STATUS_MONITOR       = 3
 *                 THREAD_STATUS_NOT_STARTED   = 5
 *                 THREAD_STATUS_RUNNING       = 1
 *                 THREAD_STATUS_SLEEPING      = 2
 *                 THREAD_STATUS_UNKNOWN       = -1
 *                 THREAD_STATUS_WAIT          = 4
 *                 THREAD_STATUS_ZOMBIE        = 0
 *             com.sun.jdi.VirtualMachine
 *                 TRACE_ALL                   = 16777215
 *                 TRACE_EVENTS                = 4
 *                 TRACE_NONE                  = 0
 *                 TRACE_OBJREFS               = 16
 *                 TRACE_RECEIVES              = 2
 *                 TRACE_REFTYPES              = 8
 *                 TRACE_SENDS                 = 1
 *             com.sun.jdi.request.EventRequest
 *                 SUSPEND_ALL                 = 2
 *                 SUSPEND_EVENT_THREAD        = 1
 *                 SUSPEND_NONE                = 0
 *             com.sun.jdi.request.StepRequest
 *                 STEP_INTO                   = 1
 *                 STEP_LINE                   = -2
 *                 STEP_MIN                    = -1
 *                 STEP_OUT                    = 3
 *                 STEP_OVER                   = 2
 * COMMENTS:
 *     This test is valid for jdk 1.4.1 or higher because of that
 *     there was no specifications for earlier versions.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm nsk.jdi.ConstantField.values001
 */

