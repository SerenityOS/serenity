/*
 * Copyright (c) 2024, Jamie Mansfield <jmansfield@cadixdev.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Span.h>
#include <LibJava/ClassFile.h>
#include <LibJava/Reader.h>

namespace Java {

class Parser {
public:
    Parser(ReadonlyBytes);

    ErrorOr<ClassFile> parse_class_file();

    ErrorOr<ConstantPoolInfo> parse_constant_pool_info();
    ErrorOr<FieldInfo> parse_field_info();
    ErrorOr<MethodInfo> parse_method_info();
    ErrorOr<AttributeInfo> parse_attribute_info();

protected:
    Reader m_reader;
};

}
