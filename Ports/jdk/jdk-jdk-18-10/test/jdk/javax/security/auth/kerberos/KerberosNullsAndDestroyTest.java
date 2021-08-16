/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8043071
 * @summary Expose session key and KRB_CRED through extended GSS-API
 */

import javax.security.auth.kerberos.*;
import java.util.function.Supplier;

public class KerberosNullsAndDestroyTest {

    public static void main(String[] args) throws Exception {

        KerberosPrincipal c = new KerberosPrincipal("me@HERE");
        KerberosPrincipal s = new KerberosPrincipal("you@THERE");

        // These object constructions should throw NullPointerException
        checkNPE(() -> new KerberosKey(c, null, 17, 1));
        checkNPE(() -> new EncryptionKey(null, 17));
        checkNPE(() -> new KerberosCredMessage(null, s, new byte[1]));
        checkNPE(() -> new KerberosCredMessage(c, null, new byte[1]));
        checkNPE(() -> new KerberosCredMessage(c, s, null));

        KerberosKey k1 = new KerberosKey(c, new byte[16], 17, 1);
        EncryptionKey k2 = new EncryptionKey(new byte[16], 17);
        KerberosCredMessage m = new KerberosCredMessage(c, s, new byte[1]);

        // These get calls should throw IllegalStateException
        k1.destroy();
        checkISE(() -> k1.getAlgorithm());
        checkISE(() -> k1.getEncoded());
        checkISE(() -> k1.getFormat());
        checkISE(() -> k1.getKeyType());
        checkISE(() -> k1.getPrincipal());
        checkISE(() -> k1.getVersionNumber());

        k2.destroy();
        checkISE(() -> k2.getAlgorithm());
        checkISE(() -> k2.getEncoded());
        checkISE(() -> k2.getFormat());
        checkISE(() -> k2.getKeyType());

        m.destroy();
        checkISE(() -> m.getSender());
        checkISE(() -> m.getRecipient());
        checkISE(() -> m.getEncoded());
    }

    static void checkNPE(Supplier<?> f) throws Exception {
        check(f, NullPointerException.class);
    }

    static void checkISE(Supplier<?> f) throws Exception {
        check(f, IllegalStateException.class);
    }

    static void check(Supplier<?> f, Class<? extends Exception> type) throws Exception {
        try {
            f.get();
        } catch (Exception e) {
            if (e.getClass() != type) {
                throw e;
            } else {
                return;
            }
        }
        throw new Exception("Should fail");
    }
}
