/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Button;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.Scrollbar;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Objects;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JDesktopPane;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.JTableHeader;

import static javax.swing.UIManager.getInstalledLookAndFeels;

/**
 * @test
 * @key headful
 * @bug 6459798
 * @author Sergey Bylokhov
 */
public final class DimensionEncapsulation implements Runnable {

    java.util.List<Component> failures = new ArrayList<>();

    public static void main(final String[] args) throws Exception {
        for (final LookAndFeelInfo laf : getInstalledLookAndFeels()) {
            SwingUtilities.invokeAndWait(() -> setLookAndFeel(laf));
            SwingUtilities.invokeAndWait(new DimensionEncapsulation());
        }
    }

    @Override
    public void run() {
        runTest(new Panel());
        runTest(new Button());
        runTest(new Checkbox());
        runTest(new Canvas());
        runTest(new Choice());
        runTest(new Label());
        runTest(new Scrollbar());
        runTest(new TextArea());
        runTest(new TextField());
        runTest(new Dialog(new JFrame()));
        runTest(new Frame());
        runTest(new Window(new JFrame()));
        runTest(new FileDialog(new JFrame()));
        runTest(new List());
        runTest(new ScrollPane());
        runTest(new JFrame());
        runTest(new JDialog(new JFrame()));
        runTest(new JWindow(new JFrame()));
        runTest(new JLabel("hi"));
        runTest(new JMenu());
        runTest(new JTree());
        runTest(new JTable());
        runTest(new JMenuItem());
        runTest(new JCheckBoxMenuItem());
        runTest(new JToggleButton());
        runTest(new JSpinner());
        runTest(new JSlider());
        runTest(Box.createVerticalBox());
        runTest(Box.createHorizontalBox());
        runTest(new JTextField());
        runTest(new JTextArea());
        runTest(new JTextPane());
        runTest(new JPasswordField());
        runTest(new JFormattedTextField());
        runTest(new JEditorPane());
        runTest(new JButton());
        runTest(new JColorChooser());
        runTest(new JFileChooser());
        runTest(new JCheckBox());
        runTest(new JInternalFrame());
        runTest(new JDesktopPane());
        runTest(new JTableHeader());
        runTest(new JLayeredPane());
        runTest(new JRootPane());
        runTest(new JMenuBar());
        runTest(new JOptionPane());
        runTest(new JRadioButton());
        runTest(new JRadioButtonMenuItem());
        runTest(new JPopupMenu());
        //runTest(new JScrollBar()); --> don't test defines max and min in
        // terms of preferred
        runTest(new JScrollPane());
        runTest(new JViewport());
        runTest(new JSplitPane());
        runTest(new JTabbedPane());
        runTest(new JToolBar());
        runTest(new JSeparator());
        runTest(new JProgressBar());
        if (!failures.isEmpty()) {
            System.out.println("These classes failed");
            for (final Component failure : failures) {
                System.out.println(failure.getClass());
            }
            throw new RuntimeException("Test failed");
        }
    }

    public void runTest(final Component c) {
        try {
            test(c);
            c.setMinimumSize(new Dimension(100, 10));
            c.setMaximumSize(new Dimension(200, 20));
            c.setPreferredSize(new Dimension(300, 30));
            test(c);
        } catch (final Throwable ignored) {
            failures.add(c);
        }
    }

    public void test(final Component component) {
        final Dimension psize = component.getPreferredSize();
        psize.width += 200;
        if (Objects.equals(psize, component.getPreferredSize())) {
            throw new RuntimeException("PreferredSize is wrong");
        }
        final Dimension msize = component.getMaximumSize();
        msize.width += 200;
        if (Objects.equals(msize, component.getMaximumSize())) {
            throw new RuntimeException("MaximumSize is wrong");
        }
        final Dimension misize = component.getMinimumSize();
        misize.width += 200;
        if (Objects.equals(misize, component.getMinimumSize())) {
            throw new RuntimeException("MinimumSize is wrong");
        }
    }

    private static void setLookAndFeel(final LookAndFeelInfo laf) {
        try {
            UIManager.setLookAndFeel(laf.getClassName());
            System.out.println("LookAndFeel: " + laf.getClassName());
        } catch (ClassNotFoundException | InstantiationException |
                UnsupportedLookAndFeelException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
