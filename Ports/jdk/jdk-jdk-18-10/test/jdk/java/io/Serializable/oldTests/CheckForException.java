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
 *          /src/share/test/serialization/psiotest.java
 *          Test pickling and unpickling an object with derived classes
 *          and using a read special to serialize the "middle" class,
 *          which raises NotSerializableException inside writeObject()
 *          and readObject() methods.
 */

import java.io.*;

public class CheckForException {
    public static void main (String argv[]) {
        System.err.println("\nRegression test of " +
                           "serialization/deserialization of " +
                           "complex objects which raise " +
                           "NotSerializableException inside " +
                           "writeObject() and readObject() methods.\n");


        FileInputStream istream = null;
        try {

            FileOutputStream ostream = new FileOutputStream("psiotest3.tmp");
            ObjectOutputStream p = new ObjectOutputStream(ostream);

            /* Catch the expected exception and
             * complain if it does not occur.
             */
            TryPickleClass npc = new TryPickleClass();

            NotSerializableException we = null;
            try {
                // Two objects of the same class that are used
                // in cleanup test below
                p.writeObject("test");
                p.writeObject("test2");
                p.writeObject(npc);
            } catch (NotSerializableException e) {
                we = e;
            }
            if (we == null) {
                System.err.println("\nTEST FAILED: Write of NoPickleClass " +
                    "should have raised an exception");
                throw new Error();
            }

            p.flush();
            ostream.close();

            istream = new FileInputStream("psiotest3.tmp");
            ObjectInputStream q = new ObjectInputStream(istream);

            /* Catch the expected exception and
             * complain if it does not occur.
             */
            TryPickleClass npc_u;

            NotSerializableException re = null;
            try {
                // Read the two objects, neither has a cleanup method
                q.readObject();
                q.readObject();
                npc_u = (TryPickleClass)q.readObject();
            } catch (NotSerializableException e) {
                re = e;
            }
            if (re == null) {
                System.err.println("\nTEST FAILED: Read of NoPickleClass " +
                   "should have raised an exception");
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

class PickleClass implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    int ii = 17;
    transient int tmp[];

    private void writeObject(ObjectOutputStream pw) throws IOException {
        pw.writeUTF("PickleClass");
        pw.writeInt(ii);
    }

    private void readObject(ObjectInputStream pr) throws IOException {
        tmp = new int[32];
        pr.readUTF();
        ii = pr.readInt();
    }

    private void readObjectCleanup(ObjectInputStream pr) {
        System.err.println("\nPickleClass cleanup correctly called on abort.");
        if (tmp != null) {
            tmp = null;
        }
    }

}

class NoPickleClass extends PickleClass {
    private static final long serialVersionUID = 1L;

    private void writeObject(ObjectOutputStream pw)
        throws NotSerializableException
    {
        throw new NotSerializableException("NoPickleClass");
    }

    private void readObject(ObjectInputStream pr)
            throws NotSerializableException
    {
            throw new NotSerializableException("NoPickleClass");
    }
}

class TryPickleClass  extends NoPickleClass {
    private static final long serialVersionUID = 1L;

    int i = 7;
    transient int tmp[];

    private void writeObject(ObjectOutputStream pw) throws IOException {
            pw.writeInt(i);
    }

    private void readObject(ObjectInputStream pr) throws IOException {
            tmp = new int[32];
            i = pr.readInt();
    }

    private void readObjectCleanup(ObjectInputStream pr) {
            System.err.println("\nCleanup called on abort");
            if (tmp != null) {
                tmp = null;
        }
    }
}
