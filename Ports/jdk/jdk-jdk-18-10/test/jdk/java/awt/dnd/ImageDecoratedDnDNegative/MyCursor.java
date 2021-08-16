/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.Point;

/**
 * An interface provides a set of custom cursors
 */

public interface MyCursor {
    public static final java.awt.Cursor NO_DROP = Toolkit.getDefaultToolkit().createCustomCursor(
        new ImageGenerator(32, 32, new Color(0xff, 0xff, 0xff, 0x00) ) {
                @Override public void paint(Graphics gr) {
                    gr.setColor(Color.GREEN);
                    ((Graphics2D)gr).setStroke(new BasicStroke(3));

                    gr.translate(width/2, height/2);
                    int R = width/4;
                    gr.drawOval(-R, -R, 2*R, 2*R);
                    gr.drawLine(-R, R, R, -R);
                }
            }.getImage(),
            new Point(0, 0),
            "My NoDrop Cursor"
    );
    public static final java.awt.Cursor MOVE = Toolkit.getDefaultToolkit().createCustomCursor(
        new ImageGenerator(32, 32, new Color(0xff, 0xff, 0xff, 0x00) ) {
                @Override public void paint(Graphics gr) {
                    gr.setColor(Color.GREEN);
                    ((Graphics2D)gr).setStroke(new BasicStroke(3));

                    gr.drawLine(0, 0, width, height);
                    gr.drawLine(0, 0, width/2, 0);
                    gr.drawLine(0, 0, 0, height/2);
                }
            }.getImage(),
            new Point(0, 0),
            "My Move Cursor"
    );
    public static final java.awt.Cursor COPY = Toolkit.getDefaultToolkit().createCustomCursor(
        new ImageGenerator(32, 32, new Color(0xff, 0xff, 0xff, 0x00) ) {
                @Override public void paint(Graphics gr) {
                    gr.setColor(Color.GREEN);
                    ((Graphics2D)gr).setStroke(new BasicStroke(3));
                    //arrow
                    gr.drawLine(0, 0, width/2, height/2);
                    gr.drawLine(0, 0, width/2, 0);
                    gr.drawLine(0, 0, 0, height/2);
                    //plus
                    gr.drawRect(width/2 - 1, height/2 -1, width/2 - 1, height/2 - 1);
                    gr.drawLine(width*3/4 - 1, height/2 - 1, width*3/4 - 1, height);
                    gr.drawLine(width/2 - 1, height*3/4 - 1, width, height*3/4 - 1);
                 }
            }.getImage(),
            new Point(0, 0),
            "My Copy Cursor"
    );
}

