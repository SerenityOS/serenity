/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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
   @summary Common definitions for general exhaustive pathname tests
   @author  Mark Reinhold
 */

import java.io.*;
import java.util.*;
import java.nio.file.*;


public class General {

    public static boolean debug = false;

    private static boolean win32 = (File.separatorChar == '\\');

    private static int gensymCounter = 0;

    protected static final String userDir = System.getProperty("user.dir");
    protected static final String workSubDir = "tmp";

    protected static String baseDir = null;
    protected static String relative = null;

    /* Generate a filename unique to this run */
    private static String gensym() {
        return "x." + ++gensymCounter;
    }

    /**
     * Create files and folders in the test working directory.
     * The purpose is to make sure the test will not go out of
     * its user dir when walking the file tree.
     *
     * @param  depth    The number of directory levels to be created under
     *                  the user directory. It should be the maximum value
     *                  of the depths passed to checkNames method (including
     *                  direct or indirect calling) in a whole test.
     */
    protected static void initTestData(int depth) throws IOException {
        File parent = new File(userDir + File.separator + workSubDir);
        if (!parent.mkdir()) {
            throw new IOException("Fail to create directory: " + parent);
        }
        for (int i = 0; i < depth; i++) {
            File tmp = new File(parent, gensym());
            tmp.createNewFile();
            tmp = new File(parent, gensym());
            if (tmp.mkdir())
                parent = tmp;
            else
                throw new IOException("Fail to create directory, " + tmp);
        }
        baseDir = parent.getAbsolutePath();
        relative = baseDir.substring(userDir.length() + 1);
    }

