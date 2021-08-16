/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Checks that JLayer inside JViewport works is correctly laid out
 * @author Alexander Potochkin
 * @library /lib/client/
 * @build ExtendedRobot
 * @run main bug6824395
 */



import javax.swing.*;
import javax.swing.plaf.LayerUI;
import java.awt.*;

public class bug6824395 {

    static JScrollPane scrollPane;
    static JFrame frame;

    public static void main(String[] args) throws Exception {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame = new JFrame("testing");
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                    JEditorPane editorPane = new JEditorPane();
                    String str = "hello\n";
                    for(int i = 0; i<5; i++) {
                        str += str;
                    }

                    editorPane.setText(str);

                    JLayer<JEditorPane> editorPaneLayer = new JLayer<JEditorPane>(editorPane);
                    LayerUI<JComponent> layerUI = new LayerUI<JComponent>();
                    editorPaneLayer.setUI(layerUI);

                    scrollPane = new JScrollPane(editorPaneLayer);
                    scrollPane.setViewportBorder(null);
                    scrollPane.setPreferredSize(new Dimension(200, 250));
                    frame.add(scrollPane);

                    frame.setSize(200, 200);
                    frame.pack();
                    frame.setVisible(true);
                }
            });
            try {
                 ExtendedRobot robot = new ExtendedRobot();
                 robot.waitForIdle(300);
             }catch(Exception ex) {
                 ex.printStackTrace();
                 throw new Error("Unexpected Failure");
             }
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    if (scrollPane.getViewportBorderBounds().width != scrollPane.getViewport().getView().getWidth()) {
                        throw new RuntimeException("Wrong component's width!");
                    }
                }
            });
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }
}
