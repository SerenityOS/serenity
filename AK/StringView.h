#pragma once

#include <AK/Vector.h>

namespace AK {

class ByteBuffer;
class String;
class StringImpl;

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

    StringView(const ByteBuffer&);
    StringView(const String&);

    bool is_null() const { return !m_characters; }
    bool is_empty() const { return m_length == 0; }
    const char* characters() const { return m_characters; }
    int length() const { return m_length; }
    char operator[](int index) const { return m_characters[index]; }

    StringView substring_view(int start, int length) const;
    Vector<StringView> split_view(char) const;
    unsigned to_uint(bool& ok) const;

    // Create a new substring view of this string view, starting either at the beginning of
    // the given substring view, or after its end, and continuing until the end of this string
    // view (that is, for the remaining part of its length). For example,
    //
    //    StringView str { "foobar" };
    //    StringView substr = str.substring_view(1, 2);  // "oo"
    //    StringView substr_from = str.substring_view_starting_from_substring(subst);  // "oobar"
    //    StringView substr_after = str.substring_view_starting_after_substring(subst);  // "bar"
    //
    // Note that this only works if the string view passed as an argument is indeed a substring
    // view of this string view, such as one created by substring_view() and split_view(). It
    // does not work for arbitrary strings; for example declaring substr in the example above as
    //
    //     StringView substr { "oo" };
    //
    // would not work.
    StringView substring_view_starting_from_substring(const StringView& substring) const;
    StringView substring_view_starting_after_substring(const StringView& substring) const;

    bool operator==(const char* cstring) const
    {
        if (is_null())
            return !cstring;
        if (!cstring)
            return false;
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
    const StringImpl* m_impl { nullptr };
    const char* m_characters { nullptr };
    int m_length { 0 };
};

}

using AK::StringView;
