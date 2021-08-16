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
 * @summary Tests that when we add environment properties to a context, its
 *          changes are reflected in the environment properties of any child
 *          context derived from the context.
 * @library ../lib/
 * @modules java.base/sun.security.util
 * @run main AddInherited
 */

import javax.naming.Context;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

public class AddInherited extends EnvTestBase {

    public static void main(String[] args) throws Exception {
        new AddInherited().run(args);
    }

    /*
     * Tests that when we add environment properties to a context, its
     * changes are reflected in the environment properties of any child
     * context derived from the context.
     */
    @Override public void runTest() throws Exception {
        setContext(new InitialDirContext(env()));

        addToEnvAndVerifyOldValIsNull("com.sun.jndi.dns.recursion", "false");
        addToEnvAndVerifyOldValIsNull("some.irrelevant.property", "somevalue");

        Context child = (Context) context().lookup(getKey());

        Hashtable<?,?> envParent = context().getEnvironment();
        Hashtable<?,?> envChild = child.getEnvironment();

        DNSTestUtils.debug(child);
        DNSTestUtils.debug("Parent env: " + envParent);
        DNSTestUtils.debug("Child env: " + envChild);

        verifyEnvProperty(envParent, "com.sun.jndi.dns.recursion", "false");
        verifyEnvProperty(envParent, "some.irrelevant.property", "somevalue");
        verifyEnvProperty(envChild, "com.sun.jndi.dns.recursion", "false");
        verifyEnvProperty(envChild, "some.irrelevant.property", "somevalue");
    }
}
