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
 * @bug 8132734 8144062 8194070
 * @summary Test the System properties for JarFile that support multi-release jar files
 * @library /lib/testlibrary/java/util/jar /test/lib/
 * @build CreateMultiReleaseTestJars
 *        jdk.test.lib.compiler.Compiler
 *        jdk.test.lib.util.JarBuilder
 * @run testng MultiReleaseJarProperties
 * @run testng/othervm -Djdk.util.jar.version=0   MultiReleaseJarProperties
 * @run testng/othervm -Djdk.util.jar.version=8   MultiReleaseJarProperties
 * @run testng/othervm -Djdk.util.jar.version=9   MultiReleaseJarProperties
 * @run testng/othervm -Djdk.util.jar.version=100 MultiReleaseJarProperties
 * @run testng/othervm -Djdk.util.jar.version=8   -Djdk.util.jar.enableMultiRelease=false MultiReleaseJarProperties
 * @run testng/othervm -Djdk.util.jar.version=9   -Djdk.util.jar.enableMultiRelease=false MultiReleaseJarProperties
 * @run testng/othervm -Djdk.util.jar.version=8   -Djdk.util.jar.enableMultiRelease=force MultiReleaseJarProperties
 * @run testng/othervm -Djdk.util.jar.version=9   -Djdk.util.jar.enableMultiRelease=force MultiReleaseJarProperties
 * @run testng/othervm -Djdk.util.jar.enableMultiRelease=false MultiReleaseJarProperties
 * @run testng/othervm -Djdk.util.jar.enableMultiRelease=force MultiReleaseJarProperties
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


public class MultiReleaseJarProperties {
    final static int BASE_VERSION = JarFile.baseVersion().major();

    final static String userdir = System.getProperty("user.dir", ".");
    final static File multirelease = new File(userdir, "multi-release.jar");
    protected int rtVersion;
    boolean force;
    protected ClassLoader cldr;
    protected Class<?> rootClass;

    @BeforeClass
    public void initialize() throws Exception {
        CreateMultiReleaseTestJars creator = new CreateMultiReleaseTestJars();
        creator.compileEntries();
        creator.buildMultiReleaseJar();
        int RUNTIME_VERSION = Runtime.version().major();
        rtVersion = Integer.getInteger("jdk.util.jar.version", RUNTIME_VERSION);
        String mrprop = System.getProperty("jdk.util.jar.enableMultiRelease", "");
        if (mrprop.equals("false")) {
            rtVersion = BASE_VERSION;
        } else if (rtVersion < BASE_VERSION) {
            rtVersion = BASE_VERSION;
        } else if (rtVersion > RUNTIME_VERSION) {
            rtVersion = RUNTIME_VERSION;
        }
        force = mrprop.equals("force");

        initializeClassLoader();
    }

    protected void initializeClassLoader() throws Exception {
        URL[] urls = new URL[]{multirelease.toURI().toURL()};
        cldr = new URLClassLoader(urls);
        // load any class, Main is convenient and in the root entries
        rootClass = cldr.loadClass("version.Main");
    }

    @AfterClass
    public void close() throws IOException {
        ((URLClassLoader) cldr).close();
        Files.delete(multirelease.toPath());
    }

    /*
     * jdk.util.jar.enableMultiRelease=force is a no-op for URLClassLoader
     */
    @Test
    public void testURLClassLoader() throws Throwable {
        Class<?> vcls = cldr.loadClass("version.Version");
        invokeMethod(vcls, rtVersion);
    }

    protected void invokeMethod(Class<?> vcls, int expected) throws Throwable {
        MethodType mt = MethodType.methodType(int.class);
        MethodHandle mh = MethodHandles.lookup().findVirtual(vcls, "getVersion", mt);
        Assert.assertEquals(expected, (int) mh.invoke(vcls.newInstance()));
    }

    /*
     * jdk.util.jar.enableMultiRelease=force should affect a custom class loader
     */
    @Test
    public void testClassLoader() throws Throwable {
        try (JarFile jf = new JarFile(multirelease)) {  // do not set runtime versioning
            ClassLoader cldr = new CustomClassLoader(jf);
            Class<?> vcls = cldr.loadClass("version.Version");
            if (rtVersion == 9) {
                try {
                    cldr.loadClass("version.PackagePrivate");
                } catch (ClassNotFoundException x) {
                    if (force) throw x;
                }
            }
            invokeMethod(vcls, force ? rtVersion : BASE_VERSION);
        }
    }

    private static class CustomClassLoader extends ClassLoader {
        private final JarFile jf;

        CustomClassLoader(JarFile jf) throws Exception {
            super(null);
            this.jf = jf;
        }

        protected Class<?> findClass(String name) throws ClassNotFoundException {
            try {
                byte[] b;
                String entryName = name.replace(".", "/") + ".class";
                JarEntry je = jf.getJarEntry(entryName);
                if (je != null) {
                    try (InputStream is = jf.getInputStream(je)) {
                        b = new byte[(int) je.getSize()];
                        is.read(b);
                    }
                    return defineClass(name, b, 0, b.length);
                }
                throw new ClassNotFoundException(name);
            } catch (IOException x) {
                throw new ClassNotFoundException(x.getMessage());
            }
        }
    }

    @Test
    public void testGetResourceAsStream() throws Exception {
        String resource = rtVersion == 9 ? "/version/PackagePrivate.java" : "/version/Version.java";
        // use fileRootClass as a base for getting resources
        getResourceAsStream(rootClass, resource);
    }

    protected void getResourceAsStream(Class<?> rootClass, String resource) throws Exception {
        try (InputStream is = rootClass.getResourceAsStream(resource)) {
            byte[] bytes = is.readAllBytes();
            resource = new String(bytes);
        }
        String match = "return " + rtVersion + ";";
        Assert.assertTrue(resource.contains(match));
    }

    @Test
    public void testGetResource() throws Exception {
        String resource = rtVersion == 9 ? "/version/PackagePrivate.java" : "/version/Version.java";
        // use rootClass as a base for getting resources
        getResource(rootClass, resource);
    }

    protected void getResource(Class<?> rootClass, String resource) throws Exception {
        URL url = rootClass.getResource(resource);
        try (InputStream is = url.openStream()) {
            byte[] bytes = is.readAllBytes();
            resource = new String(bytes);
        }
        String match = "return " + rtVersion + ";";
        Assert.assertTrue(resource.contains(match));
    }
}
