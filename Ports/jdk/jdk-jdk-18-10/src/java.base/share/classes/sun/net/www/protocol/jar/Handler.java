/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.net.www.protocol.jar;

import java.io.IOException;
import java.net.*;

/*
 * Jar URL Handler
 */
public class Handler extends java.net.URLStreamHandler {

    private static final String separator = "!/";

    protected java.net.URLConnection openConnection(URL u)
    throws IOException {
        return new JarURLConnection(u, this);
    }

    private static int indexOfBangSlash(String spec) {
        int indexOfBang = spec.length();
        while((indexOfBang = spec.lastIndexOf('!', indexOfBang)) != -1) {
            if ((indexOfBang != (spec.length() - 1)) &&
                (spec.charAt(indexOfBang + 1) == '/')) {
                return indexOfBang + 1;
            } else {
                indexOfBang--;
            }
        }
        return -1;
    }

    /**
     * Compare two jar URLs
     */
    @Override
    protected boolean sameFile(URL u1, URL u2) {
        if (!u1.getProtocol().equals("jar") || !u2.getProtocol().equals("jar"))
            return false;

        String file1 = u1.getFile();
        String file2 = u2.getFile();
        int sep1 = file1.indexOf(separator);
        int sep2 = file2.indexOf(separator);

        if (sep1 == -1 || sep2 == -1) {
            return super.sameFile(u1, u2);
        }

        String entry1 = file1.substring(sep1 + 2);
        String entry2 = file2.substring(sep2 + 2);

        if (!entry1.equals(entry2))
            return false;

        URL enclosedURL1 = null, enclosedURL2 = null;
        try {
            enclosedURL1 = new URL(file1.substring(0, sep1));
            enclosedURL2 = new URL(file2.substring(0, sep2));
        } catch (MalformedURLException unused) {
            return super.sameFile(u1, u2);
        }

        if (!super.sameFile(enclosedURL1, enclosedURL2)) {
            return false;
        }

        return true;
    }

    @Override
    protected int hashCode(URL u) {
        int h = 0;

        String protocol = u.getProtocol();
        if (protocol != null)
            h += protocol.hashCode();

        String file = u.getFile();
        int sep = file.indexOf(separator);

        if (sep == -1)
            return h + file.hashCode();

        URL enclosedURL = null;
        String fileWithoutEntry = file.substring(0, sep);
        try {
            enclosedURL = new URL(fileWithoutEntry);
            h += enclosedURL.hashCode();
        } catch (MalformedURLException unused) {
            h += fileWithoutEntry.hashCode();
        }

        String entry = file.substring(sep + 2);
        h += entry.hashCode();

        return h;
    }

    public String checkNestedProtocol(String spec) {
        if (spec.regionMatches(true, 0, "jar:", 0, 4)) {
            return "Nested JAR URLs are not supported";
        } else {
            return null;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void parseURL(URL url, String spec,
                            int start, int limit) {
        String file = null;
        String ref = null;
        // first figure out if there is an anchor
        int refPos = spec.indexOf('#', limit);
        boolean refOnly = refPos == start;
        if (refPos > -1) {
            ref = spec.substring(refPos + 1, spec.length());
            if (refOnly) {
                file = url.getFile();
            }
        }
        // then figure out if the spec is
        // 1. absolute (jar:)
        // 2. relative (i.e. url + foo/bar/baz.ext)
        // 3. anchor-only (i.e. url + #foo), which we already did (refOnly)
        boolean absoluteSpec = spec.length() >= 4
                ? spec.regionMatches(true, 0, "jar:", 0, 4)
                : false;
        spec = spec.substring(start, limit);

        String exceptionMessage = checkNestedProtocol(spec);
        if (exceptionMessage != null) {
            // NPE will be transformed into MalformedURLException by the caller
            throw new NullPointerException(exceptionMessage);
        }

        if (absoluteSpec) {
            file = parseAbsoluteSpec(spec);
        } else if (!refOnly) {
            file = parseContextSpec(url, spec);

            // Canonicalize the result after the bangslash
            int bangSlash = indexOfBangSlash(file);
            file = canonicalizeString(file, bangSlash);
        }
        setURL(url, "jar", "", -1, file, ref);
    }

    private String parseAbsoluteSpec(String spec) {
        int index;
        // check for !/
        if ((index = indexOfBangSlash(spec)) == -1) {
            throw new NullPointerException("no !/ in spec");
        }
        // test the inner URL
        try {
            String innerSpec = spec.substring(0, index - 1);
            new URL(innerSpec);
        } catch (MalformedURLException e) {
            throw new NullPointerException("invalid url: " +
                                           spec + " (" + e + ")");
        }
        return spec;
    }

    private String parseContextSpec(URL url, String spec) {
        String ctxFile = url.getFile();
        // if the spec begins with /, chop up the jar back !/
        if (spec.startsWith("/")) {
            int bangSlash = indexOfBangSlash(ctxFile);
            if (bangSlash == -1) {
                throw new NullPointerException("malformed " +
                                               "context url:" +
                                               url +
                                               ": no !/");
            }
            ctxFile = ctxFile.substring(0, bangSlash);
        } else {
            // chop up the last component
            int lastSlash = ctxFile.lastIndexOf('/');
            if (lastSlash == -1) {
                throw new NullPointerException("malformed " +
                                               "context url:" +
                                               url);
            } else if (lastSlash < ctxFile.length() - 1) {
                ctxFile = ctxFile.substring(0, lastSlash + 1);
            }
        }
        return (ctxFile + spec);
    }

    /**
     * Returns a version of the specified string with
     * canonicalization applied starting from position {@code off}
     */
    private static String canonicalizeString(String file, int off) {
        int len = file.length();
        if (off >= len || (file.indexOf("./", off) == -1 && file.charAt(len - 1) != '.')) {
            return file;
        } else {
            // Defer substring and concat until canonicalization is required
            String before = file.substring(0, off);
            String after = file.substring(off);
            return before + doCanonicalize(after);
        }
    }

    private static String doCanonicalize(String file) {
        int i, lim;

        // Remove embedded /../
        while ((i = file.indexOf("/../")) >= 0) {
            if ((lim = file.lastIndexOf('/', i - 1)) >= 0) {
                file = file.substring(0, lim) + file.substring(i + 3);
            } else {
                file = file.substring(i + 3);
            }
        }
        // Remove embedded /./
        while ((i = file.indexOf("/./")) >= 0) {
            file = file.substring(0, i) + file.substring(i + 2);
        }
        // Remove trailing ..
        while (file.endsWith("/..")) {
            i = file.indexOf("/..");
            if ((lim = file.lastIndexOf('/', i - 1)) >= 0) {
                file = file.substring(0, lim+1);
            } else {
                file = file.substring(0, i);
            }
        }
        // Remove trailing .
        if (file.endsWith("/."))
            file = file.substring(0, file.length() -1);

        return file;
    }
}
