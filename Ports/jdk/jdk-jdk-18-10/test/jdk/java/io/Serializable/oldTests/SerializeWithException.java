/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
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
 * @summary it is a new version of an old test which was
 *          /src/share/test/serialization/piotest.java
 *          Test of serialization when there is
 *          exceptions on the I/O stream
 *
 * @build PrimitivesTest
 * @run main SerializeWithException
 */

import java.io.*;

public class SerializeWithException {
   public static void main (String argv[]) {
       System.err.println("\nRegression test for testing of " +
            "serialization when there is exceptions on the I/O stream \n");

       try {
           int i = 123456;
           byte b = 12;
           short s = 45;
           char c = 'A';
           long l = 1234567890000L;
           float f = 3.14159f;
           double d = f*2;
           boolean z = true;
           String string = "The String";
           PrimitivesTest prim = new PrimitivesTest();

           /* For each of the byte offsets from 0 to 100,
              do the pickling but expect an exception */
           for (int offset = 0; offset < 200; offset++) {
               ExceptionOutputStream ostream;
               boolean expect_exception = false;
               IOException exception = null;

               try {
                   expect_exception = true;
                   exception = null;

                   ostream = new ExceptionOutputStream();
                   ostream.setExceptionOffset(offset);
                   ObjectOutputStream p = new ObjectOutputStream(ostream);

                   p.writeInt(i);
                   p.writeByte(b);
                   p.writeShort(s);
                   p.writeChar(c);
                   p.writeLong(l);
                   p.writeFloat(f);
                   p.writeDouble(d);
                   p.writeBoolean(z);
                   p.writeUTF(string);
                   p.writeObject(string);

                   p.writeObject(prim);
                   p.flush();
                   expect_exception = false;
               } catch (IOException ee) {
                   exception = ee;
               }

               if (expect_exception && exception == null) {
                   System.err.println("\nIOException did not occur at " +
                        "offset " + offset);
                   throw new Error();
               }
               if (!expect_exception && exception != null) {
                   System.err.println("\n " + exception.toString() +
                       " not expected at offset " + offset);
                   throw new Error();
               }
           }
           System.err.println("\nTEST PASSED");
       } catch (Exception e) {
           System.err.print("TEST FAILED: ");
           e.printStackTrace();
           throw new Error();
       }
    }
}

class ExceptionOutputStream extends OutputStream {
    private int exceptionOffset = 0;
    private int currentOffset = 0;

    /**
     * Writes a byte to the buffer.
     * @param b the byte
     */
    public void write(int b) throws IOException {
        if (currentOffset >= exceptionOffset) {
            throw new IOException("Debug exception");
        }
        currentOffset++;
    }


    public void setExceptionOffset(int offset) {
        exceptionOffset = offset;
        currentOffset = 0;
    }
}
