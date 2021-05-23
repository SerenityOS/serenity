/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/Object.h>
#include <LibPDF/Value.h>

namespace PDF {

Value::~Value()
{
    if (is_object())
        m_as_object->unref();
}

Value& Value::operator=(const Value& other)
{
    m_type = other.m_type;
    switch (m_type) {
    case Type::Null:
        break;
    case Type::Bool:
        m_as_bool = other.m_as_bool;
        break;
    case Type::Int:
        m_as_int = other.m_as_int;
        break;
    case Type::Float:
        m_as_float = other.m_as_float;
        break;
    case Type::Ref:
        m_as_ref = other.m_as_ref;
        break;
    case Type::Object:
        m_as_object = other.m_as_object;
        if (m_as_object)
            m_as_object->ref();
        break;
    }
    return *this;
}

String Value::to_string(int indent) const
{
    switch (m_type) {
    case Type::Null:
        return "null";
    case Type::Bool:
        return as_bool() ? "true" : "false";
    case Type::Int:
        return String::number(as_int());
    case Type::Float:
        return String::number(as_float());
    case Type::Ref:
        return String::formatted("{} {} R", as_ref_index(), as_ref_generation_index());
    case Type::Object:
        return as_object()->to_string(indent);
    }

    VERIFY_NOT_REACHED();
}

}
