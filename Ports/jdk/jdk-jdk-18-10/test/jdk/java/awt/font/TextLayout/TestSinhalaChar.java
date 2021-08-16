/*
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
 *
 */

/**
 * @test @(#)TestSinhalaChar.java
 * @key headful
 * @summary verify lack of crash on U+0DDD.
 * @bug 6795060
 */

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

public class TestSinhalaChar {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new TestSinhalaChar().run();
            }
        });
    }
    public static boolean AUTOMATIC_TEST=true;  // true; run test automatically, else manually at button push

    private void run() {
        JFrame frame = new JFrame("Test Character (no crash = PASS)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        final JLabel label = new JLabel("(empty)");
        label.setSize(400, 100);
        label.setBorder(new LineBorder(Color.black));
        label.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        if(AUTOMATIC_TEST) {  /* run the test automatically (else, manually) */
           label.setText(Character.toString('\u0DDD'));
        } else {
        JButton button = new JButton("Set Char x0DDD");
        button.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
           label.setText(Character.toString('\u0DDD'));
            }
        });
        panel.add(button);
        }
        panel.add(label);

        frame.getContentPane().add(panel);
        frame.pack();
        frame.setVisible(true);
    }
}

