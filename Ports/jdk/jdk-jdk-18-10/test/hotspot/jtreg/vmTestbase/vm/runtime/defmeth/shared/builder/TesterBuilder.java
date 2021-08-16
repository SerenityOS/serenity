/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

import nsk.share.Pair;
import jdk.internal.org.objectweb.asm.Opcodes;
import vm.runtime.defmeth.shared.Util;
import vm.runtime.defmeth.shared.data.Clazz;
import vm.runtime.defmeth.shared.data.ConcreteClass;
import vm.runtime.defmeth.shared.data.Tester;
import vm.runtime.defmeth.shared.data.method.Method;
import vm.runtime.defmeth.shared.data.method.param.IntParam;
import vm.runtime.defmeth.shared.data.method.param.Param;
import vm.runtime.defmeth.shared.data.method.param.StringParam;
import vm.runtime.defmeth.shared.data.method.result.IntResult;
import vm.runtime.defmeth.shared.data.method.result.Result;
import vm.runtime.defmeth.shared.data.method.result.ResultIgnore;
import vm.runtime.defmeth.shared.data.method.result.ThrowExResult;
import static jdk.internal.org.objectweb.asm.Opcodes.*;
import vm.runtime.defmeth.shared.data.Interface;
import vm.runtime.defmeth.shared.data.method.body.CallMethod;
import vm.runtime.defmeth.shared.data.method.body.CallMethod.Invoke;

/**
 * Builder for data.Tester instances.
 *
 * Simplifies single test construction during test case preparation.
 * Test scenario is the following: call some method and check it's result.
 */
public class TesterBuilder implements Builder<Tester> {
    // Test number
    private final int id;

    // Test class name
    private String name;

    // Static receiver of the call
    private Clazz staticReceiver;

    // Dynamic receiver of the call
    // Null, if calling static method.
    private ConcreteClass dynamicReceiver;

    // Method to call: name + erased signature
    private Method m;

    // Expected result of method invocation
    private Result result;

    // Parameters for method invocation
    private Param[] params = new Param[0];

    // Enclosing test builder
    private TestBuilder builder;

    // private method test
    private boolean testPrivateMethod;

    // Type of constant pool entry used at call site
    private CallMethod.IndexbyteOp cpEntryType = CallMethod.IndexbyteOp.CALLSITE; // Callee-specific by default

    /* package-private */ TesterBuilder(int id, TestBuilder builder) {
        this.id = id;
        this.builder = builder;
        this.testPrivateMethod = false;
    }

    public TesterBuilder name(String name) {
        this.name = name;
        return this;
    }

    private TesterBuilder static_(Clazz receiver) {
        staticReceiver = receiver;
        return this;
    }

    private TesterBuilder dynamic(ConcreteClass receiver) {
        dynamicReceiver = receiver;
        return this;
    }

    public TesterBuilder callee(Method m) {
        this.m = m;
        return this;
    }

    public TesterBuilder callee(String name, String desc) {
        return callee(
                builder.method().name(name).desc(desc).build());
    }

    public TesterBuilder callee(String name, String desc, int acc) {
        return callee(
                builder.method().name(name).desc(desc).flags(acc).build());
    }

    public TesterBuilder cpEntryType(CallMethod.IndexbyteOp type) {
        cpEntryType = type;
        return this;
    }

    public TesterBuilder callSite(Clazz staticReceiver, ConcreteClass receiver,
            String methodName, String methodDesc) {
        return static_(staticReceiver)
                .dynamic(receiver)
                .callee(methodName, methodDesc);
    }

    public TesterBuilder callSite(ConcreteClass receiver,
            String methodName, String methodDesc) {
        return dynamic(receiver).callee(methodName, methodDesc);
    }

    public TesterBuilder staticCallSite(Clazz I, String methodName, String methodDesc) {
        return static_(I).callee(methodName, methodDesc, ACC_STATIC);
    }

    public TesterBuilder callSite(Clazz I, String methodName, String methodDesc, int acc) {
        if ((acc & ACC_PRIVATE) != 0) {
            testPrivateMethod = true;
        }
        return static_(I).callee(methodName, methodDesc, acc);
    }

    public TesterBuilder privateCallSite(Clazz staticReceiver, ConcreteClass receiver,
            String methodName, String methodDesc) {
        testPrivateMethod = true;
        return static_(staticReceiver)
                .dynamic(receiver)
                .callee(methodName, methodDesc);
    }

