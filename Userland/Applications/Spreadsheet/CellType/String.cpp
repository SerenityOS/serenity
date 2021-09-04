/*
 * Copyright (c) 2020, the SerenityOS developers.
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

StringCell::~StringCell()
{
}

String StringCell::display(Cell& cell, CellTypeMetadata const& metadata) const
{
    auto string = cell.js_data().to_string_without_side_effects();
    if (metadata.length >= 0)
        return string.substring(0, metadata.length);

    return string;
}

JS::Value StringCell::js_value(Cell& cell, CellTypeMetadata const& metadata) const
{
    auto string = display(cell, metadata);
    return JS::js_string(cell.sheet().interpreter().heap(), string);
}

}
