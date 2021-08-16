/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.io;

import java.util.Properties;

import jdk.internal.util.StaticProperty;
import sun.security.action.GetPropertyAction;


class UnixFileSystem extends FileSystem {

    private final char slash;
    private final char colon;
    private final String javaHome;
    private final String userDir;

    public UnixFileSystem() {
        Properties props = GetPropertyAction.privilegedGetProperties();
        slash = props.getProperty("file.separator").charAt(0);
        colon = props.getProperty("path.separator").charAt(0);
        javaHome = StaticProperty.javaHome();
        userDir = StaticProperty.userDir();
        cache = useCanonCaches ? new ExpiringCache() : null;
        javaHomePrefixCache = useCanonPrefixCache ? new ExpiringCache() : null;
    }


    /* -- Normalization and construction -- */

    @Override
    public char getSeparator() {
        return slash;
    }

    @Override
    public char getPathSeparator() {
        return colon;
    }

    /* A normal Unix pathname contains no duplicate slashes and does not end
       with a slash.  It may be the empty string. */

    /**
     * Normalize the given pathname, starting at the given
     * offset; everything before off is already normal, and there's at least
     * one duplicate or trailing slash to be removed
     */
    private String normalize(String pathname, int off) {
        int n = pathname.length();
        while ((n > off) && (pathname.charAt(n - 1) == '/')) n--;
        if (n == 0) return "/";
        if (n == off) return pathname.substring(0, off);

        StringBuilder sb = new StringBuilder(n);
        if (off > 0) sb.append(pathname, 0, off);
        char prevChar = 0;
        for (int i = off; i < n; i++) {
            char c = pathname.charAt(i);
            if ((prevChar == '/') && (c == '/')) continue;
            sb.append(c);
            prevChar = c;
        }
        return sb.toString();
    }

    /* Check that the given pathname is normal.  If not, invoke the real
       normalizer on the part of the pathname that requires normalization.
       This way we iterate through the whole pathname string only once. */
    @Override
    public String normalize(String pathname) {
        int doubleSlash = pathname.indexOf("//");
        if (doubleSlash >= 0) {
            return normalize(pathname, doubleSlash);
        }
        if (pathname.endsWith("/")) {
            return normalize(pathname, pathname.length() - 1);
        }
        return pathname;
    }

    @Override
    public int prefixLength(String pathname) {
        return pathname.startsWith("/") ? 1 : 0;
    }

    @Override
    public String resolve(String parent, String child) {
        if (child.isEmpty()) return parent;
        if (child.charAt(0) == '/') {
            if (parent.equals("/")) return child;
            return parent + child;
        }
        if (parent.equals("/")) return parent + child;
        return parent + '/' + child;
    }

    @Override
    public String getDefaultParent() {
        return "/";
    }

    @Override
    public String fromURIPath(String path) {
        String p = path;
        if (p.endsWith("/") && (p.length() > 1)) {
            // "/foo/" --> "/foo", but "/" --> "/"
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }


    /* -- Path operations -- */

    @Override
    public boolean isAbsolute(File f) {
        return (f.getPrefixLength() != 0);
    }

    @Override
    public String resolve(File f) {
        if (isAbsolute(f)) return f.getPath();
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPropertyAccess("user.dir");
        }
        return resolve(userDir, f.getPath());
    }

    // Caches for canonicalization results to improve startup performance.
    // The first cache handles repeated canonicalizations of the same path
    // name. The prefix cache handles repeated canonicalizations within the
    // same directory, and must not create results differing from the true
    // canonicalization algorithm in canonicalize_md.c. For this reason the
    // prefix cache is conservative and is not used for complex path names.
    private final ExpiringCache cache;
    // On Unix symlinks can jump anywhere in the file system, so we only
    // treat prefixes in java.home as trusted and cacheable in the
    // canonicalization algorithm
    private final ExpiringCache javaHomePrefixCache;

