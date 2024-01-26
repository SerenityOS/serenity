/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "String.h"
#include "../Cell.h"
#include "../Spreadsheet.h"
#include <LibJS/Runtime/Completion.h>

namespace Spreadsheet {

StringCell::StringCell()
    : CellType("String"sv)
{
}

JS::ThrowCompletionOr<String> StringCell::display(Cell& cell, CellTypeMetadata const& metadata) const
{
    auto& vm = cell.sheet().global_object().vm();
    auto string = TRY(cell.js_data().to_string(vm));
    if (metadata.length >= 0)
        return MUST(String::from_view(string.code_points().unicode_substring_view(0, metadata.length)));

    return string;
}

JS::ThrowCompletionOr<JS::Value> StringCell::js_value(Cell& cell, CellTypeMetadata const& metadata) const
{
    auto& vm = cell.sheet().vm();
    auto string = TRY(display(cell, metadata));
    return JS::PrimitiveString::create(vm, string);
}

String StringCell::metadata_hint(MetadataName metadata) const
{
    if (metadata == MetadataName::Format)
        return "Ignored"_string;
    return {};
}

}
