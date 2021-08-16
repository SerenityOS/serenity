/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

/*
 * @test
 * @bug 8200151
 * @summary Tests that we can get the attributes of DNS entries for
 *          authoritative data. And nonauthoritative data by default or
 *          java.naming.authoritative is set to false, but cannot when
 *          java.naming.authoritative is set to true.
 * @library ../lib/
 * @modules java.base/sun.security.util
 * @run main AuthTest -Dtestname=AuthDefault
 * @run main AuthTest -Dtestname=AuthFalse
 * @run main AuthTest -Dtestname=AuthTrue
 */

public class AuthTest extends AuthRecursiveBase {

    public static void main(String[] args) throws Exception {
        new AuthTest().run(args);
    }

    /*
     * Tests that we can get the attributes of DNS entries for
     * authoritative data. And nonauthoritative data by default or
     * java.naming.authoritative is set to false, but cannot when
     * java.naming.authoritative is set to true.
     */
    @Override
    public void runTest() throws Exception {
        String flag = parseFlagFromTestName();

        if (flag.equalsIgnoreCase("default")) {
            setContext(new InitialDirContext());
        } else {
            Hashtable<Object, Object> env = new Hashtable<>();
            DNSTestUtils.debug("set java.naming.authoritative to " + flag);
            // java.naming.authoritative is set to true or false
            env.put(Context.AUTHORITATIVE, flag);
            setContext(new InitialDirContext(env));
        }

        retrieveAndVerifyAuthData();
        retrieveNonAuthData(Boolean.parseBoolean(flag));
    }

    private void retrieveAndVerifyAuthData() throws NamingException {
        // Ensure that auth data retrieval is OK
        retrieveAndVerifyData(getFqdnUrl(), new String[] { "*" });
    }

    /*
     * If isAuth == true, ensure that nonauth data retrieval cannot be retrieved.
     * If isAuth == false, ensure that nonauth data retrieval is OK, skip
     * checking attributes for foreign; just successful operation is sufficient.
     */
    private void retrieveNonAuthData(boolean isAuth) throws NamingException {
        try {
            Attributes retAttrs = context()
                    .getAttributes(getForeignFqdnUrl(), new String[] { "*" });
            DNSTestUtils.debug(retAttrs);
            if (isAuth) {
                throw new RuntimeException(
                        "Failed: Expecting nonauth entry not found "
                                + getForeignFqdnUrl());
            }
        } catch (NameNotFoundException e) {
            if (isAuth) {
                System.out.println("Got expected exception: " + e);
            } else {
                throw e;
            }
        }
    }
}
