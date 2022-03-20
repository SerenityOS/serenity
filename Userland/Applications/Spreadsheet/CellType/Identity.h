/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Type.h"

namespace Spreadsheet {

class IdentityCell : public CellType {

public:
    IdentityCell();
    virtual ~IdentityCell() override = default;
    virtual JS::ThrowCompletionOr<String> display(Cell&, const CellTypeMetadata&) const override;
    virtual JS::ThrowCompletionOr<JS::Value> js_value(Cell&, const CellTypeMetadata&) const override;
    String metadata_hint(MetadataName) const override;
};

}
