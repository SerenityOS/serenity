/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.sun.org.apache.xml.internal.security.utils;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class UnsyncBufferedOutputStream extends FilterOutputStream {

    protected byte[] buffer;
    protected int count;

    public UnsyncBufferedOutputStream(OutputStream out) {
        super(out);
        buffer = new byte[8192];
    }

    public UnsyncBufferedOutputStream(OutputStream out, int size) {
        super(out);
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }
        buffer = new byte[size];
    }

    @Override
    public void flush() throws IOException {
        flushInternal();
        out.flush();
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
        if (length >= buffer.length) {
            flushInternal();
            out.write(bytes, offset, length);
            return;
        }

        // flush the internal buffer first if we have not enough space left
        if (length >= (buffer.length - count)) {
            flushInternal();
        }

        // the length is always less than (internalBuffer.length - count) here so arraycopy is safe
        System.arraycopy(bytes, offset, buffer, count, length);
        count += length;
    }

    @Override
    public void write(int oneByte) throws IOException {
        if (count == buffer.length) {
            out.write(buffer, 0, count);
            count = 0;
        }
        buffer[count++] = (byte) oneByte;
    }

    private void flushInternal() throws IOException {
        if (count > 0) {
            out.write(buffer, 0, count);
            count = 0;
        }
    }
}
