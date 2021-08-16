/*
 * Copyright (c) 1998, 2018, Oracle and/or its affiliates. All rights reserved.
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
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

/*
 * @test
 * @bug 8210339
 * @summary Look up of a leaf using a composite name with DNS name as
 *          first component
 * @library ../lib/ ../FactoryTests/
 * @modules java.naming/com.sun.jndi.toolkit.dir
 *          java.base/sun.security.util
 * @build FedSubordinateNs FedObjectFactory
 * @run main/othervm LookupSubLeaf
 */

public class LookupSubLeaf extends LookupFactoryBase {

    // pre defined attribute value for '/a/b/c'
    public static final String ATTRIBUTE_VALUE = "c";

    public static void main(String[] args) throws Exception {
        new LookupSubLeaf().run(args);
    }

    /*
     * Look up of a leaf using a composite name with DNS name as
     * first component
     */
    @Override
    public void runTest() throws Exception {
        // initial context with object factory
        env().put(Context.OBJECT_FACTORIES, "FedObjectFactory");
        setContext(new InitialDirContext(env()));

        Object obj = context().lookup(getKey() + "/a/b/c");

        verifyLookupObject(obj, DirContext.class);

        Attributes attrs = ((DirContext) obj).getAttributes("");
        Attribute attr = attrs.get("name");

        DNSTestUtils.debug(getKey() + "/a/b/c is: " + attrs);
        verifyAttribute(attr);
    }

    private void verifyAttribute(Attribute attr) throws NamingException {
        if (attr == null || !ATTRIBUTE_VALUE.equals(attr.get())) {
            throw new RuntimeException(
                    "Expecting attribute value: " + ATTRIBUTE_VALUE
                            + ", but actual: " + (attr != null ?
                            attr.get() :
                            attr));
        }
    }
}
