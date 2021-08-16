/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.security.jgss;

import javax.security.auth.Subject;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.GSSCredential;

/**
 * GSS-API Utilities for using in conjunction with Sun Microsystem's
 * implementation of Java GSS-API.
 */
public class GSSUtil {
    /**
     * Do not call.
     */
    private GSSUtil() {}

    /**
     * Use this method to convert a GSSName and GSSCredential into a
     * Subject. Typically this would be done by a server that wants to
     * impersonate a client thread at the Java level by setting a client
     * Subject in the current access control context. If the server is merely
     * interested in using a principal based policy in its local JVM, then
     * it only needs to provide the GSSName of the client.
     *
     * The elements from the GSSName are placed in the principals set of this
     * Subject and those from the GSSCredential are placed in the private
     * credentials set of the Subject. Any Kerberos specific elements that
     * are added to the subject will be instances of the standard Kerberos
     * implementation classes defined in javax.security.auth.kerberos.
     *
     * @return a Subject with the entries that contain elements from the
     * given GSSName and GSSCredential.
     *
     * @param principals a GSSName containing one or more mechanism specific
     * representations of the same entity. These mechanism specific
     * representations will be populated in the returned Subject's principal
     * set.
     *
     * @param credentials a GSSCredential containing one or more mechanism
     * specific credentials for the same entity. These mechanism specific
     * credentials will be populated in the returned Subject's private
     * credential set. Passing in a value of null will imply that the private
     * credential set should be left empty.
     */
    public static Subject createSubject(GSSName principals,
                                     GSSCredential credentials) {

        return  sun.security.jgss.GSSUtil.getSubject(principals,
                                                     credentials);
    }
}
