/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
import sun.jvmstat.perfdata.monitor.*;
import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * The concrete PerfDataBuffer implementation for the <em>file:</em>
 * protocol for the HotSpot PerfData monitoring implemetation.
 * <p>
 * This class is responsible for acquiring access to the instrumentation
 * buffer stored in a file referenced by a file URI.
 *
 * @author Brian Doherty
 * @since 1.5
 */
public class PerfDataBuffer extends AbstractPerfDataBuffer {

    /**
     * Create a PerfDataBuffer instance for accessing the specified
     * instrumentation buffer.
     *
     * @param vmid the <em>file:</em> URI to the instrumentation buffer file
     *
     * @throws MonitorException
     */
    public PerfDataBuffer(VmIdentifier vmid) throws MonitorException {
        File f = new File(vmid.getURI());
        String mode = vmid.getMode();

        try {
            FileChannel fc = new RandomAccessFile(f, mode).getChannel();
            ByteBuffer bb = null;

            if (mode.compareTo("r") == 0) {
                bb = fc.map(FileChannel.MapMode.READ_ONLY, 0L, (int)fc.size());
            } else if (mode.compareTo("rw") == 0) {
                bb = fc.map(FileChannel.MapMode.READ_WRITE, 0L, (int)fc.size());
            } else {
                throw new IllegalArgumentException("Invalid mode: " + mode);
            }

            fc.close();               // doesn't need to remain open

            createPerfDataBuffer(bb, 0);
        } catch (FileNotFoundException e) {
            throw new MonitorException("Could not find " + vmid.toString());
        } catch (IOException e) {
            throw new MonitorException("Could not read " + vmid.toString());
        }
    }
}
