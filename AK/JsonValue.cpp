#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/StringBuilder.h>

namespace AK {

JsonValue::JsonValue(Type type)
    : m_type(type)
{
}

JsonValue::JsonValue(const JsonValue& other)
{
    copy_from(other);
}

JsonValue& JsonValue::operator=(const JsonValue& other)
{
    if (this != &other) {
        clear();
        copy_from(other);
    }
    return *this;
}

void JsonValue::copy_from(const JsonValue& other)
{
    m_type = other.m_type;
    switch (m_type) {
    case Type::String:
        m_value.as_string = other.m_value.as_string;
        AK::retain_if_not_null(m_value.as_string);
        break;
    case Type::Object:
        m_value.as_object = new JsonObject(*other.m_value.as_object);
        break;
    case Type::Array:
        m_value.as_array = new JsonArray(*other.m_value.as_array);
        break;
    default:
        m_value.as_string = other.m_value.as_string;
        break;
    }
}

JsonValue::JsonValue(JsonValue&& other)
{
    m_type = exchange(other.m_type, Type::Undefined);
    m_value.as_string = exchange(other.m_value.as_string, nullptr);
}

JsonValue& JsonValue::operator=(JsonValue&& other)
{
    if (this != &other) {
        m_type = exchange(other.m_type, Type::Undefined);
        m_value.as_string = exchange(other.m_value.as_string, nullptr);
    }
    return *this;
}

JsonValue::JsonValue(int value)
    : m_type(Type::Int)
{
    m_value.as_int = value;
}

JsonValue::JsonValue(unsigned value)
{
    if (value > INT32_MAX) {
        m_type = Type::Double;
        m_value.as_double = value;
    } else {
        m_type = Type::Int;
        m_value.as_int = (int)value;
    }
}

JsonValue::JsonValue(double value)
    : m_type(Type::Double)
{
    m_value.as_double = value;
}

JsonValue::JsonValue(bool value)
    : m_type(Type::Bool)
{
    m_value.as_bool = value;
}

JsonValue::JsonValue(const String& value)
{
    if (value.is_null()) {
        m_type = Type::Null;
    } else {
        m_type = Type::String;
        m_value.as_string = const_cast<StringImpl*>(value.impl());
        AK::retain_if_not_null(m_value.as_string);
    }
}

JsonValue::JsonValue(const JsonObject& value)
    : m_type(Type::Object)
{
    m_value.as_object = new JsonObject(value);
}

JsonValue::JsonValue(const JsonArray& value)
    : m_type(Type::Array)
{
    m_value.as_array = new JsonArray(value);
}

void JsonValue::clear()
{
    switch (m_type) {
    case Type::String:
        AK::release_if_not_null(m_value.as_string);
        break;
    case Type::Object:
        delete m_value.as_object;
        break;
    case Type::Array:
        delete m_value.as_array;
        break;
    default:
        break;
    }
    m_type = Type::Undefined;
    m_value.as_string = nullptr;
}

void JsonValue::to_string(StringBuilder& builder) const
{
    switch (m_type) {
    case Type::String:
        builder.appendf("\"%s\"", m_value.as_string->characters());
        break;
    case Type::Array:
        m_value.as_array->to_string(builder);
        break;
    case Type::Object:
        m_value.as_object->to_string(builder);
        break;
    case Type::Bool:
        builder.append(m_value.as_bool ? "true" : "false");
        break;
    case Type::Double:
        builder.appendf("%g", m_value.as_double);
        break;
    case Type::Int:
        builder.appendf("%d", m_value.as_int);
        break;
    case Type::Undefined:
        builder.append("undefined");
        break;
    case Type::Null:
        builder.append("null");
        break;
    default:
        ASSERT_NOT_REACHED();
    }
}

String JsonValue::to_string() const
{
    StringBuilder builder;
    to_string(builder);
    return builder.to_string();
}

}
