/*
 * Copyright (c) 2001, 2012, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4319866
 * @summary Verify that ReliableLog.snapshotSize() returns correct snapshot
 *          file size even if LogHandler doesn't flush.
 *
 * @modules java.rmi/sun.rmi.log
 * @run main/othervm SnapshotSize
 */

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import sun.rmi.log.LogHandler;
import sun.rmi.log.ReliableLog;

public class SnapshotSize extends LogHandler {

    int lastSnapshotSize = -1;

    public static void main(String[] args) throws Exception {
        SnapshotSize handler = new SnapshotSize();
        ReliableLog log = new ReliableLog(".", handler);
        if (log.snapshotSize() != handler.lastSnapshotSize) {
            throw new Error();
        }

        String[] snapshots = { "some", "sample", "objects", "to", "snapshot" };
        for (int i = 0; i < snapshots.length; i++) {
            log.snapshot(snapshots[i]);
            if (log.snapshotSize() != handler.lastSnapshotSize) {
                throw new Error();
            }
        }
    }

    public Object initialSnapshot() {
        return "initial snapshot";
    }

    public void snapshot(OutputStream out, Object value) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(value);
        oout.close();

        byte[] buf = bout.toByteArray();
        out.write(buf);         // leave unflushed
        lastSnapshotSize = buf.length;
    }

    public Object applyUpdate(Object update, Object state) {
        return state;
    }
}
