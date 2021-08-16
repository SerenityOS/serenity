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

import vm.runtime.defmeth.shared.builder.TestBuilder;

/**
 * Wrapper around some ConcreteClass instance. It delegates all calls to
 * underlying instance. Lazy resolution of a target instance by it's name
 * allows to delay resolution till runtime and "tie-the-knot" in recursive
 * hierarchies.
 */
public class ConcreteClassLazyAdapter extends ClazzLazyAdapter implements ConcreteClass {
    private final TestBuilder builder;
    private final String name;

    private ConcreteClass target;

    public ConcreteClassLazyAdapter(TestBuilder builder, String name) {
        super(builder, name);

        this.builder = builder;
        this.name = name;
    }

    private ConcreteClass delegate() {
        if (target == null) {
            target = (ConcreteClass)builder.lookup(name);
        }

        return target;
    }

    @Override
    public ConcreteClass parent() {
        return delegate().parent();
    }

    @Override
    public Interface[] interfaces() {
        return delegate().interfaces();
    }

    @Override
    public void visit(Visitor v) {
        delegate().visit(v);
    }
}
