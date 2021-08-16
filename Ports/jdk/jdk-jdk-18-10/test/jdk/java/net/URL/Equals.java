/*
 * Copyright (c) 1998, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4052976 7030649
 * @summary Test URL.equals with anchors, and jar URLs
 */

import java.net.*;

public class Equals {
    public static void main(String[] args) throws Exception {
        anchors();
        jarURLs();
    }

    static void anchors() throws Exception {
        URL url1, url2;

        url1 = new URL(null, "http://JavaSoft/Test#bar");
        url2 = new URL(null, "http://JavaSoft/Test");

        if (url1.equals(url2))
            throw new RuntimeException("URL.equals fails with anchors");
        if (url2.equals(url1))
            throw new RuntimeException("URL.equals fails with anchors");
        if (url1.equals(null))
            throw new RuntimeException("URL.equals fails given null");
    }

    static final String HTTP_URL1A = "http://localhost/xyz";
    static final String HTTP_URL1B = "http://LOCALHOST/xyz";
    static final String FILE_URL1A = "file:///c:/foo/xyz";
    static final String FILE_URL1B = "file:/c:/foo/xyz";

    static void jarURLs() throws Exception {
        int failed = 0;
        failed = compareJarURLS(HTTP_URL1A, HTTP_URL1A, "!/abc", "!/abc", true);
        failed = compareJarURLS(HTTP_URL1A, HTTP_URL1B, "!/abc", "!/abc", true);
        failed = compareJarURLS(HTTP_URL1B, HTTP_URL1A, "!/", "!/", true);
        failed = compareJarURLS(HTTP_URL1A, HTTP_URL1B, "!/abc", "!/", false);
        failed = compareJarURLS(HTTP_URL1A, HTTP_URL1B, "!/abc", "!/xy", false);
        failed = compareJarURLS(FILE_URL1A, FILE_URL1A, "!/abc", "!/abc", true);
        failed = compareJarURLS(FILE_URL1A, FILE_URL1B, "!/abc", "!/abc", true);
        failed = compareJarURLS(FILE_URL1A, FILE_URL1B, "!/", "!/", true);
        failed = compareJarURLS(FILE_URL1A, FILE_URL1B, "!/abc", "!/", false);
        failed = compareJarURLS(FILE_URL1A, FILE_URL1B, "!/abc", "!/xy", false);

        failed = (new URL("jar:file://xzy!/abc")).equals(
                      new URL("file://xzy!/abc")) ? 1 : 0;

        if (failed > 0)
            throw new RuntimeException("Some jar URL tests failed. Check output");
    }

    static int compareJarURLS(String urlStr1, String urlStr2,
                                  String entry1,  String entry2,
                                  boolean expectEqual) throws Exception {
        int failed = 0;

        URL url1 = new URL(urlStr1);
        URL url2 = new URL(urlStr2);

        if (!url1.equals(url2)) {
            System.out.println("Urls are not equal, so the test cannot run.");
            System.out.println("url1: " + url1 + ", url2:" + url2);
            return 1;
        }

        URL jarUrl1 = new URL("jar:" + urlStr1 + entry1);
        URL jarUrl2 = new URL("jar:" + urlStr2 + entry2);
        jarUrl2.openConnection();

        boolean equal = jarUrl1.equals(jarUrl2);
        if (expectEqual && !equal) {
            System.out.println("URLs should be equal, but are not. " +
                                jarUrl1 + ", " + jarUrl2);
            failed++;
        } else if (!expectEqual && equal) {
            System.out.println("URLs should NOT be equal, but are. " +
                                jarUrl1 + ", " + jarUrl2);
            failed++;
        }

        if (expectEqual) {
            // hashCode MUST produce the same integer result for equal urls
            int hash1 = jarUrl1.hashCode();
            int hash2 = jarUrl2.hashCode();
            if (hash1 != hash2) {
                System.out.println("jarUrl1.hashCode = " + hash1);
                System.out.println("jarUrl2.hashCode = " + hash2);
                System.out.println("Equal urls should have same hashCode. " +
                                    jarUrl1 + ", " + jarUrl2);
                failed++;
            }
        }

        return failed;
    }
}
