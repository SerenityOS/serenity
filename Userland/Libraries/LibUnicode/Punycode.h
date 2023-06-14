/*
 * Copyright (c) 2023, Simon Wanner <simon@skyrising.xyz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>

namespace Unicode::Punycode {

ErrorOr<String> decode(StringView);
ErrorOr<String> encode(StringView);
ErrorOr<String> encode(Utf32View);

}
