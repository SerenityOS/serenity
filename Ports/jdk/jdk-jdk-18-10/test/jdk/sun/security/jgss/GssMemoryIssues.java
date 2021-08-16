/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8028780
 * @summary JDK KRB5 module throws OutOfMemoryError when CCache is corrupt
 * @run main/othervm -Xmx8m GssMemoryIssues
 */

import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;

public class GssMemoryIssues {

    public static void main(String[] argv) throws Exception {
        GSSManager man = GSSManager.getInstance();
        String s = "me@REALM";
        GSSName name = man.createName(s, GSSName.NT_USER_NAME);
        byte[] exported = name.export();
        // Offset of the length of the mech name. Length in big endian
        int lenOffset = exported.length - s.length() - 4;
        // Make it huge
        exported[lenOffset] = 0x7f;
        try {
            man.createName(exported, GSSName.NT_EXPORT_NAME);
        } catch (GSSException gsse) {
            System.out.println(gsse);
        }
    }
}
