/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/String.h>
#include <LibJS/Heap.h>
#include <LibJS/Object.h>
#include <LibJS/PrimitiveString.h>
#include <LibJS/StringObject.h>
#include <LibJS/Value.h>

namespace JS {

String Value::to_string() const
{
    if (is_boolean())
        return as_bool() ? "true" : "false";

    if (is_null())
        return "null";

    if (is_undefined())
        return "undefined";

    if (is_number()) {
        // FIXME: This needs improvement.
        return String::number((i32)as_double());
    }

    if (is_object()) {
        return String::format("{%s}", as_object()->class_name());
    }

    if (is_string())
        return m_value.as_string->string();

    ASSERT_NOT_REACHED();
}

bool Value::to_boolean() const
{
    switch (m_type) {
    case Type::Boolean:
        return m_value.as_bool;
    case Type::Number:
        return !(m_value.as_double == 0 || m_value.as_double == -0);
    case Type::Null:
    case Type::Undefined:
        return false;
    case Type::String:
        return !as_string()->string().is_empty();
    case Type::Object:
        return true;
    default:
        ASSERT_NOT_REACHED();
    }
}

Value Value::to_object(Heap& heap) const
{
    if (is_object())
        return const_cast<Object*>(as_object());

    if (is_string())
        return heap.allocate<StringObject>(m_value.as_string);

    ASSERT_NOT_REACHED();
}

Value greater_than(Value lhs, Value rhs)
{
    ASSERT(lhs.is_number());
    ASSERT(rhs.is_number());
    return Value(lhs.as_double() > rhs.as_double());
}

Value greater_than_equals(Value lhs, Value rhs)
{
    ASSERT(lhs.is_number());
    ASSERT(rhs.is_number());
    return Value(lhs.as_double() >= rhs.as_double());
}

Value less_than(Value lhs, Value rhs)
{
    ASSERT(lhs.is_number());
    ASSERT(rhs.is_number());
    return Value(lhs.as_double() < rhs.as_double());
}

Value less_than_equals(Value lhs, Value rhs)
{
    ASSERT(lhs.is_number());
    ASSERT(rhs.is_number());
    return Value(lhs.as_double() <= rhs.as_double());
}

Value bitwise_and(Value lhs, Value rhs)
{
    ASSERT(lhs.is_number());
    ASSERT(rhs.is_number());
    return Value((i32)lhs.as_double() & (i32)rhs.as_double());
}

Value bitwise_or(Value lhs, Value rhs)
{
    ASSERT(lhs.is_number());
    ASSERT(rhs.is_number());
    return Value((i32)lhs.as_double() | (i32)rhs.as_double());
}

Value bitwise_xor(Value lhs, Value rhs)
{
    ASSERT(lhs.is_number());
    ASSERT(rhs.is_number());
    return Value((i32)lhs.as_double() ^ (i32)rhs.as_double());
}

Value bitwise_not(Value lhs)
{
    ASSERT(lhs.is_number());
    return Value(~(i32)lhs.as_double());
}

Value left_shift(Value lhs, Value rhs)
{
    ASSERT(lhs.is_number());
    ASSERT(rhs.is_number());
    return Value((i32)lhs.as_double() << (i32)rhs.as_double());
}

Value right_shift(Value lhs, Value rhs)
{
    ASSERT(lhs.is_number());
    ASSERT(rhs.is_number());
    return Value((i32)lhs.as_double() >> (i32)rhs.as_double());
}

Value add(Value lhs, Value rhs)
{
    ASSERT(lhs.is_number());
    ASSERT(rhs.is_number());
    return Value(lhs.as_double() + rhs.as_double());
}

Value sub(Value lhs, Value rhs)
{
    ASSERT(lhs.is_number());
    ASSERT(rhs.is_number());
    return Value(lhs.as_double() - rhs.as_double());
}

Value mul(Value lhs, Value rhs)
{
    ASSERT(lhs.is_number());
    ASSERT(rhs.is_number());
    return Value(lhs.as_double() * rhs.as_double());
}

Value div(Value lhs, Value rhs)
{
    ASSERT(lhs.is_number());
    ASSERT(rhs.is_number());
    return Value(lhs.as_double() / rhs.as_double());
}

Value typed_eq(Value lhs, Value rhs)
{
    if (rhs.type() != lhs.type())
        return Value(false);

    switch (lhs.type()) {
    case Value::Type::Undefined:
        return Value(true);
    case Value::Type::Null:
        return Value(true);
    case Value::Type::Number:
        return Value(lhs.as_double() == rhs.as_double());
    case Value::Type::String:
        return Value(lhs.as_string() == rhs.as_string());
    case Value::Type::Boolean:
        return Value(lhs.as_bool() == rhs.as_bool());
    case Value::Type::Object:
        return Value(lhs.as_object() == rhs.as_object());
    }

    ASSERT_NOT_REACHED();
}



const LogStream& operator<<(const LogStream& stream, const Value& value)
{
    return stream << value.to_string();
}

}
