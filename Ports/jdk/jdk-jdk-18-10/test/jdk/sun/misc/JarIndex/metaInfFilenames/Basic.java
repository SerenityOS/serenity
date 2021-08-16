/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6887710
 * @summary Verify the impact of sun.misc.JarIndex.metaInfFilenames on ServiceLoader
 * @modules jdk.jartool/sun.tools.jar
 *          jdk.httpserver
 *          jdk.compiler
 *          jdk.zipfs
 * @run main/othervm Basic
 */

import java.io.IOException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ServiceLoader;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Verifies the impact of sun.misc.JarIndex.metaInfFilenames on ServiceLoader
 * and on finding resources via Class.getResource.
 *
 * 1) Compile the test sources:
 *   jarA:
 *     META-INF/services/my.happy.land
 *     com/message/spi/MessageService.java
 *     a/A.java
 *   jarB:
 *     META-INF/JAVA2.DS
 *     META-INF/services/no.name.service
 *     b/B.java
 *   jarC:
 *     META-INF/fonts.mf
 *     META-INF/fonts/Company-corporate.ttf
 *     META-INF/fonts/kidpr.ttf
 *     META-INF/services/com.message.spi.MessageService
 *     my/impl/StandardMessageService.java
 *
 * 2) Build three jar files a.jar, b.jar, c.jar
 *
 * 3) Create an index in a.jar (jar -i a.jar b.jar c.jar)
 *      with sun.misc.JarIndex.metaInfFilenames=true
 *
 * 4) Start a HTTP server serving out the three jars.
 *
 * The test then tries to locate services/resources within the jars using
 * URLClassLoader. Each request to the HTTP server is recorded to ensure
 * only the correct amount of requests are being made.
 *
 */

public class Basic {
    static final String slash = File.separator;
    static final String[] testSources =  {
         "jarA" + slash + "a" + slash + "A.java",
         "jarA" + slash + "com" + slash + "message" + slash + "spi" + slash + "MessageService.java",
         "jarB" + slash + "b" + slash + "B.java",
         "jarC" + slash + "my" + slash + "impl" + slash + "StandardMessageService.java"};

    static final String testSrc = System.getProperty("test.src");
    static final String testSrcDir = testSrc != null ? testSrc : ".";
    static final String testClasses = System.getProperty("test.classes");
    static final String testClassesDir = testClasses != null ? testClasses : ".";

    static JarHttpServer httpServer;

    public static void main(String[] args) throws Exception {

        // Set global url cache to false so that we can track every jar request.
        (new URL("http://localhost/")).openConnection().setDefaultUseCaches(false);

        buildTest();

        try {
            httpServer = new JarHttpServer(testClassesDir);
            httpServer.start();

            doTest(httpServer.getAddress());

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            if (httpServer != null) { httpServer.stop(2); }
        }
    }

    static void buildTest() {
        /* compile the source that will be used to generate the jars */
        for (int i=0; i<testSources.length; i++)
            testSources[i] = testSrcDir + slash + testSources[i];

        compile("-d" , testClassesDir,
                "-sourcepath", testSrcDir,
                testSources[0], testSources[1], testSources[2], testSources[3]);

        /* build the 3 jar files */
        jar("-cf", testClassesDir + slash + "a.jar",
            "-C", testClassesDir, "a",
            "-C", testClassesDir, "com",
            "-C", testSrcDir + slash + "jarA", "META-INF");
        jar("-cf", testClassesDir + slash + "b.jar",
            "-C", testClassesDir, "b",
            "-C", testSrcDir + slash + "jarB", "META-INF");
        jar("-cf", testClassesDir + slash + "c.jar",
            "-C", testClassesDir, "my",
            "-C", testSrcDir + slash + "jarC", "META-INF");

        /* Create an index in a.jar for b.jar and c.jar */
        createIndex(testClassesDir);
    }

    /* run jar <args> */
    static void jar(String... args) {
        debug("Running: jar " + Arrays.toString(args));
        sun.tools.jar.Main jar = new sun.tools.jar.Main(System.out, System.err, "jar");
        if (!jar.run(args)) {
            throw new RuntimeException("jar failed: args=" + Arrays.toString(args));
        }
    }

