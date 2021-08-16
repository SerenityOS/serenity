/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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


/*
 * The Original Code is HAT. The Initial Developer of the
 * Original Code is Bill Foote, with contributions from others
 * at JavaSoft/Sun.
 */

package jdk.test.lib.hprof.parser;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Implementation of ReadBuffer using mapped file buffer
 *
 * @author A. Sundararajan
 */
class MappedReadBuffer implements ReadBuffer {
    private MappedByteBuffer buf;
    private RandomAccessFile file;

    MappedReadBuffer(RandomAccessFile file, MappedByteBuffer buf) {
        this.file = file;
        this.buf = buf;
    }

    /**
     * Factory method to create correct ReadBuffer for a given file.
     *
     * The initial purpose of this method was to choose how to read hprof file for parsing
     * depending on the size of the file and the system property 'jhat.disableFileMap':
     * "If file size is more than 2 GB and when file mapping is configured (default),
     * use mapped file reader".
     *
     * However, it has been discovered a problem with this approach.
     * Creating java.nio.MappedByteBuffer from inside the test leads to hprof file
     * is locked on Windows until test process dies since there is no good way to
     * release this resource.
     *
     * java.nio.MappedByteBuffer will be used only if 'jhat.enableFileMap' is set to true.
     * Per default 'jhat.enableFileMap' is not set.
     */
    static ReadBuffer create(RandomAccessFile file) throws IOException {
        if (canUseFileMap()) {
            MappedByteBuffer buf;
            try {
                FileChannel ch = file.getChannel();
                long size = ch.size();
                buf = ch.map(FileChannel.MapMode.READ_ONLY, 0, size);
                ch.close();
                return new MappedReadBuffer(file, buf);
            } catch (IOException exp) {
                exp.printStackTrace();
                System.err.println("File mapping failed, will use direct read");
                // fall through
            }
        } // else fall through
        return new FileReadBuffer(file);
    }

    /**
     * Set system property 'jhat.enableFileMap' to 'true' to enable file mapping.
     */
    private static boolean canUseFileMap() {
        String prop = System.getProperty("jhat.enableFileMap");
        return prop != null && prop.equals("true");
    }

    private void seek(long pos) throws IOException {
        assert pos <= Integer.MAX_VALUE :  "position overflow";
        buf.position((int)pos);
    }

    @Override
    public synchronized char getChar(long pos) throws IOException {
        seek(pos);
        return buf.getChar();
    }

    @Override
    public synchronized byte getByte(long pos) throws IOException {
        seek(pos);
        return buf.get();
    }

    @Override
    public synchronized short getShort(long pos) throws IOException {
        seek(pos);
        return buf.getShort();
    }

    @Override
    public synchronized int getInt(long pos) throws IOException {
        seek(pos);
        return buf.getInt();
    }

    @Override
    public synchronized long getLong(long pos) throws IOException {
        seek(pos);
        return buf.getLong();
    }

    @Override
    public void close() throws Exception {
        file.close();
    }

}
