/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.tools.javac.jvm;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.code.Types.UniqueType;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Pair;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * This interface models all javac entities that can be used to represent constant pool entries.
 * A pool constant entity must (i) be associated with a constant pool entry tag and have a function
 * which generates a key for the desired pool entry (so as to avoid duplicate entries when writing the
 * constant pool).
 */
public interface PoolConstant {

    /**
     * The constant pool entry key.
     */
    default Object poolKey(Types types) { return this; }

    /**
     * The constant pool entry tag.
     */
    int poolTag();

    /**
     * The root of pool constants that can be loaded (e.g. with {@code ldc}, or appear as static
     * arguments to a bootstrap method.
     */
    interface LoadableConstant extends PoolConstant {

        /**
         * Create a pool constant describing a given {@code int} value.
         */
        static LoadableConstant Int(int i) {
            return new BasicConstant(ClassFile.CONSTANT_Integer, i);
        }

        /**
         * Create a pool constant describing a given {@code float} value.
         */
        static LoadableConstant Float(float f) {
            return new BasicConstant(ClassFile.CONSTANT_Float, f);
        }

        /**
         * Create a pool constant describing a given {@code long} value.
         */
        static LoadableConstant Long(long l) {
            return new BasicConstant(ClassFile.CONSTANT_Long, l);
        }

        /**
         * Create a pool constant describing a given {@code double} value.
         */
        static LoadableConstant Double(double d) {
            return new BasicConstant(ClassFile.CONSTANT_Double, d);
        }

        /**
         * Create a pool constant describing a given {@code String} value.
         */
        static LoadableConstant String(String s) {
            return new BasicConstant(ClassFile.CONSTANT_String, s);
        }

        /**
         * This class models a pool constant of given basic type, one of {@code int}, {@code float},
         * {@code long}, {@code double} or {@code String}.
         */
        class BasicConstant implements LoadableConstant {
            int tag;
            Object data;

            private BasicConstant(int tag, Object data) {
                this.tag = tag;
                this.data = data;
            }

            @Override
            public int poolTag() {
                return tag;
            }

            @Override
            public Object poolKey(Types types) {
                return data;
            }
        }
    }

    /**
     * This interface models a dynamic pool constant (either of kind {@code InvokeDynamic} or
     * {@code ConstantDynamic}). In addition to the functionalities provided by the base interface,
     * a dynamic pool constant must expose its dynamic type, bootstrap method and static argument list.
     * Finally, a dynamic constant must have a way to compute a bootstrap method key - that is,
     * a unique key for the bootstrap method entry it refers to, to avoid duplicates when writing
     * the {@code BootstrapMethods} attribute.
     */
    interface Dynamic extends PoolConstant {

        /**
         * The dynamic constant's dynamic type.
         */
        PoolConstant dynamicType();

        /**
         * The dynamic constant's static argument list.
         */
        LoadableConstant[] staticArgs();

        /**
         * The dynamic constant's bootstrap method.
         */
        LoadableConstant bootstrapMethod();

        default BsmKey bsmKey(Types types) {
            return new BsmKey(types, bootstrapMethod(), staticArgs());
        }

        @Override
        default Object poolKey(Types types) {
            return new Pair<>(bsmKey(types), dynamicType().poolKey(types));
        }

        /**
         * A class modelling a bootstrap method key.
         */
        class BsmKey {
            /**
             * The key's bootstrap method constant.
             */
            public final LoadableConstant bsm;

            /**
             * The key's static argument list.
             */
            public final LoadableConstant[] staticArgs;

            private final Object bsmKey;
            private final List<?> staticArgKeys;

            private BsmKey(Types types, LoadableConstant bsm, LoadableConstant[] staticArgs) {
                this.bsm = bsm;
                this.bsmKey = bsm.poolKey(types);
                this.staticArgs = staticArgs;
                this.staticArgKeys = Stream.of(staticArgs)
                        .map(p -> p.poolKey(types))
                        .collect(List.collector());
            }

            @Override
            public int hashCode() {
                return bsmKey.hashCode() +
                        staticArgKeys.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                return (obj instanceof BsmKey key)
                        && Objects.equals(bsmKey, key.bsmKey)
                        && Objects.equals(staticArgKeys, key.staticArgKeys);
            }
        }
    }

    /**
     * A pool constant implementation describing a name and type pool entry.
     */
    final class NameAndType implements PoolConstant {

        final Name name;
        final Type type;

        NameAndType(Name name, Type type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public int poolTag() {
            return ClassFile.CONSTANT_NameandType;
        }

        @Override
        public Object poolKey(Types types) {
            return new Pair<>(name, new UniqueType(type, types));
        }
    }
}
