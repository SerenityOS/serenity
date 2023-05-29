/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/BlockContainer.h>

namespace Web::Layout {

class TableCellBox final : public BlockContainer {
    JS_CELL(TableCellBox, BlockContainer);

public:
    TableCellBox(DOM::Document&, DOM::Element*, NonnullRefPtr<CSS::StyleProperties>);
    TableCellBox(DOM::Document&, DOM::Element*, CSS::ComputedValues);
    virtual ~TableCellBox() override;

    size_t colspan() const;
    size_t rowspan() const;
};

}
