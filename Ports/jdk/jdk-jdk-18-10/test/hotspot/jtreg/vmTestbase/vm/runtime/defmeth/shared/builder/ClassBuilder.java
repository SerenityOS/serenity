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
import java.util.List;
import vm.runtime.defmeth.shared.data.method.Method;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

/**
 * Generic builder for classes of type {@code <S>} using
 * builder of type {@code <T>}.
 *
 * Builder type is necessary to support type-safe fluent code style.
 *
 * Example:
 * <code>
 * public class InterfaceBuilder
 *            extends ClassBuilder&lt;InterfaceBuilder,Interface&gt; { ...
 * </code>
 *
 * It produces {@code Interface} instance and all interim method calls return
 * {@code InterfaceBuilder} instance.
 *
 * @param <T>
 * @param <S>
 */
public abstract class ClassBuilder<T,S> implements Builder<S> {
    protected String name;

    // Class flags
    protected int flags = ACC_PUBLIC;

    // Exact class file major version, if needed
    protected int majorVer;

    // Class signature (if applicable)
    protected String sig;

    // Declared methods
    protected List<Method> methods = new ArrayList<>();

    // Enclosing test builder
    protected TestBuilder builder;

    public ClassBuilder(TestBuilder builder) {
        this.builder = builder;
    }

    @SuppressWarnings("unchecked")
    public T flags(int flags) {
        this.flags = flags;

        return (T)this;
    }

    @SuppressWarnings("unchecked")
    public T addFlags(int flags) {
        this.flags |= flags;

        return (T)this;
    }

    @SuppressWarnings("unchecked")
    public T name(String name) {
        this.name = name;

        return (T)this;
    }

    @SuppressWarnings("unchecked")
    public T method(Method m) {
        methods.add(m);

        return (T)this;
    }

    @SuppressWarnings("unchecked")
    public T ver(int ver) {
        this.majorVer = ver;

        return (T)this;
    }

    @SuppressWarnings("unchecked")
    public T sig(String sig) {
        this.sig = sig;

        return (T)this;
    }

    @Override
    public TestBuilder done() {
        build();

        return builder;
    }
}
