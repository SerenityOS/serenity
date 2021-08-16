/*
 * Copyright (c) 2004, 2010, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.util.logging.*;
import sun.awt.WindowIDProvider;
import sun.awt.AWTAccessor;
import sun.awt.AWTAccessor.ComponentAccessor;
import java.awt.dnd.*;
import java.awt.datatransfer.*;

public abstract class TestXEmbedServer {
    // vertical position of server AND client windows
    private static final int VERTICAL_POSITION = 200;

    private static final Logger log = Logger.getLogger("test.xembed");
    Frame f;
    Canvas client;
    Button toFocus;
    Button b_modal;
    JButton b_close;
    JDialog modal_d;
    JFrame dummy;
    Container clientCont;
    boolean passed;

    public boolean isPassed() {
        return passed;
    }

    public TestXEmbedServer(boolean manual) {

        // Enable testing extensions in XEmbed server
        System.setProperty("sun.awt.xembed.testing", "true");

        f = new Frame("Main frame");
        f.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    synchronized(TestXEmbedServer.this) {
                        TestXEmbedServer.this.notifyAll();
                    }
                    dummy.dispose();
                    f.dispose();
                }
            });

        f.setLayout(new BorderLayout());

        Container bcont = new Container();

        toFocus = new Button("Click to focus server");
        final TextField tf = new TextField(20);
        tf.setName("0");
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

        Button b_add = new Button("Add client");
        b_add.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addClient();
                }
            });
        Button b_remove = new Button("Remove client");
        b_remove.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (clientCont.getComponentCount() != 0) {
                        clientCont.remove(clientCont.getComponentCount()-1);
                    }
                }
            });
        b_close = new JButton("Close modal dialog");
        b_close.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    modal_d.dispose();
                }
            });
        b_modal = new Button("Show modal dialog");
        b_modal.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    modal_d = new JDialog(f, "Modal dialog", true);
                    modal_d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    modal_d.setBounds(0, 100, 200, 50);
                    modal_d.getContentPane().add(b_close);
                    modal_d.validate();
                    modal_d.show();
                }
            });

        bcont.add(tf);
        bcont.add(toFocus);
        bcont.add(b_add);
        bcont.add(b_remove);
        bcont.add(b_modal);
        if (manual) {
            Button pass = new Button("Pass");
            pass.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        passed = true;
                        synchronized(TestXEmbedServer.this) {
                            TestXEmbedServer.this.notifyAll();
                        }
                    }
                });
            bcont.add(pass);
            Button fail = new Button("Fail");
            fail.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        passed = false;
                        synchronized(TestXEmbedServer.this) {
                            TestXEmbedServer.this.notifyAll();
                        }
                    }
                });
            bcont.add(fail);
        }
        b_modal.setName("2");
        bcont.setLayout(new FlowLayout());
        f.add(bcont, BorderLayout.NORTH);

        clientCont = Box.createVerticalBox();
        f.add(clientCont, BorderLayout.CENTER);

        dummy = new JFrame("Dummy");
        dummy.getContentPane().add(new JButton("Button"));
        dummy.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dummy.setBounds(0, VERTICAL_POSITION, 100, 100);
        dummy.setVisible(true);

        f.setBounds(300, VERTICAL_POSITION, 800, 300);
        f.setVisible(true);
    }

    public abstract Process startClient(Rectangle bounds[], long window);

    public void addClient() {
        client = new Canvas() {
                public void paint(Graphics g) {
                    super.paint(g);
                }
            };
        client.setBackground(new Color(30, 220, 40));
        clientCont.add(client);
        clientCont.validate();
        final ComponentAccessor acc = AWTAccessor.getComponentAccessor();
        WindowIDProvider pid = (WindowIDProvider)acc.getPeer(client);
        log.fine("Added XEmbed server(Canvas) with X window ID " + pid.getWindow());
        Rectangle toFocusBounds = toFocus.getBounds();
        toFocusBounds.setLocation(toFocus.getLocationOnScreen());
        f.validate();

        // KDE doesn't accept clicks on title as activation - click below title
        Rectangle fbounds = f.getBounds();
        fbounds.y += f.getInsets().top;
        fbounds.height -= f.getInsets().top;

        Process proc = startClient(new Rectangle[] {fbounds, dummy.getBounds(), toFocusBounds,
                                                    new Rectangle(b_modal.getLocationOnScreen(), b_modal.getSize()),
                                                    new Rectangle(10, 130, 20, 20)}, pid.getWindow());
        new ClientWatcher(client, proc, clientCont).start();
    }

    public void dispose() {
        f.dispose();
        f = null;
        dummy.dispose();
        dummy = null;
        if (modal_d != null) {
            modal_d.dispose();
            modal_d = null;
        }
    }
}

class ClientWatcher extends Thread {
    private Process clientProcess;
    private Canvas client;
    private Container parent;
    public ClientWatcher(Canvas client, Process proc, Container parent) {
        this.client = client;
        this.clientProcess = proc;
        this.parent = parent;
    }

    public void run() {
        try {
            clientProcess.waitFor();
        } catch (InterruptedException ie) {
        }
        parent.remove(client);
    }
}
