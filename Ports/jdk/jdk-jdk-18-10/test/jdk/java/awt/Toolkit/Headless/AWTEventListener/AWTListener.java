/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6738181
  @library ../../../regtesthelpers
  @build Sysout
  @summary Toolkit.getAWTEventListeners returns empty array
  @author andrei dmitriev: area=awt.headless
  @run main/othervm -Djava.awt.headless=true AWTListener
*/

/**
 * In a headless mode add a listener for container events.
 * Check if a single listener is still assigned to the Toolkit class.
 */

import java.awt.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Sysout;

public class AWTListener {
    public static void main(String []s) {
        Toolkit toolkit = Toolkit.getDefaultToolkit();

        AWTEventListener orig = new AWTEventListener() {
                public void eventDispatched(AWTEvent event) { }
            };

        System.out.println("Test: listener to add = " +orig);
        toolkit.addAWTEventListener(orig, AWTEvent.CONTAINER_EVENT_MASK);

        for (AWTEventListener l: toolkit.getAWTEventListeners()){
            System.out.println("Test: listener = " +l+" ");
        }

        if ( toolkit.getAWTEventListeners().length == 0 ) {
            throw new RuntimeException("Case 1. An empty array returned unexpectedly");
        }

        for (AWTEventListener l: toolkit.getAWTEventListeners(AWTEvent.CONTAINER_EVENT_MASK)){
            System.out.println("Test: listener = " +l);
         }

        if ( toolkit.getAWTEventListeners(AWTEvent.CONTAINER_EVENT_MASK).length == 0 ) {
            throw new RuntimeException("Case 2. An empty array returned unexpectedly");
        }
        System.out.println("Test PASSED");
    }
}
