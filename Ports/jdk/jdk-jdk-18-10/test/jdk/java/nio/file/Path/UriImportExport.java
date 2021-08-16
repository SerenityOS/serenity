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
 * @bug 4313887 7003155
 * @summary Unit test for java.nio.file.Path
 */

import java.nio.file.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.PrintStream;

public class UriImportExport {

    static final PrintStream log = System.out;
    static int failures = 0;

    /**
     * Test Path -> URI -> Path
     */
    static void testPath(String s) {
        Path path = Paths.get(s);
        log.println(path);
        URI uri = path.toUri();
        log.println("  --> " + uri);
        Path result = Paths.get(uri);
        log.println("  --> " + result);
        if (!result.equals(path.toAbsolutePath())) {
            log.println("FAIL: Expected " + path + ", got " + result);
            failures++;
        }
        log.println();
    }

    /**
     * Test Path -> (expected) URI -> Path
     */
    static void testPath(String s, String expectedUri) {
        Path path = Paths.get(s);
        log.println(path);
        URI uri = path.toUri();
        log.println("  --> " + uri);
        if (!uri.toString().equals(expectedUri)) {
            log.println("FAILED: Expected " + expectedUri + ", got " + uri);
            failures++;
            return;
        }
        Path result = Paths.get(uri);
        log.println("  --> " + result);
        if (!result.equals(path.toAbsolutePath())) {
            log.println("FAIL: Expected " + path + ", got " + result);
            failures++;
        }
        log.println();
    }

    /**
     * Test URI -> Path -> URI
     */
    static void testUri(String s) throws Exception {
        URI uri = URI.create(s);
        log.println(uri);
        Path path = Paths.get(uri);
        log.println("  --> " + path);
        URI result = path.toUri();
        log.println("  --> " + result);
        if (!result.equals(uri)) {
            log.println("FAIL: Expected " + uri + ", got " + result);
            failures++;
        }
        log.println();
    }

    /**
     * Test URI -> Path fails with IllegalArgumentException
     */
    static void testBadUri(String s) throws Exception {
        URI uri = URI.create(s);
        log.println(uri);
        try {
            Path path = Paths.get(uri);
            log.format(" --> %s  FAIL: Expected IllegalArgumentException\n", path);
            failures++;
        } catch (IllegalArgumentException expected) {
            log.println("  --> IllegalArgumentException (expected)");
        }
        log.println();
    }

    public static void main(String[] args) throws Exception {
        testBadUri("file:foo");
        testBadUri("file:/foo?q");
        testBadUri("file:/foo#f");

        String osname = System.getProperty("os.name");
        if (osname.startsWith("Windows")) {
            testPath("C:\\doesnotexist");
            testPath("C:doesnotexist");
            testPath("\\\\server.nowhere.oracle.com\\share\\");
            testPath("\\\\fe80--203-baff-fe5a-749ds1.ipv6-literal.net\\share\\missing",
                "file://[fe80::203:baff:fe5a:749d%1]/share/missing");
        } else {
            testPath("doesnotexist");
            testPath("/doesnotexist");
            testPath("/does not exist");
            testUri("file:///");
            testUri("file:///foo/bar/doesnotexist");
            testUri("file:/foo/bar/doesnotexist");

            // file:///foo/bar/\u0440\u0443\u0441\u0441\u043A\u0438\u0439 (Russian)
            testUri("file:///foo/bar/%D1%80%D1%83%D1%81%D1%81%D0%BA%D0%B8%D0%B9");

            // invalid
            testBadUri("file:foo");
            testBadUri("file://server/foo");
            testBadUri("file:///foo%00");
        }

        if (failures > 0)
            throw new RuntimeException(failures + " test(s) failed");
    }
}
