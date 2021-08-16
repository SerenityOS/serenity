/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4313887 6881498
 * @modules java.base/java.lang:open
 * @summary Miscellenous tests on exceptions in java.nio.file
 */

import java.nio.file.*;
import java.io.*;
import java.util.Objects;
import java.lang.reflect.*;

public class Exceptions {

    public static void main(String[] args) throws Exception {
        testFileSystemException();
        testDirectoryIteratorException();
    }

    static void testFileSystemException() throws Exception {
        String thisFile = "source";
        String otherFile = "target";
        String reason = "Access denied";

        // getFile/getOtherFile
        testFileSystemException(thisFile, otherFile, reason);
        testFileSystemException(null, otherFile, reason);
        testFileSystemException(thisFile, null, reason);
        testFileSystemException(thisFile, otherFile, null);

        // serialization
        FileSystemException exc;
        exc = new FileSystemException(thisFile, otherFile, reason);
        exc = (FileSystemException)deserialize(serialize(exc));
        if (!exc.getFile().equals(thisFile) || !exc.getOtherFile().equals(otherFile))
            throw new RuntimeException("Exception not reconstituted completely");
    }

    static void testFileSystemException(String thisFile,
                                        String otherFile,
                                        String reason)
    {
        FileSystemException exc = new FileSystemException(thisFile, otherFile, reason);
        if (!Objects.equals(thisFile, exc.getFile()))
            throw new RuntimeException("getFile returned unexpected result");
        if (!Objects.equals(otherFile, exc.getOtherFile()))
            throw new RuntimeException("getOtherFile returned unexpected result");
        if (!Objects.equals(reason, exc.getReason()))
            throw new RuntimeException("getReason returned unexpected result");
    }

    static void testDirectoryIteratorException() throws Exception {
        // NullPointerException
        try {
            new DirectoryIteratorException(null);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException expected) { }

        // serialization
        DirectoryIteratorException exc;
        exc = new DirectoryIteratorException(new IOException());
        exc = (DirectoryIteratorException)deserialize(serialize(exc));
        IOException ioe = exc.getCause();
        if (ioe == null)
            throw new RuntimeException("Cause should not be null");

        // when deserializing then the cause should be an IOException
        hackCause(exc, null);
        try {
            deserialize(serialize(exc));
            throw new RuntimeException("InvalidObjectException expected");
        } catch (InvalidObjectException expected) { }

        hackCause(exc, new RuntimeException());
        try {
            deserialize(serialize(exc));
            throw new RuntimeException("InvalidObjectException expected");
        } catch (InvalidObjectException expected) { }
    }


    // Use reflection to set a Throwable's cause.
    static void hackCause(Throwable t, Throwable cause)
        throws NoSuchFieldException, IllegalAccessException
    {
        Field f = Throwable.class.getDeclaredField("cause");
        f.setAccessible(true);
        f.set(t, cause);
    }

    // Serialize the given object to a byte[]
    static byte[] serialize(Object o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        oos.close();
        return baos.toByteArray();
    }

    // Read an object from its serialized form
    static Object deserialize(byte[] bytes)
        throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(in);
        Object result = ois.readObject();
        ois.close();
        return result;
    }
}
