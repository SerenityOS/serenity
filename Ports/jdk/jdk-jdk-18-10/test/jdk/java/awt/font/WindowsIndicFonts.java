/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8208179
 * @summary Verifies logical fonts support Indic + other Asian code points
 * @requires (os.family == "windows")
 */

/*
 * This isn't just testing Indic fonts, a few other Asian scripts are
 * also being verified.
 * Oracle JDK for Windows had supported Devanagari and Thai in the logical
 * fonts using a proprietary font, since Windows did not have such fonts.
 * Since that was first added Microsoft added into Windows 7 a
 * number of fonts to support Indic + other Asian scripts.
 * By referencing these in the fontconfig.properties files we can enure that
 * these scripts are supported by the logical fonts when using OpenJDK for
 * Windows.
 * The test here just verifies that at least one required code point from each
 * of these scripts is available to make sure we don't regress, or to catch
 * and understand cases where those fonts may not be installed.
 */
import java.awt.Font;
import java.io.File;

public class WindowsIndicFonts {

  static boolean failed = false;
  static Font dialog = new Font(Font.DIALOG, Font.PLAIN, 12);
  static String windowsFontDir = "c:\\windows\\fonts";

  public static void main(String args[]) {

     if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
         return;
     }

     String sysRootDir = System.getenv("SYSTEMROOT");
     System.out.println("SysRootDir=" + sysRootDir);
     if (sysRootDir != null) {
        windowsFontDir = sysRootDir + "\\fonts";
     }
     test("\u0905", "Devanagari", "mangal.ttf"); // from Mangal font
     test("\u0985", "Bengali", "vrinda.ttf");    // from Vrinda font
     test("\u0a05", "Gurmukhi", "raavi.ttf");   // from Raavi font
     test("\u0a85", "Gujurati", "shruti.ttf");   // from Shruti font
     test("\u0b05", "Oriya", "kalinga.ttf");      // from Kalinga font
     test("\u0b85", "Tamil", "latha.ttf");      // from Latha font
     test("\u0c05", "Telugu", "gautami.ttf");     // from Gautami font
     test("\u0c85", "Kannada", "tunga.ttf");    // from Tunga font
     test("\u0d05", "Malayalam", "kartika.ttf");  // from Kartika font
     test("\u0c05", "Sinhala", "iskpota.ttf");    // from Iskoola Pota font
     test("\u0e05", "Thai", "dokchamp.ttf");       // from DokChampa font
     test("\u0e87", "Lao", "dokchamp.ttf");        // from DokChampa font
     test("\u0e05", "Khmer", "khmerui.ttf");      // from Khmer UI font
     test("\u1820", "Mongolian", "monbaiti.ttf");  // from Mongolian Baiti font

     if (failed) {
         throw new RuntimeException("Missing support for a script");
     }
  }

  static void test(String text, String script, String filename) {
     File f = new File(windowsFontDir, filename);
     if (!f.exists()) {
          System.out.println("Can't find required font file: " + filename);
          return;
     }
     System.out.println("found:" + f + " for " + script);
     if (dialog.canDisplayUpTo(text) != -1) {
         failed = true;
         System.out.println("No codepoint for " + script);
     }
  }

}
