/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 8016833
   @summary underlines and strikethroughs should be  painted at the correct
            positions for different kind of text styles: normal, superscript and subscript
   @author Anton Nashatyrev
   @run main bug8016833
*/
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;

public class bug8016833 {

    void drawText(final Graphics g, final boolean underline, final boolean strikethrough, final boolean background) {
        drawText(g, "mama", underline, strikethrough, background);
    }

    void drawText(final Graphics g, final String text, final boolean underline, final boolean strikethrough, final boolean background) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    final JTextPane comp = new JTextPane();
                    final StyledDocument doc = comp.getStyledDocument();

                    Style style = comp.addStyle("superscript", null);
                    setNormalStyle(style);

                    if (underline) {
                        StyleConstants.setUnderline(style, true);
                    }
                    if (strikethrough) {
                        StyleConstants.setStrikeThrough(style, true);
                    }
                    if (background) {
                        StyleConstants.setBackground(style, Color.BLUE);
                    }
                    try {
                        doc.insertString(doc.getLength(), "mama", style);
                    } catch (BadLocationException e) {
                        throw new RuntimeException(e);
                    }

                    comp.setSize(200, 100);
                    comp.paint(g);
                }
            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    void setNormalStyle(Style style) {
        StyleConstants.setSuperscript(style, true);
    }

    int getEmptyPixel() {
        return 0xFFFFFFFF;
    }

    boolean isPixelEmpty(int argb) {
        return (argb & 0x00FFFFFF) == (getEmptyPixel() & 0x00FFFFFF);
    }

    boolean isLineEmpty(BufferedImage img, int coord, boolean isHorizontal) {
        int len = isHorizontal ? img.getWidth() : img.getHeight();
        for (int i = 0; i < len; i++) {
            int pixel = isHorizontal ? img.getRGB(i, coord) : img.getRGB(coord, i);
            if (!isPixelEmpty(pixel)) {
                return false;
            }
        }
        return true;
    }

    Rectangle getPixelsOutline(BufferedImage img) {
        int x1 = 0;
        while (x1 < img.getWidth() && isLineEmpty(img, x1, false)) {
            x1++;
        }
        int x2 = img.getWidth() - 1;
        while (x2 >= 0 && isLineEmpty(img, x2, false)) {
            x2--;
        }
        int y1 = 0;
        while (y1 < img.getHeight() && isLineEmpty(img, y1, true)) {
            y1++;
        }
        int y2 = img.getHeight() - 1;
        while (y2 >= 0 && isLineEmpty(img, y2, true)) {
            y2--;
        }

        return new Rectangle(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
    }

    BufferedImage createImage() {
        final BufferedImage img = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    Graphics g = img.getGraphics();
                    g.setColor(new Color(getEmptyPixel()));
                    g.fillRect(0, 0, 10000, 10000);
                }
            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return img;
    }

    int subPixels(int pix1, int pix2) {
        if (pix1 == pix2) {
            return getEmptyPixel();
        }
        return pix1;
    }

    /**
     * Subtracts img2 from img1
     */
    BufferedImage subImages(BufferedImage img1, BufferedImage img2) {
        if (img1.getHeight() != img2.getHeight() ||
                img1.getWidth() != img2.getWidth()) {
            throw new RuntimeException("Different sizes");
        }
        BufferedImage ret = new BufferedImage(img1.getWidth(), img1.getHeight(), img1.getType());

        for (int x = 0; x < ret.getWidth(); x++) {
            for (int y = 0; y < ret.getHeight(); y++) {
                ret.setRGB(x, y, subPixels(img1.getRGB(x, y), img2.getRGB(x, y)));
            }
        }
        return ret;
    }

    void testUnderline() {
        System.out.println("  testUnderline()");

        final BufferedImage img1 = createImage();
        drawText(img1.getGraphics(), true, false, false);
        final Rectangle out1 = getPixelsOutline(img1);
        System.out.println("   Underlined: " + out1);

        final BufferedImage img2 = createImage();
        drawText(img2.getGraphics(), false, false, false);
        final Rectangle out2 = getPixelsOutline(img2);
        System.out.println("   Normal: " + out2);

        final BufferedImage img3 = subImages(img1, img2);
        final Rectangle out3 = getPixelsOutline(img3);
        System.out.println("   Sub: " + out3);

        // underline is not too thick
        assertTrue(out3.getHeight() <= 2);
        // not too wide
        assertTrue(out3.getWidth() * 0.8 < out2.getWidth());
        // not too low
        assertTrue(out3.getY() - (out1.getY() + out2.getHeight() - 1) < 4);
        // not too high
        assertTrue(out3.getY() - (out1.getY() + out2.getHeight() - 1) > 0);
    }

    void testStrikthrough() {
        System.out.println("  testStrikthrough()");

        final BufferedImage img1 = createImage();
        drawText(img1.getGraphics(), false, true, false);
        final Rectangle out1 = getPixelsOutline(img1);
        System.out.println("   Striked: " + out1);

        final BufferedImage img2 = createImage();
        drawText(img2.getGraphics(), false, false, false);
        final Rectangle out2 = getPixelsOutline(img2);
        System.out.println("   Normal: " + out2);

        final BufferedImage img3 = subImages(img1, img2);
        final Rectangle out3 = getPixelsOutline(img3);
        System.out.println("   Sub: " + out3);

        // strikethrough is not too thick
        assertTrue(out3.getHeight() <= 2);
        // not too wide
        assertTrue(out3.getWidth() * 0.8 < out2.getWidth());
        // not too low
        assertTrue(out3.getY() - (out1.getY() + out2.getHeight() - 1) < 0);
        // not too high
        assertTrue(out3.getY() - out1.getY() > 1);
    }
    void assertTrue(boolean b) {
        if (!b) {
            throw new RuntimeException("Assertion failed");
        }
    }

    static void testSuperScript() {
        System.out.println("testSuperScript()");
        bug8016833 b = new bug8016833() {
            @Override
            void setNormalStyle(Style style) {
                StyleConstants.setSuperscript(style, true);
            }
        };
        b.testUnderline();
        b.testStrikthrough();
    }

    static void testSubScript() {
        System.out.println("testSubScript()");
        bug8016833 b = new bug8016833() {
            @Override
            void setNormalStyle(Style style) {
                StyleConstants.setSubscript(style, true);
            }
        };
        b.testUnderline();
        b.testStrikthrough();
    }

    static void testNormalScript() {
        System.out.println("testNormalScript()");
        bug8016833 b = new bug8016833() {
            @Override
            void setNormalStyle(Style style) {
            }
        };
        b.testUnderline();
        b.testStrikthrough();
    }

    public static void main(String[] args) {
        testSubScript();
        testSuperScript();
        testNormalScript();
    }
}
