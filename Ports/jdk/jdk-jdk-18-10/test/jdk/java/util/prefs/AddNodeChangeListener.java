/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
  * @bug  7160252 7197662
  * @key intermittent
  * @summary Checks if events are delivered to a listener
  *          when a child node is added or removed
  * @run main/othervm -Djava.util.prefs.userRoot=. AddNodeChangeListener
  */

import java.util.prefs.*;

public class AddNodeChangeListener {

    private static final int SLEEP_ITRS = 10;
    private static boolean failed = false;
    private static Preferences userRoot, N2;
    private static NodeChangeListenerAdd ncla;

    public static void main(String[] args)
             throws BackingStoreException, InterruptedException {
        userRoot = Preferences.userRoot();
        ncla = new NodeChangeListenerAdd();
        userRoot.addNodeChangeListener(ncla);
        //Should initiate a node added event
        addNode();
        // Should not initiate a node added event
        addNode();
        //Should initate a child removed event
        removeNode();

        if (failed) {
            throw new RuntimeException("Failed");
        }
    }

    private static void addNode()
            throws BackingStoreException, InterruptedException {
        N2 = userRoot.node("N2");
        userRoot.flush();
        int passItr = -1;

        for (int i = 0; i < SLEEP_ITRS; i++) {
            System.out.print("addNode sleep iteration " + i + "...");
            Thread.sleep(3000);
            System.out.println("done.");
            if (ncla.getAddNumber() == 1) {
                passItr = i;
                break;
            }
        }
        checkPassItr(passItr, "addNode()");
    }

    private static void removeNode()
            throws BackingStoreException, InterruptedException {
        N2.removeNode();
        userRoot.flush();
        int passItr = -1;

        for (int i = 0; i < SLEEP_ITRS; i++) {
            System.out.print("removeNode sleep iteration " + i + "...");
            Thread.sleep(3000);
            System.out.println("done.");
            if (ncla.getAddNumber() == 0) {
                passItr = i;
                break;
            }
        }
        checkPassItr(passItr, "removeNode()");
    }

    /* If the listener wasn't notified on iteration 0, throw a RuntimeException
     * with some contextual information
     */
    private static void checkPassItr(int itr, String methodName) {
        if (itr == 0) {
            System.out.println(methodName + " test passed");
        } else {
            failed = true;
            if (itr == -1) {
                throw new RuntimeException("Failed in " + methodName + " - change listener never notified");
            } else {
                throw new RuntimeException("Failed in " + methodName + " - listener notified on iteration " + itr);
            }
        }
    }

    private static class NodeChangeListenerAdd implements NodeChangeListener {
        private int totalNode = 0;

        @Override
        public void childAdded(NodeChangeEvent evt) {
            totalNode++;
        }

        @Override
        public void childRemoved(NodeChangeEvent evt) {
            totalNode--;
        }

        public int getAddNumber(){
            return totalNode;
        }
    }
}
