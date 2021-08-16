/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 6524757
 * @summary Tests different locales
 * @author Sergey Malenkov
 * @modules java.desktop/sun.swing
 */

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.AbstractButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.colorchooser.AbstractColorChooserPanel;

import sun.swing.SwingUtilities2;

public class Test6524757 {
    private static final String[] KEYS = {
            "ColorChooser.okText", // NON-NLS: string key from JColorChooser
            "ColorChooser.cancelText", // NON-NLS: string key from JColorChooser
            "ColorChooser.resetText", // NON-NLS: string key from JColorChooser
            "ColorChooser.resetMnemonic", // NON-NLS: int key from JColorChooser

//NotAvail: "ColorChooser.sampleText", // NON-NLS: string key from DefaultPreviewPanel

            "ColorChooser.swatchesNameText", // NON-NLS: string key from DefaultSwatchChooserPanel
            "ColorChooser.swatchesMnemonic", // NON-NLS: string key from DefaultSwatchChooserPanel:int
            "ColorChooser.swatchesSwatchSize", // NON-NLS: dimension key from DefaultSwatchChooserPanel
            "ColorChooser.swatchesRecentText", // NON-NLS: string key from DefaultSwatchChooserPanel
            "ColorChooser.swatchesRecentSwatchSize", // NON-NLS: dimension key from DefaultSwatchChooserPanel
//NotAvail: "ColorChooser.swatchesDefaultRecentColor", // NON-NLS: color key from DefaultSwatchChooserPanel

            "ColorChooser.hsvNameText", // NON-NLS: string key from HSV ColorChooserPanel
            "ColorChooser.hsvMnemonic", // NON-NLS: int key from HSV ColorChooserPanel
            "ColorChooser.hsvHueText", // NON-NLS: string key from HSV ColorChooserPanel
            "ColorChooser.hsvSaturationText", // NON-NLS: string key from HSV ColorChooserPanel
            "ColorChooser.hsvValueText", // NON-NLS: string key from HSV ColorChooserPanel
            "ColorChooser.hsvTransparencyText", // NON-NLS: string key from HSV ColorChooserPanel

            "ColorChooser.hslNameText", // NON-NLS: string key from HSV ColorChooserPanel
            "ColorChooser.hslMnemonic", // NON-NLS: int key from HSV ColorChooserPanel
            "ColorChooser.hslHueText", // NON-NLS: string key from HSV ColorChooserPanel
            "ColorChooser.hslSaturationText", // NON-NLS: string key from HSV ColorChooserPanel
            "ColorChooser.hslLightnessText", // NON-NLS: string key from HSV ColorChooserPanel
            "ColorChooser.hslTransparencyText", // NON-NLS: string key from HSV ColorChooserPanel

            "ColorChooser.rgbNameText", // NON-NLS: string key from HSV ColorChooserPanel
            "ColorChooser.rgbMnemonic", // NON-NLS: int key from HSV ColorChooserPanel
            "ColorChooser.rgbRedText", // NON-NLS: string key from HSV ColorChooserPanel
            "ColorChooser.rgbGreenText", // NON-NLS: string key from HSV ColorChooserPanel
            "ColorChooser.rgbBlueText", // NON-NLS: string key from HSV ColorChooserPanel
            "ColorChooser.rgbAlphaText", // NON-NLS: string key from HSV ColorChooserPanel
            "ColorChooser.rgbHexCodeText", // NON-NLS: string key from HSV ColorChooserPanel
            "ColorChooser.rgbHexCodeMnemonic", // NON-NLS: int key from HSV ColorChooserPanel

            "ColorChooser.cmykNameText", // NON-NLS: string key from HSV ColorChooserPanel
            "ColorChooser.cmykMnemonic", // NON-NLS: int key from HSV ColorChooserPanel
            "ColorChooser.cmykCyanText", // NON-NLS: string key from HSV ColorChooserPanel
            "ColorChooser.cmykMagentaText", // NON-NLS: string key from HSV ColorChooserPanel
            "ColorChooser.cmykYellowText", // NON-NLS: string key from HSV ColorChooserPanel
            "ColorChooser.cmykBlackText", // NON-NLS: string key from HSV ColorChooserPanel
            "ColorChooser.cmykAlphaText", // NON-NLS: string key from HSV ColorChooserPanel
    };
    private static final Object[] KOREAN = convert(Locale.KOREAN, KEYS);
    private static final Object[] FRENCH = convert(Locale.FRENCH, KEYS);

