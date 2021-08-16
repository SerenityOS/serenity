/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Vector;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.DTD;
import javax.swing.text.html.parser.Element;
import sun.awt.SunToolkit;

/*
 * @test
 * @bug 8017492
 * @modules java.desktop/sun.awt
 * @run main/othervm Test8017492
 * @summary Tests for OutOfMemoryError/NegativeArraySizeException
 * @author Sergey Malenkov
 */

public class Test8017492 {
    public static void main(String[] args) throws Exception {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    SunToolkit.createNewAppContext();
                    DTD dtd = DTD.getDTD("dtd");
                    dtd.elements = new Vector<Element>() {
                        @Override
                        public synchronized int size() {
                            return Integer.MAX_VALUE;
                        }
                    };
                    dtd.getElement("element");
                }
                catch (Exception exception) {
                    throw new Error("unexpected", exception);
                }
            }
        };
        // run task with different AppContext
        Thread thread = new Thread(new ThreadGroup("$$$"), task);
        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                throwable.printStackTrace();
                System.exit(1);
            }
        });
        thread.start();
        thread.join();
        // add error handling
        SunToolkit.createNewAppContext();
        HTMLDocument document = new HTMLDocument() {
            @Override
            public HTMLEditorKit.ParserCallback getReader(int pos) {
                return getReader(pos, 0, 0, null);
            }

            @Override
            public HTMLEditorKit.ParserCallback getReader(int pos, int popDepth, int pushDepth, HTML.Tag insertTag) {
                return new HTMLDocument.HTMLReader(pos, popDepth, pushDepth, insertTag) {
                    @Override
                    public void handleError(String error, int pos) {
                        throw new Error(error);
                    }
                };
            }
        };
        // run parser
        new HTMLEditorKit().insertHTML(document, 0, "<html><body>text", 0, 0, null);
    }
}
