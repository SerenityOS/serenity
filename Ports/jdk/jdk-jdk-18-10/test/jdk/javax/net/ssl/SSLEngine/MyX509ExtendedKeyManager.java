/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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

import javax.net.ssl.*;
import java.security.*;
import java.net.*;
import java.security.cert.*;

class MyX509ExtendedKeyManager extends X509ExtendedKeyManager {

    X509ExtendedKeyManager akm;

    MyX509ExtendedKeyManager(X509ExtendedKeyManager akm) {
        this.akm = akm;
    }

    public String[] getClientAliases(String keyType, Principal[] issuers) {
        System.out.println("Calling from X509ExtendedKeyManager");
        return akm.getClientAliases(keyType, issuers);
    }

    public String chooseClientAlias(String[] keyType, Principal[] issuers,
            Socket socket) {
        System.out.println("Calling from X509ExtendedKeyManager");
        return akm.chooseClientAlias(keyType, issuers, socket);
    }

    public String[] getServerAliases(String keyType, Principal[] issuers) {
        System.out.println("Calling from X509ExtendedKeyManager");
        return akm.getServerAliases(keyType, issuers);
    }

    public String chooseServerAlias(String keyType, Principal[] issuers,
            Socket socket) {
        System.out.println("Calling from X509ExtendedKeyManager");
        return akm.chooseServerAlias(keyType, issuers, socket);
    }

    public X509Certificate[] getCertificateChain(String alias) {
        System.out.println("Calling from X509ExtendedKeyManager");
        return akm.getCertificateChain(alias);
    }

    public PrivateKey getPrivateKey(String alias) {
        System.out.println("Calling from X509ExtendedKeyManager");
        return akm.getPrivateKey(alias);
    }

    public String chooseEngineClientAlias(String[] keyType,
            Principal[] issuers, SSLEngine engine) {
        System.out.println("Calling from X509ExtendedKeyManager");
        return akm.chooseEngineClientAlias(keyType, issuers, engine);
    }

    public String chooseEngineServerAlias(String keyType,
            Principal[] issuers, SSLEngine engine) {
        System.out.println("Calling from X509ExtendedKeyManager");
        return akm.chooseEngineServerAlias(keyType, issuers, engine);
    }
}
