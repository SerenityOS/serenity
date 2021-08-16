/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
  test
  @bug 8031964
  @summary Dragging images from the browser does not work
  @author Petr Pchelko : area=dnd
  @library ../../regtesthelpers
  @build Sysout
  @run applet/manual=yesno URLDragTest.html
*/

import test.java.awt.regtesthelpers.Sysout;

import java.applet.Applet;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;

public class URLDragTest extends Applet {


    @Override
    public void init() {
        setBackground(Color.red);
        setDropTarget(new DropTarget(this,
                DnDConstants.ACTION_COPY,
                new DropTargetAdapter() {
                    @Override
                    public void dragEnter(DropTargetDragEvent dtde) {
                        dtde.acceptDrag(DnDConstants.ACTION_COPY);
                    }

                    @Override
                    public void dragOver(DropTargetDragEvent dtde) {
                        dtde.acceptDrag(DnDConstants.ACTION_COPY);
                    }

                    @Override
                    public void drop(DropTargetDropEvent dtde) {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY);
                        dtde.getCurrentDataFlavorsAsList()
                                .stream()
                                .map(DataFlavor::toString)
                                .forEach(Sysout::println);
                    }
                }));

        String[] instructions = {
                "1) Open the browser.",
                "2) Drag any image from the browser page to the red square",
                "3) When the image is dropped you should se the list of available DataFlavors",
                "4) If you see application/x-java-url and text/uri-list flavors - test PASSED",
                "5) Otherwise the test is FAILED"};
        Sysout.createDialogWithInstructions(instructions);
    }

    @Override
    public void start() {
        setSize(200, 200);
        setVisible(true);
    }
}
