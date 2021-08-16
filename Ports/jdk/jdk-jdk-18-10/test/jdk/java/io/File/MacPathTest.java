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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.Normalizer;

public class MacPathTest {

    public static void main(String args[]) throws Throwable {
        String osname = System.getProperty("os.name");
        if (!osname.contains("OS X") && !osname.contains("Darwin"))
            return;

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

    private static void removeAll(File file) throws Throwable {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                removeAll(f);
            }
        }
        file.delete();
    }

    private static boolean equal(Object x, Object y) {
        return x == null ? y == null : x.equals(y);
    }

    private static boolean match(File target, File src) {
        if (target.equals(src)) {
            String fname = target.toString();
            System.out.printf("    ->matched   : [%s], length=%d%n", fname, fname.length());
            return true;
        }
        return false;
    }

    private static void open_read(String what, File file) throws Throwable {
        try (FileInputStream fis = new FileInputStream(file)) {
           byte[] bytes = new byte[10];
           fis.read(bytes);
           System.out.printf("    %s:%s%n", what, new String(bytes));
        }
    }

    private static void test(String testdir, String dname, String fname_nfc)
        throws Throwable
    {
        String fname = null;
        String dname_nfd = Normalizer.normalize(dname, Normalizer.Form.NFD);
        String fname_nfd = Normalizer.normalize(fname_nfc, Normalizer.Form.NFD);

        System.out.printf("%n%n--------Testing...----------%n");
        File base = new File(testdir);
        File dir  = new File(base, dname);
        File dir_nfd =  new File(base, dname_nfd);
        File file_nfc = new File(base, fname_nfc);
        File file_nfd = new File(base, fname_nfd);

        System.out.printf("base           :[%s][len=%d]%n", testdir, testdir.length());
        System.out.printf("dir            :[%s][len=%d]%n", dname, dname.length());
        System.out.printf("fname_nfc      :[%s][len=%d]%n", fname_nfc, fname_nfc.length());
        System.out.printf("fname_nfd      :[%s][len=%d]%n", fname_nfd, fname_nfd.length());

        fname = file_nfc.toString();
        System.out.printf("file_nfc ->[%s][len=%d]%n", fname, fname.length());
        fname = file_nfd.toString();
        System.out.printf("file_nfd ->[%s][len=%d]%n%n", fname, fname.length());

        removeAll(base);
        dir.mkdirs();

        fname = dir.toString();
        System.out.printf(":Directory [%s][len=%d] created%n", fname, fname.length());

        //////////////////////////////////////////////////////////////
        if (!dir.isDirectory() || !dir_nfd.isDirectory()) {
            throw new RuntimeException("File.isDirectory() failed");
        }

        //////////////////////////////////////////////////////////////
        // write to via nfd
        try (FileOutputStream fos = new FileOutputStream(file_nfd)) {
           fos.write('n'); fos.write('f'); fos.write('d');
        }
        open_read("read in with nfc (from nfd)", file_nfc);
        file_nfd.delete();

        //////////////////////////////////////////////////////////////
        // write to with nfc
        try (FileOutputStream fos = new FileOutputStream(file_nfc)) {
           fos.write('n'); fos.write('f'); fos.write('c');
        }
        open_read("read in with nfd      (from nfc)", file_nfd);
        //file_nfc.delete();

        //////////////////////////////////////////////////////////////
        boolean found_dir = false;
        boolean found_file_nfc = false;
        boolean found_file_nfd = false;

        for (File f : base.listFiles()) {
            fname = f.toString();
            System.out.printf("Found   : [%s], length=%d%n", fname, fname.length());
            found_dir      |= match(dir, f);
            found_file_nfc |= match(file_nfc, f);
            found_file_nfd |= match(file_nfd, f);
        }

        if (!found_dir || !found_file_nfc || !found_file_nfc) {
            throw new RuntimeException("File.equal() failed");
        }
        removeAll(base);
    }
}
