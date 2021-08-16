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
 * @test
 * @bug 4100915
 * @summary Verify that [write/read]ObjectOverride methods get called.
 *          Test verifies that ALL methods to write an object can
 *          be overridden. However, the testing for reading an object
 *          is incomplete. Only test that readObjectOverride is called.
 *          An entire protocol would need to be implemented and written
 *          out before being able to test the input side of the API.
 *
 *          Also, would be appropriate that this program verify
 *          that if SerializablePermission "enableSubclassImplementation"
 *          is not in the security policy and security is enabled, that
 *          a security exception is thrown when constructing the
 *          ObjectOutputStream subclass.
 *
 *
 * @compile AbstractObjectInputStream.java AbstractObjectOutputStream.java
 * @compile XObjectInputStream.java XObjectOutputStream.java
 * @compile SubclassTest.java
 * @run main SubclassTest
 * @run main/othervm/policy=Allow.policy SubclassTest -expectSecurityException
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;

/**
 * Test if customized readObject and writeObject are called.
 */
class B implements Serializable {
    private static final long serialVersionUID = 1L;

    public int publicIntField;
    public static int numWriteObjectCalled = 0;
    B(int v) {
        publicIntField = v;
    }
    private void writeObject(ObjectOutputStream os) throws IOException {
        numWriteObjectCalled++;
        os.defaultWriteObject();
    }

    private void readObject(ObjectInputStream is)
        throws IOException, ClassNotFoundException
    {
        is.defaultReadObject();
    }

};

/**
 * Test PutFields interface.
 */

class C implements Serializable {
    private static final long serialVersionUID = 1L;

    public int xx1;
    public int xx2;
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("x1", Integer.TYPE),
        new ObjectStreamField("x2", Integer.TYPE),
        new ObjectStreamField("x3", Integer.TYPE),
        new ObjectStreamField("x4", Integer.TYPE)
    };
    C() {
        xx1 = 300;
        xx2 = 400;
    }

    private void writeObject(ObjectOutputStream os) throws IOException {
        ObjectOutputStream.PutField putFields = os.putFields();
        putFields.put("x1", xx1);
        putFields.put("x2", xx2);
        putFields.put("x3", xx1 * 2);
        putFields.put("x4", xx2 * 2);
        os.writeFields();
    }

};


class A implements Serializable {
    private static final long serialVersionUID = 1L;

    public int  publicIntField;
    public long publicLongField;
    public B    publicBField;
    public B[]  publicBArray = { new B(4), new B(6)};
    public C    publicCField;

    public A() {
        publicIntField = 3;
        publicLongField = 10L;
        publicBField = new B(5);
        publicCField = new C();
    }
};

public class SubclassTest {
    public static void main(String argv[])
        throws IOException, ClassNotFoundException
    {
        boolean expectSecurityException = false;

        if (argv.length > 0 &&
            argv[0].compareTo("-expectSecurityException") == 0)
            expectSecurityException = true;

        ByteArrayOutputStream baos = new ByteArrayOutputStream(20);
        XObjectOutputStream os = null;
        try {
            os = new XObjectOutputStream(baos);
            if (expectSecurityException)
                throw new Error("Assertion failure. " +
                                "Expected a security exception on previous line.");
        } catch (SecurityException e) {
            if (expectSecurityException) {
                System.err.println("Caught expected security exception.");
                return;
            }
            throw e;
        }
        os.writeObject(new A());
        os.close();
        if (B.numWriteObjectCalled != 3)
            throw new Error("Expected B.writeObject() to be called 3 times;" +
                            " observed only " + B.numWriteObjectCalled + " times");

        XObjectInputStream is =
            new XObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        try {
            A a = (A)is.readObject();
            throw new Error("Expected readObjectOverride() to be called and throw IOException(not implemented)");
        } catch (IOException e) {
        }
        is.close();
    }
};
