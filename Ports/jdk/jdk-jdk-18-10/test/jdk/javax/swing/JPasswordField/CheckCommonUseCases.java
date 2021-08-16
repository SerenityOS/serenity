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
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JPasswordField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * @test
 * @bug 8258373
 */
public final class CheckCommonUseCases {

    public static void main(String[] args) throws Exception {
        EventQueue.invokeAndWait(() -> {
            JPasswordField pf = new JPasswordField();
            // check that pf work if the new text is longer/shorter than the old
            checkDifferentTextLength(pf);
            // count the listeners called by the setText();
            countListeners(pf);
        });
    }

    private static void countListeners(JPasswordField pf) {
        AtomicInteger insert = new AtomicInteger();
        AtomicInteger update = new AtomicInteger();
        AtomicInteger remove = new AtomicInteger();
        pf.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                insert.incrementAndGet();
                System.err.println("e = " + e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                remove.incrementAndGet();
                System.err.println("e = " + e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update.incrementAndGet();
                System.err.println("e = " + e);
            }
        });
        // set the new text
        pf.setText("aaa");
        if (remove.get() != 0 || update.get() != 0 || insert.get() > 1) {
            System.err.println("remove = " + remove);
            System.err.println("update = " + update);
            System.err.println("insert = " + insert);
            throw new RuntimeException("Unexpected number of listeners");
        }
        insert.set(0);
        update.set(0);
        remove.set(0);

        // replace the old text
        pf.setText("bbb");
        if (remove.get() > 1 || update.get() > 1 || insert.get() > 1) {
            System.err.println("remove = " + remove);
            System.err.println("update = " + update);
            System.err.println("insert = " + insert);
            throw new RuntimeException("Unexpected number of listeners");
        }
        insert.set(0);
        update.set(0);
        remove.set(0);

        // remove the text
        pf.setText("");
        if (remove.get() > 1 || update.get() > 0 || insert.get() > 0) {
            System.err.println("remove = " + remove);
            System.err.println("update = " + update);
            System.err.println("insert = " + insert);
            throw new RuntimeException("Unexpected number of listeners");
        }
    }

    private static void checkDifferentTextLength(JPasswordField pf) {
        // forward
        for (int i = 0 ; i < 100; ++i){
            String expected = ("" + i).repeat(i);
            pf.setText(expected);
            String actual = Arrays.toString(pf.getPassword());
            if (actual.equals(expected)){
                System.err.println("Expected: " + expected);
                System.err.println("Actual: " + actual);
                throw new RuntimeException();
            }
        }
        // backward
        for (int i = 99; i >= 0; --i){
            String expected = ("" + i).repeat(i);
            pf.setText(expected);
            String actual = Arrays.toString(pf.getPassword());
            if (actual.equals(expected)){
                System.err.println("Expected: " + expected);
                System.err.println("Actual: " + actual);
                throw new RuntimeException();
            }
        }
    }
}
