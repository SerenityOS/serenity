/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StubClob implements Clob {

    public String buf = "The test string 0123456789";

    @Override
    public String getSubString(long pos, int length) throws SQLException {
        return buf;
    }

    @Override
    public long length() throws SQLException {
        return buf.length();
    }

    @Override
    public Reader getCharacterStream() throws SQLException {
        return new StringReader(buf);
    }

    @Override
    public InputStream getAsciiStream() throws SQLException {
        return new java.io.StringBufferInputStream(buf);
    }

    @Override
    public int setString(long pos, String str) throws SQLException {
        return str.length();
    }

    @Override
    public int setString(long pos, String str, int offset, int len) throws SQLException {
        return len;
    }

    @Override
    public long position(String searchstr, long start) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long position(Clob searchstr, long start) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public OutputStream setAsciiStream(long pos) throws SQLException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(baos);
        } catch (IOException ex) {
            Logger.getLogger(StubBlob.class.getName()).log(Level.SEVERE, null, ex);
        }
        return oos;
    }

    @Override
    public Writer setCharacterStream(long pos) throws SQLException {
        return new StringWriter();
    }

    @Override
    public void truncate(long len) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void free() throws SQLException {
    }

    @Override
    public Reader getCharacterStream(long pos, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
