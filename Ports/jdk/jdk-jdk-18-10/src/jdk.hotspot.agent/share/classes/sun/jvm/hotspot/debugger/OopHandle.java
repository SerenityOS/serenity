/*
 * Copyright (c) 2000, 2007, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.debugger;

/** <P> This is a tag interface (similar to Cloneable) which indicates
    that the contained address is "special" and is updated under the
    hood by the VM. The purpose is to support implementation of
    reflection on the current VM with these interfaces; if the Java
    code implementing parts of the VM requires proxies for objects in
    the heap, it must be the case that those proxies are updated if GC
    occurs. This is the level at which this updating is handled. The
    VM (and specifically the GC code) must have intimate knowledge of
    the VM-specific implementation of this interface. </P>

    <P> Note that in the case of debugging a remote VM, it is not
    workable to handle the automatic updating of these handles.
    If the debugger allows the VM to resume running, it will have to
    look up once again any object references via the path they were
    found (i.e., the activation on the stack as the root, etc.) </P>
*/

public interface OopHandle extends Address {
}
