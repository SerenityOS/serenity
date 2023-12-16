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
    : CellType("Identity"sv)
{
}

JS::ThrowCompletionOr<ByteString> IdentityCell::display(Cell& cell, CellTypeMetadata const& metadata) const
{
    auto& vm = cell.sheet().global_object().vm();
    auto data = cell.js_data();
    if (!metadata.format.is_empty())
        data = TRY(cell.sheet().evaluate(metadata.format, &cell));

    return data.to_byte_string(vm);
}

JS::ThrowCompletionOr<JS::Value> IdentityCell::js_value(Cell& cell, CellTypeMetadata const&) const
{
    return cell.js_data();
}

String IdentityCell::metadata_hint(MetadataName metadata) const
{
    if (metadata == MetadataName::Length)
        return "Ignored"_string;
    if (metadata == MetadataName::Format)
        return "JavaScript expression, `value' refers to the cell's value"_string;

    return {};
}

}
