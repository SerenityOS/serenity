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
 * @bug 6313687
 * @summary Verify that if the class descriptor for an ordinary object is the
 *          descriptor for an array class, an ObjectStreamException is thrown.
 *
 */

import java.io.*;

class TestArray implements Serializable {
    private static final long serialVersionUID = 1L;

    // size of array
    private static final int ARR_SIZE = 5;
    // serializable field
    private String[] strArr = new String[ARR_SIZE];

    public TestArray() {
        for (int i = 0; i < ARR_SIZE; i++) {
            strArr[i] = "test" + i;
        }
    }
}

public class MisplacedArrayClassDesc {
    public static final void main(String[] args) throws Exception {
       System.err.println("\nRegression test for CR6313687");
       TestArray object = new TestArray();
       try {
           // Serialize to a byte array
           ByteArrayOutputStream bos = new ByteArrayOutputStream() ;
           ObjectOutputStream out = new ObjectOutputStream(bos) ;
           out.writeObject(object);
           out.close();

           // Get the bytes of the serialized object
           byte[] buf = bos.toByteArray();
           for (int i = 0; i < buf.length; i++) {
               if (buf[i] == ObjectOutputStream.TC_ARRAY) {
                   buf[i] = ObjectOutputStream.TC_OBJECT;
                   break;
               }
           }

           // Deserialize from a byte array
           ByteArrayInputStream bais = new ByteArrayInputStream(buf);
           ObjectInputStream in = new ObjectInputStream(bais);
           TestArray ta = (TestArray) in.readObject();
           in.close();
       } catch (InstantiationError e) {
            throw new Error();
       } catch (InvalidClassException e) {
            System.err.println("\nTest passed");
            return;
       }
       throw new Error();
    }
}
