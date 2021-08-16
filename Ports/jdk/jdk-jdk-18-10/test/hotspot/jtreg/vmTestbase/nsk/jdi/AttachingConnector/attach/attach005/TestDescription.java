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
 * @summary converted from VM Testbase nsk/jdi/AttachingConnector/attach/attach005.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test checks that debugger may establish connection with
 *     a target VM via com.sun.jdi.ProcessAttach connector.
 *     Target VM is started with option "suspend=y" (VM is to be suspended immediately before the main class is loaded).
 *     Test uses shared memory for communication between a debugger and the target VM.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.AttachingConnector.attach.attach004.TestDriver
 * @run driver
 *      nsk.jdi.AttachingConnector.attach.attach004.TestDriver
 *      ${os.family}-${os.simpleArch} dt_shmem y
 *      -waitVMStartEvent
 */

