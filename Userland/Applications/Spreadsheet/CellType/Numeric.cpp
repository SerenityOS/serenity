/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Numeric.h"
#include "../Cell.h"
#include "../Spreadsheet.h"
#include "Format.h"
#include <AK/ScopeGuard.h>

namespace Spreadsheet {

NumericCell::NumericCell()
    : CellType("Numeric")
{
}

JS::ThrowCompletionOr<String> NumericCell::display(Cell& cell, CellTypeMetadata const& metadata) const
{
    return propagate_failure(cell, [&]() -> JS::ThrowCompletionOr<String> {
        auto value = TRY(js_value(cell, metadata));
        String string;
        if (metadata.format.is_empty())
            string = TRY(value.to_string(cell.sheet().global_object()));
        else
            string = format_double(metadata.format.characters(), TRY(value.to_double(cell.sheet().global_object())));

        if (metadata.length >= 0)
            return string.substring(0, min(string.length(), metadata.length));

        return string;
    });
}

JS::ThrowCompletionOr<JS::Value> NumericCell::js_value(Cell& cell, CellTypeMetadata const&) const
{
    return propagate_failure(cell, [&]() {
        return cell.js_data().to_number(cell.sheet().global_object());
    });
}

String NumericCell::metadata_hint(MetadataName metadata) const
{
    if (metadata == MetadataName::Format)
        return "Format string as accepted by `printf', all numeric formats refer to the same value (the cell's value)";

    return {};
}

}
