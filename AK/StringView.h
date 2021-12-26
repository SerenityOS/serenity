#pragma once

#include <AK/Vector.h>

namespace AK {

class String;

class StringView {
public:
    StringView() { }
    StringView(const char* characters, int length) : m_characters(characters), m_length(length) { }
    StringView(const unsigned char* characters, int length) : m_characters((const char*)characters), m_length(length) { }
    StringView(const char* cstring)
        : m_characters(cstring)
    {
        if (cstring) {
            while (*(cstring++))
                ++m_length;
        }
    }

    bool is_empty() const { return m_length == 0; }
    const char* characters() const { return m_characters; }
    int length() const { return m_length; }
    char operator[](int index) const { return m_characters[index]; }

    StringView substring_view(int start, int length) const;
    Vector<StringView> split_view(char) const;
    unsigned to_uint(bool& ok) const;

    bool operator==(const char* cstring) const { return !strcmp(m_characters, cstring); }
    bool operator!=(const char* cstring) const { return strcmp(m_characters, cstring); }

    bool operator==(const String&) const;

private:
    const char* m_characters { nullptr };
    int m_length { 0 };
};

}

using AK::StringView;
