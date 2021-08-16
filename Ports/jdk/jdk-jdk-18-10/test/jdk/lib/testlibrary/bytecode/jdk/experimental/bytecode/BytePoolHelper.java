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

import java.lang.invoke.MethodHandleInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;

/**
 * A helper for building and tracking constant pools whose entries are
 * represented as byte arrays.
 *
 * @param <S> the type of the symbol representation
 * @param <T> the type of type descriptors representation
 */
public class BytePoolHelper<S, T> implements PoolHelper<S, T, byte[]> {

    GrowableByteBuffer pool = new GrowableByteBuffer();
    GrowableByteBuffer bsm_attr = new GrowableByteBuffer();
    //Map<PoolKey, PoolKey> indicesMap = new HashMap<>();
    int currentIndex = 1;
    int currentBsmIndex = 0;

    KeyMap<PoolKey> entries = new KeyMap<>();
    KeyMap<BsmKey> bootstraps = new KeyMap<>();
    PoolKey key = new PoolKey();
    BsmKey bsmKey = new BsmKey();

    Function<S, String> symbolToString;
    Function<T, String> typeToString;

    public BytePoolHelper(Function<S, String> symbolToString, Function<T, String> typeToString) {
        this.symbolToString = symbolToString;
        this.typeToString = typeToString;
    }

    static class KeyMap<K extends AbstractKey<K>> {

        @SuppressWarnings("unchecked")
        K[] table = (K[])new AbstractKey<?>[0x10];
        int nelems;

        public void enter(K e) {
            if (nelems * 3 >= (table.length - 1) * 2)
                dble();
            int hash = getIndex(e);
            K old = table[hash];
            if (old == null) {
                nelems++;
            }
            e.next = old;
            table[hash] = e;
        }

        protected K lookup(K other) {
            K e = table[getIndex(other)];
            while (e != null && !e.equals(other))
                e = e.next;
            return e;
        }

        /**
         * Look for slot in the table.
         * We use open addressing with double hashing.
         */
        int getIndex(K e) {
            int hashMask = table.length - 1;
            int h = e.hashCode();
            int i = h & hashMask;
            // The expression below is always odd, so it is guaranteed
            // to be mutually prime with table.length, a power of 2.
            int x = hashMask - ((h + (h >> 16)) << 1);
            for (; ; ) {
                K e2 = table[i];
                if (e2 == null)
                    return i;
                else if (e.hash == e2.hash)
                    return i;
                i = (i + x) & hashMask;
            }
        }

        @SuppressWarnings("unchecked")
        private void dble() {
            K[] oldtable = table;
            table = (K[])new AbstractKey<?>[oldtable.length * 2];
            int n = 0;
            for (int i = oldtable.length; --i >= 0; ) {
                K e = oldtable[i];
                if (e != null) {
                    table[getIndex(e)] = e;
                    n++;
                }
            }
            // We don't need to update nelems for shared inherited scopes,
            // since that gets handled by leave().
            nelems = n;
        }
    }

    public static abstract class AbstractKey<K extends AbstractKey<K>> {
        int hash;
        int index = -1;
        K next;

        abstract K dup();

        public abstract boolean equals(Object o);

        @Override
        public int hashCode() {
            return hash;
        }

        void at(int index) {
            this.index = index;
        }
    }

    public static class PoolKey extends AbstractKey<PoolKey> {
        PoolTag tag;
        Object o1;
        Object o2;
        Object o3;
        Object o4;
        int size = -1;

        void setUtf8(CharSequence s) {
            tag = PoolTag.CONSTANT_UTF8;
            o1 = s;
            size = 1;
            hash = tag.tag | (s.hashCode() << 1);
        }

        void setClass(String clazz) {
            tag = PoolTag.CONSTANT_CLASS;
            o1 = clazz;
            size = 1;
            hash = tag.tag | (clazz.hashCode() << 1);
        }

        void setNameAndType(CharSequence name, String type) {
            tag = PoolTag.CONSTANT_NAMEANDTYPE;
            o1 = name;
            o2 = type;
            size = 2;
            hash = tag.tag | ((name.hashCode() | type.hashCode()) << 1);
        }

