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

import vm.runtime.defmeth.shared.data.method.body.ReturnNewInstanceBody;
import vm.runtime.defmeth.shared.data.method.body.ReturnNullBody;
import vm.runtime.defmeth.shared.data.method.param.NewInstanceParam;
import vm.runtime.defmeth.shared.data.method.param.LongParam;
import vm.runtime.defmeth.shared.data.method.result.IntResult;
import vm.runtime.defmeth.shared.data.method.param.IntParam;
import vm.runtime.defmeth.shared.data.method.param.FloatParam;
import vm.runtime.defmeth.shared.data.method.body.EmptyBody;
import vm.runtime.defmeth.shared.data.method.param.DoubleParam;
import vm.runtime.defmeth.shared.data.method.DefaultMethod;
import vm.runtime.defmeth.shared.data.method.ConcreteMethod;
import vm.runtime.defmeth.shared.data.method.body.CallMethod;
import vm.runtime.defmeth.shared.data.method.AbstractMethod;
import vm.runtime.defmeth.shared.data.method.Method;
import vm.runtime.defmeth.shared.data.method.body.ReturnIntBody;
import vm.runtime.defmeth.shared.data.method.body.ThrowExBody;
import vm.runtime.defmeth.shared.data.method.result.ThrowExResult;
import vm.runtime.defmeth.shared.data.method.param.StringParam;
import vm.runtime.defmeth.shared.data.Clazz;
import vm.runtime.defmeth.shared.data.Interface;
import vm.runtime.defmeth.shared.data.*;

/**
 * Visitor for vm.runtime.defmeth.shared.data.* class hierarchy.
 */
public interface Visitor {

    // extends Clazz
    public void visitClass        (        Clazz  clz);
    public void visitConcreteClass(ConcreteClass  clz);
    public void visitInterface    (    Interface intf);

    // extends Method
    public void visitMethod        (        Method m);
    public void visitConcreteMethod(ConcreteMethod m);
    public void visitAbstractMethod(AbstractMethod m);
    public void visitDefaultMethod ( DefaultMethod m);

    public void visitTester(                Tester t);

    // implements MethodBody
    public void visitThrowExBody          (          ThrowExBody body);
    public void visitReturnIntBody        (        ReturnIntBody body);
    public void visitReturnNullBody       (       ReturnNullBody body);
    public void visitEmptyBody            (            EmptyBody body);
    public void visitCallMethod           (           CallMethod call);
    public void visitReturnNewInstanceBody(ReturnNewInstanceBody body);

    // implements Result
    public void visitResultInt     (    IntResult res);
    public void visitResultThrowExc(ThrowExResult res);
    public void visitResultIgnore  ();

    // implements Param
    public void visitParamInt        (        IntParam i);
    public void visitParamLong       (       LongParam l);
    public void visitParamFloat      (      FloatParam f);
    public void visitParamDouble     (     DoubleParam d);
    public void visitParamString     (     StringParam  str);
    public void visitParamNewInstance(NewInstanceParam type);
    public void visitParamNull       ();
}
