/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.rmi.dgc;

import java.rmi.server.UID;
import java.security.SecureRandom;

/**
 * A VMID is a identifier that is unique across all Java virtual
 * machines.  VMIDs are used by the distributed garbage collector
 * to identify client VMs.
 *
 * @author      Ann Wollrath
 * @author      Peter Jones
 */
public final class VMID implements java.io.Serializable {
    /** Array of bytes uniquely identifying this host */
    private static final byte[] randomBytes;

    /**
     * @serial array of bytes uniquely identifying host created on
     */
    private byte[] addr;

    /**
     * @serial unique identifier with respect to host created on
     */
    private UID uid;

    /** indicate compatibility with JDK 1.1.x version of class */
    private static final long serialVersionUID = -538642295484486218L;

    static {
        // Generate 8 bytes of random data.
        SecureRandom secureRandom = new SecureRandom();
        byte bytes[] = new byte[8];
        secureRandom.nextBytes(bytes);
        randomBytes = bytes;
    }

    /**
     * Create a new VMID.  Each new VMID returned from this constructor
     * is unique for all Java virtual machines under the following
     * conditions: a) the conditions for uniqueness for objects of
     * the class <code>java.rmi.server.UID</code> are satisfied, and b) an
     * address can be obtained for this host that is unique and constant
     * for the lifetime of this object.
     */
    public VMID() {
        addr = randomBytes;
        uid = new UID();
    }

    /**
     * Return true if an accurate address can be determined for this
     * host.  If false, reliable VMID cannot be generated from this host
     * @return true if host address can be determined, false otherwise
     * @deprecated
     */
    @Deprecated
    public static boolean isUnique() {
        return true;
    }

    /**
     * Compute hash code for this VMID.
     */
    public int hashCode() {
        return uid.hashCode();
    }

    /**
     * Compare this VMID to another, and return true if they are the
     * same identifier.
     */
    public boolean equals(Object obj) {
        if (obj instanceof VMID) {
            VMID vmid = (VMID) obj;
            if (!uid.equals(vmid.uid))
                return false;
            if ((addr == null) ^ (vmid.addr == null))
                return false;
            if (addr != null) {
                if (addr.length != vmid.addr.length)
                    return false;
                for (int i = 0; i < addr.length; ++ i)
                    if (addr[i] != vmid.addr[i])
                        return false;
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Return string representation of this VMID.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (addr != null)
            for (int i = 0; i < addr.length; ++ i) {
                int x = addr[i] & 0xFF;
                sb.append((x < 0x10 ? "0" : "") +
                          Integer.toString(x, 16));
            }
        sb.append(':');
        sb.append(uid.toString());
        return sb.toString();
    }
}
