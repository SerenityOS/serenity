#pragma once

#include <AK/StringView.h>
#include <AK/Types.h>

namespace AK {

class Utf8View;

class Utf8CodepointIterator {
    friend class Utf8View;

public:
    ~Utf8CodepointIterator() {}

    bool operator==(const Utf8CodepointIterator&) const;
    bool operator!=(const Utf8CodepointIterator&) const;
    Utf8CodepointIterator& operator++();
    u32 operator*() const;

    int codepoint_length_in_bytes() const;

private:
    Utf8CodepointIterator(const unsigned char*, int);
    const unsigned char* m_ptr { nullptr };
    int m_length { -1 };
};

class Utf8View {
public:
    explicit Utf8View(const String&);
    explicit Utf8View(const StringView&);
    explicit Utf8View(const char*);
    ~Utf8View() {}

    const StringView& as_string() const { return m_string; }

    Utf8CodepointIterator begin() const;
    Utf8CodepointIterator end() const;

    const unsigned char* bytes() const { return begin_ptr(); }
    int byte_length() const { return m_string.length(); }
    int byte_offset_of(const Utf8CodepointIterator&) const;
    Utf8View substring_view(int byte_offset, int byte_length) const;
    bool is_empty() const { return m_string.is_empty(); }

    bool validate() const;

private:
    const unsigned char* begin_ptr() const;
    const unsigned char* end_ptr() const;

    StringView m_string;
};

}

using AK::Utf8View;
