/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * Named groups.
 */
public enum NamedGroup {

    SECP256R1("secp256r1"),
    SECP384R1("secp384r1"),
    SECP521R1("secp521r1"),

    X448("x448"),
    X25519("x25519"),

    FFDHE2048("ffdhe2048"),
    FFDHE3072("ffdhe3072"),
    FFDHE4096("ffdhe4096"),
    FFDHE6144("ffdhe6144"),
    FFDHE8192("ffdhe8192");

    public final String name;

    private NamedGroup(String name) {
        this.name = name;
    }
}
