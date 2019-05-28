#include <AK/AKString.h>
#include <AK/StringView.h>

namespace AK {

Vector<StringView> StringView::split_view(const char separator) const
{
    if (is_empty())
        return {};

    Vector<StringView> v;
    ssize_t substart = 0;
    for (ssize_t i = 0; i < length(); ++i) {
        char ch = characters()[i];
        if (ch == separator) {
            ssize_t sublen = i - substart;
            if (sublen != 0)
                v.append(substring_view(substart, sublen));
            substart = i + 1;
        }
    }
    ssize_t taillen = length() - substart;
    if (taillen != 0)
        v.append(substring_view(substart, taillen));
    if (characters()[length() - 1] == separator)
        v.append(String::empty().view());
    return v;
}

StringView StringView::substring_view(int start, int length) const
{
    if (!length)
        return {};
    ASSERT(start + length <= m_length);
    return { m_characters + start, length };
}

unsigned StringView::to_uint(bool& ok) const
{
    unsigned value = 0;
    for (ssize_t i = 0; i < length(); ++i) {
        if (characters()[i] < '0' || characters()[i] > '9') {
            ok = false;
            return 0;
        }
        value = value * 10;
        value += characters()[i] - '0';
    }
    ok = true;
    return value;
}

}
