/*
 * Copyright (c) 2022, Maciej <sppmacd@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Utils.h"

#include <AK/CharacterTypes.h>

// FIXME: Maybe move it to AK/String.h
bool is_valid_cpp_identifier(StringView identifier)
{
    // FIXME: Check for all reserved words
    if (identifier == "bool" || identifier == "i32")
        return false;
    if (!(identifier.length() > 0 && (is_ascii_alpha(identifier[0]) || identifier[0] == '_')))
        return false;
    for (char c : identifier) {
        if (!(is_ascii_alphanumeric(c) || c == '_'))
            return false;
    }
    return true;
}
