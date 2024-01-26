/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Date.h"
#include "../Cell.h"
#include "../Spreadsheet.h"
#include <AK/ScopeGuard.h>
#include <LibCore/DateTime.h>

namespace Spreadsheet {

DateCell::DateCell()
    : CellType("Date"sv)
{
}

JS::ThrowCompletionOr<String> DateCell::display(Cell& cell, CellTypeMetadata const& metadata) const
{
    return propagate_failure(cell, [&]() -> JS::ThrowCompletionOr<String> {
        auto& vm = cell.sheet().global_object().vm();
        auto timestamp = TRY(js_value(cell, metadata));
        auto string = MUST(Core::DateTime::from_timestamp(TRY(timestamp.to_i32(vm))).to_string(metadata.format.is_empty() ? "%Y-%m-%d %H:%M:%S"sv : metadata.format.bytes_as_string_view()));

        if (metadata.length >= 0)
            return MUST(String::from_view(string.code_points().unicode_substring_view(0, min(string.code_points().length(), metadata.length))));

        return string;
    });
}

JS::ThrowCompletionOr<JS::Value> DateCell::js_value(Cell& cell, CellTypeMetadata const&) const
{
    auto& vm = cell.sheet().global_object().vm();
    auto js_data = cell.js_data();
    auto value = TRY(js_data.to_double(vm));
    return JS::Value(value / 1000); // Turn it to seconds
}

String DateCell::metadata_hint(MetadataName metadata) const
{
    if (metadata == MetadataName::Format)
        return "Date format string as supported by `strftime'"_string;
    return {};
}

}
