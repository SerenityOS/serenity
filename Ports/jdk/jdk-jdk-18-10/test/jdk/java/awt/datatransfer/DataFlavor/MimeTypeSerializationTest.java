/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @bug 4116781
  @summary Tests that long (more than 64K) MimeType can be serialized
           and deserialized.
  @author gas@sparc.spb.su area=datatransfer
  @modules java.datatransfer
  @run main MimeTypeSerializationTest
*/

import java.awt.datatransfer.DataFlavor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

public class MimeTypeSerializationTest {

    public static void main(String[] args) throws Exception {
        boolean failed = false;

        try {
            int len = 70000;
            char[] longValue = new char[len];
            Arrays.fill(longValue, 'v');
            DataFlavor longdf = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType +
                "; class=java.lang.String; longParameter=" + new String(longValue));

            DataFlavor shortdf = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType +
                "; class=java.lang.String");

            ByteArrayOutputStream baos = new ByteArrayOutputStream(100000);
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(longdf);
            oos.writeObject(shortdf);
            oos.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            DataFlavor longdf2 = (DataFlavor) ois.readObject();
            DataFlavor shortdf2 = (DataFlavor) ois.readObject();
            ois.close();

            failed = !( longdf.getMimeType().equals(longdf2.getMimeType()) &&
                shortdf.getMimeType().equals(shortdf2.getMimeType()) );
            if (failed) {
                System.err.println("deserialized MIME type does not match original one");
            }
        } catch (IOException e) {
            failed = true;
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (failed) {
            throw new RuntimeException("test failed: serialization attempt failed");
        } else {
            System.err.println("test passed");
        }
    }
}
