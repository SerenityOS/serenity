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
 * @summary it is new version of old test which was
 *          /src/share/test/serialization/piotest.java
 *          Test of serialization/deserialization of
 *          objects with CircularListType types
 */

import java.io.*;

public class CircularList {
   public static void main (String argv[]) throws IOException {
       System.err.println("\nRegression test for testing of " +
            "serialization/deserialization of " +
            "objects with CirculalListType types \n");

       FileInputStream istream = null;
       FileOutputStream ostream = null;
       try {
           ostream = new FileOutputStream("piotest7.tmp");
           ObjectOutputStream p = new ObjectOutputStream(ostream);

           CircularListTest.setup();
           p.writeObject(CircularListTest.list);
           p.flush();

           istream = new FileInputStream("piotest7.tmp");
           ObjectInputStream q = new ObjectInputStream(istream);

           CircularListTest cv = (CircularListTest)q.readObject();
           if (cv != cv.next) {
               System.err.println("\nTEST FAILED: " +
                    "Circular List Test failed, next != self");
               throw new Error();
           }
           System.err.println("\nTEST PASSED");
       } catch (Exception e) {
           System.err.print("TEST FAILED: ");
           e.printStackTrace();
           throw new Error();
        } finally {
           if (istream != null) istream.close();
           if (ostream != null) ostream.close();
        }
    }
}

class CircularListTest implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public CircularListTest next = null;
    public static CircularListTest list = null;

    public static void setup() {
        list = new CircularListTest();
        list.next = list;
    }
}
