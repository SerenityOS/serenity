/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
   @bug 7003777
   @summary Nonexistent html entities not parsed properly.
   @author Pavel Porvatov
*/

import javax.swing.*;
import javax.swing.text.BadLocationException;

public class bug7003777 {
    private static final String[] TEST_STRINGS = {
            "&a",
            "&aa",
            "&a;",
            "&aa;",
    };

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                JTextPane pane = new JTextPane();

                pane.setContentType("text/html");

                for (String testString : TEST_STRINGS) {
                    pane.setText(testString);

                    String parsedText;

                    try {
                        parsedText = pane.getDocument().getText(0, pane.getDocument().getLength());
                    } catch (BadLocationException e) {
                        throw new RuntimeException("The test failed.", e);
                    }

                    if (parsedText.charAt(0) != '\n') {
                        throw new RuntimeException("The first char should be \\n");
                    }

                    parsedText = parsedText.substring(1);

                    if (!testString.equals(parsedText)) {
                        throw new RuntimeException("The '" + testString +
                                "' string wasn't parsed correctly. Parsed value is '" + parsedText + "'");
                    }
                }
            }
        });
    }
}
