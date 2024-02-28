/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>

namespace PDF {

Optional<u32> glyph_name_to_unicode(StringView, bool is_zapf_dingbats);

}
