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

/* @test
 * @bug 4093279
 * @compile DefaultPackage.java
 * @run main DefaultPackage
 * @summary Raise InvalidClassException if 1st NonSerializable superclass' no-arg constructor is not accessible. This test verifies default package access.
 */
import java.io.*;

class DefaultPackagePublicConstructor {
    public DefaultPackagePublicConstructor() {
    }
};

class DefaultPackageProtectedConstructor {
    protected DefaultPackageProtectedConstructor() {
    }
};

class DefaultPackageDefaultAccessConstructor {
    DefaultPackageDefaultAccessConstructor() {
    }
};

class DefaultPackagePrivateConstructor {
    private DefaultPackagePrivateConstructor() {
    }

    /* need to have at least one protected constructor to extend this class.*/
    protected DefaultPackagePrivateConstructor(int i) {
    }
};

class DefaultPublicSerializable
extends DefaultPackagePublicConstructor implements Serializable
{
    private static final long serialVersionUID = 1L;

    int field1 = 5;
};

class DefaultProtectedSerializable
extends DefaultPackageProtectedConstructor implements Serializable
{
    private static final long serialVersionUID = 1L;

    int field1 = 5;
};

class DefaultAccessSerializable
extends DefaultPackageDefaultAccessConstructor implements Serializable
{
    private static final long serialVersionUID = 1L;

    int field1 = 5;
};

@SuppressWarnings("serial") /* Incorrect declarations are being tested */
class DefaultPrivateSerializable
extends DefaultPackagePrivateConstructor implements Serializable
{
    private static final long serialVersionUID = 1L;

    int field1 = 5;

    DefaultPrivateSerializable() {
        super(1);
    }
};

class ExternalizablePublicConstructor implements Externalizable {
    private static final long serialVersionUID = 1L;

    public ExternalizablePublicConstructor() {
    }
    public void writeExternal(ObjectOutput out) throws IOException {
    }
    public void readExternal(ObjectInput in)
        throws IOException, ClassNotFoundException
        {
        }
};

@SuppressWarnings("serial") /* Incorrect declarations are being tested */
class ExternalizableProtectedConstructor implements Externalizable {
    private static final long serialVersionUID = 1L;

    protected ExternalizableProtectedConstructor() {
    }
    public void writeExternal(ObjectOutput out) throws IOException {
    }
    public void readExternal(ObjectInput in)
        throws IOException, ClassNotFoundException
        {
        }
};

@SuppressWarnings("serial") /* Incorrect declarations are being tested */
class ExternalizableAccessConstructor implements Externalizable {
    private static final long serialVersionUID = 1L;

    ExternalizableAccessConstructor() {
    }
    public void writeExternal(ObjectOutput out) throws IOException {
    }
    public void readExternal(ObjectInput in)
        throws IOException, ClassNotFoundException
        {
        }
};

@SuppressWarnings("serial") /* Incorrect declarations are being tested */
class ExternalizablePrivateConstructor implements Externalizable {
    private static final long serialVersionUID = 1L;

    private ExternalizablePrivateConstructor() {
    }
    public ExternalizablePrivateConstructor(int i) {
    }
    public void writeExternal(ObjectOutput out) throws IOException {
    }
    public void readExternal(ObjectInput in)
        throws IOException, ClassNotFoundException
        {
        }
};


public class DefaultPackage {
    public static void main(String args[])
        throws IOException, ClassNotFoundException
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream out =   new ObjectOutputStream(baos);
            out.writeObject(new DefaultPublicSerializable());
            out.writeObject(new DefaultProtectedSerializable());
            out.writeObject(new DefaultAccessSerializable());
            out.writeObject(new DefaultPrivateSerializable());

            InputStream is = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream in = new ObjectInputStream(is);
            /* (DefaultPublicSerializable) */ in.readObject();
            /* (DefaultProtectedSerializable) */ in.readObject();
            /* (DefaultAcccessSerializable) */ in.readObject();
            try {
                /* (DefaultPrivateSerializable) */ in.readObject();
                throw new Error("Expected InvalidClassException reading DefaultPrivateSerialziable");
            } catch (InvalidClassException e) {
            }
            in.close();

            baos.reset();
            out = new ObjectOutputStream(baos);
            out.writeObject(new ExternalizablePublicConstructor());

            in = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
            /* (ExternalizablePublicConstructor) */ in.readObject();
            in.close();

            baos.reset();
            out = new ObjectOutputStream(baos);
            out.writeObject(new ExternalizableProtectedConstructor());


            in = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
            try {
                /* (ExternalizableProtectedConstructor) */ in.readObject();
                throw new Error("Expected InvalidClassException reading ExternalizableProtectedConstructor");
            } catch (InvalidClassException e) {
            }
            in.close();

            baos.reset();
            out = new ObjectOutputStream(baos);
            out.writeObject(new ExternalizableAccessConstructor());

            in = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
            try {
                /* (ExternalizableAccessConstructor) */ in.readObject();
                throw new Error("Expected InvalidClassException reading ExternalizableAccessConstructor");
            } catch (InvalidClassException e) {
            }
            in.close();

            baos.reset();
            out = new ObjectOutputStream(baos);
            out.writeObject(new ExternalizablePrivateConstructor(2));

            in = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
            try {
                /* (ExternalizablePrivateConstructor) */ in.readObject();
                throw new Error("Expected InvalidClassException reading ExternalizablePrivateConstructor");
            } catch (InvalidClassException e) {
            }
            out.close();
            in.close();
        }
}
