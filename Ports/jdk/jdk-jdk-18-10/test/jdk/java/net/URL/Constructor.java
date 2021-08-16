/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4393671
 * @summary URL constructor URL(URL context, String spec) FAILED with specific input
 */

/*
 * This program tests the URL parser in the URL constructor. It
 * tries to construct a variety of valid URLs with a given context
 * (which may be null) and a variety of specs. It then compares the
 * result with an expected value.
 */

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Constructor {

    public static void main(String[] args) throws Exception {
        List<Entry> entries = new ArrayList<>();
        entries.addAll(Arrays.asList(fileURLs));
        entries.addAll(Arrays.asList(jarURLs));
        entries.addAll(Arrays.asList(normalHttpURLs));
        entries.addAll(Arrays.asList(abnormalHttpURLs));
        if (hasFtp())
            entries.addAll(Arrays.asList(ftpURLs));
        URL url;

        for (Entry e : entries) {
            if (e.context == null)
                url = new URL(e.spec);
            else
                url = new URL(new URL(e.context), e.spec);

            if (!(url.toString().equals(e.expected))) {
                throw new RuntimeException("error for: \n\tURL:" + e.context +
                                           "\n\tspec: " + e.spec +
                                           "\n\texpected: " + e.expected +
                                           "\n\tactual: " + url.toString());
            } else {
                //debug
                //System.out.println("success for: " + url);
            }
        }
    }

    private static boolean hasFtp() {
        try {
            return new java.net.URL("ftp://") != null;
        } catch (java.net.MalformedURLException x) {
            System.out.println("FTP not supported by this runtime.");
            return false;
        }
    }

    static class Entry {
        final String context;
        final String spec;
        final String expected;
        Entry(String context, String spec, String expected) {
            this.context = context;
            this.spec =spec;
            this.expected = expected;
        }
    }

    static Entry[] fileURLs = new Entry[] {
        new Entry(null,
                  "file://JavaSoft/Test",
                  "file://JavaSoft/Test"),
        new Entry(null,
                  "file:///JavaSoft/Test",
                  "file:/JavaSoft/Test"),
        new Entry(null,
                  "file:/JavaSoft/Test",
                  "file:/JavaSoft/Test"),
        new Entry(null,
                  "file:/c:/JavaSoft/Test",
                  "file:/c:/JavaSoft/Test"),
        new Entry(null,
                  "file:/c:/JavaSoft/Test:something",
                  "file:/c:/JavaSoft/Test:something"),
        new Entry(null,
                  "file:/c:/JavaSoft/Test#anchor",
                  "file:/c:/JavaSoft/Test#anchor"),
        new Entry("file://JavaSoft/Test",
                  "Test#bar",
                  "file://JavaSoft/Test#bar"),
        new Entry("file://codrus/c:/jdk/eng/index.html",
                  "pulsar.html",
                  "file://codrus/c:/jdk/eng/pulsar.html"),
        new Entry("file:///c:/jdk/eng/index.html",
                  "pulsar.html",
                  "file:/c:/jdk/eng/pulsar.html"),
        new Entry("file:///jdk/eng/index.html",
                  "pulsar.html",
                  "file:/jdk/eng/pulsar.html"),
        new Entry("file://JavaSoft/Test",
                  "file://radartoad.com/Test#bar",
                  "file://radartoad.com/Test#bar"),
        new Entry("file://JavaSoft/Test",
                  "/c:/Test#bar",
                  "file://JavaSoft/c:/Test#bar"),
    };

    static Entry[] jarURLs = new Entry[] {
        new Entry(null,
                  "jar:http://www.foo.com/dir1/jar.jar!/dir2/entry.txt",
                  "jar:http://www.foo.com/dir1/jar.jar!/dir2/entry.txt"),
        new Entry(null,
                  "jar:http://www.foo.com/dir1/jar.jar!/",
                  "jar:http://www.foo.com/dir1/jar.jar!/"),
        new Entry(null,
                  "jar:http://www.foo.com/dir1/jar.jar!/",
                  "jar:http://www.foo.com/dir1/jar.jar!/"),
        new Entry("jar:http://www.foo.com/dir1/jar.jar!/",
                  "entry.txt",
                  "jar:http://www.foo.com/dir1/jar.jar!/entry.txt"),
        new Entry("jar:http://www.foo.com/dir1/jar.jar!/",
                  "/entry.txt",
                  "jar:http://www.foo.com/dir1/jar.jar!/entry.txt"),
        new Entry("jar:http://www.foo.com/dir1/jar.jar!/",
                  "dir1/entry.txt",
                  "jar:http://www.foo.com/dir1/jar.jar!/dir1/entry.txt"),
        new Entry("jar:http://www.foo.com/dir1/jar.jar!/",
                  "/dir1/entry.txt",
                  "jar:http://www.foo.com/dir1/jar.jar!/dir1/entry.txt"),
        new Entry("jar:http://www.foo.com/dir1/jar.jar!/",
                  "entry.txt",
                  "jar:http://www.foo.com/dir1/jar.jar!/entry.txt"),
        new Entry("jar:http://www.foo.com/dir1/jar.jar!/",
                  "/entry.txt",
                  "jar:http://www.foo.com/dir1/jar.jar!/entry.txt"),
        new Entry("jar:http://www.foo.com/dir1/jar.jar!/",
                  "/entry.txt",
                  "jar:http://www.foo.com/dir1/jar.jar!/entry.txt"),
        new Entry("jar:http://www.foo.com/dir1/jar.jar!/dir1/",
                  "entry.txt",
                  "jar:http://www.foo.com/dir1/jar.jar!/dir1/entry.txt"),
        new Entry("jar:http://www.foo.com/dir1/jar.jar!/dir2/dir3/entry2.txt",
                  "/dir1/entry.txt",
                  "jar:http://www.foo.com/dir1/jar.jar!/dir1/entry.txt"),
        new Entry("jar:http://www.foo.com/dir1/jar.jar!/",
                  "/dir1/foo/entry.txt",
                  "jar:http://www.foo.com/dir1/jar.jar!/dir1/foo/entry.txt"),
        new Entry("jar:http://www.foo.com/dir1/jar.jar!/dir1/dir2/dir3/",
                  "dir4/foo/entry.txt",
                  "jar:http://www.foo.com/dir1/jar.jar!/dir1/dir2/dir3/dir4/foo/entry.txt"),
        new Entry("jar:http://www.foo.com/dir1/jar.jar!/",
                  "/dir1/foo/entry.txt",
                  "jar:http://www.foo.com/dir1/jar.jar!/dir1/foo/entry.txt"),
        new Entry(null,
                  "jar:http://www.foo.com/dir1/jar.jar!/foo.txt#anchor",
                  "jar:http://www.foo.com/dir1/jar.jar!/foo.txt#anchor"),
        new Entry("jar:http://www.foo.com/dir1/jar.jar!/foo.txt",
                  "#anchor",
                  "jar:http://www.foo.com/dir1/jar.jar!/foo.txt#anchor"),
        new Entry("jar:http://www.foo.com/dir1/jar.jar!/foo/bar/",
                  "baz/quux#anchor",
                  "jar:http://www.foo.com/dir1/jar.jar!/foo/bar/baz/quux#anchor"),
        new Entry("jar:http://balloo.com/olle.jar!/",
                  "p2",
                  "jar:http://balloo.com/olle.jar!/p2")
    };

    static Entry[] normalHttpURLs = new Entry[] {
        new Entry("http://a/b/c/d;p?q", "g",       "http://a/b/c/g"),
        new Entry("http://a/b/c/d;p?q", "./g",     "http://a/b/c/g"),
        new Entry("http://a/b/c/d;p?q", "g/",      "http://a/b/c/g/"),
        new Entry("http://a/b/c/d;p?q", "/g",      "http://a/g"),
        new Entry("http://a/b/c/d;p?q", "//g",     "http://g"),
        new Entry("http://a/b/c/d;p?q", "?y",      "http://a/b/c/?y"),
        new Entry("http://a/b/c/d;p?q", "g?y",     "http://a/b/c/g?y"),
        new Entry("http://a/b/c/d;p?q", "g#s",     "http://a/b/c/g#s"),
        new Entry("http://a/b/c/d;p?q", "g?y#s",   "http://a/b/c/g?y#s"),
        new Entry("http://a/b/c/d;p?q", ";x",      "http://a/b/c/;x"),
        new Entry("http://a/b/c/d;p?q", "g;x",     "http://a/b/c/g;x"),
        new Entry("http://a/b/c/d;p?q", "g;x?y#s", "http://a/b/c/g;x?y#s"),
        new Entry("http://a/b/c/d;p?q", ".",       "http://a/b/c/"),
        new Entry("http://a/b/c/d;p?q", "./",      "http://a/b/c/"),
        new Entry("http://a/b/c/d;p?q", "..",      "http://a/b/"),
        new Entry("http://a/b/c/d;p?q", "../",     "http://a/b/"),
        new Entry("http://a/b/c/d;p?q", "../g",    "http://a/b/g"),
        new Entry("http://a/b/c/d;p?q", "../..",   "http://a/"),
        new Entry("http://a/b/c/d;p?q", "../../",  "http://a/"),
        new Entry("http://a/b/c/d;p?q", "../../g", "http://a/g"),
        new Entry(null,
                  "http://www.javasoft.com/jdc/community/chat/index.html#javalive?frontpage-jdc",
                  "http://www.javasoft.com/jdc/community/chat/index.html#javalive?frontpage-jdc")
    };

    static Entry[] abnormalHttpURLs = new Entry[] {
        new Entry("http://a/b/c/d;p?q", "../../../g",    "http://a/../g"),
        new Entry("http://a/b/c/d;p?q", "../../../../g", "http://a/../../g"),
        new Entry("http://a/b/c/d;p?q", "/./g",          "http://a/./g"),
        new Entry("http://a/b/c/d;p?q", "/../g",         "http://a/../g"),
        new Entry("http://a/b/c/d;p?q", ".g",            "http://a/b/c/.g"),
        new Entry("http://a/b/c/d;p?q", "g.",            "http://a/b/c/g."),
        new Entry("http://a/b/c/d;p?q", "./../g",        "http://a/b/g"),
        new Entry("http://a/b/c/d;p?q", "./g/.",         "http://a/b/c/g/"),
        new Entry("http://a/b/c/d;p?q", "g/./h",         "http://a/b/c/g/h"),
        new Entry("http://a/b/c/d;p?q", "g;x=1/./y",     "http://a/b/c/g;x=1/y"),
        new Entry("http://a/b/c/d;p?q", "g;x=1/../y",    "http://a/b/c/y")
    };

    static Entry[] ftpURLs = new Entry[] {
        new Entry(null,
                  "ftp://ftp.foo.com/dir1/entry.txt",
                  "ftp://ftp.foo.com/dir1/entry.txt"),
        new Entry(null,
                  "ftp://br:pwd@ftp.foo.com/dir1/jar.jar",
                  "ftp://br:pwd@ftp.foo.com/dir1/jar.jar"),
        new Entry("ftp://ftp.foo.com/dir1/foo.txt",
                  "bar.txt",
                  "ftp://ftp.foo.com/dir1/bar.txt"),
        new Entry("ftp://ftp.foo.com/dir1/jar.jar",
                  "/entry.txt",
                  "ftp://ftp.foo.com/entry.txt"),
        new Entry("ftp://ftp.foo.com/dir1/jar.jar",
                  "dir1/entry.txt",
                  "ftp://ftp.foo.com/dir1/dir1/entry.txt"),
        new Entry("ftp://ftp.foo.com/dir1/jar.jar",
                  "/dir1/entry.txt",
                  "ftp://ftp.foo.com/dir1/entry.txt"),
        new Entry("ftp://br:pwd@ftp.foo.com/dir1/jar.jar",
                  "/dir1/entry.txt",
                  "ftp://br:pwd@ftp.foo.com/dir1/entry.txt")
    };
}
