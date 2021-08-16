/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6595866
 * @summary Test java.io.File operations with sym links
 * @build SymLinks Util
 * @run main SymLinks
 */

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import static java.nio.file.LinkOption.*;

public class SymLinks {
    static final PrintStream out = System.out;

    static final File top = new File(System.getProperty("test.dir", "."));

    // files used by the test

    static final File file              = new File(top, "foofile");
    static final File link2file         = new File(top, "link2file");
    static final File link2link2file    = new File(top, "link2link2file");

    static final File dir               = new File(top, "foodir");
    static final File link2dir          = new File(top, "link2dir");
    static final File link2link2dir     = new File(top, "link2link2dir");

    static final File link2nobody       = new File(top, "link2nobody");
    static final File link2link2nobody  = new File(top, "link2link2nobody");

    /**
     * Setup files, directories, and sym links used by test.
     */
    static void setup() throws IOException {
        // link2link2file -> link2file -> foofile
        FileOutputStream fos = new FileOutputStream(file);
        try {
            fos.write(new byte[16*1024]);
        } finally {
            fos.close();
        }
        mklink(link2file, file);
        mklink(link2link2file, link2file);

        // link2link2dir -> link2dir -> dir
        assertTrue(dir.mkdir());
        mklink(link2dir, dir);
        mklink(link2link2dir, link2dir);

        // link2link2nobody -> link2nobody -> <does-not-exist>
        mklink(link2nobody, new File(top, "DoesNotExist"));
        mklink(link2link2nobody, link2nobody);
    }

    /**
     * Remove files, directories, and sym links used by test.
     */
    static void cleanup() throws IOException {
        if (file != null)
            file.delete();
        if (link2file != null)
            Files.deleteIfExists(link2file.toPath());
        if (link2link2file != null)
            Files.deleteIfExists(link2link2file.toPath());
        if (dir != null)
            dir.delete();
        if (link2dir != null)
            Files.deleteIfExists(link2dir.toPath());
        if (link2link2dir != null)
            Files.deleteIfExists(link2link2dir.toPath());
        if (link2nobody != null)
            Files.deleteIfExists(link2nobody.toPath());
        if (link2link2nobody != null)
            Files.deleteIfExists(link2link2nobody.toPath());
    }

    /**
     * Creates a sym link source->target
     */
    static void mklink(File source, File target) throws IOException {
        Files.createSymbolicLink(source.toPath(), target.toPath());
    }

    /**
     * Returns true if the "link" exists and is a sym link.
     */
    static boolean isSymLink(File link) {
         return Files.isSymbolicLink(link.toPath());
    }

    /**
     * Returns the last modified time of a sym link.
     */
    static long lastModifiedOfSymLink(File link) throws IOException {
        BasicFileAttributes attrs =
            Files.readAttributes(link.toPath(), BasicFileAttributes.class, NOFOLLOW_LINKS);
        assertTrue(attrs.isSymbolicLink());
        return attrs.lastModifiedTime().toMillis();
    }

    /**
     * Returns true if sym links are supported on the file system where
     * "dir" exists.
     */
    static boolean supportsSymLinks(File dir) {
        Path link = dir.toPath().resolve("link");
        Path target = dir.toPath().resolve("target");
        try {
            Files.createSymbolicLink(link, target);
            Files.delete(link);
            return true;
        } catch (UnsupportedOperationException x) {
            return false;
        } catch (IOException x) {
            return false;
        }
    }

    static void assertTrue(boolean v) {
        if (!v) throw new RuntimeException("Test failed");
    }

    static void assertFalse(boolean v) {
        assertTrue(!v);
    }

    static void header(String h) {
        out.println();
        out.println();
        out.println("-- " + h + " --");
    }

