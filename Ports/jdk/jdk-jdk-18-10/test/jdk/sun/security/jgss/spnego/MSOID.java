/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8078439
 * @summary SPNEGO auth fails if client proposes MS krb5 OID
 */

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;

import java.lang.Exception;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;

public class MSOID {
    public static void main(String[] args) throws Exception {

        // msoid.txt is a NegTokenInit packet sent from Internet Explorer to
        // IIS server on a test machine. No sensitive info included.
        byte[] header = Files.readAllBytes(
                Paths.get(System.getProperty("test.src"), "msoid.txt"));
        byte[] token = Base64.getMimeDecoder().decode(
                Arrays.copyOfRange(header, 10, header.length));

        GSSCredential cred = null;
        GSSContext ctx = GSSManager.getInstance().createContext(cred);

        try {
            ctx.acceptSecContext(token, 0, token.length);
            // Before the fix, GSS_KRB5_MECH_OID_MS is not recognized
            // and acceptor chooses another mech and goes on
            throw new Exception("Should fail");
        } catch (GSSException gsse) {
            // After the fix, GSS_KRB5_MECH_OID_MS is recognized but the token
            // cannot be accepted because we don't have any krb5 credential.
            gsse.printStackTrace();
            if (gsse.getMajor() != GSSException.NO_CRED) {
                throw gsse;
            }
            for (StackTraceElement st: gsse.getStackTrace()) {
                if (st.getClassName().startsWith("sun.security.jgss.krb5.")) {
                    // Good, it is already in krb5 mech's hand.
                    return;
                }
            }
            throw gsse;
        }
    }
}
