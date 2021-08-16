/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
   @bug 7030332
   @summary Default borders in tables looks incorrect JEditorPane
   @author Pavel Porvatov
 * @run applet/manual=yesno bug7030332.html
*/

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class bug7030332 extends JApplet {
    public static final String[] HTML_SAMPLES = new String[]{
            "<table border><tr><th>Column1</th><th>Column2</th></tr></table>",
            "<table border=\"\"><tr><th>Column1</th><th>Column2</th></tr></table>",
            "<table border=\"1\"><tr><th>Column1</th><th>Column2</th></tr></table>",
            "<table border=\"2\"><tr><th>Column1</th><th>Column2</th></tr></table>",
    };

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame();

                frame.setContentPane(createContentPane());
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setSize(600, 400);
                frame.setLocationRelativeTo(null);

                frame.setVisible(true);

            }
        });
    }

    public void init() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    setContentPane(createContentPane());
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Container createContentPane() {
        JPanel result = new JPanel(new GridLayout(HTML_SAMPLES.length + 1, 3, 10, 10));

        result.add(new JLabel("Html code"));
        result.add(new JLabel("Golden image"));
        result.add(new JLabel("JEditorPane"));

        for (int i = 0; i < HTML_SAMPLES.length; i++) {
            String htmlSample = HTML_SAMPLES[i];

            JTextArea textArea = new JTextArea(htmlSample);

            textArea.setLineWrap(true);

            result.add(textArea);

            String imageName = "sample" + i + ".png";
            URL resource = bug7030332.class.getResource(imageName);

            result.add(resource == null ? new JLabel(imageName + " not found") :
                    new JLabel(new ImageIcon(resource), SwingConstants.LEFT));

            result.add(new JEditorPane("text/html", htmlSample));
        }

        return result;
    }
}
