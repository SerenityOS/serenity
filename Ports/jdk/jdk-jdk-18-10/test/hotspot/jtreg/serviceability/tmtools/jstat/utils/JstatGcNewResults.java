/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * Results of running the JstatGcTool ("jstat -gcnew <pid>")
 *
 * Output example:
 *    S0C         S1C         S0U         S1U     TT MTT     DSS          EC           EU       YGC     YGCT
 *        0.0      2048.0         0.0      1113.5 15  15      1024.0       4096.0          0.0      4     0.228
 *
 * Output description:
 * S0C     Current survivor space 0 capacity (KB).
 * S1C     Current survivor space 1 capacity (KB).
 * S0U     Survivor space 0 utilization (KB).
 * S1U     Survivor space 1 utilization (KB).
 * TT        Tenuring threshold.
 * MTT     Maximum tenuring threshold.
 * DSS     Desired survivor size (KB).
 * EC        Current eden space capacity (KB).
 * EU        Eden space utilization (KB).
 * YGC     Number of young generation GC events.
 * YGCT   Young generation garbage collection time.
 */
package utils;

import common.ToolResults;

public class JstatGcNewResults extends JstatResults {

    public JstatGcNewResults(ToolResults rawResults) {
        super(rawResults);
    }

    /**
     * Checks the overall consistency of the results reported by the tool
     */
    @Override
    public void assertConsistency() {

        assertThat(getExitCode() == 0, "Unexpected exit code: " + getExitCode());

        float S0C = getFloatValue("S0C");
        float S0U = getFloatValue("S0U");

        assertThat(S0U <= S0C, "S0U > S0C (utilization > capacity)");

        float S1C = getFloatValue("S1C");
        float S1U = getFloatValue("S1U");

        assertThat(S1U <= S1C, "S1U > S1C (utilization > capacity)");

        float EC = getFloatValue("EC");
        float EU = getFloatValue("EU");

        assertThat(EU <= EC, "EU > EC (utilization > capacity)");

        int YGC = getIntValue("YGC");
        float YGCT = getFloatValue("YGCT");

        int TT = getIntValue("TT");
        int MTT = getIntValue("MTT");
        assertThat(TT <= MTT, "TT > MTT (tenuring threshold > maximum tenuring threshold)");
    }
}
