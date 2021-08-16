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
  @test
  @bug 8142861 8143062 8147016
  @summary Check if multiresolution image behaves properly
           on HiDPI + non-HiDPI display pair.
  @author a.stepanov
  @library /test/lib
  @build jdk.test.lib.Platform
  @run applet/manual=yesno MultiDisplayTest.html
*/


import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import jdk.test.lib.Platform;

public class MultiDisplayTest extends Applet {

    private static final int W = 200, H = 200;

    private static final BaseMultiResolutionImage IMG =
        new BaseMultiResolutionImage(new BufferedImage[]{
        generateImage(1, Color.BLACK), generateImage(2, Color.BLUE)});

    private static boolean checkOS() {
        return Platform.isWindows() || Platform.isOSX();
    }

    public void init() { this.setLayout(new BorderLayout()); }

    public void start() {

        Button b = new Button("Start");
        b.setEnabled(checkOS());

        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                ParentFrame p = new ParentFrame();
                new ChildDialog(p);
            }
        });

        add(b, BorderLayout.CENTER);

        validate();
        setVisible(true);
    }


    private static BufferedImage generateImage(int scale, Color c) {

        BufferedImage image = new BufferedImage(
            scale * W, scale * H, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        g.setColor(c);
        g.fillRect(0, 0, scale * W, scale * H);

        g.setColor(Color.WHITE);
        Font f = g.getFont();
        g.setFont(new Font(f.getName(), Font.BOLD, scale * 48));
        g.drawChars((scale + "X").toCharArray(), 0, 2, scale * W / 2, scale * H / 2);

        return image;
    }

    private static class ParentFrame extends Frame {

        public ParentFrame() {
            EventQueue.invokeLater(this::CreateUI);
        }

        private void CreateUI() {

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) { dispose(); }
            });
            setSize(W, H);
            setLocation(50, 50);
            setTitle("parent");
            setResizable(false);
            setVisible(true);
        }

        @Override
        public void paint(Graphics gr) {
            gr.drawImage(IMG, 0, 0, this);
        }
    }

    private static class ChildDialog extends Dialog {

        public ChildDialog(Frame f) {
            super(f);
            EventQueue.invokeLater(this::CreateUI);
        }

        private void CreateUI() {

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) { dispose(); }
            });
            setSize(W, H);
            setTitle("child");
            setResizable(false);
            setModal(true);
            setVisible(true);
        }

        @Override
        public void paint(Graphics gr) {
            gr.drawImage(IMG, 0, 0, this);
        }
    }
}
