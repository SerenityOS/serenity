/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Map;

import com.sun.tools.classfile.ConstantPool.CONSTANT_Class_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Dynamic_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Double_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Fieldref_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Float_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Integer_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_InterfaceMethodref_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_InvokeDynamic_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Long_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_MethodHandle_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_MethodType_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Methodref_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Module_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_NameAndType_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Package_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_String_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Utf8_info;
import com.sun.tools.classfile.ConstantPool.CPInfo;

/**
 * Rewrites a class file using a map of translations.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class ClassTranslator
        implements ConstantPool.Visitor<ConstantPool.CPInfo,Map<Object,Object>> {
    /**
     * Create a new ClassFile from {@code cf}, such that for all entries
     * {@code k&nbsp;-\&gt;&nbsp;v} in {@code translations},
     * each occurrence of {@code k} in {@code cf} will be replaced by {@code v}.
     * in
     * @param cf the class file to be processed
     * @param translations the set of translations to be applied
     * @return a copy of {@code} with the values in {@code translations} substituted
     */
    public ClassFile translate(ClassFile cf, Map<Object,Object> translations) {
        ClassFile cf2 = (ClassFile) translations.get(cf);
        if (cf2 == null) {
            ConstantPool constant_pool2 = translate(cf.constant_pool, translations);
            Field[] fields2 = translate(cf.fields, cf.constant_pool, translations);
            Method[] methods2 = translateMethods(cf.methods, cf.constant_pool, translations);
            Attributes attributes2 = translateAttributes(cf.attributes, cf.constant_pool,
                    translations);

            if (constant_pool2 == cf.constant_pool &&
                    fields2 == cf.fields &&
                    methods2 == cf.methods &&
                    attributes2 == cf.attributes)
                cf2 = cf;
            else
                cf2 = new ClassFile(
                        cf.magic,
                        cf.minor_version,
                        cf.major_version,
                        constant_pool2,
                        cf.access_flags,
                        cf.this_class,
                        cf.super_class,
                        cf.interfaces,
                        fields2,
                        methods2,
                        attributes2);
            translations.put(cf, cf2);
        }
        return cf2;
    }

    ConstantPool translate(ConstantPool cp, Map<Object,Object> translations) {
        ConstantPool cp2 = (ConstantPool) translations.get(cp);
        if (cp2 == null) {
            ConstantPool.CPInfo[] pool2 = new ConstantPool.CPInfo[cp.size()];
            boolean eq = true;
            for (int i = 0; i < cp.size(); ) {
                ConstantPool.CPInfo cpInfo;
                try {
                    cpInfo = cp.get(i);
                } catch (ConstantPool.InvalidIndex e) {
                    throw new IllegalStateException(e);
                }
                ConstantPool.CPInfo cpInfo2 = translate(cpInfo, translations);
                eq &= (cpInfo == cpInfo2);
                pool2[i] = cpInfo2;
                if (cpInfo.getTag() != cpInfo2.getTag())
                    throw new IllegalStateException();
                i += cpInfo.size();
            }

            if (eq)
                cp2 = cp;
            else
                cp2 = new ConstantPool(pool2);

            translations.put(cp, cp2);
        }
        return cp2;
    }

    ConstantPool.CPInfo translate(ConstantPool.CPInfo cpInfo, Map<Object,Object> translations) {
        ConstantPool.CPInfo cpInfo2 = (ConstantPool.CPInfo) translations.get(cpInfo);
        if (cpInfo2 == null) {
            cpInfo2 = cpInfo.accept(this, translations);
            translations.put(cpInfo, cpInfo2);
        }
        return cpInfo2;
    }

    Field[] translate(Field[] fields, ConstantPool constant_pool, Map<Object,Object> translations) {
        Field[] fields2 = (Field[]) translations.get(fields);
        if (fields2 == null) {
            fields2 = new Field[fields.length];
            for (int i = 0; i < fields.length; i++)
                fields2[i] = translate(fields[i], constant_pool, translations);
            if (equal(fields, fields2))
                fields2 = fields;
            translations.put(fields, fields2);
        }
        return fields2;
    }

    Field translate(Field field, ConstantPool constant_pool, Map<Object,Object> translations) {
        Field field2 = (Field) translations.get(field);
        if (field2 == null) {
            Attributes attributes2 = translateAttributes(field.attributes, constant_pool,
                    translations);

            if (attributes2 == field.attributes)
                field2 = field;
            else
                field2 = new Field(
                        field.access_flags,
                        field.name_index,
                        field.descriptor,
                        attributes2);
            translations.put(field, field2);
        }
        return field2;
    }

    Method[] translateMethods(Method[] methods, ConstantPool constant_pool, Map<Object,Object> translations) {
        Method[] methods2 = (Method[]) translations.get(methods);
        if (methods2 == null) {
            methods2 = new Method[methods.length];
            for (int i = 0; i < methods.length; i++)
                methods2[i] = translate(methods[i], constant_pool, translations);
            if (equal(methods, methods2))
                methods2 = methods;
            translations.put(methods, methods2);
        }
        return methods2;
    }

    Method translate(Method method, ConstantPool constant_pool, Map<Object,Object> translations) {
        Method method2 = (Method) translations.get(method);
        if (method2 == null) {
            Attributes attributes2 = translateAttributes(method.attributes, constant_pool,
                    translations);

            if (attributes2 == method.attributes)
                method2 = method;
            else
                method2 = new Method(
                        method.access_flags,
                        method.name_index,
                        method.descriptor,
                        attributes2);
            translations.put(method, method2);
        }
        return method2;
    }

    Attributes translateAttributes(Attributes attributes,
            ConstantPool constant_pool, Map<Object,Object> translations) {
        Attributes attributes2 = (Attributes) translations.get(attributes);
        if (attributes2 == null) {
            Attribute[] attrArray2 = new Attribute[attributes.size()];
            ConstantPool constant_pool2 = translate(constant_pool, translations);
            boolean attrsEqual = true;
            for (int i = 0; i < attributes.size(); i++) {
                Attribute attr = attributes.get(i);
                Attribute attr2 = translate(attr, translations);
                if (attr2 != attr)
                    attrsEqual = false;
                attrArray2[i] = attr2;
            }
            if ((constant_pool2 == constant_pool) && attrsEqual)
                attributes2 = attributes;
            else
                attributes2 = new Attributes(constant_pool2, attrArray2);
            translations.put(attributes, attributes2);
        }
        return attributes2;
    }

    Attribute translate(Attribute attribute, Map<Object,Object> translations) {
        Attribute attribute2 = (Attribute) translations.get(attribute);
        if (attribute2 == null) {
            attribute2 = attribute; // don't support translation within attributes yet
                                    // (what about Code attribute)
            translations.put(attribute, attribute2);
        }
        return attribute2;
    }

    private static <T> boolean equal(T[] a1, T[] a2) {
        if (a1 == null || a2 == null)
            return (a1 == a2);
        if (a1.length != a2.length)
            return false;
        for (int i = 0; i < a1.length; i++) {
            if (a1[i] != a2[i])
                return false;
        }
        return true;
    }

    @Override
    public CPInfo visitClass(CONSTANT_Class_info info, Map<Object, Object> translations) {
        CONSTANT_Class_info info2 = (CONSTANT_Class_info) translations.get(info);
        if (info2 == null) {
            ConstantPool cp2 = translate(info.cp, translations);
            if (cp2 == info.cp)
                info2 = info;
            else
                info2 = new CONSTANT_Class_info(cp2, info.name_index);
            translations.put(info, info2);
        }
        return info;
    }

    @Override
    public CPInfo visitDouble(CONSTANT_Double_info info, Map<Object, Object> translations) {
        CONSTANT_Double_info info2 = (CONSTANT_Double_info) translations.get(info);
        if (info2 == null) {
            info2 = info;
            translations.put(info, info2);
        }
        return info;
    }

    @Override
    public CPInfo visitFieldref(CONSTANT_Fieldref_info info, Map<Object, Object> translations) {
        CONSTANT_Fieldref_info info2 = (CONSTANT_Fieldref_info) translations.get(info);
        if (info2 == null) {
            ConstantPool cp2 = translate(info.cp, translations);
            if (cp2 == info.cp)
                info2 = info;
            else
                info2 = new CONSTANT_Fieldref_info(cp2, info.class_index, info.name_and_type_index);
            translations.put(info, info2);
        }
        return info;
    }

    @Override
    public CPInfo visitFloat(CONSTANT_Float_info info, Map<Object, Object> translations) {
        CONSTANT_Float_info info2 = (CONSTANT_Float_info) translations.get(info);
        if (info2 == null) {
            info2 = info;
            translations.put(info, info2);
        }
        return info;
    }

    @Override
    public CPInfo visitInteger(CONSTANT_Integer_info info, Map<Object, Object> translations) {
        CONSTANT_Integer_info info2 = (CONSTANT_Integer_info) translations.get(info);
        if (info2 == null) {
            info2 = info;
            translations.put(info, info2);
        }
        return info;
    }

    @Override
    public CPInfo visitInterfaceMethodref(CONSTANT_InterfaceMethodref_info info, Map<Object, Object> translations) {
        CONSTANT_InterfaceMethodref_info info2 = (CONSTANT_InterfaceMethodref_info) translations.get(info);
        if (info2 == null) {
            ConstantPool cp2 = translate(info.cp, translations);
            if (cp2 == info.cp)
                info2 = info;
            else
                info2 = new CONSTANT_InterfaceMethodref_info(cp2, info.class_index, info.name_and_type_index);
            translations.put(info, info2);
        }
        return info;
    }

    @Override
    public CPInfo visitInvokeDynamic(CONSTANT_InvokeDynamic_info info, Map<Object, Object> translations) {
        CONSTANT_InvokeDynamic_info info2 = (CONSTANT_InvokeDynamic_info) translations.get(info);
        if (info2 == null) {
            ConstantPool cp2 = translate(info.cp, translations);
            if (cp2 == info.cp) {
                info2 = info;
            } else {
                info2 = new CONSTANT_InvokeDynamic_info(cp2, info.bootstrap_method_attr_index, info.name_and_type_index);
            }
            translations.put(info, info2);
        }
        return info;
    }

    public CPInfo visitDynamicConstant(CONSTANT_Dynamic_info info, Map<Object, Object> translations) {
        CONSTANT_Dynamic_info info2 = (CONSTANT_Dynamic_info) translations.get(info);
        if (info2 == null) {
            ConstantPool cp2 = translate(info.cp, translations);
            if (cp2 == info.cp) {
                info2 = info;
            } else {
                info2 = new CONSTANT_Dynamic_info(cp2, info.bootstrap_method_attr_index, info.name_and_type_index);
            }
            translations.put(info, info2);
        }
        return info;
    }

    @Override
    public CPInfo visitLong(CONSTANT_Long_info info, Map<Object, Object> translations) {
        CONSTANT_Long_info info2 = (CONSTANT_Long_info) translations.get(info);
        if (info2 == null) {
            info2 = info;
            translations.put(info, info2);
        }
        return info;
    }

    @Override
    public CPInfo visitMethodref(CONSTANT_Methodref_info info, Map<Object, Object> translations) {
        CONSTANT_Methodref_info info2 = (CONSTANT_Methodref_info) translations.get(info);
        if (info2 == null) {
            ConstantPool cp2 = translate(info.cp, translations);
            if (cp2 == info.cp)
                info2 = info;
            else
                info2 = new CONSTANT_Methodref_info(cp2, info.class_index, info.name_and_type_index);
            translations.put(info, info2);
        }
        return info;
    }

    @Override
    public CPInfo visitMethodHandle(CONSTANT_MethodHandle_info info, Map<Object, Object> translations) {
        CONSTANT_MethodHandle_info info2 = (CONSTANT_MethodHandle_info) translations.get(info);
        if (info2 == null) {
            ConstantPool cp2 = translate(info.cp, translations);
            if (cp2 == info.cp) {
                info2 = info;
            } else {
                info2 = new CONSTANT_MethodHandle_info(cp2, info.reference_kind, info.reference_index);
            }
            translations.put(info, info2);
        }
        return info;
    }

    @Override
    public CPInfo visitMethodType(CONSTANT_MethodType_info info, Map<Object, Object> translations) {
        CONSTANT_MethodType_info info2 = (CONSTANT_MethodType_info) translations.get(info);
        if (info2 == null) {
            ConstantPool cp2 = translate(info.cp, translations);
            if (cp2 == info.cp) {
                info2 = info;
            } else {
                info2 = new CONSTANT_MethodType_info(cp2, info.descriptor_index);
            }
            translations.put(info, info2);
        }
        return info;
    }

    @Override
    public CPInfo visitModule(CONSTANT_Module_info info, Map<Object, Object> translations) {
        CONSTANT_Module_info info2 = (CONSTANT_Module_info) translations.get(info);
        if (info2 == null) {
            ConstantPool cp2 = translate(info.cp, translations);
            if (cp2 == info.cp)
                info2 = info;
            else
                info2 = new CONSTANT_Module_info(cp2, info.name_index);
            translations.put(info, info2);
        }
        return info;
    }

    @Override
    public CPInfo visitNameAndType(CONSTANT_NameAndType_info info, Map<Object, Object> translations) {
        CONSTANT_NameAndType_info info2 = (CONSTANT_NameAndType_info) translations.get(info);
        if (info2 == null) {
            ConstantPool cp2 = translate(info.cp, translations);
            if (cp2 == info.cp)
                info2 = info;
            else
                info2 = new CONSTANT_NameAndType_info(cp2, info.name_index, info.type_index);
            translations.put(info, info2);
        }
        return info;
    }

    @Override
    public CPInfo visitPackage(CONSTANT_Package_info info, Map<Object, Object> translations) {
        CONSTANT_Package_info info2 = (CONSTANT_Package_info) translations.get(info);
        if (info2 == null) {
            ConstantPool cp2 = translate(info.cp, translations);
            if (cp2 == info.cp)
                info2 = info;
            else
                info2 = new CONSTANT_Package_info(cp2, info.name_index);
            translations.put(info, info2);
        }
        return info;
    }

    @Override
    public CPInfo visitString(CONSTANT_String_info info, Map<Object, Object> translations) {
        CONSTANT_String_info info2 = (CONSTANT_String_info) translations.get(info);
        if (info2 == null) {
            ConstantPool cp2 = translate(info.cp, translations);
            if (cp2 == info.cp)
                info2 = info;
            else
                info2 = new CONSTANT_String_info(cp2, info.string_index);
            translations.put(info, info2);
        }
        return info;
    }

    @Override
    public CPInfo visitUtf8(CONSTANT_Utf8_info info, Map<Object, Object> translations) {
        CONSTANT_Utf8_info info2 = (CONSTANT_Utf8_info) translations.get(info);
        if (info2 == null) {
            info2 = info;
            translations.put(info, info2);
        }
        return info;
    }

}
