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

#include <AK/FlyString.h>
#include <AK/String.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/BooleanObject.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/NumberObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/StringObject.h>
#include <LibJS/Runtime/Value.h>
#include <math.h>

namespace JS {

bool Value::is_array() const
{
    return is_object() && as_object().is_array();
}

String Value::to_string() const
{
    if (is_boolean())
        return as_bool() ? "true" : "false";

    if (is_null())
        return "null";

    if (is_undefined())
        return "undefined";

    if (is_number()) {
        if (is_nan())
            return "NaN";

        if (is_infinity())
            return as_double() < 0 ? "-Infinity" : "Infinity";

        // FIXME: This needs improvement.
        if ((double)to_i32() == as_double())
            return String::number(to_i32());
        return String::format("%.4f", as_double());
    }

    if (is_object()) {
        auto primitive_value = as_object().to_primitive(Object::PreferredType::String);
        // FIXME: Maybe we should pass in the Interpreter& and call interpreter.exception() instead?
        if (primitive_value.is_empty())
            return {};
        return primitive_value.to_string();
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
        if (is_nan()) {
            return false;
        }
        return !(m_value.as_double == 0 || m_value.as_double == -0);
    case Type::Null:
    case Type::Undefined:
        return false;
    case Type::String:
        return !as_string().string().is_empty();
    case Type::Object:
        return true;
    default:
        ASSERT_NOT_REACHED();
    }
}

Value Value::to_primitive(Interpreter&) const
{
    if (is_object())
        return as_object().to_primitive();
    return *this;
}

Object* Value::to_object(Heap& heap) const
{
    if (is_object())
        return &const_cast<Object&>(as_object());

    if (is_string())
        return StringObject::create(heap.interpreter().global_object(), *m_value.as_string);

    if (is_number())
        return NumberObject::create(heap.interpreter().global_object(), m_value.as_double);

    if (is_boolean())
        return BooleanObject::create(heap.interpreter().global_object(), m_value.as_bool);

    if (is_null() || is_undefined()) {
        heap.interpreter().throw_exception<TypeError>("ToObject on null or undefined.");
        return nullptr;
    }

    dbg() << "Dying because I can't to_object() on " << *this;
    ASSERT_NOT_REACHED();
}

Value Value::to_number() const
{
    switch (m_type) {
    case Type::Empty:
        ASSERT_NOT_REACHED();
        return {};
    case Type::Boolean:
        return Value(m_value.as_bool ? 1 : 0);
    case Type::Number:
        return Value(m_value.as_double);
    case Type::Null:
        return Value(0);
    case Type::String: {
        // FIXME: Trim whitespace beforehand
        auto& string = as_string().string();
        if (string.is_empty())
            return Value(0);
        if (string == "Infinity" || string == "+Infinity")
            return js_infinity();
        if (string == "-Infinity")
            return js_negative_infinity();
        bool ok;
        //FIXME: Parse in a better way
        auto parsed_int = string.to_int(ok);
        if (ok)
            return Value(parsed_int);

        return js_nan();
    }
    case Type::Undefined:
        return js_nan();
    case Type::Object:
        return m_value.as_object->to_primitive(Object::PreferredType::Number).to_number();
    }

    ASSERT_NOT_REACHED();
}

i32 Value::to_i32() const
{
    return static_cast<i32>(to_number().as_double());
}

double Value::to_double() const
{
    return to_number().as_double();
}

size_t Value::to_size_t() const
{
    if (is_empty())
        return 0;
    auto number = to_number();
    if (number.is_nan() || number.as_double() <= 0)
        return 0;
    return min(number.to_i32(), (i32)pow(2, 53) - 1);
}

Value greater_than(Interpreter&, Value lhs, Value rhs)
{
    return Value(lhs.to_number().as_double() > rhs.to_number().as_double());
}

Value greater_than_equals(Interpreter&, Value lhs, Value rhs)
{
    return Value(lhs.to_number().as_double() >= rhs.to_number().as_double());
}

Value less_than(Interpreter&, Value lhs, Value rhs)
{
    return Value(lhs.to_number().as_double() < rhs.to_number().as_double());
}

Value less_than_equals(Interpreter&, Value lhs, Value rhs)
{
    return Value(lhs.to_number().as_double() <= rhs.to_number().as_double());
}

Value bitwise_and(Interpreter&, Value lhs, Value rhs)
{
    return Value((i32)lhs.to_number().as_double() & (i32)rhs.to_number().as_double());
}

Value bitwise_or(Interpreter&, Value lhs, Value rhs)
{
    bool lhs_invalid = lhs.is_undefined() || lhs.is_null() || lhs.is_nan() || lhs.is_infinity();
    bool rhs_invalid = rhs.is_undefined() || rhs.is_null() || rhs.is_nan() || rhs.is_infinity();

    if (lhs_invalid && rhs_invalid)
        return Value(0);

    if (lhs_invalid || rhs_invalid)
        return lhs_invalid ? rhs.to_number() : lhs.to_number();

    if (!rhs.is_number() && !lhs.is_number())
        return Value(0);

    return Value((i32)lhs.to_number().as_double() | (i32)rhs.to_number().as_double());
}

Value bitwise_xor(Interpreter&, Value lhs, Value rhs)
{
    return Value((i32)lhs.to_number().as_double() ^ (i32)rhs.to_number().as_double());
}

Value bitwise_not(Interpreter&, Value lhs)
{
    return Value(~(i32)lhs.to_number().as_double());
}

Value unary_plus(Interpreter&, Value lhs)
{
    return lhs.to_number();
}

Value unary_minus(Interpreter&, Value lhs)
{
    if (lhs.to_number().is_nan())
        return js_nan();
    return Value(-lhs.to_number().as_double());
}

Value left_shift(Interpreter&, Value lhs, Value rhs)
{
    auto lhs_number = lhs.to_number();
    if (!lhs_number.is_finite_number())
        return Value(0);
    auto rhs_number = rhs.to_number();
    if (!rhs_number.is_finite_number())
        return lhs_number;
    return Value((i32)lhs_number.as_double() << (i32)rhs_number.as_double());
}

Value right_shift(Interpreter&, Value lhs, Value rhs)
{
    auto lhs_number = lhs.to_number();
    if (!lhs_number.is_finite_number())
        return Value(0);
    auto rhs_number = rhs.to_number();
    if (!rhs_number.is_finite_number())
        return lhs_number;
    return Value((i32)lhs_number.as_double() >> (i32)rhs_number.as_double());
}

Value unsigned_right_shift(Interpreter&, Value lhs, Value rhs)
{
    auto lhs_number = lhs.to_number();
    if (!lhs_number.is_finite_number())
        return Value(0);
    auto rhs_number = rhs.to_number();
    if (!rhs_number.is_finite_number())
        return lhs_number;
    return Value((unsigned)lhs_number.as_double() >> (i32)rhs_number.as_double());
}

Value add(Interpreter& interpreter, Value lhs, Value rhs)
{
    auto lhs_primitive = lhs.to_primitive(interpreter);
    auto rhs_primitive = rhs.to_primitive(interpreter);

    if (lhs_primitive.is_string() || rhs_primitive.is_string())
        return js_string(interpreter.heap(), String::format("%s%s", lhs_primitive.to_string().characters(), rhs_primitive.to_string().characters()));

    return Value(lhs_primitive.to_number().as_double() + rhs_primitive.to_number().as_double());
}

Value sub(Interpreter&, Value lhs, Value rhs)
{
    return Value(lhs.to_number().as_double() - rhs.to_number().as_double());
}

Value mul(Interpreter&, Value lhs, Value rhs)
{
    return Value(lhs.to_number().as_double() * rhs.to_number().as_double());
}

Value div(Interpreter&, Value lhs, Value rhs)
{
    return Value(lhs.to_number().as_double() / rhs.to_number().as_double());
}

Value mod(Interpreter&, Value lhs, Value rhs)
{
    if (lhs.to_number().is_nan() || rhs.to_number().is_nan())
        return js_nan();

    double index = lhs.to_number().as_double();
    double period = rhs.to_number().as_double();
    double trunc = (double)(i32)(index / period);

    return Value(index - trunc * period);
}

Value exp(Interpreter&, Value lhs, Value rhs)
{
    return Value(pow(lhs.to_number().as_double(), rhs.to_number().as_double()));
}

Value typed_eq(Interpreter&, Value lhs, Value rhs)
{
    if (rhs.type() != lhs.type())
        return Value(false);

    switch (lhs.type()) {
    case Value::Type::Empty:
        ASSERT_NOT_REACHED();
        return {};
    case Value::Type::Undefined:
        return Value(true);
    case Value::Type::Null:
        return Value(true);
    case Value::Type::Number:
        return Value(lhs.as_double() == rhs.as_double());
    case Value::Type::String:
        return Value(lhs.as_string().string() == rhs.as_string().string());
    case Value::Type::Boolean:
        return Value(lhs.as_bool() == rhs.as_bool());
    case Value::Type::Object:
        return Value(&lhs.as_object() == &rhs.as_object());
    }

    ASSERT_NOT_REACHED();
}

Value eq(Interpreter& interpreter, Value lhs, Value rhs)
{
    if (lhs.type() == rhs.type())
        return typed_eq(interpreter, lhs, rhs);

    if ((lhs.is_undefined() || lhs.is_null()) && (rhs.is_undefined() || rhs.is_null()))
        return Value(true);

    if (lhs.is_object() && rhs.is_boolean())
        return eq(interpreter, lhs.as_object().to_primitive(), rhs.to_number());

    if (lhs.is_boolean() && rhs.is_object())
        return eq(interpreter, lhs.to_number(), rhs.as_object().to_primitive());

    if (lhs.is_object())
        return eq(interpreter, lhs.as_object().to_primitive(), rhs);

    if (rhs.is_object())
        return eq(interpreter, lhs, rhs.as_object().to_primitive());

    if (lhs.is_number() || rhs.is_number())
        return Value(lhs.to_number().as_double() == rhs.to_number().as_double());

    if ((lhs.is_string() && rhs.is_boolean()) || (lhs.is_string() && rhs.is_boolean()))
        return Value(lhs.to_number().as_double() == rhs.to_number().as_double());

    return Value(false);
}

Value in(Interpreter& interpreter, Value lhs, Value rhs)
{
    if (!rhs.is_object())
        return interpreter.throw_exception<TypeError>("'in' operator must be used on object");

    return Value(!rhs.as_object().get(lhs.to_string()).is_empty());
}

Value instance_of(Interpreter&, Value lhs, Value rhs)
{
    if (!lhs.is_object() || !rhs.is_object())
        return Value(false);

    auto constructor_prototype_property = rhs.as_object().get("prototype");
    if (!constructor_prototype_property.is_object())
        return Value(false);

    return Value(lhs.as_object().has_prototype(&constructor_prototype_property.as_object()));
}

const LogStream& operator<<(const LogStream& stream, const Value& value)
{
    return stream << value.to_string();
}

}
