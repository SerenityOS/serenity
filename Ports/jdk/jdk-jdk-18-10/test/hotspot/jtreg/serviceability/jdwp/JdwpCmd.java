/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Generic JDWP command
 * @param <T> the corresponding JDWP reply class, to construct a reply object
 */
public abstract class JdwpCmd<T extends JdwpReply> {

    private ByteBuffer data;
    private static int id = 1;
    private final byte FLAGS = 0;
    private T reply;
    private final int dataLen;
    private final int HEADER_LEN = 11;

    /**
     * JDWWp command
     * @param cmd command code
     * @param cmdSet command set
     * @param replyClz command reply class
     * @param dataLen length of additional data for the command
     */
    JdwpCmd(int cmd, int cmdSet, Class<T> replyClz, int dataLen) {
        this.dataLen = dataLen;
        data = ByteBuffer.allocate(HEADER_LEN + dataLen);
        data.putInt(HEADER_LEN + dataLen);
        data.putInt(id++);
        data.put(FLAGS);
        data.put((byte) cmdSet);
        data.put((byte) cmd);
        if (replyClz != null) {
            try {
                reply = replyClz.newInstance();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    JdwpCmd(int cmd, int cmdSet, Class<T> replyClz) {
        this(cmd, cmdSet, replyClz, 0);
    }

    int getDataLength() {
        return dataLen;
    }

    public final T send(JdwpChannel channel) throws IOException {
        channel.write(data.array(), HEADER_LEN + getDataLength());
        if (reply != null) {
            reply.initFromStream(channel.getInputStream());
        }
        return (T) reply;
    }

    protected void putRefId(long refId) {
        data.putLong(refId);
    }

    protected void putInt(int val) {
        data.putInt(val);
    }

    protected static int refLen() {
        return 8;
    }

}
