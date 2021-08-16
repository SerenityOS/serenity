/*
 * Copyright (c) 2004, 2008, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import sun.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;

public class JavaClient {
    ClientContainer cont;
    public static void main(String[] args) {
        if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
            return;
        }

        // Enable testing extensions in XEmbed server
        System.setProperty("sun.awt.xembed.testing", "true");

        boolean xtoolkit = "sun.awt.X11.XToolkit".equals(Toolkit.getDefaultToolkit().getClass().getName());
        final EmbeddedFrame ef = createEmbeddedFrame(xtoolkit, Long.parseLong(args[0]));
        ef.setBackground(new Color(100, 100, 200));
        ef.setLayout(new BorderLayout());
        ef.add(new ClientContainer(ef), BorderLayout.CENTER);
        ef.pack();
        ef.registerListeners();
        ef.setVisible(true);
    }
    private static EmbeddedFrame createEmbeddedFrame(boolean xtoolkit, long window) {
        try {
            Class cl = (xtoolkit?Class.forName("sun.awt.X11.XEmbeddedFrame"):Class.forName("sun.awt.motif.MEmbeddedFrame"));
            Constructor cons = cl.getConstructor(new Class[]{Long.TYPE, Boolean.TYPE});
            return (EmbeddedFrame)cons.newInstance(new Object[] {window, true});
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Can't create embedded frame");
        }
    }
}

class ClientContainer extends Container {
    Window parent;
    int width, height;
    public ClientContainer(Window w) {
        parent = w;
        width = 500;
        height = 50;
        final TextField tf = new TextField(30);

        DragSource ds = new DragSource();
        final DragSourceListener dsl = new DragSourceAdapter() {
                public void dragDropEnd(DragSourceDropEvent dsde) {
                }
            };
        final DragGestureListener dgl = new DragGestureListener() {
                public void dragGestureRecognized(DragGestureEvent dge) {
                    dge.startDrag(null, new StringSelection(tf.getText()), dsl);
                }
            };
        ds.createDefaultDragGestureRecognizer(tf, DnDConstants.ACTION_COPY, dgl);

        final DropTargetListener dtl = new DropTargetAdapter() {
                public void drop(DropTargetDropEvent dtde) {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    try {
                        tf.setText(tf.getText() + (String)dtde.getTransferable().getTransferData(DataFlavor.stringFlavor));
                    } catch (Exception e) {
                    }
                }
            };
        final DropTarget dt = new DropTarget(tf, dtl);

        setLayout(new FlowLayout());
        add(tf);
        Button close = new Button("Close");
        close.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    parent.dispose();
                }
            });
        Button inc = new Button("Increase size");
        inc.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    changeSize(10);
                }
            });
        Button dec = new Button("Decrease size");
        dec.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    changeSize(-10);
                }
            });
        add(close);
        add(inc);
        add(dec);
    }
    void changeSize(int step) {
        width += step;
        height += step;
        parent.pack();
    }
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }
}
