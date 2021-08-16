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

/*
 * @test
 * @bug 8208279
 * @summary Tests that when we remove the authoritative property from a context,
 *          that it affects subsequent context operations.
 * @library ../lib/
 * @modules java.base/sun.security.util
 * @run main RemoveOps
 */

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;

public class RemoveOps extends EnvTestBase {

    public static void main(String[] args) throws Exception {
        new RemoveOps().run(args);
    }

    /*
     * Tests that when we remove the authoritative property from a context,
     * that it affects subsequent context operations.
     */
    @Override public void runTest() throws Exception {
        initContext();

        // Context.AUTHORITATIVE been set in initContext(), should not be null
        Object val = context().removeFromEnvironment(Context.AUTHORITATIVE);

        if (!"true".equals(val)) {
            throw new RuntimeException(
                    "Failed: unexpected return value for removeFromEnvironment: "
                            + val);
        }

        retrieveAndVerifyAuthData();
        retrieveNonAuthData();
    }

    private void initContext() throws NamingException {
        // Make authoritative
        env().put(Context.AUTHORITATIVE, "true");
        setContext(new InitialDirContext(env()));
    }

    private void retrieveAndVerifyAuthData() throws NamingException {
        // Ensure that auth data retrieval is OK
        retrieveAndVerifyData(getFqdnUrl(), new String[] { "*" });
    }

    private void retrieveNonAuthData() throws NamingException {
        try {
            // Ensure that nonauth data retrieval succeeds
            Attributes retAttrs = context()
                    .getAttributes(getForeignFqdnUrl(), new String[] { "*" });
            DNSTestUtils.debug(context().getEnvironment());
            DNSTestUtils.debug(retAttrs);

        } catch (NameNotFoundException e) {
            throw new RuntimeException(
                    "Failed: Expecting nonauthoritative entry found: "
                            + getForeignFqdnUrl(), e);
        }
    }
}
