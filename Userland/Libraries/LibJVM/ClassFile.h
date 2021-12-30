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
};

struct FieldInfo {
    u16 access_flags;
    u16 name_index;
    u16 descriptor_index;
    AK::FixedArray<AttributeInfo> attributes;
};

struct MethodInfo {
    u16 access_flags;
    u16 name_index;
    u16 descriptor_index;
    AK::FixedArray<AttributeInfo> attributes;
};

// https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html
struct ClassFile {
    u16 minor_version;
    u16 major_version;
    FixedArray<ConstantPool::Constant> constant_pool;
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
