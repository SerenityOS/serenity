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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.text.EditorKit;
/*
 * @test
 * @key headful
 * @bug 8031109
 * @author Victor Dyakov
 * @summary  Rendering HTML code in JEditorPane throws NumberFormatException
 * @run main bug8031109
 */
public class bug8031109 {

    private static final String TEXT_HTML = "text/html";

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                JEditorPane editorPane = new JEditorPane();
                editorPane.setEditable(false);
                EditorKit defaultHtmlEditor = JEditorPane
                        .createEditorKitForContentType(TEXT_HTML);
                editorPane.setEditorKitForContentType(TEXT_HTML, defaultHtmlEditor);
                editorPane.setContentType(TEXT_HTML);
                editorPane.getDocument().putProperty("IgnoreCharsetDirective",
                        Boolean.TRUE);
                URL url = generateHtmlFile();
                String html = "<html>"
                        + "<frameset rows=\"120px, 120 PX ,  * , 10 *\">\n"
                        + "   <frame name=\"top\" src=\"" + url + "\" />\n"
                        + "   <frame name=\"center\" src=\"" + url + "\" />\n"
                        + "   <frame name=\"main\" src=\"" + url + "\" />\n"
                        + "   <noframes>\n"
                        + "   <body>\n"
                        + "      Your browser does not support frames.\n"
                        + "   </body>\n"
                        + "   </noframes>\n"
                        + "</frameset>";
                editorPane.setText(html);
                JScrollPane scrollPane = new JScrollPane(editorPane);

                JFrame frame = new JFrame();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.getContentPane().add(scrollPane);
                frame.setSize(500, 500);
                frame.setVisible(true);
            }
        });
    }

    private static URL generateHtmlFile() {
        File file = new File("hello.html");
        try (
                FileWriter fw = new FileWriter(file.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);) {
            bw.write("<head></head><body>Hello World!</body>");
            return file.toURI().toURL();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
