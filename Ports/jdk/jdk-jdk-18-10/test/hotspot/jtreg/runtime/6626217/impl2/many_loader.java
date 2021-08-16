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
// loaders. This class is loaded via LOADER2. A similar named class will
// be loaded via LOADER1.
public class many_loader extends bug_21227 {
    final Object _ref_to_be_p0wned;

    many_loader() {
        _ref_to_be_p0wned = bug_21227._p0wnee;
        System.out.println("Gonna hack this thing: " + _ref_to_be_p0wned.toString() );
    }

    // I need to compile (hence call in a loop) a function which returns a value
    // loaded from classloader other than the system one. The point of this
    // call is to give me an abstract 'hook' into a function loaded with a
    // foreign loader.
    public many_loader[] make(IFace iface) {
        throw new Error("do not call me");
    }
}
