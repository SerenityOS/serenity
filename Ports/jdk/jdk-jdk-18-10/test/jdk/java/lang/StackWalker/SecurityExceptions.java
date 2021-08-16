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

/*
 * @test
 * @bug 8020968
 * @summary Test security permission check
 * @run main/othervm/java.security.policy=noperms.policy SecurityExceptions true
 * @run main/othervm/java.security.policy=stackwalk.policy SecurityExceptions false
 */
public class SecurityExceptions {
    public static void main(String[] args) {
        boolean expectException = Boolean.parseBoolean(args[0]);

        StackWalker sw = StackWalker.getInstance();

        try {
            sw = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
            if (expectException) {
                throw new RuntimeException("Expected SecurityException, but none thrown");
            }
        } catch (SecurityException e) {
            if (!expectException) {
                System.err.println("Unexpected security exception:");
                throw e;
            }
        }
    }
}
