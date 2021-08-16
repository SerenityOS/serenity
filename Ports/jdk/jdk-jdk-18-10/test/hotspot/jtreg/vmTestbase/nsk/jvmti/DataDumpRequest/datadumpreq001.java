/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.DataDumpRequest;

import java.io.*;

import nsk.share.*;
import nsk.share.jvmti.*;
import vm.share.ProcessUtils;

/**
 * This test exercises the JVMTI event <code>DataDumpRequest</code>.
 * <br>It verifies that the event will be sent only during the live
 * phase of VM execution.<p>
 * The test works as follows. The agent enables the DataDumpRequest
 * event on <code>OnLoad</code> phase. Then the java part
 * imitates Ctrl-\ or Ctrl-Break (on Windows).If the DataDumpRequest
 * was not send the test ignores it and passes. Otherwise, the VM phase
 * is checked during the DataDumpRequest callback.<br>
 * Note that sending CTRL-\ causes HotSpot VM itself to print its full
 * thread dump. The dump may be ignored.
 *
 * @see vm.share.ProcessUtils#sendCtrlBreak
 */
public class datadumpreq001 {
        static {
                System.loadLibrary("ProcessUtils");
                System.loadLibrary("datadumpreq001");
        }

        public static native int waitForResult();

        public static void main(String[] argv) {
                argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

                // produce JCK-like exit status
                System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
        }

        public static int run(String argv[], PrintStream out) {
                ProcessUtils.sendCtrlBreak();
                return waitForResult();
        }
}
