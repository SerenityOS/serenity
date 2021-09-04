/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Type.h"

namespace Spreadsheet {

class StringCell : public CellType {

public:
    StringCell();
    virtual ~StringCell() override;
    virtual String display(Cell&, CellTypeMetadata const&) const override;
    virtual JS::Value js_value(Cell&, CellTypeMetadata const&) const override;
};

}
