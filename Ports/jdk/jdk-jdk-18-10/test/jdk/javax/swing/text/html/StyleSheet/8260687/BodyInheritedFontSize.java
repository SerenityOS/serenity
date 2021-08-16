/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument.AbstractElement;
import javax.swing.text.Document;
import javax.swing.text.GlyphView;
import javax.swing.text.View;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

/*
 * @test
 * @bug 8260687
 * @summary  Tests inherited font-size is the same as explicitly specified
 * @run main BodyInheritedFontSize
 */
public class BodyInheritedFontSize {
    private static final String HTML_TEXT = """
            <html>
            <body>
              <p style="font-size: 100%">100% from body</p>
              <p>16pt inherited from body</p>
              <p style="font-size: 16pt">16pt paragraph</p>
            </body>
            </html>
            """;

    private static JEditorPane createEditorPane(boolean w3cUnits, boolean showFrame) {
        JEditorPane htmlPane = new JEditorPane();
        htmlPane.setEditable(false);

        if (w3cUnits) {
            htmlPane.putClientProperty(JEditorPane.W3C_LENGTH_UNITS, Boolean.TRUE);
        }

        HTMLEditorKit kit = new HTMLEditorKit();
        htmlPane.setEditorKit(kit);

        StyleSheet styleSheet = kit.getStyleSheet();
        styleSheet.addRule("body { font-family: sans-serif; font-size: 16pt; }");

        Document doc = kit.createDefaultDocument();
        htmlPane.setDocument(doc);
        htmlPane.setText(HTML_TEXT);

        if (showFrame) {
            JFrame frame = new JFrame("HtmlFontSizeGUITest: "
                                              + (w3cUnits ? "w3c" : "std"));
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.add(new JScrollPane(htmlPane), BorderLayout.CENTER);
            frame.setLocationRelativeTo(null);
            frame.pack();
            frame.setVisible(true);
        }

        // Ignore the result but perform layout
        htmlPane.getPreferredSize();

        return htmlPane;
    }

    public static void main(String[] args) throws Exception {
        final List<String> argsList = Arrays.asList(args);
        final boolean showFrame = toShowFrame(argsList);
        final boolean debugPrint = toDebugPrint(argsList);

        final List<Exception> exceptions = new ArrayList<>(2);
        SwingUtilities.invokeAndWait(() -> {
            for (boolean w3cUnits : new boolean[] {true, false}) {
                JEditorPane htmlPane = createEditorPane(w3cUnits, showFrame);
                try {
                    checkFontSize(htmlPane, w3cUnits, debugPrint);
                } catch (Exception e) {
                    exceptions.add(e);
                }
            }
        });
        if (exceptions.size() > 0) {
            exceptions.forEach(System.err::println);
            throw new RuntimeException(
                    "Test failed: " + exceptions.get(0).getMessage(),
                    exceptions.get(0));
        }
    }

    private static boolean toShowFrame(final List<String> argsList) {
        return argsList.contains("-show");
    }

    private static boolean toDebugPrint(final List<String> argsList) {
        return argsList.contains("-print");
    }

    private static void checkFontSize(JEditorPane htmlPane,
                                      boolean w3cUnits,
                                      boolean debugPrint) {
        final View rootView = htmlPane.getUI().getRootView(htmlPane);
        final View boxView = rootView.getView(0);
        final View bodyView = boxView.getView(1);

        int fontSizePercentage = getViewFontSize(bodyView.getView(0), debugPrint);
        int fontSizeInherited  = getViewFontSize(bodyView.getView(1), debugPrint);
        int fontSizeExplicit   = getViewFontSize(bodyView.getView(2), debugPrint);
        if (debugPrint) {
            System.out.println("w3cUnits: " + w3cUnits + "\n"
                    + "Percentage: " + fontSizePercentage + "\n"
                    + "Inherited: " + fontSizeInherited + "\n"
                    + "Explicit: " + fontSizeExplicit + "\n");
        }
        if (fontSizeInherited != fontSizeExplicit
                || fontSizePercentage != fontSizeExplicit) {
            throw new RuntimeException("The font size is different with "
                    + (w3cUnits ? "w3cUnits" : "stdUnits") + ": "
                    + "Percentage: " + fontSizePercentage + " vs. "
                    + "Inherited: " + fontSizeInherited + " vs. "
                    + "Explicit: " + fontSizeExplicit);
        }
    }

    private static int getViewFontSize(View paragraphView, boolean debugPrint) {
        GlyphView inlineView = findFirstTextRun(paragraphView);
        int fontSize = inlineView.getFont().getSize();
        if (debugPrint) {
            ((AbstractElement) inlineView.getElement()).dump(System.out, 1);
        }
        return fontSize;
    }

    private static GlyphView findFirstTextRun(View view) {
        if (view instanceof GlyphView) {
            return (GlyphView) view;
        }
        for (int i = 0; i < view.getViewCount(); i++) {
            GlyphView textRun = findFirstTextRun(view.getView(i));
            if (textRun != null) {
                return textRun;
            }
        }
        return null;
    }
}
