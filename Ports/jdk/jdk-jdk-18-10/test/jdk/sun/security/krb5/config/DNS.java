/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8002344
 * @summary Krb5LoginModule config class does not return proper KDC list from DNS
 * @modules java.security.jgss/sun.security.krb5
 * @build java.naming/javax.naming.spi.NamingManager
 * @run main/othervm DNS
 */
import sun.security.krb5.Config;
import sun.security.krb5.KrbException;

public class DNS {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.security.krb5.conf",
                System.getProperty("test.src", ".") +"/no-such-file.conf");
        Config config = Config.getInstance();
        try {
            String r = config.getDefaultRealm();
            throw new Exception("What? There is a default realm " + r + "?");
        } catch (KrbException ke) {
            ke.printStackTrace();
            if (ke.getCause() != null) {
                throw new Exception("There should be no cause. Won't try DNS");
            }
        }
        String kdcs = config.getKDCList("X");
        if (!kdcs.equals("a.com.:88 b.com.:99") &&
                !kdcs.equals("a.com. b.com.:99")) {
            throw new Exception("Strange KDC: [" + kdcs + "]");
        };
    }
}
