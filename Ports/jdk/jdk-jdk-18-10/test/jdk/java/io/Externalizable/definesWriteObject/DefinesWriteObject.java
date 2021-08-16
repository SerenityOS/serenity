/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 4151469
 * @summary Write and read an Externalizable class that defines writeObject.
 * There was some confusion over writeObject method needing
 * to be defined by an Externalizable class. It does not
 * need to exist, but Externalizable class should work correctly
 * if the member exists.
 */

import java.io.*;

public class DefinesWriteObject implements Externalizable {

    private int    intData = 4;
    private Object objData = new String("hello");

    public DefinesWriteObject() {
    }

    public DefinesWriteObject(int i, Object o) {
        intData = i;
        objData = o;
    }

    /**
     * There was some confusion over writeObject method needing
     * to be defined by an Externalizable class. It does not
     * need to exist, but Externalizable class should work correctly
     * if the member exists.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
    }

    /**
     * @serialData Writes an integer, Object.
     */
    public void writeExternal(ObjectOutput out)
        throws IOException
    {
        out.writeInt(intData);
        out.writeObject(objData);
    }

    public void readExternal(ObjectInput in)
        throws IOException, ClassNotFoundException
    {
        intData = in.readInt();
        objData = in.readObject();
    }

    public static void main(String args[])
        throws IOException, ClassNotFoundException
    {
        DefinesWriteObject obj1 = new DefinesWriteObject(5, "GoodBye");
        DefinesWriteObject obj2 = new DefinesWriteObject(6, "AuRevoir");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj1);
        oos.writeObject(obj2);
        oos.close();

        ByteArrayInputStream bais =
            new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        DefinesWriteObject readObject1 = (DefinesWriteObject)ois.readObject();
        DefinesWriteObject readObject2 = (DefinesWriteObject)ois.readObject();
        ois.close();

        // verify that deserialize data matches objects serialized.
        if (obj1.intData != readObject1.intData ||
            obj2.intData != readObject2.intData) {
            throw new Error("Unexpected mismatch between integer data written and read.");
        }
        if ( ! ((String)obj1.objData).equals((String)readObject1.objData) ||
             ! ((String)obj2.objData).equals((String)readObject2.objData)) {
            throw new Error("Unexpected mismatch between String data written and read.");
        }
    }
};