        void setMemberRef(PoolTag poolTag, String owner, CharSequence name, String type) {
            tag = poolTag;
            o1 = owner;
            o2 = name;
            o3 = type;
            size = 3;
            hash = tag.tag | ((owner.hashCode() | name.hashCode() | type.hashCode()) << 1);
        }

        void setInvokeDynamic(int bsmIndex, CharSequence name, String type) {
            tag = PoolTag.CONSTANT_INVOKEDYNAMIC;
            o1 = bsmIndex;
            o2 = name;
            o3 = type;
            size = 3;
            hash = tag.tag | ((bsmIndex | name.hashCode() | type.hashCode()) << 1);
        }

        void setDynamicConstant(int bsmIndex, CharSequence name, String type) {
            tag = PoolTag.CONSTANT_DYNAMIC;
            o1 = bsmIndex;
            o2 = name;
            o3 = type;
            size = 3;
            hash = tag.tag | ((bsmIndex | name.hashCode() | type.hashCode()) << 1);
        }

        void setString(String s) {
            tag = PoolTag.CONSTANT_STRING;
            o1 = s;
            size = 1;
            hash = tag.tag | (s.hashCode() << 1);
        }

        void setInteger(Integer i) {
            tag = PoolTag.CONSTANT_INTEGER;
            o1 = i;
            size = 1;
            hash = tag.tag | (i.hashCode() << 1);
        }

        void setFloat(Float f) {
            tag = PoolTag.CONSTANT_FLOAT;
            o1 = f;
            size = 1;
            hash = tag.tag | (f.hashCode() << 1);
        }

        void setLong(Long l) {
            tag = PoolTag.CONSTANT_LONG;
            o1 = l;
            size = 1;
            hash = tag.tag | (l.hashCode() << 1);
        }

        void setDouble(Double d) {
            tag = PoolTag.CONSTANT_DOUBLE;
            o1 = d;
            size = 1;
            hash = tag.tag | (d.hashCode() << 1);
        }

        void setMethodType(String type) {
            tag = PoolTag.CONSTANT_METHODTYPE;
            o1 = type;
            size = 1;
            hash = tag.tag | (type.hashCode() << 1);
        }

        void setMethodHandle(int bsmKind, String owner, CharSequence name, String type) {
            tag = PoolTag.CONSTANT_METHODHANDLE;
            o1 = bsmKind;
            o2 = owner;
            o3 = name;
            o4 = type;
            size = 4;
            hash = tag.tag | (bsmKind | owner.hashCode() | name.hashCode() | type.hashCode() << 1);
        }

        @Override
        public boolean equals(Object obj) {
            PoolKey that = (PoolKey) obj;
            if (tag != that.tag) return false;
            switch (size) {
                case 1:
                    if (!o1.equals(that.o1)) {
                        return false;
                    }
                    break;
                case 2:
                    if (!o2.equals(that.o2) || !o1.equals(that.o1)) {
                        return false;
                    }
                    break;
                case 3:
                    if (!o3.equals(that.o3) || !o2.equals(that.o2) || !o1.equals(that.o1)) {
                        return false;
                    }
                    break;
                case 4:
                    if (!o4.equals(that.o4) || !o3.equals(that.o3) || !o2.equals(that.o2) || !o1.equals(that.o1)) {
                        return false;
                    }
                    break;
            }
            return true;
        }

        PoolKey dup() {
            PoolKey poolKey = new PoolKey();
            poolKey.tag = tag;
            poolKey.size = size;
            poolKey.hash = hash;
            poolKey.o1 = o1;
            poolKey.o2 = o2;
            poolKey.o3 = o3;
            poolKey.o4 = o4;
            return poolKey;
        }
    }

    static class BsmKey extends AbstractKey<BsmKey> {
        String bsmClass;
        CharSequence bsmName;
        String bsmType;
        List<Integer> bsmArgs;

        void set(String bsmClass, CharSequence bsmName, String bsmType, List<Integer> bsmArgs) {
            this.bsmClass = bsmClass;
            this.bsmName = bsmName;
            this.bsmType = bsmType;
            this.bsmArgs = bsmArgs;
            hash = bsmClass.hashCode() | bsmName.hashCode() | bsmType.hashCode() | Objects.hash(bsmArgs);
        }

