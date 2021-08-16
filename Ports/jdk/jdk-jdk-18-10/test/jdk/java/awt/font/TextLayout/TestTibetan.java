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

/* @test @(#)TestTibetan.java
 * @summary verify tibetan output
 * @bug 6886358
 * @ignore Requires a special font installed
 */

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

public class TestTibetan {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new TestTibetan().run();
            }
        });
    }
    public static boolean AUTOMATIC_TEST=true;  // true; run test automatically, else manually at button push

    private void run()  {
        Font ourFont = null;
        try {
            //For best results: Font from:  http://download.savannah.gnu.org/releases/free-tibetan/jomolhari/
            // place in $(user.home)/fonts/
            ourFont = Font.createFont(Font.TRUETYPE_FONT, new java.io.File(new java.io.File(System.getProperty("user.home"),"fonts"), "Jomolhari-alpha3c-0605331.ttf"));

            //ourFont = new Font("serif",Font.PLAIN, 24);
            ourFont = ourFont.deriveFont((float)24.0);
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

        final String str = "\u0F04\u0F05\u0F0D\u0F0D\u0020\u0F4F\u0F72\u0F53\u0F0B\u0F4F\u0F72\u0F53\u0F0B\u0F42\u0FB1\u0F72\u0F0B\u0F51\u0F54\u0F60\u0F0B\u0F62\u0FA9\u0F63";  // TinTin.

        if(AUTOMATIC_TEST) {  /* run the test automatically (else, manually) */
            label.setText(str);
        } else {
        JButton button = new JButton("Set Char x0DDD");
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

