/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8076596
 * @run main/othervm/policy=Test8076596.security.policy/secure=Test8076596 -ea -esa Test8076596
 */

import java.security.AccessController;
import java.security.PrivilegedAction;

public class Test8076596 extends SecurityManager {
    public Test8076596() {
        // 1. Using lambda
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> null);
        // 2. Using inner class
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                return null;
            }
        });
    }

    public static void main(String[] args) {
        // empty
    }
}
