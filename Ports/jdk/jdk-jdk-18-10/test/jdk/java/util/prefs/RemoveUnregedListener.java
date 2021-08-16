/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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


/* @test
 * @bug 4705094 7197662
 * @summary Checks if correct exception gets thrown when removing an
 *          unregistered NodeChangeListener .
 * @run main/othervm -Djava.util.prefs.userRoot=. RemoveUnregedListener
 */

import java.util.prefs.*;
import java.util.*;

public class RemoveUnregedListener {
    public static void main(String[] args) throws Exception {
        Preferences userRoot = null;
        Preferences N1 = null;
        NodeChangeListenerTestAdd ncl = new NodeChangeListenerTestAdd();
        NodeChangeListenerTestAdd ncl2 = new NodeChangeListenerTestAdd();
        NodeChangeListenerTestAdd ncl3 = new NodeChangeListenerTestAdd();
        try {
            userRoot = Preferences.userRoot();
            N1 = userRoot.node("N1");
            userRoot.flush();

            //add ncl nc2
            N1.addNodeChangeListener(ncl);
            N1.addNodeChangeListener(ncl2);
            N1.removeNodeChangeListener(ncl3);
            throw new RuntimeException();
        } catch (IllegalArgumentException iae) {
            System.out.println("Test Passed!");
        } catch (Exception e) {
            System.out.println("Test Failed");
            throw e;
        }
    }

}
class NodeChangeListenerTestAdd implements NodeChangeListener {
    public void childAdded(NodeChangeEvent evt) {}
    public void childRemoved(NodeChangeEvent evt) {}
}
