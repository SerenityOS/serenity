/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8055463 8153272
 * @summary Test createFont APIs
 * @run main CreateFontArrayTest
 */

import java.awt.Font;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/*
 * This test pokes around in platform folders/directories to try
 * to find some fonts with which to test. It will do different things
 * on different platforms and may not do anything at all if the platform
 * directories aren't where it expects. For example if /usr/share/fonts
 * is not used on a particular Linux distro or on Windows the fonts are
 * not in c:\windows\fonts (which would be the right place on 99.99% of
 * systems you will find today.
 * It ought to be very reliable but it is not 100% guaranteed.
 * Failure to find fonts to test is 'not a product bug'.
 * Fonts on a system having different content than we expect based on
 * file extension is also 'not a product bug'.
 * The former will cause silent success, the latter may cause 'noisy' failure
 * and the test would then need to be dialled back to be more cautious.
 */

public class CreateFontArrayTest {

    public static void main(String[] args) throws Exception {
        test(".ttc", 2, -1, true);
        test(".ttf", 1,  1, true);
        test(".otf", 1,  1, true);
        test(".pfa", 0,  0, false);
        test(".pfb", 0,  0, false);
    }

    static File getPlatformFontFolder(String ext) throws Exception {
        boolean recurse = false;
        String folder = null;
        String os = System.getProperty("os.name");
        if (os.startsWith("Win")) {
            folder = "c:\\windows\\fonts";
        } else if (os.startsWith("Linux")) {
            folder = "/usr/share/fonts";
            recurse = true; // need to dig to find fonts.
        } else if (os.startsWith("Mac")) {
            // Disabled until createFont can handle mac font names.
            //folder = "/Library/Fonts";
        }
        if (folder == null) {
            return null;
        }
        File dir = new File(folder);
        if (!dir.exists() || !dir.isDirectory()) {
            return null;
        }
        // Have a root.
        if (!recurse) {
            return dir;
        }

        // If "recurse" is set, try to find a sub-folder which contains
        // fonts with the specified extension
        return findSubFolder(dir, ext);
    }

    static File findSubFolder(File folder, String ext) {
        File[] files =
            folder.listFiles(f -> f.getName().toLowerCase().endsWith(ext));
        if (files != null && files.length > 0) {
            return folder;
        }
        File[] subdirs = folder.listFiles(f -> f.isDirectory());
        for (File f : subdirs) {
            File subfolder = findSubFolder(f, ext);
            if (subfolder != null) {
                return subfolder;
            }
        }
        return null;
    }

    static void test(String ext, int min, int max,
                     boolean expectSuccess ) throws Exception {

        File dir = getPlatformFontFolder(ext);
        if (dir == null) {
            System.out.println("No folder to test for " + ext);
            return;
        }
        File[] files =
            dir.listFiles(f -> f.getName().toLowerCase().endsWith(ext));
        if (files == null || files.length == 0) {
            System.out.println("No files to test for " + ext);
            return;
        }
        System.out.println("Create from file " + files[0]);
        Font[] fonts = null;
        try {
            fonts = Font.createFonts(files[0]);
            System.out.println("createFont from file returned " + fonts);
        } catch (Exception e) {
            if (expectSuccess) {
                throw new RuntimeException("Unexpected exception", e);
            } else {
                System.out.println("Got expected exception " + e);
                return;
            }
        }
        for (Font f : fonts) {
            System.out.println(ext + " component : " + f);
        }
        if (fonts.length < min) {
            throw new RuntimeException("Expected at least " + min +
                                       " but got " + fonts.length);
        }
        if (max > 0 && fonts.length > max) {
            throw new RuntimeException("Expected no more than " + max +
                                       " but got " + fonts.length);
        }
        FileInputStream fis = null;
        try {
            System.out.println("Create from stream " + files[0]);
            fis = new FileInputStream(files[0]);
            InputStream s = new BufferedInputStream(fis);
            fonts = null;
            try {
                fonts = Font.createFonts(s);
                System.out.println("createFont from stream returned " + fonts);
            } catch (Exception e) {
                if (expectSuccess) {
                    throw new RuntimeException("Unexpected exception", e);
                } else {
                    System.out.println("Got expected exception " + e);
                    return;
                }
            }
            for (Font f : fonts) {
                System.out.println(ext + " component : " + f);
            }
            if (fonts.length < min) {
                throw new RuntimeException("Expected at least " + min +
                                           " but got " + fonts.length);
            }
            if (max > 0 && fonts.length > max) {
                throw new RuntimeException("Expected no more than " + max +
                                           " but got " + fonts.length);
            }
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }
}
