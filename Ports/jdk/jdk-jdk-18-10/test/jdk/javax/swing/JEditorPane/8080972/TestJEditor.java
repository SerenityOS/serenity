/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.ViewFactory;

/*
 * @test
 * @bug 8080972 8169887
 * @summary Audit Core Reflection in module java.desktop for places that will
 *          require changes to work with modules
 * @author Alexander Scherbatiy
 * @run main/othervm -Djava.security.manager=allow TestJEditor
 */
public class TestJEditor {

    public static void main(String[] args) throws Exception {

        SwingUtilities.invokeAndWait(TestJEditor::testJEditorPane);
        System.setSecurityManager(new SecurityManager());
        SwingUtilities.invokeAndWait(TestJEditor::testJEditorPane);
    }

    private static void testJEditorPane() {

        try {

            JEditorPane.registerEditorKitForContentType("text/html", UserEditorKit.class.getName());
            EditorKit editorKit = JEditorPane.createEditorKitForContentType("text/html");

            if (!(editorKit instanceof UserEditorKit)) {
                throw new RuntimeException("Editor kit is not UserEditorKit!");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class UserEditorKit extends EditorKit {

        @Override
        public String getContentType() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ViewFactory getViewFactory() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Action[] getActions() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Caret createCaret() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Document createDefaultDocument() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void read(InputStream in, Document doc, int pos) throws IOException, BadLocationException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void write(OutputStream out, Document doc, int pos, int len) throws IOException, BadLocationException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void read(Reader in, Document doc, int pos) throws IOException, BadLocationException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void write(Writer out, Document doc, int pos, int len) throws IOException, BadLocationException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
