/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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
package sun.tools.attach;

import com.sun.tools.attach.VirtualMachineDescriptor;
import com.sun.tools.attach.AttachPermission;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.spi.AttachProvider;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import sun.jvmstat.monitor.HostIdentifier;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.MonitoredVmUtil;
import sun.jvmstat.monitor.VmIdentifier;

/*
 * Platform specific provider implementations extend this
 */
public abstract class HotSpotAttachProvider extends AttachProvider {

    public HotSpotAttachProvider() {
    }

    public void checkAttachPermission() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(
                new AttachPermission("attachVirtualMachine")
            );
        }
    }

    /*
     * This listVirtualMachines implementation is based on jvmstat. Can override
     * this in platform implementations when there is a more efficient mechanism
     * available.
     */
    public List<VirtualMachineDescriptor> listVirtualMachines() {
        ArrayList<VirtualMachineDescriptor> result =
            new ArrayList<VirtualMachineDescriptor>();

        MonitoredHost host;
        Set<Integer> vms;
        try {
            host = MonitoredHost.getMonitoredHost(new HostIdentifier((String)null));
            vms = host.activeVms();
        } catch (Throwable t) {
            if (t instanceof ExceptionInInitializerError) {
                t = t.getCause();
            }
            if (t instanceof ThreadDeath) {
                throw (ThreadDeath)t;
            }
            if (t instanceof SecurityException) {
                return result;
            }
            throw new InternalError(t);          // shouldn't happen
        }

        for (Integer vmid: vms) {
            String pid = vmid.toString();
            String name = pid;      // default to pid if name not available
            boolean isAttachable = false;
            MonitoredVm mvm = null;
            try {
                mvm = host.getMonitoredVm(new VmIdentifier(pid));
                try {
                    isAttachable = MonitoredVmUtil.isAttachable(mvm);
                    // use the command line as the display name
                    name =  MonitoredVmUtil.commandLine(mvm);
                } catch (Exception e) {
                }
                if (isAttachable) {
                    result.add(new HotSpotVirtualMachineDescriptor(this, pid, name));
                }
            } catch (Throwable t) {
                if (t instanceof ThreadDeath) {
                    throw (ThreadDeath)t;
                }
            } finally {
                if (mvm != null) {
                    mvm.detach();
                }
            }
        }
        return result;
    }

    /**
     * Test if a VM is attachable. If it's not attachable,
     * an AttachNotSupportedException will be thrown. For example,
     * 1.4.2 or 5.0 VM are not attachable. There are cases that
     * we can't determine if a VM is attachable or not and this method
     * will just return.
     *
     * This method uses the jvmstat counter to determine if a VM
     * is attachable. If the target VM does not have a jvmstat
     * share memory buffer, this method returns.
     *
     * @exception AttachNotSupportedException if it's not attachable
     */
    void testAttachable(String id) throws AttachNotSupportedException {
        MonitoredVm mvm = null;
        try {
            VmIdentifier vmid = new VmIdentifier(id);
            MonitoredHost host = MonitoredHost.getMonitoredHost(vmid);
            mvm = host.getMonitoredVm(vmid);

            if (MonitoredVmUtil.isAttachable(mvm)) {
                // it's attachable; so return false
                return;
            }
        } catch (Throwable t) {
            if (t instanceof ThreadDeath) {
                ThreadDeath td = (ThreadDeath)t;
                throw td;
            }
            // we do not know what this id is
            return;
        } finally {
            if (mvm != null) {
                mvm.detach();
            }
        }

        // we're sure it's not attachable; throw exception
        throw new AttachNotSupportedException(
                  "The VM does not support the attach mechanism");
    }


    /**
     * A virtual machine descriptor to describe a HotSpot virtual machine.
     */
    static class HotSpotVirtualMachineDescriptor extends VirtualMachineDescriptor {
        HotSpotVirtualMachineDescriptor(AttachProvider provider,
                                        String id,
                                        String displayName) {
            super(provider, id, displayName);
        }

        public boolean isAttachable() {
            return true;
        }
    }
}
