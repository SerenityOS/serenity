/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4987336
   @summary JSlider doesn't show label's animated icon.
   @author Pavel Porvatov
   @run applet/manual=done bug4987336.html
*/

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

public class bug4987336 extends JApplet {
    private static final String IMAGE_RES = "box.gif";

    private static final String ANIM_IMAGE_RES = "duke.gif";

    public void init() {
        JPanel pnLafs = new JPanel();
        pnLafs.setLayout(new BoxLayout(pnLafs, BoxLayout.Y_AXIS));

        ButtonGroup group = new ButtonGroup();

        pnLafs.setBorder(new TitledBorder("Available Lafs"));

        for (UIManager.LookAndFeelInfo lafInfo : UIManager.getInstalledLookAndFeels()) {
            LafRadioButton comp = new LafRadioButton(lafInfo);

            pnLafs.add(comp);
            group.add(comp);
        }

        JPanel pnContent = new JPanel();

        pnContent.setLayout(new BoxLayout(pnContent, BoxLayout.Y_AXIS));

        pnContent.add(pnLafs);
        pnContent.add(createSlider(true, IMAGE_RES, IMAGE_RES, ANIM_IMAGE_RES, ANIM_IMAGE_RES));
        pnContent.add(createSlider(false, IMAGE_RES, IMAGE_RES, ANIM_IMAGE_RES, ANIM_IMAGE_RES));
        pnContent.add(createSlider(true, ANIM_IMAGE_RES, null, IMAGE_RES, IMAGE_RES));
        pnContent.add(createSlider(false, ANIM_IMAGE_RES, null, IMAGE_RES, IMAGE_RES));

        getContentPane().add(new JScrollPane(pnContent));
    }

    private static JSlider createSlider(boolean enabled,
                                        String firstEnabledImage, String firstDisabledImage,
                                        String secondEnabledImage, String secondDisabledImage) {
        Hashtable<Integer, JComponent> dictionary = new Hashtable<Integer, JComponent>();

        dictionary.put(0, createLabel(firstEnabledImage, firstDisabledImage));
        dictionary.put(1, createLabel(secondEnabledImage, secondDisabledImage));

        JSlider result = new JSlider(0, 1);

        result.setLabelTable(dictionary);
        result.setPaintLabels(true);
        result.setEnabled(enabled);

        return result;
    }

    private static JLabel createLabel(String enabledImage, String disabledImage) {
        ImageIcon enabledIcon = enabledImage == null ? null :
                new ImageIcon(bug4987336.class.getResource(enabledImage));

        ImageIcon disabledIcon = disabledImage == null ? null :
                new ImageIcon(bug4987336.class.getResource(disabledImage));

        JLabel result = new JLabel(enabledImage == null && disabledImage == null ? "No image" : "Image",
                enabledIcon, SwingConstants.LEFT);

        result.setDisabledIcon(disabledIcon);

        return result;
    }

    private class LafRadioButton extends JRadioButton {
        public LafRadioButton(final UIManager.LookAndFeelInfo lafInfo) {
            super(lafInfo.getName(), lafInfo.getName().equals(UIManager.getLookAndFeel().getName()));

            addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        UIManager.setLookAndFeel(lafInfo.getClassName());

                        SwingUtilities.updateComponentTreeUI(bug4987336.this);
                    } catch (Exception ex) {
                        // Ignore such errors
                        System.out.println("Cannot set LAF " + lafInfo.getName());
                    }
                }
            });
        }
    }
}
