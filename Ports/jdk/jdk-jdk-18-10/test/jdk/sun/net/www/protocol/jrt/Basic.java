/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Basic test of jimage protocol handler
 * @run testng Basic
 */

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class Basic {

    @DataProvider(name = "urls")
    public Object[][] urls() {
        Object[][] data = {
            { "jrt:/java.base/java/lang/Object.class",    true },
            { "jrt:/java.desktop/java/lang/Object.class", false },
        };
        return data;
    }

    @Test(dataProvider = "urls")
    public void testConnect(String urlString, boolean exists) throws Exception {
        URL url = new URL(urlString);
        URLConnection uc = url.openConnection();
        try {
            uc.connect();
            if (!exists) fail("IOException expected");
        } catch (IOException ioe) {
            if (exists) fail("IOException not expected");
        }
    }

    @Test(dataProvider = "urls")
    public void testInputStream(String urlString, boolean exists) throws Exception {
        URL url = new URL(urlString);
        URLConnection uc = url.openConnection();
        try {
            int b = uc.getInputStream().read();
            assertTrue(b != -1);
            if (!exists) fail("IOException expected");
        } catch (IOException ioe) {
            if (exists) fail("IOException not expected");
        }
    }

    @Test(dataProvider = "urls")
    public void testContentLength(String urlString, boolean exists) throws Exception {
        URL url = new URL(urlString);
        int len = url.openConnection().getContentLength();
        assertTrue((exists && len > 0) || (!exists && len == -1));
    }

    @Test(dataProvider = "urls")
    public void testGetContent(String urlString, boolean exists) throws Exception {
        URL url = new URL(urlString);
        try {
            Object obj = url.getContent();
            assertTrue(obj != null);
            if (!exists) fail("IOException expected");
        } catch (IOException ioe) {
            if (exists) fail("IOException not expected");
        }
    }
}
