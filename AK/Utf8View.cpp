#include <AK/Utf8View.h>
#include <AK/LogStream.h>

namespace AK {

Utf8View::Utf8View(const String& string)
    : m_string(string)
{
}

Utf8View::Utf8View(const StringView& string)
    : m_string(string)
{
}

Utf8View::Utf8View(const char* string)
    : m_string(string)
{
}

const unsigned char* Utf8View::begin_ptr() const
{
    return (const unsigned char*)m_string.characters_without_null_termination();
}

const unsigned char* Utf8View::end_ptr() const
{
    return begin_ptr() + m_string.length();
}

Utf8CodepointIterator Utf8View::begin() const
{
    return { begin_ptr(), (int)m_string.length() };
}

Utf8CodepointIterator Utf8View::end() const
{
    return { end_ptr(), 0 };
}

int Utf8View::byte_offset_of(const Utf8CodepointIterator& it) const
{
    ASSERT(it.m_ptr >= begin_ptr());
    ASSERT(it.m_ptr <= end_ptr());

    return it.m_ptr - begin_ptr();
}

Utf8View Utf8View::substring_view(int byte_offset, int byte_length) const
{
    StringView string = m_string.substring_view(byte_offset, byte_length);
    return Utf8View { string };
}

static inline bool decode_first_byte(
    unsigned char byte,
    int& out_codepoint_length_in_bytes,
    u32& out_value)
{
    if ((byte & 128) == 0) {
        out_value = byte;
        out_codepoint_length_in_bytes = 1;
        return true;
    }
    if ((byte & 64) == 0) {
        return false;
    }
    if ((byte & 32) == 0) {
        out_value = byte & 31;
        out_codepoint_length_in_bytes = 2;
        return true;
    }
    if ((byte & 16) == 0) {
        out_value = byte & 15;
        out_codepoint_length_in_bytes = 3;
        return true;
    }
    if ((byte & 8) == 0) {
        out_value = byte & 7;
        out_codepoint_length_in_bytes = 4;
        return true;
    }

    return false;
}

bool Utf8View::validate() const
{
    for (auto ptr = begin_ptr(); ptr < end_ptr(); ptr++) {
        int codepoint_length_in_bytes;
        u32 value;
        bool first_byte_makes_sense = decode_first_byte(*ptr, codepoint_length_in_bytes, value);
        if (!first_byte_makes_sense)
            return false;

        for (int i = 1; i < codepoint_length_in_bytes; i++) {
            ptr++;
            if (ptr >= end_ptr())
                return false;
            if (*ptr >> 6 != 2)
                return false;
        }
    }

    return true;
}

Utf8CodepointIterator::Utf8CodepointIterator(const unsigned char* ptr, int length)
    : m_ptr(ptr)
    , m_length(length)
{
}

bool Utf8CodepointIterator::operator==(const Utf8CodepointIterator& other) const
{
    return m_ptr == other.m_ptr && m_length == other.m_length;
}

bool Utf8CodepointIterator::operator!=(const Utf8CodepointIterator& other) const
{
    return !(*this == other);
}

Utf8CodepointIterator& Utf8CodepointIterator::operator++()
{
    ASSERT(m_length > 0);

    int codepoint_length_in_bytes;
    u32 value;
    bool first_byte_makes_sense = decode_first_byte(*m_ptr, codepoint_length_in_bytes, value);

    ASSERT(first_byte_makes_sense);
    (void)value;

    ASSERT(codepoint_length_in_bytes <= m_length);
    m_ptr += codepoint_length_in_bytes;
    m_length -= codepoint_length_in_bytes;

    return *this;
}

int Utf8CodepointIterator::codepoint_length_in_bytes() const
{
    ASSERT(m_length > 0);
    int codepoint_length_in_bytes;
    u32 value;
    bool first_byte_makes_sense = decode_first_byte(*m_ptr, codepoint_length_in_bytes, value);
    ASSERT(first_byte_makes_sense);
    return codepoint_length_in_bytes;
}

u32 Utf8CodepointIterator::operator*() const
{
    ASSERT(m_length > 0);

    u32 codepoint_value_so_far;
    int codepoint_length_in_bytes;

    bool first_byte_makes_sense = decode_first_byte(m_ptr[0], codepoint_length_in_bytes, codepoint_value_so_far);
    if (!first_byte_makes_sense) {
        dbg() << "First byte doesn't make sense, bytes: " << StringView((const char*)m_ptr, m_length);
    }
    ASSERT(first_byte_makes_sense);
    if (codepoint_length_in_bytes > m_length) {
        dbg() << "Not enough bytes (need " << codepoint_length_in_bytes << ", have " << m_length << "), first byte is: " << m_ptr[0] << " " << (const char*)m_ptr;
    }
    ASSERT(codepoint_length_in_bytes <= m_length);

    for (int offset = 1; offset < codepoint_length_in_bytes; offset++) {
        ASSERT(m_ptr[offset] >> 6 == 2);
        codepoint_value_so_far <<= 6;
        codepoint_value_so_far |= m_ptr[offset] & 63;
    }

    return codepoint_value_so_far;
}

}
