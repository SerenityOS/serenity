/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4313887 6838333
 * @summary Unit test for java.nio.file.attribute.UserDefinedFileAttributeView
 * @library ../..
 * @key randomness
 */

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.*;
import static java.nio.file.LinkOption.*;
import java.nio.file.attribute.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.io.IOException;

public class Basic {

    private static Random rand = new Random();

    private static final String ATTR_NAME = "mime_type";
    private static final String ATTR_VALUE = "text/plain";
    private static final String ATTR_VALUE2 = "text/html";

    static interface Task {
        void run() throws Exception;
    }

    static void tryCatch(Class<? extends Throwable> ex, Task task) {
        boolean caught = false;
        try {
            task.run();
        } catch (Throwable x) {
            if (ex.isAssignableFrom(x.getClass())) {
                caught = true;
            } else {
                throw new RuntimeException(x);
            }
        }
        if (!caught)
            throw new RuntimeException(ex.getName() + " expected");
    }

    static void expectNullPointerException(Task task) {
        tryCatch(NullPointerException.class, task);
    }

    static boolean hasAttribute(UserDefinedFileAttributeView view, String attr)
        throws IOException
    {
        for (String name: view.list()) {
            if (name.equals(ATTR_NAME))
                return true;
        }
        return false;
    }

    static void test(Path file, LinkOption... options) throws IOException {
        final UserDefinedFileAttributeView view =
            Files.getFileAttributeView(file, UserDefinedFileAttributeView.class, options);
        ByteBuffer buf = rand.nextBoolean() ?
            ByteBuffer.allocate(100) : ByteBuffer.allocateDirect(100);

        // Test: write
        buf.put(ATTR_VALUE.getBytes()).flip();
        int size = buf.remaining();
        int nwrote = view.write(ATTR_NAME, buf);
        if (nwrote != size)
            throw new RuntimeException("Unexpected number of bytes written");

        // Test: size
        if (view.size(ATTR_NAME) != size)
            throw new RuntimeException("Unexpected size");

        // Test: read
        buf.clear();
        int nread = view.read(ATTR_NAME, buf);
        if (nread != size)
            throw new RuntimeException("Unexpected number of bytes read");
        buf.flip();
        String value = Charset.defaultCharset().decode(buf).toString();
        if (!value.equals(ATTR_VALUE))
            throw new RuntimeException("Unexpected attribute value");

        // Test: read with insufficient space
        tryCatch(IOException.class, new Task() {
            public void run() throws IOException {
                view.read(ATTR_NAME, ByteBuffer.allocateDirect(1));
            }});

        // Test: replace value
        buf.clear();
        buf.put(ATTR_VALUE2.getBytes()).flip();
        size = buf.remaining();
        view.write(ATTR_NAME, buf);
        if (view.size(ATTR_NAME) != size)
            throw new RuntimeException("Unexpected size");

        // Test: list
        if (!hasAttribute(view, ATTR_NAME))
            throw new RuntimeException("Attribute name not in list");

        // Test: delete
        view.delete(ATTR_NAME);
        if (hasAttribute(view, ATTR_NAME))
            throw new RuntimeException("Attribute name in list");

        // Test: dynamic access
        String name = "user:" + ATTR_NAME;
        byte[] valueAsBytes = ATTR_VALUE.getBytes();
        Files.setAttribute(file, name, valueAsBytes);
        byte[] actualAsBytes = (byte[])Files.getAttribute(file, name);
        if (!Arrays.equals(valueAsBytes, actualAsBytes))
            throw new RuntimeException("Unexpected attribute value");
        Map<String,?> map = Files.readAttributes(file, name);
        if (!Arrays.equals(valueAsBytes, (byte[])map.get(ATTR_NAME)))
            throw new RuntimeException("Unexpected attribute value");
        map = Files.readAttributes(file, "user:*");
        if (!Arrays.equals(valueAsBytes, (byte[])map.get(ATTR_NAME)))
            throw new RuntimeException("Unexpected attribute value");
    }

