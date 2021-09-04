/*
 * Copyright (c) 2020, the SerenityOS developers.
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

IdentityCell::~IdentityCell()
{
}

String IdentityCell::display(Cell& cell, CellTypeMetadata const&) const
{
    return cell.js_data().to_string_without_side_effects();
}

JS::Value IdentityCell::js_value(Cell& cell, CellTypeMetadata const&) const
{
    return cell.js_data();
}

}
