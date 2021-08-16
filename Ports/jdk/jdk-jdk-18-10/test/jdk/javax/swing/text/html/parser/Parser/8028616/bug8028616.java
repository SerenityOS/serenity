/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8028616
 * @summary Tests correct parsing of the text with leading slash (/)
 * @author Dmitry Markov
 */

import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.io.StringReader;

public class bug8028616 {
    private static final String text = "/ at start is bad";
    private static Object lock = new Object();
    private static boolean isCallbackInvoked = false;
    private static Exception exception = null;

    public static void main(String[] args) throws Exception {
        ParserCB cb = new ParserCB();
        HTMLEditorKit htmlKit = new HTMLEditorKit();
        HTMLDocument htmlDoc = (HTMLDocument) htmlKit.createDefaultDocument();

        htmlDoc.getParser().parse(new StringReader(text), cb, true);

        synchronized (lock) {
            if (!isCallbackInvoked) {
                lock.wait(5000);
            }
        }

        if (!isCallbackInvoked) {
            throw new RuntimeException("Test Failed: ParserCallback.handleText() is not invoked for text - " + text);
        }

        if (exception != null) {
            throw exception;
        }
    }

    private static class ParserCB extends HTMLEditorKit.ParserCallback {
        @Override
        public void handleText(char[] data, int pos) {
            synchronized (lock) {
                if (!text.equals(new String(data)) || pos != 0) {
                    exception = new RuntimeException(
                        "Test Failed: the data passed to ParserCallback.handleText() does not meet the expectation");
                }
                isCallbackInvoked = true;
                lock.notifyAll();
            }
        }
    }
}

