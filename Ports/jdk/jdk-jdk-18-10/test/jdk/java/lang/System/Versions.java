/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4989690 6259855 6706299 8204565
 * @summary Check that version-related system property invariants hold.
 * @author Martin Buchholz
 */

import java.io.*;
import java.net.URLClassLoader;
import java.net.URL;

public class Versions {
    static String getProperty(String prop) throws Exception {
        String value = System.getProperty(prop);
        if (value == null)
            throw new Exception("No such system property: " + prop);
        System.out.printf("%s=%s%n", prop, value);
        return value;
    }

    static ClassLoader cl;

    static void checkClassVersion(int major, int minor, boolean expectSupported)
        throws Exception
    {
        final String className  = "ClassVersionTest";
        final String classFile  = className + ".class";

        // We create an invalid class file, (only magic and version info),
        // but the version info must be checked before the body.
        final DataOutputStream dos =
            new DataOutputStream(new FileOutputStream(classFile));
        dos.writeLong((0xCafeBabel << 32) + (minor << 16) + major);
        dos.close();

        boolean supported = true;
        try {
            Class.forName(className, false, cl);
        } catch (UnsupportedClassVersionError e) {
            supported = false;
        } catch (Throwable t) {
            // We expect an Exception indicating invalid class file
        }
        new File(classFile).delete();
        if (supported != expectSupported)
            throw new Exception("Forgot to update java.class.version?");
    }

    public static void main(String [] args) throws Exception {
        String classVersion   = getProperty("java.class.version");
        String javaVersion    = getProperty("java.version");
        String runtimeVersion = getProperty("java.runtime.version");
        String specVersion    = getProperty("java.specification.version");
        String vmSpecVersion  = getProperty("java.vm.specification.version");
        String featureVersion = Integer.toString(Runtime.version().feature());

        if (! (javaVersion.startsWith(specVersion) &&
               runtimeVersion.startsWith(specVersion) &&
               specVersion.equals(featureVersion) &&
               vmSpecVersion.equals(featureVersion))) {
            throw new Exception("Invalid version-related system properties");
        }

        //----------------------------------------------------------------
        // Check that java.class.version is correct.
        // Injecting a larger major or minor version number into a
        // .class file should result in UnsupportedClassVersionError.
        //----------------------------------------------------------------
        String[] versions = classVersion.split("\\.");
        int majorVersion = Integer.parseInt(versions[0]);
        int minorVersion = Integer.parseInt(versions[1]);
        System.out.printf("majorVersion=%s%n",majorVersion);
        System.out.printf("minorVersion=%s%n",minorVersion);

        // Look in ".", and *not* in CLASSPATH
        cl = new URLClassLoader(new URL[]{new File("./").toURL()}, null);

        checkClassVersion(majorVersion    , minorVersion    , true );
        checkClassVersion(majorVersion + 1, minorVersion    , false);
        checkClassVersion(majorVersion    , minorVersion + 1, false);
    }
}
