#include <AK/Function.h>
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
        AK::ref_if_not_null(m_value.as_string);
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
        clear();
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

JsonValue::JsonValue(const char* cstring)
    : JsonValue(String(cstring))
{
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
        AK::ref_if_not_null(m_value.as_string);
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
        AK::deref_if_not_null(m_value.as_string);
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

void JsonValue::serialize(StringBuilder& builder) const
{
    switch (m_type) {
    case Type::String:
        builder.appendf("\"%s\"", m_value.as_string->characters());
        break;
    case Type::Array:
        m_value.as_array->serialize(builder);
        break;
    case Type::Object:
        m_value.as_object->serialize(builder);
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

String JsonValue::serialized() const
{
    StringBuilder builder;
    serialize(builder);
    return builder.to_string();
}

static bool is_whitespace(char ch)
{
    return ch == ' ' || ch == '\n' || ch == '\t' || ch == '\v' || ch == '\r';
}

JsonValue JsonValue::from_string(const StringView& input)
{
    int index = 0;

    auto peek = [&] {
        return input[index];
    };

    auto consume = [&]() -> char {
        if (index < input.length())
            return input[index++];
        return '\0';
    };

    auto consume_while = [&](auto condition) {
        while (condition(peek()))
            consume();
    };

    auto extract_while = [&](auto condition) {
        StringBuilder builder;
        while (condition(peek()))
            builder.append(consume());
        return builder.to_string();
    };

    auto consume_whitespace = [&] {
        consume_while([](char ch) { return is_whitespace(ch); });
    };

    auto consume_specific = [&](char expected_ch) {
        char consumed_ch = consume();
        ASSERT(consumed_ch == expected_ch);
    };

    Function<JsonValue()> parse;

    auto parse_object_member = [&](JsonObject& object) {
        consume_whitespace();
        consume_specific('"');
        auto name = extract_while([](char ch) { return ch != '"'; });
        consume_specific('"');
        consume_whitespace();
        consume_specific(':');
        consume_whitespace();
        auto value = parse();
        object.set(name, value);
    };

    auto parse_object = [&]() -> JsonValue {
        JsonObject object;
        consume_specific('{');
        for (;;) {
            consume_whitespace();
            if (peek() == '}')
                break;
            parse_object_member(object);
            consume_whitespace();
            if (peek() == '}')
                break;
            consume_specific(',');
        }
        consume_specific('}');
        return object;
    };

    auto parse_array = [&]() -> JsonValue {
        JsonArray array;
        consume_specific('[');
        for (;;) {
            consume_whitespace();
            if (peek() == ']')
                break;
            array.append(parse());
            consume_whitespace();
            if (peek() == ']')
                break;
            consume_specific(',');
        }
        consume_whitespace();
        consume_specific(']');
        return array;
    };

    auto parse_string = [&]() -> JsonValue {
        consume_specific('"');
        auto string = extract_while([](char ch) { return ch != '"'; });
        consume_specific('"');
        return JsonValue(string);
    };

    auto parse_number = [&]() -> JsonValue {
        auto number_string = extract_while([](char ch) { return ch == '-' || (ch >= '0' && ch <= '9'); });
        bool ok;
        auto value = JsonValue(number_string.to_int(ok));
        ASSERT(ok);
        return value;
    };

    auto consume_string = [&](const char* str) {
        for (size_t i = 0, length = strlen(str); i < length; ++i)
            consume_specific(str[i]);
    };

    auto parse_true = [&]() -> JsonValue {
        consume_string("true");
        return JsonValue(true);
    };

    auto parse_false = [&]() -> JsonValue {
        consume_string("false");
        return JsonValue(false);
    };

    auto parse_null = [&]() -> JsonValue {
        consume_string("null");
        return JsonValue(JsonValue::Type::Null);
    };

    auto parse_undefined = [&]() -> JsonValue {
        consume_string("undefined");
        return JsonValue(JsonValue::Type::Undefined);
    };

    parse = [&]() -> JsonValue {
        consume_whitespace();
        auto type_hint = peek();
        switch (type_hint) {
        case '{':
            return parse_object();
        case '[':
            return parse_array();
        case '"':
            return parse_string();
        case '-':
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
            return parse_number();
        case 'f':
            return parse_false();
        case 't':
            return parse_true();
        case 'n':
            return parse_null();
        case 'u':
            return parse_undefined();
        }
        ASSERT_NOT_REACHED();
    };

    return parse();
}

}
