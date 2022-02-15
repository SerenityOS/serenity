/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Identity.h"
#include "../Cell.h"
#include "../Spreadsheet.h"

namespace Spreadsheet {

IdentityCell::IdentityCell()
    : CellType("Identity")
{
}

JS::ThrowCompletionOr<String> IdentityCell::display(Cell& cell, const CellTypeMetadata&) const
{
    return cell.js_data().to_string(cell.sheet().global_object());
}

JS::ThrowCompletionOr<JS::Value> IdentityCell::js_value(Cell& cell, const CellTypeMetadata&) const
{
    return cell.js_data();
}

}
