/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4842702 8211765
 * @summary Check that constructors throw specified exceptions
 * @author Martin Buchholz
 */

import java.util.jar.JarFile;
import java.io.File;
import java.io.IOException;

public class Constructor {
    private static void Unreached (Object o)
        throws Exception
    {
        // Should never get here
        throw new Exception ("Expected exception was not thrown");
    }

    public static void main(String[] args)
        throws Exception
    {
        try { Unreached (new JarFile ((File) null, true, JarFile.OPEN_READ)); }
        catch (NullPointerException e) {}

        try { Unreached (new JarFile ((File) null, true)); }
        catch (NullPointerException e) {}

        try { Unreached (new JarFile ((File) null)); }
        catch (NullPointerException e) {}

        try { Unreached (new JarFile ((String) null, true)); }
        catch (NullPointerException e) {}

        try { Unreached (new JarFile ((String) null)); }
        catch (NullPointerException e) {}

        try { Unreached (new JarFile ("NoSuchJar.jar")); }
        catch (IOException e) {}

        try { Unreached (new JarFile (new File ("NoSuchJar.jar"))); }
        catch (IOException e) {}

        // Test that an IOExcception is thrown when an invalid charater
        // is part of the path on Windows and Unix
        final String invalidOSPath = System.getProperty("os.name")
                .startsWith("Windows") ? "C:\\*" : "foo\u0000bar";

        try { Unreached (new JarFile (invalidOSPath)); }
        catch (IOException e) {}
    }
}
