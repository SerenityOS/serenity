/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6421058
 * @summary Verify font of the text field is changed to the font of
 *          JSpinner if the font of text field was NOT set by the user
 */

import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.UIResource;
import static javax.swing.UIManager.getInstalledLookAndFeels;

public class FontByDefault implements Runnable {

    public static void main(final String[] args) throws Exception {
        for (final UIManager.LookAndFeelInfo laf : getInstalledLookAndFeels()) {
            SwingUtilities.invokeAndWait(() -> setLookAndFeel(laf));
            SwingUtilities.invokeAndWait(new FontByDefault());
        }
    }

    @Override
    public void run() {
        final JFrame mainFrame = new JFrame();
        try {
            testDefaultFont(mainFrame);
        } finally {
            mainFrame.dispose();
        }
    }

    private static void testDefaultFont(final JFrame frame) {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JSpinner spinner = new JSpinner();
        frame.add(spinner);
        frame.setSize(300, 100);
        frame.setVisible(true);

        final DefaultEditor editor = (DefaultEditor) spinner.getEditor();
        final Font editorFont = editor.getTextField().getFont();

        /*
         * Validate that the font of the text field is changed to the
         * font of JSpinner if the font of text field was not set by the
         * user.
         */

        if (!(editorFont instanceof UIResource)) {
            throw new RuntimeException("Font must be UIResource");
        }
        if (!editorFont.equals(spinner.getFont())) {
            throw new RuntimeException("Wrong FONT");
        }
    }

    private static void setLookAndFeel(final UIManager.LookAndFeelInfo laf) {
        try {
            UIManager.setLookAndFeel(laf.getClassName());
        } catch (ClassNotFoundException | InstantiationException |
                UnsupportedLookAndFeelException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
