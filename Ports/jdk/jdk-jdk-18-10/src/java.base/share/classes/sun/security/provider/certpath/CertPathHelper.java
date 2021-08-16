/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.provider.certpath;

import java.util.Date;
import java.util.Set;

import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509CRLSelector;

import sun.security.x509.GeneralNameInterface;

/**
 * Helper class that allows access to JDK specific known-public methods in the
 * java.security.cert package. It relies on a subclass in the
 * java.security.cert packages that is initialized before any of these methods
 * are called (achieved via static initializers).
 *
 * The methods are made available in this fashion for performance reasons.
 *
 * @author Andreas Sterbenz
 */
public abstract class CertPathHelper {

    /**
     * Object used to tunnel the calls. Initialized by CertPathHelperImpl.
     */
    protected static CertPathHelper instance;

    protected CertPathHelper() {
        // empty
    }

    protected abstract void implSetPathToNames(X509CertSelector sel,
            Set<GeneralNameInterface> names);

    protected abstract void implSetDateAndTime(X509CRLSelector sel, Date date, long skew);

    protected abstract boolean implIsJdkCA(TrustAnchor anchor);

    static void setPathToNames(X509CertSelector sel,
            Set<GeneralNameInterface> names) {
        instance.implSetPathToNames(sel, names);
    }

    public static void setDateAndTime(X509CRLSelector sel, Date date, long skew) {
        instance.implSetDateAndTime(sel, date, skew);
    }

    public static boolean isJdkCA(TrustAnchor anchor) {
        return (anchor == null) ? false : instance.implIsJdkCA(anchor);
    }
}