    static void miscTests(final Path file) throws IOException {
        final UserDefinedFileAttributeView view =
            Files.getFileAttributeView(file, UserDefinedFileAttributeView.class);
        view.write(ATTR_NAME, ByteBuffer.wrap(ATTR_VALUE.getBytes()));

        // NullPointerException
        final ByteBuffer buf = ByteBuffer.allocate(100);

        expectNullPointerException(new Task() {
            public void run() throws IOException {
                view.read(null, buf);
            }});
        expectNullPointerException(new Task() {
            public void run() throws IOException {
                view.read(ATTR_NAME, null);
            }});
        expectNullPointerException(new Task() {
            public void run() throws IOException {
                view.write(null, buf);
            }});
        expectNullPointerException(new Task() {
            public void run() throws IOException {
               view.write(ATTR_NAME, null);
            }});
        expectNullPointerException(new Task() {
            public void run() throws IOException {
                view.size(null);
            }});
        expectNullPointerException(new Task() {
            public void run() throws IOException {
                view.delete(null);
            }});
        expectNullPointerException(new Task() {
            public void run() throws IOException {
                Files.getAttribute(file, null);
            }});
        expectNullPointerException(new Task() {
            public void run() throws IOException {
                Files.getAttribute(file, "user:" + ATTR_NAME, (LinkOption[])null);
            }});
        expectNullPointerException(new Task() {
            public void run() throws IOException {
                Files.setAttribute(file, "user:" + ATTR_NAME, null);
            }});
        expectNullPointerException(new Task() {
            public void run() throws IOException {
                Files.setAttribute(file, null, new byte[0]);
            }});
        expectNullPointerException(new Task() {
            public void run() throws IOException {
                Files.setAttribute(file, "user: " + ATTR_NAME, new byte[0], (LinkOption[])null);
            }});
        expectNullPointerException(new Task() {
            public void run() throws IOException {
                Files.readAttributes(file, (String)null);
            }});
        expectNullPointerException(new Task() {
            public void run() throws IOException {
                Files.readAttributes(file, "*", (LinkOption[])null);
            }});

        // Read-only buffer
        tryCatch(IllegalArgumentException.class, new Task() {
            public void run() throws IOException {
                ByteBuffer buf = ByteBuffer.wrap(ATTR_VALUE.getBytes()).asReadOnlyBuffer();
                view.write(ATTR_NAME, buf);
                buf.flip();
                view.read(ATTR_NAME, buf);
            }});

        // Zero bytes remaining
        tryCatch(IOException.class, new Task() {
            public void run() throws IOException {
                ByteBuffer buf = buf = ByteBuffer.allocateDirect(100);
                buf.position(buf.capacity());
                view.read(ATTR_NAME, buf);
            }});
    }

    public static void main(String[] args) throws IOException {
        // create temporary directory to run tests
        Path dir = TestUtil.createTemporaryDirectory();
        try {
            if (!Files.getFileStore(dir).supportsFileAttributeView("user")) {
                System.out.println("UserDefinedFileAttributeView not supported - skip test");
                return;
            }

            // test access to user defined attributes of regular file
            Path file = dir.resolve("foo.html");
            Files.createFile(file);
            try {
                test(file);
            } finally {
                Files.delete(file);
            }

            // test access to user defined attributes of directory
            Path subdir = dir.resolve("foo");
            Files.createDirectory(subdir);
            try {
                test(subdir);
            } finally {
                Files.delete(subdir);
            }

            // test access to user defined attributes of sym link
            if (TestUtil.supportsLinks(dir)) {
                Path target = dir.resolve("doesnotexist");
                Path link = dir.resolve("link");
                Files.createSymbolicLink(link, target);
                try {
                    test(link, NOFOLLOW_LINKS);
                } catch (IOException x) {
                    // access to attributes of sym link may not be supported
                } finally {
                    Files.delete(link);
                }
            }

            // misc. tests
            try {
                file = dir.resolve("foo.txt");
                Files.createFile(file);
                miscTests(dir);
            } finally {
                Files.delete(file);
            }

        } finally {
            TestUtil.removeAll(dir);
        }
    }
 }
