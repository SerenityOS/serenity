/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8194873
 * @requires (os.family == "Windows")
 * @summary Checks that right ALT (ALT_GRAPH) key works on Swing components
 * @run main RightAltKeyTest
 */

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.GridLayout;
import java.awt.Robot;
import java.lang.reflect.InvocationTargetException;

public class RightAltKeyTest {

    boolean action = false;
    JFrame frame;

    void testJMenu() {
        frame = new JFrame("Menu Frame");
        JMenuBar mb = new JMenuBar();
        JMenu m1 = new JMenu("File");
        JMenuItem i1 = new JMenuItem("Save");
        JMenuItem i2 = new JMenuItem("Load");

        m1.setMnemonic(KeyEvent.VK_F);

        m1.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                action = true;
                disposeUI();
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            @Override
            public void menuCanceled(MenuEvent e) {
            }
        });

        frame.setJMenuBar(mb);
        mb.add(m1);
        m1.add(i1);
        m1.add(i2);

        frame.setSize(200, 200);
        frame.setVisible(true);
    }

    void testJMenuItem() {
        frame = new JFrame("Menu Frame");
        JMenuBar mb = new JMenuBar();
        JMenu m1 = new JMenu("File");
        JMenuItem i1 = new JMenuItem("Save");
        i1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                InputEvent.ALT_GRAPH_DOWN_MASK));
        i1.addActionListener((e) -> {
            action = true;
            disposeUI();
        });

        frame.setJMenuBar(mb);
        mb.add(m1);
        m1.add(i1);

        frame.setSize(200, 200);
        frame.setVisible(true);
    }

    void testJOptionPane() {
        int selection =  JOptionPane.showConfirmDialog(null, "Do you wish " +
                "to save file?","Confirm", JOptionPane.YES_NO_CANCEL_OPTION);
        //Pressed Yes
        if (selection == 0) {
            action = true;
        }
    }

    void testJTabbedPane() {
        frame =new JFrame();
        JPanel p1=new JPanel();
        JPanel p2=new JPanel();
        JTabbedPane tp=new JTabbedPane();
        tp.add("Main",p1);
        tp.add("Visit",p2);
        tp.setMnemonicAt(0, KeyEvent.VK_M);
        tp.setMnemonicAt(1, KeyEvent.VK_V);

        tp.addChangeListener((e) -> {
            if (tp.getSelectedIndex() == 1)
                action = true;
            disposeUI();
        });

        frame.add(tp);
        frame.setSize(200,200);
        frame.setVisible(true);
    }

    void testJTextArea() {
        JTextField firstField = new JTextField(10);
        JTextField lastField = new JTextField(10);

        JLabel firstLabel = new JLabel("First Name", JLabel.RIGHT);
        firstLabel.setDisplayedMnemonic('F');
        firstLabel.setLabelFor(firstField);

        JLabel lastLabel = new JLabel("Last Name", JLabel.RIGHT);
        lastLabel.setDisplayedMnemonic('L');
        lastLabel.setLabelFor(lastField);

        JPanel p = new JPanel();
        p.setLayout(new GridLayout(2, 2, 5, 5));
        p.add(firstLabel);
        p.add(firstField);
        p.add(lastLabel);
        p.add(lastField);

        frame = new JFrame("MnemonicLabels");
        lastField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                action = true;
                disposeUI();
            }

            @Override
            public void focusLost(FocusEvent e) {

            }
        });

        frame.add(p);
        frame.setSize(200,200);
        frame.setVisible(true);
    }

    void test() throws Exception {
        UIManager.LookAndFeelInfo[] lookAndFeels = UIManager
                .getInstalledLookAndFeels();
        for (UIManager.LookAndFeelInfo lookAndFeel : lookAndFeels) {
            UIManager.setLookAndFeel(lookAndFeel.getClassName());

            Robot robot = new Robot();
            robot.setAutoDelay(100);
            robot.waitForIdle();

            action = false;
            SwingUtilities.invokeLater(this::testJMenu);
            robot.waitForIdle();
            robot.keyPress(KeyEvent.VK_ALT_GRAPH);
            robot.keyPress(KeyEvent.VK_F);
            robot.keyRelease(KeyEvent.VK_F);
            robot.keyRelease(KeyEvent.VK_ALT_GRAPH);
            robot.waitForIdle();
            if (!action)
                errLog("JMenu", lookAndFeel.getClassName());

            action = false;
            SwingUtilities.invokeLater(this::testJMenuItem);
            robot.waitForIdle();
            robot.keyPress(KeyEvent.VK_ALT_GRAPH);
            robot.keyPress(KeyEvent.VK_S);
            robot.keyRelease(KeyEvent.VK_S);
            robot.keyRelease(KeyEvent.VK_ALT_GRAPH);
            robot.waitForIdle();
            if (!action)
                errLog("JMenuItem", lookAndFeel.getClassName());

            action = false;
            SwingUtilities.invokeLater(this::testJOptionPane);
            robot.waitForIdle();
            robot.keyPress(KeyEvent.VK_ALT_GRAPH);
            robot.keyPress(KeyEvent.VK_Y);
            robot.keyRelease(KeyEvent.VK_Y);
            robot.keyRelease(KeyEvent.VK_ALT_GRAPH);
            robot.waitForIdle();
            if (!action)
                errLog("JOptionPane", lookAndFeel.getClassName());

            action = false;
            SwingUtilities.invokeLater(this::testJTabbedPane);
            robot.waitForIdle();
            robot.keyPress(KeyEvent.VK_ALT_GRAPH);
            robot.keyPress(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_ALT_GRAPH);
            robot.waitForIdle();
            if (!action)
                errLog("JTabbedPane", lookAndFeel.getClassName());

            action = false;
            SwingUtilities.invokeLater(this::testJTextArea);
            robot.waitForIdle();
            robot.keyPress(KeyEvent.VK_ALT_GRAPH);
            robot.keyPress(KeyEvent.VK_L);
            robot.keyRelease(KeyEvent.VK_L);
            robot.keyRelease(KeyEvent.VK_ALT_GRAPH);
            robot.waitForIdle();
            if (!action)
                errLog("JTextArea", lookAndFeel.getClassName());
        }
        System.out.println("Passed.");
    }

    void disposeUI() {
        frame.setVisible(false);
        frame.dispose();
    }

    void errLog(String componentName, String lookAndFeel)
            throws InvocationTargetException, InterruptedException
    {
        SwingUtilities.invokeAndWait(this::disposeUI);
        throw new RuntimeException("Actions are not performed for "+
                componentName + " with " + lookAndFeel + " look and feel.");
    }

    public static void main(String[] args) throws Exception {
        RightAltKeyTest t = new RightAltKeyTest();
        t.test();
    }
}
