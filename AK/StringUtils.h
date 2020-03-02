#pragma once

#include <AK/Forward.h>

namespace AK {

enum class CaseSensitivity {
    CaseInsensitive,
    CaseSensitive,
};

namespace StringUtils {

    bool matches(const StringView& str, const StringView& mask, CaseSensitivity = CaseSensitivity::CaseInsensitive);
    int convert_to_int(const StringView&, bool& ok);
    unsigned convert_to_uint(const StringView&, bool& ok);

}

}

using AK::CaseSensitivity;
