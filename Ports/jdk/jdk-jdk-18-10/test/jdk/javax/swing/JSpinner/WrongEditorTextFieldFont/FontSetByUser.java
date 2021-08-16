/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.UIResource;

import static javax.swing.JSpinner.*;
import static javax.swing.UIManager.getInstalledLookAndFeels;

/**
 * @test
 * @key headful
 * @bug 5036022
 */
public class FontSetByUser implements Runnable {

    private static final Font USERS_FONT = new Font("dialog", Font.BOLD, 41);

    public static void main(final String[] args) throws Exception {
        for (final UIManager.LookAndFeelInfo laf : getInstalledLookAndFeels()) {
            SwingUtilities.invokeAndWait(() -> setLookAndFeel(laf));
            SwingUtilities.invokeAndWait(new FontSetByUser());
        }
    }

    @Override
    public void run() {
        final JFrame frame1 = new JFrame();
        try {
            testDefaultFont(frame1);
        } finally {
            frame1.dispose();
        }
    }

    private static void testDefaultFont(final JFrame frame) {
        final JSpinner spinner = new JSpinner();
        final JSpinner spinner_u = new JSpinner();
        frame.setLayout(new FlowLayout(FlowLayout.CENTER, 50, 50));
        frame.getContentPane().add(spinner);
        frame.getContentPane().add(spinner_u);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        final DefaultEditor ed = (DefaultEditor) spinner.getEditor();
        final DefaultEditor ed_u = (DefaultEditor) spinner_u.getEditor();
        ed_u.getTextField().setFont(USERS_FONT);

        for (int i = 5; i < 40; i += 5) {
            /*
             * Validate that the font of the text field is changed to the
             * font of JSpinner if the font of text field was not set by the
             * user.
             */
            final Font tff = ed.getTextField().getFont();
            if (!(tff instanceof UIResource)) {
                throw new RuntimeException("Font must be UIResource");
            }
            if (spinner.getFont().getSize() != tff.getSize()) {
                throw new RuntimeException("Rrong size");
            }
            spinner.setFont(new Font("dialog", Font.BOLD, i));
            /*
             * Validate that the font of the text field is NOT changed to the
             * font of JSpinner if the font of text field was set by the user.
             */
            final Font tff_u = ed_u.getTextField().getFont();
            if (tff_u instanceof UIResource || !tff_u.equals(USERS_FONT)) {
                throw new RuntimeException("Font must NOT be UIResource");
            }
            if (spinner_u.getFont().getSize() == tff_u.getSize()) {
                throw new RuntimeException("Wrong size");
            }
            spinner_u.setFont(new Font("dialog", Font.BOLD, i));
        }
    }

    private static void setLookAndFeel(final UIManager.LookAndFeelInfo laf) {
        try {
            UIManager.setLookAndFeel(laf.getClassName());
            System.out.println("LookAndFeel: " + laf.getClassName());
        } catch (ClassNotFoundException | InstantiationException |
                UnsupportedLookAndFeelException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
