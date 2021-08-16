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
 * @summary it is new version of old test which was under
 *          /src/share/test/serialization/psiotest.java
 *          Test validation callbacks
 */

import java.io.*;

public class ValidateClass {
    public static void main (String argv[]) {
        System.err.println("\nRegression test for validation of callbacks \n");

        FileInputStream istream = null;
        try {

            FileOutputStream ostream = new FileOutputStream("psiotest4.tmp");
            ObjectOutputStream p = new ObjectOutputStream(ostream);

            /* Catch the expected exception and
             * complain if it does not occur.
             */

            // Serialize a bunch of objects that will be validated when read
            // Make a list of classes with intermingled priorities
            Validator vc = new Validator(0, null);
            vc = new Validator(2, vc);
            vc = new Validator(0, vc);
            vc = new Validator(3, vc);
            vc = new Validator(Integer.MIN_VALUE, vc);
            vc = new Validator(1, vc);
            vc = new Validator(1, vc);
            vc = new Validator(0, vc);

            p.writeObject(vc);
            p.flush();
            ostream.close();

            istream = new FileInputStream("psiotest4.tmp");
            ObjectInputStream q = new ObjectInputStream(istream);

            Validator vc_u;

            vc_u = (Validator)q.readObject();
            if (Validator.validated != Integer.MIN_VALUE) {
                System.err.println("\nTEST FAILED: Validation callbacks did " +
                    "not complete.");
                throw new Error();
            }
            istream.close();
            System.err.println("\nTEST PASSED");
        } catch (Exception e) {
            System.err.print("TEST FAILED: ");
            e.printStackTrace();
            throw new Error();
        }
    }
}

class MissingWriterClass implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    int i = 77;

    private void writeObject(ObjectOutputStream pw) throws IOException {
        pw.writeInt(i);
    }
}

class MissingReaderClass implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    int i = 77;

    private void readObject(ObjectInputStream pr) throws IOException {
        i = pr.readInt();
    }
}


class Validator implements ObjectInputValidation, java.io.Serializable  {
    private static final long serialVersionUID = 1L;

    static int validated = Integer.MAX_VALUE; // Last value validated
    int priority;
    Validator next = null;

    public Validator(int prio, Validator n) {
        priority = prio;
        next = n;
    }

    // Handle serialization/deserialization
    private void writeObject(ObjectOutputStream pw) throws IOException {
        pw.writeInt(priority);
        pw.writeObject(next);
    }

    private void readObject(ObjectInputStream pr)
        throws IOException, ClassNotFoundException
    {
        priority = pr.readInt();
        next = (Validator)pr.readObject();

        pr.registerValidation(this, priority);
    }

    public void validateObject() throws InvalidObjectException {
        if (validated < priority) {
            System.err.println("\nTEST FAILED: Validations called out " +
                "of order: Previous priority: " + validated + " < " +
                "new priority: " + priority);
            throw new Error();
        }
        validated = priority;
    }
}
