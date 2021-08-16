/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8224475 8238985
 * @summary Verify that JTextPane renders images properly for HTML text
 * @run main/manual TestJTextPaneHTMLRendering
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Image;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.text.EditorKit;
import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class TestJTextPaneHTMLRendering {
    private static JFrame mainFrame = new JFrame();
    private static Dictionary<URL, Image> cache;
    private static JTextPane textPane;
    private static URL urlArrow;

    private static volatile boolean testResult = false;
    private static volatile CountDownLatch countDownLatch;
    private static final String INSTRUCTIONS = "INSTRUCTIONS:\n\n" +
            "Verify that the JTextPane is filled with blue arrow images.\n" +
            "There should be 200 images (10 rows of 20 images each).\n" +
            "This test will run for 10 iterations and the current iteration\n" +
            "is being displayed at top of JTextPane. JTextpane will be\n" +
            "repainted each time  and should have same output\n"+
            "If yes, Press Pass, Otherwise, Press Fail.\n";

    public static void main(String args[]) throws Exception {
        urlArrow = new URL("http:\\arrow.png");
        countDownLatch = new CountDownLatch(1);

        SwingUtilities.invokeLater(TestJTextPaneHTMLRendering::createUI);
        countDownLatch.await(15, TimeUnit.MINUTES);
        SwingUtilities.invokeLater(mainFrame::dispose);

        if (!testResult) {
            throw new RuntimeException("Test failed!");
        }
    }

    private static void createUI() {
        JPanel mainControlPanel = new JPanel(new BorderLayout(20, 20));
        JPanel resultButtonPanel = new JPanel(new GridBagLayout());

        createTestUI(mainControlPanel);

        JTextArea instructionTextArea = new JTextArea();
        instructionTextArea.setText(INSTRUCTIONS);
        instructionTextArea.setEditable(false);
        instructionTextArea.setBackground(Color.white);
        mainControlPanel.add(instructionTextArea, BorderLayout.NORTH);

        JButton passButton = new JButton("Pass");
        passButton.setActionCommand("Pass");
        passButton.addActionListener((ActionEvent e) -> {
            testResult = true;
            countDownLatch.countDown();

        });

        JButton failButton = new JButton("Fail");
        failButton.setActionCommand("Fail");
        failButton.addActionListener(e -> {
            countDownLatch.countDown();
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;

        resultButtonPanel.add(passButton, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        resultButtonPanel.add(failButton, gbc);

        mainControlPanel.add(resultButtonPanel, BorderLayout.SOUTH);

        mainFrame.add(mainControlPanel);
        mainFrame.pack();

        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mainFrame.dispose();
                countDownLatch.countDown();
            }
        });
        mainFrame.setVisible(true);
    }

    static void createTestUI(JPanel panel) {
        textPane = new JTextPane();
        panel.add(textPane, BorderLayout.CENTER);

        final EditorKit l_kit = textPane.getEditorKitForContentType("text/html");
        textPane.setEditable(false);
        textPane.setEditorKit(l_kit);
        cache = (Dictionary<URL, Image>)textPane.getDocument().getProperty("imageCache");
        if (cache==null) {
            cache=new Hashtable<URL, Image>();
            textPane.getDocument().putProperty("imageCache",cache);
        }

        URL arrowLocationUrl = TestJTextPaneHTMLRendering.class.getResource("arrow.png");
        ImageIcon imageIcon = new ImageIcon(arrowLocationUrl);
        Image image = imageIcon.getImage();
        Image scaledImage = image.getScaledInstance(24, 24, java.awt.Image.SCALE_SMOOTH);
        cache.put(urlArrow, scaledImage);
        new Thread(TestJTextPaneHTMLRendering::runTest).start();
    }

    static void runTest() {
        for (int i=0; i < 10; i++)
        {
            StringBuffer sb = new StringBuffer();
            sb.append("<html><body bgcolor=\"#BBBBBB\"><center>Iteration " + (i+1) + " -> " + "<br>");
            for (int j=1;j<201;j++)
            {
                sb.append("<img src=\"" + urlArrow + "\">");
                if (j%20 == 0) sb.append("<br>");
            }
            textPane.setText(sb.toString());
            textPane.validate();
            textPane.repaint();
            try {
                Thread.currentThread().sleep(1000);
            } catch (InterruptedException e) { System.err.println(e); }
        }
    }
}
