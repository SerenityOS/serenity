/*
 * Copyright (c) 2024, Jamie Mansfield <jmansfield@cadixdev.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/FlyString.h>
#include <AK/HashMap.h>
#include <AK/Types.h>
#include <AK/Variant.h>
#include <AK/Vector.h>

namespace Java {

// https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.1-200-E.1
// https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.5-200-A.1
// https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.6-200-A.1
enum AccessFlag : u16 {
    Acc_Public = 0x0001,
    Acc_Private = 0x0002,
    Acc_Protected = 0x0004,
    Acc_Static = 0x0008,
    Acc_Final = 0x0010,
    Acc_Synchronised = 0x0020,
    Acc_Super = 0x0020,
    Acc_Bridge = 0x0040,
    Acc_Volatile = 0x0040,
    Acc_Varargs = 0x0080,
    Acc_Transient = 0x0080,
    Acc_Native = 0x0100,
    Acc_Interface = 0x0200,
    Acc_Abstract = 0x0400,
    Acc_Strict = 0x0800,
    Acc_Synthetic = 0x1000,
    Acc_Annotation = 0x2000,
    Acc_Enum = 0x4000,
    Acc_Module = 0x8000,
};

// https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.4-210
enum ConstantPoolTag : u8 {
    Utf8 = 1,
    Integer = 3,
    Float = 4,
    Long = 5,
    Double = 6,
    Class = 7,
    String = 8,
    FieldRef = 9,
    MethodRef = 10,
    InterfaceMethodRef = 11,
    NameAndType = 12,
    MethodHandle = 15,
    MethodType = 16,
    Dynamic = 17,
    InvokeDynamic = 18,
    Module = 19,
    Package = 20,
};

// https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.4.1
struct ConstantClassInfo {
    u16 name_index;
};

// https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.4.2
struct ConstantFieldRefInfo {
    u16 class_index;
    u16 name_and_type_index;
};

// https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.4.2
struct ConstantMethodRefInfo {
    u16 class_index;
    u16 name_and_type_index;
};

// https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.4.2
struct ConstantInterfaceMethodRefInfo {
    u16 class_index;
    u16 name_and_type_index;
};

// https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.4.3
struct ConstantStringInfo {
    u16 string_index;
};

// https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.4.4
struct ConstantIntegerInfo {
    u32 bytes;
};

// https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.4.4
struct ConstantFloatInfo {
    u32 bytes;
};

// https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.4.5
struct ConstantLongInfo {
    u32 high_bytes;
    u32 low_bytes;
};

// https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.4.5
struct ConstantDoubleInfo {
    u32 high_bytes;
    u32 low_bytes;
};

// https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.4.6
struct ConstantNameAndTypeInfo {
    u16 name_index;
    u16 descriptor_index;
};

// https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.4.7
struct ConstantUtf8Info {
    FlyString value;
};

// https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.4.8
struct ConstantMethodHandleInfo {
    u8 reference_kind;
    u16 reference_index;
};

// https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.4.9
struct ConstantMethodTypeInfo {
    u16 descriptor_index;
};

// https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.4.10
struct ConstantDynamicInfo {
    u16 bootstrap_method_attr_index;
    u16 name_and_type_index;
};

// https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.4.10
struct ConstantInvokeDynamicInfo {
    u16 bootstrap_method_attr_index;
    u16 name_and_type_index;
};

// https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.4.11
struct ConstantModuleInfo {
    u16 name_index;
};

// https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.4.12
struct ConstantPackageInfo {
    u16 name_index;
};

// https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.7
struct AttributeInfo {
    u16 name_index;
    Vector<u8> info;
};

// https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.5
struct FieldInfo {
    u16 access_flags;
    u16 name_index;
    u16 descriptor_index;
    Vector<AttributeInfo> attributes;
};

// https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.6
struct MethodInfo {
    u16 access_flags;
    u16 name_index;
    u16 descriptor_index;
    Vector<AttributeInfo> attributes;
};

using ConstantPoolInfo = Variant<
    ConstantClassInfo,
    ConstantFieldRefInfo,
    ConstantMethodRefInfo,
    ConstantInterfaceMethodRefInfo,
    ConstantStringInfo,
    ConstantIntegerInfo,
    ConstantFloatInfo,
    ConstantLongInfo,
    ConstantDoubleInfo,
    ConstantNameAndTypeInfo,
    ConstantUtf8Info,
    ConstantMethodHandleInfo,
    ConstantMethodTypeInfo,
    ConstantDynamicInfo,
    ConstantInvokeDynamicInfo,
    ConstantModuleInfo,
    ConstantPackageInfo>;

StringView constant_pool_info_to_name(ConstantPoolInfo info)
{
    if (info.has<Java::ConstantClassInfo>()) {
        return "Class"sv;
    }
    if (info.has<Java::ConstantFieldRefInfo>()) {
        return "Fieldref"sv;
    }
    if (info.has<Java::ConstantMethodRefInfo>()) {
        return "Methodref"sv;
    }
    if (info.has<Java::ConstantInterfaceMethodRefInfo>()) {
        return "InterfaceMethodref"sv;
    }
    if (info.has<Java::ConstantStringInfo>()) {
        return "String"sv;
    }
    if (info.has<Java::ConstantIntegerInfo>()) {
        return "Integer"sv;
    }
    if (info.has<Java::ConstantFloatInfo>()) {
        return "Float"sv;
    }
    if (info.has<Java::ConstantLongInfo>()) {
        return "Long"sv;
    }
    if (info.has<Java::ConstantDoubleInfo>()) {
        return "Double"sv;
    }
    if (info.has<Java::ConstantNameAndTypeInfo>()) {
        return "NameAndType"sv;
    }
    if (info.has<Java::ConstantUtf8Info>()) {
        return "Utf8"sv;
    }
    if (info.has<Java::ConstantMethodHandleInfo>()) {
        return "MethodHandle"sv;
    }
    if (info.has<Java::ConstantMethodTypeInfo>()) {
        return "MethodType"sv;
    }
    if (info.has<Java::ConstantDynamicInfo>()) {
        return "Dynamic"sv;
    }
    if (info.has<Java::ConstantInvokeDynamicInfo>()) {
        return "InvokeDynamic"sv;
    }
    if (info.has<Java::ConstantModuleInfo>()) {
        return "Module"sv;
    }
    if (info.has<Java::ConstantPackageInfo>()) {
        return "Package"sv;
    }
    VERIFY_NOT_REACHED();
}

class ConstantPool {
public:
    ConstantPool(HashMap<u16, ConstantPoolInfo> infos)
        : m_infos(infos)
    {
    }

    Optional<ConstantPoolInfo> get(u16 index) const
    {
        return m_infos.get(index - 1);
    }

    u16 size() const
    {
        return m_infos.size();
    }

protected:
    HashMap<u16, ConstantPoolInfo> m_infos;
};

// https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.1
struct ClassFile {
    u16 minor_version;
    u16 major_version;
    ConstantPool constant_pool;
    u16 access_flags;
    u16 this_class;
    u16 super_class;
    Vector<u16> interfaces;
    Vector<FieldInfo> fields;
    Vector<MethodInfo> methods;
    Vector<AttributeInfo> attributes;
};

}
