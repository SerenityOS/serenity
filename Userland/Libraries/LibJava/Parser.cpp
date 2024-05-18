/*
 * Copyright (c) 2024, Jamie Mansfield <jmansfield@cadixdev.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/StringBuilder.h>
#include <LibJava/Parser.h>

namespace Java {

Parser::Parser(ReadonlyBytes bytes)
    : m_reader(bytes)
{
}

ErrorOr<ClassFile> Parser::parse_class_file()
{
    // ClassFile {

    //     u4             magic;
    auto magic_number = m_reader.read<u32>();
    if (magic_number != 0xCAFEBABE)
        return Error::from_string_view("invalid magic number"sv);

    //     u2             minor_version;
    auto minor_version = m_reader.read<u16>();
    //     u2             major_version;
    auto major_version = m_reader.read<u16>();

    if (major_version < 45 || major_version > 66)
        return Error::from_string_view("unsupported major version"sv);

    // For a class file whose major_version is 56 or above, the minor_version must be 0 or 65535.
    if (major_version >= 56 && (minor_version < 0 || minor_version > 65535))
        return Error::from_string_view("invalid minor version"sv);

    //     u2             constant_pool_count;
    auto constant_pool_count = m_reader.read<u16>();

    //     cp_info        constant_pool[constant_pool_count-1];
    HashMap<u16, ConstantPoolInfo> constant_pool;
    for (u16 i = 0; i < (constant_pool_count - 1); i++) {
        auto info = TRY(parse_constant_pool_info());

        if (info.has<ConstantLongInfo>() || info.has<ConstantDoubleInfo>())
            constant_pool.set(i++, info);

        constant_pool.set(i, info);
    }

    //     u2             access_flags;
    auto access_flags = m_reader.read<u16>();

    //     u2             this_class;
    auto this_class = m_reader.read<u16>();

    //     u2             super_class;
    auto super_class = m_reader.read<u16>();

    //     u2             interfaces_count;
    auto interfaces_count = m_reader.read<u16>();

    //     u2             interfaces[interfaces_count];
    Vector<u16> interfaces;
    interfaces.ensure_capacity(interfaces_count);

    //     u2             fields_count;
    auto fields_count = m_reader.read<u16>();

    //     field_info     fields[fields_count];
    Vector<FieldInfo> fields;
    fields.ensure_capacity(fields_count);
    for (u16 i = 0; i < fields_count; i++) {
        fields.append(TRY(parse_field_info()));
    }

    //     u2             methods_count;
    auto methods_count = m_reader.read<u16>();

    //     method_info    methods[methods_count];
    Vector<MethodInfo> methods;
    methods.ensure_capacity(methods_count);
    for (u16 i = 0; i < methods_count; i++) {
        methods.append(TRY(parse_method_info()));
    }

    //     u2             attributes_count;
    auto attributes_count = m_reader.read<u16>();

    //     attribute_info attributes[attributes_count];
    Vector<AttributeInfo> attributes;
    attributes.ensure_capacity(attributes_count);
    for (u16 i = 0; i < attributes_count; i++) {
        attributes.append(TRY(parse_attribute_info()));
    }

    // }

    return ClassFile {
        .minor_version = minor_version,
        .major_version = major_version,
        .constant_pool = constant_pool,
        .access_flags = access_flags,
        .this_class = this_class,
        .super_class = super_class,
        .interfaces = interfaces,
        .fields = fields,
        .methods = methods,
        .attributes = attributes,
    };
}

ErrorOr<ConstantPoolInfo> Parser::parse_constant_pool_info()
{
    //     u1 tag;
    auto tag = m_reader.read<ConstantPoolTag>();

    switch (tag) {
    case Utf8: {
        //     u2 length;
        auto length = m_reader.read<u16>();

        //     u1 bytes[length];
        StringBuilder builder;
        for (u16 i = 0; i < length; i++) {
            builder.append(m_reader.read<u8>());
        }

        return ConstantUtf8Info {
            .value = builder.to_fly_string_without_validation(),
        };
    }
    case Integer: {
        //     u4 bytes;
        auto bytes = m_reader.read<u32>();

        return ConstantIntegerInfo {
            .bytes = bytes,
        };
    }
    case Float: {
        //     u4 bytes;
        auto bytes = m_reader.read<u32>();

        return ConstantFloatInfo {
            .bytes = bytes,
        };
    }
    case Long: {
        //     u4 high_bytes;
        auto high_bytes = m_reader.read<u32>();

        //     u4 low_bytes;
        auto low_bytes = m_reader.read<u32>();

        return ConstantLongInfo {
            .high_bytes = high_bytes,
            .low_bytes = low_bytes,
        };
    }
    case Double: {
        //     u4 high_bytes;
        auto high_bytes = m_reader.read<u32>();

        //     u4 low_bytes;
        auto low_bytes = m_reader.read<u32>();

        return ConstantDoubleInfo {
            .high_bytes = high_bytes,
            .low_bytes = low_bytes,
        };
    }
    case Class: {
        //     u2 name_index;
        auto name_index = m_reader.read<u16>();

        return ConstantClassInfo {
            .name_index = name_index,
        };
    }
    case String: {
        //     u2 string_index;
        auto string_index = m_reader.read<u16>();

        return ConstantStringInfo {
            .string_index = string_index,
        };
    }
    case FieldRef: {
        //     u2 class_index;
        auto class_index = m_reader.read<u16>();

        //     u2 name_and_type_index;
        auto name_and_type_index = m_reader.read<u16>();

        return ConstantFieldRefInfo {
            .class_index = class_index,
            .name_and_type_index = name_and_type_index,
        };
    }
    case MethodRef: {
        //     u2 class_index;
        auto class_index = m_reader.read<u16>();

        //     u2 name_and_type_index;
        auto name_and_type_index = m_reader.read<u16>();

        return ConstantMethodRefInfo {
            .class_index = class_index,
            .name_and_type_index = name_and_type_index,
        };
    }
    case InterfaceMethodRef: {
        //     u2 class_index;
        auto class_index = m_reader.read<u16>();

        //     u2 name_and_type_index;
        auto name_and_type_index = m_reader.read<u16>();

        return ConstantInterfaceMethodRefInfo {
            .class_index = class_index,
            .name_and_type_index = name_and_type_index,
        };
    }
    case NameAndType: {
        //     u2 name_index;
        auto name_index = m_reader.read<u16>();

        //     u2 descriptor_index;
        auto descriptor_index = m_reader.read<u16>();

        return ConstantNameAndTypeInfo {
            .name_index = name_index,
            .descriptor_index = descriptor_index,
        };
    }
    case MethodHandle: {
        //     u1 reference_kind;
        auto reference_kind = m_reader.read<u8>();

        //     u2 reference_index;
        auto reference_index = m_reader.read<u16>();

        return ConstantMethodHandleInfo {
            .reference_kind = reference_kind,
            .reference_index = reference_index,
        };
    }
    case MethodType: {
        //     u2 descriptor_index;
        auto descriptor_index = m_reader.read<u16>();

        return ConstantMethodTypeInfo {
            .descriptor_index = descriptor_index,
        };
    }
    case Dynamic: {
        //     u2 bootstrap_method_attr_index;
        auto bootstrap_method_attr_index = m_reader.read<u16>();

        //     u2 name_and_type_index;
        auto name_and_type_index = m_reader.read<u16>();

        return ConstantDynamicInfo {
            .bootstrap_method_attr_index = bootstrap_method_attr_index,
            .name_and_type_index = name_and_type_index,
        };
    }
    case InvokeDynamic: {
        //     u2 bootstrap_method_attr_index;
        auto bootstrap_method_attr_index = m_reader.read<u16>();

        //     u2 name_and_type_index;
        auto name_and_type_index = m_reader.read<u16>();

        return ConstantInvokeDynamicInfo {
            .bootstrap_method_attr_index = bootstrap_method_attr_index,
            .name_and_type_index = name_and_type_index,
        };
    }
    case Module: {
        //     u2 name_index;
        auto name_index = m_reader.read<u16>();

        return ConstantModuleInfo {
            .name_index = name_index,
        };
    }
    case Package: {
        //     u2 name_index;
        auto name_index = m_reader.read<u16>();

        return ConstantPackageInfo {
            .name_index = name_index,
        };
    }
    default:
        VERIFY_NOT_REACHED();
    }
}

ErrorOr<FieldInfo> Parser::parse_field_info()
{
    auto access_flags = m_reader.read<u16>();
    auto name_index = m_reader.read<u16>();
    auto descriptor_index = m_reader.read<u16>();
    auto attributes_count = m_reader.read<u16>();

    Vector<AttributeInfo> attributes;
    attributes.ensure_capacity(attributes_count);
    for (u16 i = 0; i < attributes_count; i++) {
        attributes.append(TRY(parse_attribute_info()));
    }

    return FieldInfo {
        .access_flags = access_flags,
        .name_index = name_index,
        .descriptor_index = descriptor_index,
        .attributes = attributes,
    };
}

ErrorOr<MethodInfo> Parser::parse_method_info()
{
    auto access_flags = m_reader.read<u16>();
    auto name_index = m_reader.read<u16>();
    auto descriptor_index = m_reader.read<u16>();
    auto attributes_count = m_reader.read<u16>();

    Vector<AttributeInfo> attributes;
    attributes.ensure_capacity(attributes_count);
    for (u16 i = 0; i < attributes_count; i++) {
        attributes.append(TRY(parse_attribute_info()));
    }

    return MethodInfo {
        .access_flags = access_flags,
        .name_index = name_index,
        .descriptor_index = descriptor_index,
        .attributes = attributes,
    };
}

ErrorOr<AttributeInfo> Parser::parse_attribute_info()
{
    auto name_index = m_reader.read<u16>();
    auto length = m_reader.read<u32>();

    Vector<u8> info;
    info.ensure_capacity(length);
    for (u32 i = 0; i < length; i++) {
        info.append(m_reader.read<u8>());
    }

    return AttributeInfo {
        .name_index = name_index,
        .info = info,
    };
}

}