    @Override
    public String canonicalize(String path) throws IOException {
        if (!useCanonCaches) {
            return canonicalize0(path);
        } else {
            String res = cache.get(path);
            if (res == null) {
                String dir = null;
                String resDir;
                if (useCanonPrefixCache) {
                    // Note that this can cause symlinks that should
                    // be resolved to a destination directory to be
                    // resolved to the directory they're contained in
                    dir = parentOrNull(path);
                    if (dir != null) {
                        resDir = javaHomePrefixCache.get(dir);
                        if (resDir != null) {
                            // Hit only in prefix cache; full path is canonical
                            String filename = path.substring(1 + dir.length());
                            res = resDir + slash + filename;
                            cache.put(dir + slash + filename, res);
                        }
                    }
                }
                if (res == null) {
                    res = canonicalize0(path);
                    cache.put(path, res);
                    if (useCanonPrefixCache &&
                        dir != null && dir.startsWith(javaHome)) {
                        resDir = parentOrNull(res);
                        // Note that we don't allow a resolved symlink
                        // to elsewhere in java.home to pollute the
                        // prefix cache (java.home prefix cache could
                        // just as easily be a set at this point)
                        if (resDir != null && resDir.equals(dir)) {
                            File f = new File(res);
                            if (f.exists() && !f.isDirectory()) {
                                javaHomePrefixCache.put(dir, resDir);
                            }
                        }
                    }
                }
            }
            return res;
        }
    }
    private native String canonicalize0(String path) throws IOException;
    // Best-effort attempt to get parent of this path; used for
    // optimization of filename canonicalization. This must return null for
    // any cases where the code in canonicalize_md.c would throw an
    // exception or otherwise deal with non-simple pathnames like handling
    // of "." and "..". It may conservatively return null in other
    // situations as well. Returning null will cause the underlying
    // (expensive) canonicalization routine to be called.
    static String parentOrNull(String path) {
        if (path == null) return null;
        char sep = File.separatorChar;
        int last = path.length() - 1;
        int idx = last;
        int adjacentDots = 0;
        int nonDotCount = 0;
        while (idx > 0) {
            char c = path.charAt(idx);
            if (c == '.') {
                if (++adjacentDots >= 2) {
                    // Punt on pathnames containing . and ..
                    return null;
                }
            } else if (c == sep) {
                if (adjacentDots == 1 && nonDotCount == 0) {
                    // Punt on pathnames containing . and ..
                    return null;
                }
                if (idx == 0 ||
                    idx >= last - 1 ||
                    path.charAt(idx - 1) == sep) {
                    // Punt on pathnames containing adjacent slashes
                    // toward the end
                    return null;
                }
                return path.substring(0, idx);
            } else {
                ++nonDotCount;
                adjacentDots = 0;
            }
            --idx;
        }
        return null;
    }

    /* -- Attribute accessors -- */

    public native int getBooleanAttributes0(File f);

    @Override
    public int getBooleanAttributes(File f) {
        int rv = getBooleanAttributes0(f);
        return rv | isHidden(f);
    }

    @Override
    public boolean hasBooleanAttributes(File f, int attributes) {
        int rv = getBooleanAttributes0(f);
        if ((attributes & BA_HIDDEN) != 0) {
            rv |= isHidden(f);
        }
        return (rv & attributes) == attributes;
    }

    private static int isHidden(File f) {
        return f.getName().startsWith(".") ? BA_HIDDEN : 0;
    }

    @Override
    public native boolean checkAccess(File f, int access);

    @Override
    public native long getLastModifiedTime(File f);

    @Override
    public native long getLength(File f);

    @Override
    public native boolean setPermission(File f, int access, boolean enable, boolean owneronly);

    /* -- File operations -- */

    @Override
    public native boolean createFileExclusively(String path)
        throws IOException;

    @Override
    public boolean delete(File f) {
        // Keep canonicalization caches in sync after file deletion
        // and renaming operations. Could be more clever than this
        // (i.e., only remove/update affected entries) but probably
        // not worth it since these entries expire after 30 seconds
        // anyway.
        if (useCanonCaches) {
            cache.clear();
        }
        if (useCanonPrefixCache) {
            javaHomePrefixCache.clear();
        }
        return delete0(f);
    }
    private native boolean delete0(File f);

    @Override
    public native String[] list(File f);

    @Override
    public native boolean createDirectory(File f);

    @Override
    public boolean rename(File f1, File f2) {
        // Keep canonicalization caches in sync after file deletion
        // and renaming operations. Could be more clever than this
        // (i.e., only remove/update affected entries) but probably
        // not worth it since these entries expire after 30 seconds
        // anyway.
        if (useCanonCaches) {
            cache.clear();
        }
        if (useCanonPrefixCache) {
            javaHomePrefixCache.clear();
        }
        return rename0(f1, f2);
    }
    private native boolean rename0(File f1, File f2);

    @Override
    public native boolean setLastModifiedTime(File f, long time);

    @Override
    public native boolean setReadOnly(File f);

    /* -- Filesystem interface -- */

    @Override
    public File[] listRoots() {
        try {
            @SuppressWarnings("removal")
            SecurityManager security = System.getSecurityManager();
            if (security != null) {
                security.checkRead("/");
            }
            return new File[] { new File("/") };
        } catch (SecurityException x) {
            return new File[0];
        }
    }

    /* -- Disk usage -- */

    @Override
    public native long getSpace(File f, int t);

    /* -- Basic infrastructure -- */

    private native long getNameMax0(String path);

    @Override
    public int getNameMax(String path) {
        long nameMax = getNameMax0(path);
        if (nameMax > Integer.MAX_VALUE) {
            nameMax = Integer.MAX_VALUE;
        }
        return (int)nameMax;
    }

    @Override
    public int compare(File f1, File f2) {
        return f1.getPath().compareTo(f2.getPath());
    }

    @Override
    public int hashCode(File f) {
        return f.getPath().hashCode() ^ 1234321;
    }


    private static native void initIDs();

    static {
        initIDs();
    }
}
