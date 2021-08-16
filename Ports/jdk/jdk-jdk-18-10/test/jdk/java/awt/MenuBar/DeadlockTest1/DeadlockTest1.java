/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @key headful
  @bug 6990904
  @summary on oel5.5, Frame doesn't show if the Frame has only a MenuBar as its component.
  @author Andrei Dmitriev: area=awt-menubar
  @run main/timeout=30 DeadlockTest1
*/

import java.awt.*;

public class DeadlockTest1 {
    Frame f = new Frame("Menu Frame");

    DeadlockTest1() {
        MenuBar menubar = new MenuBar();

        Menu file = new Menu("File");
        Menu edit = new Menu("Edit");
        Menu help = new Menu("Help");

        MenuItem open = new MenuItem("Open");
        MenuItem close = new MenuItem("Close");
        MenuItem copy = new MenuItem("Copy");
        MenuItem paste = new MenuItem("Paste");

        file.add(open);
        file.add(close);

        edit.add(copy);
        edit.add(paste);
        menubar.add(file);
        menubar.add(edit);
        menubar.add(help);
        menubar.setHelpMenu(help);

        f.setMenuBar(menubar);
        f.setSize(400,200);
        f.setVisible(true);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException z) {
            throw new RuntimeException(z);
        }
        f.dispose();
     }

    public static void main(String argv[]) {
        new DeadlockTest1();
    }
}
