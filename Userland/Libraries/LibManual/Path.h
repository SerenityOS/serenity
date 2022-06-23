/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/LexicalPath.h>

namespace Manual {

LexicalPath const manual_base_path { "/usr/share/man" };

constexpr StringView const top_level_section_prefix = "man"sv;

}
