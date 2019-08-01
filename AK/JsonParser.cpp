#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <AK/StringBuilder.h>

namespace AK {

static inline bool is_whitespace(char ch)
{
    return ch == ' ' || ch == '\n' || ch == '\t' || ch == '\v' || ch == '\r';
}

char JsonParser::peek() const
{
    if (m_index < m_input.length())
        return m_input[m_index];
    return '\0';
}

char JsonParser::consume()
{
    if (m_index < m_input.length())
        return m_input[m_index++];
    return '\0';
}

template<typename C>
void JsonParser::consume_while(C condition)
{
    while (condition(peek()))
        consume();
}

template<typename C>
String JsonParser::extract_while(C condition)
{
    StringBuilder builder;
    while (condition(peek()))
        builder.append(consume());
    return builder.to_string();
};

void JsonParser::consume_whitespace()
{
    consume_while([](char ch) { return is_whitespace(ch); });
}

void JsonParser::consume_specific(char expected_ch)
{
    char consumed_ch = consume();
    ASSERT(consumed_ch == expected_ch);
}

String JsonParser::consume_quoted_string()
{
    consume_specific('"');
    StringBuilder builder;
    for (;;) {
        char ch = peek();
        if (ch == '"')
            break;
        if (ch != '\\') {
            builder.append(consume());
            continue;
        }
        consume();
        char escaped_ch = consume();
        switch (escaped_ch) {
        case 'n':
        case 'r':
            builder.append('\n');
            break;
        case 't':
            builder.append('\t');
            break;
        case 'b':
            builder.append('\b');
            break;
        case 'f':
            builder.append('\f');
            break;
        case 'u':
            // FIXME: Implement \uXXXX
            ASSERT_NOT_REACHED();
            break;
        default:
            builder.append(escaped_ch);
            break;
        }
    }
    consume_specific('"');
    return builder.to_string();
}

JsonValue JsonParser::parse_object()
{
    JsonObject object;
    consume_specific('{');
    for (;;) {
        consume_whitespace();
        if (peek() == '}')
            break;
        consume_whitespace();
        auto name = consume_quoted_string();
        consume_whitespace();
        consume_specific(':');
        consume_whitespace();
        auto value = parse();
        object.set(name, move(value));
        consume_whitespace();
        if (peek() == '}')
            break;
        consume_specific(',');
    }
    consume_specific('}');
    return object;
}

JsonValue JsonParser::parse_array()
{
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
}

JsonValue JsonParser::parse_string()
{
    return consume_quoted_string();
}

JsonValue JsonParser::parse_number()
{
    auto number_string = extract_while([](char ch) { return ch == '-' || (ch >= '0' && ch <= '9'); });
    bool ok;
    auto value = JsonValue(number_string.to_uint(ok));
    if (!ok)
        value = JsonValue(number_string.to_int(ok));
    ASSERT(ok);
    return value;
}

void JsonParser::consume_string(const char* str)
{
    for (size_t i = 0, length = strlen(str); i < length; ++i)
        consume_specific(str[i]);
}

JsonValue JsonParser::parse_true()
{
    consume_string("true");
    return JsonValue(true);
}

JsonValue JsonParser::parse_false()
{
    consume_string("false");
    return JsonValue(false);
}

JsonValue JsonParser::parse_null()
{
    consume_string("null");
    return JsonValue(JsonValue::Type::Null);
}

JsonValue JsonParser::parse_undefined()
{
    consume_string("undefined");
    return JsonValue(JsonValue::Type::Undefined);
}

JsonValue JsonParser::parse()
{
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

    return JsonValue();
}

}
