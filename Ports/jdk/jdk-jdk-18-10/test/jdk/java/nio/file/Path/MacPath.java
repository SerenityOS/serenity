/*
 * Copyright (c) 2008, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.text.Normalizer;
import java.util.Set;
import java.util.regex.Pattern;

public class MacPath {

    public static void main(String args[]) throws Throwable {
        System.out.printf("sun.jnu.encoding=%s, file.encoding=%s%n",
                          System.getProperty("file.encoding"),
                          System.getProperty("sun.jnu.encoding"));
        // English
        test("TestDir_apple",                                    // test dir
             "dir_macosx",                                       // dir
             "file_macosx");                                     // file

        // Japanese composite character
        test("TestDir_\u30c8\u30a4\u30e4\u30cb\u30ca\u30eb/",
             "dir_\u30a4\u30c1\u30b4\u306e\u30b1\u30fc\u30ad",
             "file_\u30a4\u30c1\u30b4\u306e\u30b1\u30fc\u30ad");

        // latin-1 supplementory
        test("TestDir_K\u00f6rperlich\u00e4\u00df/",
             "dir_Entt\u00e4uschung",
             "file_Entt\u00e4uschung");

        test("TestDir_K\u00f6rperlich\u00e4\u00df/",
             "dir_Entt\u00c4uschung",
             "file_Entt\u00c4uschung");

        // Korean syblla
        test("TestDir_\uac00\uac01\uac02",
             "dir_\uac20\uac21\uac22",
             "file_\uacc0\uacc1\uacc2");
    }

    private static boolean equal(Object x, Object y) {
        return x == null ? y == null : x.equals(y);
    }

    private static boolean match(Path target, Path src) {
        String fname = target.toString();
        System.out.printf("    --> Trying  [%s], length=%d...", fname, fname.length());
        if (target.equals(src)) {
            System.out.println(" MATCHED!");
            return true;
        } else {
            System.out.println(" NOT MATCHED!");
        }
        return false;
    }

    private static void test(String testdir, String dname, String fname_nfc)
        throws Throwable
    {
        String fname = null;
        String dname_nfd = Normalizer.normalize(dname, Normalizer.Form.NFD);
        String fname_nfd = Normalizer.normalize(fname_nfc, Normalizer.Form.NFD);

        System.out.printf("%n%n--------Testing...----------%n");
        Path bpath = Paths.get(testdir);
        Path dpath = Paths.get(testdir, dname);
        Path dpath_nfd = Paths.get(testdir, dname_nfd);
        Path fpath_nfc = Paths.get(testdir, fname_nfc);
        Path fpath_nfd = Paths.get(testdir, fname_nfd);

        if (Files.exists(bpath))
            TestUtil.removeAll(bpath);
        Files.createDirectories(dpath);

        fname = dpath.toString();
        System.out.printf(":Directory [%s][len=%d] created%n", fname, fname.length());

        //////////////////////////////////////////////////////////////
        if (!Files.isDirectory(dpath) || !Files.isDirectory(dpath_nfd)) {
            throw new RuntimeException("Files.isDirectory(...) failed");
        }

        //////////////////////////////////////////////////////////////
        // write out with nfd, read in with nfc + case
        Files.write(fpath_nfd, new byte[] { 'n', 'f', 'd'});
        System.out.println("    read in with nfc      (from nfd):" + new String(Files.readAllBytes(fpath_nfc)));

        // check attrs with nfc + case
        Set<PosixFilePermission> pfp = Files.getPosixFilePermissions(fpath_nfd);
        if (!equal(pfp, Files.getPosixFilePermissions(fpath_nfc)) ) {
            throw new RuntimeException("Files.getPosixfilePermission(...) failed");
        }
        Files.delete(fpath_nfd);

        // write out with nfc, read in with nfd + case
        Files.write(fpath_nfc, new byte[] { 'n', 'f', 'c'});
        System.out.println("    read in with nfd      (from nfc):" + new String(Files.readAllBytes(fpath_nfd)));

        // check attrs with nfc + case
        pfp = Files.getPosixFilePermissions(fpath_nfc);
        if (!equal(pfp, Files.getPosixFilePermissions(fpath_nfd))) {
            throw new RuntimeException("Files.getPosixfilePermission(...) failed");
        }
        //////////////////////////////////////////////////////////////
        boolean found_dir = false;
        boolean found_file_nfc = false;
        boolean found_file_nfd = false;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(bpath)) {
            for (Path path: stream) {
                fname = path.toString();
                System.out.printf("Found   : [%s], length=%d%n", fname, fname.length());
                found_dir      |= match(dpath, path);
                found_file_nfc |= match(fpath_nfc, path);
                found_file_nfd |= match(fpath_nfd, path);
            }
        }
        if (!found_dir || !found_file_nfc || !found_file_nfd) {
            throw new RuntimeException("File.equal() failed");
        }
        // glob
        String glob = "*" + fname_nfd.substring(2);  // remove leading "FI" from "FILE..."
        System.out.println("glob=" + glob);
        boolean globmatched = false;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(bpath, glob)) {
            for (Path path: stream) {
                fname = path.toString();
                System.out.printf("PathMatch : [%s], length=%d%n", fname, fname.length());
                globmatched |= match(fpath_nfc, path);
            }
        }
        if (!globmatched) {
            //throw new RuntimeException("path matcher failed");
            // it appears we have a regex.anon_eq bug in hangul syllable handling
            System.out.printf("pathmatcher failed, glob=[%s]%n", glob);
            System.out.printf("    -> fname_nfd.matches(fname_nfc)=%b%n",
                              Pattern.compile(fname_nfd, Pattern.CANON_EQ)
                                     .matcher(fname_nfc)
                                     .matches());
            System.out.printf("    -> fname_nfc.matches(fname_nfd)=%b%n",
                              Pattern.compile(fname_nfc, Pattern.CANON_EQ)
                                     .matcher(fname_nfd)
                                     .matches());
        }
        TestUtil.removeAll(bpath);
    }
}
