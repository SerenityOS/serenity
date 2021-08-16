/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * Copyright (c) 2011-2012, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tck.java.time;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamConstants;
import java.io.Serializable;
import java.util.Formatter;
import java.util.Map;

/**
 * Base test class.
 */
public abstract class AbstractTCKTest {

    /**
     * Map from package name to the serialVersionUID of the .Ser class for the package.
     */
    private static Map<String, Long> serialVersionUIDs = Map.of(
                "java.time",        -7683839454370182990L,
                "java.time.chrono", -6103370247208168577L,
                "java.time.zone",   -8885321777449118786L
                );

    /**
     * Returns the serialVersionUID for the class.
     * The SUIDs are defined by the specification for each class.
     * @param serClass the class to return the SUID of
     * @return returns the serialVersionUID for the class
     */
    public final static long getSUID(Class<?> serClass) {
        String pkgName = serClass.getPackageName();
        return serialVersionUIDs.get(pkgName);
    }


    protected static boolean isIsoLeap(long year) {
        if (year % 4 != 0) {
            return false;
        }
        if (year % 100 == 0 && year % 400 != 0) {
            return false;
        }
        return true;
    }

    protected static void assertSerializable(Object object) throws IOException, ClassNotFoundException {
        assertEquals(object instanceof Serializable, true);
        Object deserializedObject = writeThenRead(object);
        assertEquals(deserializedObject, object);
    }

    protected static void assertSerializableSame(Object object) throws IOException, ClassNotFoundException {
        assertEquals(object instanceof Serializable, true);
        Object deserializedObject = writeThenRead(object);
        assertSame(deserializedObject, object);
    }

    private static Object writeThenRead(Object object) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos) ) {
            oos.writeObject(object);
        }
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            return ois.readObject();
        }
    }

    protected static void assertSerializedBySer(Object object, byte[] expectedBytes, byte[]... matches) throws Exception {
        String serClass = object.getClass().getPackage().getName() + ".Ser";
        long serVer = getSUID(object.getClass());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos) ) {
            oos.writeObject(object);
        }
        byte[] bytes = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        try (DataInputStream dis = new DataInputStream(bais)) {
            assertEquals(dis.readShort(), ObjectStreamConstants.STREAM_MAGIC);
            assertEquals(dis.readShort(), ObjectStreamConstants.STREAM_VERSION);
            assertEquals(dis.readByte(), ObjectStreamConstants.TC_OBJECT);
            assertEquals(dis.readByte(), ObjectStreamConstants.TC_CLASSDESC);
            assertEquals(dis.readUTF(), serClass);
            assertEquals(dis.readLong(), serVer);
            assertEquals(dis.readByte(), ObjectStreamConstants.SC_EXTERNALIZABLE | ObjectStreamConstants.SC_BLOCK_DATA);
            assertEquals(dis.readShort(), 0);  // number of fields
            assertEquals(dis.readByte(), ObjectStreamConstants.TC_ENDBLOCKDATA);  // end of classdesc
            assertEquals(dis.readByte(), ObjectStreamConstants.TC_NULL);  // no superclasses
            if (expectedBytes.length < 256) {
                assertEquals(dis.readByte(), ObjectStreamConstants.TC_BLOCKDATA);
                assertEquals(dis.readUnsignedByte(), expectedBytes.length, "blockdata length incorrect");
            } else {
                assertEquals(dis.readByte(), ObjectStreamConstants.TC_BLOCKDATALONG);
                assertEquals(dis.readInt(), expectedBytes.length, "blockdatalong length incorrect");
            }
            byte[] input = new byte[expectedBytes.length];
            dis.readFully(input);
            assertEquals(input, expectedBytes);
            if (matches.length > 0) {
                for (byte[] match : matches) {
                    boolean matched = false;
                    while (matched == false) {
                        try {
                            dis.mark(1000);
                            byte[] possible = new byte[match.length];
                            dis.readFully(possible);
                            assertEquals(possible, match);
                            matched = true;
                        } catch (AssertionError ex) {
                            dis.reset();
                            dis.readByte();  // ignore
                        }
                    }
                }
            } else {
                assertEquals(dis.readByte(), ObjectStreamConstants.TC_ENDBLOCKDATA);  // end of blockdata
                assertEquals(dis.read(), -1);
            }
        }
    }

    /**
     * Verify the class cannot be deserialized from a handcoded stream.
     * Fail if the deserialization does <em>not</em> throw an Exception.
     * @param serClass the class to embed in the handcoded stream
     * @throws Exception if an unexpected condition occurs
     */
    protected static void assertNotSerializable(Class<?> serClass) throws Exception {
        long serVer = getSUID(serClass);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeShort(ObjectStreamConstants.STREAM_MAGIC);
            out.writeShort(ObjectStreamConstants.STREAM_VERSION);
            out.writeByte(ObjectStreamConstants.TC_OBJECT);
            out.writeByte(ObjectStreamConstants.TC_CLASSDESC);
            out.writeUTF(serClass.getName());
            out.writeLong(serVer);
            out.writeByte(ObjectStreamConstants.SC_SERIALIZABLE);   // Flags ObjectStreamConstants
            out.writeShort(0);  // number of fields
            out.writeByte(ObjectStreamConstants.TC_ENDBLOCKDATA);
            out.writeByte(ObjectStreamConstants.TC_NULL);  // no superclasses
        }

        byte[] bytes = baos.toByteArray();

        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(bis)) {
            Object o = in.readObject();
        } catch (Exception ioe) {
            // Expected exception
            return;
        }
        fail("Class should not be deserializable " + serClass.getName());
    }

    /**
     * Utility method to dump a byte array in a java syntax.
     * @param bytes and array of bytes
     * @return a string containing the bytes formatted in java syntax
     */
    protected static String dumpSerialStream(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 5);
        Formatter fmt = new Formatter(sb);
        fmt.format("    byte[] bytes = {" );
        final int linelen = 10;
        for (int i = 0; i < bytes.length; i++) {
            if (i % linelen == 0) {
                fmt.format("%n        ");
            }
            fmt.format(" %3d,", bytes[i] & 0xff);
            if ((i % linelen) == (linelen-1) || i == bytes.length - 1) {
                fmt.format("  /*");
                int s = i / linelen * linelen;
                int k = i % linelen;
                for (int j = 0; j <= k && s + j < bytes.length; j++) {
                    fmt.format(" %c", bytes[s + j] & 0xff);
                }
                fmt.format(" */");
            }
        }
        fmt.format("%n    };%n");
        return sb.toString();
    }
}
