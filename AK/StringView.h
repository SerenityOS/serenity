#pragma once

#include <AK/Vector.h>

namespace AK {

class String;

class StringView {
public:
    StringView() {}
    StringView(const char* characters, int length)
        : m_characters(characters)
        , m_length(length)
    {
    }
    StringView(const unsigned char* characters, int length)
        : m_characters((const char*)characters)
        , m_length(length)
    {
    }
    StringView(const char* cstring)
        : m_characters(cstring)
    {
        if (cstring) {
            while (*(cstring++))
                ++m_length;
        }
    }
    StringView(const AK::String& string);

    bool is_empty() const { return m_length == 0; }
    const char* characters() const { return m_characters; }
    int length() const { return m_length; }
    char operator[](int index) const { return m_characters[index]; }

    StringView substring_view(int start, int length) const;
    Vector<StringView> split_view(char) const;
    unsigned to_uint(bool& ok) const;

    bool operator==(const char* cstring) const
    {
        int other_length = strlen(cstring);
        if (m_length != other_length)
            return false;
        return !memcmp(m_characters, cstring, m_length);
    }
    bool operator!=(const char* cstring) const
    {
        return !(*this == cstring);
    }

    bool operator==(const String&) const;

private:
    friend class String;
    const AK::String* m_string { nullptr };
    const char* m_characters { nullptr };
    int m_length { 0 };
};

}

using AK::StringView;
