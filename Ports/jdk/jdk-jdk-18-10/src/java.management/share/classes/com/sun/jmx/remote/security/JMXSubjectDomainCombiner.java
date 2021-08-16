/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jmx.remote.security;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.ProtectionDomain;
import javax.security.auth.Subject;
import javax.security.auth.SubjectDomainCombiner;

/**
 * <p>This class represents an extension to the {@link SubjectDomainCombiner}
 * and is used to add a new {@link ProtectionDomain}, comprised of a null
 * codesource/signers and an empty permission set, to the access control
 * context with which this combiner is combined.</p>
 *
 * <p>When the {@link #combine} method is called the {@link ProtectionDomain}
 * is augmented with the permissions granted to the set of principals present
 * in the supplied {@link Subject}.</p>
 */
@SuppressWarnings("removal")
public class JMXSubjectDomainCombiner extends SubjectDomainCombiner {

    public JMXSubjectDomainCombiner(Subject s) {
        super(s);
    }

    public ProtectionDomain[] combine(ProtectionDomain[] current,
                                      ProtectionDomain[] assigned) {
        // Add a new ProtectionDomain with the null codesource/signers, and
        // the empty permission set, to the end of the array containing the
        // 'current' protections domains, i.e. the ones that will be augmented
        // with the permissions granted to the set of principals present in
        // the supplied subject.
        //
        ProtectionDomain[] newCurrent;
        if (current == null || current.length == 0) {
            newCurrent = new ProtectionDomain[1];
            newCurrent[0] = pdNoPerms;
        } else {
            newCurrent = new ProtectionDomain[current.length + 1];
            for (int i = 0; i < current.length; i++) {
                newCurrent[i] = current[i];
            }
            newCurrent[current.length] = pdNoPerms;
        }
        return super.combine(newCurrent, assigned);
    }

    /**
     * A null CodeSource.
     */
    private static final CodeSource nullCodeSource =
        new CodeSource(null, (java.security.cert.Certificate[]) null);

    /**
     * A ProtectionDomain with a null CodeSource and an empty permission set.
     */
    private static final ProtectionDomain pdNoPerms =
        new ProtectionDomain(nullCodeSource, new Permissions(), null, null);

    /**
     * Get the current AccessControlContext combined with the supplied subject.
     */
    public static AccessControlContext getContext(Subject subject) {
        return new AccessControlContext(AccessController.getContext(),
                                        new JMXSubjectDomainCombiner(subject));
    }

    /**
     * Get the AccessControlContext of the domain combiner created with
     * the supplied subject, i.e. an AccessControlContext with the domain
     * combiner created with the supplied subject and where the caller's
     * context has been removed.
     */
    public static AccessControlContext
        getDomainCombinerContext(Subject subject) {
        return new AccessControlContext(
            new AccessControlContext(new ProtectionDomain[0]),
            new JMXSubjectDomainCombiner(subject));
    }
}
