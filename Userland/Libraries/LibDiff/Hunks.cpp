/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2023, Shannon Booth <shannon.ml.booth@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Hunks.h"
#include <AK/Debug.h>

namespace Diff {

Optional<HunkLocation> Parser::consume_unified_location()
{
    auto consume_range = [this](Range& range) {
        if (!consume_line_number(range.start_line))
            return false;

        if (consume_specific(',')) {
            if (!consume_line_number(range.number_of_lines))
                return false;
        } else {
            range.number_of_lines = 1;
        }
        return true;
    };

    if (!consume_specific("@@ -"))
        return {};

    HunkLocation location;

    if (!consume_range(location.old_range))
        return {};

    if (!consume_specific(" +"))
        return {};

    if (!consume_range(location.new_range))
        return {};

    if (!consume_specific(" @@"))
        return {};

    return location;
}

bool Parser::consume_line_number(size_t& number)
{
    auto line = consume_while(is_ascii_digit);

    auto maybe_number = line.to_uint<size_t>();
    if (!maybe_number.has_value())
        return false;

    number = maybe_number.value();
    return true;
}

ErrorOr<Vector<Hunk>> Parser::parse_hunks()
{
    Vector<Hunk> hunks;

    while (!is_eof()) {
        // Try an locate a hunk location in this hunk. It may be prefixed with information.
        auto maybe_location = consume_unified_location();
        consume_line();

        if (!maybe_location.has_value())
            continue;

        Hunk hunk { *maybe_location, {} };

        auto old_lines_expected = hunk.location.old_range.number_of_lines;
        auto new_lines_expected = hunk.location.new_range.number_of_lines;

        // We've found a location. Now parse out all of the expected content lines.
        while (old_lines_expected != 0 || new_lines_expected != 0) {
            StringView line = consume_line();

            if (line.is_empty())
                return Error::from_string_literal("Malformed empty content line in patch");

            if (line[0] != ' ' && line[0] != '+' && line[0] != '-')
                return Error::from_string_literal("Invaid operation in patch");

            auto const operation = Line::operation_from_symbol(line[0]);

            if (operation != Line::Operation::Removal) {
                if (new_lines_expected == 0)
                    return Error::from_string_literal("Found more removal and context lines in patch than expected");

                --new_lines_expected;
            }

            if (operation != Line::Operation::Addition) {
                if (old_lines_expected == 0)
                    return Error::from_string_literal("Found more addition and context lines in patch than expected");

                --old_lines_expected;
            }

            auto const content = line.substring_view(1, line.length() - 1);
            TRY(hunk.lines.try_append(Line { operation, TRY(String::from_utf8(content)) }));
        }

        TRY(hunks.try_append(hunk));
    }

    if constexpr (HUNKS_DEBUG) {
        for (auto const& hunk : hunks) {
            dbgln("{}", hunk.location);
            for (auto const& line : hunk.lines)
                dbgln("{}", line);
        }
    }

    return hunks;
}

ErrorOr<Vector<Hunk>> parse_hunks(StringView diff)
{
    Parser lexer(diff);
    return lexer.parse_hunks();
}
}
