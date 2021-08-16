/*
 * Copyright (c) 2003, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4866847 7152564 7155693
 * @summary various CodeSource.implies tests
 */

import java.security.CodeSource;
import java.net.URL;

public class Implies {
    public static void main(String[] args) throws Exception {
        URL thisURL = new URL("http", "localhost", "file");
        URL thatURL = new URL("http", null, "file");
        // should not throw NullPointerException
        testImplies(thisURL, thatURL, false);

        thisURL = new URL("http", "localhost", "dir/-");
        thatURL = new URL("HTTP", "localhost", "dir/file");
        // protocol check should ignore case
        testImplies(thisURL, thatURL, true);

        thisURL = new URL("http", "localhost", 80, "dir/-");
        thatURL = new URL("HTTP", "localhost", "dir/file");
        // port check should match default port of thatURL
        testImplies(thisURL, thatURL, true);

        System.out.println("test passed");
    }

    private static void testImplies(URL thisURL, URL thatURL, boolean result)
        throws SecurityException
    {
        CodeSource thisCs =
            new CodeSource(thisURL, (java.security.cert.Certificate[]) null);
        CodeSource thatCs =
            new CodeSource(thatURL, (java.security.cert.Certificate[]) null);
        if (thisCs.implies(thatCs) != result) {
            throw new SecurityException("test failed");
        }
    }
}
