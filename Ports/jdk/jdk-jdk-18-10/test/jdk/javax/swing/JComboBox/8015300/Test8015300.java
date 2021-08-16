/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.java.swing.plaf.windows.WindowsComboBoxUI.WindowsComboBoxEditor;
import java.awt.Toolkit;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.ComboBoxEditor;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.UIManager;

import static javax.swing.SwingUtilities.invokeAndWait;
import static javax.swing.SwingUtilities.windowForComponent;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

/*
 * @test
 * @key headful
 * @bug 8015300
   @requires (os.family == "windows")
 * @summary Tests that editable combobox selects all text.
 * @author Sergey Malenkov
 * @library /lib/client/
 * @modules java.desktop/com.sun.java.swing.plaf.windows
 * @build ExtendedRobot
 * @run main Test8015300
 */

public class Test8015300 {
    private static final ExtendedRobot robot = createRobot();
    private static final String[] ITEMS = {
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
            "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};

    private static JComboBox<String> combo;

    public static void main(String[] args) throws Exception {
        UIManager.LookAndFeelInfo[] array = UIManager.getInstalledLookAndFeels();
        for (UIManager.LookAndFeelInfo info : array) {
            UIManager.setLookAndFeel(info.getClassName());
            System.err.println("L&F: " + info.getName());
            invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    combo = new JComboBox<>(ITEMS);
                    combo.addItemListener(new ItemListener() {
                        @Override
                        public void itemStateChanged(ItemEvent event) {
                            if (ItemEvent.SELECTED == event.getStateChange() && combo.isEditable()) {
                                ComboBoxEditor editor = combo.getEditor();
                                Object component = editor.getEditorComponent();
                                if (component instanceof JTextField) {
                                    JTextField text = (JTextField) component;
                                    boolean selected = null != text.getSelectedText();

                                    StringBuilder sb = new StringBuilder();
                                    sb.append(" - ").append(combo.getSelectedIndex());
                                    sb.append(": ").append(event.getItem());
                                    if (selected) {
                                        sb.append("; selected");
                                    }
                                    System.err.println(sb);
                                    if ((editor instanceof WindowsComboBoxEditor) == (null == text.getSelectedText())) {
                                        throw new Error("unexpected state of text selection");
                                    }
                                }
                            }
                        }
                    });
                    JFrame frame = new JFrame(getClass().getSimpleName());
                    frame.add(combo);
                    frame.pack();
                    frame.setLocationRelativeTo(null);
                    frame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                    frame.setVisible(true);
                }
            });
            for (int i = 0; i < ITEMS.length; ++i) {
                select(i, true);
                select(1, false);
            }
            invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    windowForComponent(combo).dispose();
                }
            });
        }
    }

    private static void select(final int index, final boolean editable) throws Exception {
        invokeAndWait(new Runnable() {
            @Override
            public void run() {
                combo.setEditable(editable);
                combo.setSelectedIndex(index);
            }
        });
        robot.waitForIdle(50);
    }
    private static ExtendedRobot createRobot() {
         try {
             ExtendedRobot robot = new ExtendedRobot();
             return robot;
         }catch(Exception ex) {
             ex.printStackTrace();
             throw new Error("Unexpected Failure");
         }
    }
}
