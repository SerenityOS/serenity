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

import java.security.*;

/*
 * Export a private key from the named keychain entry without supplying a
 * password. See JDK-8062264.
 *
 * NOTE: Keychain access controls must already have been lowered to permit
 *       the target entry to be accessed.
 */
public class ExportPrivateKeyNoPwd {

    public static final void main(String[] args) throws Exception {

        if (args.length != 1) {
            throw new Exception(
                "ExportPrivateKeyNoPwd: must supply name of a keystore entry");
        }
        String alias = args[0];

        KeyStore ks = KeyStore.getInstance("KeychainStore");
        System.out.println("ExportPrivateKeyNoPwd: loading keychains...");
        ks.load(null, null);

        System.out.println("ExportPrivateKeyNoPwd: exporting key...");
        Key key = ks.getKey(alias, null);
        if (key instanceof PrivateKey) {
            System.out.println("ExportPrivateKeyNoPwd: exported " +
                key.getAlgorithm() + " private key from '" + alias + "'");
        } else {
            throw new Exception("Error exporting private key from keychain");
        }
    }
}

