/*
 * Copyright (c) 2004, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvmstat.perfdata.monitor.protocol.file;

import sun.jvmstat.monitor.*;
import sun.jvmstat.monitor.event.VmListener;
import sun.jvmstat.perfdata.monitor.*;
import java.util.List;
import java.lang.reflect.*;
import java.io.*;

/**
 * Concrete implementation of the AbstractMonitoredVm class for the
 * <em>file:</em> protocol for the HotSpot PerfData monitoring implementation.
 * <p>
 * This class provides the ability to attach to the instrumentation buffer
 * (saved or live) of a target Java Virtual Machine by providing a
 * <em>file</em> URI to a file containing the instrmentation buffer data.
 *
 * @author Brian Doherty
 * @since 1.5
 */
public class FileMonitoredVm extends AbstractMonitoredVm {

    /**
     * Create a FileMonitoredVm instance.
     *
     * @param vmid the vm identifier referring to the file
     * @param interval sampling interval (unused in this protocol).
     */
    public FileMonitoredVm(VmIdentifier vmid, int interval)
           throws MonitorException {
        super(vmid, interval);
        this.pdb = new PerfDataBuffer(vmid);
    }

    /**
     * {@inheritDoc}.
     *<p>
     * Note - the <em>file:</em> protocol currently does not support
     * the registration or notification of listeners.
     */
    public void addVmListener(VmListener l) { }

    /**
     * {@inheritDoc}.
     *<p>
     * Note - the <em>file:</em> protocol currently does not support
     * the registration or notification of listeners.
     */
    public void removeVmListener(VmListener l) { }
}
