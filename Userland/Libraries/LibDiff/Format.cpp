/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Format.h"
#include <AK/DeprecatedString.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>

namespace Diff {
DeprecatedString generate_only_additions(StringView text)
{
    auto lines = text.split_view('\n', SplitBehavior::KeepEmpty);
    StringBuilder builder;
    builder.appendff("@@ -0,0 +1,{} @@\n", lines.size());
    for (auto const& line : lines) {
        builder.appendff("+{}\n", line);
    }
    return builder.to_deprecated_string();
}
};