    /* run javac <args> */
    static void compile(String... args) {
        debug("Running: javac " + Arrays.toString(args));
        if (com.sun.tools.javac.Main.compile(args) != 0) {
             throw new RuntimeException("javac failed: args=" + Arrays.toString(args));
        }
    }

    static String jar;
    static {
        jar = System.getProperty("java.home") + slash+  "bin" + slash + "jar";
    }

    /* create the index */
    static void createIndex(String workingDir) {
        // ProcessBuilder is used so that the current directory can be set
        // to the directory that directly contains the jars.
        debug("Running jar to create the index");
        ProcessBuilder pb = new ProcessBuilder(
           jar, "-J-Dsun.misc.JarIndex.metaInfFilenames=true", "-i", "a.jar", "b.jar", "c.jar");
        pb.directory(new File(workingDir));
        //pd.inheritIO();
        try {
            Process p = pb.start();
            if(p.waitFor() != 0)
                throw new RuntimeException("jar indexing failed");

            if(debug && p != null) {
                String line = null;
                BufferedReader reader =
                         new BufferedReader(new InputStreamReader(p.getInputStream()));
                while((line = reader.readLine()) != null)
                    debug(line);
                reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                while((line = reader.readLine()) != null)
                    debug(line);
            }
        } catch(InterruptedException ie) { throw new RuntimeException(ie);
        } catch(IOException e) { throw new RuntimeException(e); }
    }

    static final boolean debug = true;

    static void debug(Object message) { if (debug) System.out.println(message); }

    /* service define in c.jar */
    static final String messageService = "com.message.spi.MessageService";

    /* a service that is not defined in any of the jars */
    static final String unknownService = "java.lang.Object";

    static void doTest(InetSocketAddress serverAddress) throws IOException {
        URL baseURL = new URL("http://localhost:" + serverAddress.getPort() + "/");

        int failed = 0;

        // Tests using java.util.SerivceLoader
        if (!javaUtilServiceLoaderTest(baseURL, messageService, true, false, true)) {
            System.out.println("Test: ServiceLoader looking for " + messageService + ", failed");
            failed++;
        }
        if (!javaUtilServiceLoaderTest(baseURL, unknownService, false, false, false)) {
            System.out.println("Test: ServiceLoader looking for " + unknownService + " failed");
            failed++;
        }

        // Tests using java.lang.Class (similar to the FontManager in javafx)
        if (!klassLoader(baseURL, "/META-INF/fonts.mf", true, false, true)) {
            System.out.println("Test: klassLoader looking for /META-INF/fonts.mf failed");
            failed++;
        }
        if (!klassLoader(baseURL, "/META-INF/unknown.mf", false, false, false)) {
            System.out.println("Test: klassLoader looking for /META-INF/unknown.mf failed");
            failed++;
        }

        if (failed > 0)
            throw new RuntimeException("Failed: " + failed + " tests");
    }

    static boolean javaUtilServiceLoaderTest(URL baseURL,
                                             String serviceClass,
                                             boolean expectToFind,
                                             boolean expectbDotJar,
                                             boolean expectcDotJar) throws IOException {
        debug("----------------------------------");
        debug("Running test with java.util.ServiceLoader looking for " + serviceClass);
        URLClassLoader loader = getLoader(baseURL);
        httpServer.reset();

        Class<?> messageServiceClass = null;
        try {
            messageServiceClass = loader.loadClass(serviceClass);
        } catch (ClassNotFoundException cnfe) {
            System.err.println(cnfe);
            throw new RuntimeException("Error in test: " + cnfe);
        }

        Iterator<?> iterator = (ServiceLoader.load(messageServiceClass, loader)).iterator();
        if (expectToFind && !iterator.hasNext()) {
            debug(messageServiceClass + " NOT found.");
            return false;
        }

        while (iterator.hasNext()) {
            debug("found " + iterator.next() + " " + messageService);
        }

        debug("HttpServer: " + httpServer);

        if (!expectbDotJar && httpServer.bDotJar > 0) {
            debug("Unexpeced request sent to the httpserver for b.jar");
            return false;
        }
        if (!expectcDotJar && httpServer.cDotJar > 0) {
            debug("Unexpeced request sent to the httpserver for c.jar");
            return false;
        }

        return true;
    }

