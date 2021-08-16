/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4459099
 * @key i18n
 * @summary Tests non ANSI code page locales set default file encoding
 * to "utf-8".  This test must be run on Windows 2K/XP in one of Armenian,
 * Georgian, Hindi, Punjabi, Gujarati, Tamil, Telugu, Kannada, Marathi,
 * or Sanskrit languages.
 */

public class NonAnsiFileEncodingTest {
    public static void main(String[] s)  {
        String OS = System.getProperty("os.name");
        String lang = System.getProperty("user.language");
        String fileenc = System.getProperty("file.encoding");

        if (!(OS.equals("Windows 2000") || OS.equals("Windows XP"))) {
            System.out.println("This test is not meaningful on the platform \"" + OS + "\".");
            return;
        }

        if (!(lang.equals("hy") ||      // Armenian
              lang.equals("ka") ||      // Georgian
              lang.equals("hi") ||      // Hindi
              lang.equals("pa") ||      // Punjabi
              lang.equals("gu") ||      // Gujarati
              lang.equals("ta") ||      // Tamil
              lang.equals("te") ||      // Telugu
              lang.equals("kn") ||      // Kannada
              lang.equals("mr") ||      // Marathi
              lang.equals("sa"))) {     // Sanskrit
            System.out.println("Windows' locale settings for this test is incorrect.  Select one of \"Armenian\", \"Georgian\", \"Hindi\", \"Punjabi\", \"Gujarati\", \"Tamil\", \"Telugu\", \"Kannada\", \"Marathi\", or \"Sanskrit\" for the user locale, and \"English(United States)\" for the system default locale using the Control Panel.");
            return;
        }

        if (!fileenc.equals("utf-8")) {
            throw new RuntimeException("file.encoding is incorrectly set to \"" + fileenc + "\".  Should be \"utf-8\".");
        }
    }
}
