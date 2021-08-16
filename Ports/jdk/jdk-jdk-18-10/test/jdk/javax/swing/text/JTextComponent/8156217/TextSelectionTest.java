/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

/**
 * @test
 * @bug 8156217
 * @summary Selected text is shifted on HiDPI display
 * @run main/manual/othervm -Dsun.java2d.uiScale=2 TextSelectionTest
 */
public class TextSelectionTest {

    private static final String INSTRUCTIONS = "This is a manual test.\n"
            + "\n"
            + "Select the current text from the end to the beginning.\n"
            + "\n"
            + "If the text is slightly shiftted from one side to another\n"
            + "and back during selection press Fail.\n"
            + "Otherwise, press Pass.";

    private static final CountDownLatch latch = new CountDownLatch(1);
    private static volatile boolean passed = false;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(TextSelectionTest::createAndShowGUI);
        latch.await(3, TimeUnit.MINUTES);
        System.out.println("passed: " + passed);
        if (!passed) {
            throw new RuntimeException("Test fails!");
        }
    }

    private static void createAndShowGUI() {

        JFrame frame = new JFrame("Follow the instructions below:");
        frame.setSize(700, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new BorderLayout());
        JTextComponent textComponent = new JTextArea(INSTRUCTIONS);
        textComponent.setEditable(false);
        Font font = textComponent.getFont();
        font = font.deriveFont(24.0f);
        textComponent.setFont(font);
        panel.add(textComponent, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel(new FlowLayout());
        JButton passButton = new JButton("Pass");
        passButton.addActionListener((e) -> {
            passed = true;
            latch.countDown();
            frame.dispose();
        });
        JButton failsButton = new JButton("Fail");
        failsButton.addActionListener((e) -> {
            passed = false;
            latch.countDown();
            frame.dispose();
        });

        buttonsPanel.add(passButton);
        buttonsPanel.add(failsButton);
        panel.add(buttonsPanel, BorderLayout.SOUTH);

        frame.getContentPane().add(panel);

        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                latch.countDown();
            }
        });
        frame.setVisible(true);
    }
}
