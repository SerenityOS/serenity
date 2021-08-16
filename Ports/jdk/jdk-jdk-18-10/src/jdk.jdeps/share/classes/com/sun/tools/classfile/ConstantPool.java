/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.classfile;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * See JVMS, section 4.5.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class ConstantPool {

    public static class InvalidIndex extends ConstantPoolException {
        private static final long serialVersionUID = -4350294289300939730L;
        InvalidIndex(int index) {
            super(index);
        }

        @Override
        public String getMessage() {
            // i18n
            return "invalid index #" + index;
        }
    }

    public static class UnexpectedEntry extends ConstantPoolException {
        private static final long serialVersionUID = 6986335935377933211L;
        UnexpectedEntry(int index, int expected_tag, int found_tag) {
            super(index);
            this.expected_tag = expected_tag;
            this.found_tag = found_tag;
        }

        @Override
        public String getMessage() {
            // i18n?
            return "unexpected entry at #" + index + " -- expected tag " + expected_tag + ", found " + found_tag;
        }

        public final int expected_tag;
        public final int found_tag;
    }

    public static class InvalidEntry extends ConstantPoolException {
        private static final long serialVersionUID = 1000087545585204447L;
        InvalidEntry(int index, int tag) {
            super(index);
            this.tag = tag;
        }

        @Override
        public String getMessage() {
            // i18n?
            return "unexpected tag at #" + index + ": " + tag;
        }

        public final int tag;
    }

    public static class EntryNotFound extends ConstantPoolException {
        private static final long serialVersionUID = 2885537606468581850L;
        EntryNotFound(Object value) {
            super(-1);
            this.value = value;
        }

        @Override
        public String getMessage() {
            // i18n?
            return "value not found: " + value;
        }

        public final Object value;
    }

    public static final int CONSTANT_Utf8 = 1;
    public static final int CONSTANT_Integer = 3;
    public static final int CONSTANT_Float = 4;
    public static final int CONSTANT_Long = 5;
    public static final int CONSTANT_Double = 6;
    public static final int CONSTANT_Class = 7;
    public static final int CONSTANT_String = 8;
    public static final int CONSTANT_Fieldref = 9;
    public static final int CONSTANT_Methodref = 10;
    public static final int CONSTANT_InterfaceMethodref = 11;
    public static final int CONSTANT_NameAndType = 12;
    public static final int CONSTANT_MethodHandle = 15;
    public static final int CONSTANT_MethodType = 16;
    public static final int CONSTANT_Dynamic = 17;
    public static final int CONSTANT_InvokeDynamic = 18;
    public static final int CONSTANT_Module = 19;
    public static final int CONSTANT_Package = 20;

    public static enum RefKind {
        REF_getField(1),
        REF_getStatic(2),
        REF_putField(3),
        REF_putStatic(4),
        REF_invokeVirtual(5),
        REF_invokeStatic(6),
        REF_invokeSpecial(7),
        REF_newInvokeSpecial(8),
        REF_invokeInterface(9);

        public final int tag;

        RefKind(int tag) {
            this.tag = tag;
        }

        static RefKind getRefkind(int tag) {
            switch(tag) {
                case 1:
                    return REF_getField;
                case 2:
                    return REF_getStatic;
                case 3:
                    return REF_putField;
                case 4:
                    return REF_putStatic;
                case 5:
                    return REF_invokeVirtual;
                case 6:
                    return REF_invokeStatic;
                case 7:
                    return REF_invokeSpecial;
                case 8:
                    return REF_newInvokeSpecial;
                case 9:
                    return REF_invokeInterface;
                default:
                    return null;
            }
        }
    }

    ConstantPool(ClassReader cr) throws IOException, InvalidEntry {
        int count = cr.readUnsignedShort();
        pool = new CPInfo[count];
        for (int i = 1; i < count; i++) {
            int tag = cr.readUnsignedByte();
            switch (tag) {
            case CONSTANT_Class:
                pool[i] = new CONSTANT_Class_info(this, cr);
                break;

            case CONSTANT_Double:
                pool[i] = new CONSTANT_Double_info(cr);
                i++;
                break;

            case CONSTANT_Fieldref:
                pool[i] = new CONSTANT_Fieldref_info(this, cr);
                break;

            case CONSTANT_Float:
                pool[i] = new CONSTANT_Float_info(cr);
                break;

            case CONSTANT_Integer:
                pool[i] = new CONSTANT_Integer_info(cr);
                break;

            case CONSTANT_InterfaceMethodref:
                pool[i] = new CONSTANT_InterfaceMethodref_info(this, cr);
                break;

            case CONSTANT_InvokeDynamic:
                pool[i] = new CONSTANT_InvokeDynamic_info(this, cr);
                break;

            case CONSTANT_Dynamic:
                pool[i] = new CONSTANT_Dynamic_info(this, cr);
                break;

            case CONSTANT_Long:
                pool[i] = new CONSTANT_Long_info(cr);
                i++;
                break;

            case CONSTANT_MethodHandle:
                pool[i] = new CONSTANT_MethodHandle_info(this, cr);
                break;

            case CONSTANT_MethodType:
                pool[i] = new CONSTANT_MethodType_info(this, cr);
                break;

            case CONSTANT_Methodref:
                pool[i] = new CONSTANT_Methodref_info(this, cr);
                break;

            case CONSTANT_Module:
                pool[i] = new CONSTANT_Module_info(this, cr);
                break;

            case CONSTANT_NameAndType:
                pool[i] = new CONSTANT_NameAndType_info(this, cr);
                break;

            case CONSTANT_Package:
                pool[i] = new CONSTANT_Package_info(this, cr);
                break;

            case CONSTANT_String:
                pool[i] = new CONSTANT_String_info(this, cr);
                break;

            case CONSTANT_Utf8:
                pool[i] = new CONSTANT_Utf8_info(cr);
                break;

            default:
                throw new InvalidEntry(i, tag);
            }
        }
    }

    public ConstantPool(CPInfo[] pool) {
        this.pool = pool;
    }

    public int size() {
        return pool.length;
    }

    public int byteLength() {
        int length = 2;
        for (int i = 1; i < size(); ) {
            CPInfo cpInfo = pool[i];
            length += cpInfo.byteLength();
            i += cpInfo.size();
        }
        return length;
    }

    public CPInfo get(int index) throws InvalidIndex {
        if (index <= 0 || index >= pool.length)
            throw new InvalidIndex(index);
        CPInfo info = pool[index];
        if (info == null) {
            // this occurs for indices referencing the "second half" of an
            // 8 byte constant, such as CONSTANT_Double or CONSTANT_Long
            throw new InvalidIndex(index);
        }
        return pool[index];
    }

    private CPInfo get(int index, int expected_type) throws InvalidIndex, UnexpectedEntry {
        CPInfo info = get(index);
        if (info.getTag() != expected_type)
            throw new UnexpectedEntry(index, expected_type, info.getTag());
        return info;
    }

    public CONSTANT_Utf8_info getUTF8Info(int index) throws InvalidIndex, UnexpectedEntry {
        return ((CONSTANT_Utf8_info) get(index, CONSTANT_Utf8));
    }

    public CONSTANT_Class_info getClassInfo(int index) throws InvalidIndex, UnexpectedEntry {
        return ((CONSTANT_Class_info) get(index, CONSTANT_Class));
    }

    public CONSTANT_Module_info getModuleInfo(int index) throws InvalidIndex, UnexpectedEntry {
        return ((CONSTANT_Module_info) get(index, CONSTANT_Module));
    }

    public CONSTANT_NameAndType_info getNameAndTypeInfo(int index) throws InvalidIndex, UnexpectedEntry {
        return ((CONSTANT_NameAndType_info) get(index, CONSTANT_NameAndType));
    }

    public CONSTANT_Package_info getPackageInfo(int index) throws InvalidIndex, UnexpectedEntry {
        return ((CONSTANT_Package_info) get(index, CONSTANT_Package));
    }

    public String getUTF8Value(int index) throws InvalidIndex, UnexpectedEntry {
        return getUTF8Info(index).value;
    }

    public int getUTF8Index(String value) throws EntryNotFound {
        for (int i = 1; i < pool.length; i++) {
            CPInfo info = pool[i];
            if (info instanceof CONSTANT_Utf8_info &&
                    ((CONSTANT_Utf8_info) info).value.equals(value))
                return i;
        }
        throw new EntryNotFound(value);
    }

    public Iterable<CPInfo> entries() {
        return () -> new Iterator<CPInfo>() {

            public boolean hasNext() {
                return next < pool.length;
            }

            public CPInfo next() {
                current = pool[next];
                switch (current.getTag()) {
                    case CONSTANT_Double:
                    case CONSTANT_Long:
                        next += 2;
                        break;
                    default:
                        next += 1;
                }
                return current;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

            private CPInfo current;
            private int next = 1;

        };
    }

    private CPInfo[] pool;

    public interface Visitor<R,P> {
        R visitClass(CONSTANT_Class_info info, P p);
        R visitDouble(CONSTANT_Double_info info, P p);
        R visitFieldref(CONSTANT_Fieldref_info info, P p);
        R visitFloat(CONSTANT_Float_info info, P p);
        R visitInteger(CONSTANT_Integer_info info, P p);
        R visitInterfaceMethodref(CONSTANT_InterfaceMethodref_info info, P p);
        R visitInvokeDynamic(CONSTANT_InvokeDynamic_info info, P p);
        R visitDynamicConstant(CONSTANT_Dynamic_info info, P p);
        R visitLong(CONSTANT_Long_info info, P p);
        R visitMethodref(CONSTANT_Methodref_info info, P p);
        R visitMethodHandle(CONSTANT_MethodHandle_info info, P p);
        R visitMethodType(CONSTANT_MethodType_info info, P p);
        R visitModule(CONSTANT_Module_info info, P p);
        R visitNameAndType(CONSTANT_NameAndType_info info, P p);
        R visitPackage(CONSTANT_Package_info info, P p);
        R visitString(CONSTANT_String_info info, P p);
        R visitUtf8(CONSTANT_Utf8_info info, P p);
    }

    public static abstract class CPInfo {
        CPInfo() {
            this.cp = null;
        }

        CPInfo(ConstantPool cp) {
            this.cp = cp;
        }

        public abstract int getTag();

        /** The number of slots in the constant pool used by this entry.
         * 2 for CONSTANT_Double and CONSTANT_Long; 1 for everything else. */
        public int size() {
            return 1;
        }

        public abstract int byteLength();

        public abstract <R,D> R accept(Visitor<R,D> visitor, D data);

        protected final ConstantPool cp;
    }

    public static abstract class CPRefInfo extends CPInfo {
        protected CPRefInfo(ConstantPool cp, ClassReader cr, int tag) throws IOException {
            super(cp);
            this.tag = tag;
            class_index = cr.readUnsignedShort();
            name_and_type_index = cr.readUnsignedShort();
        }

        protected CPRefInfo(ConstantPool cp, int tag, int class_index, int name_and_type_index) {
            super(cp);
            this.tag = tag;
            this.class_index = class_index;
            this.name_and_type_index = name_and_type_index;
        }

        public int getTag() {
            return tag;
        }

        public int byteLength() {
            return 5;
        }

        public CONSTANT_Class_info getClassInfo() throws ConstantPoolException {
            return cp.getClassInfo(class_index);
        }

        public String getClassName() throws ConstantPoolException {
            return cp.getClassInfo(class_index).getName();
        }

        public CONSTANT_NameAndType_info getNameAndTypeInfo() throws ConstantPoolException {
            return cp.getNameAndTypeInfo(name_and_type_index);
        }

        public final int tag;
        public final int class_index;
        public final int name_and_type_index;
    }

    public static class CONSTANT_Class_info extends CPInfo {
        CONSTANT_Class_info(ConstantPool cp, ClassReader cr) throws IOException {
            super(cp);
            name_index = cr.readUnsignedShort();
        }

        public CONSTANT_Class_info(ConstantPool cp, int name_index) {
            super(cp);
            this.name_index = name_index;
        }

        public int getTag() {
            return CONSTANT_Class;
        }

        public int  byteLength() {
            return 3;
        }

        /**
         * Get the raw value of the class referenced by this constant pool entry.
         * This will either be the name of the class, in internal form, or a
         * descriptor for an array class.
         * @return the raw value of the class
         */
        public String getName() throws ConstantPoolException {
            return cp.getUTF8Value(name_index);
        }

        /**
         * If this constant pool entry identifies either a class or interface type,
         * or a possibly multi-dimensional array of a class of interface type,
         * return the name of the class or interface in internal form. Otherwise,
         * (i.e. if this is a possibly multi-dimensional array of a primitive type),
         * return null.
         * @return the base class or interface name
         */
        public String getBaseName() throws ConstantPoolException {
            String name = getName();
            if (name.startsWith("[")) {
                int index = name.indexOf("[L");
                if (index == -1)
                    return null;
                return name.substring(index + 2, name.length() - 1);
            } else
                return name;
        }

        public int getDimensionCount() throws ConstantPoolException {
            String name = getName();
            int count = 0;
            while (name.charAt(count) == '[')
                count++;
            return count;
        }

        @Override
        public String toString() {
            return "CONSTANT_Class_info[name_index: " + name_index + "]";
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitClass(this, data);
        }

        public final int name_index;
    }

    public static class CONSTANT_Double_info extends CPInfo {
        CONSTANT_Double_info(ClassReader cr) throws IOException {
            value = cr.readDouble();
        }

        public CONSTANT_Double_info(double value) {
            this.value = value;
        }

        public int getTag() {
            return CONSTANT_Double;
        }

        public int  byteLength() {
            return 9;
        }

        @Override
        public int size() {
            return 2;
        }

        @Override
        public String toString() {
            return "CONSTANT_Double_info[value: " + value + "]";
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitDouble(this, data);
        }

        public final double value;
    }

    public static class CONSTANT_Fieldref_info extends CPRefInfo {
        CONSTANT_Fieldref_info(ConstantPool cp, ClassReader cr) throws IOException {
            super(cp, cr, CONSTANT_Fieldref);
        }

        public CONSTANT_Fieldref_info(ConstantPool cp, int class_index, int name_and_type_index) {
            super(cp, CONSTANT_Fieldref, class_index, name_and_type_index);
        }

        @Override
        public String toString() {
            return "CONSTANT_Fieldref_info[class_index: " + class_index + ", name_and_type_index: " + name_and_type_index + "]";
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitFieldref(this, data);
        }
    }

    public static class CONSTANT_Float_info extends CPInfo {
        CONSTANT_Float_info(ClassReader cr) throws IOException {
            value = cr.readFloat();
        }

        public CONSTANT_Float_info(float value) {
            this.value = value;
        }

        public int getTag() {
            return CONSTANT_Float;
        }

        public int byteLength() {
            return 5;
        }

        @Override
        public String toString() {
            return "CONSTANT_Float_info[value: " + value + "]";
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitFloat(this, data);
        }

        public final float value;
    }

    public static class CONSTANT_Integer_info extends CPInfo {
        CONSTANT_Integer_info(ClassReader cr) throws IOException {
            value = cr.readInt();
        }

        public CONSTANT_Integer_info(int value) {
            this.value = value;
        }

        public int getTag() {
            return CONSTANT_Integer;
        }

        public int byteLength() {
            return 5;
        }

        @Override
        public String toString() {
            return "CONSTANT_Integer_info[value: " + value + "]";
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitInteger(this, data);
        }

        public final int value;
    }

    public static class CONSTANT_InterfaceMethodref_info extends CPRefInfo {
        CONSTANT_InterfaceMethodref_info(ConstantPool cp, ClassReader cr) throws IOException {
            super(cp, cr, CONSTANT_InterfaceMethodref);
        }

        public CONSTANT_InterfaceMethodref_info(ConstantPool cp, int class_index, int name_and_type_index) {
            super(cp, CONSTANT_InterfaceMethodref, class_index, name_and_type_index);
        }

        @Override
        public String toString() {
            return "CONSTANT_InterfaceMethodref_info[class_index: " + class_index + ", name_and_type_index: " + name_and_type_index + "]";
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitInterfaceMethodref(this, data);
        }
    }

    public static class CONSTANT_InvokeDynamic_info extends CPInfo {
        CONSTANT_InvokeDynamic_info(ConstantPool cp, ClassReader cr) throws IOException {
            super(cp);
            bootstrap_method_attr_index = cr.readUnsignedShort();
            name_and_type_index = cr.readUnsignedShort();
        }

        public CONSTANT_InvokeDynamic_info(ConstantPool cp, int bootstrap_method_index, int name_and_type_index) {
            super(cp);
            this.bootstrap_method_attr_index = bootstrap_method_index;
            this.name_and_type_index = name_and_type_index;
        }

        public int getTag() {
            return CONSTANT_InvokeDynamic;
        }

        public int byteLength() {
            return 5;
        }

        @Override
        public String toString() {
            return "CONSTANT_InvokeDynamic_info[bootstrap_method_index: " + bootstrap_method_attr_index + ", name_and_type_index: " + name_and_type_index + "]";
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitInvokeDynamic(this, data);
        }

        public CONSTANT_NameAndType_info getNameAndTypeInfo() throws ConstantPoolException {
            return cp.getNameAndTypeInfo(name_and_type_index);
        }

        public final int bootstrap_method_attr_index;
        public final int name_and_type_index;
    }

    public static class CONSTANT_Long_info extends CPInfo {
        CONSTANT_Long_info(ClassReader cr) throws IOException {
            value = cr.readLong();
        }

        public CONSTANT_Long_info(long value) {
            this.value = value;
        }

        public int getTag() {
            return CONSTANT_Long;
        }

        @Override
        public int size() {
            return 2;
        }

        public int byteLength() {
            return 9;
        }

        @Override
        public String toString() {
            return "CONSTANT_Long_info[value: " + value + "]";
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitLong(this, data);
        }

        public final long value;
    }

    public static class CONSTANT_MethodHandle_info extends CPInfo {
        CONSTANT_MethodHandle_info(ConstantPool cp, ClassReader cr) throws IOException {
            super(cp);
            reference_kind =  RefKind.getRefkind(cr.readUnsignedByte());
            reference_index = cr.readUnsignedShort();
        }

        public CONSTANT_MethodHandle_info(ConstantPool cp, RefKind ref_kind, int member_index) {
            super(cp);
            this.reference_kind = ref_kind;
            this.reference_index = member_index;
        }

        public int getTag() {
            return CONSTANT_MethodHandle;
        }

        public int byteLength() {
            return 4;
        }

        @Override
        public String toString() {
            return "CONSTANT_MethodHandle_info[ref_kind: " + reference_kind + ", member_index: " + reference_index + "]";
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitMethodHandle(this, data);
        }

        public CPRefInfo getCPRefInfo() throws ConstantPoolException {
            int expected = CONSTANT_Methodref;
            int actual = cp.get(reference_index).getTag();
            // allow these tag types also:
            switch (actual) {
                case CONSTANT_Fieldref:
                case CONSTANT_InterfaceMethodref:
                    expected = actual;
            }
            return (CPRefInfo) cp.get(reference_index, expected);
        }

        public final RefKind reference_kind;
        public final int reference_index;
    }

    public static class CONSTANT_MethodType_info extends CPInfo {
        CONSTANT_MethodType_info(ConstantPool cp, ClassReader cr) throws IOException {
            super(cp);
            descriptor_index = cr.readUnsignedShort();
        }

        public CONSTANT_MethodType_info(ConstantPool cp, int signature_index) {
            super(cp);
            this.descriptor_index = signature_index;
        }

        public int getTag() {
            return CONSTANT_MethodType;
        }

        public int byteLength() {
            return 3;
        }

        @Override
        public String toString() {
            return "CONSTANT_MethodType_info[signature_index: " + descriptor_index + "]";
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitMethodType(this, data);
        }

        public String getType() throws ConstantPoolException {
            return cp.getUTF8Value(descriptor_index);
        }

        public final int descriptor_index;
    }

    public static class CONSTANT_Methodref_info extends CPRefInfo {
        CONSTANT_Methodref_info(ConstantPool cp, ClassReader cr) throws IOException {
            super(cp, cr, CONSTANT_Methodref);
        }

        public CONSTANT_Methodref_info(ConstantPool cp, int class_index, int name_and_type_index) {
            super(cp, CONSTANT_Methodref, class_index, name_and_type_index);
        }

        @Override
        public String toString() {
            return "CONSTANT_Methodref_info[class_index: " + class_index + ", name_and_type_index: " + name_and_type_index + "]";
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitMethodref(this, data);
        }
    }

    public static class CONSTANT_Module_info extends CPInfo {
        CONSTANT_Module_info(ConstantPool cp, ClassReader cr) throws IOException {
            super(cp);
            name_index = cr.readUnsignedShort();
        }

        public CONSTANT_Module_info(ConstantPool cp, int name_index) {
            super(cp);
            this.name_index = name_index;
        }

        public int getTag() {
            return CONSTANT_Module;
        }

        public int  byteLength() {
            return 3;
        }

        /**
         * Get the raw value of the module name referenced by this constant pool entry.
         * This will be the name of the module.
         * @return the raw value of the module name
         */
        public String getName() throws ConstantPoolException {
            return cp.getUTF8Value(name_index);
        }

        @Override
        public String toString() {
            return "CONSTANT_Module_info[name_index: " + name_index + "]";
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitModule(this, data);
        }

        public final int name_index;
    }

    public static class CONSTANT_NameAndType_info extends CPInfo {
        CONSTANT_NameAndType_info(ConstantPool cp, ClassReader cr) throws IOException {
            super(cp);
            name_index = cr.readUnsignedShort();
            type_index = cr.readUnsignedShort();
        }

        public CONSTANT_NameAndType_info(ConstantPool cp, int name_index, int type_index) {
            super(cp);
            this.name_index = name_index;
            this.type_index = type_index;
        }

        public int getTag() {
            return CONSTANT_NameAndType;
        }

        public int byteLength() {
            return 5;
        }

        public String getName() throws ConstantPoolException {
            return cp.getUTF8Value(name_index);
        }

        public String getType() throws ConstantPoolException {
            return cp.getUTF8Value(type_index);
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitNameAndType(this, data);
        }

        @Override
        public String toString() {
            return "CONSTANT_NameAndType_info[name_index: " + name_index + ", type_index: " + type_index + "]";
        }

        public final int name_index;
        public final int type_index;
    }

    public static class CONSTANT_Dynamic_info extends CPInfo {
        CONSTANT_Dynamic_info(ConstantPool cp, ClassReader cr) throws IOException {
            super(cp);
            bootstrap_method_attr_index = cr.readUnsignedShort();
            name_and_type_index = cr.readUnsignedShort();
        }

        public CONSTANT_Dynamic_info(ConstantPool cp, int bootstrap_method_index, int name_and_type_index) {
            super(cp);
            this.bootstrap_method_attr_index = bootstrap_method_index;
            this.name_and_type_index = name_and_type_index;
        }

        public int getTag() {
            return CONSTANT_Dynamic;
        }

        public int byteLength() {
            return 5;
        }

        @Override
        public String toString() {
            return "CONSTANT_Dynamic_info[bootstrap_method_index: " + bootstrap_method_attr_index + ", name_and_type_index: " + name_and_type_index + "]";
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitDynamicConstant(this, data);
        }

        public CONSTANT_NameAndType_info getNameAndTypeInfo() throws ConstantPoolException {
            return cp.getNameAndTypeInfo(name_and_type_index);
        }

        public final int bootstrap_method_attr_index;
        public final int name_and_type_index;
    }

    public static class CONSTANT_Package_info extends CPInfo {
        CONSTANT_Package_info(ConstantPool cp, ClassReader cr) throws IOException {
            super(cp);
            name_index = cr.readUnsignedShort();
        }

        public CONSTANT_Package_info(ConstantPool cp, int name_index) {
            super(cp);
            this.name_index = name_index;
        }

        public int getTag() {
            return CONSTANT_Package;
        }

        public int  byteLength() {
            return 3;
        }

        /**
         * Get the raw value of the package name referenced by this constant pool entry.
         * This will be the name of the package, in internal form.
         * @return the raw value of the module name
         */
        public String getName() throws ConstantPoolException {
            return cp.getUTF8Value(name_index);
        }

        @Override
        public String toString() {
            return "CONSTANT_Package_info[name_index: " + name_index + "]";
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitPackage(this, data);
        }

        public final int name_index;
    }

    public static class CONSTANT_String_info extends CPInfo {
        CONSTANT_String_info(ConstantPool cp, ClassReader cr) throws IOException {
            super(cp);
            string_index = cr.readUnsignedShort();
        }

        public CONSTANT_String_info(ConstantPool cp, int string_index) {
            super(cp);
            this.string_index = string_index;
        }

        public int getTag() {
            return CONSTANT_String;
        }

        public int byteLength() {
            return 3;
        }

        public String getString() throws ConstantPoolException {
            return cp.getUTF8Value(string_index);
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitString(this, data);
        }

        @Override
        public String toString() {
            return "CONSTANT_String_info[class_index: " + string_index + "]";
        }

        public final int string_index;
    }

    public static class CONSTANT_Utf8_info extends CPInfo {
        CONSTANT_Utf8_info(ClassReader cr) throws IOException {
            value = cr.readUTF();
        }

        public CONSTANT_Utf8_info(String value) {
            this.value = value;
        }

        public int getTag() {
            return CONSTANT_Utf8;
        }

        public int byteLength() {
            class SizeOutputStream extends OutputStream {
                @Override
                public void write(int b) {
                    size++;
                }
                int size;
            }
            SizeOutputStream sizeOut = new SizeOutputStream();
            DataOutputStream out = new DataOutputStream(sizeOut);
            try { out.writeUTF(value); } catch (IOException ignore) { }
            return 1 + sizeOut.size;
        }

        @Override
        public String toString() {
            if (value.length() < 32 && isPrintableAscii(value))
                return "CONSTANT_Utf8_info[value: \"" + value + "\"]";
            else
                return "CONSTANT_Utf8_info[value: (" + value.length() + " chars)]";
        }

        static boolean isPrintableAscii(String s) {
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c < 32 || c >= 127)
                    return false;
            }
            return true;
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitUtf8(this, data);
        }

        public final String value;
    }

}
