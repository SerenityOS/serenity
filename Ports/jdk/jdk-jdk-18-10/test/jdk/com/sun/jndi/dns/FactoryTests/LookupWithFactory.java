/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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
import javax.naming.directory.InitialDirContext;

/*
 * @test
 * @bug 8208483
 * @summary Tests that we can look up a DNS node that contains TXT records
 *          and use an ObjectFactory to return an object from it.
 *          Since an ObjectFactory doesn't have access to attributes, the
 *          factory must looking the attributes.
 * @library ../lib/
 * @modules java.base/sun.security.util
 * @build TestDnsObject TxtFactory
 * @run main/othervm LookupWithFactory
 */

public class LookupWithFactory extends LookupFactoryBase {

    public static void main(String[] args) throws Exception {
        new LookupWithFactory().run(args);
    }

    /*
     * Tests that we can look up a DNS node that contains TXT records
     * and use an ObjectFactory to return an object from it.
     * Since an ObjectFactory doesn't have access to attributes, the
     * factory must looking the attributes.
     */
    @Override
    public void runTest() throws Exception {
        // initial context with object factory
        env().put(Context.OBJECT_FACTORIES, "TxtFactory");
        setContext(new InitialDirContext(env()));

        Object obj = context().lookup(getKey());
        verifyLookupObject(obj);
    }
}
