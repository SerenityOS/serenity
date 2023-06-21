/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2023, Shannon Booth <shannon.ml.booth@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Format.h"
#include <AK/DeprecatedString.h>
#include <AK/Stream.h>
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

ErrorOr<void> write_normal(Hunk const& hunk, Stream& stream, ColorOutput color_output)
{
    auto original_start = hunk.original_start_line;
    auto target_start = hunk.target_start_line;
    auto num_added = hunk.added_lines.size();
    auto num_removed = hunk.removed_lines.size();

    // Source line(s)
    TRY(stream.write_formatted("{}", original_start));

    if (num_removed > 1)
        TRY(stream.write_formatted(",{}", original_start + num_removed - 1));

    // Action
    if (num_added > 0 && num_removed > 0)
        TRY(stream.write_formatted("c"));
    else if (num_added > 0)
        TRY(stream.write_formatted("a"));
    else
        TRY(stream.write_formatted("d"));

    // Target line(s)
    TRY(stream.write_formatted("{}", target_start));
    if (num_added > 1)
        TRY(stream.write_formatted(",{}", target_start + num_added - 1));

    TRY(stream.write_formatted("\n"));

    for (auto const& line : hunk.removed_lines) {
        if (color_output == ColorOutput::Yes)
            TRY(stream.write_formatted("\033[31;1m< {}\033[0m\n", line));
        else
            TRY(stream.write_formatted("< {}\n", line));
    }

    if (num_added > 0 && num_removed > 0)
        TRY(stream.write_formatted("---\n"));

    for (auto const& line : hunk.added_lines) {
        if (color_output == ColorOutput::Yes)
            TRY(stream.write_formatted("\033[32;1m> {}\033[0m\n", line));
        else
            TRY(stream.write_formatted("> {}\n", line));
    }

    return {};
}

}
