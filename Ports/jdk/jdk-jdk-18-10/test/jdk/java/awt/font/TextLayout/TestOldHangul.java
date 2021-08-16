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

/* @test @(#)TestOldHangul.java
 * @summary Verify Old Hangul display
 * @bug 6886358
 * @ignore Requires a special font installed.
 */

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

public class TestOldHangul {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new TestOldHangul().run();
            }
        });
    }
    public static boolean AUTOMATIC_TEST=true;  // true; run test automatically, else manually at button push

    private void run()  {
        Font ourFont = null;
        final String fontName = "UnBatangOdal.ttf";  // download from http://chem.skku.ac.kr/~wkpark/project/font/GSUB/UnbatangOdal/  and place in {user.home}/fonts/
        try {
            ourFont = Font.createFont(Font.TRUETYPE_FONT, new java.io.File(new java.io.File(System.getProperty("user.home"),"fonts"), fontName));
            ourFont = ourFont.deriveFont((float)48.0);
        } catch(Throwable t) {
            t.printStackTrace();
            System.err.println("Fail: " + t);
            return;
        }
        JFrame frame = new JFrame(System.getProperty("java.version"));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        final JTextArea label = new JTextArea("(empty)");
        label.setSize(400, 300);
        label.setBorder(new LineBorder(Color.black));
        label.setFont(ourFont);
        final String str = "\u110A\u119E\u11B7\u0020\u1112\u119E\u11AB\uAE00\u0020\u1100\u119E\u11F9\u0020\u112B\u119E\u11BC\n";

        if(AUTOMATIC_TEST) {  /* run the test automatically (else, manually) */
            label.setText(str);
        } else {
        JButton button = new JButton("Old Hangul");
        button.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                label.setText(str);
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

