/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JPasswordField;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.GapContent;
import javax.swing.text.PlainDocument;
import javax.swing.text.Segment;
import javax.swing.text.StringContent;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

/**
 * @test
 * @bug 8258373
 * @summary The JPasswordField#setText() should reset the old internal storage
 */
public final class CleanInternalStorageOnSetText {

    public static void main(String[] args) throws Exception {
        EventQueue.invokeAndWait(() -> {
            // default case
            testStorage(false, new JPasswordField());
            testStorage(true, new JPasswordField());

            // custom Plain String document
            Document document = new PlainDocument(new StringContent());
            testStorage(false, new JPasswordField(document, "", 10));
            document = new PlainDocument(new StringContent());
            testStorage(true, new JPasswordField(document, "", 10));

            // custom Plain GAP document
            document = new PlainDocument(new GapContent());
            testStorage(false, new JPasswordField(document, "", 10));
            document = new PlainDocument(new GapContent());
            testStorage(true, new JPasswordField(document, "", 10));

            // custom HTMLDocument String document
            document = new HTMLDocument(new StringContent(), new StyleSheet());
            testStorage(false, new JPasswordField(document, "", 10));
            document = new HTMLDocument(new StringContent(), new StyleSheet());
            testStorage(true, new JPasswordField(document, "", 10));

            // custom HTMLDocument GAP document
            document = new HTMLDocument(new GapContent(), new StyleSheet());
            testStorage(false, new JPasswordField(document, "", 10));
            document = new HTMLDocument(new GapContent(), new StyleSheet());
            testStorage(true, new JPasswordField(document, "", 10));
        });
    }

    private static void testStorage(boolean makeGap, JPasswordField pf) {
        test(pf, "123", makeGap);
        test(pf, "1234567", makeGap);
        test(pf, "1234567890", makeGap);
        test(pf, "1".repeat(100), makeGap);
        test(pf, "1234567890", makeGap);
        test(pf, "1234567", makeGap);
        test(pf, "123", makeGap);
        test(pf, "", makeGap);
    }

    private static void test(JPasswordField pf, String text, boolean makeGap) {
        pf.setText(text);
        if (makeGap && text.length() > 3) {
            try {
                pf.getDocument().remove(1, 2);
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
        }
        // if no gaps we can check whole array
        char[] internalArray = getInternalArray(pf);
        ArrayList<Segment> segments = new ArrayList<>();
        if (makeGap) {
            // if gaps exists we can check only part of the array
            Document doc = pf.getDocument();
            int nleft = doc.getLength();
            Segment sgm = new Segment();
            sgm.setPartialReturn(true);
            int offs = 0;
            try {
                while (nleft > 0) {
                    doc.getText(offs, nleft, sgm);
                    segments.add(sgm);
                    nleft -= sgm.count;
                    offs += sgm.count;
                }
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
        }
        System.err.println("Before = " + Arrays.toString(internalArray));
        pf.setText("");
        System.err.println("After = " + Arrays.toString(internalArray));

        if (!makeGap) {
            for (char c : internalArray) {
                if (c != '\u0000' && c != '\n') {
                    throw new RuntimeException(Arrays.toString(internalArray));
                }
            }
        } else {
            for (Segment sgm : segments) {
                for (int i = sgm.offset; i < sgm.count + sgm.offset; i++) {
                    char c = sgm.array[i];
                    if (c != '\u0000' && c != '\n') {
                        throw new RuntimeException(Arrays.toString(sgm.array));
                    }
                }
            }
        }
    }

    /**
     * This method returns the reference to the internal data stored by the
     * document inside JPasswordField.
     */
    private static char[] getInternalArray(JPasswordField pf) {
        Document doc = pf.getDocument();
        int nleft = doc.getLength();
        Segment text = new Segment();
        int offs = 0;
        text.setPartialReturn(true);
        try {
            doc.getText(offs, nleft, text);
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
        return text.array;
    }
}
