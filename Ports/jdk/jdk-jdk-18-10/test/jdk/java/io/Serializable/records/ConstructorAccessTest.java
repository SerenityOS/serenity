/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8246774
 * @summary Ensures that the serialization implementation can *always* access
 *          the record constructor
 * @run testng ConstructorAccessTest
 * @run testng/othervm/java.security.policy=empty_security.policy ConstructorAccessTest
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Externalizable;
import java.io.Serializable;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.System.out;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/*implicit*/ record Aux1 (int x) implements Serializable { }

/*implicit*/ record Aux2 (int x) implements Serializable { }

public class ConstructorAccessTest {

    public record A (int x) implements Serializable { }

    protected static record B (long l) implements Serializable { }

    /*implicit*/ static record C (float f) implements Serializable { }

    private record D (double d) implements Serializable { }

    interface ThrowingExternalizable extends Externalizable {
        default void writeExternal(ObjectOutput out) {
            fail("should not reach here");
        }
        default void readExternal(ObjectInput in) {
            fail("should not reach here");
        }
    }

    public record E (short s) implements ThrowingExternalizable { }

    protected static record F (long l) implements ThrowingExternalizable { }

    /*implicit*/ static record G (double d) implements ThrowingExternalizable { }

    private record H (double d) implements ThrowingExternalizable { }

    @DataProvider(name = "recordInstances")
    public Object[][] recordInstances() {
        return new Object[][] {
            new Object[] { new A(34)        },
            new Object[] { new B(44L)       },
            new Object[] { new C(4.5f)      },
            new Object[] { new D(440d)      },
            new Object[] { new E((short)12) },
            new Object[] { new F(45L)       },
            new Object[] { new G(0.4d)      },
            new Object[] { new H(440d)      },
            new Object[] { new Aux1(63)     },
            new Object[] { new Aux2(64)     },
        };
    }

    @Test(dataProvider = "recordInstances")
    public void roundTrip(Object objToSerialize) throws Exception {
        out.println("\n---");
        out.println("serializing : " + objToSerialize);
        var objDeserialized = serializeDeserialize(objToSerialize);
        out.println("deserialized: " + objDeserialized);
        assertEquals(objToSerialize, objDeserialized);
        assertEquals(objDeserialized, objToSerialize);
    }

    // ---

    static <T> byte[] serialize(T obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.close();
        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    static <T> T deserialize(byte[] streamBytes)
        throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(streamBytes);
        ObjectInputStream ois  = new ObjectInputStream(bais);
        return (T) ois.readObject();
    }

    static <T> T serializeDeserialize(T obj)
        throws IOException, ClassNotFoundException
    {
        return deserialize(serialize(obj));
    }
}
