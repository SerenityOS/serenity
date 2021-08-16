/*
 * Copyright (c) 2002, 2004, Oracle and/or its affiliates. All rights reserved.
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

import java.net.InetAddress;
import java.util.*;
import javax.naming.*;
import javax.naming.directory.*;

/*
 * Tests if a canonical name (CNAME) record exists for a
 * specified host - throws exception if DNS isn't configured
 * or there isn't a CNAME record.
 */

public class CanonicalName {

    public static void main(String args[]) throws Exception {
        final Hashtable<String,String> env = new Hashtable<String,String>();
        env.put("java.naming.factory.initial",
                "com.sun.jndi.dns.DnsContextFactory");
        DirContext ctx = new InitialDirContext(env);

        String ids[] = { "CNAME" };
        Attributes attrs = ctx.getAttributes(args[0], ids);

        NamingEnumeration ne = attrs.getAll();
        if (!ne.hasMoreElements()) {
            throw new Exception("no CNAME record");
        }

        // print out the CNAME records

        while (ne.hasMoreElements()) {
            Attribute attr = (Attribute)ne.next();
            for (NamingEnumeration e = attr.getAll(); e.hasMoreElements();) {
                System.out.println(args[0] + " -> " + e.next());
            }
        }

    }
}
