/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**/

public class TestSecurityManager extends SecurityManager {
    public static final int EXIT_VALUE = 123;

    public TestSecurityManager() {
    }

    public void checkListen(int port) {
        // 4269910: ok, now the registry will *really* go away...
        //
        // the registry needs to listen on sockets so they
        // will exit when they try to do so... this is used as a sign
        // by the main test process to detect that the proper security
        // manager has been installed in the relevant VMs.
        //
        System.exit(EXIT_VALUE);
    }

    public void checkExit(int status) {
        // permit check exit for all code
    }
}
