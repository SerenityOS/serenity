/*
 * Copyright (c) 2002, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Locale;
import java.util.Arrays; // JDK 1.2
import java.io.OutputStream;
import javax.naming.ldap.Control;
import java.lang.reflect.Method;
import javax.net.SocketFactory;

/**
 * Represents identity information about an anonymous LDAP connection.
 * This base class contains the following information:
 * - protocol version number
 * - server's hostname (case-insensitive)
 * - server's port number
 * - prototype type (plain or ssl)
 * - controls to be sent with the LDAP bind request
 *
 * All other identity classes must be a subclass of ClientId.
 * Identity subclasses would add more distinguishing information, depending
 * on the type of authentication that the connection is to have.
 *
 * The equals() and hashCode() methods of this class and its subclasses are
 * important because they are used to determine whether two requests for
 * the same connection are identical, and thus whether the same connection
 * may be shared. This is especially important for authenticated connections
 * because a mistake would result in a serious security violation.
 *
 * @author Rosanna Lee
 */
class ClientId {
    private final int version;
    private final String hostname;
    private final int port;
    private final String protocol;
    private final Control[] bindCtls;
    private final OutputStream trace;
    private final String socketFactory;
    private final int myHash;
    private final int ctlHash;

    private SocketFactory factory = null;
    private Method sockComparator = null;
    private boolean isDefaultSockFactory = false;
    public static final boolean debug = false;

    ClientId(int version, String hostname, int port, String protocol,
            Control[] bindCtls, OutputStream trace, String socketFactory) {
        this.version = version;
        this.hostname = hostname.toLowerCase(Locale.ENGLISH);  // ignore case
        this.port = port;
        this.protocol = protocol;
        this.bindCtls = (bindCtls != null ? bindCtls.clone() : null);
        this.trace = trace;
        //
        // Needed for custom socket factory pooling
        //
        this.socketFactory = socketFactory;
        if ((socketFactory != null) &&
             !socketFactory.equals(LdapCtx.DEFAULT_SSL_FACTORY)) {
            try {
                Class<?> socketFactoryClass =
                        Obj.helper.loadClass(socketFactory);
                this.sockComparator = socketFactoryClass.getMethod(
                                "compare", new Class<?>[]{Object.class, Object.class});
                Method getDefault = socketFactoryClass.getMethod(
                                            "getDefault", new Class<?>[]{});
                this.factory =
                        (SocketFactory)getDefault.invoke(null, new Object[]{});
            } catch (Exception e) {
                // Ignore it here, the same exceptions are/will be handled by
                // LdapPoolManager and Connection classes.
                if (debug) {
                    System.out.println("ClientId received an exception");
                    e.printStackTrace();
                }
            }
        } else {
             isDefaultSockFactory = true;
        }

        // The SocketFactory field is not used in the myHash
        // computation as there is no right way to compute the hash code
        // for this field. There is no harm in skipping it from the hash
        // computation
        myHash = version + port
            + (trace != null ? trace.hashCode() : 0)
            + (this.hostname != null ? this.hostname.hashCode() : 0)
            + (protocol != null ? protocol.hashCode() : 0)
            + (ctlHash=hashCodeControls(bindCtls));
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ClientId)) {
            return false;
        }

        ClientId other = (ClientId)obj;

        return myHash == other.myHash
            && version == other.version
            && port == other.port
            && trace == other.trace
            && (hostname == other.hostname // null OK
                || (hostname != null && hostname.equals(other.hostname)))
            && (protocol == other.protocol // null OK
                || (protocol != null && protocol.equals(other.protocol)))
            && ctlHash == other.ctlHash
            && (equalsControls(bindCtls, other.bindCtls))
            && (equalsSockFactory(other));
    }

    public int hashCode() {
        return myHash;
    }

    private static int hashCodeControls(Control[] c) {
        if (c == null) {
            return 0;
        }

        int code = 0;
        for (int i = 0; i < c.length; i++) {
            code = code * 31 + c[i].getID().hashCode();
        }
        return code;
    }

    private static boolean equalsControls(Control[] a, Control[] b) {
        if (a == b) {
            return true;  // both null or same
        }
        if (a == null || b == null) {
            return false; // one is non-null
        }
        if (a.length != b.length) {
            return false;
        }

        for (int i = 0; i < a.length; i++) {
            if (!a[i].getID().equals(b[i].getID())
                || a[i].isCritical() != b[i].isCritical()
                || !Arrays.equals(a[i].getEncodedValue(),
                    b[i].getEncodedValue())) {
                return false;
            }
        }
        return true;
    }

    private boolean equalsSockFactory(ClientId other) {
        if (this.isDefaultSockFactory && other.isDefaultSockFactory) {
            return true;
        }
        else if (!other.isDefaultSockFactory) {
             return invokeComparator(other, this);
        } else {
             return invokeComparator(this, other);
        }
    }

    // delegate the comparison work to the SocketFactory class
    // as there is no enough information here, to do the comparison
    private boolean invokeComparator(ClientId c1, ClientId c2) {
        Object ret;
        try {
            ret = (c1.sockComparator).invoke(
                        c1.factory, c1.socketFactory, c2.socketFactory);
        } catch(Exception e) {
            if (debug) {
                System.out.println("ClientId received an exception");
                e.printStackTrace();
            }
            // Failed to invoke the comparator; flag inequality
            return false;
        }
        if (((Integer) ret) == 0) {
            return true;
        }
        return false;
    }

    private static String toStringControls(Control[] ctls) {
        if (ctls == null) {
            return "";
        }
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < ctls.length; i++) {
            str.append(ctls[i].getID());
            str.append(' ');
        }
        return str.toString();
    }

    public String toString() {
        return (hostname + ":" + port + ":" +
            (protocol != null ? protocol : "") + ":" +
            toStringControls(bindCtls) + ":" +
            socketFactory);
    }
}
