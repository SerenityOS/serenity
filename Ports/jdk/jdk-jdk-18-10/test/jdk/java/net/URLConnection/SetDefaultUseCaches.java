/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8163449 8175261
 * @summary Allow per protocol setting for URLConnection defaultUseCaches
 * @run testng/othervm SetDefaultUseCaches
 */

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLConnection;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class SetDefaultUseCaches {

    final URL fileURL = uncheckURL("file:///a/b.txt");
    final URL httpURL = uncheckURL("http://www.foo.com/");
    final URL jarFileURL = uncheckURL("jar:file:///a/b.jar!/anEntry");
    final URL jarHttpURL = uncheckURL("jar:http://www.foo.com/a/b.jar!/anEntry");

    @Test
    public void test() throws Exception {
        // check JAR both before and after other protocol tests as JAR URLs
        // effectively wrap/embed other URLs. The syntax is jar:<url>!/{entry}
        checkJAR(true);
        checkJAR(false);
        checkJAR(true);

        checkHTTP();
        checkFile();

        // ensure that JAR URLs still respect their per-protocol value
        checkJAR(false);
        checkJAR(true);
        checkJAR(false);
    }

    void checkHTTP() throws IOException {
        // check default default is true
        URLConnection httpURLConn = httpURL.openConnection();
        assertTrue(httpURLConn.getDefaultUseCaches());

        // set default for http to false and check
        URLConnection.setDefaultUseCaches("HTTP", false);

        httpURLConn = httpURL.openConnection();
        assertTrue(httpURLConn.getDefaultUseCaches());
        assertFalse(httpURLConn.getUseCaches());
        assertFalse(URLConnection.getDefaultUseCaches("http"));
    }

    void checkFile() throws IOException {
        URLConnection fileURLConn = fileURL.openConnection();
        assertTrue(fileURLConn.getDefaultUseCaches());

        // set default default to false and check other values the same
        fileURLConn.setDefaultUseCaches(false);
        fileURLConn.setDefaultUseCaches("fiLe", true);
        assertFalse(fileURLConn.getDefaultUseCaches());
        assertTrue(URLConnection.getDefaultUseCaches("fiLE"));
    }

    void checkJAR(boolean defaultValue) throws IOException {
        URLConnection.setDefaultUseCaches("JAR", defaultValue);
        assertEquals(URLConnection.getDefaultUseCaches("JAr"), defaultValue);

        URLConnection jarFileURLConn = jarFileURL.openConnection();
        URLConnection jarHttpURLConn = jarHttpURL.openConnection();
        assertEquals(jarFileURLConn.getUseCaches(), defaultValue);
        assertEquals(jarHttpURLConn.getUseCaches(), defaultValue);
        jarFileURLConn.setUseCaches(!defaultValue);
        jarHttpURLConn.setUseCaches(!defaultValue);
        assertEquals(jarFileURLConn.getUseCaches(), !defaultValue);
        assertEquals(jarHttpURLConn.getUseCaches(), !defaultValue);

        URLConnection.setDefaultUseCaches("JaR", !defaultValue); // case-insensitive
        assertEquals(URLConnection.getDefaultUseCaches("jAR"), !defaultValue);

        jarFileURLConn = jarFileURL.openConnection();
        jarHttpURLConn = jarHttpURL.openConnection();
        assertEquals(jarFileURLConn.getUseCaches(), !defaultValue);
        assertEquals(jarHttpURLConn.getUseCaches(), !defaultValue);
        jarFileURLConn.setUseCaches(defaultValue);
        jarHttpURLConn.setUseCaches(defaultValue);
        assertEquals(jarFileURLConn.getUseCaches(), defaultValue);
        assertEquals(jarHttpURLConn.getUseCaches(), defaultValue);
    }

    static URL uncheckURL(String url) {
        try { return new URL(url); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
}
