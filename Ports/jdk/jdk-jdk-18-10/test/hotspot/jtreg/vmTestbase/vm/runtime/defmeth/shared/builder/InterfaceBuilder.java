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

package vm.runtime.defmeth.shared.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import vm.runtime.defmeth.shared.data.Interface;
import vm.runtime.defmeth.shared.data.InterfaceImpl;
import vm.runtime.defmeth.shared.data.method.Method;

/**
 * Builder for Interface instances.
 */
public class InterfaceBuilder extends ClassBuilder<InterfaceBuilder,Interface> {

    // Superinterfaces
    private List<Interface> parents = new ArrayList<>();

    /* package-private */ InterfaceBuilder(TestBuilder builder) {
        super(builder);
    }

    public InterfaceBuilder extend(Interface... intf) {
        parents.addAll(Arrays.asList(intf));

        return this;
    }

    public ClassMethodBuilder<InterfaceBuilder> defaultMethod(String name, String desc) {
        MethodBuilder mb = builder.method().name(name).desc(desc).type(MethodType.DEFAULT);
        return new ClassMethodBuilder<>(this, mb);
    }

    public ClassMethodBuilder<InterfaceBuilder> abstractMethod(String name, String desc) {
        MethodBuilder mb = builder.method().name(name).desc(desc).type(MethodType.ABSTRACT);
        return new ClassMethodBuilder<>(this, mb);
    }

    /**
     * Construct Data.Interface instance.
     * @return instance of Data.Interface class
     */
    @Override
    public Interface build() {
        if (name == null) {
            throw new IllegalStateException();
        }

        Interface intf = new InterfaceImpl(flags, name, majorVer, sig, parents.toArray(new Interface[0]),
                methods.toArray(new Method[0]));

        if (builder.hasElement(name)) {
            throw new IllegalStateException();
        }

        builder.register(intf);
        builder.finishConstruction(this);

        return intf;
    }
}
