#include <AK/String.h>
#include <AK/StringView.h>

namespace AK {

StringView::StringView(const String& string)
    : m_impl(string.impl())
    , m_characters(string.characters())
    , m_length(string.length())
{
}

StringView::StringView(const ByteBuffer& buffer)
    : m_characters((const char*)buffer.data())
    , m_length((size_t)buffer.size())
{
}

Vector<StringView> StringView::split_view(const char separator, bool keep_empty) const
{
    if (is_empty())
        return {};

    Vector<StringView> v;
    size_t substart = 0;
    for (size_t i = 0; i < length(); ++i) {
        char ch = characters_without_null_termination()[i];
        if (ch == separator) {
            size_t sublen = i - substart;
            if (sublen != 0 || keep_empty)
                v.append(substring_view(substart, sublen));
            substart = i + 1;
        }
    }
    size_t taillen = length() - substart;
    if (taillen != 0 || keep_empty)
        v.append(substring_view(substart, taillen));
    if (characters_without_null_termination()[length() - 1] == separator && keep_empty)
        v.append(String::empty());
    return v;
}

Vector<StringView> StringView::lines(bool consider_cr) const
{
    if (is_empty())
        return {};

    if (!consider_cr)
        return split_view('\n', true);

    Vector<StringView> v;
    size_t substart = 0;
    bool last_ch_was_cr = false;
    bool split_view = false;
    for (size_t i = 0; i < length(); ++i) {
        char ch = characters_without_null_termination()[i];
        if (ch == '\n') {
            split_view = true;
            if (last_ch_was_cr) {
                substart = i + 1;
                split_view = false;
                last_ch_was_cr = false;
            }
        }
        if (ch == '\r') {
            split_view = true;
            last_ch_was_cr = true;
        }
        if (split_view) {
            size_t sublen = i - substart;
            v.append(substring_view(substart, sublen));
            substart = i + 1;
        }
        split_view = false;
    }
    size_t taillen = length() - substart;
    if (taillen != 0)
        v.append(substring_view(substart, taillen));
    return v;
}

bool StringView::starts_with(const StringView& str) const
{
    if (str.is_empty())
        return true;
    if (is_empty())
        return false;
    if (str.length() > length())
        return false;
    if (characters_without_null_termination() == str.characters_without_null_termination())
        return true;
    return !memcmp(characters_without_null_termination(), str.characters_without_null_termination(), str.length());
}

bool StringView::ends_with(const StringView& str) const
{
    if (str.is_empty())
        return true;
    if (is_empty())
        return false;
    if (str.length() > length())
        return false;
    return !memcmp(characters_without_null_termination() + length() - str.length(), str.characters_without_null_termination(), str.length());
}

StringView StringView::substring_view(size_t start, size_t length) const
{
    ASSERT(start + length <= m_length);
    return { m_characters + start, length };
}

StringView StringView::substring_view_starting_from_substring(const StringView& substring) const
{
    const char* remaining_characters = substring.characters_without_null_termination();
    ASSERT(remaining_characters >= m_characters);
    ASSERT(remaining_characters <= m_characters + m_length);
    size_t remaining_length = m_length - (remaining_characters - m_characters);
    return { remaining_characters, remaining_length };
}

StringView StringView::substring_view_starting_after_substring(const StringView& substring) const
{
    const char* remaining_characters = substring.characters_without_null_termination() + substring.length();
    ASSERT(remaining_characters >= m_characters);
    ASSERT(remaining_characters <= m_characters + m_length);
    size_t remaining_length = m_length - (remaining_characters - m_characters);
    return { remaining_characters, remaining_length };
}

int StringView::to_int(bool& ok) const
{
    bool negative = false;
    int value = 0;
    size_t i = 0;

    if (is_empty()) {
        ok = false;
        return 0;
    }

    if (characters_without_null_termination()[0] == '-') {
        i++;
        negative = true;
    }
    for (; i < length(); i++) {
        if (characters_without_null_termination()[i] < '0' || characters_without_null_termination()[i] > '9') {
            ok = false;
            return 0;
        }
        value = value * 10;
        value += characters_without_null_termination()[i] - '0';
    }
    ok = true;

    return negative ? -value : value;
}

unsigned StringView::to_uint(bool& ok) const
{
    unsigned value = 0;
    for (size_t i = 0; i < length(); ++i) {
        if (characters_without_null_termination()[i] < '0' || characters_without_null_termination()[i] > '9') {
            ok = false;
            return 0;
        }
        value = value * 10;
        value += characters_without_null_termination()[i] - '0';
    }
    ok = true;
    return value;
}

unsigned StringView::hash() const
{
    if (is_empty())
        return 0;
    if (m_impl)
        return m_impl->hash();
    return string_hash(characters_without_null_termination(), length());
}

}
