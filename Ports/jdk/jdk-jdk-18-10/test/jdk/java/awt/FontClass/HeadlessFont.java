/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.text.AttributedCharacterIterator;
import java.text.StringCharacterIterator;
import java.util.HashMap;
import java.util.Map;

/*
 * @test
 * @summary Check that Font constructors and methods do not throw
 *          unexpected exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessFont
 */

public class HeadlessFont {

    public static void main(String args[]) {
        HashMap attMap = new HashMap();
        attMap.put(TextAttribute.FAMILY, "Helvetica Bold");
        attMap.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_LIGHT);
        attMap.put(TextAttribute.WIDTH, TextAttribute.WIDTH_REGULAR);
        attMap.put(TextAttribute.SIZE, new Float(20));
        attMap.put(TextAttribute.FOREGROUND, Color.white);
        attMap.put(TextAttribute.BACKGROUND, Color.black);

        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int i = 8; i < 17; i++) {
                Font f1 = new Font(font, Font.PLAIN, i);
                Font f2 = new Font(font, Font.BOLD, i);
                Font f3 = new Font(font, Font.ITALIC, i);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, i);

                FontMetrics metrics = Toolkit.getDefaultToolkit().getFontMetrics(f1);
                metrics = Toolkit.getDefaultToolkit().getFontMetrics(f2);
                metrics = Toolkit.getDefaultToolkit().getFontMetrics(f3);
                metrics = Toolkit.getDefaultToolkit().getFontMetrics(f4);

                AffineTransform trans = f1.getTransform();
                trans = f2.getTransform();
                trans = f3.getTransform();
                trans = f4.getTransform();

                String str;
                str = f1.getFamily();
                str = f2.getFamily();
                str = f3.getFamily();
                str = f4.getFamily();

                str = f1.getPSName();
                str = f2.getPSName();
                str = f3.getPSName();
                str = f4.getPSName();

                str = f1.getName();
                str = f2.getName();
                str = f3.getName();
                str = f4.getName();

                str = f1.getFontName();
                str = f2.getFontName();
                str = f3.getFontName();
                str = f4.getFontName();

                str = f1.toString();
                str = f2.toString();
                str = f3.toString();
                str = f4.toString();

                int s;
                s = f1.getStyle();
                s = f2.getStyle();
                s = f3.getStyle();
                s = f4.getStyle();

                s = f1.getSize();
                s = f2.getSize();
                s = f3.getSize();
                s = f4.getSize();

                s = f1.hashCode();
                s = f2.hashCode();
                s = f3.hashCode();
                s = f4.hashCode();

                s = f1.getNumGlyphs();
                s = f2.getNumGlyphs();
                s = f3.getNumGlyphs();
                s = f4.getNumGlyphs();

                s = f1.getMissingGlyphCode();
                s = f2.getMissingGlyphCode();
                s = f3.getMissingGlyphCode();
                s = f4.getMissingGlyphCode();

                float f;
                f = f1.getSize2D();
                f = f2.getSize2D();
                f = f3.getSize2D();
                f = f4.getSize2D();


                byte b;
                b = f1.getBaselineFor('c');
                b = f2.getBaselineFor('c');
                b = f3.getBaselineFor('c');
                b = f4.getBaselineFor('c');

                Map m = f1.getAttributes();
                m = f2.getAttributes();
                m = f3.getAttributes();
                m = f4.getAttributes();

                AttributedCharacterIterator.Attribute[] a;
                a = f1.getAvailableAttributes();
                a = f2.getAvailableAttributes();
                a = f3.getAvailableAttributes();
                a = f4.getAvailableAttributes();


                Font fnt;
                fnt = f1.deriveFont(Font.BOLD | Font.ITALIC, (float) 80);
                fnt = f2.deriveFont(Font.BOLD | Font.ITALIC, (float) 80);
                fnt = f3.deriveFont(Font.BOLD | Font.ITALIC, (float) 80);
                fnt = f4.deriveFont(Font.BOLD | Font.ITALIC, (float) 80);

                fnt = f1.deriveFont(80f);
                fnt = f2.deriveFont(80f);
                fnt = f3.deriveFont(80f);
                fnt = f4.deriveFont(80f);

                fnt = f1.deriveFont(Font.BOLD | Font.ITALIC);
                fnt = f2.deriveFont(Font.BOLD | Font.ITALIC);
                fnt = f3.deriveFont(Font.BOLD | Font.ITALIC);
                fnt = f4.deriveFont(Font.BOLD | Font.ITALIC);

                fnt = f1.deriveFont(attMap);
                fnt = f2.deriveFont(attMap);
                fnt = f3.deriveFont(attMap);
                fnt = f4.deriveFont(attMap);


                if (!f1.isPlain())
                    throw new RuntimeException("Plain font " + f1.getName() + " says it's not plain");
                if (f2.isPlain())
                    throw new RuntimeException("Bold font " + f1.getName() + " says it is plain");
                if (f3.isPlain())
                    throw new RuntimeException("Italic font " + f1.getName() + " says it is plain");
                if (f4.isPlain())
                    throw new RuntimeException("Bold|Italic font " + f1.getName() + " says it is plain");

                if (f1.isBold())
                    throw new RuntimeException("Plain font " + f1.getName() + " says it is bold");
                if (!f2.isBold())
                    throw new RuntimeException("Bold font " + f1.getName() + " says it's not bold");
                if (f3.isBold())
                    throw new RuntimeException("Italic font " + f1.getName() + " says it is bold");
                if (!f4.isBold())
                    throw new RuntimeException("Bold|Italic font " + f1.getName() + " says it's not bold");

                if (f1.isItalic())
                    throw new RuntimeException("Plain font " + f1.getName() + " says it is italic");
                if (f2.isItalic())
                    throw new RuntimeException("Bold font " + f1.getName() + " says it is italic");
                if (!f3.isItalic())
                    throw new RuntimeException("Italic font " + f1.getName() + " says it's not italic");
                if (!f4.isItalic())
                    throw new RuntimeException("Bold|Italic font " + f1.getName() + " says it's not italic");

                f1.canDisplay('~');
                f2.canDisplay('~');
                f3.canDisplay('~');
                f4.canDisplay('~');
                f1.canDisplay('c');
                f2.canDisplay('c');
                f3.canDisplay('c');
                f4.canDisplay('c');

                f1.canDisplayUpTo("canDisplayUpTo");
                f2.canDisplayUpTo("canDisplayUpTo");
                f3.canDisplayUpTo("canDisplayUpTo");
                f4.canDisplayUpTo("canDisplayUpTo");

                str = "canDisplayUpTo";
                f1.canDisplayUpTo(str.toCharArray(), 0, str.length());
                f2.canDisplayUpTo(str.toCharArray(), 0, str.length());
                f3.canDisplayUpTo(str.toCharArray(), 0, str.length());
                f4.canDisplayUpTo(str.toCharArray(), 0, str.length());

                f1.canDisplayUpTo(new StringCharacterIterator(str), 0, str.length());
                f2.canDisplayUpTo(new StringCharacterIterator(str), 0, str.length());
                f3.canDisplayUpTo(new StringCharacterIterator(str), 0, str.length());
                f4.canDisplayUpTo(new StringCharacterIterator(str), 0, str.length());

                f1.getItalicAngle();
                f2.getItalicAngle();
                f3.getItalicAngle();
                f4.getItalicAngle();

                f1.hasUniformLineMetrics();
                f2.hasUniformLineMetrics();
                f3.hasUniformLineMetrics();
                f4.hasUniformLineMetrics();
            }
        }

        Font f = new Font(attMap);
        f = Font.getFont(attMap);
        f = Font.decode(null);
    }
}
