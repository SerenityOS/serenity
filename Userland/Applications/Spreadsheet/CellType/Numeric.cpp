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

JS::ThrowCompletionOr<String> NumericCell::display(Cell& cell, const CellTypeMetadata& metadata) const
{
    return propagate_failure(cell, [&]() -> JS::ThrowCompletionOr<String> {
        auto value = TRY(js_value(cell, metadata));
        String string;
        if (metadata.format.is_empty())
            string = TRY(value.to_string(cell.sheet().global_object()));
        else
            string = format_double(metadata.format.characters(), TRY(value.to_double(cell.sheet().global_object())));

        if (metadata.length >= 0)
            return string.substring(0, metadata.length);

        return string;
    });
}

JS::ThrowCompletionOr<JS::Value> NumericCell::js_value(Cell& cell, const CellTypeMetadata&) const
{
    return propagate_failure(cell, [&]() {
        return cell.js_data().to_number(cell.sheet().global_object());
    });
}

}
