/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4496785
 * @summary Verify that all ciphersuites work in all configurations
 * @author Andreas Sterbenz
 * @library ../../TLSCommon
 * @run main/othervm/timeout=300 ClientJSSEServerJSSE
 */
import java.security.Security;

public class ClientJSSEServerJSSE {

    public static void main(String[] args) throws Exception {
        // reset security properties to make sure that the algorithms
        // and keys used in this test are not disabled.
        Security.setProperty("jdk.tls.disabledAlgorithms", "");
        Security.setProperty("jdk.certpath.disabledAlgorithms", "");

        CipherTest.main(new JSSEFactory(), args);
    }

    private static class JSSEFactory extends CipherTest.PeerFactory {

        String getName() {
            return "Client JSSE - Server JSSE";
        }

        CipherTest.Client newClient(CipherTest cipherTest) throws Exception {
            return new JSSEClient(cipherTest);
        }

        CipherTest.Server newServer(CipherTest cipherTest) throws Exception {
            return new JSSEServer(cipherTest);
        }
    }
}
