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
 * @summary Unit test for java.nio.file.attribute.DosFileAttributeView
 * @library ../..
 */

import java.nio.file.*;
import static java.nio.file.LinkOption.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.io.IOException;

public class Basic {

    static void check(boolean okay) {
        if (!okay)
            throw new RuntimeException("Test failed");
    }

    // exercise each setter/getter method, leaving all attributes unset
    static void testAttributes(DosFileAttributeView view) throws IOException {
        view.setReadOnly(true);
        check(view.readAttributes().isReadOnly());
        view.setReadOnly(false);
        check(!view.readAttributes().isReadOnly());
        view.setHidden(true);
        check(view.readAttributes().isHidden());
        view.setHidden(false);
        check(!view.readAttributes().isHidden());
        view.setArchive(true);
        check(view.readAttributes().isArchive());
        view.setArchive(false);
        check(!view.readAttributes().isArchive());
        view.setSystem(true);
        check(view.readAttributes().isSystem());
        view.setSystem(false);
        check(!view.readAttributes().isSystem());
    }

    // set the value of all attributes
    static void setAll(DosFileAttributeView view, boolean value)
        throws IOException
    {
        view.setReadOnly(value);
        view.setHidden(value);
        view.setArchive(value);
        view.setSystem(value);
    }

    // read and write FAT attributes
    static void readWriteTests(Path dir) throws IOException {

        // create "foo" and test that we can read/write each FAT attribute
        Path file = Files.createFile(dir.resolve("foo"));
        try {
            testAttributes(Files.getFileAttributeView(file, DosFileAttributeView.class));

            // Following tests use a symbolic link so skip if not supported
            if (!TestUtil.supportsLinks(dir))
                return;

            Path link = dir.resolve("link");
            Files.createSymbolicLink(link, file);

            // test following links
            testAttributes(Files.getFileAttributeView(link, DosFileAttributeView.class));

            // test not following links
            try {
                try {
                    testAttributes(Files
                        .getFileAttributeView(link, DosFileAttributeView.class, NOFOLLOW_LINKS));
                } catch (IOException x) {
                    // access to link attributes not supported
                    return;
                }

                // set all attributes on link
                // run test on target of link (which leaves them all un-set)
                // check that attributes of link remain all set
                setAll(Files
                    .getFileAttributeView(link, DosFileAttributeView.class, NOFOLLOW_LINKS), true);
                testAttributes(Files
                    .getFileAttributeView(link, DosFileAttributeView.class));
                DosFileAttributes attrs =
                    Files.getFileAttributeView(link, DosFileAttributeView.class, NOFOLLOW_LINKS)
                         .readAttributes();
                check(attrs.isReadOnly());
                check(attrs.isHidden());
                check(attrs.isArchive());
                check(attrs.isSystem());
                setAll(Files
                    .getFileAttributeView(link, DosFileAttributeView.class, NOFOLLOW_LINKS), false);

                // set all attributes on target
                // run test on link (which leaves them all un-set)
                // check that attributes of target remain all set
                setAll(Files.getFileAttributeView(link, DosFileAttributeView.class), true);
                testAttributes(Files
                    .getFileAttributeView(link, DosFileAttributeView.class, NOFOLLOW_LINKS));
                attrs = Files.getFileAttributeView(link, DosFileAttributeView.class).readAttributes();
                check(attrs.isReadOnly());
                check(attrs.isHidden());
                check(attrs.isArchive());
                check(attrs.isSystem());
                setAll(Files.getFileAttributeView(link, DosFileAttributeView.class), false);
            } finally {
                TestUtil.deleteUnchecked(link);
            }
        } finally {
            TestUtil.deleteUnchecked(file);
        }
    }

    public static void main(String[] args) throws IOException {
        // create temporary directory to run tests
        Path dir = TestUtil.createTemporaryDirectory();

        try {
            // skip test if DOS file attributes not supported
            if (!Files.getFileStore(dir).supportsFileAttributeView("dos")) {
                System.out.println("DOS file attribute not supported.");
                return;
            }
            readWriteTests(dir);
        } finally {
            TestUtil.removeAll(dir);
        }
    }
}