    /* Tries to find a resource in a similar way to the font manager in javafx
     * com.sun.javafx.scene.text.FontManager */
    static boolean klassLoader(URL baseURL,
                               String resource,
                               boolean expectToFind,
                               boolean expectbDotJar,
                               boolean expectcDotJar) throws IOException {
        debug("----------------------------------");
        debug("Running test looking for " + resource);
        URLClassLoader loader = getLoader(baseURL);
        httpServer.reset();

        Class<?> ADotAKlass = null;
        try {
            ADotAKlass = loader.loadClass("a.A");
        } catch (ClassNotFoundException cnfe) {
            System.err.println(cnfe);
            throw new RuntimeException("Error in test: " + cnfe);
        }

        URL u = ADotAKlass.getResource(resource);
        if (expectToFind && u == null) {
            System.out.println("Expected to find " + resource + " but didn't");
            return false;
        }

        debug("HttpServer: " + httpServer);

        if (!expectbDotJar && httpServer.bDotJar > 0) {
            debug("Unexpeced request sent to the httpserver for b.jar");
            return false;
        }
        if (!expectcDotJar && httpServer.cDotJar > 0) {
            debug("Unexpeced request sent to the httpserver for c.jar");
            return false;
        }

        return true;
    }

    static URLClassLoader getLoader(URL baseURL) throws IOException {
        ClassLoader loader = Basic.class.getClassLoader();

        while (loader.getParent() != null)
            loader = loader.getParent();

        return new URLClassLoader( new URL[]{
            new URL(baseURL, "a.jar"),
            new URL(baseURL, "b.jar"),
            new URL(baseURL, "c.jar")}, loader );
    }

    /**
     * HTTP Server to server the jar files.
     */
    static class JarHttpServer implements HttpHandler {
        final String docsDir;
        final HttpServer httpServer;
        int aDotJar, bDotJar, cDotJar;

        JarHttpServer(String docsDir) throws IOException {
            this.docsDir = docsDir;

            httpServer = HttpServer.create(new InetSocketAddress(0), 0);
            httpServer.createContext("/", this);
        }

        void start() throws IOException {
            httpServer.start();
        }

        void stop(int delay) {
            httpServer.stop(delay);
        }

        InetSocketAddress getAddress() {
            return httpServer.getAddress();
        }

        void reset() {
            aDotJar = bDotJar = cDotJar = 0;
        }

        @Override
        public String toString() {
            return "aDotJar=" + aDotJar + ", bDotJar=" + bDotJar + ", cDotJar=" + cDotJar;
        }

        public void handle(HttpExchange t) throws IOException {
            InputStream is = t.getRequestBody();
            Headers map = t.getRequestHeaders();
            Headers rmap = t.getResponseHeaders();
            URI uri = t.getRequestURI();

            debug("Server: received request for " + uri);
            String path = uri.getPath();
            if (path.endsWith("a.jar"))
                aDotJar++;
            else if (path.endsWith("b.jar"))
                bDotJar++;
            else if (path.endsWith("c.jar"))
                cDotJar++;
            else
                System.out.println("Unexpected resource request" + path);

            while (is.read() != -1);
            is.close();

            File file = new File(docsDir, path);
            if (!file.exists())
                throw new RuntimeException("Error: request for " + file);
            long clen = file.length();
            t.sendResponseHeaders (200, clen);
            OutputStream os = t.getResponseBody();
            FileInputStream fis = new FileInputStream(file);
            try {
                byte[] buf = new byte [16 * 1024];
                int len;
                while ((len=fis.read(buf)) != -1) {
                    os.write (buf, 0, len);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            fis.close();
            os.close();
        }
    }
}
