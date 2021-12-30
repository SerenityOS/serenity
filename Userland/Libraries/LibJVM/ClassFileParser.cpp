/*
 * Copyright (c) 2021, Noah Haasis <haasis_noah@yahoo.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClassFileParser.h"
#include <AK/Endian.h>
#include <AK/TypeCasts.h>

namespace JVM {

ErrorOr<OwnPtr<ClassFile>> ClassFileParser::parse(ByteBuffer buffer)
{
    m_source = move(buffer);
    m_classfile = make<ClassFile>();

    u32 magic = read_u32();
    VERIFY(magic == 0xcafebabe);

    m_classfile->major_version = read_u16();
    m_classfile->minor_version = read_u16();

    parse_constant_pool();

    m_classfile->access_flags = read_u16();

    parse_class_references();

    parse_interfaces();

    parse_fields();

    parse_methods();

    parse_attributes();

    VERIFY((size_t)m_offset == m_source.size());

    m_classfile->class_file_data = move(m_source);

    return move(m_classfile);
}

void ClassFileParser::parse_constant_pool()
{
    u16 constant_pool_count = read_u16() - 1;

    auto constants = FixedArray<ConstantPool::Constant>(constant_pool_count);
    m_classfile->constant_pool.set_constants(constants);
    for (auto& constant : m_classfile->constant_pool.constants())
        constant = parse_constant_info();
}

ConstantPool::Constant ClassFileParser::parse_constant_info()
{
    u8 tag = read_u8();
    switch (tag) {
    case ConstantPool::Constant::Tag::Class: {
        ConstantPool::Class class_constant;
        class_constant.name_index = read_u16() - 1;
        return ConstantPool::Constant(class_constant);
    }
    case ConstantPool::Constant::Tag::Utf8: {
        ConstantPool::Utf8 utf_8;
        utf_8.length = read_u16();
        utf_8.bytes = data_at_offset();
        advance((int)utf_8.length);
        return ConstantPool::Constant(utf_8);
    }
    case ConstantPool::Constant::Tag::NameAndType: {
        ConstantPool::NameAndType name_and_type;
        name_and_type.name_index = read_u16() - 1;
        name_and_type.descriptor_index = read_u16() - 1;
        return ConstantPool::Constant(name_and_type);
    }
    case ConstantPool::Constant::Tag::Integer: {
        ConstantPool::Integer integer;
        integer.bytes = read_u32();
        return ConstantPool::Constant(integer);
    }
    case ConstantPool::Constant::Tag::Methodref: {
        ConstantPool::Methodref methodref;
        methodref.class_index = read_u16();
        methodref.name_and_type_index = read_u16();
        return ConstantPool::Constant(methodref);
    }
    default:
        TODO();
    }
}

void ClassFileParser::parse_class_references()
{
    m_classfile->this_class = m_classfile->constant_pool.class_at(read_u16() - 1);
    u16 super_class_index = read_u16() - 1;
    if (super_class_index == 0)
        m_classfile->super_class = {};
    else
        m_classfile->super_class = m_classfile->constant_pool.class_at(super_class_index);
}

void ClassFileParser::parse_interfaces()
{
    u16 interfaces_count = read_u16();
    auto interfaces = FixedArray<ConstantPool::Class>(interfaces_count);
    for (auto& interface : interfaces)
        interface = m_classfile->constant_pool.class_at(read_u16() - 1);

    m_classfile->interfaces = interfaces;
}

void ClassFileParser::parse_fields()
{
    u16 fields_count = read_u16();
    auto fields = FixedArray<FieldInfo>(fields_count);

    for (auto& field : fields) {
        FieldInfo field_info;
        field_info.access_flags = read_u16();
        field_info.name_index = read_u16() - 1;
        field_info.descriptor_index = read_u16() - 1;
        parse_attributes();
        field = field_info;
    }

    m_classfile->fields = fields;
}

void ClassFileParser::parse_methods()
{
    u16 methods_count = read_u16();
    auto methods = FixedArray<MethodInfo>(methods_count);

    for (auto& method : methods) {
        MethodInfo method_info;
        method_info.access_flags = read_u16();
        method_info.name_index = read_u16() - 1;
        method_info.descriptor_index = read_u16() - 1;
        parse_attributes();
        method = method_info;
    }

    m_classfile->methods = methods;
}

void ClassFileParser::parse_attributes()
{
    u16 attributes_count = read_u16();
    auto attributes = FixedArray<AttributeInfo>(attributes_count);

    for (auto& attribute : attributes) {
        AttributeInfo attribute_info;
        attribute_info.attribute_name_index = read_u16() - 1;
        attribute_info.attribute_length = read_u32();
        attribute_info.info = data_at_offset();
        advance((int)attribute_info.attribute_length);
        attribute = attribute_info;
    }

    m_classfile->attributes = attributes;
}

u32 ClassFileParser::read_u32()
{
    auto result = AK::convert_between_host_and_big_endian(*((u32 const*)data_at_offset()));
    advance(4);
    return result;
}

u16 ClassFileParser::read_u16()
{
    auto result = AK::convert_between_host_and_big_endian(*((u16 const*)data_at_offset()));
    advance(2);
    return result;
}

u8 ClassFileParser::read_u8()
{
    auto result = *data_at_offset();
    advance(1);
    return result;
}

void ClassFileParser::advance(int amount)
{
    m_offset += amount;
}

u8 const* ClassFileParser::data_at_offset() const
{
    return m_source.offset_pointer(m_offset);
}

}
