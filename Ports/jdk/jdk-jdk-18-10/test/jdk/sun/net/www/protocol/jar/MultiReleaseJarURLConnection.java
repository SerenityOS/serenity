/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8132734 8144062 8159785 8194070
 * @summary Test that URL connections to multi-release jars can be runtime versioned
 * @library /lib/testlibrary/java/util/jar /test/lib
 * @modules jdk.compiler
 *          jdk.httpserver
 *          jdk.jartool
 * @build CreateMultiReleaseTestJars
 *        jdk.test.lib.net.SimpleHttpServer
 *        jdk.test.lib.util.JarBuilder
 *        jdk.test.lib.compiler.Compiler
 * @run testng MultiReleaseJarURLConnection
 */

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.JarFile;

import jdk.test.lib.net.SimpleHttpServer;
import jdk.test.lib.net.URIBuilder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class MultiReleaseJarURLConnection {
    String userdir = System.getProperty("user.dir", ".");
    String unversioned = userdir + "/unversioned.jar";
    String unsigned = userdir + "/multi-release.jar";
    String signed = userdir + "/signed-multi-release.jar";
    static final String TESTCONTEXT = "/multi-release.jar";
    SimpleHttpServer server;

    @BeforeClass
    public void initialize() throws Exception {
        CreateMultiReleaseTestJars creator = new CreateMultiReleaseTestJars();
        creator.compileEntries();
        creator.buildUnversionedJar();
        creator.buildMultiReleaseJar();
        creator.buildSignedMultiReleaseJar();
        server = new SimpleHttpServer(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), TESTCONTEXT, System.getProperty("user.dir", "."));
        server.start();
    }

    @AfterClass
    public void close() throws IOException {
        // Windows requires server to stop before file is deleted
        if (server != null)
            server.stop();
        Files.delete(Paths.get(unversioned));
        Files.delete(Paths.get(unsigned));
        Files.delete(Paths.get(signed));
    }

    @DataProvider(name = "data")
    public Object[][] createData() {
        return new Object[][]{
                {"unversioned", unversioned},
                {"unsigned", unsigned},
                {"signed", signed}
        };
    }

    @Test(dataProvider = "data")
    public void testRuntimeVersioning(String style, String file) throws Exception {
        String urlFile = "jar:file:" + file + "!/";
        String baseUrlEntry = urlFile + "version/Version.java";
        String rtreturn = "return " + Runtime.version().major();

        Assert.assertTrue(readAndCompare(new URL(baseUrlEntry), "return 8"));
        // #runtime is "magic" for a multi-release jar, but not for unversioned jar
        Assert.assertTrue(readAndCompare(new URL(baseUrlEntry + "#runtime"),
                style.equals("unversioned") ? "return 8" : rtreturn));
        // #fragment or any other fragment is not magic
        Assert.assertTrue(readAndCompare(new URL(baseUrlEntry + "#fragment"), "return 8"));
        // cached entities not affected
        Assert.assertTrue(readAndCompare(new URL(baseUrlEntry), "return 8"));

        // the following tests will not work with unversioned jars
        if (style.equals("unversioned"))
            return;

        // direct access to versioned entry
        String versUrlEntry = urlFile + "META-INF/versions/" + Runtime.version().major()
                + "/version/Version.java";
        Assert.assertTrue(readAndCompare(new URL(versUrlEntry), rtreturn));
        // adding any fragment does not change things
        Assert.assertTrue(readAndCompare(new URL(versUrlEntry + "#runtime"), rtreturn));
        Assert.assertTrue(readAndCompare(new URL(versUrlEntry + "#fragment"), rtreturn));
    }

    @Test(dataProvider = "data")
    public void testCachedJars(String style, String file) throws Exception {
        String urlFile = "jar:file:" + file + "!/";

        URL rootUrl = new URL(urlFile);
        JarURLConnection juc = (JarURLConnection) rootUrl.openConnection();
        JarFile rootJar = juc.getJarFile();
        Runtime.Version root = rootJar.getVersion();

        URL runtimeUrl = new URL(urlFile + "#runtime");
        juc = (JarURLConnection) runtimeUrl.openConnection();
        JarFile runtimeJar = juc.getJarFile();
        Runtime.Version runtime = runtimeJar.getVersion();
        if (style.equals("unversioned")) {
            Assert.assertEquals(root, runtime);
        } else {
            Assert.assertNotEquals(root, runtime);
        }

        juc = (JarURLConnection) rootUrl.openConnection();
        JarFile jar = juc.getJarFile();
        Assert.assertEquals(jar.getVersion(), root);
        Assert.assertEquals(jar, rootJar);

        juc = (JarURLConnection) runtimeUrl.openConnection();
        jar = juc.getJarFile();
        Assert.assertEquals(jar.getVersion(), runtime);
        Assert.assertEquals(jar, runtimeJar);

        rootJar.close();
        runtimeJar.close();
        jar.close(); // probably not needed
    }

    @DataProvider(name = "resourcedata")
    public Object[][] createResourceData() throws Exception {
        return new Object[][]{
                {"unversioned", Paths.get(unversioned).toUri().toURL()},
                {"unsigned", Paths.get(unsigned).toUri().toURL()},
                {"signed", Paths.get(signed).toUri().toURL()},
                {"unversioned", new URL("file:" + unversioned)},
                {"unsigned", new URL("file:" + unsigned)},
                {"signed", new URL("file:" + signed)},
                {"unversioned", new URL("jar:file:" + unversioned + "!/")},
                {"unsigned", new URL("jar:file:" + unsigned + "!/")},
                {"signed", new URL("jar:file:" + signed + "!/")},
                // external jar received via http protocol
                {"http", toHttpJarURL(server.getPort(), "/multi-release.jar", "!/")},
                {"http", URIBuilder.newBuilder().scheme("http").port(server.getPort())
                        .loopback().path("/multi-release.jar").toURL()},
        };
    }

    @Test(dataProvider = "resourcedata")
    public void testResources(String style, URL url) throws Throwable {
        // System.out.println("  testing " + style + " url: " + url);
        URL[] urls = {url};
        URLClassLoader cldr = new URLClassLoader(urls);
        Class<?> vcls = cldr.loadClass("version.Version");

        // verify we are loading a runtime versioned class
        MethodType mt = MethodType.methodType(int.class);
        MethodHandle mh = MethodHandles.lookup().findVirtual(vcls, "getVersion", mt);
        Assert.assertEquals((int)mh.invoke(vcls.newInstance()),
                style.equals("unversioned") ? 8 : Runtime.version().major());

        // now get a resource and verify that we don't have a fragment attached
        Enumeration<URL> vclsUrlEnum = cldr.getResources("version/Version.class");
        Assert.assertTrue(vclsUrlEnum.hasMoreElements());
        URL vclsUrls[] = new URL[] {
            vcls.getResource("/version/Version.class"),
            vcls.getResource("Version.class"),
            cldr.getResource("version/Version.class"),
            vclsUrlEnum.nextElement()
        };
        Assert.assertFalse(vclsUrlEnum.hasMoreElements());
        for (URL vclsUrl : vclsUrls) {
            String fragment = vclsUrl.getRef();
            Assert.assertNull(fragment);

            // and verify that the the url is a reified pointer to the runtime entry
            String rep = vclsUrl.toString();
            //System.out.println("    getResource(\"/version/Version.class\") returned: " + rep);
            if (style.equals("http")) {
                Assert.assertTrue(rep.startsWith("jar:http:"));
            } else {
                Assert.assertTrue(rep.startsWith("jar:file:"));
            }
            String suffix;
            if (style.equals("unversioned")) {
                suffix = ".jar!/version/Version.class";
            } else {
                suffix = ".jar!/META-INF/versions/" + Runtime.version().major()
                        + "/version/Version.class";
            }
            Assert.assertTrue(rep.endsWith(suffix));
        }
        cldr.close();
    }

    private static URL toHttpJarURL(int port, String jar, String file)
            throws MalformedURLException, URISyntaxException {
        assert file.startsWith("!/");
        URI httpURI = URIBuilder.newBuilder()
                .scheme("http")
                .loopback()
                .port(port)
                .path(jar)
                .build();
        return new URL("jar:" + httpURI + file);
    }

    private boolean readAndCompare(URL url, String match) throws Exception {
        boolean result;
        // necessary to do it this way, instead of openStream(), so we can
        // close underlying JarFile, otherwise windows can't delete the file
        URLConnection conn = url.openConnection();
        try (InputStream is = conn.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            result = (new String(bytes)).contains(match);
        }
        if (conn instanceof JarURLConnection) {
            ((JarURLConnection) conn).getJarFile().close();
        }
        return result;
    }
}
