/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @run testng StreamZipEntriesTest
 * @summary Make sure we can stream entries of a zip file.
 */

import java.io.File;
import java.io.IOException;
import java.lang.Object;
import java.lang.System;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class StreamZipEntriesTest {

    @Test
    public void testStreamZip() throws IOException {
        try (ZipFile zf = new ZipFile(new File(System.getProperty("test.src", "."), "input.zip"))) {
            zf.stream().forEach(e -> assertTrue(e instanceof ZipEntry));
            zf.stream().forEach(e -> assertEquals(e.toString(), "ReadZip.java"));

            Object elements[] = zf.stream().toArray();
            assertEquals(1, elements.length);
            assertEquals(elements[0].toString(), "ReadZip.java");
        }
    }

    @Test
    public void testStreamJar() throws IOException {
        try (JarFile jf = new JarFile(new File(System.getProperty("test.src", "."), "input.jar"))) {
            jf.stream().forEach(e -> assertTrue(e instanceof JarEntry));

            Object elements[] = jf.stream().toArray();
            assertEquals(3, elements.length);
            assertEquals(elements[0].toString(), "META-INF/");
            assertEquals(elements[1].toString(), "META-INF/MANIFEST.MF");
            assertEquals(elements[2].toString(), "ReleaseInflater.java");
        }
    }

    @Test
    public void testClosedZipFile() throws IOException {
        ZipFile zf = new ZipFile(new File(System.getProperty("test.src", "."), "input.zip"));
        zf.close();
        try {
            Stream s = zf.stream();
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException e) {
            // expected;
        }
    }

    @Test
    public void testClosedJarFile() throws IOException {
        JarFile jf = new JarFile(new File(System.getProperty("test.src", "."), "input.jar"));
        jf.close();
        try {
            Stream s = jf.stream();
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException e) {
            // expected;
        }
    }
}
