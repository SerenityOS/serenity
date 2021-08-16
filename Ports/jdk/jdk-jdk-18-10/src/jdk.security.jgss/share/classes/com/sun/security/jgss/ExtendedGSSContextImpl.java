/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

import org.ietf.jgss.*;
import sun.security.jgss.GSSContextImpl;
import sun.security.krb5.internal.AuthorizationData;

// The impl is almost identical to GSSContextImpl with only 2 differences:
// 1. It implements the extended interface
// 2. It translates result to data types here in inquireSecContext
class ExtendedGSSContextImpl extends GSSContextImpl
        implements ExtendedGSSContext {

    public ExtendedGSSContextImpl(GSSContextImpl old) {
        super(old);
    }

    @Override
    public Object inquireSecContext(InquireType type) throws GSSException {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(
                    new InquireSecContextPermission(type.toString()));
        }
        Object output = super.inquireSecContext(type.name());
        if (output != null) {
            if (type == InquireType.KRB5_GET_AUTHZ_DATA) {
                AuthorizationData ad = (AuthorizationData) output;
                AuthorizationDataEntry[] authzData =
                        new AuthorizationDataEntry[ad.count()];
                for (int i = 0; i < ad.count(); i++) {
                    authzData[i] = new AuthorizationDataEntry(
                            ad.item(i).adType, ad.item(i).adData);
                }
                output = authzData;
            }
        }
        return output;
    }
}
