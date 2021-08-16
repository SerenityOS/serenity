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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import jdk.test.lib.compiler.CompilerUtils;
import org.testng.annotations.BeforeTest;
import static org.testng.Assert.*;

/**
 * An abstract superclass for tests that require to serialize and deserialize
 * record-like and record classes. Can be used for determining migration and
 * interop between record-like and record classes.
 */
public class AbstractTest {

    static final String TEST_SRC = System.getProperty("test.src", ".");
    static final String TEST_CLASSES = System.getProperty("test.classes", ".");
    static final Path TEST_CLASSES_DIR = Path.of(TEST_CLASSES);

    static final Path PLAIN_SRC_DIR = Path.of(TEST_SRC, "plain");
    static final Path PLAIN_DEST_DIR = Path.of("plain");

    static final Path RECORD_SRC_DIR = Path.of(TEST_SRC, "record");
    static final Path RECORD_DEST_DIR = Path.of("record");

    @BeforeTest
    public void setup() throws IOException {
        assertTrue(CompilerUtils.compile(PLAIN_SRC_DIR, PLAIN_DEST_DIR,
                   "--class-path", TEST_CLASSES_DIR.toString()));

        assertTrue(CompilerUtils.compile(RECORD_SRC_DIR, RECORD_DEST_DIR,
                   "--class-path", TEST_CLASSES_DIR.toString()));
    }

    static <T> byte[] serialize(T obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.close();
        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    static <T> T deserializeAsPlain(byte[] streamBytes)
        throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(streamBytes);
        ObjectInputStream ois  = new PlainObjectInputStream(bais);
        return (T) ois.readObject();
    }

    @SuppressWarnings("unchecked")
    static <T> T deserializeAsRecord(byte[] streamBytes)
        throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(streamBytes);
        ObjectInputStream ois  = new RecordObjectInputStream(bais);
        return (T) ois.readObject();
    }

    static Point newPlainPoint(int x, int y) throws Exception {
        Class<?> c = Class.forName("PointImpl", true, plainLoader);
        var point = (Point)c.getConstructor(int.class, int.class).newInstance(x, y);
        assert !point.getClass().isRecord();
        return point;
    }

    static DefaultValues newPlainDefaultValues(Point point) throws Exception {
        Class<?> c = Class.forName("DefaultValuesImpl", true, plainLoader);
        var defaultValues =  (DefaultValues)c.getConstructor(Point.class).newInstance(point);
        assert !defaultValues.getClass().isRecord();
        return defaultValues;
    }

    static final ClassLoader plainLoader = plainLoader();

    static final ClassLoader plainLoader() {
        try {
            return new URLClassLoader("plain-loader",
                                      new URL[] { URI.create(PLAIN_DEST_DIR.toUri() + "/").toURL() },
                                      AbstractTest.class.getClassLoader());
        } catch(MalformedURLException e) {
            throw new AssertionError(e);
        }
    }

    static final ClassLoader recordLoader = recordLoader();

    static final ClassLoader recordLoader() {
        try {
            return new URLClassLoader("record-loader",
                                      new URL[] { URI.create(RECORD_DEST_DIR.toUri() + "/").toURL() },
                                      AbstractTest.class.getClassLoader());
        } catch(MalformedURLException e) {
            throw new AssertionError(e);
        }
    }

    static class PlainObjectInputStream extends ObjectInputStream {

        PlainObjectInputStream(InputStream is) throws IOException {
            super(is);
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc)
            throws ClassNotFoundException
        {
            return Class.forName(desc.getName(), false, plainLoader);
        }
    }

    /**
     * An OIS implementation that delegates its class lookup to a loader
     * that attempts to locate the class equivalent of the specified stream
     * class description first in the test's record implementations.
     */
    static class RecordObjectInputStream extends ObjectInputStream {

        RecordObjectInputStream(InputStream is) throws IOException {
            super(is);
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc)
            throws ClassNotFoundException
        {
            return Class.forName(desc.getName(), false, recordLoader);
        }
    }
}
