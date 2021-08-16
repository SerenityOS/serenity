/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4313887 6838333 8132497 8242292
 * @summary Unit test for java.nio.file.FileSystem
 * @library .. /test/lib
 * @build jdk.test.lib.Platform
 *        jdk.test.lib.util.FileUtils
 * @run main/othervm Basic
 */

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderNotFoundException;
import java.util.HashMap;
import jdk.test.lib.util.FileUtils;

/**
 * Simple sanity checks for java.nio.file.FileSystem
 */
public class Basic {

    static void check(boolean okay, String msg) {
        if (!okay)
            throw new RuntimeException(msg);
    }

    static void checkFileStores(FileSystem fs) throws IOException {
        // sanity check method
        if (FileUtils.areFileSystemsAccessible()) {
            System.out.println("\n--- Begin FileStores ---");
            for (FileStore store: fs.getFileStores()) {
                System.out.println(store);
            }
            System.out.println("--- EndFileStores ---\n");
        } else {
            System.err.println
                ("Skipping FileStore check due to file system access failure");
        }
    }

    static void checkSupported(FileSystem fs, String... views) {
        for (String view: views) {
            check(fs.supportedFileAttributeViews().contains(view),
                "support for '" + view + "' expected");
        }
    }

    static void checkNoUOE() throws IOException, URISyntaxException {
        String dir = System.getProperty("test.dir", ".");
        String fileName = dir + File.separator + "foo.bar";
        Path path = Paths.get(fileName);
        Path file = Files.createFile(path);
        try {
            URI uri = new URI("jar", file.toUri().toString(), null);
            System.out.println(uri);
            FileSystem fs = FileSystems.newFileSystem(uri, new HashMap());
            fs.close();
        } catch (ProviderNotFoundException pnfe) {
            System.out.println("Expected ProviderNotFoundException caught: "
                + "\"" + pnfe.getMessage() + "\"");
        }
    }

    static void checkIAE() throws IOException {
        URI absoluteUri = Path.of("foo.bar").toUri();
        URI relativeUri = URI.create(absoluteUri.getSchemeSpecificPart());
        System.out.println(relativeUri);
        try {
            FileSystem fs = FileSystems.getFileSystem(relativeUri);
            throw new RuntimeException("IllegalArgumentException expected");
        } catch (IllegalArgumentException iae) {
            System.out.println("Expected IllegalArgumentException caught: "
                + "\"" + iae.getMessage() + "\"");
        } catch (Exception e) {
            throw new RuntimeException("IllegalArgumentException expected", e);
        }
    }

    public static void main(String[] args)
        throws IOException, URISyntaxException {
        String os = System.getProperty("os.name");
        FileSystem fs = FileSystems.getDefault();

        // close should throw UOE
        try {
            fs.close();
            throw new RuntimeException("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException e) { }
        check(fs.isOpen(), "should be open");

        check(!fs.isReadOnly(), "should provide read-write access");

        check(fs.provider().getScheme().equals("file"),
            "should use 'file' scheme");

        // sanity check FileStores
        checkFileStores(fs);

        // sanity check supportedFileAttributeViews
        checkSupported(fs, "basic");
        if (os.equals("Linux"))
            checkSupported(fs, "posix", "unix", "owner", "dos", "user");
        if (os.contains("OS X"))
            checkSupported(fs, "posix", "unix", "owner");
        if (os.equals("Windows"))
            checkSupported(fs, "owner", "dos", "acl", "user");

        // sanity check throwing of IllegalArgumentException by
        // FileSystems.getFileSystem(URI) if the URI scheme is null
        checkIAE();

        // sanity check non-throwing of UnsupportedOperationException by
        // FileSystems.newFileSystem(URI, ..)
        checkNoUOE();
    }
}
