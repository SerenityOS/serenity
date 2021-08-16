/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package test;

// A simple class to extend an abstract class and get loaded with different
// loaders. This class is loaded via the bootstrap loader. A similar named class will
// be loaded via Loader2.
public class D_ambgs extends C {

    // We are loaded by the bootstrap loader. iface is an object of a class
    // loaded by Loader2. As it references D_ambgs, Loader2 will trigger
    // loading the version known to it, which differs from this one.
    public D_ambgs[] make(A iface) {
        // This function needs to return a value known to be loaded from Loader2.
        // Since I need to use a yet different loader, I need to make an unknown
        // foreign call. In this case I'll be using an interface to make the
        // unknown call, with but a single implementor so the compiler can do the
        // upcast statically.
        return iface == null ? null : iface.gen();
    }
}
