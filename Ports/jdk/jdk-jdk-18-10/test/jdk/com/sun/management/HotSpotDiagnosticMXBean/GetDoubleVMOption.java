/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8061616
 * @summary Basic Test for HotSpotDiagnosticMXBean.getVMOption() and double values
 * @author  Jaroslav Bachorik
 *
 * @run main/othervm -XX:CompileThresholdScaling=0.14 GetDoubleVMOption
 */

import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.VMOption;
import java.lang.management.ManagementFactory;
import java.util.List;
import javax.management.MBeanServer;

public class GetDoubleVMOption {
    private static final String COMPILE_THRESHOLD_SCALING = "CompileThresholdScaling";
    private static final String EXPECTED_VALUE = "0.14";
    private static final String HOTSPOT_DIAGNOSTIC_MXBEAN_NAME =
        "com.sun.management:type=HotSpotDiagnostic";

    public static void main(String[] args) throws Exception {
        List<HotSpotDiagnosticMXBean> list =
            ManagementFactory.getPlatformMXBeans(HotSpotDiagnosticMXBean.class);
        HotSpotDiagnosticMXBean mbean = list.get(0);
        checkVMOption(mbean);

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        mbean = ManagementFactory.newPlatformMXBeanProxy(mbs,
                    HOTSPOT_DIAGNOSTIC_MXBEAN_NAME,
                    HotSpotDiagnosticMXBean.class);
        checkVMOption(mbean);
    }

    private static void checkVMOption(HotSpotDiagnosticMXBean mbean) {
        VMOption option = mbean.getVMOption(COMPILE_THRESHOLD_SCALING);
        if (!option.getValue().equalsIgnoreCase(EXPECTED_VALUE)) {
            throw new RuntimeException("Unexpected value: " +
                option.getValue() + " expected: " + EXPECTED_VALUE);
        }
    }
}
