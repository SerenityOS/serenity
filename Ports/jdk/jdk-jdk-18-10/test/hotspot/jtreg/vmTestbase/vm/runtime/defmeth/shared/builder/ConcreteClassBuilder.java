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
import vm.runtime.defmeth.shared.data.ConcreteClass;
import vm.runtime.defmeth.shared.data.Interface;
import vm.runtime.defmeth.shared.data.method.Method;

import static jdk.internal.org.objectweb.asm.Opcodes.*;
import vm.runtime.defmeth.shared.data.ConcreteClassImpl;

/**
 * Builder of {@link Data.ConcreteClass} instances.
 */
public class ConcreteClassBuilder extends ClassBuilder<ConcreteClassBuilder,ConcreteClass>{
    // Super class
    private ConcreteClass parent;

    // Implemented interfaces
    private List<Interface> interfaces = new ArrayList<>();

    /* package-private */ ConcreteClassBuilder(TestBuilder builder) {
        super(builder);
    }

    public ConcreteClassBuilder extend(ConcreteClass parent) {
        this.parent = parent;

        return this;
    }

    public ConcreteClassBuilder extend(Class<?> parent) {
        this.parent = builder.toConcreteClass(parent);

        return this;
    }

    public ConcreteClassBuilder implement(Interface... ifaces) {
        interfaces.addAll(Arrays.asList(ifaces));

        return this;
    }

    public ClassMethodBuilder<ConcreteClassBuilder> abstractMethod(String name, String desc) {
        MethodBuilder mb = new MethodBuilder(builder).name(name).desc(desc).type(MethodType.ABSTRACT);

        // Ensure that the class enclosing abstract method is also abstract
        addFlags(ACC_ABSTRACT);

        return new ClassMethodBuilder<>(this, mb);
    }

    public ClassMethodBuilder<ConcreteClassBuilder> concreteMethod(String name, String desc) {
        MethodBuilder mb = new MethodBuilder(builder).name(name).desc(desc).type(MethodType.CONCRETE);
        return new ClassMethodBuilder<>(this, mb);
    }

    /**
     * Construct Data.ConcreteClass instance.
     * @return instance of Data.Interface class
     */
    @Override
    public ConcreteClass build() {
        if (name == null) {
            throw new IllegalStateException();
        }

        if (builder.hasElement(name)) {
            throw new IllegalStateException(name);
        }

        ConcreteClass clz = new ConcreteClassImpl(name, flags, majorVer, sig,
                (parent != null) ? parent : ConcreteClass.OBJECT,
                interfaces.toArray(new Interface[0]),
                methods.toArray(new Method[0]));

        builder.register(clz);
        builder.finishConstruction(this);

        return clz;
    }
}
