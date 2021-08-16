/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
   @key headful
   @bug 4936917 7190578 8174717
   @summary  Tests if background is correctly painted when <BODY> has css margins
   @author Denis Sharypov
   @library ../../../regtesthelpers
   @run main bug4936917
*/



import java.awt.Color;
import java.awt.Point;
import java.awt.Robot;
import java.util.Timer;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;


public class bug4936917 {

    private boolean passed = false;
    private Timer timer;
    private JEditorPane editorPane;
    private static JFrame f;
    private volatile Point p = null;

    private String text =
                "<html><head><style>" +
                "body {background-color: #cccccc; margin-top: 36.000000pt;}" +
                "</style></head>" +
                "<body> some text </body></html>";

    public void init() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                editorPane = new JEditorPane("text/html", "");
                editorPane.setEditable(false);
                editorPane.setMargin(new java.awt.Insets(0, 0, 0, 0));
                editorPane.setText(text);

                f = new JFrame();
                f.getContentPane().add(editorPane);
                f.setSize(600, 400);
                f.setVisible(true);
            }
        });
        blockTillDisplayed(editorPane);
        Robot robot  = new Robot();
        robot.waitForIdle();
        robot.delay(300);

        int x0 = p.x + 15 ;
        int y = p.y + 15;
        int match = 0;
        int nonmatch = 0;

        passed = true;
        for (int x = x0; x < x0 + 10; x++) {
            System.out.println("color ("+x+"," + y +")=" + robot.getPixelColor(x,y));
            if (!robot.getPixelColor(x, y).equals(new Color(0xcc, 0xcc, 0xcc))) {
                nonmatch++;
            } else match++;
        }
        if (nonmatch > match) {
            passed = false;
        }
    }

    void blockTillDisplayed(JComponent comp) throws Exception {
        while (p == null) {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    p = comp.getLocationOnScreen();
                });
            } catch (IllegalStateException e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                }
            }
        }
    }

    public void destroy() throws Exception {
        SwingUtilities.invokeAndWait(()->f.dispose());
        if(!passed) {
            throw new RuntimeException("Test failed.");
        }
    }


    public static void main(String args[]) throws Exception {
            bug4936917 test = new bug4936917();
            test.init();
            test.destroy();
    }
}
