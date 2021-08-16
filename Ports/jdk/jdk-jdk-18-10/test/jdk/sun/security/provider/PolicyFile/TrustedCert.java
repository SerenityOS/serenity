/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4409615
 * @summary KeyStore alias principal grant fails for trusted certificate entry
 *
 * this should always work:
 *              main/othervm/policy=TrustedCert.policy \
 *              -Dkeystore=TrustedCert.keystore1 -Dfoo=bar TrustedCert
 *
 * @run main/othervm/policy=TrustedCert.policy -Dkeystore=TrustedCert.keystore1 -Dfoo=bar TrustedCert
 */

import java.security.PrivilegedAction;
import java.util.Collections;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;

public class TrustedCert {

    public static void main(String[] args) {
        System.out.println(
            Subject.doAsPrivileged(
                new Subject(true,
                            Collections.singleton(new X500Principal("CN=Tim")),
                            Collections.EMPTY_SET,
                            Collections.EMPTY_SET),
                new PrivilegedAction() {
                    public Object run() {
                        return System.getProperty("foo");
                    }
                },
                null));
    }
}
