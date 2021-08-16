/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.x509;

import java.io.IOException;

import sun.security.util.*;

/**
 * @author      Ram Marti
 */

public final class AccessDescription {

    private int myhash = -1;

    private ObjectIdentifier accessMethod;

    private GeneralName accessLocation;

    public static final ObjectIdentifier Ad_OCSP_Id =
        ObjectIdentifier.of(KnownOIDs.OCSP);

    public static final ObjectIdentifier Ad_CAISSUERS_Id =
        ObjectIdentifier.of(KnownOIDs.caIssuers);

    public static final ObjectIdentifier Ad_TIMESTAMPING_Id =
        ObjectIdentifier.of(KnownOIDs.AD_TimeStamping);

    public static final ObjectIdentifier Ad_CAREPOSITORY_Id =
        ObjectIdentifier.of(KnownOIDs.caRepository);

    public AccessDescription(ObjectIdentifier accessMethod, GeneralName accessLocation) {
        this.accessMethod = accessMethod;
        this.accessLocation = accessLocation;
    }

    public AccessDescription(DerValue derValue) throws IOException {
        DerInputStream derIn = derValue.getData();
        accessMethod = derIn.getOID();
        accessLocation = new GeneralName(derIn.getDerValue());
    }

    public ObjectIdentifier getAccessMethod() {
        return accessMethod;
    }

    public GeneralName getAccessLocation() {
        return accessLocation;
    }

    public void encode(DerOutputStream out) throws IOException {
        DerOutputStream tmp = new DerOutputStream();
        tmp.putOID(accessMethod);
        accessLocation.encode(tmp);
        out.write(DerValue.tag_Sequence, tmp);
    }

    public int hashCode() {
        if (myhash == -1) {
            myhash = accessMethod.hashCode() + accessLocation.hashCode();
        }
        return myhash;
    }

    public boolean equals(Object obj) {
        if (obj == null || (!(obj instanceof AccessDescription))) {
            return false;
        }
        AccessDescription that = (AccessDescription)obj;

        if (this == that) {
            return true;
        }
        return (accessMethod.equals(that.getAccessMethod()) &&
            accessLocation.equals(that.getAccessLocation()));
    }

    public String toString() {
        String method = null;
        if (accessMethod.equals(Ad_CAISSUERS_Id)) {
            method = "caIssuers";
        } else if (accessMethod.equals(Ad_CAREPOSITORY_Id)) {
            method = "caRepository";
        } else if (accessMethod.equals(Ad_TIMESTAMPING_Id)) {
            method = "timeStamping";
        } else if (accessMethod.equals(Ad_OCSP_Id)) {
            method = "ocsp";
        } else {
            method = accessMethod.toString();
        }
        return ("\n   accessMethod: " + method +
                "\n   accessLocation: " + accessLocation.toString() + "\n");
    }
}
