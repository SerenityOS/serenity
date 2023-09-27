/*
 * Copyright (c) 2023, Gurkirat Singh <tbhaxor@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Slugify.h>
#include <AK/StringView.h>

namespace AK {
ErrorOr<String> slugify(String const& input, char const glue)
{
    StringBuilder sb;
    bool just_processed_space = false;

    for (auto const& code_point : input.code_points()) {
        if (is_ascii_alphanumeric(code_point)) {
            sb.append_code_point(to_ascii_lowercase(code_point));
            just_processed_space = false;
        } else if ((code_point == static_cast<u32>(glue) || is_ascii_space(code_point)) && !just_processed_space) {
            sb.append_code_point(glue);
            just_processed_space = true;
        }
    }

    auto output = TRY(sb.to_string());
    if (output.ends_with(static_cast<u32>(glue))) {
        return output.trim(StringView { &glue, 1 }, TrimMode::Right);
    }
    return output;
}
}
