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

ErrorOr<void> write_unified(Hunk const& hunk, Stream& stream, ColorOutput color_output)
{
    TRY(stream.write_formatted("{}\n", hunk.location));

    if (color_output == ColorOutput::Yes) {
        for (auto const& line : hunk.lines) {
            if (line.operation == Line::Operation::Addition)
                TRY(stream.write_formatted("\033[32;1m{}\033[0m\n", line));
            else if (line.operation == Line::Operation::Removal)
                TRY(stream.write_formatted("\033[31;1m{}\033[0m\n", line));
            else
                TRY(stream.write_formatted("{}\n", line));
        }
    } else {
        for (auto const& line : hunk.lines)
            TRY(stream.write_formatted("{}\n", line));
    }

    return {};
}

ErrorOr<void> write_normal(Hunk const& hunk, Stream& stream, ColorOutput color_output)
{
    // Source line(s)
    TRY(stream.write_formatted("{}", hunk.location.old_range.start_line));
    if (hunk.location.old_range.number_of_lines > 1)
        TRY(stream.write_formatted(",{}", (hunk.location.old_range.start_line + hunk.location.old_range.number_of_lines - 1)));

    // Action
    if (hunk.location.old_range.number_of_lines > 0 && hunk.location.new_range.number_of_lines > 0)
        TRY(stream.write_formatted("c"));
    else if (hunk.location.new_range.number_of_lines > 0)
        TRY(stream.write_formatted("a"));
    else
        TRY(stream.write_formatted("d"));

    // Target line(s)
    TRY(stream.write_formatted("{}", hunk.location.new_range.start_line));
    if (hunk.location.new_range.number_of_lines > 1)
        TRY(stream.write_formatted(",{}", (hunk.location.new_range.start_line + hunk.location.new_range.number_of_lines - 1)));

    TRY(stream.write_formatted("\n"));

    for (auto const& line : hunk.lines) {
        VERIFY(line.operation == Line::Operation::Removal || line.operation == Line::Operation::Addition);

        if (line.operation == Line::Operation::Addition) {
            if (color_output == ColorOutput::Yes)
                TRY(stream.write_formatted("\033[32;1m> {}\033[0m\n", line.content));
            else
                TRY(stream.write_formatted("> {}\n", line.content));
        } else {
            if (color_output == ColorOutput::Yes)
                TRY(stream.write_formatted("\033[31;1m< {}\033[0m\n", line.content));
            else
                TRY(stream.write_formatted("< {}\n", line.content));
        }
    }

    return {};
}

}
