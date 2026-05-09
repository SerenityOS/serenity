/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>

namespace SSH {

constexpr auto PROTOCOL_STRING = "SSH-2.0-SerenitySSH SerenityOS\r\n"sv;

ErrorOr<void> validate_identification_string(ReadonlyBytes);

}
