/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Numeric.h"
#include "../Cell.h"
#include "../Spreadsheet.h"
#include "Format.h"
#include <YAK/ScopeGuard.h>

namespace Spreadsheet {

NumericCell::NumericCell()
    : CellType("Numeric")
{
}

NumericCell::~NumericCell()
{
}

String NumericCell::display(Cell& cell, const CellTypeMetadata& metadata) const
{
    ScopeGuard propagate_exception { [&cell] {
        if (auto exc = cell.sheet().interpreter().exception()) {
            cell.sheet().interpreter().vm().clear_exception();
            cell.set_exception(exc);
        }
    } };
    auto value = js_value(cell, metadata);
    String string;
    if (metadata.format.is_empty())
        string = value.to_string_without_side_effects();
    else
        string = format_double(metadata.format.characters(), value.to_double(cell.sheet().global_object()));

    if (metadata.length >= 0)
        return string.substring(0, metadata.length);

    return string;
}

JS::Value NumericCell::js_value(Cell& cell, const CellTypeMetadata&) const
{
    ScopeGuard propagate_exception { [&cell] {
        if (auto exc = cell.sheet().interpreter().exception()) {
            cell.sheet().interpreter().vm().clear_exception();
            cell.set_exception(exc);
        }
    } };
    return cell.js_data().to_number(cell.sheet().global_object());
}

}
