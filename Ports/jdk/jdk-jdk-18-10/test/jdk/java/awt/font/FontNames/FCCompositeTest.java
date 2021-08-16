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
 * @bug 8219901
 * @modules java.desktop/sun.font
 * @requires (os.family == "linux")
 * @summary Verifies if the first fonts of CompositeFont and fc-match are same.
 */

import java.awt.Font;
import java.util.Locale;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import sun.font.FontUtilities;
import sun.font.Font2D;
import sun.font.CompositeFont;
import sun.font.PhysicalFont;

public class FCCompositeTest {

    final static String[] names =
        new String[]{"Monospaced","SansSerif","Serif"};
    final static String[] fcnames =
        new String[]{"monospace","sans","serif"};

    public static void main(String args[]) {
        for(int i = 0; i < names.length; i++) {
            test(i);
        }
    }

    private static void test(int index) {
        boolean matched = false;
        String fullName = "";
        String fcFullName = "";
        try {
            Font2D f2d = FontUtilities.getFont2D(
                             new Font(names[index], Font.PLAIN, 12));
            if (!(f2d instanceof CompositeFont)) {
                System.out.println("WARNING: Not CompositeFont");
                return;
            }
            PhysicalFont pf = ((CompositeFont)f2d).getSlotFont(0);
            fullName = pf.getFontName(Locale.ENGLISH);
            System.out.println("PF="+fullName);

            String[] command = {"fc-match",
                                fcnames[index],
                                "fullname"};
            Runtime runtime = Runtime.getRuntime();
            Process p = runtime.exec(command, null, null);
            p.waitFor();
            InputStream is = p.getInputStream();
            InputStream es = p.getErrorStream();
            BufferedReader br =
                new BufferedReader(new InputStreamReader(is));
            BufferedReader errorBr =
                new BufferedReader(new InputStreamReader(es));
            String line;
            while ((line = errorBr.readLine()) != null) {
                if (line.contains("warning") && line.contains("language")) {
                    System.out.println("Skip test by fc-match warning");
                    return;
                }
            }
            while (!matched) {
                String fcname = br.readLine();
                if (fcname == null) break;
                fcFullName = fcname;
                if (fcname.equals("")) {
                    System.out.println("Skip if no fullname");
                    return;
                }
                fcname = fcname.replaceAll("\\\\", "");
                String[] list = fcname.split("=|,", 0);
                for (int i = 1; i < list.length; i++) {
                    // skip header
                    if (fullName.equals(list[i])) {
                        matched = true;
                        break;
                    }
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Method invocation exception");
        }
        if (!matched) {
            throw new RuntimeException("FullName mismatch: "+fullName+"|"+
                                           fcFullName);
        }
    }
}
