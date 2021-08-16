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

import vm.runtime.defmeth.shared.data.AbstractVisitor;
import vm.runtime.defmeth.shared.data.method.param.*;
import vm.runtime.defmeth.shared.executor.AbstractReflectionTest;

/**
 * Extracts a value from Param object.
 * It is used when method is invoked using Reflection API
 * or through MethodHandle.invokeWithArguments.
 */
public class ParamValueExtractor extends AbstractVisitor {
    private Object value;
    private AbstractReflectionTest methodInvokeTest;

    public Object value() {
        param.visit(this);
        return value;
    }

    private Param param;
    public ParamValueExtractor(AbstractReflectionTest methodInvokeTest, Param param) {
        this.methodInvokeTest = methodInvokeTest;
        this.param = param; }

    @Override public void visitParamInt(IntParam i) { value = i.value(); }
    @Override public void visitParamLong(LongParam l) { value = l.value(); }
    @Override public void visitParamFloat(FloatParam f) { value = f.value(); }
    @Override public void visitParamDouble(DoubleParam d) { value = d.value(); }
    @Override public void visitParamString(StringParam str) { value = str.value(); }
    @Override public void visitParamNull() { value = null; }
    @Override public void visitParamNewInstance(NewInstanceParam type) {
        try {
            value = methodInvokeTest.resolve(type.clazz()).newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new Error(e);
        }
    }
}
