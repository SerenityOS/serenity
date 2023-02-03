/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, networkException <networkexception@serenityos.org>
 * Copyright (c) 2023, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>

namespace Web::Infra {

DeprecatedString strip_and_collapse_whitespace(StringView string);
bool is_code_unit_prefix(StringView potential_prefix, StringView input);
ErrorOr<String> convert_to_scalar_value_string(StringView string);

}
