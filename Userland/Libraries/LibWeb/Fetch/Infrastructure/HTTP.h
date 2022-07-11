/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/StringView.h>

namespace Web::Fetch {

// https://fetch.spec.whatwg.org/#http-tab-or-space
// An HTTP tab or space is U+0009 TAB or U+0020 SPACE.
inline constexpr StringView HTTP_TAB_OR_SPACE = "\t "sv;

}
