/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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

import javax.security.auth.login.LoginException;

/**
 * Login module which passes all the time
 */

public class DummyLoginModule extends SmartLoginModule {
    private final String header;

    public DummyLoginModule() {
        header = "DummyLoginModule: ";
    }

    @Override
    public boolean login() throws LoginException {
        System.out.println("\t\t" + header + " login method is called ");
        System.out.println("\t\t" + header + " login:PASS");
        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        System.out.println("\t\t" + header + " commit method is called");
        System.out.println("\t\t" + header + " commit:PASS");
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        System.out.println("\t\t" + header + " abort method is called ");
        System.out.println("\t\t" + header + " abort:PASS");

        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        System.out.println("\t\t" + header + " logout method is called");
        System.out.println("\t\t" + header + " logout:PASS");
        return true;
    }

}
