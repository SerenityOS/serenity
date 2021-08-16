/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     7028071
 * @summary Basic unit test of OperatingSystemMXBean.getProcessCpuLoad()
 *
 * @run main GetSystemCpuLoad
 */

import java.lang.management.*;
import com.sun.management.OperatingSystemMXBean;

public class GetSystemCpuLoad {
    public static void main(String[] argv) throws Exception {
        OperatingSystemMXBean mbean = (com.sun.management.OperatingSystemMXBean)
            ManagementFactory.getOperatingSystemMXBean();
        double load;
        for(int i=0; i<10; i++) {
            load = mbean.getSystemCpuLoad();
            if((load<0.0 || load>1.0) && load != -1.0) {
                throw new RuntimeException("getSystemCpuLoad() returns " + load
                       +  " which is not in the [0.0,1.0] interval");
            }
            try {
                Thread.sleep(200);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
