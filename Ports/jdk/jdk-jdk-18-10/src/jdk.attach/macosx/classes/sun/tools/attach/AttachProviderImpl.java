/*
 * Copyright (c) 2005, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.sun.tools.attach.AttachNotSupportedException;
import java.io.IOException;

/*
 * An AttachProvider implementation for Bsd that uses a UNIX domain
 * socket.
 */
public class AttachProviderImpl extends HotSpotAttachProvider {

    public AttachProviderImpl() {
    }

    public String name() {
        return "sun";
    }

    public String type() {
        return "socket";
    }

    public VirtualMachine attachVirtualMachine(String vmid)
        throws AttachNotSupportedException, IOException
    {
        checkAttachPermission();

        // AttachNotSupportedException will be thrown if the target VM can be determined
        // to be not attachable.
        testAttachable(vmid);

        return new VirtualMachineImpl(this, vmid);
    }

    public VirtualMachine attachVirtualMachine(VirtualMachineDescriptor vmd)
        throws AttachNotSupportedException, IOException
    {
        if (vmd.provider() != this) {
            throw new AttachNotSupportedException("provider mismatch");
        }
        // To avoid re-checking if the VM if attachable, we check if the descriptor
        // is for a hotspot VM - these descriptors are created by the listVirtualMachines
        // implementation which only returns a list of attachable VMs.
        if (vmd instanceof HotSpotVirtualMachineDescriptor) {
            assert ((HotSpotVirtualMachineDescriptor)vmd).isAttachable();
            checkAttachPermission();
            return new VirtualMachineImpl(this, vmd.id());
        } else {
            return attachVirtualMachine(vmd.id());
        }
    }

}
