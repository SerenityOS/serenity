/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, JetBrains s.r.o.. All rights reserved.
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
 * @bug 8264292
 * @summary Test implementation of NSAccessibilityList protocol peer
  * @author Artem.Semenov@jetbrains.com
 * @run main/manual AccessibleJListTest
 * @requires (os.family == "windows" | os.family == "mac")
 */

import javax.swing.JList;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JWindow;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.Popup;
import javax.swing.PopupFactory;

import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AccessibleJListTest extends AccessibleComponentTest {

    private static final String[] NAMES = {"One", "Two", "Three", "Four", "Five"};
    static JWindow window;

    public static void main(String[] args) throws Exception {
        a11yTest = new AccessibleJListTest();

        countDownLatch = a11yTest.createCountDownLatch();
        SwingUtilities.invokeLater(((AccessibleJListTest) a11yTest)::createSimpleList);
        countDownLatch.await();

        if (!testResult) {
            throw new RuntimeException(a11yTest.exceptionString);
        }

        countDownLatch = a11yTest.createCountDownLatch();
        SwingUtilities.invokeLater(((AccessibleJListTest) a11yTest)::createSimpleListRenderer);
        countDownLatch.await();

        if (!testResult) {
            throw new RuntimeException(a11yTest.exceptionString);
        }

        countDownLatch = a11yTest.createCountDownLatch();
        SwingUtilities.invokeLater(((AccessibleJListTest) a11yTest)::createSimpleListNamed);
        countDownLatch.await();

        if (!testResult) {
            throw new RuntimeException(a11yTest.exceptionString);
        }

        countDownLatch = a11yTest.createCountDownLatch();
        SwingUtilities.invokeLater(((AccessibleJListTest) a11yTest)::createCombobox);
        countDownLatch.await();

        if (!testResult) {
            throw new RuntimeException(a11yTest.exceptionString);
        }

        countDownLatch = a11yTest.createCountDownLatch();
        SwingUtilities.invokeLater(((AccessibleJListTest) a11yTest)::createPushButton);
        countDownLatch.await();

        if (!testResult) {
            throw new RuntimeException(a11yTest.exceptionString);
        }

        countDownLatch = a11yTest.createCountDownLatch();
        SwingUtilities.invokeLater(((AccessibleJListTest) a11yTest)::createPushButtonHeavyWeight);
        countDownLatch.await();

        if (!testResult) {
            throw new RuntimeException(a11yTest.exceptionString);
        }
    }

    @java.lang.Override
    public CountDownLatch createCountDownLatch() {
        return new CountDownLatch(1);
    }

    public void createSimpleList() {
        INSTRUCTIONS = "INSTRUCTIONS:\n"
                + "Check a11y of JList.\n\n"
                + "Turn screen reader on, and Tab to the list.\n"
                + "Press the up and down arrow buttons to move through the list.\n\n"
                + "If you can hear menu items tab further and press PASS, otherwise press FAIL.\n";

        JPanel frame = new JPanel();

        JList<String> list = new JList<>(NAMES);

        frame.setLayout(new FlowLayout());
        frame.add(list);
        exceptionString = "Accessible JList simple list test failed!";
        super.createUI(frame, "Accessible JList test");
    }

    public void createSimpleListRenderer() {
        INSTRUCTIONS = "INSTRUCTIONS:\n"
                + "Check a11y of JList with renderer.\n\n"
                + "Turn screen reader on, and Tab to the list.\n"
                + "Press the up and down arrow buttons to move through the list.\n\n"
                + "If you can hear menu items tab further and press PASS, otherwise press FAIL.\n";

        JPanel frame = new JPanel();

        JList<String> list = new JList<>(NAMES);
        list.setCellRenderer(new AccessibleJListTestRenderer());

        frame.setLayout(new FlowLayout());
        frame.add(list);
        exceptionString = "Accessible JList with renderer simple list test failed!";
        super.createUI(frame, "Accessible JList test");
    }

    public void createSimpleListNamed() {
        INSTRUCTIONS = "INSTRUCTIONS:\n"
                + "Check a11y of named JList.\n\n"
                + "Turn screen reader on, and Tab to the list.\n"
                + "Press the tab button to move to second list.\n\n"
                + "If you can hear second list name: \"second list\" - tab further and press PASS, otherwise press FAIL.\n";

        JPanel frame = new JPanel();

        JList<String> list = new JList<>(NAMES);
        JList<String> secondList = new JList<>(NAMES);
        secondList.getAccessibleContext().setAccessibleName("Second list");
        frame.setLayout(new FlowLayout());
        frame.add(list);
        frame.add(secondList);
        exceptionString = "Accessible JList simple list named test failed!";
        super.createUI(frame, "Accessible JList test");
    }


    public void createCombobox() {
        INSTRUCTIONS = "INSTRUCTIONS:\n"
                + "Check a11y of JList in a combobox.\n\n"
                + "Turn screen reader on, and Tab to the combobox.\n"
                + "Press the up and down arrow buttons to move through the list.\n\n"
                + "If you can hear combobox items tab further and press PASS, otherwise press FAIL.\n";

        JPanel frame = new JPanel();

        JComboBox<String> combo = new JComboBox<>(NAMES);

        frame.setLayout(new FlowLayout());
        frame.add(combo);
        exceptionString = "Accessible JList combobox test failed!";
        super.createUI(frame, "Accessible JList test");
    }

    public void createPushButton() {
        INSTRUCTIONS = "INSTRUCTIONS:\n"
                + "Check a11y of JList in a popup in a simple window.\n\n"
                + "Turn screen reader on, and Tab to the show button and press space.\n"
                + "Press the up and down arrow buttons to move through the list.\n\n"
                + "If you can hear popup menu items tab further and press PASS, otherwise press FAIL.\n";

        JPanel frame = new JPanel();

        JButton button = new JButton("show");
        button.setPreferredSize(new Dimension(100, 35));

        button.addActionListener(new ActionListener() {

            final Runnable dispose = () -> {
                window.dispose();
                window = null;
                button.setText("show");
            };

            @Override
            public void actionPerformed(ActionEvent e) {
                if (window == null) {
                    Rectangle bounds = frame.getBounds();
                    window = new JWindow(mainFrame);
                    JList<String> winList = new JList<>(NAMES);
                    winList.addKeyListener(new KeyAdapter() {
                        @Override
                        public void keyPressed(KeyEvent e) {
                            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                                dispose.run();
                            }
                        }
                    });
                    window.add(winList);
                    window.setLocation(bounds.x + bounds.width + 20, bounds.y);
                    window.pack();
                    window.setVisible(true);
                    button.setText("hide (ESC)");
                } else {
                    dispose.run();
                }
            }
        });

        frame.setLayout(new FlowLayout());
        frame.add(button);
        exceptionString = "Accessible JList push button with simple window test failed!";
        super.createUI(frame, "Accessible JList test");
    }

    public void createPushButtonHeavyWeight() {
        INSTRUCTIONS = "INSTRUCTIONS:\n"
                + "Check a11y of JList in a popup in a heavy weight window.\n\n"
                + "Turn screen reader on, and Tab to the show button and press space.\n"
                + "Press the up and down arrow buttons to move through the list.\n\n"
                + "If you can hear popup menu items tab further and press PASS, otherwise press FAIL.\n";

        JPanel frame = new JPanel();

        JButton button = new JButton("show");
        button.setPreferredSize(new Dimension(100, 35));

        button.addActionListener(new ActionListener() {
            private Popup popup = null;

            final Runnable dispose = () -> {
                popup.hide();
                popup = null;
                button.requestFocus();
                button.setText("show");
            };

            @Override
            public void actionPerformed(ActionEvent e) {
                if (popup == null) {
                    Rectangle bounds = frame.getBounds();
                    PopupFactory factory = PopupFactory.getSharedInstance();
                    JList<String> winList = new JList<>(NAMES);
                    winList.addKeyListener(new KeyAdapter() {
                        @Override
                        public void keyPressed(KeyEvent e) {
                            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                                dispose.run();
                            }
                        }
                    });
                    popup = factory.getPopup(frame, winList, bounds.x + bounds.width + 20, bounds.y);
                    Window c = SwingUtilities.getWindowAncestor(winList);
                    if (c != null) {
                        c.setAutoRequestFocus(true);
                        c.setFocusableWindowState(true);
                    }
                    popup.show();
                    button.setText("hide (ESC)");
                } else {
                    dispose.run();
                }
            }
        });

        frame.setLayout(new FlowLayout());
        frame.add(button);
        exceptionString = "Accessible JList push button with heavy weight window test failed!";
        super.createUI(frame, "Accessible JList test");
    }

    public static class AccessibleJListTestRenderer extends JPanel implements ListCellRenderer {
        private JLabel labelAJT = new JLabel("AJL");
        private JLabel itemName = new JLabel();

        AccessibleJListTestRenderer() {
            super(new FlowLayout());
            setFocusable(false);
            layoutComponents();
        }

        private void layoutComponents() {
            add(labelAJT);
            add(itemName);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            return new Dimension(Math.min(size.width, 245), size.height);
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            itemName.setText(((String) value));

            getAccessibleContext().setAccessibleName(labelAJT.getText() + ", " + itemName.getText());
            return this;
        }
    }
}
