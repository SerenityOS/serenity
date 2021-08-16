/*
 * Copyright (c) 2004, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4990825
 * @summary prolog size and overflow sanity checks
 *
 * @run main/othervm -XX:+UsePerfData PrologSizeSanityCheck
 */

import sun.jvmstat.monitor.*;

public class PrologSizeSanityCheck {

    private static final String sizeName = "sun.perfdata.size";
    private static final String usedName = "sun.perfdata.used";
    private static final String overflowName = "sun.perfdata.overflow";
    private static final int K = 1024;

    public static void main(String args[]) throws Exception {

        VmIdentifier vmid = new VmIdentifier("0");
        MonitoredHost localhost = MonitoredHost.getMonitoredHost("localhost");
        MonitoredVm self = localhost.getMonitoredVm(vmid);

        IntegerMonitor prologSize = (IntegerMonitor)self.findByName(sizeName);
        IntegerMonitor prologUsed = (IntegerMonitor)self.findByName(usedName);
        IntegerMonitor prologOverflow =
                (IntegerMonitor)self.findByName(overflowName);

        if (prologOverflow.intValue() != 0) {
            throw new RuntimeException("jvmstat memory buffer overflow: "
                    + sizeName + "=" + prologSize.intValue()
                    + usedName + "=" + prologUsed.intValue()
                    + overflowName + "=" + prologOverflow.intValue()
                    + " : PerfDataMemorySize must be increased");
        }

        if (prologUsed.intValue() + 3*K >= prologSize.intValue()) {
          /*
           * we want to leave at least 3K of space to allow for long
           * string names in the various path oriented strings and for
           * the command line argument and vm argument strings. 3K is
           * somewhat of an arbitrary figure, but it is based on failure
           * scenarios observed in SQE when jvmstat was originally
           * introduced in 1.4.1.
           */
            throw new RuntimeException(
                   "jvmstat memory buffer usage approaching size: "
                    + sizeName + "=" + prologSize.intValue()
                    + usedName + "=" + prologUsed.intValue()
                    + " : consider increasing PerfDataMemorySize");
        }
    }
}
