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
#include <LibJS/Runtime/ValueInlines.h>

namespace Spreadsheet {

NumericCell::NumericCell()
    : CellType("Numeric"sv)
{
}

JS::ThrowCompletionOr<String> NumericCell::display(Cell& cell, CellTypeMetadata const& metadata) const
{
    return propagate_failure(cell, [&]() -> JS::ThrowCompletionOr<String> {
        auto& vm = cell.sheet().global_object().vm();
        auto value = TRY(js_value(cell, metadata));
        String string;
        if (metadata.format.is_empty())
            string = TRY(value.to_string(vm));
        else {
            auto format = MUST(String::formatted("{}\0", metadata.format));
            string = format_double(format.bytes_as_string_view().characters_without_null_termination(), TRY(value.to_double(vm)));
        }

        if (metadata.length >= 0)
            return MUST(String::from_view(string.code_points().unicode_substring_view(0, min(string.code_points().length(), metadata.length))));

        return string;
    });
}

JS::ThrowCompletionOr<JS::Value> NumericCell::js_value(Cell& cell, CellTypeMetadata const&) const
{
    return propagate_failure(cell, [&]() {
        auto& vm = cell.sheet().global_object().vm();
        return cell.js_data().to_number(vm);
    });
}

String NumericCell::metadata_hint(MetadataName metadata) const
{
    if (metadata == MetadataName::Format)
        return "Format string as accepted by `printf', all numeric formats refer to the same value (the cell's value)"_string;

    return {};
}

}
