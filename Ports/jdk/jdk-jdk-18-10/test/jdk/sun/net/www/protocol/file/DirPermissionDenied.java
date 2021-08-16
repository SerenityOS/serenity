/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6977851
 * @summary NPE from FileURLConnection.connect
 * @library /test/lib
 * @build DirPermissionDenied jdk.test.lib.process.*
 *        jdk.test.lib.util.FileUtils
 * @run testng DirPermissionDenied
 */

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.util.FileUtils;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeTest;
public class DirPermissionDenied {
    private static final Path TEST_DIR = Paths.get(
            "DirPermissionDeniedDirectory");

    @Test
    public void doTest() throws MalformedURLException {
        URL url = new URL(TEST_DIR.toUri().toString());
        try {
            URLConnection uc = url.openConnection();
            uc.connect();
        } catch (IOException e) {
            // OK
        } catch (Exception e) {
            throw new RuntimeException("Failed " + e);
        }

        try {
            URLConnection uc = url.openConnection();
            uc.getInputStream();
        } catch (IOException e) {
            // OK
        } catch (Exception e) {
            throw new RuntimeException("Failed " + e);
        }

        try {
            URLConnection uc = url.openConnection();
            uc.getContentLengthLong();
        } catch (IOException e) {
            // OK
        } catch (Exception e) {
            throw new RuntimeException("Failed " + e);
        }
    }

    @BeforeTest
    public void setup() throws Throwable {
        // mkdir and chmod "333"
        Files.createDirectories(TEST_DIR);
        ProcessTools.executeCommand("chmod", "333", TEST_DIR.toString())
                    .outputTo(System.out)
                    .errorTo(System.out)
                    .shouldHaveExitValue(0);
    }

    @AfterTest
    public void tearDown() throws Throwable {
        // add read permission to ensure the dir removable
        ProcessTools.executeCommand("chmod", "733", TEST_DIR.toString())
                    .outputTo(System.out)
                    .errorTo(System.out)
                    .shouldHaveExitValue(0);
        FileUtils.deleteFileIfExistsWithRetry(TEST_DIR);
    }
}
