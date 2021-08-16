/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

import  java.io.*;

/*
 * Copyright 2003 Wily Technology, Inc.
 */

public class NamedBuffer
{
    private final String    fName;
    private final byte[]    fBuffer;

    public
    NamedBuffer(    String  name,
                    byte[]  buffer)
        {
        fName =     name;
        fBuffer =   buffer;
        }

    public
    NamedBuffer(    String      name,
                    InputStream stream)
        throws IOException
        {
        this(   name,
                loadBufferFromStream(stream));
        }

    public String
    getName()
        {
        return fName;
        }

    public byte[]
    getBuffer()
        {
        return fBuffer;
        }

    public static byte[]
    loadBufferFromStream(InputStream stream)
        throws IOException
        {
        // hack for now, just assume the stream will fit in our reasonable size buffer.
        // if not, panic
        int bufferLimit = 200 * 1024;
        byte[]  readBuffer = new byte[bufferLimit];
        int actualSize = stream.read(readBuffer);
        if ( actualSize >= bufferLimit )
            {
            // if there might be more bytes, just surrender
            throw new IOException("too big for buffer");
            }

        byte[] resultBuffer = new byte[actualSize];
        System.arraycopy(   readBuffer,
                            0,
                            resultBuffer,
                            0,
                            actualSize);
        return resultBuffer;
        }
}
