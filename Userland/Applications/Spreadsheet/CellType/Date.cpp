/*
 * Copyright (c) 2020, the SerenityOS developers.
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

DateCell::~DateCell()
{
}

String DateCell::display(Cell& cell, const CellTypeMetadata& metadata) const
{
    ScopeGuard propagate_exception { [&cell] {
        if (auto exc = cell.sheet().interpreter().exception()) {
            cell.sheet().interpreter().vm().clear_exception();
            cell.set_exception(exc);
        }
    } };
    auto timestamp = js_value(cell, metadata);
    auto string = Core::DateTime::from_timestamp(timestamp.to_i32(cell.sheet().global_object())).to_string(metadata.format.is_empty() ? "%Y-%m-%d %H:%M:%S" : metadata.format.characters());

    if (metadata.length >= 0)
        return string.substring(0, metadata.length);

    return string;
}

JS::Value DateCell::js_value(Cell& cell, const CellTypeMetadata&) const
{
    auto js_data = cell.js_data();
    auto value = js_data.to_double(cell.sheet().global_object());
    return JS::Value(value / 1000); // Turn it to seconds
}

}
