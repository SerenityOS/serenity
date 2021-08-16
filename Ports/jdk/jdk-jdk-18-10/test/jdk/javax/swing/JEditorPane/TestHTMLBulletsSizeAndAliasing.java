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
 * @bug 8201925 8202013
 * @summary  Verifies if JEditorPane unordered list bullets look pixelated
 *           and large  relative to text font size
 * @run main/manual TestHTMLBulletsSizeAndAliasing
 */
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JEditorPane;

public class TestHTMLBulletsSizeAndAliasing {

    public static void main(String[] args) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        AliasingTest test = new AliasingTest(latch);
        Thread T1 = new Thread(test);
        T1.start();

        // wait for latch to complete
        boolean ret = false;
        try {
            ret = latch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            throw ie;
        }
        if (!ret) {
            test.dispose();
            throw new RuntimeException(" User has not executed the test");
        }

        if (test.testResult == false) {
            throw new RuntimeException("JEditorPane unordered list bullets look pixelated");
        }
    }
}

class AliasingTest implements Runnable {
    static JFrame f;
    static JDialog dialog;
    public boolean testResult = false;
    private final CountDownLatch latch;

    public AliasingTest(CountDownLatch latch) throws Exception {
        this.latch = latch;
    }

    @Override
    public void run() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                createUI();
                aliasingTest();
            });
        } catch (Exception ex) {
            if (f != null) {
                f.dispose();
            }
            latch.countDown();
            throw new RuntimeException("createUI Failed: " + ex.getMessage());
        }

    }

    public void dispose() {
        dialog.dispose();
        f.dispose();
    }


    private static String getHtml() {
        return "<html><body>" +
               "<ul>" +
               "<li>Text</li>" +
               "<li>Document</li>" +
               "</ul>" +
               "</body></html>";
    }

    private static Component createSplitPane() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                createHtmlViewer(false), createHtmlViewer(true));
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(0.5);
        splitPane.setPreferredSize(new Dimension(150, 150));
        return splitPane;
    }

    private static Component createHtmlViewer(boolean antialiasing) {
        JEditorPane editorPane;
        if (antialiasing) {
            editorPane = new JEditorPane() {
                @Override
                public void paint(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    super.paint(g2d);
                    g2d.dispose();
                }
            };
        }
        else {
            editorPane = new JEditorPane();
        }
        editorPane.setEditable(false);
        editorPane.setContentType("text/html");
        editorPane.setText(getHtml());
        return new JScrollPane(editorPane);
    }
    private static void aliasingTest() {
        f = new JFrame("List Bullets");
        f.add(createSplitPane());
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }


    private final void createUI() {
        String description
                = " INSTRUCTIONS:\n"
                + " A JEditorPane divided by SplitPane will be shown.\n"
                + " The upper html is rendered in a default JEditorPane.\n "
                + " The lower html is rendered in a JEditorPane using "
                + " rendering hints to turn on antialiasing.\n"
                + " If upper html bullets looks pixelated AND"
                + " larger than needed relative to text font size\n"
                + " and not as smooth as shown in lower html\n "
                + " then press fail else press pass";

        dialog = new JDialog();
        dialog.setTitle("textselectionTest");
        JTextArea textArea = new JTextArea(description);
        textArea.setEditable(false);
        final JButton passButton = new JButton("PASS");
        passButton.addActionListener((e) -> {
            testResult = true;
            dispose();
            latch.countDown();
        });
        final JButton failButton = new JButton("FAIL");
        failButton.addActionListener((e) -> {
            testResult = false;
            dispose();
            latch.countDown();
        });
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(textArea, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(passButton);
        buttonPanel.add(failButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        dialog.add(mainPanel);
        dialog.pack();
        dialog.setVisible(true);
    }
}
