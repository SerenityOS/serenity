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
 * @bug 8132734 8194070
 * @summary Test the System properties for JarFile that support multi-release jar files
 * @library /lib/testlibrary/java/util/jar /test/lib
 * @modules jdk.jartool
 *          jdk.compiler
 *          jdk.httpserver
 * @build CreateMultiReleaseTestJars
 *        jdk.test.lib.net.SimpleHttpServer
 *        jdk.test.lib.compiler.Compiler
 *        jdk.test.lib.util.JarBuilder
 * @run testng MultiReleaseJarHttpProperties
 * @run testng/othervm -Djdk.util.jar.version=0   MultiReleaseJarHttpProperties
 * @run testng/othervm -Djdk.util.jar.version=8   MultiReleaseJarHttpProperties
 * @run testng/othervm -Djdk.util.jar.version=9   MultiReleaseJarHttpProperties
 * @run testng/othervm -Djdk.util.jar.version=100 MultiReleaseJarHttpProperties
 * @run testng/othervm -Djdk.util.jar.version=8   -Djdk.util.jar.enableMultiRelease=false MultiReleaseJarHttpProperties
 * @run testng/othervm -Djdk.util.jar.version=9   -Djdk.util.jar.enableMultiRelease=false MultiReleaseJarHttpProperties
 * @run testng/othervm -Djdk.util.jar.version=8   -Djdk.util.jar.enableMultiRelease=force MultiReleaseJarHttpProperties
 * @run testng/othervm -Djdk.util.jar.version=9   -Djdk.util.jar.enableMultiRelease=force MultiReleaseJarHttpProperties
 * @run testng/othervm -Djdk.util.jar.enableMultiRelease=false MultiReleaseJarHttpProperties
 * @run testng/othervm -Djdk.util.jar.enableMultiRelease=force MultiReleaseJarHttpProperties
 */

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;

import jdk.test.lib.net.SimpleHttpServer;
import jdk.test.lib.net.URIBuilder;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class MultiReleaseJarHttpProperties extends MultiReleaseJarProperties {
    private SimpleHttpServer server;
    static final String TESTCONTEXT = "/multi-release.jar";  //mapped to local file path

    @BeforeClass
    public void initialize() throws Exception {
        server = new SimpleHttpServer(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), TESTCONTEXT,
                System.getProperty("user.dir", "."));
        server.start();
        super.initialize();
    }

    @Override
    protected void initializeClassLoader() throws Exception {
        URL[] urls = new URL[]{
                URIBuilder.newBuilder().scheme("http").port(server.getPort()).loopback()
                        .path(TESTCONTEXT).toURL(),
        };
        cldr = new URLClassLoader(urls);
        // load any class, Main is convenient and in the root entries
        rootClass = cldr.loadClass("version.Main");
    }

    @AfterClass
    public void close() throws IOException {
        // Windows requires server to stop before file is deleted
        if (server != null)
            server.stop();
        super.close();
    }

    /*
     * jdk.util.jar.enableMultiRelease=force is a no-op for URLClassLoader
     */

    @Test
    public void testURLClassLoader() throws Throwable {
        Class<?> vcls = cldr.loadClass("version.Version");
        invokeMethod(vcls, rtVersion);
    }

    @Test
    public void testGetResourceAsStream() throws Exception {
        String resource = rtVersion == 9 ? "/version/PackagePrivate.java" : "/version/Version.java";
        // use rootClass as a base for getting resources
        getResourceAsStream(rootClass, resource);
    }

    @Test
    public void testGetResource() throws Exception {
        String resource = rtVersion == 9 ? "/version/PackagePrivate.java" : "/version/Version.java";
        // use rootClass as a base for getting resources
        getResource(rootClass, resource);
    }
}
