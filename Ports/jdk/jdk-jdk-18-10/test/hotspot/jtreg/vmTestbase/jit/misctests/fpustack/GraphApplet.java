/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase jit/misctests/fpustack.
 * VM Testbase keywords: [jit, desktop, jdk_desktop, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.misctests.fpustack.GraphApplet
 */

package jit.misctests.fpustack;

import java.util.*;
import java.awt.*;
import java.applet.Applet;
import nsk.share.TestFailure;


public class GraphApplet extends Applet {
    private GraphPanel panel;
    private boolean isApplet = true;
    private boolean initialized = false;

    /**
    ** main method for testing that class
    **
    **/
    public static void main( String[] args )        {
        Frame f = new Frame("GraphApplet");
        GraphApplet app = new GraphApplet();
        app.isApplet = false;
        app.setSize(600,400);
        f.setLayout( new BorderLayout() );
        f.add("Center",app);
        f.setSize(600,400);

        app.init();
        //      f.pack();
        f.show(true);
        app.start();

        try {
            Thread.currentThread().sleep(5*1000);
        } catch (InterruptedException e) {
        }

        f.show(false);
        app.stop();
        f.dispose();
        return;
    }

    /**
    ** init-Method in applet's lifecycle.
    ** the graphic panel is build up and the date is filled.
    **/
    public synchronized void init() {
        System.out.println( "GraphApplet : init");
        setLayout(new BorderLayout());

        panel = new GraphPanel(this, new layout() );
        fill( panel );
        add("Center", panel);
        Panel p = new Panel();
        add("South", p);
        initialized = true;
    }

    public synchronized void start() {
        System.out.println( "GraphApplet : start");
        panel.formatNodes();
    }
    public synchronized void stop() {
        initialized = false;
        System.out.println( "GraphApplet : stop");
    }

    public synchronized void destroy() {
        System.out.println( "GraphApplet : destroy");
    }

    /**
    ** paint the Applet
    **/
    public synchronized void paint(Graphics g) {
        try {
            while ( ! initialized )
                Thread.currentThread().sleep(5);
        } catch (InterruptedException e) {}
        if (g instanceof PrintGraphics )
            System.out.println( "printing GraphApplet ...");
    }

    public synchronized void print(Graphics g) {
        try {
            while ( ! initialized )
                Thread.currentThread().sleep(5);
        } catch (InterruptedException e) {}
        System.out.println( "Print Applet "  + g);
        panel.print(g);
    }

    public void print() {
        // System.out.println( "Print Applet");
        Toolkit kit = getToolkit();
        try {

            PrintJob job = kit.getPrintJob( new Frame("x"), "PrintableFrame print job",
                                            null);
            // do the printing if the user didn't cancel the print job
            if (job != null) {
                Graphics g = job.getGraphics();
                printAll(g);                                    // not paint(g)
                g.dispose();                                    // finish with this page
                job.end();                                              // finish with the PrintJob
            }
        } catch (Exception ex) {
            System.out.println( "print exception " + ex);
        }
    }

    /**
    **
    ** @param      panel   the container for nodes
    **
    **/
    private void
        fill(  GraphPanel panel )       {
            panel.addNodes("Node1", "Node2", "Node3" );
    }
}