    /**
     * Find a file in the given subdirectory, or descend into further
     * subdirectories, if any, if no file is found here.  Return null if no
     * file can be found anywhere beneath the given subdirectory.
     * @param  dir     Directory at which we started
     * @param  subdir  Subdirectory that we're exploring
     * @param  dl      Listing of subdirectory
     */
    private static String findSomeFile(String dir, String subdir, String[] dl) {
        for (int i = 0; i < dl.length; i++) {
            File f = new File(subdir, dl[i]);
            File df = new File(dir, f.getPath());
            if (Files.isRegularFile(df.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                return f.getPath();
            }
        }
        for (int i = 0; i < dl.length; i++) {
            File f = (subdir.length() == 0) ? new File(dl[i])
                                            : new File(subdir, dl[i]);
            File df = new File(dir, f.getPath());
            if (Files.isDirectory(df.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                String[] dl2 = df.list();
                if (dl2 != null) {
                    String ff = findSomeFile(dir, f.getPath(), dl2);
                    if (ff != null) return ff;
                }
            }
        }
        return null;
    }


    /**
     * Construct a string that names a file in the given directory.  If create
     * is true, then create a file if none is found, and throw an exception if
     * that is not possible; otherwise, return null if no file can be found.
     */
    private static String findSomeFile(String dir, boolean create) {
        File d = new File(dir);
        String[] dl = d.list();
        if (dl == null) {
            throw new RuntimeException("Can't list " + dir);
        }
        for (int i = 0; i < dl.length; i++) {
            File f = new File(dir, dl[i]);
            if (Files.isRegularFile(f.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                return dl[i];
            }
        }
        String f = findSomeFile(dir, "", dl);
        if (f != null) {
            return f;
        }
        if (create) {
            File nf = new File(d, gensym());
            OutputStream os;
            try {
                os = new FileOutputStream(nf);
                os.close();
            } catch (IOException x) {
                throw new RuntimeException("Can't create a file in " + dir);
            }
            return nf.getName();
        }
        return null;
    }


    /**
     * Construct a string that names a subdirectory of the given directory.
     * If create is true, then create a subdirectory if none is found, and
     * throw an exception if that is not possible; otherwise, return null if
     * no subdirectory can be found.
     */
    private static String findSomeDir(String dir, boolean create) {
        File d = new File(dir);
        String[] dl = d.list();
        if (dl == null) {
            throw new RuntimeException("Can't list " + dir);
        }
        for (int i = 0; i < dl.length; i++) {
            File f = new File(d, dl[i]);
            if (Files.isDirectory(f.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                String[] dl2 = f.list();
                if (dl2 == null || dl2.length >= 250) {
                    /* Heuristic to avoid scanning huge directories */
                    continue;
                }
                return dl[i];
            }
        }
        if (create) {
            File sd = new File(d, gensym());
            if (sd.mkdir()) return sd.getName();
        }
        return null;
    }


    /** Construct a string that does not name a file in the given directory */
    private static String findNon(String dir) {
        File d = new File(dir);
        String[] x = new String[] { "foo", "bar", "baz" };
        for (int i = 0; i < x.length; i++) {
            File f = new File(d, x[i]);
            if (!f.exists()) {
                return x[i];
            }
        }
        for (int i = 0; i < 1024; i++) {
            String n = "xx" + Integer.toString(i);
            File f = new File(d, n);
            if (!f.exists()) {
                return n;
            }
        }
        throw new RuntimeException("Can't find a non-existent file in " + dir);
    }


    /** Ensure that the named file does not exist */
    public static void ensureNon(String fn) {
        if ((new File(fn)).exists()) {
            throw new RuntimeException("Test path " + fn + " exists");
        }
    }


    /** Tell whether the given character is a "slash" on this platform */
    private static boolean isSlash(char x) {
        if (x == File.separatorChar) return true;
        if (win32 && (x == '/')) return true;
        return false;
    }


    /**
     * Trim trailing slashes from the given string, but leave singleton slashes
     * alone (they denote root directories)
     */
    private static String trimTrailingSlashes(String s) {
        int n = s.length();
        if (n == 0) return s;
        n--;
        while ((n > 0) && isSlash(s.charAt(n))) {
            if ((n >= 1) && s.charAt(n - 1) == ':') break;
            n--;
        }
        return s.substring(0, n + 1);
    }


    /** Concatenate two paths, trimming slashes as needed */
    private static String pathConcat(String a, String b) {
        if (a.length() == 0) return b;
        if (b.length() == 0) return a;
        if (isSlash(a.charAt(a.length() - 1))
            || isSlash(b.charAt(0))
            || (win32 && (a.charAt(a.length() - 1) == ':'))) {
            return a + b;
        } else {
            return a + File.separatorChar + b;
        }
    }



    /** Hash table of input pathnames, used to detect duplicates */
    private static Hashtable<String, String> checked = new Hashtable<>();

    /**
     * Check the given pathname.  Its canonical pathname should be the given
     * answer.  If the path names a file that exists and is readable, then
     * FileInputStream and RandomAccessFile should both be able to open it.
     */
    public static void check(String answer, String path) throws IOException {
        String ans = trimTrailingSlashes(answer);
        if (path.length() == 0) return;
        if (checked.get(path) != null) {
            System.err.println("DUP " + path);
            return;
        }
        checked.put(path, path);

        String cpath;
        try {
            File f = new File(path);
            cpath = f.getCanonicalPath();
            if (f.exists() && f.isFile() && f.canRead()) {
                InputStream in = new FileInputStream(path);
                in.close();
                RandomAccessFile raf = new RandomAccessFile(path, "r");
                raf.close();
            }
        } catch (IOException x) {
            System.err.println(ans + " <-- " + path + " ==> " + x);
            if (debug) return;
            else throw x;
        }
        if (cpath.equals(ans)) {
            System.err.println(ans + " <== " + path);
        } else {
            System.err.println(ans + " <-- " + path + " ==> " + cpath + " MISMATCH");
            if (!debug) {
                throw new RuntimeException("Mismatch: " + path + " ==> " + cpath +
                                           ", should be " + ans);
            }
        }
    }



    /*
     * The following three mutually-recursive methods generate and check a tree
     * of filenames of arbitrary depth.  Each method has (at least) these
     * arguments:
     *
     *     int depth         Remaining tree depth
     *     boolean create    Controls whether test files and directories
     *                       will be created as needed
     *     String ans        Expected answer for the check method (above)
     *     String ask        Input pathname to be passed to the check method
     */


    /** Check a single slash case, plus its children */
    private static void checkSlash(int depth, boolean create,
                                  String ans, String ask, String slash)
        throws Exception
    {
        check(ans, ask + slash);
        checkNames(depth, create,
                   ans.endsWith(File.separator) ? ans : ans + File.separator,
                   ask + slash);
    }


    /** Check slash cases for the given ask string */
    public static void checkSlashes(int depth, boolean create,
                                    String ans, String ask)
        throws Exception
    {
        check(ans, ask);
        if (depth == 0) return;

        checkSlash(depth, create, ans, ask, "/");
        checkSlash(depth, create, ans, ask, "//");
        checkSlash(depth, create, ans, ask, "///");
        if (win32) {
            checkSlash(depth, create, ans, ask, "\\");
            checkSlash(depth, create, ans, ask, "\\\\");
            checkSlash(depth, create, ans, ask, "\\/");
            checkSlash(depth, create, ans, ask, "/\\");
            checkSlash(depth, create, ans, ask, "\\\\\\");
        }
    }


    /** Check name cases for the given ask string */
    public static void checkNames(int depth, boolean create,
                                  String ans, String ask)
        throws Exception
    {
        int d = depth - 1;
        File f = new File(ans);
        String n;

        /* Normal name */
        if (f.exists()) {
            if (Files.isDirectory(f.toPath(), LinkOption.NOFOLLOW_LINKS) && f.list() != null) {
                if ((n = findSomeFile(ans, create)) != null)
                    checkSlashes(d, create, ans + n, ask + n);
                if ((n = findSomeDir(ans, create)) != null)
                    checkSlashes(d, create, ans + n, ask + n);
            }
            n = findNon(ans);
            checkSlashes(d, create, ans + n, ask + n);
        } else {
            n = "foo" + depth;
            checkSlashes(d, create, ans + n, ask + n);
        }

        /* "." */
        checkSlashes(d, create, trimTrailingSlashes(ans), ask + ".");

        /* ".." */
        if ((n = f.getParent()) != null) {
            String n2;
            if (win32
                && ((n2 = f.getParentFile().getParent()) != null)
                && n2.equals("\\\\")) {
                /* Win32 resolves \\foo\bar\.. to \\foo\bar */
                checkSlashes(d, create, ans, ask + "..");
            } else {
                checkSlashes(d, create, n, ask + "..");
            }
        }
        else {
            if (win32)
                checkSlashes(d, create, ans, ask + "..");
            else {
                // Fix for 4237875. We must ensure that we are sufficiently
                // deep in the path hierarchy to test parents this high up
                File thisPath = new File(ask);
                File nextPath = new File(ask + "..");
                if (!thisPath.getCanonicalPath().equals(nextPath.getCanonicalPath()))
                    checkSlashes(d, create, ans + "..", ask + "..");
            }
        }
    }
}
