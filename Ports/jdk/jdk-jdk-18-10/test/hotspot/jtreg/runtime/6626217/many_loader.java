/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

// A simple class to extend an abstract class and get loaded with different
// loaders. This class is loaded via LOADER1. A similar named class will
// be loaded via LOADER2.
public class many_loader extends bug_21227 {
    public You_Have_Been_P0wned _p0wnee;

    // I need to compile (hence call in a loop) a function which returns a value
    // loaded from classloader other than the system one. The point of this
    // call is to give me an abstract 'hook' into a function loaded with a
    // foreign loader.

    // The original 'make(boolean)' returns a bug_21227. The VM will inject a
    // synthetic method to up-cast the returned 'from_loader1' into a
    // 'bug_21227'.
    public many_loader[] make(IFace iface) {
        // This function needs to return a value known to be loaded from LOADER2.
        // Since I need to use a yet different loader, I need to make an unknown
        // foreign call. In this case I'll be using an interface to make the
        // unknown call, with but a single implementor so the compiler can do the
        // upcast statically.
        return iface==null ? null : iface.gen();
    }
}
