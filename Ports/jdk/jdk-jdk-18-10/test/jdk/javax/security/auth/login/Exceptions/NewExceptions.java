/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4469116
 * @summary LoginException subclasses
 */

import javax.security.auth.login.*;

public class NewExceptions {
    public static void main(String[] args) throws Exception {
        AccountException ac =
                new AccountException("AccountException");
        AccountExpiredException aee =
                new AccountExpiredException("AccountExpiredException");
        AccountLockedException ale =
                new AccountLockedException("AccountLockedException");
        AccountNotFoundException anfe =
                new AccountNotFoundException("AccountNotFoundException");
        CredentialException ce =
                new CredentialException("CredentialException");
        CredentialExpiredException cee =
                new CredentialExpiredException("CredentialExpiredException");
        CredentialNotFoundException cnfe =
                new CredentialNotFoundException("CredentialNotFoundException");

        if (! (ac instanceof LoginException) ||
            ! (ce instanceof LoginException) ||
            ! (aee instanceof AccountException) ||
            ! (ale instanceof AccountException) ||
            ! (anfe instanceof AccountException) ||
            ! (cee instanceof CredentialException) ||
            ! (cnfe instanceof CredentialNotFoundException)) {
            throw new SecurityException("Test 1 failed");
        }

        if (!ac.getMessage().equals("AccountException") ||
            !aee.getMessage().equals("AccountExpiredException") ||
            !ale.getMessage().equals("AccountLockedException") ||
            !anfe.getMessage().equals("AccountNotFoundException") ||
            !ce.getMessage().equals("CredentialException") ||
            !cee.getMessage().equals("CredentialExpiredException") ||
            !cnfe.getMessage().equals("CredentialNotFoundException")) {
            throw new SecurityException("Test 2 failed");
        }
    }
}
