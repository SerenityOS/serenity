/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/String.h>
#include <AK/Types.h>

namespace Unicode {

// Note: The single code point case conversions only perform simple case folding.
// Use the full-string transformations for full case folding.
u32 to_unicode_lowercase(u32 code_point);
u32 to_unicode_uppercase(u32 code_point);

String to_unicode_lowercase_full(StringView const&);
String to_unicode_uppercase_full(StringView const&);

}
