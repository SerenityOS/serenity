/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4105609
 * @summary Test printing of drawArc preceded by drawString
 * @author robi.khan
 */

import java.awt.*;
import java.awt.event.*;

public class PrintArcTest extends Panel implements ActionListener {
    PrintCanvas canvas;

    public PrintArcTest () {
        setLayout(new BorderLayout());
        canvas = new PrintCanvas ();
        add("North", canvas);

        Button b = new Button("Click Me to Print!");
        b.setActionCommand ("print");
        b.addActionListener (this);
        add("South", b);
        validate();
    }


    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd.equals("print")) {
            PrintJob pjob = getToolkit().getPrintJob(getFrame(), "Printing Test", null);
            if (pjob != null) {
                Graphics pg = pjob.getGraphics();

                if (pg != null)  {
                    canvas.printAll(pg);
                    pg.dispose();  //flush page
                }
                pjob.end();
            }
        }
    }

    private Frame getFrame() {
        Container cont = getParent();;

        while ( !(cont instanceof Frame  ) ) {
            cont = cont.getParent();
        }

        return (Frame)cont;
    }

    public static void main(String args[]) {
        PrintArcTest test = new PrintArcTest();
        Frame   f = new Frame( "PrintArcTest for Bug #4105609");
        f.add( test );
        f.setSize(500,400);
        f.show();
        f.addWindowListener( new WindowAdapter() {
                                        public void windowClosing(WindowEvent ev) {
                                            System.exit(0);
                                        }
                                    }
                            );
    }
}

class PrintCanvas extends Canvas {
    public Dimension getPreferredSize() {
            return new Dimension(300,300);
    }

    public void paint (Graphics g) {
        g.drawString("drawArc(25,50,150,100,45,90);",25,25);
        g.drawArc(25,50,150,100,45,90);

        g.drawString("drawOval(25,200,200,40);",25,175);
        g.drawOval(25,200,200,40);

        g.dispose();
    }
}