    public TesterBuilder interfaceCallSite(Clazz staticReceiver, ConcreteClass receiver,
            String methodName, String methodDesc) {
        return static_(staticReceiver)
                .dynamic(receiver)
                .callee(methodName, methodDesc, ACC_INTERFACE)
                .cpEntryType(CallMethod.IndexbyteOp.INTERFACEMETHODREF);
    }

    public TesterBuilder new_(ConcreteClass receiver) {
        return callSite(receiver, receiver,"<init>", "()V");
    }

    public TesterBuilder params(int... intParams) {
        this.params = new Param[intParams.length];
        for (int i = 0; i < intParams.length; i++) {
            this.params[i] = new IntParam(intParams[i]);
        }

        return this;
    }

    public TesterBuilder params(String... strParams) {
        this.params = new Param[strParams.length];
        for (int i = 0; i < strParams.length; i++) {
            this.params[i] = new StringParam(strParams[i]);
        }

        return this;
    }

    public TesterBuilder params(Param... params) {
        this.params = params;
        return this;
    }

    public TesterBuilder result(Result result) {
        this.result = result;
        return this;
    }

    public TesterBuilder ignoreResult() { return result(new ResultIgnore());}
    public TesterBuilder returns(int i)     { return result(new IntResult(i)); }
    public TesterBuilder throws_(Clazz clz) {
        return result(new ThrowExResult(clz, false, null));
    }

    public TesterBuilder throws_(Class<? extends Throwable> exc) {
        return result(new ThrowExResult(builder.toClazz(exc), false, null));
    }

    public TesterBuilder throwsExact(Clazz clz) {
        return result(new ThrowExResult(clz, true, null));
    }

    public TesterBuilder throwsExact(Class<? extends Throwable> exc) {
            return result(new ThrowExResult(builder.toClazz(exc), true, null));
    }

    public TesterBuilder succeeds() {
        return result(new ResultIgnore());
    }

    public TesterBuilder loadClass(Clazz clz) {
        return static_(builder.toClazz(Class.class))
                .callee("forName", "(Ljava/lang/String;)Ljava/lang/Class;", ACC_STATIC)
                .params(clz.name());
    }

    /**
     * Choose right instruction for a call depending on the callee type.
     *
     * @return
     */
    private CallMethod.Invoke getCallInsn() {
        if ((m.acc() & Opcodes.ACC_STATIC) != 0) {
            return Invoke.STATIC;
        } else if (staticReceiver instanceof Interface) {
            return Invoke.INTERFACE;
        } else if (staticReceiver instanceof ConcreteClass) {
            if (m.isConstructor()) {
                return Invoke.SPECIAL;
            } else if ((m.acc() & Opcodes.ACC_INTERFACE) == 0) {
                return Invoke.VIRTUAL;
            } else {
                return Invoke.INTERFACE;
            }
        } else {
            throw new UnsupportedOperationException("Can't detect invoke instruction");
        }
    }

    @Override
    public Tester build() {
        if (staticReceiver == null) {
            throw new IllegalStateException();
        }

        if (m == null) {
            throw new IllegalStateException();
        }

        if (result == null) {
            throw new IllegalStateException();
        }

        if (params.length == 0) {
            params =
                Util.getDefaultValues(
                    Util.parseDesc(m.desc()).first);
        }

        if (name == null) {
            name = String.format("Test%d_%s_%s_%s", id,
                                 staticReceiver != null ? staticReceiver.getShortName() : "",
                                 dynamicReceiver != null ? dynamicReceiver.getShortName() : "",
                                 m.name());
        }

        Invoke callInsn = getCallInsn();

        Pair<String[],String> calleeDetails = Util.parseDesc(m.desc());
        String returnType = calleeDetails.second;

        CallMethod call = new CallMethod(callInsn, staticReceiver, dynamicReceiver,
                                        m.name(), m.desc(), params,
                                        returnType, false, cpEntryType);

        Tester tester = new Tester(name, call, result, testPrivateMethod);

        builder.register(tester);

        // Notify test builder that test construction is finished
        builder.finishConstruction(this);

        return tester;
    }

    @Override
    public TestBuilder done() {
        build();
        return builder;
    }
}
