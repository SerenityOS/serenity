/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "IdentificationString.h"

#include <AK/Debug.h>
#include <AK/Format.h>

namespace SSH {

ErrorOr<void> validate_identification_string(ReadonlyBytes input)
{
    // "The maximum length of the string is 255 characters, including the
    // Carriage Return and Line Feed."
    if (input.size() > 255)
        return Error::from_string_literal("Protocol version string is too long");

    auto protocol_string = StringView { input };

    if (input.size() <= 10)
        return Error::from_string_literal("Protocol version string is too short");

    auto match_and_consume = [&](StringView match) -> bool {
        if (!protocol_string.starts_with(match))
            return false;

        protocol_string = protocol_string.bytes().slice(match.length());
        return true;
    };

    if (!match_and_consume("SSH-"sv))
        return Error::from_string_literal("Protocol is not SSH");

    if (!match_and_consume("2.0"sv))
        return Error::from_string_literal("Invalid protocol version");

    if (!match_and_consume("-"sv))
        return Error::from_string_literal("Missing hyphen after protocol version");

    if (!protocol_string.ends_with("\r\n"sv))
        return Error::from_string_literal("Protocol version string doesn't end with CR LF");

    dbgln_if(SSH_DEBUG, "Client protocol string: {:s}", input.trim(input.size() - 2));

    return {};
}

}
