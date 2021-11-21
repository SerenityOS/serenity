/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Type.h"

namespace Spreadsheet {

class NumericCell : public CellType {

public:
    NumericCell();
    virtual ~NumericCell() override;
    virtual JS::ThrowCompletionOr<String> display(Cell&, const CellTypeMetadata&) const override;
    virtual JS::ThrowCompletionOr<JS::Value> js_value(Cell&, const CellTypeMetadata&) const override;
};

}
