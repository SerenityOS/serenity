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

JS::ThrowCompletionOr<String> IdentityCell::display(Cell& cell, CellTypeMetadata const& metadata) const
{
    auto data = cell.js_data();
    if (!metadata.format.is_empty())
        data = TRY(cell.sheet().evaluate(metadata.format, &cell));

    return data.to_string(cell.sheet().global_object());
}

JS::ThrowCompletionOr<JS::Value> IdentityCell::js_value(Cell& cell, CellTypeMetadata const&) const
{
    return cell.js_data();
}

String IdentityCell::metadata_hint(MetadataName metadata) const
{
    if (metadata == MetadataName::Length)
        return "Ignored";
    if (metadata == MetadataName::Format)
        return "JavaScript expression, `value' refers to the cell's value";

    return {};
}

}
