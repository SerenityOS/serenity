/*
 * Copyright (c) 2021, Noah Haasis <haasis_noah@yahoo.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ClassFile.h"
#include <AK/ByteBuffer.h>

namespace JVM {

// https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html
class ClassFileParser {
public:
    ErrorOr<OwnPtr<ClassFile>> parse(ByteBuffer);

private:
    u16 read_u16();
    u32 read_u32();
    u8 read_u8();

    void advance(int);

    u8 const* data_at_offset() const;

    void parse_constant_pool();
    ConstantPool::Constant parse_constant_info();
    void parse_class_references();
    void parse_interfaces();
    void parse_fields();
    void parse_methods();
    void parse_attributes();

    ByteBuffer m_source;
    int m_offset = 0;

    AK::OwnPtr<ClassFile> m_classfile;
};

}
