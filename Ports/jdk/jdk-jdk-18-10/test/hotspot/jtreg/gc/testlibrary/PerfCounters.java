/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

package gc.testlibrary;

import sun.jvmstat.monitor.Monitor;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.VmIdentifier;
import jdk.test.lib.process.ProcessTools;

/**
 * PerfCounters can be used to get a performance counter from the currently
 * executing VM.
 *
 * Throws a runtime exception if an error occurs while communicating with the
 * currently executing VM.
 */
public class PerfCounters {
    private final static MonitoredVm vm;

    static {
        try {
            String pid = Long.toString(ProcessTools.getProcessId());
            VmIdentifier vmId = new VmIdentifier(pid);
            MonitoredHost host = MonitoredHost.getMonitoredHost(vmId);
            vm = host.getMonitoredVm(vmId);
        } catch (Exception e) {
            throw new RuntimeException("Could not connect to the VM");
        }
    }

    /**
     * Returns the performance counter with the given name.
     *
     * @param name The name of the performance counter.
     * @throws IllegalArgumentException If no counter with the given name exists.
     * @throws MonitorException If an error occurs while communicating with the VM.
     * @return The performance counter with the given name.
     */
    public static PerfCounter findByName(String name)
        throws MonitorException, IllegalArgumentException {
        Monitor m = vm.findByName(name);
        if (m == null) {
            throw new IllegalArgumentException("Did not find a performance counter with name " + name);
        }
        return new PerfCounter(m, name);
    }
}
