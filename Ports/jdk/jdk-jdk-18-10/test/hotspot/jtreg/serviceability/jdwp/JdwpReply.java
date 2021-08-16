/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Generic JDWP reply
 */
public abstract class JdwpReply {

    protected final static int HEADER_LEN = 11;
    private byte[] errCode = new byte[2];
    private byte[] data;

    public final void initFromStream(InputStream is) throws IOException {
        DataInputStream ds = new DataInputStream(is);

        int length = ds.readInt();
        int id = ds.readInt();
        byte flags = (byte) ds.read();

        ds.read(errCode, 0, 2);

        int dataLength = length - HEADER_LEN;
        if (dataLength > 0) {
            data = new byte[dataLength];
            int bytesRead = ds.read(data, 0, dataLength);
            // For large data JDWP agent sends two packets: 1011 bytes in
            // the first packet (1000 + HEADER_LEN) and the rest in the
            // second packet.
            if (bytesRead > 0 && bytesRead < dataLength) {
                System.out.println("[" + getClass().getName() + "] Only " +
                        bytesRead + " bytes of " + dataLength + " were " +
                        "read in the first packet. Reading the rest...");
                ds.read(data, bytesRead, dataLength - bytesRead);
            }

            parseData(new DataInputStream(new ByteArrayInputStream(data)));
        }
    }

    protected void parseData(DataInputStream ds) throws IOException {
    }

    protected byte[] readJdwpString(DataInputStream ds) throws IOException {
        byte[] str = null;
        int len = ds.readInt();
        if (len > 0) {
            str = new byte[len];
            ds.read(str, 0, len);
        }
        return str;
    }

    protected long readRefId(DataInputStream ds) throws IOException {
        return ds.readLong();
    }

    public int getErrorCode() {
        return (((errCode[0] & 0xFF) << 8) | (errCode[1] & 0xFF));
    }
}
