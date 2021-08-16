/*
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
 *
 *  (C) Copyright IBM Corp. 1999 All Rights Reserved.
 *  Copyright 1997 The Open Group Research Institute.  All rights reserved.
 */

package sun.security.krb5.internal.util;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This class implements a buffered output stream. It provides methods to write a chunck of
 * bytes to underlying data stream.
 *
 * @author Yanni Zhang
 *
 */
public class KrbDataOutputStream extends BufferedOutputStream {
    public KrbDataOutputStream(OutputStream os) {
        super(os);
    }
    public void write32(int num) throws IOException {
        byte[] bytes = new byte[4];
        bytes[0] = (byte)((num & 0xff000000) >> 24 & 0xff);
        bytes[1] = (byte)((num & 0x00ff0000) >> 16 & 0xff);
        bytes[2] = (byte)((num & 0x0000ff00) >> 8 & 0xff);
        bytes[3] = (byte)(num & 0xff);
        write(bytes, 0, 4);
    }

    public void write16(int num) throws IOException {
        byte[] bytes = new byte[2];
        bytes[0] = (byte)((num & 0xff00) >> 8 & 0xff);
        bytes[1] = (byte)(num & 0xff);
        write(bytes, 0, 2);
    }

    public void write8(int num) throws IOException {
        write(num & 0xff);
    }
}
