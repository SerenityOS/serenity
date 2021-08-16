/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.SecurityTools;
import jdk.test.lib.security.CertUtils;

import java.security.KeyStore;

/*
 * @test
 * @bug 6415696 6931562 8180570
 * @requires os.family == "windows"
 * @library /test/lib
 * @summary Test "keytool -changealias" using the Microsoft CryptoAPI provider.
 */
public class KeytoolChangeAlias {
    public static void main(String[] args) throws Exception {

        KeyStore ks = KeyStore.getInstance("Windows-MY");
        ks.load(null, null);

        try {
            ks.setCertificateEntry("246810", CertUtils.getCertFromFile("246810.cer"));

            if (ks.containsAlias("13579")) {
                ks.deleteEntry("13579");
            }

            int before = ks.size();

            ks.store(null, null); // no-op, but let's do it before a keytool command

            SecurityTools.keytool("-changealias",
                    "-storetype", "Windows-My",
                    "-alias", "246810",
                    "-destalias", "13579").shouldHaveExitValue(0);

            ks.load(null, null);

            if (ks.size() != before) {
                throw new Exception("error: unexpected number of entries in the "
                        + "Windows-MY store. Before: " + before
                        + ". After: " + ks.size());
            }

            if (!ks.containsAlias("13579")) {
                throw new Exception("error: cannot find the new alias name"
                        + " in the Windows-MY store");
            }
        } finally {
            ks.deleteEntry("13579");
            ks.deleteEntry("246810");
            ks.store(null, null);
        }
    }
}
