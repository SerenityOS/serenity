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

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

/*
 * @test
 * @bug 8200151
 * @summary Tests that we can get the attributes of DNS entries of
 *          nonrecursive data. And recursive data by default or when
 *          com.sun.jndi.dns.recursion is set to true, but not when
 *          com.sun.jndi.dns.recursion is set to false.
 * @library ../lib/
 * @modules java.base/sun.security.util
 * @run main RecursiveTest -Dtestname=RecursiveDefault
 * @run main RecursiveTest -Dtestname=RecursiveFalse
 * @run main RecursiveTest -Dtestname=RecursiveTrue
 */

public class RecursiveTest extends AuthRecursiveBase {

    public static void main(String[] args) throws Exception {
        new RecursiveTest().run(args);
    }

    /*
     * Tests that we can get the attributes of DNS entries of
     * nonrecursive data. And recursive data by default or when
     * com.sun.jndi.dns.recursion is set to true, but not when
     * com.sun.jndi.dns.recursion is set to false.
     */
    @Override
    public void runTest() throws Exception {
        String flag = parseFlagFromTestName();

        if (flag.equalsIgnoreCase("default")) {
            setContext(new InitialDirContext());
        } else {
            Hashtable<Object, Object> env = new Hashtable<>();
            DNSTestUtils.debug("set com.sun.jndi.dns.recursion to " + flag);
            // com.sun.jndi.dns.recursion is set to true or false
            env.put("com.sun.jndi.dns.recursion", flag);
            setContext(new InitialDirContext(env));
        }

        retrieveAndVerifyNonRecursiveData();
        retrieveRecursiveData(
                flag.equalsIgnoreCase("default") || Boolean.parseBoolean(flag));
    }

    private void retrieveAndVerifyNonRecursiveData() throws NamingException {
        // Ensure that nonrecursive data retrieval is OK
        retrieveAndVerifyData(getFqdnUrl());
    }

    /*
     * If isRecur == true, ensure that recursive data retrieval is OK.
     * If isRecur == false, ensure that recursive data retrieval fails.
     */
    private void retrieveRecursiveData(boolean isRecur) throws NamingException {
        Attributes retAttrs = context().getAttributes(getForeignFqdnUrl());
        if (isRecur) {
            DNSTestUtils.verifySchema(retAttrs, new String[] { "CNAME" },
                    new String[] {});
        } else {
            DNSTestUtils.debug(retAttrs);
            if (retAttrs.size() > 0) {
                throw new RuntimeException("Expecting recursive data not found "
                        + getForeignFqdnUrl());
            }
        }
    }
}
