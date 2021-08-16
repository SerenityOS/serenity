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

/*
 * @test
 * @bug 4619564 7197662
 * @summary XMl Comments in Preferences File lead to ClassCastException
 * @run main/othervm -Djava.util.prefs.userRoot=. CommentsInXml
 * @author kladko
 */

import java.io.*;
import java.util.prefs.*;

public class CommentsInXml {

    public static void main(String[] argv) throws Exception {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        bos.write(new String(
            "<!DOCTYPE preferences SYSTEM                          " +
            "\"http://java.sun.com/dtd/preferences.dtd\">          " +
            "<preferences EXTERNAL_XML_VERSION=\"1.0\">            " +
            "  <root type=\"user\">                                " +
            "    <map>                                             " +
            "    </map>                                            " +
            "    <node name=\"hlrAgent\"> <!-- HLR Agent -->       " +
            "      <map>                                           " +
            "        <entry key=\"agentName\" value=\"HLRAgent\" />" +
            "      </map>                                          " +
            "    </node>                                           " +
            "  </root>                                             " +
            "</preferences>                                        "
        ).getBytes());

        Preferences ur = Preferences.userRoot();
        ur.importPreferences(new ByteArrayInputStream(bos.toByteArray()));
        ur.node("hlrAgent").removeNode(); // clean
    }
}
