/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7201053 8194486
 * @summary Krb5LoginModule shows NPE when both useTicketCache and storeKey
 *          are set to true
 * @library /test/lib
 * @compile -XDignore.symbol.file UseCacheAndStoreKey.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts UseCacheAndStoreKey
 */

import java.io.FileOutputStream;
import javax.security.auth.login.LoginException;

// The basic krb5 test skeleton you can copy from
public class UseCacheAndStoreKey {

    public static void main(String[] args) throws Exception {

        new OneKDC(null).writeJAASConf();

        // KDC would save ccache for client
        System.setProperty("test.kdc.save.ccache", "cache.here");
        try (FileOutputStream fos = new FileOutputStream(OneKDC.JAAS_CONF)) {
            fos.write((
                "me {\n" +
                "    com.sun.security.auth.module.Krb5LoginModule required\n" +
                "    principal=\"" + OneKDC.USER + "\"\n" +
                "    useTicketCache=true\n" +
                "    ticketCache=cache.here\n" +
                "    isInitiator=true\n" +
                "    storeKey=true;\n};\n"
                ).getBytes());
        }

        // The first login will use default callback and succeed
        Context.fromJAAS("me");

        // The second login uses ccache and won't be able to store the keys
        try {
            Context.fromJAAS("me");
            throw new Exception("Should fail");
        } catch (LoginException le) {
            if (le.getMessage().indexOf("NullPointerException") >= 0
                    || le.getCause() instanceof NullPointerException) {
                throw new Exception("NPE");
            }
        }
    }
}
