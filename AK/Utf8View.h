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

private:
    Utf8CodepointIterator(const unsigned char*, int);
    const unsigned char* m_ptr { nullptr };
    int m_length { -1 };
};

class Utf8View {
public:
    explicit Utf8View(const StringView&);
    ~Utf8View() {}

    const StringView& as_string() const { return m_string; }

    Utf8CodepointIterator begin() const;
    Utf8CodepointIterator end() const;

    bool validate() const;

private:
    const unsigned char* begin_ptr() const;
    const unsigned char* end_ptr() const;

    StringView m_string;
};

}

using AK::Utf8View;
