/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Tests that when we remove environment properties from a context,
 *          its changes are reflected in the environment properties of any
 *          child context derived from the context.
 * @library ../lib/
 * @modules java.base/sun.security.util
 * @run main RemoveInherited
 */

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

public class RemoveInherited extends EnvTestBase {

    private static final String IRRELEVANT_PROPERTY_NAME = "some.irrelevant.property";
    private static final String IRRELEVANT_PROPERTY_VALUE = "somevalue";
    private static final String DNS_RECURSION_PROPERTY_NAME = "com.sun.jndi.dns.recursion";
    private static final String DNS_RECURSION_PROPERTY_VALUE = "false";

    public static void main(String[] args) throws Exception {
        new RemoveInherited().run(args);
    }

    /*
     * Tests that when we remove environment properties from a context,
     * its changes are reflected in the environment properties of any
     * child context derived from the context.
     */
    @Override public void runTest() throws Exception {
        initContext();

        // some.irrelevant.property been set in initContext(), should not be null
        Object val = context().removeFromEnvironment(IRRELEVANT_PROPERTY_NAME);

        if (!IRRELEVANT_PROPERTY_VALUE.equals(val)) {
            throw new RuntimeException(
                    "Failed: unexpected return value for removeFromEnvironment: "
                            + val);
        }

        Context child = (Context) context().lookup(getKey());

        Hashtable<?,?> envParent = context().getEnvironment();
        Hashtable<?,?> envChild = child.getEnvironment();

        DNSTestUtils.debug(child);
        DNSTestUtils.debug("Parent env: " + envParent);
        DNSTestUtils.debug("Child env: " + envChild);

        verifyEnvProperty(envParent, DNS_RECURSION_PROPERTY_NAME,
                DNS_RECURSION_PROPERTY_VALUE);
        verifyEnvProperty(envParent, IRRELEVANT_PROPERTY_NAME, null);
        verifyEnvProperty(envChild, DNS_RECURSION_PROPERTY_NAME,
                DNS_RECURSION_PROPERTY_VALUE);
        verifyEnvProperty(envChild, IRRELEVANT_PROPERTY_NAME, null);
    }

    private void initContext() throws NamingException {
        env().put(DNS_RECURSION_PROPERTY_NAME, DNS_RECURSION_PROPERTY_VALUE);
        env().put(IRRELEVANT_PROPERTY_NAME, IRRELEVANT_PROPERTY_VALUE);
        setContext(new InitialDirContext(env()));
    }
}
