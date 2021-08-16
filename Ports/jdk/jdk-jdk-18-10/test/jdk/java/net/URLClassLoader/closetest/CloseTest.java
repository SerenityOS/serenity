/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4167874
 * @modules java.logging
 *          jdk.httpserver
 *          jdk.compiler
 * @library ../../../../com/sun/net/httpserver
 *          /test/lib
 * @build jdk.test.lib.compiler.CompilerUtils
 *        jdk.test.lib.util.FileUtils
 *        jdk.test.lib.util.JarUtils
 *        jdk.test.lib.Platform
 *        FileServerHandler
 * @run main/othervm CloseTest
 * @summary URL-downloaded jar files can consume all available file descriptors
 */

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.net.URIBuilder;
import jdk.test.lib.util.JarUtils;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class CloseTest extends Common {
    private static final String WORK_DIR = System.getProperty("user.dir")
            + "/";
//
// needs two jar files test1.jar and test2.jar with following structure
//
// com/foo/TestClass
// com/foo/TestClass1
// com/foo/Resource1
// com/foo/Resource2
//
// and a directory hierarchy with the same structure/contents

    public static void main(String args[]) throws Exception {
        setup();

        startHttpServer(WORK_DIR + "serverRoot/");

        String testjar = WORK_DIR + "test.jar";
        copyFile(WORK_DIR + "test1.jar", testjar);
        test(testjar, 1);

        // repeat test with different implementation
        // of test.jar (whose TestClass.getValue() returns 2
        copyFile(WORK_DIR + "test2.jar", testjar);
        test(testjar, 2);

        // repeat test using a directory of files
        String testdir = WORK_DIR + "testdir/";
        rm_minus_rf(new File(testdir));
        copyDir(WORK_DIR + "test1/", testdir);
        test(testdir, 1);

        testdir = WORK_DIR + "testdir/";
        rm_minus_rf(new File(testdir));
        copyDir(WORK_DIR + "test2/", testdir);
        test(testdir, 2);
        getHttpServer().stop(3);
    }

    // create a loader on jarfile (or directory), plus a http loader
    // load a class , then look for a resource
    // also load a class from http loader
    // then close the loader
    // check further new classes/resources cannot be loaded
    // check jar (or dir) can be deleted
    // check existing classes can be loaded
    // check boot classes can be loaded

    static void test(String name, int expectedValue) throws Exception {

        URL url = new URL("file", null, name);
        URL url2 = getServerURL();
        System.out.println("Doing tests with URL: " + url + " and " + url2);
        URL[] urls = new URL[2];
        urls[0] = url;
        urls[1] = url2;
        URLClassLoader loader = new URLClassLoader(urls);
        Class testclass = loadClass("com.foo.TestClass", loader, true);
        Class class2 = loadClass("Test", loader, true); // from http
        class2.newInstance();
        Object test = testclass.newInstance();
        Method method = testclass.getDeclaredMethods()[0]; // int getValue();
        int res = (Integer) method.invoke(test);

        if (res != expectedValue) {
            throw new RuntimeException("wrong value from getValue() [" + res +
                    "/" + expectedValue + "]");
        }

        // should find /resource1
        URL u1 = loader.findResource("com/foo/Resource1");
        if (u1 == null) {
            throw new RuntimeException("can't find com/foo/Resource1 in test1.jar");
        }
        loader.close();

        // should NOT find /resource2 even though it is in jar
        URL u2 = loader.findResource("com/foo/Resource2");
        if (u2 != null) {
            throw new RuntimeException("com/foo/Resource2 unexpected in test1.jar");
        }

        // load tests
        loadClass("com.foo.TestClass1", loader, false);
        loadClass("com.foo.TestClass", loader, true);
        loadClass("java.util.ArrayList", loader, true);

        // now check we can delete the path
        rm_minus_rf(new File(name));
        System.out.println(" ... OK");
    }

    static HttpServer httpServer;

    static HttpServer getHttpServer() {
        return httpServer;
    }

    static URL getServerURL() throws Exception {
        int port = httpServer.getAddress().getPort();
        return URIBuilder.newBuilder()
            .scheme("http")
            .loopback()
            .port(port)
            .path("/")
            .toURL();
    }

    static void startHttpServer(String docroot) throws Exception {
        httpServer = HttpServer.create(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 0),
                10);
        HttpContext ctx = httpServer.createContext(
                "/", new FileServerHandler(docroot)
        );
        httpServer.start();
    }

    /**
     * Prepare jars files for the tests
     */
    private static void setup () throws IOException {
        String[] tests = new String[]{"test1", "test2"};
        Path workDir = Paths.get(WORK_DIR);
        Path testSrc = Paths.get(System.getProperty("test.src"));
        for (String test : tests) {
            Path testSrcDir =  testSrc.resolve(test);
            Path testTargetDir = workDir.resolve(test);
            // Compile sources for corresponding test
            CompilerUtils.compile(testSrcDir, testTargetDir);
            // Copy all resources
            Path packages = Paths.get("com", "foo");
            Path copySrcDir = testSrcDir.resolve(packages);
            Path copyTargetDir = testTargetDir.resolve(packages);
            Files.createDirectories(copyTargetDir);
            Path res1 = Paths.get("Resource1");
            Path res2 = Paths.get("Resource2");
            Files.copy(copySrcDir.resolve(res1), copyTargetDir.resolve(res1),
                       REPLACE_EXISTING);
            Files.copy(copySrcDir.resolve(res2), copyTargetDir.resolve(res2),
                       REPLACE_EXISTING);
            // Create jar
            JarUtils.createJarFile(workDir.resolve(test + ".jar"), testTargetDir);
        }

        // Copy and compile server test class
        Path serverRoot = Paths.get("serverRoot");
        Path targetDir = workDir.resolve(serverRoot);
        Path file = Paths.get("Test.java");
        Files.createDirectories(targetDir);
        Files.copy(testSrc.resolve(serverRoot).resolve(file),
                   targetDir.resolve(file), REPLACE_EXISTING);
        CompilerUtils.compile(targetDir, targetDir);
    }
}
