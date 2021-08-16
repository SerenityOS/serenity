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
 * Results of running the JstatGcTool ("jstat -gc <pid>")
 *
 * Output example:
 *    S0C         S1C         S0U         S1U          EC           EU           OC           OU          MC         MU       CCSC      CCSU     YGC     YGCT     FGC    FGCT     CGC    CGCT       GCT
 *        0.0      2048.0         0.0      1079.5       4096.0          0.0       2048.0          0.0     7680.0     7378.6     768.0     664.7      4     0.140     0     0.000     0     0.000     0.140
 *
 * Output description:
 * S0C     Current survivor space 0 capacity (KB).
 * S1C     Current survivor space 1 capacity (KB).
 * S0U     Survivor space 0 utilization (KB).
 * S1U     Survivor space 1 utilization (KB).
 * EC      Current eden space capacity (KB).
 * EU      Eden space utilization (KB).
 * OC      Current old space capacity (KB).
 * OU      Old space utilization (KB).
 * MC      Current metaspace capacity (KB).
 * MU      Metaspace utilization (KB).
 * CCSC    Compressed Class Space capacity
 * CCSU    Compressed Class Space utilization
 * YGC     Number of young generation GC Events.
 * YGCT    Young generation garbage collection time.
 * FGC     Number of full GC events.
 * FGCT    Full garbage collection time.
 * CGC     Concurrent Collections (STW phase)
 * CGCT    Concurrent Garbage Collection Time (STW phase)
 * GCT     Total garbage collection time.
 *
 */
package utils;

import common.ToolResults;

public class JstatGcResults extends JstatResults {

    public JstatGcResults(ToolResults rawResults) {
        super(rawResults);
    }

    /**
     * Checks the overall consistency of the results reported by the tool
     */
    @Override
    public void assertConsistency() {

        assertThat(getExitCode() == 0, "Unexpected exit code: " + getExitCode());

        float OC = getFloatValue("OC");
        float OU = getFloatValue("OU");
        assertThat(OU <= OC, "OU > OC (utilization > capacity)");

        float MC = getFloatValue("MC");
        float MU = getFloatValue("MU");
        assertThat(MU <= MC, "MU > MC (utilization > capacity)");

        float CCSC = getFloatValue("CCSC");
        float CCSU = getFloatValue("CCSU");
        assertThat(CCSU <= CCSC, "CCSU > CCSC (utilization > capacity)");

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
        assertThat(YGCT >= 0, "Incorrect time value for YGCT");

        float GCT = getFloatValue("GCT");
        assertThat(GCT >= 0, "Incorrect time value for GCT");
        assertThat(GCT >= YGCT, "GCT < YGCT (total garbage collection time < young generation garbage collection time)");

        int CGC = 0;
        float CGCT = 0.0f;
        try {
            CGC = getIntValue("CGC");
        } catch (NumberFormatException e) {
            if (!e.getMessage().equals("Unparseable number: \"-\"")) {
                throw e;
            }
        }
        if (CGC > 0) {
            CGCT = getFloatValue("CGCT");
            assertThat(CGCT >= 0, "Incorrect time value for CGCT");
        }

        int FGC = getIntValue("FGC");
        float FGCT = getFloatValue("FGCT");
        assertThat(FGCT >= 0, "Incorrect time value for FGCT");

        assertThat(GCT >= FGCT, "GCT < YGCT (total garbage collection time < full generation garbage collection time)");

        assertThat(checkFloatIsSum(GCT, YGCT, CGCT, FGCT), "GCT != (YGCT + CGCT + FGCT) " + "(GCT = " + GCT + ", YGCT = " + YGCT
                + ", CGCT = " + CGCT + ", FGCT = " + FGCT + ", (YCGT + CGCT + FGCT) = " + (YGCT + CGCT + FGCT) + ")");
    }
}
