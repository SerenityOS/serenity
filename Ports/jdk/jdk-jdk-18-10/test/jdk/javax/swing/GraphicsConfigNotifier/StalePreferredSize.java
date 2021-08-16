/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.JTree;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SpinnerListModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.tree.DefaultMutableTreeNode;

import sun.swing.MenuItemLayoutHelper;

import static javax.swing.UIManager.getInstalledLookAndFeels;

/**
 * @test
 * @key headful
 * @bug 8201552 8213843 8213535
 * @summary Initial layout of the component should use correct graphics config.
 *          It is checked by SwingUtilities.updateComponentTreeUI(), if layout
 *          was correct the call to updateComponentTreeUI() will be no-op.
 * @modules java.desktop/sun.swing
 * @compile -encoding utf-8 StalePreferredSize.java
 * @run main/othervm/timeout=400 StalePreferredSize
 * @run main/othervm/timeout=400 -Dsun.java2d.uiScale=1 StalePreferredSize
 * @run main/othervm/timeout=400 -Dsun.java2d.uiScale=2.25 StalePreferredSize
 */
public final class StalePreferredSize {

    // Some text to be tested
    static final String TEXT[] = new String[]{
            "<span>A few words to get started before the "
                    + "bug</span><span>overlapping text</span>",
            "A quick brown fox jumps over the lazy dog",
            "El veloz murciélago hindú comía feliz cardillo y kiwi. La cigüeña "
                    + "tocaba el saxofón detrás del palenque de paja",
            "Voix ambiguë d’un cœur qui au zéphyr préfère les jattes de kiwis",
            "다람쥐 헌 쳇바퀴에 타고파",
            "Съешь ещё этих мягких французских булок да выпей же чаю"};

    static JFrame frame;
    static Popup popup;
    static JComponent component;
    static int typeFont = 0; // 0 - default, 1 - bold, 2 - italic
    static boolean addViaPopup;

    public static void main(final String[] args) throws Exception {
        for (final UIManager.LookAndFeelInfo laf : getInstalledLookAndFeels()) {
            EventQueue.invokeAndWait(() -> setLookAndFeel(laf));
            for (typeFont = 0; typeFont < 3; typeFont++) {
                System.err.println("typeFont = " + typeFont);
                for (boolean usePopup : new boolean[]{true, false}) {
                    addViaPopup = usePopup;
                    System.err.println("Use popup: " + usePopup);
                    for (final boolean html : new boolean[]{true, false}) {
                        for (String text : TEXT) {
                            if (html) {
                                text = "<html>" + text + "</html>";
                            }
                            test(text);
                        }
                    }
                }
            }
        }
    }

    private static void test(String text) throws Exception {
        System.err.println("text = " + text);
        // Each Callable create a component to be tested
        final List<Callable<JComponent>> comps = List.of(
                () -> new JLabel(text),
                () -> new JButton(text),
                () -> new JMenuItem(text),
                () -> new JMenu(text),
                () -> new JList<>(new String[]{text}),
                () -> new JComboBox<>(new String[]{text}),
                () -> new JTextField(text),
                () -> new JTextArea(text),
                () -> new JCheckBox(text),
                () -> new JFormattedTextField(text),
                () -> new JRadioButton(text),
                () -> new JTree(new DefaultMutableTreeNode(text)),
                () -> new JSpinner(new SpinnerListModel(new String[]{text})),
                () -> {
                    JToolTip tip = new JToolTip();
                    tip.setTipText(text);
                    return tip;
                    },
                () -> {
                    JEditorPane pane = new JEditorPane();
                    pane.setText(text);
                    return pane;
                    },
                () -> {
                    JTable table = new JTable(1, 1);
                    table.getModel().setValueAt(text, 0, 0);
                    return table;
                    }
        );

        for (final Callable<JComponent> creator : comps) {
            checkComponent(creator);
        }
    }

    static void checkComponent(Callable<JComponent> creator) throws Exception {
        EventQueue.invokeAndWait(() -> {

            try {
                component = creator.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            component.setEnabled(false); // minimize paint/focus events amount
            Font font = component.getFont();
            if (typeFont == 1) {
                component.setFont(new Font(font.deriveFont(Font.BOLD).getAttributes()));
            }
            if (typeFont == 2) {
                component.setFont(new Font(font.deriveFont(Font.ITALIC).getAttributes()));
            }

            frame = new JFrame();
            // incorrect initial insets may ruin our size calculation
            frame.setUndecorated(true); // TODO JDK-8244388
            frame.setLayout(new FlowLayout());
            frame.setSize(700, 400);
            frame.setLocationRelativeTo(null);
            if (addViaPopup) {
                // doing our best to show lightweight or mediumweight popup
                int x = frame.getX() + 50;
                int y = frame.getY() + 200;
                PopupFactory factory = PopupFactory.getSharedInstance();
                popup = factory.getPopup(frame, component, x, y);
                if (component instanceof JMenuItem) {
                    // TODO JDK-8244400
                    MenuItemLayoutHelper.clearUsedParentClientProperties((JMenuItem)component);
                }
            } else {
                frame.add(new JScrollPane(component));
            }
            frame.setVisible(true);
            if (popup != null) {
                popup.show();
            }
        });

        EventQueue.invokeAndWait(() -> {
            if (!component.isValid()) {
                dispose();
                throw new RuntimeException("Component must be valid");
            }

            // After the frame was shown we change nothing, so current layout
            // should be optimal and updateComponentTreeUI() should be no-op
            Dimension before = component.getPreferredSize();
            SwingUtilities.updateComponentTreeUI(frame);
            Dimension after = component.getPreferredSize();

            // We change the font size to some big value, as a result the
            // layout and preferredSize of the component should be changed
            component.setFont(component.getFont().deriveFont(35f));
            Dimension last = component.getPreferredSize();

            dispose();

            if (!Objects.equals(before, after)) {
                System.err.println("Component: " + component);
                System.err.println("Before: " + before);
                System.err.println("After: " + after);
                throw new RuntimeException("Wrong PreferredSize");
            }
            // TODO JDK-8206024
//            if (Objects.equals(after, last)) {
//                System.err.println("Component: " + component);
//                System.err.println("After: " + after);
//                System.err.println("Last: " + last);
//                throw new RuntimeException("Wrong PreferredSize");
//            }
        });
    }

    private static void dispose() {
        if (popup != null) {
            popup.hide();
            popup = null;
        }
        frame.dispose();
    }

    private static void setLookAndFeel(final UIManager.LookAndFeelInfo laf) {
        try {
            UIManager.setLookAndFeel(laf.getClassName());
            System.err.println("LookAndFeel: " + laf.getClassName());
        } catch (final UnsupportedLookAndFeelException ignored) {
            System.err.println(
                    "Unsupported LookAndFeel: " + laf.getClassName());
        } catch (ClassNotFoundException | InstantiationException |
                IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
