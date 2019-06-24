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
    JsonValue parse_array();
    JsonValue parse_object();
    JsonValue parse_number();
    JsonValue parse_string();
    JsonValue parse_false();
    JsonValue parse_true();
    JsonValue parse_null();
    JsonValue parse_undefined();

    template<typename C>
    void consume_while(C);

    template<typename C>
    String extract_while(C);

    StringView m_input;
    int m_index { 0 };
};

}

using AK::JsonParser;
