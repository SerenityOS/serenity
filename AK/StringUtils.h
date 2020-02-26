#pragma once

#include <AK/Forward.h>

namespace AK {

enum class CaseSensitivity {
    CaseInsensitive,
    CaseSensitive,
};

namespace StringUtils {

    bool matches(const StringView& str, const StringView& mask, CaseSensitivity = CaseSensitivity::CaseInsensitive);

}

}

using AK::CaseSensitivity;
