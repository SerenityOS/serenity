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
 * @summary Verifies that privileged operations performed in the record
 *          constructor throw, when run without the required permissions
 * @run testng/othervm/java.security.policy=empty_security.policy ConstructorPermissionTest
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessControlException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.System.out;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

/**
 * Ensures that the appropriate exception, invalid object exception, with a
 * suitable cause, is thrown when the record constructor performs a privileged
 * operation without permission.
 */
public class ConstructorPermissionTest {

    /** "big switch" that can be used to allow/disallow record construction
     * set to true after the data provider has constructed all record objects */
    private static volatile boolean firstDataSetCreated;

    record R1 () implements Serializable {
        public R1 {
            if (firstDataSetCreated) {
                try { Files.list(Path.of(".")); }
                catch (IOException unexpected) { throw new AssertionError(unexpected); }
            }
        }
    }

    record R2 (int x) implements Serializable {
        public R2 {
            if (firstDataSetCreated) {
                try { new Socket("localhost", 8080); }
                catch (IOException unexpected) { throw new AssertionError(unexpected); }
            }
        }
    }

    record R3 (String... args) implements Serializable {
        public R3 {
            if (firstDataSetCreated)
                ProcessHandle.current();
        }
    }

    static final Class<InvalidObjectException> IOE = InvalidObjectException.class;

    @DataProvider(name = "exceptionInstances")
    public Object[][] exceptionInstances() {
        var objs = new Object[][] {
            new Object[] { new R1(),     AccessControlException.class, "FilePermission"   },
            new Object[] { new R2(1),    AccessControlException.class, "SocketPermission" },
            new Object[] { new R3("s"),  AccessControlException.class, "manageProcess"    },
        };
        firstDataSetCreated = true;
        return objs;
    }

    @Test(dataProvider = "exceptionInstances")
    public void testExceptions(Object objectToSerialize,
                               Class<? extends Throwable> expectedExType,
                               String expectedExMessage)
        throws Exception
    {
        out.println("\n---");
        out.println("serializing: " + objectToSerialize);
        byte[] bytes = serialize(objectToSerialize);
        InvalidObjectException ioe = expectThrows(IOE, () -> deserialize(bytes));
        out.println("caught expected IOE: " + ioe);
        Throwable t = ioe.getCause();
        assertTrue(t.getClass().equals(expectedExType),
                   "Expected:" + expectedExType + ", got:" + t);
        out.println("expected cause " + expectedExType +" : " + t);
        String msg = t.getMessage();
        assertTrue(msg.contains(expectedExMessage),
                   "Expected message to contain [" + expectedExMessage + "], in " + msg);
    }

    // TODO: add positive tests with permissions granted.

    // --- infra

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
}
