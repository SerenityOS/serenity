/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4387136 4947349 7197662
 * @summary Due to a bug in XMLSupport.putPreferencesInXml(...),
 *          node's keys would not get exported.
 * @run main/othervm -Djava.util.prefs.userRoot=. ExportNode
 * @author Konstantin Kladko
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class ExportNode {
    private static final String NODE_NAME_1 = "ExportNodeTestName1";
    private static final String NODE_NAME_2 = "ExportNodeTestName2";

    public static void main(String[] args) throws
        BackingStoreException, IOException {
        Preferences N1 = Preferences.userRoot().node("ExportNodeTest1");
        N1.put(NODE_NAME_1,"ExportNodeTestValue1");
        Preferences N2 = N1.node("ExportNodeTest2");
        N2.put(NODE_NAME_2,"ExportNodeTestValue2");
        ByteArrayOutputStream exportStream = new ByteArrayOutputStream();
        N2.exportNode(exportStream);

        // Removal of preference node should always succeed on Solaris/Linux
        // by successfully acquiring the appropriate file lock (4947349)
        N1.removeNode();

        String streamAsString = exportStream.toString("UTF-8");

        StringBuilder sb = null;
        if (streamAsString.lastIndexOf(NODE_NAME_2) == -1) {
            if (sb == null)
                sb = new StringBuilder();
            sb.append(NODE_NAME_2 + " should have been found");
        }
        if (streamAsString.lastIndexOf(NODE_NAME_1) != -1) {
            if (sb == null)
                sb = new StringBuilder();
            else
                sb.append("; ");
            sb.append(NODE_NAME_1 + " should *not* have been found");
        }

        if (sb != null)
            throw new RuntimeException(sb.toString());
   }
}
