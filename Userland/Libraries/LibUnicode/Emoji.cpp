/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibUnicode/Emoji.h>

namespace Unicode {

Optional<Emoji> __attribute__((weak)) find_emoji_for_code_points(Span<u32 const>) { return {}; }

}
