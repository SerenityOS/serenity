/*
 * Copyright (c) 2021, Noah Haasis <haasis_noah@yahoo.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Error.h>
#include <AK/FixedArray.h>
#include <AK/OwnPtr.h>
#include <AK/Types.h>

#include "ConstantPool.h"

namespace JVM {

struct AttributeInfo {
    u16 attribute_name_index;
    u32 attribute_length;
    u8 const* info;

    ConstantPool::Utf8 attribute_name(ConstantPool const& constant_pool) const { return constant_pool.utf8_at(attribute_name_index); }
};

struct FieldInfo {
    u16 access_flags;
    u16 name_index;
    u16 descriptor_index;
    AK::FixedArray<AttributeInfo> attributes;

    ConstantPool::Utf8 name(ConstantPool const& constant_pool) const { return constant_pool.utf8_at(name_index); }
    ConstantPool::Utf8 descriptor(ConstantPool const& constant_pool) const { return constant_pool.utf8_at(descriptor_index); }
};

struct MethodInfo {
    u16 access_flags;
    u16 name_index;
    u16 descriptor_index;
    AK::FixedArray<AttributeInfo> attributes;

    ConstantPool::Utf8 name(ConstantPool const& constant_pool) const { return constant_pool.utf8_at(name_index); }
    ConstantPool::Utf8 descriptor(ConstantPool const& constant_pool) const { return constant_pool.utf8_at(descriptor_index); }
};

// https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html
struct ClassFile {
    u16 minor_version;
    u16 major_version;
    ConstantPool constant_pool;
    u16 access_flags;
    ConstantPool::Class this_class;
    AK::Optional<ConstantPool::Class> super_class;
    FixedArray<ConstantPool::Class> interfaces;
    FixedArray<FieldInfo> fields;
    FixedArray<MethodInfo> methods;
    FixedArray<AttributeInfo> attributes;

    // The constant pool and attributes contain references into this buffer for e.g. strings or bytecode.
    ByteBuffer class_file_data;
};
}
