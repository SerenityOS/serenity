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

import vm.runtime.defmeth.shared.data.Clazz;
import vm.runtime.defmeth.shared.data.ConcreteClass;
import vm.runtime.defmeth.shared.data.method.body.CallMethod;
import vm.runtime.defmeth.shared.data.method.body.MethodBody;

/**
 * Context-specific builder for method instances of type {@code <T>}.
 * Allows to construct type-safe nested builders for classes with methods.
 *
 * Example:
 * <code>
 * Interface I =
 *      new TestBuilder().intf("I")
 *          .defaultMethod("m", "()V").emptyBody().build()
 *      .build();
 * </code>
 *
 * @param <T>
 */
public class ClassMethodBuilder<T> {
    /* Enclosing Clazz builder */
    private ClassBuilder<T,?> cb;

    private MethodBuilder mb;

    /* package-private */ ClassMethodBuilder(ClassBuilder<T,?> cb, MethodBuilder mb) {
        this.cb = cb;
        this.mb = mb;
    }

    public ClassMethodBuilder<T> name(String name) { mb.name(name); return this; }
    public ClassMethodBuilder<T> desc(String desc) { mb.desc(desc); return this; }
    public ClassMethodBuilder<T> sig(String sig)   { mb.sig(sig);   return this; }

    public ClassMethodBuilder<T> body(MethodBody body) { mb.body(body);   return this; }

    public ClassMethodBuilder<T> emptyBody()           { mb.empty();       return this; }
    public ClassMethodBuilder<T> returns(int i)        { mb.returns(i);    return this; }
    public ClassMethodBuilder<T> returnsNull()         { mb.returnsNull(); return this; }
    public ClassMethodBuilder<T> throw_(Clazz clz)     { mb.throws_(clz);  return this; }
    public ClassMethodBuilder<T> throw_(Class<? extends Throwable> exc) {
        mb.throws_(exc);
        return this;
    }
    public ClassMethodBuilder<T> returnsNewInstance(ConcreteClass clz)  { mb.returnsNewInstance(clz); return this; }

    public ClassMethodBuilder<T> callSuper(Clazz callee, String methodName, String methodDesc) {
        mb.superCall(callee, methodName, methodDesc);
        return this;
    }

    public ClassMethodBuilder<T> invokeSpecial(Clazz callee, String methodName, String methodDesc) {
        mb.invokeSpecial(callee, methodName, methodDesc);
        return this;
    }

    public ClassMethodBuilder<T> invokeStatic(Clazz callee, String methodName, String methodDesc) {
        mb.invokeStatic(callee, methodName, methodDesc);
        return this;
    }

    public ClassMethodBuilder<T> invoke(CallMethod.Invoke callInsn, Clazz staticCallee,
                                        ConcreteClass callee, String methodName,
                                        String methodDesc, CallMethod.IndexbyteOp generateIndexbyteOp) {
        mb.invoke(callInsn, staticCallee, callee, methodName, methodDesc, generateIndexbyteOp);
        return this;
    }

    public ClassMethodBuilder<T> public_()         { mb.public_();         return this; }
    public ClassMethodBuilder<T> protected_()      { mb.protected_();      return this; }
    public ClassMethodBuilder<T> private_()        { mb.private_();        return this; }
    public ClassMethodBuilder<T> package_private() { mb.package_private(); return this; }

    public ClassMethodBuilder<T> static_()         { mb.static_();         return this; }
    public ClassMethodBuilder<T> synthetic()       { mb.synthetic();       return this; }

    public ClassMethodBuilder<T> flags(int flags)    { mb.flags(flags);    return this; }
    public ClassMethodBuilder<T> addFlags(int flags) { mb.addFlags(flags); return this; }

    @SuppressWarnings("unchecked")
    public T build() { cb.method(mb.build()); return (T)cb; }
}
