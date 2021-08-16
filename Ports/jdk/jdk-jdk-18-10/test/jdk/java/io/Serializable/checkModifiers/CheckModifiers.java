/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4214888
 * @clean CheckModifiers TestClass1 TestClass2
 * @build CheckModifiers
 * @run main CheckModifiers
 * @summary Make sure that serialpersistentFields data member is used to
 *          represent tyhe serializable fields only if it has the modfiers
 *          static, final, private and the type is ObjectStreamField.
 *          No need to check for static, as ObjectStreamField class is not
 *          serializable.
 *
 */

import java.io.*;
class TestClass1 implements Serializable {
    private static final long serialVersionUID = 1L;

    // Missing the "final" modifier
    @SuppressWarnings("serial") /* Incorrect declarations are being tested */
    private static ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("field1", Integer.class),
        new ObjectStreamField("field2", Double.TYPE),
    };

    Integer field1;
    double field2;
    int field3;
    String field4;

    public TestClass1(Integer f1, double f2, int f3, String f4) {
        field1 = f1;
        field2 = f2;
        field3 = f3;
        field4 = f4;
    }

    private void readObject(ObjectInputStream ois)
        throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField pfields = ois.readFields();

        field1 = (Integer) pfields.get("field1", Integer.valueOf(100));
        field2 = pfields.get("field2", 99.99);

        /* These fields must be present in the stream */
        try {
            field3 = pfields.get("field3", 99);
            System.out.println("Passes test 1a");
        } catch(IllegalArgumentException e) {
            throw new Error("data field: field3 not in the persistent stream");
        }
        try {
            field4 = (String) pfields.get("field4", "Default string");
            System.out.println("Passes test 1b");
        } catch(IllegalArgumentException e) {
            throw new Error("data field: field4 not in the persistent stream");
        }
    }
};


class TestClass2 implements Serializable {
    private static final long serialVersionUID = 1L;

    // public instead of private
    @SuppressWarnings("serial") /* Incorrect declarations are being tested */
    public static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("field1", Integer.class),
        new ObjectStreamField("field2", Double.TYPE),
    };

    Integer field1;
    double field2;
    int field3;
    String field4;

    public TestClass2(Integer f1, double f2, int f3, String f4) {
        field1 = f1;
        field2 = f2;
        field3 = f3;
        field4 = f4;
    }

    private void readObject(ObjectInputStream ois)
        throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField pfields = ois.readFields();

        field1 = (Integer) pfields.get("field1", Integer.valueOf(100));
        field2 = pfields.get("field2", 99.99);

        /* These fields must be present in the stream */
        try {
            field3 = pfields.get("field3", 99);
            System.out.println("Passes test 2a");
        } catch(IllegalArgumentException e) {
            throw new Error("data field: field3 not in the persistent stream");
        }
        try {
            field4 = (String) pfields.get("field4", "Default string");
            System.out.println("Passes test 2b");
        } catch(IllegalArgumentException e) {
            throw new Error("data field: field4 not in the persistent stream");
        }
    }
};

class TestClass3 implements Serializable{
    private static final long serialVersionUID = 1L;

    // Not of type ObjectStreamField
    @SuppressWarnings("serial") /* Incorrect declarations are being tested */
    private final String[] serialPersistentFields =  {"Foo","Foobar"};;
    Integer field1;
    double field2;
    int field3;
    String field4;

    public TestClass3(Integer f1, double f2, int f3, String f4) {
        field1 = f1;
        field2 = f2;
        field3 = f3;
        field4 = f4;
    }

    private void readObject(ObjectInputStream ois)
        throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField pfields = ois.readFields();

        field1 = (Integer) pfields.get("field1", Integer.valueOf(100));
        field2 = pfields.get("field2", 99.99);
        field3 = pfields.get("field3", 99);
        field4 = (String) pfields.get("field4", "Default string");

        try {
            String[] tserialPersistentFields =
                (String[])pfields.get("serialPersistentFields", null);
            System.out.println("Passes test 3");
        } catch(IllegalArgumentException e) {
            throw new Error("non-static field:  " +
                "serialPersistentFields must be in the persistent stream");
        }
    }
};

class TestClass4 implements Serializable {
    private static final long serialVersionUID = 1L;

    // Correct format
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("field1", Integer.class),
        new ObjectStreamField("field2", Double.TYPE),
    };

    Integer field1;
    double field2;
    int field3;
    String field4;

    public TestClass4(Integer f1, double f2, int f3, String f4) {
        field1 = f1;
        field2 = f2;
        field3 = f3;
        field4 = f4;
    }

    private void readObject(ObjectInputStream ois)
        throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField pfields = ois.readFields();

        field1 = (Integer) pfields.get("field1", Integer.valueOf(100));
        field2 = pfields.get("field2", 99.99);

        try {
            field3 = pfields.get("field3", 99);
            throw new Error("data field: field3 in the persistent stream");
        } catch(IllegalArgumentException e) {
            System.out.println("Passes test 4a");
        }
        try {
            field4 = (String) pfields.get("field4", "Default string");
            throw new Error("data field: field4 in the persistent stream");
        } catch(IllegalArgumentException e) {
            System.out.println("Passes test 4b");
        }
    }
};

public class CheckModifiers {
    public static void main(String[] args)
        throws ClassNotFoundException, IOException{
        TestClass1 tc1 = new TestClass1(100, 25.56, 2000,
            new String("Test modifiers of serialPersistentFields"));

        TestClass2 tc2 = new TestClass2(100, 25.56, 2000,
            new String("Test modifiers of serialPersistentFields"));

        TestClass3 tc3 = new TestClass3(100, 25.56, 2000,
            new String("Test Type of serialPersistentFields"));

        TestClass4 tc4 = new TestClass4(100, 25.56, 2000,
            new String("Test modifiers of serialPersistentFields"));


        FileOutputStream fos = new FileOutputStream("fields.ser");
        try {
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            System.out.println("Writing obj 1");
            oos.writeObject(tc1);
            System.out.println("Writing obj 2");
            oos.writeObject(tc2);
            System.out.println("Writing obj 3");
            oos.writeObject(tc3);
            System.out.println("Writing obj 4");
            oos.writeObject(tc4);
            oos.flush();
        } finally {
            fos.close();
        }

        FileInputStream fis = new FileInputStream("fields.ser");
        try {
            ObjectInputStream ois = new ObjectInputStream(fis);
            System.out.println("Test modifiers for serialPeristentFields ");
            System.out.println("---------------------------------------- ");
            System.out.println("Declaration missing final modifier");
            ois.readObject();
            System.out.println();
            System.out.println("Declaration with public instead of private access");
            ois.readObject();
            System.out.println();
            System.out.println("Declaration with different type");
            ois.readObject();
            System.out.println();
            System.out.println("Declaration as in specification");
            ois.readObject();
        } finally {
            fis.close();
        }
    }
};
