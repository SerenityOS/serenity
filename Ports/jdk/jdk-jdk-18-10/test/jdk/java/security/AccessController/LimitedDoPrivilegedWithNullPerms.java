/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8050281
 * @summary Test that NullPointerException is thrown if any element of perms
 * parameter is null
 * @run testng LimitedDoPrivilegedWithNullPerms
 */
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.PropertyPermission;
import org.testng.annotations.Test;

public class LimitedDoPrivilegedWithNullPerms {

    AccessControlContext acc = AccessController.getContext();
    Permission p1 = new PropertyPermission("user.name", "read");

    @Test(expectedExceptions = NullPointerException.class)
    public void test1() {
        AccessController.doPrivileged(
                (PrivilegedAction<Void>) () -> null, acc, null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test2() {
        AccessController.doPrivileged(
                (PrivilegedAction<Void>) () -> null, acc, p1, null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test3() {
        AccessController.doPrivilegedWithCombiner(
                (PrivilegedAction<Void>) () -> null, acc, null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test4() {
        AccessController.doPrivilegedWithCombiner(
                (PrivilegedAction<Void>) () -> null, acc, p1, null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test5() throws PrivilegedActionException {
        AccessController.doPrivileged(
                (PrivilegedExceptionAction<Void>) () -> null,
                acc, null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test6() throws PrivilegedActionException {
        AccessController.doPrivileged(
                (PrivilegedExceptionAction<Void>) () -> null,
                acc, p1, null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test7() throws PrivilegedActionException {
        AccessController.doPrivilegedWithCombiner(
                (PrivilegedExceptionAction<Void>) () -> null,
                acc, null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test8() throws PrivilegedActionException {
        AccessController.doPrivilegedWithCombiner(
                (PrivilegedExceptionAction<Void>) () -> null,
                acc, p1, null);
    }
}
