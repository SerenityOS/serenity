/*
 * Copyright (c) 2001, 2003, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4419266 4842702
 * @summary Make sure verifying signed Jar doesn't throw SecurityException
 */
import java.io.File;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.util.Enumeration;

public class VerifySignedJar {
    private static void Unreached (Object o)
        throws Exception
    {
        // Should never get here
        throw new Exception ("Expected exception was not thrown");
    }

    public static void main(String[] args) throws Exception {
        File f = new File(System.getProperty("test.src", "."), "thawjar.jar");
        JarFile jf = new JarFile(f);
        try {
            // Read entries via Enumeration
            for (Enumeration e = jf.entries(); e.hasMoreElements();)
                jf.getInputStream((ZipEntry) e.nextElement());

            // Read entry by name
            ZipEntry ze = jf.getEntry("getprop.class");
            JarEntry je = jf.getJarEntry("getprop.class");

            // Make sure we throw NPE on null objects
            try { Unreached (jf.getEntry(null)); }
            catch (NullPointerException e) {}

            try { Unreached (jf.getJarEntry(null)); }
            catch (NullPointerException e) {}

            try { Unreached (jf.getInputStream(null)); }
            catch (NullPointerException e) {}

        } catch (SecurityException se) {
            throw new Exception("Got SecurityException when verifying signed " +
                "jar:" + se);
        }
    }
}
