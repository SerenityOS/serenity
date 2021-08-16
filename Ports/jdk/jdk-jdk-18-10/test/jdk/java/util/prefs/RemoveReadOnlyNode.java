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
 * @bug 6178148 7197662
 * @summary check if wrong exception gets thrown if one of the child
 *          nodes is readonly on underlying filesystem when node is
 *          being removed.
 * @run main/othervm -Djava.util.prefs.userRoot=. RemoveReadOnlyNode
 */

import java.io.*;
import java.util.prefs.*;

public class RemoveReadOnlyNode {
    public static void main(String[] args) throws Exception {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Windows"))
            return;
        Preferences root = Preferences.userRoot();
        Preferences node1 = root.node("node1");
        Preferences node1A = node1.node("node1A");
        Preferences node1B = node1.node("node1B");
        node1B.put("mykey", "myvalue");
        node1.flush();
        String node1BDirName = System.getProperty("user.home")
            + "/.java/.userPrefs"
            + "/node1/node1B";
        File node1BDir = new File(node1BDirName);
        node1BDir.setReadOnly();
        try {
            node1.removeNode();
        }
        catch (BackingStoreException ex) {
            //expected exception
        } finally {
            Runtime.getRuntime().exec("chmod 755 " + node1BDirName).waitFor();
            try {
                node1.removeNode();
            } catch (Exception e) {}
        }
    }
}
