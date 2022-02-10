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
    : CellType("Date")
{
}

JS::ThrowCompletionOr<String> DateCell::display(Cell& cell, const CellTypeMetadata& metadata) const
{
    return propagate_failure(cell, [&]() -> JS::ThrowCompletionOr<String> {
        auto timestamp = TRY(js_value(cell, metadata));
        auto string = Core::DateTime::from_timestamp(TRY(timestamp.to_i32(cell.sheet().global_object()))).to_string(metadata.format.is_empty() ? "%Y-%m-%d %H:%M:%S" : metadata.format.characters());

        if (metadata.length >= 0)
            return string.substring(0, metadata.length);

        return string;
    });
}

JS::ThrowCompletionOr<JS::Value> DateCell::js_value(Cell& cell, const CellTypeMetadata&) const
{
    auto js_data = cell.js_data();
    auto value = TRY(js_data.to_double(cell.sheet().global_object()));
    return JS::Value(value / 1000); // Turn it to seconds
}

}
