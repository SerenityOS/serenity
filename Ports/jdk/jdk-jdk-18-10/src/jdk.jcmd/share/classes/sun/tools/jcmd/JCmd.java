/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.tools.jcmd;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Comparator;
import java.net.URISyntaxException;

import com.sun.tools.attach.AttachOperationFailedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.sun.tools.attach.AttachNotSupportedException;

import sun.tools.attach.HotSpotVirtualMachine;
import sun.tools.common.ProcessArgumentMatcher;
import sun.tools.common.PrintStreamPrinter;
import sun.tools.jstat.JStatLogger;
import sun.jvmstat.monitor.Monitor;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.VmIdentifier;

public class JCmd {
    public static void main(String[] args) {
        Arguments arg = null;
        try {
            arg = new Arguments(args);
        } catch (IllegalArgumentException ex) {
            System.err.println("Error parsing arguments: " + ex.getMessage()
                               + "\n");
            Arguments.usage();
            System.exit(1);
        }

        if (arg.isShowUsage()) {
            Arguments.usage();
            System.exit(0);
        }

        ProcessArgumentMatcher ap = null;
        try {
            ap = new ProcessArgumentMatcher(arg.getProcessString());
        } catch (IllegalArgumentException iae) {
            System.err.println("Invalid pid '" + arg.getProcessString()  + "' specified");
            System.exit(1);
        }

        if (arg.isListProcesses()) {
            for (VirtualMachineDescriptor vmd : ap.getVirtualMachineDescriptors(/* include jcmd in listing */)) {
                System.out.println(vmd.id() + " " + vmd.displayName());
            }
            System.exit(0);
        }

        Collection<String> pids = ap.getVirtualMachinePids(JCmd.class);

        if (pids.isEmpty()) {
            System.err.println("Could not find any processes matching : '"
                               + arg.getProcessString() + "'");
            System.exit(1);
        }

        boolean success = true;
        for (String pid : pids) {
            System.out.println(pid + ":");
            if (arg.isListCounters()) {
                listCounters(pid);
            } else {
                try {
                    executeCommandForPid(pid, arg.getCommand());
                } catch(AttachOperationFailedException ex) {
                    System.err.println(ex.getMessage());
                    success = false;
                } catch(Exception ex) {
                    ex.printStackTrace();
                    success = false;
                }
            }
        }
        System.exit(success ? 0 : 1);
    }

    private static void executeCommandForPid(String pid, String command)
        throws AttachNotSupportedException, IOException,
               UnsupportedEncodingException {
        VirtualMachine vm = VirtualMachine.attach(pid);

        // Cast to HotSpotVirtualMachine as this is an
        // implementation specific method.
        HotSpotVirtualMachine hvm = (HotSpotVirtualMachine) vm;
        String lines[] = command.split("\\n");
        for (String line : lines) {
            if (line.trim().equals("stop")) {
                break;
            }

            InputStream is = hvm.executeJCmd(line);

            if (PrintStreamPrinter.drainUTF8(is, System.out) == 0) {
                System.out.println("Command executed successfully");
            }
        }
        vm.detach();
    }

    private static void listCounters(String pid) {
        // Code from JStat (can't call it directly since it does System.exit)
        VmIdentifier vmId = null;
        try {
            vmId = new VmIdentifier(pid);
        } catch (URISyntaxException e) {
            System.err.println("Malformed VM Identifier: " + pid);
            return;
        }
        try {
            MonitoredHost monitoredHost = MonitoredHost.getMonitoredHost(vmId);
            MonitoredVm monitoredVm = monitoredHost.getMonitoredVm(vmId, -1);
            JStatLogger logger = new JStatLogger(monitoredVm);
            logger.printSnapShot("\\w*", // all names
                    new AscendingMonitorComparator(), // comparator
                    false, // not verbose
                    true, // show unsupported
                    System.out);
            monitoredHost.detach(monitoredVm);
        } catch (MonitorException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Class to compare two Monitor objects by name in ascending order.
     * (from jstat)
     */
    static class AscendingMonitorComparator implements Comparator<Monitor> {

        public int compare(Monitor m1, Monitor m2) {
            String name1 = m1.getName();
            String name2 = m2.getName();
            return name1.compareTo(name2);
        }
    }
}
