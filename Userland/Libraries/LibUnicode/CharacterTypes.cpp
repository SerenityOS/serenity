/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Platform.h>
#include <AK/Types.h>
#include <LibUnicode/CharacterTypes.h>

#if ENABLE_UNICODE_DATA
#    include <LibUnicode/UnicodeData.h>
#else
#    include <AK/CharacterTypes.h>
#endif

namespace Unicode {

u32 to_unicode_lowercase(u32 code_point)
{
#if ENABLE_UNICODE_DATA
    auto unicode_data = unicode_data_for_code_point(code_point);
    if (unicode_data.has_value())
        return unicode_data->simple_lowercase_mapping;
    return code_point;
#else
    return AK::to_ascii_lowercase(code_point);
#endif
}

u32 to_unicode_uppercase(u32 code_point)
{
#if ENABLE_UNICODE_DATA
    auto unicode_data = unicode_data_for_code_point(code_point);
    if (unicode_data.has_value())
        return unicode_data->simple_uppercase_mapping;
    return code_point;
#else
    return AK::to_ascii_uppercase(code_point);
#endif
}

}
