/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "String.h"
#include "../Cell.h"
#include "../Spreadsheet.h"

namespace Spreadsheet {

StringCell::StringCell()
    : CellType("String")
{
}

JS::ThrowCompletionOr<String> StringCell::display(Cell& cell, const CellTypeMetadata& metadata) const
{
    auto string = TRY(cell.js_data().to_string(cell.sheet().global_object()));
    if (metadata.length >= 0)
        return string.substring(0, metadata.length);

    return string;
}

JS::ThrowCompletionOr<JS::Value> StringCell::js_value(Cell& cell, const CellTypeMetadata& metadata) const
{
    auto string = TRY(display(cell, metadata));
    return JS::js_string(cell.sheet().interpreter().heap(), string);
}

}
