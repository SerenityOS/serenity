/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6387275
  @summary List: the focus is at the top of the first item, XAWT
  @requires os.family == "linux"
  @modules java.desktop/sun.awt
           java.desktop/java.awt.peer
           java.desktop/sun.awt.X11:open
  @run main FocusEmptyListTest
*/

import java.awt.*;
import java.lang.reflect.*;
import java.awt.peer.ListPeer;

import sun.awt.AWTAccessor;

public class FocusEmptyListTest extends Frame {

    public static void main(final String[] args) {
        FocusEmptyListTest app = new FocusEmptyListTest();
        app.start();
    }

    public void start() {
        boolean isXToolkit = Toolkit.getDefaultToolkit()
            .getClass().getName().equals("sun.awt.X11.XToolkit");
        if (!isXToolkit) {
            System.out.println("The test is XAWT-only.");
            return;
        }

        List list = new List();
        Object isIndexDisplayed = null;
        setLayout(new FlowLayout());

        getToolkit().addAWTEventListener(System.out::println,
            AWTEvent.FOCUS_EVENT_MASK | AWTEvent.WINDOW_FOCUS_EVENT_MASK);

        add(list);
        list.add("item1");

        setSize(200, 200);
        setUndecorated(true);
        setLocationRelativeTo(null);
        setVisible(true);
        validate();

        list.removeAll();

        try {

            // peer = List.getPeer()
            ListPeer peer = AWTAccessor.getComponentAccessor().getPeer(list);
            System.out.println("peer = " + peer);
            Class peerClass = peer.getClass();
            System.out.println("peer's class = " + peerClass);

            // isIndexDisplayed = peer.isIndexDisplayed(-1)
            Method isIndexDisplayedM
                = peerClass.getDeclaredMethod("isIndexDisplayed", Integer.TYPE);
            System.out.println("method = " + isIndexDisplayedM);
            isIndexDisplayedM.setAccessible(true);
            isIndexDisplayed = isIndexDisplayedM.invoke(peer, -1);
            System.out.println("isIndexDisplayed=" + isIndexDisplayed);

        } catch (Throwable thr) {
            throw new RuntimeException("TEST FAILED: " + thr);
        }

        if ((Boolean) isIndexDisplayed) {
            throw new RuntimeException("TEST FAILED: -1 should be"
                + " invisible index");
        }

    }// start()

}// class AutomaticAppletTest
