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
import java.io.File;
import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.NumericShaper;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.metal.MetalLookAndFeel;

/**
 * @test
 * @bug 8132119 8168992 8169897 8207941
 * @author Alexandr Scherbatiy
 * @summary Provide public API for text related methods in SwingBasicGraphicsUtils2
 */
public class bug8132119 {

    private static final int WIDTH = 50;
    private static final int HEIGHT = 50;
    private static final Color DRAW_COLOR = Color.RED;
    private static final Color BACKGROUND_COLOR = Color.GREEN;
    private static final NumericShaper NUMERIC_SHAPER = NumericShaper.getShaper(
            NumericShaper.ARABIC);

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(bug8132119::testStringMethods);
    }

    private static void testStringMethods() {
        setMetalLAF();
        testStringWidth();
        testStringClip();
        testDrawEmptyString();
        testDrawString(false);
        testDrawString(true);
        checkNullArguments();
    }

    private static void testStringWidth() {

        String str = "12345678910\u036F";
        JComponent comp = createComponent(str);
        Font font = comp.getFont();
        FontMetrics fontMetrics = comp.getFontMetrics(font);
        float stringWidth = BasicGraphicsUtils.getStringWidth(comp, fontMetrics, str);

        if (stringWidth == fontMetrics.stringWidth(str)) {
            throw new RuntimeException("Numeric shaper is not used!");
        }

        if (stringWidth != getLayoutWidth(str, font, NUMERIC_SHAPER)) {
            throw new RuntimeException("Wrong text width!");
        }
    }

    private static void testStringClip() {

        String str = "1234567890";
        JComponent comp = createComponent(str);
        FontMetrics fontMetrics = comp.getFontMetrics(comp.getFont());

        int width = (int) BasicGraphicsUtils.getStringWidth(comp, fontMetrics, str);

        String clip = BasicGraphicsUtils.getClippedString(comp, fontMetrics, str, width);
        checkClippedString(str, clip, str);

        clip = BasicGraphicsUtils.getClippedString(comp, fontMetrics, str, width + 1);
        checkClippedString(str, clip, str);

        clip = BasicGraphicsUtils.getClippedString(comp, fontMetrics, str, -1);
        checkClippedString(str, clip, "...");

        clip = BasicGraphicsUtils.getClippedString(comp, fontMetrics, str, 0);
        checkClippedString(str, clip, "...");

        clip = BasicGraphicsUtils.getClippedString(comp, fontMetrics,
                str, width - width / str.length());
        int endIndex = str.length() - 3;
        checkClippedString(str, clip, str.substring(0, endIndex) + "...");
    }

    private static void checkClippedString(String str, String res, String golden) {
        if (!golden.equals(res)) {
            throw new RuntimeException(String.format("The string '%s' is not "
                    + "properly clipped. The result is '%s' instead of '%s'",
                    str, res, golden));
        }
    }

    private static void testDrawEmptyString() {
        JLabel label = new JLabel();
        BufferedImage buffImage = createBufferedImage(50, 50);
        Graphics2D g2 = buffImage.createGraphics();
        g2.setColor(DRAW_COLOR);
        BasicGraphicsUtils.drawString(null, g2, null, 0, 0);
        BasicGraphicsUtils.drawString(label, g2, null, 0, 0);
        BasicGraphicsUtils.drawString(null, g2, "", 0, 0);
        BasicGraphicsUtils.drawString(label, g2, "", 0, 0);
        BasicGraphicsUtils.drawStringUnderlineCharAt(null, g2, null, 3, 0, 0);
        BasicGraphicsUtils.drawStringUnderlineCharAt(label, g2, null, 3, 0, 0);
        BasicGraphicsUtils.drawStringUnderlineCharAt(null, g2, "", 3, 0, 0);
        BasicGraphicsUtils.drawStringUnderlineCharAt(label, g2, "", 3, 0, 0);
        g2.dispose();
        checkImageIsEmpty(buffImage);
    }

    private static void testDrawString(boolean underlined) {
        String str = "AOB";
        JComponent comp = createComponent(str);

        BufferedImage buffImage = createBufferedImage(WIDTH, HEIGHT);
        Graphics2D g2 = buffImage.createGraphics();

        g2.setColor(DRAW_COLOR);
        g2.setFont(comp.getFont());
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        FontMetrics fontMetrices = comp.getFontMetrics(comp.getFont());
        float width = BasicGraphicsUtils.getStringWidth(comp, fontMetrices, str);
        int y = 3 * HEIGHT / 4;

        if (underlined) {
            BasicGraphicsUtils.drawStringUnderlineCharAt(comp, g2, str, 1, 0, y);
        } else {
            BasicGraphicsUtils.drawString(comp, g2, str, 0, y);
        }
        g2.dispose();

        float xx = 0;
        if (underlined) {
            xx = BasicGraphicsUtils.getStringWidth(comp, fontMetrices, "A") +
                BasicGraphicsUtils.getStringWidth(comp, fontMetrices, "O")/2  - 5;
        } else {
            xx = BasicGraphicsUtils.getStringWidth(comp, fontMetrices, "A") +
                BasicGraphicsUtils.getStringWidth(comp, fontMetrices, "O")/2;
        }

        checkImageContainsSymbol(buffImage, (int) xx, underlined ? 3 : 2);
    }

    private static void checkNullArguments() {

        Graphics2D g = null;
        try {
            String text = "Test";
            JComponent component = new JLabel(text);
            BufferedImage img = createBufferedImage(100, 100);
            g = img.createGraphics();
            checkNullArguments(component, g, text);
        } finally {
            g.dispose();
        }
    }

    private static void checkNullArguments(JComponent comp, Graphics2D g,
            String text) {

        checkNullArgumentsDrawString(comp, g, text);
        checkNullArgumentsDrawStringUnderlineCharAt(comp, g, text);
        checkNullArgumentsGetClippedString(comp, text);
        checkNullArgumentsGetStringWidth(comp, text);
    }

    private static void checkNullArgumentsDrawString(JComponent comp, Graphics2D g,
            String text) {

        float x = 50;
        float y = 50;
        BasicGraphicsUtils.drawString(null, g, text, x, y);
        BasicGraphicsUtils.drawString(comp, g, null, x, y);

        try {
            BasicGraphicsUtils.drawString(comp, null, text, x, y);
        } catch (NullPointerException e) {
            return;
        }

        throw new RuntimeException("NPE is not thrown");
    }

    private static void checkNullArgumentsDrawStringUnderlineCharAt(
            JComponent comp, Graphics2D g, String text) {

        int x = 50;
        int y = 50;
        BasicGraphicsUtils.drawStringUnderlineCharAt(null, g, text, 1, x, y);
        BasicGraphicsUtils.drawStringUnderlineCharAt(comp, g, null, 1, x, y);

        try {
            BasicGraphicsUtils.drawStringUnderlineCharAt(comp, null, text, 1, x, y);
        } catch (NullPointerException e) {
            return;
        }

        throw new RuntimeException("NPE is not thrown");
    }

    private static void checkNullArgumentsGetClippedString(
            JComponent comp, String text) {

        FontMetrics fontMetrics = comp.getFontMetrics(comp.getFont());

        BasicGraphicsUtils.getClippedString(null, fontMetrics, text, 1);
        String result = BasicGraphicsUtils.getClippedString(comp, fontMetrics, null, 1);
        if (!"".equals(result)) {
            throw new RuntimeException("Empty string is not returned!");
        }

        try {
            BasicGraphicsUtils.getClippedString(comp, null, text, 1);
        } catch (NullPointerException e) {
            return;
        }

        throw new RuntimeException("NPE is not thrown");
    }

    private static void checkNullArgumentsGetStringWidth(JComponent comp,
            String text) {

        FontMetrics fontMetrics = comp.getFontMetrics(comp.getFont());
        BasicGraphicsUtils.getStringWidth(null, fontMetrics, text);
        float result = BasicGraphicsUtils.getStringWidth(comp, fontMetrics, null);

        if (result != 0) {
            throw new RuntimeException("The string length is not 0");
        }

        try {
            BasicGraphicsUtils.getStringWidth(comp, null, text);
        } catch (NullPointerException e) {
            return;
        }

        throw new RuntimeException("NPE is not thrown");
    }

    private static void setMetalLAF() {
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static JComponent createComponent(String str) {
        JComponent comp = new JLabel(str);
        comp.setSize(WIDTH, HEIGHT);
        comp.putClientProperty(TextAttribute.NUMERIC_SHAPING, NUMERIC_SHAPER);
        comp.setFont(getFont());
        return comp;
    }

    private static String getFontName(String fn, String[] fontNames) {
        String fontName = null;
        for (String name : fontNames) {
            if (fn.equals(name)) {
                fontName = name;
                break;
            }
        }
        return fontName;
    }

    private static Font getFont() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontNames = ge.getAvailableFontFamilyNames();

        // We do not have Arial on all systems so provide some reasonable fallbacks.
        // In case the fallbacks are not available as well, choose as last fallback
        // the first font - however this might be a problematic choice.
        String fontName = getFontName("Arial", fontNames);
        if (fontName == null) {
            fontName = getFontName("Bitstream Charter", fontNames);
            if (fontName == null) {
                fontName = getFontName("Dialog", fontNames);
                if (fontName == null) {
                    fontName = fontNames[0];
                    System.out.println("warning - preferred fonts not on the system, fall back to first font " + fontName);
                }
            }
        }
        return new Font(fontName, Font.PLAIN, 30);
    }

    private static float getLayoutWidth(String text, Font font, NumericShaper shaper) {
        HashMap map = new HashMap();
        map.put(TextAttribute.FONT, font);
        map.put(TextAttribute.NUMERIC_SHAPING, shaper);
        FontRenderContext frc = new FontRenderContext(null, false, false);
        TextLayout layout = new TextLayout(text, map, frc);
        return layout.getAdvance();
    }

    private static void checkImageIsEmpty(BufferedImage buffImage) {
        int background = BACKGROUND_COLOR.getRGB();

        for (int i = 0; i < buffImage.getWidth(); i++) {
            for (int j = 0; j < buffImage.getHeight(); j++) {
                if (background != buffImage.getRGB(i, j)) {
                    throw new RuntimeException("Image is not empty!");
                }
            }
        }
    }

    private static void checkImageContainsSymbol(BufferedImage buffImage,
            int x, int intersections) {

        int background = BACKGROUND_COLOR.getRGB();
        boolean isBackground = true;
        int backgroundChangesCount = 0;

        for (int y = 0; y < buffImage.getHeight(); y++) {
            if (!(isBackground ^ (background != buffImage.getRGB(x, y)))) {
                isBackground = !isBackground;
                backgroundChangesCount++;
            }
        }


        if (backgroundChangesCount != intersections * 2) {
            try {
                ImageIO.write(buffImage, "png", new File("image.png"));
            } catch (Exception e) {}
            throw new RuntimeException("String is not properly drawn!");
        }
    }

    private static BufferedImage createBufferedImage(int width, int height) {
        BufferedImage bufffImage = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);

        Graphics2D g = bufffImage.createGraphics();
        g.setColor(BACKGROUND_COLOR);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return bufffImage;
    }
}
