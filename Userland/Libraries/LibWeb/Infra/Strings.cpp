/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/Utf8View.h>
#include <LibWeb/Infra/CharacterTypes.h>
#include <LibWeb/Infra/Strings.h>

namespace Web::Infra {

// https://infra.spec.whatwg.org/#strip-and-collapse-ascii-whitespace
String strip_and_collapse_whitespace(StringView string)
{
    // Replace any sequence of one or more consecutive code points that are ASCII whitespace in the string with a single U+0020 SPACE code point.
    StringBuilder builder;
    for (auto code_point : Utf8View { string }) {
        if (Infra::is_ascii_whitespace(code_point)) {
            if (!builder.string_view().ends_with(' '))
                builder.append(' ');
            continue;
        }
        builder.append_code_point(code_point);
    }

    // ...and then remove any leading and trailing ASCII whitespace from that string.
    return builder.string_view().trim(Infra::ASCII_WHITESPACE);
}

}
