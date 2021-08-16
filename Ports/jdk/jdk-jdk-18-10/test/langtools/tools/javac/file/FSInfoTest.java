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

import com.sun.tools.javac.file.FSInfo;
import com.sun.tools.javac.util.Context;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/*
 * @test
 * @bug 8232170
 * @summary Test com.sun.tools.javac.file.FSInfo
 * @modules jdk.compiler/com.sun.tools.javac.util
 *          jdk.compiler/com.sun.tools.javac.file
 * @run testng FSInfoTest
 */
public class FSInfoTest {

    /**
     * Tests that if a jar file has a manifest with a invalid path value for {@code Class-Path} attribute,
     * then parsing such a jar file through {@link FSInfo#getJarClassPath(Path)} doesn't throw any other
     * exception other than {@link IOException}
     *
     * @throws Exception
     */
    @Test
    public void testInvalidClassPath() throws Exception {
        final String invalidOSPath = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("windows")
                ? "C:\\*" : "foo\u0000bar";
        final Path jarFile = Files.createTempFile(null, ".jar");
        jarFile.toFile().deleteOnExit();
        final Manifest mf = new Manifest();
        mf.getMainAttributes().putValue("Manifest-Version", "1.0");
        // add Class-Path which points to an invalid path
        System.out.println("Intentionally using an invalid Class-Path entry " + invalidOSPath + " in manifest");
        mf.getMainAttributes().putValue("Class-Path", invalidOSPath + " " + "/some/other-random/path");

        // create a jar file with the manifest
        try (final JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarFile), mf)) {
        }
        final FSInfo fsInfo = FSInfo.instance(new Context());
        try {
            fsInfo.getJarClassPath(jarFile);
            // we don't rely on fsInfo.getJarClassPath to throw an exception for invalid
            // paths. Hence no Assert.fail(...) call here. But if it does throw some exception,
            // then that exception should always be a IOException.
        } catch (IOException ioe) {
            // expected
            System.out.println("(As expected) FSInfo.getJarClassPath threw an IOException - " + ioe.getMessage());
        }
    }
}
