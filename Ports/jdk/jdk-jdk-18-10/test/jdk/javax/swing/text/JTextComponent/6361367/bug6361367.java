/*
 * Copyright (c) 2006, 2019, Oracle and/or its affiliates. All rights reserved.
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
   @bug 6361367 7109126 8225144
   @key headful
   @summary Tests that backspace key does delete when Shift is pressed
*/

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;

public class bug6361367 {
    final static String testString = "123 456 789";
    final static String resultString = "123 456 78";
    final static List<Class<? extends JTextComponent>> textClasses = Arrays.asList(
        JTextArea.class, JEditorPane.class, JTextPane.class,
        JTextField.class, JFormattedTextField.class, JPasswordField.class);

    public static void main(String[] args) throws Exception {
        for(UIManager.LookAndFeelInfo info: UIManager.getInstalledLookAndFeels()) {
            UIManager.setLookAndFeel(info.getClassName());
            System.out.println(info);
            for (Class<? extends JTextComponent> clazz : textClasses) {
                boolean res = test(clazz);
                if (! res) {
                    throw new RuntimeException("failed");
                }
            }
        }
    }

    static boolean test(final Class<? extends JTextComponent> textComponentClass) throws Exception  {
        final AtomicReference<JFrame> ref = new AtomicReference<>();
        final JTextComponent textComponent = invokeAndWait(
            new Callable<JTextComponent>() {
                public JTextComponent call() throws Exception {
                    return initialize(textComponentClass, ref);
                }
            });
        waitForFocus(textComponent);
        Robot robot = new Robot();
        robot.setAutoWaitForIdle(true);
        robot.setAutoDelay(100);
        robot.keyPress(KeyEvent.VK_END);
        robot.keyRelease(KeyEvent.VK_END);
        robot.keyPress(KeyEvent.VK_SHIFT);
        robot.keyPress(KeyEvent.VK_BACK_SPACE);
        robot.keyRelease(KeyEvent.VK_BACK_SPACE);
        robot.keyRelease(KeyEvent.VK_SHIFT);
        String str = invokeAndWait(
            new Callable<String>() {
                public String call() throws Exception {
                    JFrame frame = ref.get();
                    if (frame != null) {
                        frame.dispose();
                    }
                    return textComponent.getText();
                }
        });
        return resultString.equals(str);
    }

    static JTextComponent initialize(
            Class<? extends JTextComponent> textComponentClass,
            AtomicReference<JFrame> ref) throws Exception {
        JFrame frame = new JFrame("bug6361367");
        ref.set(frame);
        JTextComponent textComponent = textComponentClass.newInstance();
        textComponent.setText(testString);
        frame.add(textComponent);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        textComponent.requestFocus();
        Caret caret = textComponent.getCaret();
        caret.setDot(textComponent.getDocument().getLength());
        return textComponent;
    }

    static <T> T invokeAndWait(Callable<T> callable) throws Exception {
        FutureTask<T> future = new FutureTask<T>(callable);
        SwingUtilities.invokeLater(future);
        return future.get();
    }

    static void waitForFocus(JComponent component) throws Exception  {
        synchronized (component) {
            while(! component.isFocusOwner()) {
                component.wait(100);
            }
        }
    }
}
