/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7158329
 * @bug 8001208
 * @summary NPE in sun.security.krb5.Credentials.acquireDefaultCreds()
 * @library /test/lib
 * @modules java.security.jgss/sun.security.krb5
 *          java.security.jgss/sun.security.krb5.internal.ccache
 * @compile -XDignore.symbol.file EmptyCC.java
 * @run main EmptyCC tmpcc
 * @run main EmptyCC FILE:tmpcc
 */
import java.io.File;

import jdk.test.lib.process.Proc;
import sun.security.krb5.Credentials;
import sun.security.krb5.PrincipalName;
import sun.security.krb5.internal.ccache.CredentialsCache;

public class EmptyCC {
    public static void main(String[] args) throws Exception {
        final PrincipalName pn = new PrincipalName("dummy@FOO.COM");
        final String ccache = args[0];

        if (args.length == 1) {
            // Main process, write the ccache and launch sub process
            CredentialsCache cache = CredentialsCache.create(pn, ccache);
            cache.save();
            Proc p = Proc.create("EmptyCC").args(ccache, "readcc")
                    .env("KRB5CCNAME", ccache).start();
            p.waitFor();
        } else {
            // Sub process, read the ccache
            String cc = System.getenv("KRB5CCNAME");
            if (!cc.equals(ccache)) {
                throw new Exception("env not set correctly");
            }
            // 8001208: Fix for KRB5CCNAME not complete
            // Make sure the ccache is created with bare file name
            if (CredentialsCache.getInstance() == null) {
                throw new Exception("Cache not instantiated");
            }
            if (!new File("tmpcc").exists()) {
                throw new Exception("File not found");
            }
            Credentials.acquireTGTFromCache(pn, null);
        }
    }
}
