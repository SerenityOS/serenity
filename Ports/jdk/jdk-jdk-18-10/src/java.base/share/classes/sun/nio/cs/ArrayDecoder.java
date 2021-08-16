/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.cs;

/*
 * FastPath byte[]->char[] decoder, REPLACE on malformed or
 * unmappable input.
 *
 * FastPath encoded byte[]-> "String Latin1 coding" byte[] decoder for use when
 * charset is always decodable to the internal String Latin1 coding byte[], ie. all mappings <=0xff
 */

public interface ArrayDecoder {
    int decode(byte[] src, int off, int len, char[] dst);

    default boolean isASCIICompatible() {
        return false;
    }

    // Is always decodable to internal String Latin1 coding, ie. all mappings <= 0xff
    default boolean isLatin1Decodable() {
        return false;
    }

    // Decode to internal String Latin1 coding byte[] fastpath for when isLatin1Decodable == true
    default int decodeToLatin1(byte[] src, int sp, int len, byte[] dst) {
        return 0;
    }
}
