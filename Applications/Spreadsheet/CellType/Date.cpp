/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "Date.h"
#include "../Cell.h"
#include "../Spreadsheet.h"
#include <LibCore/DateTime.h>

namespace Spreadsheet {

DateCell::DateCell()
    : CellType("Date")
{
}

DateCell::~DateCell()
{
}

String DateCell::display(Cell& cell, const CellTypeMetadata& metadata) const
{
    auto timestamp = js_value(cell, metadata);
    auto string = Core::DateTime::from_timestamp(timestamp.to_i32(cell.sheet->global_object())).to_string(metadata.format.is_empty() ? "%Y-%m-%d %H:%M:%S" : metadata.format.characters());

    if (metadata.length >= 0)
        return string.substring(0, metadata.length);

    return string;
}

JS::Value DateCell::js_value(Cell& cell, const CellTypeMetadata&) const
{
    auto value = cell.js_data().to_double(cell.sheet->global_object());
    return JS::Value(value / 1000); // Turn it to seconds
}

}
