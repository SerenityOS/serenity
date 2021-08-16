/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6359948
 * @summary Check that MXBean operations have the expected ReturnType in MBeanOperationInfo
 * @author Luis-Miguel Alventosa
 *
 * @run clean MBeanOperationInfoTest
 * @run build MBeanOperationInfoTest
 * @run main MBeanOperationInfoTest
 */

import java.lang.management.*;
import javax.management.*;
import javax.management.openmbean.*;

public class MBeanOperationInfoTest {
    private static final String[][] returnTypes = {
        { "dumpAllThreads(boolean,boolean)", "[Ljavax.management.openmbean.CompositeData;" },
        { "findDeadlockedThreads()", "[J" },
        { "findMonitorDeadlockedThreads()", "[J" },
        { "getThreadInfo(long)","javax.management.openmbean.CompositeData"},
        { "getThreadInfo(long,int)","javax.management.openmbean.CompositeData"},
        { "getThreadInfo([J)","[Ljavax.management.openmbean.CompositeData;"},
        { "getThreadInfo([J,int)","[Ljavax.management.openmbean.CompositeData;"},
        { "getThreadInfo([J,boolean,boolean)","[Ljavax.management.openmbean.CompositeData;"},
        { "getThreadCpuTime(long)","long"},
        { "getThreadUserTime(long)","long"},
        { "resetPeakThreadCount()","void"},
    };
    public static void main(String[] args) throws Exception {
        int error = 0;
        int tested = 0;
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName on = new ObjectName(ManagementFactory.THREAD_MXBEAN_NAME);
        MBeanInfo mbi = mbs.getMBeanInfo(on);
        MBeanOperationInfo[] ops = mbi.getOperations();
        for (MBeanOperationInfo op : ops) {
            String name = op.getName();
            String rt = op.getReturnType();
            StringBuilder sb = new StringBuilder();
            sb.append(name + "(");
            for (MBeanParameterInfo param : op.getSignature()) {
                sb.append(param.getType() + ",");
            }
            int comma = sb.lastIndexOf(",");
            if (comma == -1) {
                sb.append(")");
            } else {
                sb.replace(comma, comma + 1, ")");
            }
            System.out.println("\nNAME = " + sb.toString() + "\nRETURN TYPE = " + rt);
            for (String[] rts : returnTypes) {
                if (sb.toString().equals(rts[0])) {
                    if (rt.equals(rts[1])) {
                        System.out.println("OK");
                        tested++;
                    } else {
                        System.out.println("KO: EXPECTED RETURN TYPE = " + rts[1]);
                        error++;
                    }
                }
            }
        }
        if (error > 0) {
            System.out.println("\nTEST FAILED");
            throw new Exception("TEST FAILED: " + error + " wrong return types");
        } else if (tested != returnTypes.length &&
                   !System.getProperty("java.specification.version").equals("1.5")) {
            System.out.println("\nTEST FAILED");
            throw new Exception("TEST FAILED: " + tested + " cases tested, " +
            returnTypes.length + " expected");
        } else {
            System.out.println("\nTEST PASSED");
        }
    }
}
