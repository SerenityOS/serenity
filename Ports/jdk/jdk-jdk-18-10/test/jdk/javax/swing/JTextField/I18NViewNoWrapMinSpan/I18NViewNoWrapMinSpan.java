/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
   @bug 8076164
   @summary [JTextField] When input too long Thai character, cursor's behavior
            is odd
   @author Semyon Sadetsky
*/

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

public class I18NViewNoWrapMinSpan {

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                JTextField textField = new JTextField(15);
                textField.setText("\u0E2112345");
                float noSpaceMin = textField.getUI().getRootView(textField)
                        .getMinimumSpan(0);
                textField.getDocument().insertString(3, " ", null);
                if (noSpaceMin > textField.getUI().getRootView(textField)
                        .getMinimumSpan(0)) {
                    throw new RuntimeException(
                            "Minimum span is calculated for wrapped text");
                }
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println("ok");
    }
}
