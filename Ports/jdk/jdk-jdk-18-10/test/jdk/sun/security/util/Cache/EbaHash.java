/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8186654
 * @summary Transpositions of an array result in the same
 *          EqualByteArray.hashCode()
 * @modules java.base/sun.security.util
 */

import sun.security.util.Cache;

public class EbaHash {
    public static void main(String[] args) throws Throwable {
        int h1 = new Cache.EqualByteArray(new byte[] {1,2,3}).hashCode();
        int h2 = new Cache.EqualByteArray(new byte[] {2,3,1}).hashCode();
        int h3 = new Cache.EqualByteArray(new byte[] {3,1,2}).hashCode();
        if (h1 == h2 && h2 == h3) {
            throw new RuntimeException("Transpositions of an array" +
                " resulted in the same hashCode()");
        }
    }
}
