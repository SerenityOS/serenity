/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7165725
 * @summary  Tests if HTML parser can handle successive script tags in a line
 *           and it does not call false text callback after script tags.
 * @run main bug7165725
 */

import java.awt.BorderLayout;
import java.awt.Robot;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;
import javax.swing.text.AbstractDocument.AbstractElement;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

public class bug7165725 extends JFrame {
    private static class GoldenElement {

        private String goldenName;
        private List<GoldenElement> goldenChildren;

        GoldenElement(String goldenName, GoldenElement... goldenChildren){
            this.goldenName = goldenName;
            if (goldenChildren != null) {
                this.goldenChildren = Arrays.asList(goldenChildren);
            } else {
                this.goldenChildren = new ArrayList<>();
            }
        }

        // throws RuntimeException if not ok
        public void checkStructureEquivalence(AbstractDocument.AbstractElement elem) {
            String name = elem.getName();
            if (!goldenName.equals(name)) {
                throw new RuntimeException("Bad structure: expected element name is '" + goldenName + "' but the actual name was '" + name + "'.");
            }
            int goldenChildCount = goldenChildren.size();
            int childCount = elem.getChildCount();
            if (childCount != goldenChildCount) {
                System.out.print("D: children: ");
                for (int i = 0; i < childCount; i++) {
                    System.out.print(" " + elem.getElement(i).getName());
                }
                System.out.println("");
                System.out.print("D: goldenChildren: ");
                for (GoldenElement ge : goldenChildren) {
                    System.out.print(" " + ge.goldenName);
                }
                System.out.println("");

                throw new RuntimeException("Bad structure: expected child count of element '" + goldenName + "' is '" + goldenChildCount + "' but the actual count was '" + childCount + "'.");
            }
            for (int i = 0; i < childCount; i++) {
                AbstractDocument.AbstractElement nextElem = (AbstractDocument.AbstractElement) elem.getElement(i);
                GoldenElement goldenElement = goldenChildren.get(i);
                goldenElement.checkStructureEquivalence(nextElem);
            }
        }
    }

