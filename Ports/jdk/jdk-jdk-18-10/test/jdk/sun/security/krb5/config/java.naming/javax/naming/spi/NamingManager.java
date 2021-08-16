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

package javax.naming.spi;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.InitialDirContext;

/**
 * A fake javax.naming.spi.NamingManager. It allows reading a DNS
 * record without contacting a real server.
 *
 * See DNS.java and dns.sh.
 */
public class NamingManager {
    NamingManager() {}
    public static Context getURLContext(
            String scheme, Hashtable<?,?> environment)
            throws NamingException {
        return new InitialDirContext() {
            public Attributes getAttributes(String name, String[] attrIds)
                    throws NamingException {
                return new BasicAttributes() {
                    public Attribute get(String attrID) {
                        BasicAttribute ba  = new BasicAttribute(attrID);
                        ba.add("1 1 99 b.com.");
                        ba.add("0 0 88 a.com.");    // 2nd has higher priority
                        return ba;
                    }
                };
            }
        };
    }
}
