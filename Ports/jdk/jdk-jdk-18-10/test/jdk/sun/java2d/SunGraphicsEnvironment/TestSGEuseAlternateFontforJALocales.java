/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 7032930
 *
 * @summary verify the existence of the  method
 *           SunGraphicsEnvironment.useAlternateFontforJALocales
 *
 * @modules java.desktop/sun.java2d
 * @run main/othervm TestSGEuseAlternateFontforJALocales
 * @run main/othervm -Dfile.encoding=windows-31j -Duser.language=ja -Duser.country=JA TestSGEuseAlternateFontforJALocales
 *
 */

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Locale;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;

public class TestSGEuseAlternateFontforJALocales {

    public static void main(String args[]) throws Exception {
        System.out.println("Default Charset = "
            + Charset.defaultCharset().name());
        System.out.println("Locale = " + Locale.getDefault());
        String os = System.getProperty("os.name");
        String encoding = System.getProperty("file.encoding");
        /* Want to test the JA locale uses alternate font for DialogInput. */
        boolean jaTest = encoding.equalsIgnoreCase("windows-31j");
        if (!os.startsWith("Win") && jaTest) {
            System.out.println("Skipping Windows only test");
            return;
        }

        String className = "sun.java2d.SunGraphicsEnvironment";
        String methodName = "useAlternateFontforJALocales";
        Class sge = Class.forName(className);
        Method uafMethod = sge.getMethod(methodName, (Class[])null);
        Object ret = uafMethod.invoke(null);
        GraphicsEnvironment ge =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        ge.preferLocaleFonts();
        ge.preferProportionalFonts();
        if (jaTest) {
            Font msMincho = new Font("MS Mincho", Font.PLAIN, 12);
            if (!msMincho.getFamily(Locale.ENGLISH).equals("MS Mincho")) {
                 System.out.println("MS Mincho not installed. Skipping test");
                 return;
            }
            Font dialogInput = new Font("DialogInput", Font.PLAIN, 12);
            Font courierNew = new Font("Courier New", Font.PLAIN, 12);
            Font msGothic = new Font("MS Gothic", Font.PLAIN, 12);
            BufferedImage bi = new BufferedImage(1,1,1);
            Graphics2D g2d = bi.createGraphics();
            FontMetrics cnMetrics = g2d.getFontMetrics(courierNew);
            FontMetrics diMetrics = g2d.getFontMetrics(dialogInput);
            FontMetrics mmMetrics = g2d.getFontMetrics(msMincho);
            FontMetrics mgMetrics = g2d.getFontMetrics(msGothic);
            // This tests to make sure we at least have applied
            //  "preferLocaleFonts for Japanese
            if (cnMetrics.charWidth('A') == diMetrics.charWidth('A')) {
                 throw new RuntimeException
                       ("Courier New should not be used for DialogInput");
            }
            // This is supposed to make sure we are using MS Mincho instead
            //  of MS Gothic. However they are metrics identical so its
            // not definite proof.
            if (diMetrics.charWidth('A') != mmMetrics.charWidth('A')) {
                 throw new RuntimeException
                     ("MS Mincho should be used for DialogInput");
            }
       }
   }
}