        BsmKey dup() {
            BsmKey bsmKey = new BsmKey();
            bsmKey.bsmClass = bsmClass;
            bsmKey.bsmName = bsmName;
            bsmKey.bsmType = bsmType;
            bsmKey.bsmArgs = bsmArgs;
            bsmKey.hash = hash;
            return bsmKey;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof BsmKey) {
                BsmKey that = (BsmKey)obj;
                return Objects.equals(bsmClass, that.bsmClass) &&
                        Objects.equals(bsmName, that.bsmName) &&
                        Objects.equals(bsmType, that.bsmType) &&
                        Objects.deepEquals(bsmArgs, that.bsmArgs);
            } else {
                return false;
            }
        }
    }

    @Override
    public int putClass(S symbol) {
        return putClassInternal(symbolToString.apply(symbol));
    }

    private int putClassInternal(String symbol) {
        key.setClass(symbol);
        PoolKey poolKey = entries.lookup(key);
        if (poolKey == null) {
            poolKey = key.dup();
            int utf8_idx = putUtf8(symbol);
            poolKey.at(currentIndex++);
            entries.enter(poolKey);
            pool.writeByte(PoolTag.CONSTANT_CLASS.tag);
            pool.writeChar(utf8_idx);
        }
        return poolKey.index;
    }

    @Override
    public int putFieldRef(S owner, CharSequence name, T type) {
        return putMemberRef(PoolTag.CONSTANT_FIELDREF, owner, name, type);
    }

    @Override
    public int putMethodRef(S owner, CharSequence name, T type, boolean isInterface) {
        return putMemberRef(isInterface ? PoolTag.CONSTANT_INTERFACEMETHODREF : PoolTag.CONSTANT_METHODREF,
                owner, name, type);
    }

    int putMemberRef(PoolTag poolTag, S owner, CharSequence name, T type) {
        return putMemberRefInternal(poolTag, symbolToString.apply(owner), name, typeToString.apply(type));
    }

    int putMemberRefInternal(PoolTag poolTag, String owner, CharSequence name, String type) {
        key.setMemberRef(poolTag, owner, name, type);
        PoolKey poolKey = entries.lookup(key);
        if (poolKey == null) {
            poolKey = key.dup();
            int owner_idx = putClassInternal(owner);
            int nameAndType_idx = putNameAndType(name, type);
            poolKey.at(currentIndex++);
            entries.enter(poolKey);
            pool.writeByte(poolTag.tag);
            pool.writeChar(owner_idx);
            pool.writeChar(nameAndType_idx);
        }
        return poolKey.index;
    }

    @Override
    public int putInt(int i) {
        key.setInteger(i);
        PoolKey poolKey = entries.lookup(key);
        if (poolKey == null) {
            poolKey = key.dup();
            poolKey.at(currentIndex++);
            entries.enter(poolKey);
            pool.writeByte(PoolTag.CONSTANT_INTEGER.tag);
            pool.writeInt(i);
        }
        return poolKey.index;
    }

    @Override
    public int putFloat(float f) {
        key.setFloat(f);
        PoolKey poolKey = entries.lookup(key);
        if (poolKey == null) {
            poolKey = key.dup();
            poolKey.at(currentIndex++);
            entries.enter(poolKey);
            pool.writeByte(PoolTag.CONSTANT_FLOAT.tag);
            pool.writeFloat(f);
        }
        return poolKey.index;
    }

    @Override
    public int putLong(long l) {
        key.setLong(l);
        PoolKey poolKey = entries.lookup(key);
        if (poolKey == null) {
            poolKey = key.dup();
            poolKey.at(currentIndex++);
            entries.enter(poolKey);
            pool.writeByte(PoolTag.CONSTANT_LONG.tag);
            pool.writeLong(l);
            currentIndex++;
        }
        return poolKey.index;
    }

    @Override
    public int putDouble(double d) {
        key.setDouble(d);
        PoolKey poolKey = entries.lookup(key);
        if (poolKey == null) {
            poolKey = key.dup();
            poolKey.at(currentIndex++);
            entries.enter(poolKey);
            pool.writeByte(PoolTag.CONSTANT_DOUBLE.tag);
            pool.writeDouble(d);
            currentIndex++;
        }
        return poolKey.index;
    }


    @Override
    public int putInvokeDynamic(CharSequence invokedName, T invokedType, S bsmClass, CharSequence bsmName, T bsmType, Consumer<StaticArgListBuilder<S, T, byte[]>> staticArgs) {
        return putInvokeDynamicInternal(invokedName, typeToString.apply(invokedType), symbolToString.apply(bsmClass), bsmName, typeToString.apply(bsmType), staticArgs);
    }

    @Override
    public int putDynamicConstant(CharSequence constName, T constType, S bsmClass, CharSequence bsmName, T bsmType, Consumer<StaticArgListBuilder<S, T, byte[]>> staticArgs) {
        return putDynamicConstantInternal(constName, typeToString.apply(constType), symbolToString.apply(bsmClass), bsmName, typeToString.apply(bsmType), staticArgs);
    }

    private int putInvokeDynamicInternal(CharSequence invokedName, String invokedType, String bsmClass, CharSequence bsmName, String bsmType, Consumer<StaticArgListBuilder<S, T, byte[]>> staticArgs) {
        int bsmIndex = putBsmInternal(bsmClass, bsmName, bsmType, staticArgs);
        key.setInvokeDynamic(bsmIndex, invokedName, invokedType);
        PoolKey poolKey = entries.lookup(key);
        if (poolKey == null) {
            poolKey = key.dup();
            int nameAndType_idx = putNameAndType(invokedName, invokedType);
            poolKey.at(currentIndex++);
            entries.enter(poolKey);
            pool.writeByte(PoolTag.CONSTANT_INVOKEDYNAMIC.tag);
            pool.writeChar(bsmIndex);
            pool.writeChar(nameAndType_idx);
        }
        return poolKey.index;
    }

    private int putDynamicConstantInternal(CharSequence constName, String constType, String bsmClass, CharSequence bsmName, String bsmType, Consumer<StaticArgListBuilder<S, T, byte[]>> staticArgs) {
        int bsmIndex = putBsmInternal(bsmClass, bsmName, bsmType, staticArgs);
        key.setDynamicConstant(bsmIndex, constName, constType);
        PoolKey poolKey = entries.lookup(key);
        if (poolKey == null) {
            poolKey = key.dup();
            int nameAndType_idx = putNameAndType(constName, constType);
            poolKey.at(currentIndex++);
            entries.enter(poolKey);
            pool.writeByte(PoolTag.CONSTANT_DYNAMIC.tag);
            pool.writeChar(bsmIndex);
            pool.writeChar(nameAndType_idx);
        }
        return poolKey.index;
    }

    private int putBsmInternal(String bsmClass, CharSequence bsmName, String bsmType, Consumer<StaticArgListBuilder<S, T, byte[]>> staticArgs) {
        ByteStaticArgListBuilder staticArgsBuilder = new ByteStaticArgListBuilder();
        staticArgs.accept(staticArgsBuilder);
        List<Integer> static_idxs = staticArgsBuilder.indexes;
        bsmKey.set(bsmClass, bsmName, bsmType, static_idxs);
        BsmKey poolKey = bootstraps.lookup(bsmKey);
        if (poolKey == null) {
            poolKey = bsmKey.dup();
            // TODO the BSM could be a static method on an interface
            int bsm_ref = putHandleInternal(MethodHandleInfo.REF_invokeStatic, bsmClass, bsmName, bsmType, false);
            poolKey.at(currentBsmIndex++);
            bootstraps.enter(poolKey);
            bsm_attr.writeChar(bsm_ref);
            bsm_attr.writeChar(static_idxs.size());
            for (int i : static_idxs) {
                bsm_attr.writeChar(i);
            }
        }
        return poolKey.index;
    }
    //where
        class ByteStaticArgListBuilder implements StaticArgListBuilder<S, T, byte[]> {

            List<Integer> indexes = new ArrayList<>();

            public ByteStaticArgListBuilder add(int i) {
                indexes.add(putInt(i));
                return this;
            }
            public ByteStaticArgListBuilder add(float f) {
                indexes.add(putFloat(f));
                return this;
            }
            public ByteStaticArgListBuilder add(long l) {
                indexes.add(putLong(l));
                return this;
            }
            public ByteStaticArgListBuilder add(double d) {
                indexes.add(putDouble(d));
                return this;
            }
            public ByteStaticArgListBuilder add(String s) {
                indexes.add(putString(s));
                return this;
            }
            @Override
            public StaticArgListBuilder<S, T, byte[]> add(int refKind, S owner, CharSequence name, T type) {
                indexes.add(putHandle(refKind, owner, name, type));
                return this;
            }
            public <Z> ByteStaticArgListBuilder add(Z z, ToIntBiFunction<PoolHelper<S, T, byte[]>, Z> poolFunc) {
                indexes.add(poolFunc.applyAsInt(BytePoolHelper.this, z));
                return this;
            }
            public ByteStaticArgListBuilder add(CharSequence constName, T constType, S bsmClass, CharSequence bsmName, T bsmType, Consumer<StaticArgListBuilder<S, T, byte[]>> staticArgs) {
                indexes.add(putDynamicConstant(constName, constType, bsmClass, bsmName, bsmType, staticArgs));
                return this;
            }
        }

    @Override
    public int putMethodType(T s) {
        return putMethodTypeInternal(typeToString.apply(s));
    }

    private int putMethodTypeInternal(String s) {
        key.setMethodType(s);
        PoolKey poolKey = entries.lookup(key);
        if (poolKey == null) {
            poolKey = key.dup();
            int desc_idx = putUtf8(s);
            poolKey.at(currentIndex++);
            entries.enter(poolKey);
            pool.writeByte(PoolTag.CONSTANT_METHODTYPE.tag);
            pool.writeChar(desc_idx);
        }
        return poolKey.index;
    }

    @Override
    public int putHandle(int refKind, S owner, CharSequence name, T type) {
        return putHandleInternal(refKind, symbolToString.apply(owner), name, typeToString.apply(type), false);
    }

    @Override
    public int putHandle(int refKind, S owner, CharSequence name, T type, boolean isInterface) {
        return putHandleInternal(refKind, symbolToString.apply(owner), name, typeToString.apply(type), isInterface);
    }

    private int putHandleInternal(int refKind, String owner, CharSequence name, String type, boolean isInterface) {
        key.setMethodHandle(refKind, owner, name, type);
        PoolKey poolKey = entries.lookup(key);
        if (poolKey == null) {
            poolKey = key.dup();
            int ref_idx = putMemberRefInternal(fromKind(refKind, isInterface), owner, name, type);
            poolKey.at(currentIndex++);
            entries.enter(poolKey);
            pool.writeByte(PoolTag.CONSTANT_METHODHANDLE.tag);
            pool.writeByte(refKind);
            pool.writeChar(ref_idx);
        }
        return poolKey.index;
    }

    PoolTag fromKind(int bsmKind, boolean isInterface) {
        switch (bsmKind) {
            case 1: // REF_getField
            case 2: // REF_getStatic
            case 3: // REF_putField
            case 4: // REF_putStatic
                return PoolTag.CONSTANT_FIELDREF;
            case 5: // REF_invokeVirtual
            case 6: // REF_invokeStatic
            case 7: // REF_invokeSpecial
            case 8: // REF_newInvokeSpecial
            case 9: // REF_invokeInterface
                return isInterface ? PoolTag.CONSTANT_INTERFACEMETHODREF : PoolTag.CONSTANT_METHODREF;
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public int putType(T s) {
        return putUtf8(typeToString.apply(s));
    }

    public int putUtf8(CharSequence s) {
        key.setUtf8(s);
        PoolKey poolKey = entries.lookup(key);
        if (poolKey == null) {
            poolKey = key.dup();
            poolKey.at(currentIndex++);
            entries.enter(poolKey);
            pool.writeByte(PoolTag.CONSTANT_UTF8.tag);
            putUTF8Internal(s);
        }
        return poolKey.index;
    }

    /**
     * Puts an UTF8 string into this byte vector. The byte vector is
     * automatically enlarged if necessary.
     *
     * @param s a String whose UTF8 encoded length must be less than 65536.
     * @return this byte vector.
     */
    void putUTF8Internal(final CharSequence s) {
        int charLength = s.length();
        if (charLength > 65535) {
            throw new IllegalArgumentException();
        }
        // optimistic algorithm: instead of computing the byte length and then
        // serializing the string (which requires two loops), we assume the byte
        // length is equal to char length (which is the most frequent case), and
        // we start serializing the string right away. During the serialization,
        // if we find that this assumption is wrong, we continue with the
        // general method.
        pool.writeChar(charLength);
        for (int i = 0; i < charLength; ++i) {
            char c = s.charAt(i);
            if (c >= '\001' && c <= '\177') {
                pool.writeByte((byte) c);
            } else {
                encodeUTF8(s, i, 65535);
                break;
            }
        }
    }

    /**
     * Puts an UTF8 string into this byte vector. The byte vector is
     * automatically enlarged if necessary. The string length is encoded in two
     * bytes before the encoded characters, if there is space for that (i.e. if
     * this.length - i - 2 >= 0).
     *
     * @param s             the String to encode.
     * @param i             the index of the first character to encode. The previous
     *                      characters are supposed to have already been encoded, using
     *                      only one byte per character.
     * @param maxByteLength the maximum byte length of the encoded string, including the
     *                      already encoded characters.
     * @return this byte vector.
     */
    void encodeUTF8(final CharSequence s, int i, int maxByteLength) {
        int charLength = s.length();
        int byteLength = i;
        char c;
        for (int j = i; j < charLength; ++j) {
            c = s.charAt(j);
            if (c >= '\001' && c <= '\177') {
                byteLength++;
            } else if (c > '\u07FF') {
                byteLength += 3;
            } else {
                byteLength += 2;
            }
        }
        if (byteLength > maxByteLength) {
            throw new IllegalArgumentException();
        }
        int byteLengthFinal = byteLength;
        pool.withOffset(pool.offset - i - 2, buf -> buf.writeChar(byteLengthFinal));
        for (int j = i; j < charLength; ++j) {
            c = s.charAt(j);
            if (c >= '\001' && c <= '\177') {
                pool.writeChar((byte) c);
            } else if (c > '\u07FF') {
                pool.writeChar((byte) (0xE0 | c >> 12 & 0xF));
                pool.writeChar((byte) (0x80 | c >> 6 & 0x3F));
                pool.writeChar((byte) (0x80 | c & 0x3F));
            } else {
                pool.writeChar((byte) (0xC0 | c >> 6 & 0x1F));
                pool.writeChar((byte) (0x80 | c & 0x3F));
            }
        }
    }

    @Override
    public int putString(String s) {
        key.setString(s);
        PoolKey poolKey = entries.lookup(key);
        if (poolKey == null) {
            poolKey = key.dup();
            int utf8_index = putUtf8(s);
            poolKey.at(currentIndex++);
            entries.enter(poolKey);
            pool.writeByte(PoolTag.CONSTANT_STRING.tag);
            pool.writeChar(utf8_index);
        }
        return poolKey.index;
    }

    int putNameAndType(CharSequence name, String type) {
        key.setNameAndType(name, type);
        PoolKey poolKey = entries.lookup(key);
        if (poolKey == null) {
            poolKey = key.dup();
            int name_idx = putUtf8(name);
            int type_idx = putUtf8(type);
            poolKey.at(currentIndex++);
            entries.enter(poolKey);
            pool.writeByte(PoolTag.CONSTANT_NAMEANDTYPE.tag);
            pool.writeChar(name_idx);
            pool.writeChar(type_idx);
        }
        return poolKey.index;
    }

    @Override
    public int size() {
        return currentIndex - 1;
    }

    @Override
    public byte[] entries() {
        return pool.bytes();
    }

    <Z extends ClassBuilder<S, T, Z>> void addAttributes(ClassBuilder<S , T, Z> cb) {
        if (currentBsmIndex > 0) {
            GrowableByteBuffer bsmAttrBuf = new GrowableByteBuffer();
            bsmAttrBuf.writeChar(currentBsmIndex);
            bsmAttrBuf.writeBytes(bsm_attr);
            cb.withAttribute("BootstrapMethods", bsmAttrBuf.bytes());
        }
    }
}
