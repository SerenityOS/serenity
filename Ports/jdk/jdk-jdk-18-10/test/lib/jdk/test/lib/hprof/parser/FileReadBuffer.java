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

/**
 * Implementation of ReadBuffer using a RandomAccessFile
 *
 * @author A. Sundararajan
 */
class FileReadBuffer implements ReadBuffer {
    // underlying file to read
    private RandomAccessFile file;

    FileReadBuffer(RandomAccessFile file) {
        this.file = file;
    }

    private void seek(long pos) throws IOException {
        file.getChannel().position(pos);
    }

    public synchronized void get(long pos, byte[] buf) throws IOException {
        seek(pos);
        file.read(buf);
    }

    @Override
    public synchronized char getChar(long pos) throws IOException {
        seek(pos);
        return file.readChar();
    }

    @Override
    public synchronized byte getByte(long pos) throws IOException {
        seek(pos);
        return (byte) file.read();
    }

    @Override
    public synchronized short getShort(long pos) throws IOException {
        seek(pos);
        return file.readShort();
    }

    @Override
    public synchronized int getInt(long pos) throws IOException {
        seek(pos);
        return file.readInt();
    }

    @Override
    public synchronized long getLong(long pos) throws IOException {
        seek(pos);
        return file.readLong();
    }

    @Override
    public void close() throws Exception {
        file.close();
    }
}
