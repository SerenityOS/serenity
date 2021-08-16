/*
 *
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.accessibility.*;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import java.io.*;
import java.applet.*;
import java.net.*;

/**
 * JTree Demo
 *
 * @author Jeff Dinkins
 */
public class TreeDemo extends DemoModule {

    JTree tree;

    /**
     * main method allows us to run as a standalone demo.
     */
    public static void main(String[] args) {
        TreeDemo demo = new TreeDemo(null);
        demo.mainImpl();
    }

    /**
     * TreeDemo Constructor
     */
    public TreeDemo(SwingSet2 swingset) {
        // Set the title for this demo, and an icon used to represent this
        // demo inside the SwingSet2 app.
        super(swingset, "TreeDemo", "toolbar/JTree.gif");

        getDemoPanel().add(createTree(), BorderLayout.CENTER);
    }

    public JScrollPane createTree() {
        DefaultMutableTreeNode top = new DefaultMutableTreeNode(getString("TreeDemo.music"));
        DefaultMutableTreeNode category = null;
        DefaultMutableTreeNode artist = null;
        DefaultMutableTreeNode record = null;

        // open tree data
        URL url = getClass().getResource("/resources/tree.txt");

        try {
            // convert url to buffered string
            InputStream is = url.openStream();
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            BufferedReader reader = new BufferedReader(isr);

            // read one line at a time, put into tree
            String line = reader.readLine();
            while(line != null) {
                // System.out.println("reading in: ->" + line + "<-");
                char linetype = line.charAt(0);
                switch(linetype) {
                   case 'C':
                     category = new DefaultMutableTreeNode(line.substring(2));
                     top.add(category);
                     break;
                   case 'A':
                     if(category != null) {
                         category.add(artist = new DefaultMutableTreeNode(line.substring(2)));
                     }
                     break;
                   case 'R':
                     if(artist != null) {
                         artist.add(record = new DefaultMutableTreeNode(line.substring(2)));
                     }
                     break;
                   case 'S':
                     if(record != null) {
                         record.add(new DefaultMutableTreeNode(line.substring(2)));
                     }
                     break;
                   default:
                     break;
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
        }

        tree = new JTree(top) {
            public Insets getInsets() {
                return new Insets(5,5,5,5);
            }
        };

        tree.setEditable(true);

        return new JScrollPane(tree);
    }

    void updateDragEnabled(boolean dragEnabled) {
        tree.setDragEnabled(dragEnabled);
    }

}
