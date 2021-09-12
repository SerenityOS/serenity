/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Identity.h"
#include "../Cell.h"

namespace Spreadsheet {

IdentityCell::IdentityCell()
    : CellType("Identity")
{
}

IdentityCell::~IdentityCell()
{
}

String IdentityCell::display(Cell& cell, const CellTypeMetadata&) const
{
    return cell.js_data().to_string_without_side_effects();
}

JS::Value IdentityCell::js_value(Cell& cell, const CellTypeMetadata&) const
{
    return cell.js_data();
}

}
