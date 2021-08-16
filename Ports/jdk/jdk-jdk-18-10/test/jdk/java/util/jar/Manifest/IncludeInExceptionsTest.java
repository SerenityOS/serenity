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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @test
 * @bug 8216362
 * @run main/othervm -Djdk.includeInExceptions=jar IncludeInExceptionsTest yes
 * @run main/othervm IncludeInExceptionsTest
 * @summary Verify that the property jdk.net.includeInExceptions works as expected
 * when an error occurs while reading an invalid Manifest file.
 */
/*
 * @see Manifest#Manifest(JarVerifier,InputStream,String)
 * @see Manifest#getErrorPosition
 */
public class IncludeInExceptionsTest {

    static final String FILENAME = "Unique-Filename-Expected-In_Msg.jar";

    static final byte[] INVALID_MANIFEST = (
            "Manifest-Version: 1.0\r\n" +
            "\r\n" +
            "Illegal\r\n" +
            "\r\n").getBytes(UTF_8);

    static String createJarInvalidManifest(String jar) throws IOException {
        try (OutputStream out = Files.newOutputStream(Paths.get(jar));
            JarOutputStream jos = new JarOutputStream(out)) {
            JarEntry je = new JarEntry(JarFile.MANIFEST_NAME);
            jos.putNextEntry(je);
            jos.write(INVALID_MANIFEST);
            jos.closeEntry();
        }
        return jar;
    }

    static void test(Callable<?> attempt, boolean includeInExceptions) throws Exception {
        try {
            attempt.call();
            throw new AssertionError("Excpected Exception not thrown");
        } catch (IOException e) {
            boolean foundFileName = e.getMessage().contains(FILENAME);
            if(includeInExceptions && !foundFileName) {
                throw new AssertionError("JAR file name expected but not found in error message");
            } else if (foundFileName && !includeInExceptions) {
                throw new AssertionError("JAR file name found but should not be in error message");
            }
        }
    }

    public static void main(String[] args) throws Exception {

        boolean includeInExceptions;
        if(args.length > 0) {
            includeInExceptions = true;
            System.out.println("**** Running test WITH -Djdk.includeInExceptions=jar");
        } else {
            includeInExceptions = false;
            System.out.println("**** Running test WITHOUT -Djdk.includeInExceptions=jar");
        }

        test(() -> new JarFile(createJarInvalidManifest(FILENAME)).getManifest(),
                includeInExceptions);
        test(() -> new JarFile(createJarInvalidManifest("Verifying-" + FILENAME),
                true).getManifest(), includeInExceptions);
    }

}

