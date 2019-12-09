#pragma once

#include <AK/JsonValue.h>

namespace AK {

class JsonParser {
public:
    explicit JsonParser(const StringView& input)
        : m_input(input)
    {
    }
    ~JsonParser()
    {
    }

    JsonValue parse();

private:
    char peek() const;
    char consume();
    void consume_whitespace();
    void consume_specific(char expected_ch);
    void consume_string(const char*);
    String consume_quoted_string();
    JsonArray parse_array();
    JsonObject parse_object();
    JsonValue parse_number();
    JsonValue parse_string();
    JsonValue parse_false();
    JsonValue parse_true();
    JsonValue parse_null();
    JsonValue parse_undefined();

    template<typename C>
    void consume_while(C);

    StringView m_input;
    size_t m_index { 0 };

    String m_last_string_starting_with_character[256];
};

}

using AK::JsonParser;
