/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8225037
 * @library /test/lib
 * @summary Basic test for java.net.JarURLConnection default behavior
 * @run testng/othervm TestDefaultBehavior
 */

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import jdk.test.lib.util.JarUtils;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class TestDefaultBehavior {

    // Disable caching and create three jar files:
    //   1. jar without a manifest
    //   2. jar with a manifest
    //   3. jar with manifest that includes an entry attribute
    @BeforeTest
    public void setup() throws Exception {
        URLConnection.setDefaultUseCaches("jar", false);
        URLConnection.setDefaultUseCaches("file", false);

        Path foo = Path.of("foo.txt");
        Files.writeString(foo, "Hello there");

        Files.createDirectory(Path.of("META-INF"));
        Path manifest = Path.of("META-INF/MANIFEST.MF");
        Files.writeString(manifest, "Manifest-Version: 5.5\n");

        JarUtils.createJarFile(Path.of("test.jar"), Path.of("."), foo);
        JarUtils.createJarFile(Path.of("testWithManifest.jar"), Path.of("."), manifest, foo);

        Files.writeString(manifest, "Manifest-Version: 7.7\n\n" +       // main-section
                                         "Name: foo.txt\nGreeting: true\n"); // individual-section
        JarUtils.createJarFile(Path.of("testWithManifestAndAttr.jar"), Path.of("."), manifest, foo);
    }

    @Test
    public void noEntry() throws Exception {
        URI fileURI = Path.of("test.jar").toUri();
        URL jarFileURL = URI.create("jar:" + fileURI + "!/").toURL();
        JarURLConnection jarURLConnection = new CustomJarURLConnection(jarFileURL);

        assertEquals(jarURLConnection.getAttributes(), null);
        assertEquals(jarURLConnection.getCertificates(), null);
        assertEquals(jarURLConnection.getEntryName(), null);
        assertEquals(jarURLConnection.getJarEntry(), null);
        assertNotNull(jarURLConnection.getJarFile());
        assertEquals(jarURLConnection.getJarFileURL(), fileURI.toURL());
        assertEquals(jarURLConnection.getMainAttributes(), null);
        assertEquals(jarURLConnection.getManifest(), null);
    }

    @Test
    public void withEntry() throws Exception {
        URI fileURI = Path.of("test.jar").toUri();
        URL jarFileURL = URI.create("jar:" + fileURI + "!/foo.txt").toURL();
        JarURLConnection jarURLConnection = new CustomJarURLConnection(jarFileURL);

        assertEquals(jarURLConnection.getAttributes(), null);
        assertEquals(jarURLConnection.getCertificates(), null);
        assertEquals(jarURLConnection.getEntryName(), "foo.txt");
        assertEquals(jarURLConnection.getJarEntry().getName(), "foo.txt");
        assertNotNull(jarURLConnection.getJarFile());
        assertEquals(jarURLConnection.getJarFileURL(), fileURI.toURL());
        assertEquals(jarURLConnection.getMainAttributes(), null);
        assertEquals(jarURLConnection.getManifest(), null);
    }

    @Test
    public void manifestNoEntry() throws Exception {
        URI fileURI = Path.of("testWithManifest.jar").toUri();
        URL jarFileURL = URI.create("jar:" + fileURI + "!/").toURL();
        JarURLConnection jarURLConnection = new CustomJarURLConnection(jarFileURL);

        assertEquals(jarURLConnection.getAttributes(), null);
        assertEquals(jarURLConnection.getCertificates(), null);
        assertEquals(jarURLConnection.getEntryName(), null);
        assertEquals(jarURLConnection.getJarEntry(), null);
        assertNotNull(jarURLConnection.getJarFile());
        assertEquals(jarURLConnection.getJarFileURL(), fileURI.toURL());
        assertEquals(jarURLConnection.getMainAttributes().getValue("Manifest-Version"), "5.5");
        assertNotNull(jarURLConnection.getManifest());
    }

    @Test
    public void manifestWithEntry() throws Exception {
        URI fileURI = Path.of("testWithManifest.jar").toUri();
        URL jarFileURL = URI.create("jar:" + fileURI + "!/foo.txt").toURL();
        JarURLConnection jarURLConnection = new CustomJarURLConnection(jarFileURL);

        assertEquals(jarURLConnection.getAttributes(), null);
        assertEquals(jarURLConnection.getCertificates(), null);
        assertEquals(jarURLConnection.getEntryName(), "foo.txt");
        assertEquals(jarURLConnection.getJarEntry().getName(), "foo.txt");
        assertNotNull(jarURLConnection.getJarFile());
        assertEquals(jarURLConnection.getJarFileURL(), fileURI.toURL());
        assertEquals(jarURLConnection.getMainAttributes().getValue("Manifest-Version"), "5.5");
        assertNotNull(jarURLConnection.getManifest());
    }

    @Test
    public void manifestNoEntryAttr() throws Exception {
        URI fileURI = Path.of("testWithManifestAndAttr.jar").toUri();
        URL jarFileURL = URI.create("jar:" + fileURI + "!/").toURL();
        JarURLConnection jarURLConnection = new CustomJarURLConnection(jarFileURL);

        assertEquals(jarURLConnection.getAttributes(), null);
        assertEquals(jarURLConnection.getCertificates(), null);
        assertEquals(jarURLConnection.getEntryName(), null);
        assertEquals(jarURLConnection.getJarEntry(), null);
        assertNotNull(jarURLConnection.getJarFile());
        assertEquals(jarURLConnection.getJarFileURL(), fileURI.toURL());
        assertEquals(jarURLConnection.getMainAttributes().getValue("Manifest-Version"), "7.7");
        assertNotNull(jarURLConnection.getManifest());
    }

    @Test
    public void manifestWithEntryAttr() throws Exception {
        URI fileURI = Path.of("testWithManifestAndAttr.jar").toUri();
        URL jarFileURL = URI.create("jar:" + fileURI + "!/foo.txt").toURL();
        JarURLConnection jarURLConnection = new CustomJarURLConnection(jarFileURL);

        assertEquals(jarURLConnection.getAttributes().getValue("Greeting"), "true");
        assertEquals(jarURLConnection.getCertificates(), null);
        assertEquals(jarURLConnection.getEntryName(), "foo.txt");
        assertEquals(jarURLConnection.getJarEntry().getName(), "foo.txt");
        assertNotNull(jarURLConnection.getJarFile());
        assertEquals(jarURLConnection.getJarFileURL(), fileURI.toURL());
        assertEquals(jarURLConnection.getMainAttributes().getValue("Manifest-Version"), "7.7");
        assertNotNull(jarURLConnection.getManifest());
    }

    // A minimal JarURLConnection
    static class CustomJarURLConnection extends JarURLConnection {
        private final URL jarFileURL;
        private JarFile jarFile;

        CustomJarURLConnection(URL url) throws MalformedURLException {
            super(url);
            jarFileURL = url;
        }

        @Override
        public JarFile getJarFile() throws IOException {
            if (jarFile == null)
                connect();
            return jarFile;
        }

        @Override
        public void connect() throws IOException {
            jarFile = ((JarURLConnection)jarFileURL.openConnection()).getJarFile();
        }
    }
}
