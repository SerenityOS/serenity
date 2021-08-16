/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JEditorPane;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollBar;
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
import javax.swing.JToolTip;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.UIManager.LookAndFeelInfo;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import static javax.swing.UIManager.getInstalledLookAndFeels;

/**
 * @test
 * @key headful
 * @bug 8134947 8253977 8240709
 * @library /test/lib
 * @run main/timeout=450/othervm UnninstallUIMemoryLeaks
 */
public final class UnninstallUIMemoryLeaks {

    private static JFrame frame;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(400);
            // run one task per look and feel
            List<Process> tasks = new ArrayList<>();
            for (LookAndFeelInfo laf : getInstalledLookAndFeels()) {
                String name = laf.getName();
                tasks.add(runProcess(laf));
            }
            for (Process p : tasks) {
                if (!p.waitFor(end - System.nanoTime(), TimeUnit.NANOSECONDS)) {
                    p.destroyForcibly();
                }
            }
            for (Process task : tasks) {
                new OutputAnalyzer(task).shouldHaveExitValue(0)
                                        .stderrShouldBeEmpty();
            }
            return;
        }

        try {
            createGUI();
            long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(350);
            SwingUtilities.invokeAndWait(() -> {
                while (end > System.nanoTime()) {
                    SwingUtilities.updateComponentTreeUI(frame);
                }
                checkListenersCount(frame);
            });
        } finally {
            if (frame != null) {EventQueue.invokeAndWait(frame::dispose);}
        }
    }

    private static void createGUI() throws Exception {
        EventQueue.invokeAndWait(() -> {
            frame = new JFrame();
            //TODO we sometimes generate unnecessary repaint events
            frame.setIgnoreRepaint(true);
            frame.setLayout(new FlowLayout());

            frame.add(new JButton("JButton"));
            frame.add(new JCheckBox("JCheckBox"));
            frame.add(new JComboBox<>());
            frame.add(new JEditorPane());
            frame.add(new JFormattedTextField("JFormattedTextField"));
            frame.add(new JLabel("label"));
            frame.add(new JPanel());
            frame.add(new JPasswordField("JPasswordField"));
            frame.add(new JProgressBar());
            frame.add(new JRadioButton("JRadioButton"));
            frame.add(new JScrollBar());
            frame.add(new JScrollPane());
            frame.add(new JSeparator());
            frame.add(new JSlider());
            frame.add(new JSpinner());
            frame.add(new JSplitPane());
            frame.add(new JTabbedPane());
            frame.add(new JTable());
            frame.add(new JTextArea("JTextArea"));
            frame.add(new JTextField("JTextField"));
            frame.add(new JTextPane());
            frame.add(new JToggleButton());
            frame.add(new JToolBar());
            frame.add(new JToolTip());
            frame.add(new JTree());
            frame.add(new JViewport());

            final JMenuBar bar = new JMenuBar();
            final JMenu menu1 = new JMenu("menu1");
            final JMenu menu2 = new JMenu("menu2");
            menu1.add(new JMenuItem("menuitem"));
            menu2.add(new JCheckBoxMenuItem("JCheckBoxMenuItem"));
            menu2.add(new JRadioButtonMenuItem("JRadioButtonMenuItem"));
            bar.add(menu1);
            bar.add(menu2);
            frame.setJMenuBar(bar);

            final String[] data = {"one", "two", "three", "four"};
            final JList<String> list = new JList<>(data);
            frame.add(list);

            final JDesktopPane pane = new JDesktopPane();
            final JInternalFrame internalFrame = new JInternalFrame();
            internalFrame.setBounds(10, 10, 130, 130);
            internalFrame.setVisible(true);
            pane.add(internalFrame);
            pane.setSize(150, 150);

            frame.add(pane);
            frame.pack();
            frame.setSize(600, 600);
            frame.setLocationRelativeTo(null);
            // Commented to prevent a reference from RepaintManager
            // frame.setVisible(true);
        });
    }

    private static void checkListenersCount(Component comp) {
        test(comp.getComponentListeners());
        test(comp.getFocusListeners());
        test(comp.getHierarchyListeners());
        test(comp.getHierarchyBoundsListeners());
        test(comp.getKeyListeners());
        test(comp.getMouseListeners());
        test(comp.getMouseMotionListeners());
        test(comp.getMouseWheelListeners());
        test(comp.getInputMethodListeners());
        test(comp.getPropertyChangeListeners());
        if (comp instanceof JComponent) {
            test(((JComponent) comp).getAncestorListeners());
            test(((JComponent) comp).getVetoableChangeListeners());
        }
        if (comp instanceof JMenuItem) {
            test(((JMenuItem) comp).getMenuKeyListeners());
            test(((JMenuItem) comp).getMenuDragMouseListeners());
        }
        if (comp instanceof JMenu) {
            test(((JMenu) comp).getMenuListeners());
        }
        if(comp instanceof Container) {
            for(Component child: ((Container)comp).getComponents()){
                checkListenersCount(child);
            }
        }
    }

    /**
     * Checks the count of specific listeners, assumes that the proper
     * implementation does not use more than 20 listeners.
     */
    private static void test(Object[] listeners) {
        int length = listeners.length;
        if (length > 20) {
            throw new RuntimeException("The count of listeners is: " + length);
        }
    }

    private static Process runProcess(LookAndFeelInfo laf) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "-Dswing.defaultlaf=" + laf.getClassName(), "-mx9m",
                "-XX:+HeapDumpOnOutOfMemoryError",
                UnninstallUIMemoryLeaks.class.getSimpleName(), "mark");
        return ProcessTools.startProcess(laf.getName(), pb);
    }
}
