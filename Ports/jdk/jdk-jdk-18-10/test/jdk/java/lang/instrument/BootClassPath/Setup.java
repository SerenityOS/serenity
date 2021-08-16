/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * Used by BootClassPath.sh.
 *
 * Given a "work directory" this class creates a sub-directory with a
 * name that uses locale specific characters. It then creates a jar
 * manifest file in the work directory with a Boot-Class-Path that
 * encodes the created sub-directory. Finally it creates a file
 * "boot.dir" in the work directory with the name of the sub-directory.
 */
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;

public class Setup {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java Setup <work-dir> <premain-class>");
            return;
        }
        String workDir = args[0];
        String premainClass = args[1];

        String manifestFile = workDir + fileSeparator + "MANIFEST.MF";
        String bootClassPath = "boot" + suffix();

        String bootDir = workDir + fileSeparator + bootClassPath;

        /*
         * Environment variable settings ("null" if unset)
         */
        System.out.println("Env vars:");
        System.out.println("  LANG=" + System.getenv("LANG"));
        System.out.println("  LC_ALL=" + System.getenv("LC_ALL"));
        System.out.println("  LC_CTYPE=" + System.getenv("LC_CTYPE"));

        /*
         * Create sub-directory
         */
        File f = new File(bootDir);
        f.mkdir();

        /*
         * Create manifest file with Boot-Class-Path encoding the
         * sub-directory name.
         */
        try (FileOutputStream out = new FileOutputStream(manifestFile)) {
            out.write("Manifest-Version: 1.0\n".getBytes("UTF-8"));

            byte[] premainBytes =
                ("Premain-Class: " + premainClass + "\n").getBytes("UTF-8");
            out.write(premainBytes);

            out.write( "Boot-Class-Path: ".getBytes("UTF-8") );

            byte[] value = bootClassPath.getBytes("UTF-8");
            for (int i=0; i<value.length; i++) {
                int v = (int)value[i];
                if (v < 0) v += 256;
                byte[] escaped =
                    ("%" + Integer.toHexString(v)).getBytes("UTF-8");
                out.write(escaped);
            }
            out.write( "\n\n".getBytes("UTF-8") );
        }

        /*
         * Write the name of the boot dir to "boot.dir"
         */
        f = new File(workDir + fileSeparator + "boot.dir");
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(bootDir.getBytes(defaultEncoding));
        }
    }

    /* ported from test/sun/tools/launcher/UnicodeTest.java */

    private static final String fileSeparator = System.getProperty("file.separator");
    private static final String osName = System.getProperty("os.name");
    private static final String defaultEncoding = Charset.defaultCharset().name();

    // language names taken from java.util.Locale.getDisplayLanguage for the respective language
    private static final String arabic = "\u0627\u0644\u0639\u0631\u0628\u064a\u0629";
    private static final String s_chinese = "\u4e2d\u6587";
    private static final String t_chinese = "\u4e2d\u6587";
    private static final String russian = "\u0440\u0443\u0441\u0441\u043A\u0438\u0439";
    private static final String hindi = "\u0939\u093f\u0902\u0926\u0940";
    private static final String greek = "\u03b5\u03bb\u03bb\u03b7\u03bd\u03b9\u03ba\u03ac";
    private static final String hebrew = "\u05e2\u05d1\u05e8\u05d9\u05ea";
    private static final String japanese = "\u65e5\u672c\u8a9e";
    private static final String korean = "\ud55c\uad6d\uc5b4";
    private static final String lithuanian = "Lietuvi\u0173";
    private static final String czech = "\u010de\u0161tina";
    private static final String turkish = "T\u00fcrk\u00e7e";
    private static final String spanish = "espa\u00f1ol";
    private static final String thai = "\u0e44\u0e17\u0e22";
    private static final String unicode = arabic + s_chinese + t_chinese
            + russian + hindi + greek + hebrew + japanese + korean
            + lithuanian + czech + turkish + spanish + thai;

    private static String suffix() {

        // Mapping from main platform encodings to language names
        // for Unix and Windows, respectively. Use empty suffix
        // for Windows encodings where OEM encoding differs.
        // Use null if encoding isn't used.
        String[][] names = {
            { "UTF-8",          unicode,        ""              },
            { "windows-1256",   null,           ""              },
            { "iso-8859-6",     arabic,         null            },
            { "GBK",            s_chinese,      s_chinese       },
            { "GB18030",        s_chinese,      s_chinese       },
            { "GB2312",         s_chinese,      null            },
            { "x-windows-950",  null,           t_chinese       },
            { "x-MS950-HKSCS",  null,           t_chinese       },
            { "x-euc-tw",       t_chinese,      null            },
            { "Big5",           t_chinese,      null            },
            { "Big5-HKSCS",     t_chinese,      null            },
            { "windows-1251",   null,           ""              },
            { "iso-8859-5",     russian,        null            },
            { "koi8-r",         russian,        null            },
            { "windows-1253",   null,           ""              },
            { "iso-8859-7",     greek,          null            },
            { "windows-1255",   null,           ""              },
            { "iso8859-8",      hebrew,         null            },
            { "windows-31j",    null,           japanese        },
            { "x-eucJP-Open",   japanese,       null            },
            { "x-EUC-JP-LINUX", japanese,       null            },
            { "x-pck",          japanese,       null            },
            { "x-windows-949",  null,           korean          },
            { "euc-kr",         korean,         null            },
            { "windows-1257",   null,           ""              },
            { "iso-8859-13",    lithuanian,     null            },
            { "windows-1250",   null,           ""              },
            { "iso-8859-2",     czech,          null            },
            { "windows-1254",   null,           ""              },
            { "iso-8859-9",     turkish,        null            },
            { "windows-1252",   null,           ""              },
            { "iso-8859-1",     spanish,        null            },
            { "iso-8859-15",    spanish,        null            },
            { "x-windows-874",  null,           thai            },
            { "tis-620",        thai,           null            },
        };

        int column;
        if (osName.startsWith("Windows")) {
            column = 2;
        } else {
            column = 1;
        }
        for (int i = 0; i < names.length; i++) {
             if (names[i][0].equalsIgnoreCase(defaultEncoding)) {
                 return names[i][column];
             }
         }
         return "";
    }
}
