/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "../Cell.h"
#include "Type.h"

namespace Spreadsheet {

template<typename Callable>
static auto propagate_failure(Cell& cell, Callable&& steps)
{
    auto result_or_error = steps();
    if (result_or_error.is_error())
        cell.set_thrown_value(*result_or_error.throw_completion().value());

    return result_or_error;
}

class NumericCell : public CellType {

public:
    NumericCell();
    virtual ~NumericCell() override = default;
    virtual JS::ThrowCompletionOr<ByteString> display(Cell&, CellTypeMetadata const&) const override;
    virtual JS::ThrowCompletionOr<JS::Value> js_value(Cell&, CellTypeMetadata const&) const override;
    virtual String metadata_hint(MetadataName) const override;
};

}
