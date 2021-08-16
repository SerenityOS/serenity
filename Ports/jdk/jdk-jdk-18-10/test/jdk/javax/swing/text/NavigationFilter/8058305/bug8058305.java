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
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.NavigationFilter;
import javax.swing.text.Position;

/*
 * @test
 * @key headful
 * @bug 8058305
 * @summary BadLocationException is not thrown by
 *   javax.swing.text.View.getNextVisualPositionFrom() for invalid positions
 * @run main bug8058305
 */
public class bug8058305 {

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(bug8058305::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame();

        JFormattedTextField textField = new JFormattedTextField();
        NavigationFilter navigationFilter = new NavigationFilter();
        textField.setText("Test for Tests");
        frame.getContentPane().add(textField);
        frame.pack();

        Position.Bias[] biasRet = {Position.Bias.Forward};
        try {
            navigationFilter.getNextVisualPositionFrom(textField, 100,
                    Position.Bias.Backward, SwingConstants.EAST, biasRet);
            throw new RuntimeException("BadLocationException is not thrown!");
        } catch (BadLocationException expectedException) {
        }

        frame.setVisible(true);

        try {
            navigationFilter.getNextVisualPositionFrom(textField, 200,
                    Position.Bias.Forward, SwingConstants.WEST, biasRet);
            throw new RuntimeException("BadLocationException is not thrown!");
        } catch (BadLocationException expectedException) {
        }
    }
}
