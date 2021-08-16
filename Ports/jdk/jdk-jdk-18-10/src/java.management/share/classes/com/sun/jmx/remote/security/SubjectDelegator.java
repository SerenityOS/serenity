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

import java.security.AccessController;
import java.security.AccessControlContext;
import java.security.Permission;
import java.security.Principal;
import java.security.PrivilegedAction;
import javax.security.auth.Subject;

import javax.management.remote.SubjectDelegationPermission;

import java.util.*;

public class SubjectDelegator {
    /* Return the AccessControlContext appropriate to execute an
       operation on behalf of the delegatedSubject.  If the
       authenticatedAccessControlContext does not have permission to
       delegate to that subject, throw SecurityException.  */
    @SuppressWarnings("removal")
    public AccessControlContext
        delegatedContext(AccessControlContext authenticatedACC,
                         Subject delegatedSubject,
                         boolean removeCallerContext)
            throws SecurityException {

        if (System.getSecurityManager() != null && authenticatedACC == null) {
            throw new SecurityException("Illegal AccessControlContext: null");
        }

        // Check if the subject delegation permission allows the
        // authenticated subject to assume the identity of each
        // principal in the delegated subject
        //
        Collection<Principal> ps = getSubjectPrincipals(delegatedSubject);
        final Collection<Permission> permissions = new ArrayList<>(ps.size());
        for(Principal p : ps) {
            final String pname = p.getClass().getName() + "." + p.getName();
            permissions.add(new SubjectDelegationPermission(pname));
        }
        PrivilegedAction<Void> action =
            new PrivilegedAction<Void>() {
                public Void run() {
                    for (Permission sdp : permissions) {
                        AccessController.checkPermission(sdp);
                    }
                    return null;
                }
            };
        AccessController.doPrivileged(action, authenticatedACC);

        return getDelegatedAcc(delegatedSubject, removeCallerContext);
    }

    @SuppressWarnings("removal")
    private AccessControlContext getDelegatedAcc(Subject delegatedSubject, boolean removeCallerContext) {
        if (removeCallerContext) {
            return JMXSubjectDomainCombiner.getDomainCombinerContext(delegatedSubject);
        } else {
            return JMXSubjectDomainCombiner.getContext(delegatedSubject);
        }
    }

    /**
     * Check if the connector server creator can assume the identity of each
     * principal in the authenticated subject, i.e. check if the connector
     * server creator codebase contains a subject delegation permission for
     * each principal present in the authenticated subject.
     *
     * @return {@code true} if the connector server creator can delegate to all
     * the authenticated principals in the subject. Otherwise, {@code false}.
     */
    @SuppressWarnings("removal")
    public static synchronized boolean
        checkRemoveCallerContext(Subject subject) {
        try {
            for (Principal p : getSubjectPrincipals(subject)) {
                final String pname =
                    p.getClass().getName() + "." + p.getName();
                final Permission sdp =
                    new SubjectDelegationPermission(pname);
                AccessController.checkPermission(sdp);
            }
        } catch (SecurityException e) {
            return false;
        }
        return true;
    }

    /**
     * Retrieves the {@linkplain Subject} principals
     * @param subject The subject
     * @return If the {@code Subject} is immutable it will return the principals directly.
     *         If the {@code Subject} is mutable it will create an unmodifiable copy.
     */
    private static Collection<Principal> getSubjectPrincipals(Subject subject) {
        if (subject.isReadOnly()) {
            return subject.getPrincipals();
        }

        List<Principal> principals = Arrays.asList(subject.getPrincipals().toArray(new Principal[0]));
        return Collections.unmodifiableList(principals);
    }
}
