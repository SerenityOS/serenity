/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

package vm.runtime.defmeth.shared.data;

import vm.runtime.defmeth.shared.data.method.Method;

public abstract class ClazzImpl implements Clazz {

    /** Fixed major version of class file if needed.
     * "0" means default value (depends on the context). */
    int ver = 0; // class file major version

    String name;

    /** Access flags of the class.
     * "-1" means default (depends on the context). */
    int flags = -1;

    Method[] methods;

    /** Language-level signature of the class.
     * Represents information about generic parameters */
    String sig;

    ClazzImpl(String name, int ver, int flags, String sig, Method[] methods) {
        this.name = name;
        this.ver = ver;
        this.flags = flags;
        this.sig = sig;
        this.methods = methods;
    }

    public String name() {
        return name;
    }

    public int ver() {
        return ver;
    }

    public Method[] methods() {
        return methods;
    }

    public int flags() {
        return flags;
    }

    public String sig() {
        return sig;
    }

    /** Class name in VM-internal format */
    public String intlName() {
        return name.replaceAll("\\.","/");
    }

    public String getShortName() {
        return name.replaceAll(".*\\.","");
    }
}
