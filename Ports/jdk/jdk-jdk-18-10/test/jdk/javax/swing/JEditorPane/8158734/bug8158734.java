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

/* @test
   @bug 8158734
   @summary JEditorPane.createEditorKitForContentType throws NPE after 6882559
   @author Mikhail Cherkasov
   @run main bug8158734
*/

import javax.swing.*;
import javax.swing.text.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;


public class bug8158734 {

    public static final String TYPE = "test/test";
    public static final String TYPE_2 = "test2/test2";

    static boolean myClassloaderWasUsed = false;

    static class MyEditorKit extends EditorKit {
        @Override
        public String getContentType() {
            return null;
        }

        @Override
        public ViewFactory getViewFactory() {
            return null;
        }

        @Override
        public Action[] getActions() {
            return new Action[0];
        }

        @Override
        public Caret createCaret() {
            return null;
        }

        @Override
        public Document createDefaultDocument() {
            return null;
        }

        @Override
        public void read(InputStream in, Document doc, int pos) throws IOException, BadLocationException {
        }

        @Override
        public void write(OutputStream out, Document doc, int pos, int len) throws IOException, BadLocationException {

        }

        @Override
        public void read(Reader in, Document doc, int pos) throws IOException, BadLocationException {
        }

        @Override
        public void write(Writer out, Document doc, int pos, int len) throws IOException, BadLocationException {
        }
    }

    static class MyClassloader extends ClassLoader {
        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            myClassloaderWasUsed = true;
            return super.loadClass(name);
        }
    }

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                JEditorPane c = new JEditorPane();
                c.setContentType(TYPE);

                final MyClassloader loader = new MyClassloader();
                JEditorPane.registerEditorKitForContentType(TYPE_2, MyEditorKit.class.getName(), loader);
                JEditorPane.registerEditorKitForContentType(TYPE_2, MyEditorKit.class.getName(), null);
                JEditorPane.createEditorKitForContentType(TYPE_2);

                if (myClassloaderWasUsed) {
                    throw new RuntimeException("Class loader has not been removed for '" + TYPE_2 + "' type");
                }
            }
        });

    }
}