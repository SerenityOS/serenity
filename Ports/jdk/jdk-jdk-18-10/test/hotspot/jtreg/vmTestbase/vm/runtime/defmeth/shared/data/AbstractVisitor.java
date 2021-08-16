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

import vm.runtime.defmeth.shared.data.method.AbstractMethod;
import vm.runtime.defmeth.shared.data.method.ConcreteMethod;
import vm.runtime.defmeth.shared.data.method.DefaultMethod;
import vm.runtime.defmeth.shared.data.method.Method;
import vm.runtime.defmeth.shared.data.method.body.CallMethod;
import vm.runtime.defmeth.shared.data.method.body.EmptyBody;
import vm.runtime.defmeth.shared.data.method.body.ReturnIntBody;
import vm.runtime.defmeth.shared.data.method.body.ReturnNewInstanceBody;
import vm.runtime.defmeth.shared.data.method.body.ReturnNullBody;
import vm.runtime.defmeth.shared.data.method.body.ThrowExBody;
import vm.runtime.defmeth.shared.data.method.param.DoubleParam;
import vm.runtime.defmeth.shared.data.method.param.FloatParam;
import vm.runtime.defmeth.shared.data.method.param.IntParam;
import vm.runtime.defmeth.shared.data.method.param.LongParam;
import vm.runtime.defmeth.shared.data.method.param.NewInstanceParam;
import vm.runtime.defmeth.shared.data.method.param.StringParam;
import vm.runtime.defmeth.shared.data.method.result.IntResult;
import vm.runtime.defmeth.shared.data.method.result.ThrowExResult;

/**
 * {@code AbstactVisitor} provides implementation for all methods from {@code Visitor},
 * which throws {@code UnsupportedOperationException}. It is useful
 * for custom visitors, which expect only a subset of events to occur.
 */
public class AbstractVisitor implements Visitor {
    @Override
    public void visitClass(Clazz clz) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitConcreteClass(ConcreteClass clz) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitInterface(Interface intf) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitMethod(Method m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitConcreteMethod(ConcreteMethod m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitAbstractMethod(AbstractMethod m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitDefaultMethod(DefaultMethod m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitTester(Tester t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitThrowExBody(ThrowExBody body) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitReturnIntBody(ReturnIntBody body) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitReturnNullBody(ReturnNullBody body) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitEmptyBody(EmptyBody body) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitCallMethod(CallMethod call) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitReturnNewInstanceBody(ReturnNewInstanceBody body) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitResultInt(IntResult res) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitResultThrowExc(ThrowExResult res) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitResultIgnore() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitParamInt(IntParam i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitParamLong(LongParam l) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitParamFloat(FloatParam f) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitParamDouble(DoubleParam d) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitParamString(StringParam str) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitParamNewInstance(NewInstanceParam type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitParamNull() {
        throw new UnsupportedOperationException();
    }
}