    /**
     * Tests go here.
     */
    static void go() throws IOException {

        // check setup
        assertTrue(file.isFile());
        assertTrue(isSymLink(link2file));
        assertTrue(isSymLink(link2link2file));
        assertTrue(dir.isDirectory());
        assertTrue(isSymLink(link2dir));
        assertTrue(isSymLink(link2link2dir));
        assertTrue(isSymLink(link2nobody));
        assertTrue(isSymLink(link2link2nobody));

        header("createNewFile");

        assertFalse(file.createNewFile());
        assertFalse(link2file.createNewFile());
        assertFalse(link2link2file.createNewFile());
        assertFalse(dir.createNewFile());
        assertFalse(link2dir.createNewFile());
        assertFalse(link2link2dir.createNewFile());
        assertFalse(link2nobody.createNewFile());
        assertFalse(link2link2nobody.createNewFile());

        header("mkdir");

        assertFalse(file.mkdir());
        assertFalse(link2file.mkdir());
        assertFalse(link2link2file.mkdir());
        assertFalse(dir.mkdir());
        assertFalse(link2dir.mkdir());
        assertFalse(link2link2dir.mkdir());
        assertFalse(link2nobody.mkdir());
        assertFalse(link2link2nobody.mkdir());

        header("delete");

        File link = new File(top, "mylink");
        try {
            mklink(link, file);
            assertTrue(link.delete());
            assertTrue(!isSymLink(link));
            assertTrue(file.exists());

            mklink(link, link2file);
            assertTrue(link.delete());
            assertTrue(!isSymLink(link));
            assertTrue(link2file.exists());

            mklink(link, dir);
            assertTrue(link.delete());
            assertTrue(!isSymLink(link));
            assertTrue(dir.exists());

            mklink(link, link2dir);
            assertTrue(link.delete());
            assertTrue(!isSymLink(link));
            assertTrue(link2dir.exists());

            mklink(link, link2nobody);
            assertTrue(link.delete());
            assertTrue(!isSymLink(link));
            assertTrue(isSymLink(link2nobody));

        } finally {
            Files.deleteIfExists(link.toPath());
        }

        header("renameTo");

        File newlink = new File(top, "newlink");
        assertTrue(link2file.renameTo(newlink));
        try {
            assertTrue(file.exists());
            assertTrue(isSymLink(newlink));
            assertTrue(!isSymLink(link2file));
        } finally {
            newlink.renameTo(link2file);  // restore link
        }

        assertTrue(link2dir.renameTo(newlink));
        try {
            assertTrue(dir.exists());
            assertTrue(isSymLink(newlink));
            assertTrue(!isSymLink(link2dir));
        } finally {
            newlink.renameTo(link2dir);  // restore link
        }

        header("list");

        final String name = "entry";
        File entry = new File(dir, name);
        try {
            assertTrue(dir.list().length == 0);   // directory should be empty
            assertTrue(link2dir.list().length == 0);
            assertTrue(link2link2dir.list().length == 0);

            assertTrue(entry.createNewFile());
            assertTrue(dir.list().length == 1);
            assertTrue(dir.list()[0].equals(name));

            // access directory by following links
            assertTrue(link2dir.list().length == 1);
            assertTrue(link2dir.list()[0].equals(name));
            assertTrue(link2link2dir.list().length == 1);
            assertTrue(link2link2dir.list()[0].equals(name));

            // files that are not directories
            assertTrue(link2file.list() == null);
            assertTrue(link2nobody.list() == null);

        } finally {
            entry.delete();
        }

        header("isXXX");

        assertTrue(file.isFile());
        assertTrue(link2file.isFile());
        assertTrue(link2link2file.isFile());

        assertTrue(dir.isDirectory());
        assertTrue(link2dir.isDirectory());
        assertTrue(link2link2dir.isDirectory());

        // on Windows we test with the DOS hidden attribute set
        if (System.getProperty("os.name").startsWith("Windows")) {
            DosFileAttributeView view = Files
                .getFileAttributeView(file.toPath(), DosFileAttributeView.class);
            view.setHidden(true);
            try {
                assertTrue(file.isHidden());
                assertTrue(link2file.isHidden());
                assertTrue(link2link2file.isHidden());
            } finally {
                view.setHidden(false);
            }
            assertFalse(file.isHidden());
            assertFalse(link2file.isHidden());
            assertFalse(link2link2file.isHidden());
        }

        header("length");

        long len = file.length();
        assertTrue(len > 0L);
        // these tests should follow links
        assertTrue(link2file.length() == len);
        assertTrue(link2link2file.length() == len);
        assertTrue(link2nobody.length() == 0L);

        header("lastModified / setLastModified");

        // need time to diff between link and file
        long origLastModified = file.lastModified();
        assertTrue(origLastModified != 0L);
        try { Thread.sleep(2000); } catch (InterruptedException x) { }
        file.setLastModified(System.currentTimeMillis());

        long lastModified = file.lastModified();
        assertTrue(lastModified != origLastModified);
        assertTrue(lastModifiedOfSymLink(link2file) != lastModified);
        assertTrue(lastModifiedOfSymLink(link2link2file) != lastModified);
        assertTrue(link2file.lastModified() == lastModified);
        assertTrue(link2link2file.lastModified() == lastModified);
        assertTrue(link2nobody.lastModified() == 0L);

        origLastModified = dir.lastModified();
        assertTrue(origLastModified != 0L);
        dir.setLastModified(0L);
        assertTrue(dir.lastModified() == 0L);
        assertTrue(link2dir.lastModified() == 0L);
        assertTrue(link2link2dir.lastModified() == 0L);
        dir.setLastModified(origLastModified);

        header("setXXX / canXXX");

        assertTrue(file.canRead());
        assertTrue(file.canWrite());
        assertTrue(link2file.canRead());
        assertTrue(link2file.canWrite());
        assertTrue(link2link2file.canRead());
        assertTrue(link2link2file.canWrite());

        if (!Util.isPrivileged() && file.setReadOnly()) {
            assertFalse(file.canWrite());
            assertFalse(link2file.canWrite());
            assertFalse(link2link2file.canWrite());

            assertTrue(file.setWritable(true));             // make writable
            assertTrue(file.canWrite());
            assertTrue(link2file.canWrite());
            assertTrue(link2link2file.canWrite());

            assertTrue(link2file.setReadOnly());            // make read only
            assertFalse(file.canWrite());
            assertFalse(link2file.canWrite());
            assertFalse(link2link2file.canWrite());

            assertTrue(link2link2file.setWritable(true));   // make writable
            assertTrue(file.canWrite());
            assertTrue(link2file.canWrite());
            assertTrue(link2link2file.canWrite());
        }
    }

    public static void main(String[] args) throws IOException {
        if (supportsSymLinks(top)) {
            try {
                setup();
                go();
            } finally {
                cleanup();
            }
        }
    }

}