    public static void main(String[] args) {
        Locale reservedLocale = Locale.getDefault();
        try {
            // it affects Swing because it is not initialized
            Locale.setDefault(Locale.KOREAN);
            validate(KOREAN, create());

            // it does not affect Swing because it is initialized
            Locale.setDefault(Locale.CANADA);
            validate(KOREAN, create());

            // it definitely should affect Swing
            JComponent.setDefaultLocale(Locale.FRENCH);
            validate(FRENCH, create());
        } finally {
            // restore the reserved locale
            Locale.setDefault(reservedLocale);
        }
    }

    private static void validate(Object[] expected, Object[] actual) {
        int count = expected.length;
        if (count != actual.length) {
            throw new Error("different size: " + count + " <> " + actual.length);
        }
        for (int i = 0; i < count; i++) {
            if (!expected[i].equals(actual[i])) {
                throw new Error("unexpected value for key: " + KEYS[i]);
            }
        }
    }

    private static Object[] convert(Locale locale, String[] keys) {
        int count = keys.length;
        Object[] array = new Object[count];
        for (int i = 0; i < count; i++) {
            array[i] = convert(locale, keys[i]);
        }
        return array;
    }

    private static Object convert(Locale locale, String key) {
        if (key.endsWith("Text")) { // NON-NLS: suffix for text message
            return UIManager.getString(key, locale);
        }
        if (key.endsWith("Size")) { // NON-NLS: suffix for dimension
            return UIManager.getDimension(key, locale);
        }
        if (key.endsWith("Color")) { // NON-NLS: suffix for color
            return UIManager.getColor(key, locale);
        }
        int value = SwingUtilities2.getUIDefaultsInt(key, locale, -1);
        return Integer.valueOf(value);
    }

    private static Object[] create() {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setVisible(true);

        // show color chooser
        JColorChooser chooser = new JColorChooser();
        JDialog dialog = JColorChooser.createDialog(frame, null, false, chooser, null, null);
        dialog.setVisible(true);

        // process all values
        List<Object> list = new ArrayList<Object>(KEYS.length);

        Component component = getC(getC(dialog.getLayeredPane(), 0), 1);
        AbstractButton ok = (AbstractButton) getC(component, 0);
        AbstractButton cancel = (AbstractButton) getC(component, 1);
        AbstractButton reset = (AbstractButton) getC(component, 2);
        list.add(ok.getText());
        list.add(cancel.getText());
        list.add(reset.getText());
        list.add(Integer.valueOf(reset.getMnemonic()));

        for (int i = 0; i < 5; i++) {
            AbstractColorChooserPanel panel = (AbstractColorChooserPanel) getC(getC(getC(chooser, 0), i), 0);
            list.add(panel.getDisplayName());
            list.add(Integer.valueOf(panel.getMnemonic()));
            if (i == 0) {
                JLabel label = (JLabel) getC(getC(panel, 0), 1);
                JPanel upper = (JPanel) getC(getC(getC(panel, 0), 0), 0);
                JPanel lower = (JPanel) getC(getC(getC(panel, 0), 2), 0);
                addSize(list, upper, 1, 1, 31, 9);
                list.add(label.getText());
                addSize(list, lower, 1, 1, 5, 7);
            }
            else {
                Component container = getC(panel, 0);
                for (int j = 0; j < 3; j++) {
                    AbstractButton button = (AbstractButton) getC(container, j);
                    list.add(button.getText());
                }
                JLabel label = (JLabel) getC(container, 3);
                list.add(label.getText());
                if (i == 4) {
                    label = (JLabel) getC(container, 4);
                    list.add(label.getText());
                }
                if (i == 3) {
                    label = (JLabel) getC(panel, 1);
                    list.add(label.getText());
                    list.add(Integer.valueOf(label.getDisplayedMnemonic()));
                }
            }
        }

        // close dialog
        dialog.setVisible(false);
        dialog.dispose();

        // close frame
        frame.setVisible(false);
        frame.dispose();

        return list.toArray();
    }

    private static void addSize(List<Object> list, Component component, int x, int y, int w, int h) {
        Dimension size = component.getPreferredSize();
        int width = (size.width + 1) / w - x;
        int height = (size.height + 1) / h - y;
        list.add(new Dimension(width, height));
    }

    private static Component getC(Component component, int index) {
        Container container = (Container) component;
        return container.getComponent(index);
    }
}
