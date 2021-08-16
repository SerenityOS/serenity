/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4109476
 * @summary Test that URLConnection.getLastModified returns the last
 *          modified date on files and jar files.
 */
import java.net.URL;
import java.net.URLConnection;
import java.io.File;

public class GetLastModified {

    static boolean testFailed = false;

    static void test(String s) throws Exception {
        URL url = new URL(s);
        URLConnection conn = url.openConnection();
        if (conn.getLastModified() == 0) {
            System.out.println("Failed: getLastModified returned 0 for URL: " + s);
            testFailed = true;
        }
    }

    public static void main(String args[]) throws Exception {

        File file = new File(System.getProperty("test.src", "."), "jars");

        String fileURL = "file:" + file.getCanonicalPath() + file.separator +
                         "test.jar";
        test(fileURL);

        String jarURL = "jar:" + fileURL + "!/";
        test(jarURL);

        if (testFailed) {
            throw new Exception("Test failed - getLastModified returned 0");
        }
    }

}
