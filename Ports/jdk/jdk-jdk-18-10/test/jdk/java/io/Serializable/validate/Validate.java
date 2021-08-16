/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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
@bug 4094892
@summary Verify that an object is not validated more than once during deserialization.

*/

import java.io.*;
import java.util.Date;

public class Validate {
    public static void main(String[] args) throws Exception {
        try {
            // Write class out
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(baos);

            Class1 c1 = new Class1(11, 22);
            out.writeObject(c1);
            out.writeObject(new Class1(22,33));
            out.writeObject(new Date());
            out.writeObject(new Date());
            out.writeObject(new Date());
            out.writeObject(new Date());
            out.flush();
            out.close();

            // Read it back
            ByteArrayInputStream bais =
                new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream in = new ObjectInputStream(bais);
            Class1 cc1 = (Class1) in.readObject();
            Class1 cc2 = (Class1) in.readObject();
            System.out.println("date: " + in.readObject());
            System.out.println("date: " + in.readObject());
            System.out.println("date: " + in.readObject());
            System.out.println("date: " + in.readObject());
            in.close();

            System.out.println(cc1.a + " " + cc1.b);
            System.out.println(cc2.a + " " + cc2.b);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw e;
        }
    }
}

class Class1 implements Serializable, ObjectInputValidation {
    private static final long serialVersionUID = 1L;

    int a, b;
    transient int validates;

    public Class1(int aa, int bb) {
        a = aa;
        b = bb;
    }
    public void validateObject() throws InvalidObjectException {
        if (validates > 0)
            throw new Error("Implementation error: Re-validating object " + this.toString());
        validates++;

        System.out.println("Validating " + this.toString());
        if (a > b) {
            throw new InvalidObjectException("Fields cannot be negative");
        }
    }
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
            in.registerValidation(this, 1);
            in.defaultReadObject();
    }
}
