/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * This is a dumy URL context factory for 'ldapv4://'.
 */

package org.example.ldapv4;

import java.util.*;
import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.spi.*;

public class ldapv4URLContextFactory implements ObjectFactory {

    public ldapv4URLContextFactory() {
    }

    public Object getObjectInstance(Object urlInfo, Name name, Context nameCtx,
            Hashtable<?,?> env) throws Exception {

        Hashtable<String,String> env2 = new Hashtable<>();
        env2.put(Context.INITIAL_CONTEXT_FACTORY,
            "com.sun.jndi.ldap.LdapCtxFactory");
        String ldapUrl = (String)env.get(Context.PROVIDER_URL);
        env2.put(Context.PROVIDER_URL, ldapUrl.replaceFirst("ldapv4", "ldap"));
        //env2.put("com.sun.jndi.ldap.trace.ber", System.out);
        return new ldapv4URLContext(env2);
    }
}
