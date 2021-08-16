/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.experimental.bytecode;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

public class IsolatedMethodBuilder extends MethodBuilder<Class<?>, String, Object[]> {

    public IsolatedMethodBuilder(Lookup lookup, String name, String type) {
        super(null, name, type, new IsolatedMethodPoolHelper(lookup), null);
    }

    static class IsolatedMethodPoolHelper implements PoolHelper<Class<?>, String, Object[]> {
        Map<Object, Integer> constants = new HashMap<>();
        Lookup lookup;

        private IsolatedMethodPoolHelper(Lookup lookup) {
            this.lookup = lookup;
        }

        @Override
        public int putClass(Class<?> symbol) {
            return putIfAbsent(symbol);
        }

        @Override
        public int putFieldRef(Class<?> owner, CharSequence name, String type) {
            try {
                Field f = owner.getDeclaredField(name.toString()); //TODO: we should unreflect for a var handle
                return putIfAbsent(lookup.unreflectGetter(f));
            } catch (Throwable ex) {
                ex.printStackTrace();
                return -1;
            }
        }

        @Override
        public int putMethodRef(Class<?> owner, CharSequence name, String type, boolean isInterface) {
            try {
                Method m = owner.getDeclaredMethod(name.toString()); //we should unreflect according to method vs. constructor
                //and static vs. private etc.
                return putIfAbsent(lookup.unreflect(m));
            } catch (Throwable ex) {
                ex.printStackTrace();
                return -1;
            }
        }

        @Override
        public int putInt(int i) {
            return putIfAbsent(i);
        }

        @Override
        public int putFloat(float f) {
            return putIfAbsent(f);
        }

        @Override
        public int putLong(long l) {
            return putIfAbsent(l);
        }

        @Override
        public int putDouble(double d) {
            return putIfAbsent(d);
        }

        @Override
        public int putString(String s) {
            return putIfAbsent(s);
        }

        @Override
        public int putInvokeDynamic(CharSequence invokedName, String invokedType, Class<?> bsmClass, CharSequence bsmName, String bsmType, Consumer<StaticArgListBuilder<Class<?>, String, Object[]>> staticArgs) {
            return 0; //???
        }

        @Override
        public int putDynamicConstant(CharSequence constName, String constType, Class<?> bsmClass, CharSequence bsmName, String bsmType, Consumer<StaticArgListBuilder<Class<?>, String, Object[]>> staticArgs) {
            return 0; //???
        }

        @Override
        public int putHandle(int refKind, Class<?> owner, CharSequence name, String type) {
            return 0; //???
        }

        @Override
        public int putHandle(int refKind, Class<?> owner, CharSequence name, String type, boolean isInterface) {
            return 0; //???
        }

        @Override
        public int putMethodType(String s) {
            return 0; //???
        }

        @Override
        public int putUtf8(CharSequence s) {
            return putIfAbsent(s);
        }

        @Override
        public int putType(String s) {
            return putIfAbsent(s);
        }

        @Override
        public int size() {
            return constants.size();
        }

        @Override
        public Object[] entries() {
            return constants.keySet().toArray();
        }

        int putIfAbsent(Object o) {
            int nextIndex = constants.size() + 1;
            Object res = constants.putIfAbsent(o, nextIndex);
            return res == null ?
                    nextIndex : (Integer)res;
        }
    }

    public Object[] entries() {
        return poolHelper.entries();
    }

    @Override
    public byte[] build() {
        byte[] arr = super.build();
        int codelength_offset = 2 + 2 + 2 + 2 +
                2 + 4 + 2 + 2;
        int code_offset = codelength_offset + 4;
        int length = ByteBuffer.wrap(arr).getInt(codelength_offset);
        byte[] opcodes = new byte[length];
        System.arraycopy(arr, code_offset, opcodes, 0, length);
        return opcodes;
    }

    public static void main(String[] args) {
        IsolatedMethodBuilder imb =  new IsolatedMethodBuilder(MethodHandles.lookup(), "foo", "(java/lang/String;)I");
        imb.withCode(C ->
                    C.aload_0()
                     .invokevirtual(String.class, "length", "()I", false)
                     .ireturn());
        byte[] opcodes = imb.build();
        System.out.println(Arrays.toString(opcodes));
    }
}
