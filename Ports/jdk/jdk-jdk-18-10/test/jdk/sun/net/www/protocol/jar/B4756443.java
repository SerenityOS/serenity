/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4756443
 * @summary REGRESSION: NPE in JarURLConnection.getLastModified after setUseCache(false)
 */

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class B4756443 {

    public static void main(String [] args) throws IOException
    {
        String testsrc = System.getProperty ("test.src");
        URL u;
        if (testsrc == null || "".equals (testsrc)) {
            /* not running in jtreg */
            u = new URL ("jar:file:./foo.jar!/a/b/test.xml");
        } else {
            /* running in jtreg */
            File f = new File(testsrc + File.separator
                            + "foo.jar");
            String s = f.toURL().toString();
            u = new URL ("jar:" + s + "!/a/b/test.xml");
        }
        System.out.println ("Testing with: " + u);
        URLConnection con = u.openConnection ();
        con.setUseCaches (false);
        con.connect ();
        long l = con.getLastModified ();

        URLConnection con1 = u.openConnection ();
        con1.setUseCaches (true);
        con1.connect ();
        long l1 = con1.getLastModified ();

        if (l != l1) {
            throw new RuntimeException ("l != l1 ("+l+"/"+l1+")");
        }
    }
}
