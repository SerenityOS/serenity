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

import java.util.HashSet;
import java.util.Set;
import nsk.share.Pair;
import vm.runtime.defmeth.shared.Util;
import vm.runtime.defmeth.shared.data.Clazz;
import vm.runtime.defmeth.shared.data.method.AbstractMethod;
import vm.runtime.defmeth.shared.data.method.ConcreteMethod;
import vm.runtime.defmeth.shared.data.method.DefaultMethod;
import vm.runtime.defmeth.shared.data.method.Method;
import vm.runtime.defmeth.shared.data.method.body.CallMethod;
import vm.runtime.defmeth.shared.data.method.body.EmptyBody;
import vm.runtime.defmeth.shared.data.method.body.MethodBody;
import vm.runtime.defmeth.shared.data.method.body.ReturnIntBody;
import vm.runtime.defmeth.shared.data.method.body.ReturnNewInstanceBody;
import vm.runtime.defmeth.shared.data.method.body.ReturnNullBody;
import vm.runtime.defmeth.shared.data.method.body.ThrowExBody;
import vm.runtime.defmeth.shared.data.method.param.Param;
import static jdk.internal.org.objectweb.asm.Opcodes.*;
import vm.runtime.defmeth.shared.data.ConcreteClass;

/**
 * Builder of Method instances.
 */
public class MethodBuilder {
    private MethodType type;
    private int flags = 0;
    private String name;
    private MethodBody body;

    // Method's erased signature
    private String desc;

    // Method's language-level signature
    private String sig;

    private TestBuilder builder;

    /* package-private */ MethodBuilder(TestBuilder builder) {
        this.builder = builder;
    }

    /* package-private */ MethodBuilder type(MethodType type) {
        this.type = type;
        return this;
    }

    public MethodBuilder name(String name) {
        this.name = name;

        return this;
    }

    public MethodBuilder desc(String desc) {
        this.desc = desc;
        return this;
    }

    public MethodBuilder sig(String sig) {
        this.sig = sig;
        return this;
    }

    public MethodBuilder body(MethodBody body) {
        if (type == null || type == MethodType.ABSTRACT) {
            throw new IllegalStateException();
        }

        this.body = body;

        return this;
    }

    public MethodBuilder empty()            { return body(new EmptyBody()); }
    public MethodBuilder returns(int i)     { return body(new ReturnIntBody(i)); }
    public MethodBuilder returnsNull()      { return body(new ReturnNullBody()); }
    public MethodBuilder throws_(Clazz clz) { return body(new ThrowExBody(clz)); }
    public MethodBuilder throws_(Class<? extends Throwable> exc) {
        return body(new ThrowExBody(builder.toClazz(exc)));
    }

    public MethodBuilder returnsNewInstance(ConcreteClass clz) {
        return body(new ReturnNewInstanceBody(clz));
    }

    public MethodBuilder superCall(Clazz callee, String methodName, String methodDesc) {
        return invokeSpecial(callee, methodName, methodDesc);
    }

    public MethodBuilder invokeSpecial(Clazz callee, String methodName, String methodDesc) {
        return invoke(CallMethod.Invoke.SPECIAL, callee, null, methodName,
                    methodDesc, CallMethod.IndexbyteOp.CALLSITE);
    }

    public MethodBuilder invokeStatic(Clazz callee, String methodName, String methodDesc) {
        return invoke(CallMethod.Invoke.STATIC, callee, null, methodName,
                    methodDesc, CallMethod.IndexbyteOp.CALLSITE);
    }

    public MethodBuilder invoke(CallMethod.Invoke callInsn, Clazz staticCallee,
                                ConcreteClass callee, String methodName, String methodDesc,
                                CallMethod.IndexbyteOp generateIndexbyteOp) {
        Pair<String[],String> calleeDetails = Util.parseDesc(methodDesc);
        Param[] params = Util.getDefaultValues(calleeDetails.first);
        String returnType = calleeDetails.second;

        boolean popResult = Util.isNonVoid(returnType)
                && !Util.isNonVoid(Util.parseDesc(desc).second);

        return body(new CallMethod(
                            callInsn, staticCallee, callee,
                            methodName, methodDesc, params, returnType,
                            popResult, generateIndexbyteOp));
    }

    // set of accessibility flags for the method
    private Set<AccessFlag> methodAccFlags = new HashSet<>();

    public MethodBuilder public_()         { methodAccFlags.add(AccessFlag.PUBLIC);          return this; }
    public MethodBuilder protected_()      { methodAccFlags.add(AccessFlag.PROTECTED);       return this; }
    public MethodBuilder private_()        { methodAccFlags.add(AccessFlag.PRIVATE);         return this; }
    public MethodBuilder package_private() { methodAccFlags.add(AccessFlag.PACKAGE_PRIVATE); return this; }

    public MethodBuilder static_()    { return addFlags(ACC_STATIC); }
    public MethodBuilder synthetic()    { return addFlags(ACC_SYNTHETIC); }

    private void parseFlags(int flags) {
        if ((flags & ACC_PUBLIC) != 0) {
            public_();
        }
        if ((flags & ACC_PROTECTED) != 0) {
            protected_();
        }
        if ((flags & ACC_PRIVATE) != 0) {
            private_();
        }
        if ((flags & (ACC_PUBLIC | ACC_PROTECTED | ACC_PRIVATE)) == 0) {
            package_private();
        }
    }

    public MethodBuilder flags(int flags) {
        methodAccFlags.clear();

        parseFlags(flags);
        this.flags = flags;

        return this;
    }

    public MethodBuilder addFlags(int flags) {
        parseFlags(flags);
        this.flags |= flags;

        return this;
    }

    public Method build() {
        if (name == null) {
            throw new IllegalStateException();
        }

        int lFlags = flags;
        if (!methodAccFlags.isEmpty()) {
            for (AccessFlag flag : methodAccFlags) {
                lFlags |= flag.mask;
            }
        } else {
            lFlags |= AccessFlag.PUBLIC.mask;
        }

        if (type != null) {
            switch (type) {
                case ABSTRACT:
                    // Excerpt from JVMS-4.6 "Methods [Modified]:
                    //   If a specific method of a class or interface has its ACC_ABSTRACT flag set,
                    // it must not have any of its ACC_FINAL, ACC_NATIVE, ACC_PRIVATE, ACC_STATIC, or
                    // ACC_SYNCHRONIZED flags set (8.4.3.1, 8.4.3.3, 8.4.3.4).
                    lFlags |= (builder.accFlags & ~(ACC_NATIVE | ACC_PRIVATE | ACC_STATIC | ACC_SYNCHRONIZED));
                    return new AbstractMethod(lFlags, name, desc, sig);
                case CONCRETE:
                    lFlags |= builder.accFlags;
                    return new ConcreteMethod(lFlags, name, desc, sig, body);
                case DEFAULT:
                    // Excerpt from JVMS-4.6 "Methods [Modified]:
                    // Methods of interfaces may set any of the flags in Table 4.5 except ACC_PROTECTED, ACC_FINAL,
                    // ACC_NATIVE, and ACC_SYNCHRONIZED (9.4); they must have exactly one of the ACC_PUBLIC or
                    // ACC_PRIVATE flags set.
                    lFlags |= (builder.accFlags & ~(ACC_NATIVE | ACC_PRIVATE | ACC_STATIC | ACC_SYNCHRONIZED));
                    return new DefaultMethod(lFlags, name, desc, sig, body);
                default:
                    throw new IllegalStateException();
            }
        } else {
            return new Method(lFlags, name, desc, sig);
        }
    }
}
