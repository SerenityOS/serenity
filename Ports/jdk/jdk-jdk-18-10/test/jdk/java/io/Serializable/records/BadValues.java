/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @summary Basic test for ClassNotFoundException
 * @run testng BadValues
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import org.testng.annotations.Test;
import static java.io.ObjectStreamConstants.*;
import static java.lang.System.out;
import static org.testng.Assert.*;

/**
 * Not directly related to records but provokes surrounding code, and ensures
 * that the record changes don't break anything.
 */
public class BadValues {

    /**
     * Returns a stream of bytes for the given class, uid, and flags. The
     * stream will have no stream field values.
     */
    static byte[] byteStreamFor(String className, long uid, byte flags)
        throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeShort(STREAM_MAGIC);
        dos.writeShort(STREAM_VERSION);
        dos.writeByte(TC_OBJECT);
        dos.writeByte(TC_CLASSDESC);
        dos.writeUTF(className);
        dos.writeLong(uid);
        dos.writeByte(flags);
        dos.writeShort(0);                // number of fields
        dos.writeByte(TC_ENDBLOCKDATA);   // no annotations
        dos.writeByte(TC_NULL);           // no superclasses
        dos.write(TC_ENDBLOCKDATA);       // end block - for SC_WRITE_METHOD
        dos.close();
        return baos.toByteArray();
    }

    static final Class<ClassNotFoundException> CNFE = ClassNotFoundException.class;

    @Test
    public void testNotFoundSer() throws Exception {
        out.println("\n---");
        byte[] bytes = byteStreamFor("XxYyZz", 0L, (byte)SC_SERIALIZABLE);
        Throwable t = expectThrows(CNFE, () -> deserialize(bytes));
        out.println("caught expected CNFE: " + t);
    }

    @Test
    public void testNotFoundSerWr() throws Exception {
        out.println("\n---");
        byte[] bytes = byteStreamFor("XxYyZz", 0L, (byte)(SC_SERIALIZABLE | SC_WRITE_METHOD));
        Throwable t = expectThrows(CNFE, () -> deserialize(bytes));
        out.println("caught expected CNFE: " + t);
    }

    @Test
    public void testNotFoundExt() throws Exception {
        out.println("\n---");
        byte[] bytes = byteStreamFor("AaBbCc", 0L, (byte)SC_EXTERNALIZABLE);
        Throwable t = expectThrows(CNFE, () -> deserialize(bytes));
        out.println("caught expected CNFE: " + t);
    }

    @Test
    public void testNotFoundExtWr() throws Exception {
        out.println("\n---");
        byte[] bytes = byteStreamFor("AaBbCc", 0L, (byte)(SC_SERIALIZABLE | SC_WRITE_METHOD));
        Throwable t = expectThrows(CNFE, () -> deserialize(bytes));
        out.println("caught expected CNFE: " + t);
    }

    @SuppressWarnings("unchecked")
    static <T> T deserialize(byte[] streamBytes)
        throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(streamBytes);
        ObjectInputStream ois  = new ObjectInputStream(bais);
        return (T) ois.readObject();
    }
}
