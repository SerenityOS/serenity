/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 6317435 7110700
 * @summary Verify that stack trace contains a proper cause of
 *          InvalidClassException (methods: checkSerialize,
 *          checkDeserialize or checkDefaultSerialize)
 *
 * @author Andrey Ozerov
 *
 */
import java.io.*;

class NotSerializableObject {
    private String m_str;
    private Integer m_int;

    public NotSerializableObject(String m_str, Integer m_int) {
        this.m_str = m_str;
        this.m_int = m_int;
    }
}

@SuppressWarnings("serial") /* Incorrect declarations are being tested */
class SerializableObject extends NotSerializableObject
    implements Serializable
{
    private static final long serialVersionUID = 1L;

    public SerializableObject(String m_str, Integer m_int) {
        super(m_str, m_int);
    }

    public SerializableObject() {
        super("test", 10);
    }
}

public class ExpectedStackTrace {
    private static final String SER_METHOD_NAME = "checkSerializable";

    public static final void main(String[] args) throws Exception {
        System.err.println("\nRegression test for CRs 6317435, 7110700");
        checkSerializable(getObject());
    }

    private static Object getObject() throws Exception {
        ObjectStreamClass osc =
            ObjectStreamClass.lookup(SerializableObject.class);
        SerializableObject initObj =
            (SerializableObject) osc.forClass().getConstructor().newInstance();
        return initObj;
    }

    private static void checkSerializable(Object initObj) throws Exception {
        try {
            // Serialize to a byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream() ;
            ObjectOutputStream out = new ObjectOutputStream(bos) ;
            out.writeObject(initObj);
            out.close();

            // Get the bytes of the serialized object
            byte[] buf = bos.toByteArray();

            // Deserialize from a byte array
            ByteArrayInputStream bais = new ByteArrayInputStream(buf);
            ObjectInputStream in = new ObjectInputStream(bais);
            SerializableObject finObj = (SerializableObject) in.readObject();
            in.close();
            throw new Error();
        } catch(ObjectStreamException ex) {
            StackTraceElement[] stes = ex.getStackTrace();
            boolean found = false;
            for (int i = 0; i<stes.length-1; i++) {
                StackTraceElement ste = stes[i];
                String nme = ste.getMethodName();
                if (nme.equals(SER_METHOD_NAME)) {
                    found = true;
                }
            }
            if (found) {
                if (ex.getCause() != null) {
                    throw new Error("\nTest for CR 7110700 FAILED");
                }
                System.err.println("\nTEST PASSED");
            } else {
                throw new Error("\nTest for CR 6317435 FAILED");
            }
        }
    }
}
