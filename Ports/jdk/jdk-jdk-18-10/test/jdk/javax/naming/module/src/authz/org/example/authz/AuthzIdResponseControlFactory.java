/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/**
 * This class constructs LDAPv3 Authorization Identity response controls.
 */

package org.example.authz;

import java.io.*;
import javax.naming.*;
import javax.naming.ldap.*;

public class AuthzIdResponseControlFactory extends ControlFactory {

    public AuthzIdResponseControlFactory() {
    }

    public Control getControlInstance(Control control) throws NamingException {
        String id = control.getID();

        try {
            if (id.equals(AuthzIdResponseControl.OID)) {
                return new AuthzIdResponseControl(id, control.isCritical(),
                    control.getEncodedValue());
            }
        } catch (IOException e) {
            NamingException ne = new NamingException();
            ne.setRootCause(e);
            throw ne;
        }

        return null;
    }
}