    private JEditorPane editorPane;
    public void execute(final String urlStr, final GoldenElement goldenElement) throws Exception {
        System.out.println();
        System.out.println("***** TEST: " + urlStr + " *****");
        System.out.println();

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try {
                    editorPane = new JEditorPane();
                    editorPane.setEditorKit(new HTMLEditorKit() {
                        public Document createDefaultDocument() {
                            AbstractDocument doc =
                                    (AbstractDocument) super.createDefaultDocument();
                            doc.setAsynchronousLoadPriority(-1);
                            return doc;
                        }
                    });
                    editorPane.setPage(new URL(urlStr));
                } catch (IOException ex) {
                    throw new RuntimeException("Test failed", ex);
                }
                editorPane.setEditable(false);
                JScrollPane scroller = new JScrollPane();
                JViewport vp = scroller.getViewport();
                vp.add(editorPane);
                add(scroller, BorderLayout.CENTER);
                setDefaultCloseOperation(EXIT_ON_CLOSE);
                setSize(400, 400);
                setLocationRelativeTo(null);
                setVisible(true);
            }
        });

        Robot robot = new Robot();
        robot.waitForIdle();

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                HTMLDocument doc = (HTMLDocument) editorPane.getDocument();
                doc.dump(System.out);
                goldenElement.checkStructureEquivalence((AbstractElement) doc.getDefaultRootElement());
                dispose();
            }
        });

        System.out.println();
        System.out.println("*********************************");
        System.out.println();
    }

    public static void main(String[] args) throws Exception {

        String dirURL = getDirURL();

        System.out.println("dirURL = " + dirURL);

        new bug7165725().execute(dirURL + "successive-script-tag.html", createSuccessiveScriptTags());
        new bug7165725().execute(dirURL + "false-text-after-script.html", createFalseTextAfterScript());

        checkByCallbackForSuccessiveScript();
        checkByCallbackForFalseTextAfterScript();

        System.out.println();
        System.out.println();
        System.out.println("Test passed.");
    }

    static String getDirURL() {
        return "file:///" +
                new File(System.getProperty("test.src", ".")).getAbsolutePath() +
                File.separator;
    }

    static String getParsedContentOneLine(String path) throws Exception {
        File f = new File(path);
        FileReader fr = new FileReader(f);
        ParserDelegator pd = new ParserDelegator();
        SBParserCallback sbcallback = new SBParserCallback();
        pd.parse(fr, sbcallback, true);
        fr.close();
        return sbcallback.getStringOneLine();
    }

    static String getParsedContentOneLine(URL url) throws Exception {
        return getParsedContentOneLine(url.getPath());
    }

    static void checkByCallbackForSuccessiveScript() throws Exception {
        String content = getParsedContentOneLine(new URL(getDirURL() + "successive-script-tag.html"));
        if (!content.matches(".*<script .*/js/js1\\.js.*<script .*/js/js2\\.js.*<script .*/js/js3\\.js.*"))
            throw new RuntimeException("Failed to lookup script tags/attributes.");
        if (!content.matches(".*<style .*stylesheets/base\\.css.*<style .*stylesheets/adv\\.css.*"))
            throw new RuntimeException("Failed to lookup style tags.");
    }

    static void checkByCallbackForFalseTextAfterScript() throws Exception {
        String content = getParsedContentOneLine(new URL(getDirURL() + "false-text-after-script.html"));
        final int bodyIdx = content.indexOf("<body ");
        if (bodyIdx > 0) {
            String sbody = content.substring(bodyIdx);
            // There should be no Text(...) in this html
            if (sbody.indexOf("Text(") >= 0)
                throw new RuntimeException("Unexpected text found.");
        } else {
            throw new RuntimeException("Failed to find body tag.");
        }
    }

    private static GoldenElement createSuccessiveScriptTags() {
        return new GoldenElement("html",
                new GoldenElement("head",
                        new GoldenElement("p-implied",
                                new GoldenElement("title"),
                                new GoldenElement("title"),
                                new GoldenElement("script"),
                                new GoldenElement("comment"),
                                new GoldenElement("script"),
                                new GoldenElement("script"),
                                new GoldenElement("comment"),
                                new GoldenElement("script"),
                                new GoldenElement("script"),
                                new GoldenElement("comment"),
                                new GoldenElement("script"),
                                new GoldenElement("content"))),
                new GoldenElement("body",
                        new GoldenElement("p-implied",
                                new GoldenElement("content"))));
    }

    private static GoldenElement createFalseTextAfterScript() {
        return new GoldenElement("html",
                new GoldenElement("head",
                        new GoldenElement("p-implied",
                                new GoldenElement("title"),
                                new GoldenElement("title"),
                                new GoldenElement("content"))),
                new GoldenElement("body",
                        new GoldenElement("form",
                                new GoldenElement("p-implied",
                                        new GoldenElement("input"),
                                        new GoldenElement("input"),
                                        new GoldenElement("content"))),
                        new GoldenElement("p-implied",
                                new GoldenElement("script"),
                                new GoldenElement("comment"),
                                new GoldenElement("script"),
                                new GoldenElement("script"),
                                new GoldenElement("comment"),
                                new GoldenElement("script"),
                                new GoldenElement("content"))));
    }

    static class SBParserCallback extends HTMLEditorKit.ParserCallback
    {
        private int indentSize = 0;
        private ArrayList<String> elist = new ArrayList<>();

        public String getStringOneLine() {
            StringBuilder sb = new StringBuilder();
            for (String s : elist) sb.append(s);
            return sb.toString();
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            for (String s : elist) sb.append(s + "\n");
            return sb.toString();
        }

        protected void indent() {
            indentSize += 3;
        }
        protected void unIndent() {
            indentSize -= 3; if (indentSize < 0) indentSize = 0;
        }

        protected String pIndent() {
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < indentSize; i++) sb.append(" ");
            return sb.toString();
        }

        public void handleText(char[] data, int pos) {
            elist.add(pIndent() + "Text(" + data.length + " chars) \"" + new String(data) + "\"");
        }

        public void handleComment(char[] data, int pos) {
            elist.add(pIndent() + "Comment(" + data.length + " chars)");
        }

        public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
            elist.add(pIndent() + "Tag start(<" + t.toString() + " " + a + ">, " +
                    a.getAttributeCount() + " attrs)");
            indent();
        }

        public void handleEndTag(HTML.Tag t, int pos) {
            unIndent();
            elist.add(pIndent() + "Tag end(</" + t.toString() + ">)");
        }

        public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
            elist.add(pIndent() + "Tag(<" + t.toString() + ">, " +
                    a.getAttributeCount() + " attrs)");
        }

        public void handleError(String errorMsg, int pos){
        }
    }
}
