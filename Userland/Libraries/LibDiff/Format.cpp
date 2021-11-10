/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Format.h"
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>

namespace Diff {
String generate_only_additions(const String& text)
{
    auto lines = text.split('\n', true); // Keep empty
    StringBuilder builder;
    builder.appendff("@@ -0,0 +1,{} @@\n", lines.size());
    for (const auto& line : lines) {
        builder.appendff("+{}\n", line);
    }
    return builder.to_string();
}
};
