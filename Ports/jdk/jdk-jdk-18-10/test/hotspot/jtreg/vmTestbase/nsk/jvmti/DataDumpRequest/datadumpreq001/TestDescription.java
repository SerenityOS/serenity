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
 * @summary converted from VM Testbase nsk/jvmti/DataDumpRequest/datadumpreq001.
 * VM Testbase keywords: [jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test exercises the JVMTI event DataDumpRequest.
 *     It verifies that the event will be sent only during the live phase of
 *     VM execution.
 *     The test works as follows. The agent enables the DataDumpRequest
 *     event on 'OnLoad' phase. Then the java part run a special perl script
 *     'datadumpreq001.pl' in a separate child process. The script obtains PID
 *     of its parent process and sends it the signal QUIT (CTRL-\). If the
 *     DataDumpRequest was not send and/or the script failed, the test ignores
 *     it and passes. Otherwise, the VM phase is checked during the
 *     DataDumpRequest callback.
 * COMMENTS
 *     Note that obtained PID mentioned above may not belong to the java
 *     process itself in non-HotSpot VMs. It may happen if VM runs child
 *     process indirectly via a separate shell.
 *     Besides that, sending CTRL-\ causes the HotSpot VM itself to print its
 *     full thread dump. The dump may be ignored.
 *
 *     The test has been fixed due to the bug 4947594.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.DataDumpRequest.datadumpreq001
 * @run main/othervm/native
 *      -agentlib:datadumpreq001=-waittime=5,-verbose=
 *      nsk.jvmti.DataDumpRequest.datadumpreq001
 */

