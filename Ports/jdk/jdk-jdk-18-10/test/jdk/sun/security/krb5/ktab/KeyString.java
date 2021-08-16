/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6917791
 * @summary KeyTabEntry, when the byte value smaller then 16, the string drop '0'
 * @modules java.security.jgss/sun.security.krb5
 *          java.security.jgss/sun.security.krb5.internal
 *          java.security.jgss/sun.security.krb5.internal.ktab
 */

import sun.security.krb5.internal.ktab.KeyTabEntry;

public class KeyString {
    public static void main(String[] args) throws Exception {
        KeyTabEntry e = new KeyTabEntry(null, null, null, 1, 1, new byte[8]);
        // "0x" plus eight "00"
        if (e.getKeyString().length() != 18) {
            throw new Exception("key bytes length not correct");
        }
    }
}
