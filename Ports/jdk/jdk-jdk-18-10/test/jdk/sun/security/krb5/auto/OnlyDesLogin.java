/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8014310 8194486
 * @summary JAAS/Krb5LoginModule using des encytypes failure with NPE
 *          after JDK-8012679
 * @library /test/lib
 * @compile -XDignore.symbol.file OnlyDesLogin.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts OnlyDesLogin
 */

import sun.security.krb5.Config;

import javax.security.auth.login.LoginException;

public class OnlyDesLogin {

    public static void main(String[] args) throws Exception {

        OneKDC kdc = new OneKDC(null);
        kdc.writeJAASConf();

        KDC.saveConfig(OneKDC.KRB5_CONF, kdc,
                "default_tkt_enctypes=des-cbc-md5",
                "default_tgs_enctypes=des-cbc-md5",
                "permitted_enctypes=des-cbc-md5");
        Config.refresh();

        try {
            Context.fromJAAS("client");
            throw new Exception("What?");
        } catch (LoginException le) {
            // This is OK
        }
    }
}
