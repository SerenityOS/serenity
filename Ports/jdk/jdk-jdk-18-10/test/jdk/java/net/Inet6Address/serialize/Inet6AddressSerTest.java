/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamConstants;
import static java.io.ObjectStreamConstants.STREAM_MAGIC;
import static java.io.ObjectStreamConstants.TC_CLASSDESC;
import static java.io.ObjectStreamConstants.TC_ENDBLOCKDATA;
import static java.io.ObjectStreamConstants.TC_NULL;
import static java.io.ObjectStreamConstants.TC_OBJECT;
import java.net.Inet6Address;
/**
 * @test
 * @bug 8194676
 * @summary NullPointerException is thrown if ipaddress is not set.
 * @run main Inet6AddressSerTest
 */
public class Inet6AddressSerTest implements ObjectStreamConstants {

    static class PayloadTest {

        private static byte[] serialize(String className) throws IOException {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos)) {
                // stream headers
                dos.writeShort(STREAM_MAGIC);
                dos.writeShort(5); // version

                // Inet6Address
                dos.writeByte(TC_OBJECT);
                dos.writeByte(TC_CLASSDESC);
                dos.writeUTF(className);
                dos.writeLong(6880410070516793377L);
                dos.writeByte(2);
                dos.writeShort(0);
                dos.writeByte(TC_ENDBLOCKDATA);
                dos.writeByte(TC_NULL);
                return baos.toByteArray();
            }
        }

        private static Object deserialize(final byte[] buffer)
                throws IOException, ClassNotFoundException {
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(buffer))) {
                return ois.readObject();
            }
        }

        void test(String className) throws IOException, ClassNotFoundException {
            deserialize(serialize(className));
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        try {
            new PayloadTest().test(Inet6Address.class.getName());
            throw new RuntimeException("Expected exception not raised");
        } catch (InvalidObjectException ioe) {
            if (ioe.getMessage().contains("invalid address length")) {
                System.out.println(String.format("Got expected exception: %s", ioe));
            } else {
                throw new RuntimeException("Expected exception not raised");
            }
        } catch (RuntimeException re) {
            throw new RuntimeException("Expected exception not raised");
        }
    }
}
