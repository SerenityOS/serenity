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
 * @bug 8024861
 * @summary Incomplete token triggers GSS-API NullPointerException
 */

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;

public class MechTokenMissing {
    public static void main(String[] args) throws Exception {
        GSSCredential cred = null;
        GSSContext ctx = GSSManager.getInstance().createContext(cred);

        String var =
            /*0000*/ "60 1C 06 06 2B 06 01 05 05 02 A0 12 30 10 A0 0E " +
            /*0010*/ "30 0C 06 0A 2B 06 01 04 01 82 37 02 02 0A ";
        byte[] token = new byte[var.length()/3];
        for (int i=0; i<token.length; i++) {
            token[i] = Integer.valueOf(var.substring(3*i,3*i+2), 16).byteValue();
        }
        try {
            ctx.acceptSecContext(token, 0, token.length);
        } catch (GSSException gsse) {
            System.out.println("Expected exception: " + gsse);
        }
    }
}
