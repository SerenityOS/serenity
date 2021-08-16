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

/* @test
 * @summary Verify Variation Selector matches an expected image
 * @bug 8187100
 * @ignore Requires a special font installed.
 */

import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.ImageIcon;
import java.awt.Font;
import java.awt.Color;

public class TestVS {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new TestVS().run();
            }
        });
    }

    private void run()  {
        Font ourFont = null;
        final String fontName = "ipaexm.ttf";
        // download from https://ipafont.ipa.go.jp/node26#en
        // and place in {user.home}/fonts/
        try {
            ourFont = Font.createFont(Font.TRUETYPE_FONT,
                          new java.io.File(new java.io.File(
                              System.getProperty("user.home"),
                              "fonts"), fontName));
            ourFont = ourFont.deriveFont((float)48.0);
            final String actualFontName = ourFont.getFontName();
            if (!actualFontName.equals("IPAexMincho")) {
                System.err.println("*** Warning: missing font IPAexMincho.");
                System.err.println("*** Using font: " + actualFontName);
            }
        } catch(Throwable t) {
            t.printStackTrace();
            System.err.println("Fail: " + t);
            return;
        }
        JFrame frame = new JFrame(System.getProperty("java.version"));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        final JTextArea label = new JTextArea("empty");
        label.setSize(400, 300);
        label.setBorder(new LineBorder(Color.black));
        label.setFont(ourFont);

        final String str = "\u845b\udb40\udd00\u845b\udb40\udd01\n";

        label.setText(str);

        panel.add(label);
        panel.add(new JLabel(ourFont.getFamily()));

        // Show the expected result.
        panel.add(new JLabel(new ImageIcon("TestVS-expect.png")));

        frame.getContentPane().add(panel);
        frame.pack();
        frame.setVisible(true);
    }
}
