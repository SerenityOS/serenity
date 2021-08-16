/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4928019
 * @key headful
 * @summary Makes sure all the basic classes can be created with GTK.
 * @author Scott Violet
 */

import javax.swing.*;
import javax.swing.plaf.basic.*;

public class bug4928019 {
    public static void main(String[] args) throws Throwable {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
        } catch (UnsupportedLookAndFeelException ex) {
            System.err.println("GTKLookAndFeel is not supported on this platform." +
                    " Test is considered passed.");
            return;
        } catch (ClassNotFoundException ex) {
            System.err.println("GTKLookAndFeel class is not found." +
                    " Test is considered passed.");
            return;
        }
        new JButton() {
            public void updateUI() {
                setUI(new BasicButtonUI());
            }
        };
        new JCheckBox() {
            public void updateUI() {
                setUI(new BasicCheckBoxUI());
            }
        };
        new JCheckBoxMenuItem() {
            public void updateUI() {
                setUI(new BasicCheckBoxMenuItemUI());
            }
        };
        new JColorChooser() {
            public void updateUI() {
                setUI(new BasicColorChooserUI());
            }
        };
        new JComboBox() {
            public void updateUI() {
                setUI(new BasicComboBoxUI());
            }
        };
        new JDesktopPane() {
            public void updateUI() {
                setUI(new BasicDesktopPaneUI());
            }
        };
        new JEditorPane() {
            public void updateUI() {
                setUI(new BasicEditorPaneUI());
            }
        };
        new JFileChooser() {
            public void updateUI() {
                setUI(new BasicFileChooserUI(null));
            }
        };
        new JFormattedTextField() {
            public void updateUI() {
                setUI(new BasicFormattedTextFieldUI());
            }
        };
        new JInternalFrame() {
            public void updateUI() {
                setUI(new BasicInternalFrameUI(null));
            }
        };
        new JLabel() {
            public void updateUI() {
                setUI(new BasicLabelUI());
            }
        };
        new JList() {
            public void updateUI() {
                setUI(new BasicListUI());
            }
        };
        new JMenuBar() {
            public void updateUI() {
                setUI(new BasicMenuBarUI());
            }
        };
        new JMenuItem() {
            public void updateUI() {
                setUI(new BasicMenuItemUI());
            }
        };
        new JMenu() {
            public void updateUI() {
                setUI(new BasicMenuUI());
            }
        };
        new JOptionPane() {
            public void updateUI() {
                setUI(new BasicOptionPaneUI());
            }
        };
        new JPanel() {
            public void updateUI() {
                setUI(new BasicPanelUI());
            }
        };
        new JPasswordField() {
            public void updateUI() {
                setUI(new BasicPasswordFieldUI());
            }
        };
        new JPopupMenu() {
            public void updateUI() {
                setUI(new BasicPopupMenuUI());
            }
        };
        new JProgressBar() {
            public void updateUI() {
                setUI(new BasicProgressBarUI());
            }
        };
        new JRadioButton() {
            public void updateUI() {
                setUI(new BasicRadioButtonUI());
            }
        };
        new JRadioButtonMenuItem() {
            public void updateUI() {
                setUI(new BasicRadioButtonMenuItemUI());
            }
        };
        new JRootPane() {
            public void updateUI() {
                setUI(new BasicRootPaneUI());
            }
        };
        new JScrollBar() {
            public void updateUI() {
                setUI(new BasicScrollBarUI());
            }
        };
        new JScrollPane() {
            public void updateUI() {
                setUI(new BasicScrollPaneUI());
            }
        };
        new JSeparator() {
            public void updateUI() {
                setUI(new BasicSeparatorUI());
            }
        };
        new JSlider() {
            public void updateUI() {
                setUI(new BasicSliderUI(null));
            }
        };
        new JSpinner() {
            public void updateUI() {
                setUI(new BasicSpinnerUI());
            }
        };
        new JSplitPane() {
            public void updateUI() {
                setUI(new BasicSplitPaneUI());
            }
        };
        new JTabbedPane() {
            public void updateUI() {
                setUI(new BasicTabbedPaneUI());
            }
        };
        new JTable() {
            public void updateUI() {
                setUI(new BasicTableUI());
            }
        };
        new JTextArea() {
            public void updateUI() {
                setUI(new BasicTextAreaUI());
            }
        };
        new JTextField() {
            public void updateUI() {
                setUI(new BasicTextFieldUI());
            }
        };
        new JTextPane() {
            public void updateUI() {
                setUI(new BasicTextPaneUI());
            }
        };
        new JToggleButton() {
            public void updateUI() {
                setUI(new BasicToggleButtonUI());
            }
        };
        new JToolBar() {
            public void updateUI() {
                setUI(new BasicToolBarUI());
            }
        };
        new JToolTip() {
            public void updateUI() {
                setUI(new BasicToolTipUI());
            }
        };
        new JTree() {
            public void updateUI() {
                setUI(new BasicTreeUI());
            }
        };
        new JViewport() {
            public void updateUI() {
                setUI(new BasicViewportUI());
            }
        };
        System.out.println("DONE");
    }
}
