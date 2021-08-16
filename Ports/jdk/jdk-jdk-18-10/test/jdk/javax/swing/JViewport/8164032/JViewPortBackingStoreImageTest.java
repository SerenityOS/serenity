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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

/*
 * @test
 * @bug 8164032 8156217
 * @summary JViewport backing store image is not scaled on HiDPI display
 * @run main/manual JViewPortBackingStoreImageTest
 */
public class JViewPortBackingStoreImageTest {

    private static volatile boolean testResult = false;
    private static volatile CountDownLatch countDownLatch;
    private static final String INSTRUCTIONS = "INSTRUCTIONS:\n\n"
            + "Verify text is drawn with high resolution and text selection "
            + "is not shifted when JViewPort is used on HiDPI display.\n\n"
            + "If the display does not support HiDPI mode press PASS.\n\n"
            + "1. Check that the text does not have low resolution.\n"
            + "If no, press FAIL.\n\n"
            + "2. Select the current text from the end to the beginning.\n"
            + "\n"
            + "If the text is slightly shiftted from one side to another\n"
            + "and back during selection press Fail.\n"
            + "Otherwise, press Pass.";

    private static DefaultStyledDocument doc;
    private static StyleContext styles;
    private static HashMap<String, Style> contentAttributes;

    public static void main(String args[]) throws Exception {
        countDownLatch = new CountDownLatch(1);

        SwingUtilities.invokeLater(JViewPortBackingStoreImageTest::createUI);
        countDownLatch.await(15, TimeUnit.MINUTES);

        if (!testResult) {
            throw new RuntimeException("Test fails!");
        }
    }

    private static void createUI() {

        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        final JFrame mainFrame = new JFrame();
        GridBagLayout layout = new GridBagLayout();
        JPanel mainControlPanel = new JPanel(layout);
        JPanel resultButtonPanel = new JPanel(layout);

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 15, 5, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainControlPanel.add(createComponent(), gbc);

        JTextArea instructionTextArea = new JTextArea();
        instructionTextArea.setText(INSTRUCTIONS);
        instructionTextArea.setEditable(false);
        instructionTextArea.setBackground(Color.white);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainControlPanel.add(instructionTextArea, gbc);

        JButton passButton = new JButton("Pass");
        passButton.setActionCommand("Pass");
        passButton.addActionListener((ActionEvent e) -> {
            testResult = true;
            mainFrame.dispose();
            countDownLatch.countDown();

        });

        JButton failButton = new JButton("Fail");
        failButton.setActionCommand("Fail");
        failButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainFrame.dispose();
                countDownLatch.countDown();
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 0;

        resultButtonPanel.add(passButton, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        resultButtonPanel.add(failButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        mainControlPanel.add(resultButtonPanel, gbc);

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

    private static JComponent createComponent() {
        createStyles();
        for (int i = 0; i < data.length; i++) {
            Paragraph p = data[i];
            addParagraph(p);
        }

        JTextPane textPane = new JTextPane(doc);

        JScrollPane scroller = new JScrollPane();
        JViewport port = scroller.getViewport();
        port.setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
        port.add(textPane);

        return scroller;
    }

    static void createStyles() {
        styles = new StyleContext();
        doc = new DefaultStyledDocument(styles);
        contentAttributes = new HashMap<>();

        // no attributes defined
        Style s = styles.addStyle(null, null);
        contentAttributes.put("none", s);

        Style def = styles.getStyle(StyleContext.DEFAULT_STYLE);

        Style heading = styles.addStyle("heading", def);
        StyleConstants.setFontFamily(heading, "SansSerif");
        StyleConstants.setBold(heading, true);
        StyleConstants.setAlignment(heading, StyleConstants.ALIGN_CENTER);
        StyleConstants.setSpaceAbove(heading, 10);
        StyleConstants.setSpaceBelow(heading, 10);
        StyleConstants.setFontSize(heading, 18);

        // Title
        Style sty = styles.addStyle("title", heading);
        StyleConstants.setFontSize(sty, 32);

        // author
        sty = styles.addStyle("author", heading);
        StyleConstants.setItalic(sty, true);
        StyleConstants.setSpaceBelow(sty, 25);
    }

    static void addParagraph(Paragraph p) {
        try {
            Style s = null;
            for (int i = 0; i < p.data.length; i++) {
                AttributedContent run = p.data[i];
                s = contentAttributes.get(run.attr);
                doc.insertString(doc.getLength(), run.content, s);
            }

            Style ls = styles.getStyle(p.logical);
            doc.setLogicalStyle(doc.getLength() - 1, ls);
            doc.insertString(doc.getLength(), "\n", null);
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Paragraph[] data = new Paragraph[]{
        new Paragraph("title", new AttributedContent[]{
            new AttributedContent("none", "ALICE'S ADVENTURES IN WONDERLAND")
        }),
        new Paragraph("author", new AttributedContent[]{
            new AttributedContent("none", "Lewis Carroll")
        }),
        new Paragraph("heading", new AttributedContent[]{
            new AttributedContent("alice", " ")
        })};

    static class Paragraph {

        Paragraph(String logical, AttributedContent[] data) {
            this.logical = logical;
            this.data = data;
        }
        String logical;
        AttributedContent[] data;
    }

    static class AttributedContent {

        AttributedContent(String attr, String content) {
            this.attr = attr;
            this.content = content;
        }
        String attr;
        String content;
    }
}
