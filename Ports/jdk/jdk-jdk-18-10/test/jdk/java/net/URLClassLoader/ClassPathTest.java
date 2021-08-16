/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4110602
 * @author Benjamin Renaud
 * @summary check that URLClassLoader correctly interprets Class-Path
 *
 * This test ensures that a manually constructed URLClassLoader is
 * able to:
 *
 * 1. load a class
 * 2. resolve Class-Path dependencies
 * 3. have that class use a dependent class in a different JAR file
 */
import java.util.jar.*;
import java.util.*;
import java.io.*;
import java.net.*;

public class ClassPathTest {

    JarFile jarFile;
    Manifest manifest;
    Attributes mainAttributes;
    Map map;
    URLClassLoader ucl;

    static class TestException extends RuntimeException {
        TestException(Throwable t) {
            super("URLClassLoader ClassPathTest failed with: " + t);
        }
    }

    public ClassPathTest() {
        File local = new File(System.getProperty("test.src", "."),
                              "jars/class_path_test.jar");
        String jarFileName = local.getPath();

        try {
            jarFile = new JarFile(jarFileName);
        }
        catch (IOException e) {
            System.err.println("Could not find jar file " + jarFileName);
            throw new TestException(e);
        }
        try {
            URL url = getUrl(new File(jarFileName));
            System.out.println("url: " + url);

            ucl = new URLClassLoader(new URL[] { url });

            // Moved this inside try block, as it may raise IOException.
            // (maddox)
            manifest = jarFile.getManifest();
        }
        catch (Exception e) {
            throw new TestException(e);
        }
        //manifest = jarFile.getManifest();
        mainAttributes = manifest.getMainAttributes();
        map = manifest.getEntries();

        Iterator it = map.entrySet().iterator();
        Class clazz = null;

        while (it.hasNext()) {
            Map.Entry e = (Map.Entry)it.next();
            Attributes a = (Attributes)e.getValue();

            Attributes.Name an = new Attributes.Name("Class-Path");
            if (a.containsKey(an)) {
                String val = a.getValue(an);
                if (val != null)
                    System.out.println("Class-Path: " + val);
            }

            if (a.containsKey(new Attributes.Name("Java-Bean"))) {

                String beanClassName = nameToClassName((String)e.getKey());
                System.out.println("JavaBean Class: " + beanClassName);

                try {
                    clazz = ucl.loadClass(beanClassName);
                }
                catch (Throwable t) {
                    throw new TestException(t);
                } if (clazz != null) {
                    try {
                        System.out.println("instantiating " + beanClassName);
                        clazz.newInstance();
                        System.out.println("done instantiating " +
                                           beanClassName);
                    } catch (Throwable t2) {
                        throw new TestException(t2);
                    }
                }
            }
        }
    }

    String nameToClassName(String key) {
        String key2 = key.replace('/', File.separatorChar);
        int li = key2.lastIndexOf(".class");
        key2 = key2.substring(0, li);
        return key2;
    }

    private static URL getUrl(File file) {
        String name;
        try {
            name = file.getCanonicalPath();
        } catch (IOException e) {
            name = file.getAbsolutePath();
        }
        name = name.replace(File.separatorChar, '/');
        if (!name.startsWith("/")) {
            name = "/" + name;
        }
        try {
            return new URL( "file:" + name);
        } catch (MalformedURLException e) {
            throw new TestException(e);
        }
    }

    public static void main(String args[]) {
        new ClassPathTest();
    }
}
