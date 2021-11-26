/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/BlockContainer.h>

namespace Web::Layout {

class TableCellBox final : public BlockContainer {
public:
    TableCellBox(DOM::Document&, DOM::Element*, NonnullRefPtr<CSS::StyleProperties>);
    TableCellBox(DOM::Document&, DOM::Element*, CSS::ComputedValues);
    virtual ~TableCellBox() override;

    TableCellBox* next_cell() { return next_sibling_of_type<TableCellBox>(); }
    const TableCellBox* next_cell() const { return next_sibling_of_type<TableCellBox>(); }

    size_t colspan() const;

    static CSS::Display static_display() { return CSS::Display { CSS::Display::Internal::TableCell }; }
};

}
