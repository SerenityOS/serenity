/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8000354 8000685 8004371 8043119
 * @summary Basic test of storeToXML and loadToXML
 * @run main/othervm -Djava.security.manager=allow LoadAndStoreXML
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.util.PropertyPermission;

public class LoadAndStoreXML {
    static final String bomChar = "\uFEFF";

    /**
     * Simple policy implementation that grants a set of permissions to
     * all code sources and protection domains.
     */
    static class SimplePolicy extends Policy {
        private final Permissions perms;

        public SimplePolicy(Permission...permissions) {
            perms = new Permissions();
            for (Permission permission : permissions)
                perms.add(permission);
        }

        @Override
        public PermissionCollection getPermissions(CodeSource cs) {
            return perms;
        }

        @Override
        public PermissionCollection getPermissions(ProtectionDomain pd) {
            return perms;
        }

        @Override
        public boolean implies(ProtectionDomain pd, Permission p) {
            return perms.implies(p);
        }
    }

    /**
     * A {@code ByteArrayInputStream} that allows testing if the
     * {@code close} method has been invoked.
     */
    static class TestInputStream extends ByteArrayInputStream {
        private boolean closed;

        TestInputStream(byte[] buf) {
            super(buf);
        }

        boolean isOpen() {
            return !closed;
        }

        public void close() throws IOException {
            try {
                super.close();
            } finally {
                closed = true;
            }
        }
    }

    /**
     * A {@code ByteArrayOutputStream} that allows testing if the
     * {@code close} method has been invoked.
     */
    static class TestOutputStream extends ByteArrayOutputStream {
        private boolean closed;

        boolean isOpen() {
            return !closed;
        }

        public void close() throws IOException {
            try {
                super.close();
            } finally {
                closed = true;
            }
        }
    }

    /**
     * Sanity test that properties saved with Properties#storeToXML can be
     * read with Properties#loadFromXML.
     */
    static void testLoadAndStore(String encoding, boolean appendBOM) throws IOException {
        System.out.println("testLoadAndStore, encoding=" + encoding);

        Properties props = new Properties();
        props.put("k0", "\u6C34");
        props.put("k1", "foo");
        props.put("k2", "bar");
        props.put("k3", "\u0020\u0391\u0392\u0393\u0394\u0395\u0396\u0397");
        props.put("k4", "\u7532\u9aa8\u6587");
        props.put("k5", "<java.home>/conf/jaxp.properties");

        TestOutputStream out = new TestOutputStream();
        props.storeToXML(out, null, encoding);
        if (!out.isOpen())
            throw new RuntimeException("OutputStream closed by storeToXML");

        Properties p = new Properties();
        TestInputStream in;
        if (appendBOM) {
            byte[] byteOrderMark = bomChar.getBytes(Charset.forName(encoding));
            byte[] outArray = out.toByteArray();
            byte[] inputArray = new byte[byteOrderMark.length + outArray.length];
            System.arraycopy(byteOrderMark, 0, inputArray, 0, byteOrderMark.length);
            System.arraycopy(outArray, 0, inputArray, byteOrderMark.length, outArray.length);
            in = new TestInputStream(inputArray);
        } else {
            in = new TestInputStream(out.toByteArray());
        }
        p.loadFromXML(in);
        if (in.isOpen())
            throw new RuntimeException("InputStream not closed by loadFromXML");

        if (!p.equals(props)) {
            System.err.println("stored: " + props);
            System.err.println("loaded: " + p);
            throw new RuntimeException("Test failed");
        }
    }

    /**
     * Test loadFromXML with a document that does not have an encoding declaration
     */
    static void testLoadWithoutEncoding() throws IOException {
        System.out.println("testLoadWithoutEncoding");

        Properties expected = new Properties();
        expected.put("foo", "bar");

        String s = "<?xml version=\"1.0\"?>" +
                   "<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">" +
                   "<properties>" +
                   "<entry key=\"foo\">bar</entry>" +
                   "</properties>";
        ByteArrayInputStream in = new ByteArrayInputStream(s.getBytes("UTF-8"));
        Properties props = new Properties();
        props.loadFromXML(in);

        if (!props.equals(expected)) {
            System.err.println("loaded: " + props + ", expected: " + expected);
            throw new RuntimeException("Test failed");
        }
    }

    /**
     * Test loadFromXML with unsupported encoding
     */
    static void testLoadWithBadEncoding() throws IOException {
        System.out.println("testLoadWithBadEncoding");
        String s = "<?xml version=\"1.0\" encoding=\"BAD\"?>" +
                   "<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">" +
                   "<properties>" +
                   "<entry key=\"foo\">bar</entry>" +
                   "</properties>";
        ByteArrayInputStream in = new ByteArrayInputStream(s.getBytes("UTF-8"));
        Properties props = new Properties();
        try {
            props.loadFromXML(in);
            throw new RuntimeException("UnsupportedEncodingException expected");
        } catch (UnsupportedEncodingException expected) { }
    }

    /**
     * Test storeToXML with unsupported encoding
     */
    static void testStoreWithBadEncoding() throws IOException {
        System.out.println("testStoreWithBadEncoding");
        Properties props = new Properties();
        props.put("foo", "bar");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            props.storeToXML(out, null, "BAD");
            throw new RuntimeException("UnsupportedEncodingException expected");
        } catch (UnsupportedEncodingException expected) { }
    }

    /**
     * Test loadFromXML with malformed documents
     */
    static void testLoadWithMalformedDoc(Path dir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.xml")) {
            for (Path file: stream) {
                System.out.println("testLoadWithMalformedDoc, file=" + file.getFileName());
                try (InputStream in = Files.newInputStream(file)) {
                    Properties props = new Properties();
                    try {
                        props.loadFromXML(in);
                        throw new RuntimeException("InvalidPropertiesFormatException not thrown");
                    } catch (InvalidPropertiesFormatException x) {
                        System.out.println(x);
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {

        testLoadAndStore("UTF-8", false);
        testLoadAndStore("UTF-16", false);
        testLoadAndStore("UTF-16BE", false);
        testLoadAndStore("UTF-16LE", false);
        testLoadAndStore("UTF-16BE", true);
        testLoadAndStore("UTF-16LE", true);
        testLoadWithoutEncoding();
        testLoadWithBadEncoding();
        testStoreWithBadEncoding();

        // malformed documents
        String src = System.getProperty("test.src");
        String subdir = "invalidxml";
        Path dir = (src == null) ? Paths.get(subdir) : Paths.get(src, subdir);
        testLoadWithMalformedDoc(dir);

        // re-run sanity test with security manager
        Policy orig = Policy.getPolicy();
        Policy p = new SimplePolicy(new RuntimePermission("setSecurityManager"),
                                    new PropertyPermission("line.separator", "read"));
        Policy.setPolicy(p);
        System.setSecurityManager(new SecurityManager());
        try {
            testLoadAndStore("UTF-8", false);
        } finally {
            // turn off security manager and restore policy
            System.setSecurityManager(null);
            Policy.setPolicy(orig);
        }

    }
}
