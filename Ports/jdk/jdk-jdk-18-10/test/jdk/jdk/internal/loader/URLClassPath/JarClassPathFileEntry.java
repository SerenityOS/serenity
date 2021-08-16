/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import jdk.test.lib.util.JarUtils;
import jdk.test.lib.compiler.InMemoryJavaCompiler;
import jdk.test.lib.helpers.ClassFileInstaller;

/*
 * @test
 * @bug 8216401 8235361
 * @summary Test classloading via JAR Class-Path entries
 * @library /test/lib
 *
 * @run main/othervm JarClassPathFileEntry
 * @run main/othervm -Djdk.net.URLClassPath.disableClassPathURLCheck=true JarClassPathFileEntry
 * @run main/othervm -Djdk.net.URLClassPath.disableClassPathURLCheck=false JarClassPathFileEntry
 */

public class JarClassPathFileEntry {
    private final static boolean IS_WINDOWS = System.getProperty("os.name").startsWith("Windows");

    private final static String TEST_CLASSES = System.getProperty("test.classes");
    private final static String OTHER_DIR = TEST_CLASSES + "/OTHER/";

    private final static Path OTHER_JAR_PATH = Paths.get(OTHER_DIR, "Other.jar");
    private final static Path CONTEXT_JAR_PATH = Paths.get(TEST_CLASSES, "Context.jar");

    public static void main(String[] args) throws Throwable {
        String fileScheme = "file:" + (IS_WINDOWS ? toUnixPath(OTHER_JAR_PATH.toString())
                                                      :        OTHER_JAR_PATH.toString());
        doTest(fileScheme);

        if (IS_WINDOWS) {
            // Relative URL encoding of absolute path, e.g. /C:\\path\\to\\file.jar
            String driveLetter = "/" + OTHER_JAR_PATH;
            doTest(driveLetter);
        }
    }

    /* Load a class from Other.jar via the given Class-Path entry */
    private static void doTest(String classPathEntry) throws Throwable {
        // Create Other.class in OTHER_DIR, off the default classpath
        byte klassbuf[] = InMemoryJavaCompiler.compile("Other",
                                                       "public class Other {}");
        ClassFileInstaller.writeClassToDisk("Other", klassbuf, OTHER_DIR);

        // Create Other.jar in OTHER_DIR
        JarUtils.createJarFile(OTHER_JAR_PATH,
                               Paths.get(OTHER_DIR),
                               Paths.get(OTHER_DIR, "Other.class"));

        // Create Context.class
        klassbuf = InMemoryJavaCompiler.compile("Context",
                                                "public class Context {}");
        ClassFileInstaller.writeClassToDisk("Context", klassbuf, TEST_CLASSES);

        // Create Context.jar w/ "file:" entry for Other.jar
        Manifest mf = new Manifest();
        Attributes attrs = mf.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");

        attrs.put(Attributes.Name.CLASS_PATH, classPathEntry);

        System.out.println("Creating Context.jar with Class-Path: " + classPathEntry);
        JarUtils.createJarFile(CONTEXT_JAR_PATH, mf,
                               Paths.get(TEST_CLASSES),
                               Paths.get(TEST_CLASSES, "Context.class"));

        // Use URLClassLoader w/ Context.jar to load Other.class, which will
        // load via the Class-Path entry
        URL url = CONTEXT_JAR_PATH.toUri().toURL();
        URLClassLoader ucl = new URLClassLoader("TestURLClassLoader",
                                                new URL[]{ url },
                                                null); // don't delegate to App CL
        Class<?> otherClass = Class.forName("Other", true, ucl); // ClassNotFoundException -> fail
        System.out.println("Loaded: " + otherClass);
    }

    /* Convert a Windows path to a unix-style path, and remove any drive letter */
    private static String toUnixPath(String orig) {
        String retVal = new File(orig).toURI().getPath();
        int colonAt = retVal.indexOf(':');

        if (colonAt != -1 && colonAt < 3) {
            retVal = retVal.substring(colonAt + 1); // Start after the drive letter
        }
        return retVal;
    }
}
