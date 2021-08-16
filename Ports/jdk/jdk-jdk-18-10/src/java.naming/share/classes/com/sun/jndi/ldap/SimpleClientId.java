/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;  // JDK1.2
import java.io.OutputStream;
import javax.naming.ldap.Control;

/**
 * Represents the identity of a 'simple' authenticated LDAP connection.
 * In addition to ClientId information, this class contains also the
 * username and password.
 *
 * @author Rosanna Lee
 */
class SimpleClientId extends ClientId {
    private final String username;
    private final Object passwd;
    private final int myHash;

    SimpleClientId(int version, String hostname, int port,
        String protocol, Control[] bindCtls, OutputStream trace,
        String socketFactory, String username, Object passwd) {

        super(version, hostname, port, protocol, bindCtls, trace,
                socketFactory);

        this.username = username;
        int pwdHashCode = 0;
        if (passwd == null) {
            this.passwd = null;
        } else if (passwd instanceof byte[]) {
            this.passwd = ((byte[])passwd).clone();
            pwdHashCode = Arrays.hashCode((byte[])passwd);
        } else if (passwd instanceof char[]) {
            this.passwd = ((char[])passwd).clone();
            pwdHashCode = Arrays.hashCode((char[])passwd);
        } else {
            this.passwd = passwd;
            pwdHashCode = passwd.hashCode();
        }

        myHash = super.hashCode()
            ^ (username != null ? username.hashCode() : 0)
            ^ pwdHashCode;
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof SimpleClientId)) {
            return false;
        }

        SimpleClientId other = (SimpleClientId)obj;

        return super.equals(obj)
            && (username == other.username // null OK
                || (username != null && username.equals(other.username)))
            && ((passwd == other.passwd)  // null OK
                || (passwd != null && other.passwd != null
                    && (((passwd instanceof String) && passwd.equals(other.passwd))
                        || ((passwd instanceof byte[])
                            && (other.passwd instanceof byte[])
                            && Arrays.equals((byte[])passwd, (byte[])other.passwd))
                        || ((passwd instanceof char[])
                            && (other.passwd instanceof char[])
                            && Arrays.equals((char[])passwd, (char[])other.passwd)))));

    }

    public int hashCode() {
        return myHash;
    }

    public String toString() {
        return super.toString() + ":" + username; // omit password for security
    }
}
