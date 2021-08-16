/*
 * Copyright (c) 1999, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.jndi.ldap;

import javax.naming.*;
import javax.naming.directory.*;

import com.sun.jndi.toolkit.ctx.Continuation;
import java.util.Vector;
import javax.naming.ldap.Control;


final class LdapNamingEnumeration
        extends AbstractLdapNamingEnumeration<NameClassPair> {

    private static final String defaultClassName = DirContext.class.getName();

    LdapNamingEnumeration(LdapCtx homeCtx, LdapResult answer, Name listArg,
                                 Continuation cont) throws NamingException {
        super(homeCtx, answer, listArg, cont);
    }

    @Override
    protected NameClassPair createItem(String dn, Attributes attrs,
            Vector<Control> respCtls) throws NamingException {

        Attribute attr;
        String className = null;

        // use the Java classname if present
        if ((attr = attrs.get(Obj.JAVA_ATTRIBUTES[Obj.CLASSNAME])) != null) {
            className = (String)attr.get();
        } else {
            className = defaultClassName;
        }
        CompositeName cn = new CompositeName();
        cn.add(getAtom(dn));

        NameClassPair ncp;
        if (respCtls != null) {
            ncp = new NameClassPairWithControls(
                        cn.toString(), className,
                        homeCtx.convertControls(respCtls));
        } else {
            ncp = new NameClassPair(cn.toString(), className);
        }
        ncp.setNameInNamespace(dn);
        return ncp;
    }

    @Override
    protected AbstractLdapNamingEnumeration<? extends NameClassPair> getReferredResults(
            LdapReferralContext refCtx) throws NamingException {
        // repeat the original operation at the new context
        return (AbstractLdapNamingEnumeration<? extends NameClassPair>)refCtx.list(listArg);
    }
}
