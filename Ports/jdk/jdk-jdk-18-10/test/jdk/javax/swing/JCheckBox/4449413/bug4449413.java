/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4449413
 * @summary Tests that checkbox and radiobuttons' check marks are visible when background is black
 * @author Ilya Boyandin
 * @run main/manual bug4449413
 */

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;
import javax.swing.plaf.metal.OceanTheme;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class bug4449413 extends JFrame {

    private static final String INSTRUCTIONS =
            "There are eight controls with black backgrounds.\n" +
            "Four enabled (on the left side) and four disabled (on the right side)\n" +
            "checkboxes and radiobuttons.\n\n" +
            "1. If at least one of the controls' check marks is not visible:\n" +
            "   the test fails.\n";

    private static final String INSTRUCTIONS_ADDITIONS_METAL =
            "\n" +
            "2. Uncheck the \"Use Ocean Theme\" check box.\n" +
            "   If now at least one of the controls' check marks is not visible:\n" +
            "   the test fails.\n";

    private static final CountDownLatch latch = new CountDownLatch(1);
    private static volatile boolean failed = true;

    private final MetalTheme defaultMetalTheme = new DefaultMetalTheme();
    private final MetalTheme oceanTheme = new OceanTheme();

    private static bug4449413 instance;

    boolean isMetalLookAndFeel() {
        return UIManager.getLookAndFeel() instanceof MetalLookAndFeel;
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeLater(() -> {
            instance = new bug4449413();
            instance.createAndShowGUI();
        });

        boolean timeoutHappened = !latch.await(2, TimeUnit.MINUTES);

        SwingUtilities.invokeAndWait(() -> {
            if (instance != null) {
                instance.dispose();
            }
        });

        System.out.println("Passed: " + !failed);

        if (timeoutHappened || failed) {
            throw new RuntimeException("Test failed!");
        }
    }

    private void createAndShowGUI() {
        setTitle(UIManager.getLookAndFeel().getClass().getName());

        addComponentsToPane();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                latch.countDown();
            }
        });

        setLocationRelativeTo(null);
        pack();
        setVisible(true);
    }

    public void addComponentsToPane() {
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        JPanel testedPanel = new JPanel();
        testedPanel.setLayout(new GridLayout(4, 6, 10, 15));
        for (int k = 0; k <= 3; k++) {
            for (int j = 1; j >= 0; j--) {
                AbstractButton b = createButton(j, k);
                testedPanel.add(b);
            }
        }

        add(testedPanel);


        if (isMetalLookAndFeel()) {
            JCheckBox oceanThemeSwitch = new JCheckBox("Use Ocean theme", true);
            oceanThemeSwitch.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    MetalLookAndFeel.setCurrentTheme(oceanTheme);
                } else {
                    MetalLookAndFeel.setCurrentTheme(defaultMetalTheme);
                }
                SwingUtilities.updateComponentTreeUI(testedPanel);
            });

            add(oceanThemeSwitch);
        }

        JTextArea instructionArea = new JTextArea(
                isMetalLookAndFeel()
                        ? INSTRUCTIONS + INSTRUCTIONS_ADDITIONS_METAL
                        : INSTRUCTIONS
                );

        instructionArea.setEditable(false);
        instructionArea.setFocusable(false);
        instructionArea.setMargin(new Insets(10,10,10,10));

        add(instructionArea);


        JButton passButton = new JButton("Pass");
        JButton failButton = new JButton("Fail");

        ActionListener actionListener = e -> {
            failed = e.getSource() == failButton;
            latch.countDown();
        };

        passButton.addActionListener(actionListener);
        failButton.addActionListener(actionListener);

        JPanel passFailPanel = new JPanel();
        passFailPanel.add(passButton);
        passFailPanel.add(failButton);

        add(passFailPanel);
    }

    static AbstractButton createButton(int enabled, int type) {
        AbstractButton b = switch (type) {
            case 0 -> new JRadioButton("RadioButton");
            case 1 -> new JCheckBox("CheckBox");
            case 2 -> new JRadioButtonMenuItem("RBMenuItem");
            case 3 -> new JCheckBoxMenuItem("CBMenuItem");
            default -> throw new IllegalArgumentException("type should be in range of 0..3");
        };

        b.setOpaque(true);
        b.setBackground(Color.black);
        b.setForeground(Color.white);
        b.setEnabled(enabled == 1);
        b.setSelected(true);
        return b;
    }
}
